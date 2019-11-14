package org.dspace.app.cris.importexport;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcelBulkFieldValueTest {

	public static String REGEX_VALUE_AND_VISIBILITY = "\\[.*visibility=([\\w]+)[^\\]]*\\](.*)";
	private static Pattern pattern = Pattern.compile(REGEX_VALUE_AND_VISIBILITY, Pattern.DOTALL);

	public static void main(String[] args) {
		String[] vals = new String[] { "[visibility=PUBLIC]Lorem ipsum dolor sit amet [consectetur adipiscing]",
				"[visibility=PUBLIC]Lorem ipsum dolor sit amet [consectetur adipiscing] Lorem ipsum dolor sit amet,\nconsectetur adipiscing elit,\nsed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
				"[visibility=PUBLIC URL=http://www.personalpage.com /~mypage/]Personal Page",
				"[visibility=HIDE CRISID=rp00001 SOURCEID=asdfasdfa.eee SOURCEREF=asdf.aeraser UUID=12314-1231-322-11]<Lorem ipsum dolor sit amet,\n\rconsectetur adipiscing elit.>" };

		for (String val : vals) {
			System.out.println(val + "--->");
			Matcher tagmatch = pattern.matcher(val);
			if (tagmatch.find()) {
				System.out.println("value=" + tagmatch.group(2));
				System.out.println("visibility=" + tagmatch.group(1));
			} else {
				System.out.println(val);
			}
			System.out.println();
		}
	}

}
