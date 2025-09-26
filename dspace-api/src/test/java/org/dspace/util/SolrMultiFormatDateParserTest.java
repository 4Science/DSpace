/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Drive the SolrMultiFormatDateParser from a table of test formats and sample data
 * using JUnit's Parameterized runner.
 *
 * @author mhwood
 */
@RunWith(Parameterized.class)
public class SolrMultiFormatDateParserTest {
    private static Locale vmLocale;
    private final String toParseDate;
    private final String expectedFormat;
    private final String expectedResult;

    /**
     * Test a single date format.
     * JUnit will instantiate this class repeatedly with data from {@link #dateFormatsToTest}.
     */
    public SolrMultiFormatDateParserTest(String toParseDate,
                                         String expectedFormat, String expectedResult) {
        this.toParseDate = toParseDate;
        this.expectedFormat = expectedFormat;
        this.expectedResult = expectedResult;
    }

    /**
     * Date formats and samples to drive the parameterized test.
     */
    @Parameterized.Parameters
    public static Collection dateFormatsToTest() {
        // Format: "String to parse", "format of string", "expected result in UTC"
        return Arrays.asList(new Object[][] {
            {"19570127", "yyyyMMdd", "1957-01-27T00:00Z"},
            {"27-01-1957", "dd-MM-yyyy", "1957-01-27T00:00Z"},
            {"1957-01-27", "yyyy-MM-dd", "1957-01-27T00:00Z"},
            {"01/27/1957", "MM/dd/yyyy", "1957-01-27T00:00Z"},
            {"1957/01/27", "yyyy/MM/dd","1957-01-27T00:00Z"},
            {"195701272006", "yyyyMMddHHmm", "1957-01-27T20:06Z"},
            {"19570127 2006", "yyyyMMdd HHmm", "1957-01-27T20:06Z"},
            {"27-01-1957 20:06", "dd-MM-yyyy HH:mm", "1957-01-27T20:06Z"},
            {"1957-01-27 20:06", "yyyy-MM-dd HH:mm", "1957-01-27T20:06Z"},
            {"01/27/1957 20:06", "MM/dd/yyyy HH:mm", "1957-01-27T20:06Z"},
            {"1957/01/27 20:06", "yyyy/MM/dd HH:mm", "1957-01-27T20:06Z"},
            {"19570127200620", "yyyyMMddHHmmss", "1957-01-27T20:06:20Z"},
            {"19570127 200620", "yyyyMMdd HHmmss", "1957-01-27T20:06:20Z"},
            {"27-01-1957 20:06:20", "dd-MM-yyyy HH:mm:ss", "1957-01-27T20:06:20Z"},
            {"01/27/1957 20:06:20", "MM/dd/yyyy HH:mm:ss", "1957-01-27T20:06:20Z"},
            {"1957/01/27 20:06:20", "yyyy/MM/dd HH:mm:ss", "1957-01-27T20:06:20Z"},
            {"1957 Jan 27", "yyyy MMM dd", "1957-01-27T00:00Z"},
            {"1957-01", "yyyy-MM", "1957-01-01T00:00Z"},
            {"195701", "yyyyMM", "1957-01-01T00:00Z"},
            {"1957", "yyyy", "1957-01-01T00:00Z"},
            {"1957-01-27T12:34:56Z", "yyyy-MM-dd'T'HH:mm:ss'Z'", "1957-01-27T12:34:56Z"},
            {"1957-01-27T12:34:56.789Z", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "1957-01-27T12:34:56.789Z"},
            // Empty string result means that this should not parse successfully
            {"1957/01/2720:06:20", "yyyy/MM/ddHH:mm:ss", ""},

            // BCE dates
            {"-2010", "uuuu", "-2010-01-01T00:00Z"},
//            {"-1", "uuuu", "true"},
//            {"-10", "uuuu", "-10-01-01T00:00Z"},
//            {"-100", "uuuu", "-100-01-01T00:00Z"},
//            {"-45 Aug 7", "uuuu MMM d", "true"},
            {"-2010-02", "uuuu-MM", "-2010-02-01T00:00Z"},
            {"-2010-02-13", "uuuu-MM-dd", "-2010-02-13T00:00Z"},
            {"-2010-02-13T00:01:01Z", "uuuu-MM-dd'T'HH:mm:ss'Z'", "-2010-02-13T00:01:01Z"},

            // Edge cases
            {"0001-01-01", "yyyy-MM-dd", "0001-01-01T00:00Z"},
            {"0000", "uuuu", "0000-01-01T00:00Z"},
            {"0000-03", "uuuu-MM", "0000-03-01T00:00Z"},
            {"0000-01-01", "uuuu", "0000-01-01T00:00Z"}
        });
    }

    @BeforeClass
    public static void setUpClass() {
        Map<String, String> formats = new HashMap<>(32);
        formats.put("\\d{8}", "uuuuMMdd");
        formats.put("\\d{1,2}-\\d{1,2}-\\d{4}", "dd-MM-uuuu");
        formats.put("\\d{4}-\\d{1,2}-\\d{1,2}", "uuuu-MM-dd");
        formats.put("\\d{4}-\\d{1,2}", "uuuu-MM");
        formats.put("\\d{1,2}/\\d{1,2}/\\d{4}", "MM/dd/uuuu");
        formats.put("\\d{4}/\\d{1,2}/\\d{1,2}", "uuuu/MM/dd");
        formats.put("\\d{1,2}\\s[a-z]{3}\\s\\d{4}", "dd MMM uuuu");
        formats.put("\\d{1,2}\\s[a-z]{4,}\\s\\d{4}", "dd MMMM uuuu");
        formats.put("\\d{12}", "uuuuMMddHHmm");
        formats.put("\\d{8}\\s\\d{4}", "uuuuMMdd HHmm");
        formats.put("\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}", "dd-MM-uuuu HH:mm");
        formats.put("\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}", "uuuu-MM-dd HH:mm");
        formats.put("\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}", "MM/dd/uuuu HH:mm");
        formats.put("\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}", "uuuu/MM/dd HH:mm");
        formats.put("\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}", "dd MMM uuuu HH:mm");
        formats.put("\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}", "dd MMMM uuuu HH:mm");
        formats.put("\\d{4}\\s[a-z]{3}\\s\\d{1,2}", "uuuu MMM dd");
        formats.put("\\d{14}", "uuuuMMddHHmmss");
        formats.put("\\d{6}", "uuuuMM");
        formats.put("\\d{1,4}", "uuuu");
        formats.put("\\d{8}\\s\\d{6}", "uuuuMMdd HHmmss");
        formats.put("\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}", "dd-MM-uuuu HH:mm:ss");
        formats.put("\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}", "uuuu-MM-dd HH:mm:ss");
        formats.put("\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}", "MM/dd/uuuu HH:mm:ss");
        formats.put("\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}", "uuuu/MM/dd HH:mm:ss");
        formats.put("\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}", "dd MMM uuuu HH:mm:ss");
        formats.put("\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}", "dd MMMM uuuu HH:mm:ss");
        formats.put("\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}Z", "uuuu-MM-dd'T'HH:mm:ss'Z'");
        formats.put("\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}\\.\\d{3}Z", "uuuu-MM-dd'T'HH:mm:ss.SSS'Z'");

        new SolrMultiFormatDateParser().setPatterns(formats);
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of parse method, of class SolrMultiFormatDateParser.
     */
    @Test
    public void testParse() {
        ZonedDateTime result = SolrMultiFormatDateParser.parse(toParseDate);
        // Verify that the parsed ZonedDateTime is equal to the expected String result (or null if result is empty)
        if (!expectedResult.isEmpty()) {
            assertEquals("Should parse: " + expectedFormat, expectedResult, result.toString());
        } else {
            assertNull("Should not parse: " + expectedFormat, result);
        }
    }
}
