/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Context;

/**
 * Common constants and static methods for working with Solr.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class SolrUtils {
    /** Solr uses UTC always. */
    public static final TimeZone SOLR_TIME_ZONE = TimeZone.getTimeZone(ZoneOffset.UTC);

    /** Restricted ISO 8601 format used by Solr. */
    public static final String SOLR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final String DATE_FORMAT_DCDATE = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /** Do not instantiate. */
    private SolrUtils() {
    }

    /**
     * Create a formatter configured for Solr-style date strings and the UTC time
     * zone.
     * 
     * @see SOLR_DATE_FORMAT
     *
     * @return date formatter compatible with Solr.
     */
    public static DateFormat getDateFormatter() {
        DateFormat formatter = new SimpleDateFormat(SolrUtils.SOLR_DATE_FORMAT);
        formatter.setTimeZone(SOLR_TIME_ZONE);
        return formatter;
    }

    /**
     * Maps target type into a string format.
     * 
     * @param type a {@code String} that represents the date format style
     * @return {@code String} pattern for that type
     */
    public static String getDateformatFrom(String type) {
        String dateformatString = null;
        if ("DAY".equals(type)) {
            dateformatString = "dd-MM-yyyy";
        } else if ("MONTH".equals(type)) {
            dateformatString = "MMMM yyyy";
        } else if ("YEAR".equals(type)) {
            dateformatString = "yyyy";
        }
        return dateformatString;
    }

    /**
     * This method tries to convert a target string into a Date using the possible
     * SOLR date format used in DSpace.
     * 
     * @param context    The Dspace context
     * @param dateString The date formatted as a string
     * @return {@code Date} parsed date
     */
    public static Date parseSolrDate(Context context, String dateString) {
        Date date = null;
        if (!StringUtils.isBlank(dateString) && dateString.matches("^[0-9]{4}\\-[0-9]{2}.*")) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(SOLR_DATE_FORMAT, context.getCurrentLocale());
                date = format.parse(dateString);
            } catch (ParseException e) {
                try {
                    // We should use the dcdate (the dcdate is used when generating random data)
                    SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT_DCDATE, context.getCurrentLocale());
                    date = format.parse(dateString);
                } catch (ParseException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return date;
    }
}
