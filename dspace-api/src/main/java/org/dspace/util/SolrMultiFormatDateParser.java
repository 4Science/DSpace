/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 *
 **/
public class SolrMultiFormatDateParser {
    private static final Logger log = LogManager.getLogger();

    private static final ArrayList<Rule> rules = new ArrayList<>();
    private static final TimeZone UTC_ZONE = TimeZone.getTimeZone("UTC");
    private static final ZoneId UTC = ZoneId.of("UTC");

    private static final BceYearStrategy ADJUSTED = year -> -(year - 1);
    private static final BceYearStrategy RAW      = year -> -year;

    @FunctionalInterface
    private interface BceYearStrategy {
        int toProlepticYear(int parsedYear);
    }


    public static ZonedDateTime parse(String dateString) {
        return parse(dateString, ADJUSTED);
    }

    public static ZonedDateTime parseRaw(String dateString) {
        return parse(dateString, RAW);
    }

    private static ZonedDateTime parse(String dateString, BceYearStrategy strategy) {
        if (dateString == null) {
            return null;
        }

        if (dateString.startsWith("-")) {
            ZonedDateTime bce = parseBce(dateString, strategy);
            if (bce != null) {
                return bce;
            }
        }

        // CE path with regex/format rules
        for (Rule candidate : rules) {
            if (candidate.pattern.matcher(dateString).matches()) {
                try {
                    synchronized (candidate.format) {
                        Date parsedDate = candidate.format.parse(dateString);
                        return parsedDate.toInstant().atZone(UTC);
                    }
                } catch (ParseException ex) {
                    log.info("Date string '{}' matched pattern '{}' but did not parse: {}",
                             () -> dateString, candidate.format::toPattern, ex::getMessage);
                }
            }
        }
        return null;
    }

    /**
     * Parse BCE/proleptic date strings like:
     * -2010, -2010-02, -2010-02-13, -2010-02-13T00:00:00Z
     */
    private static ZonedDateTime parseBce(String s, BceYearStrategy strategy) {
        try {
            // Full instant with time (YYYY-MM-DDThh:mm:ssZ, year may be 1-4 digits)
            if (s.matches("^-\\d{1,4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$")) {
                String[] dateTimeParts = s.substring(1, s.length() - 1).split("T")[0].split("-");
                int year  = Integer.parseInt(dateTimeParts[0]);
                int month = Integer.parseInt(dateTimeParts[1]);
                int day   = Integer.parseInt(dateTimeParts[2]);

                return LocalDate.of(strategy.toProlepticYear(year), month, day)
                                .atStartOfDay(UTC);
            }

            // YYYY-MM-DD
            if (s.matches("^-\\d{1,4}-\\d{2}-\\d{2}$")) {
                String[] parts = s.substring(1).split("-");
                int year  = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int day   = Integer.parseInt(parts[2]);

                return LocalDate.of(strategy.toProlepticYear(year), month, day)
                                .atStartOfDay(UTC);
            }

            // YYYY-MM
            if (s.matches("^-\\d{1,4}-\\d{2}$")) {
                String[] parts = s.substring(1).split("-");
                int year  = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);

                return LocalDate.of(strategy.toProlepticYear(year), month, 1)
                                .atStartOfDay(UTC);
            }

            // YYYY
            if (s.matches("^-\\d{1,4}$")) {
                int year = Integer.parseInt(s.substring(1));
                return LocalDate.of(strategy.toProlepticYear(year), 1, 1)
                                .atStartOfDay(UTC);
            }

        } catch (DateTimeParseException | NumberFormatException ex) {
            log.warn("BCE date '{}' did not parse: {}", s, ex.getMessage());
        }
        return null;
    }



    /**
     * Add regex/format rules (injected externally).
     */
    @Inject
    public void setPatterns(Map<String, String> patterns) {
        for (Entry<String, String> rule : patterns.entrySet()) {
            Pattern pattern;
            try {
                pattern = Pattern.compile(rule.getKey(), Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException ex) {
                log.error("Skipping format with unparseable pattern '{}'", rule::getKey);
                continue;
            }

            SimpleDateFormat format;
            try {
                format = new SimpleDateFormat(rule.getValue());
            } catch (IllegalArgumentException ex) {
                log.error("Skipping uninterpretable date format '{}'", rule::getValue);
                continue;
            }
            format.setCalendar(Calendar.getInstance(UTC_ZONE));
            format.setLenient(false);

            rules.add(new Rule(pattern, format));
        }
    }

    /**
     * Holder for a pair:  compiled regex, compiled SimpleDateFormat.
     */
    private record Rule(Pattern pattern, SimpleDateFormat format) {
    }

}

