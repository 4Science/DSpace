/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.servicemanager.DSpaceKernelInit;

/**
 * Attempt to parse date strings in a variety of formats, including BCE dates.
 * This uses an external list of regular expressions and associated DateTimeFormatter patterns.
 * Inject the list as pairs of strings using {@link #setPatterns}.
 *
 * Dates are parsed as being in the UTC zone.
 *
 * @author mwood
 */
public class SolrMultiFormatDateParser {
    private static final Logger log = LogManager.getLogger();

    /**
     * A list of rules, each binding a regular expression to a date format.
     */
    private static final ArrayList<Rule> rules = new ArrayList<>();

    private static final ZoneId UTC_ZONE = ZoneOffset.UTC;

    private static final BceYearStrategy ADJUSTED = year -> -(year - 1);
    private static final BceYearStrategy RAW      = year -> -year;

    @FunctionalInterface
    private interface BceYearStrategy {
        int toProlepticYear(int parsedYear);
    }

    @Inject
    public void setPatterns(Map<String, String> patterns) {
        for (Entry<String, String> ruleEntry : patterns.entrySet()) {
            String regex = ruleEntry.getKey();
            String patternStr = ruleEntry.getValue();

            Pattern pattern;
            try {
                pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException ex) {
                log.error("Skipping format with unparsable pattern '{}'", ruleEntry::getKey);
                continue;
            }

            DateTimeFormatter formatter;
            try {
                if (patternStr.contains("MMM")) {
                    formatter = new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendValue(ChronoField.YEAR, 1, 4, SignStyle.NORMAL)
                        .appendLiteral(' ')
                        .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT)
                        .appendLiteral(' ')
                        .appendValue(ChronoField.DAY_OF_MONTH)
                        .toFormatter()
                        .withZone(UTC_ZONE);
                } else {
                    formatter = DateTimeFormatter.ofPattern(patternStr).withZone(UTC_ZONE);
                }
            } catch (IllegalArgumentException ex) {
                log.error("Skipping uninterpretable date format '{}'", ruleEntry::getValue);
                continue;
            }

            // Determine granularity: YEAR, MONTH, DAY, TIME
            Rule.DateGranularity granularity = getGranularity(patternStr);

            // Detect BCE if pattern starts with "-" in regex
            boolean bce = regex.startsWith("-");

            rules.add(new Rule(pattern, formatter, granularity, bce));
        }
    }


    /**
     * Determine the granularity of a date pattern (e.g. YYYY-MM-DD is a "day", but YYYY-MM is a "month")
     */
    private static Rule.DateGranularity getGranularity(String datePattern) {
        if (datePattern.contains("HH")) {
            return Rule.DateGranularity.TIME;
        } else if (datePattern.contains("dd") || datePattern.contains("d")) {
            return Rule.DateGranularity.DAY;
        } else if (datePattern.contains("MM") || datePattern.contains("MMM")) {
            return Rule.DateGranularity.MONTH;
        } else {
            return Rule.DateGranularity.YEAR;
        }
    }

    public static ZonedDateTime parse(String dateString) {
        return parseInternal(dateString, RAW);
    }

    /**
     * Parse BCE dates using the "adjusted" strategy (subtract one).
     * For example: -2010 will become year -2009.
     */
    public static ZonedDateTime parseAdjusted(String dateString) {
        return parseInternal(dateString, ADJUSTED);
    }

    private static ZonedDateTime parseInternal(String dateString, BceYearStrategy strategy) {
        if (dateString == null) {
            return null;
        }

        boolean isBce = dateString.startsWith("-");
        String toParse = isBce ? dateString.substring(1).trim() : dateString;

        for (Rule candidate : rules) {
            if (candidate.pattern.matcher(toParse).matches()) {
                try {
                    ZonedDateTime result;
                    switch (candidate.granularity) {
                        case TIME:
                            result = ZonedDateTime.parse(toParse, candidate.format);
                            break;
                        case DAY:
                            result = LocalDate.parse(toParse, candidate.format)
                                              .atStartOfDay(UTC_ZONE);
                            break;
                        case MONTH:
                            result = YearMonth.parse(toParse, candidate.format)
                                              .atDay(1).atStartOfDay(UTC_ZONE);
                            break;
                        case YEAR:
                            result = Year.parse(toParse, candidate.format)
                                         .atMonth(1).atDay(1).atStartOfDay(UTC_ZONE);
                            break;
                        default:
                            throw new DateTimeException("Could not find a valid parser for this matched pattern.");
                    }

                    if (result.getYear() == 0) {
                        result = result.withYear(strategy.toProlepticYear(0));
                    }
                    if (isBce) {
                        result = result.withYear(strategy.toProlepticYear(result.getYear()));
                    }

                    return result;
                } catch (DateTimeParseException ex) {
                    log.info("Date string '{}' matched pattern '{}' but did not parse: {}",
                             () -> dateString, candidate.format::toString, ex::getMessage);
                }
            }
        }

        // fallback to regex BCE handler
        if (isBce) {
            return parseBce(dateString, strategy);
        }

        return null;
    }



    /**
     * Parse BCE/proleptic date strings like:
     * -2010, -2010-02, -2010-02-13, -2010-02-13T00:00:00Z
     */
    private static ZonedDateTime parseBce(String s, BceYearStrategy strategy) {
        try {
            // full instant with time (YYYY-MM-DDThh:mm:ssZ, year may be 1-4 digits)
            if (s.matches("^-\\d{1,4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$")) {
                String[] dateTimeParts = s.substring(1, s.length() - 1).split("T")[0].split("-");
                int year  = Integer.parseInt(dateTimeParts[0]);
                int month = Integer.parseInt(dateTimeParts[1]);
                int day   = Integer.parseInt(dateTimeParts[2]);
                return LocalDate.of(strategy.toProlepticYear(year), month, day)
                                .atStartOfDay(UTC_ZONE);
            }
            // YYYY-MM-DD
            if (s.matches("^-\\d{1,4}-\\d{2}-\\d{2}$")) {
                String[] parts = s.substring(1).split("-");
                int year  = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int day   = Integer.parseInt(parts[2]);
                return LocalDate.of(strategy.toProlepticYear(year), month, day)
                                .atStartOfDay(UTC_ZONE);
            }
            // YYYY-MM
            if (s.matches("^-\\d{1,4}-\\d{2}$")) {
                String[] parts = s.substring(1).split("-");
                int year  = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                return LocalDate.of(strategy.toProlepticYear(year), month, 1)
                                .atStartOfDay(UTC_ZONE);
            }
            // YYYY
            if (s.matches("^-\\d{1,4}$")) {
                int year = Integer.parseInt(s.substring(1));
                return LocalDate.of(strategy.toProlepticYear(year), 1, 1)
                                .atStartOfDay(UTC_ZONE);
            }
        } catch (DateTimeException | NumberFormatException ex) {
            log.warn("BCE date '{}' did not parse: {}", s, ex.getMessage());
        }
        return null;
    }

    /**
     * Small CLI for testing.
     */
    public static void main(String[] args) throws IOException {
        DSpaceKernelInit.getKernel(null); // Mainly to initialize Spring

        if (args.length > 0) {
            for (String arg : args) {
                testDate(arg);
            }
        } else {
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = input.readLine()) != null) {
                testDate(line.trim());
            }
        }
    }

    private static void testDate(String arg) {
        ZonedDateTime result = parse(arg);
        if (null == result) {
            System.out.println("Did not match any pattern.");
        } else {
            System.out.println(result.format(DateTimeFormatter.ISO_INSTANT));
        }
    }

    /**
     * Holder for a rule.
     */
    private static class Rule {
        enum DateGranularity { YEAR, MONTH, DAY, TIME }
        final Pattern pattern;
        final DateTimeFormatter format;
        final DateGranularity granularity;
        final boolean bce;

        public Rule(Pattern pattern, DateTimeFormatter format,
                    DateGranularity granularity, boolean bce) {
            this.pattern = pattern;
            this.format = format;
            this.granularity = granularity;
            this.bce = bce;
        }
    }
}
