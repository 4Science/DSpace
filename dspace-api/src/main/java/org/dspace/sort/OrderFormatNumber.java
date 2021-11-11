package org.dspace.sort;

import org.apache.commons.lang3.StringUtils;
import org.dspace.core.ConfigurationManager;

/**
 * Formatter that adds "0" as left padding if the string is less than MAX_PADDING characters long
 *
 */
public class OrderFormatNumber implements OrderFormatDelegate {

	private static final int MAX_PADDING = ConfigurationManager.getIntProperty("browse.OrderFormatNumber.padding.max", 7);
	
	@Override
	public String makeSortString(String value, String language)
	{
		value = StringUtils.strip(value);
		int padding = MAX_PADDING - value.length();
		
		if (padding > 0)
        {
            // padding the value from left with 0 so that 87 -> 0087
            return String.format("%1$0" + padding + "d", 0)
                    + value;
        }
        else
        {
            return value;
        }
	}

}
