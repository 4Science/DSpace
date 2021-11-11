package org.dspace.sort;

/**
 * Formatter that adds "0" as left padding if the string is less than 7 characters long
 *
 */
public class OrderFormatNumber implements OrderFormatDelegate {

	@Override
	public String makeSortString(String value, String language)
	{
		int padding = 7;
		
		padding -= value.length();
		
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
