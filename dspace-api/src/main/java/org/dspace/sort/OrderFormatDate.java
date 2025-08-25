/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sort;

import java.time.LocalDate;
import java.time.ZoneId;

import org.dspace.util.MultiFormatDateParser;

/**
 * Standard date ordering delegate implementation using date format
 * parsing from o.d.u.MultiFormatDateParser.
 *
 * @author Andrea Bollini
 * @author Alan Orth
 */
public class OrderFormatDate implements OrderFormatDelegate {
    @Override
    public String makeSortString(String value, String language) {
        LocalDate localDate = MultiFormatDateParser.parse(value);
        if (localDate == null) {
            return null;
        }
        return localDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toString();
    }
}
