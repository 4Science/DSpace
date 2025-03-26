/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sort;

import org.apache.commons.lang3.StringUtils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Formatter that adds "0" as left padding if the string is less than MAX_PADDING characters long
 *
 */
public class OrderFormatNumber implements OrderFormatDelegate {

    private ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    @Override
    public String makeSortString(String value, String language) {
        int maxPadding = configurationService.getIntProperty(
            "browse.OrderFormatNumber.padding.max", 7);

        value = StringUtils.strip(value);
        int padding = maxPadding - value.length();

        if (padding > 0) {
            // padding the value from left with 0 so that 87 -> 0087
            return String.format("%1$0" + padding + "d", 0)
                + value;
        } else {
            return value;
        }
    }

}
