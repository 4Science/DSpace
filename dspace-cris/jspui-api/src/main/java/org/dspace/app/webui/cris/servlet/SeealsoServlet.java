/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.servlet.DSpaceServlet;

import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.springframework.web.util.HtmlUtils;

/**
 * Servlet implementation class
 * This Servlet lookup Information for the Referenced gnd identifier from SeeAlso-Providers and prints them as tabular data
 * @author Florian Gantner(florian.gantner@uni-bamberg.de)
 */
public class SeealsoServlet extends DSpaceServlet {
	
	DSpace dspace = new DSpace();
	ConfigurationService conf = dspace.getConfigurationService();
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		/**
		 * Make Request to SeeAlso Provider and generate html table from there
		 * */
		PrintWriter pw = response.getWriter();
		try {
		String id = request.getParameter("gnd");
		if(id == null || id.isEmpty()) { 
			response.setStatus(500);
			return;
		}
		
		//using configuration from beacon.cfg
		String conflink = conf.getProperty("beacon.seealso.link");
		String boxtitle = "";
		if(request.getParameter("boxtitle") != null) {
		boxtitle = (String) request.getParameter("boxtitle");
		String boxurl = conf.getProperty("beacon.seealso."+boxtitle+".link");
			if(boxurl != null && !boxurl.isEmpty()) {
				conflink = boxurl;
			}
		}
			
		conflink = conflink.replace("{{ID}}", id);
		String recv = "";
		String recvbuff = "";
		
		URL jsonpage = new URL(conflink);
		   URLConnection urlcon = jsonpage.openConnection();
		   BufferedReader buffread = new BufferedReader(new InputStreamReader(urlcon.getInputStream()));

		   while ((recv = buffread.readLine()) != null)
		    recvbuff += recv;
		   buffread.close();

			List<String> no1 = (List<String>) new ArrayList<String>(); //Label
			List<String> no2 = (List<String>) new ArrayList<String>(); //Description
			List<String> no3 = (List<String>) new ArrayList<String>(); //URIs
			//Link
			if(Pattern.compile("^\\[http:\\/\\/d\\-nb\\.info\\/gnd\\/\\"+id+"]").matcher(recvbuff).find()) {
				 //Check Format and Response
				 response.setStatus(500);
			 }else {
				//correct separation of result ["<GND>",[""],[""],[""]] into the three lists
				 recvbuff = recvbuff.substring(1, recvbuff.length() - 1 );
				 String[] recvbuff2 = recvbuff.split(",",2); //ignore first part which has been checked before
				 recvbuff = recvbuff2[1];
			int cnt = 0; // 
			int openBr = 0; //Number of Brackets opened
			int Br = 0; //total brackets
			int openBrIdX = 0; // Index of last Bracket opening
			for (int i = 0; i < recvbuff.length(); i++){
			    char c = recvbuff.charAt(i);
			    if(c == ',') continue;
			    if(c == '[') {
			    if(openBrIdX == 0 && openBr == 0) {
			    	openBrIdX = i;
			    }
			    	openBr++;
			    	Br++;
			    }else if(c == ']') {
			    	openBr--;
			    	Br++;
			    }
			    if((openBr == 0 && Br > 0)) {
			    	cnt++;
			    	String res = recvbuff.substring(openBrIdX+1, i);
			    	openBrIdX = 0;
			    	//String entry[] = res.split(",");
			    	//Parse String "","","", - Consider commas inside
			    	
			    	String[] tokens = res.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
			        	if(cnt == 1) {
					    	 no1.addAll(Arrays.asList(tokens));
					     }else if(cnt == 2) {
					    	 no2.addAll(Arrays.asList(tokens));
					     }else if(cnt == 3) {
					    	 no3.addAll(Arrays.asList(tokens));
					     }	
			    	}
			}
			 
			 if(no1.isEmpty() || no1.size() == 0 || no1.size() != no2.size() ||no2.size() != no3.size()) {
		            	//if any list is empty or list sizes differ
		            	response.setStatus(400);
		     }else {
		    	 		String resource = "http://d-nb.info/gnd/"+id;
		            	pw.write(printResult(no1, no2, no3, boxtitle, resource));
		                response.setStatus(200);
		     }
		}
		}catch(Exception e) {
			response.setStatus(500);
		}
        response.setHeader("Content-Type", "text/html; charset=UTF-8");
        pw.flush();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		doGet(request, response);
	}
	
	/**
	 * Print the Lists in a tabular
	 * */
	private String printResult(List<String> no1, List<String> no2, List<String> no3, String box, String resource) {
		StringBuilder sb = new StringBuilder();
		sb.append("<table id=\"seealsoTable"+box+"\" class=\"dynaFieldComboValue\"><thead>");
		sb.append("<tr><th id=\"table-name"+box+"\">Link</th></tr></thead><tbody>");
		for(int it = 0; it < no1.size(); it++) {
			String label = no1.get(it).trim();
			label = label.substring(1, label.length() -1);
			label = HtmlUtils.htmlEscape(label);
			String desc = no2.get(it).trim();
			desc = desc.substring(1, desc.length() -1);
			desc = HtmlUtils.htmlEscape(desc);
			String link = no3.get(it).trim();
			link = link.substring(1, link.length() -1);
			link = HtmlUtils.htmlEscape(link);
			sb.append("<tr>");
			sb.append("<td>");
			sb.append("<span data-placement=\"top\" data-original-title=\""+desc+"\" data-toggle=\"tooltip\">");
			sb.append("<a property=\"http://www.w3.org/2000/01/rdf-schema#seeAlso\" target=\"blank\"  title=\""+label+"\" href=\""+link+"\">");
			sb.append(label);
			sb.append("</a</span>");
			sb.append("</td>");
			sb.append("</tr>");
			
		}
		sb.append("</tbody></table>");
		return sb.toString();
	}
}
