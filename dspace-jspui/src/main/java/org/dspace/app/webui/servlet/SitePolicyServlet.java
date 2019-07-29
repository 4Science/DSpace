/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Servlet that sets site_policy_accepted when a user accepts the site policy
 * so it doesn't show up again.
 * 
 * @author Cornelius Matějka <cornelius.matejka@uni-bamberg.de>
 */
public class SitePolicyServlet extends DSpaceServlet
{
    /** log4j logger */
	private static Logger log = Logger.getLogger(SitePolicyServlet.class);
	private String sitePolicyURL = ConfigurationManager.getProperty("dspace.site.policy.url");
	private String sitePolicy = ""; 
	
	public SitePolicyServlet() throws FileNotFoundException, IOException {
		super();
		sitePolicy = StringUtils.isEmpty(sitePolicyURL) ? getSitePolicyFromFile() : "";
	}

    @Override
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        EPerson currentUser = context.getCurrentUser();
        boolean policyReadChecked = Boolean.parseBoolean(request.getParameter("policyReadChecked"));
        currentUser.setSitePolicyAccepted(policyReadChecked);
        currentUser.update();
        context.complete();
    }

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
    	/*
    	 * If the login was triggered by a request, which requires authentication,
    	 * this request is flagged as interrupted. In some circumstances, when the
    	 * policy was not already accepted, the type of the AJAX request, which is
    	 * POST, is overwritten by the original request. If the original request's
    	 * type is GET, this servlet is not able to process the request an will fail.
    	 * Therefore we use this nasty workaround and check whether 'policyReadChecked'
    	 * is set or not and call the proper method.
    	 */
    	if(request.getParameter("policyReadChecked") != null) {
    		doDSPost(context, request, response);
    	} else if (StringUtils.isNotEmpty(sitePolicyURL)){
    		response.sendRedirect(sitePolicyURL);
    	} else {
    		response.getWriter().write(sitePolicy);
    	}
    }
    
    private String getSitePolicyFromFile() throws FileNotFoundException, IOException {
    	StringBuilder sb = new StringBuilder();
    	try(BufferedReader br = new BufferedReader(new FileReader(Paths.get(ConfigurationManager.getProperty("dspace.dir"), "config/site-policy.html").toFile()))) {
    		String line;
    		while((line = br.readLine()) != null) {
    			sb.append(line);
    		}
    	}
    	return sb.toString();
    	
    }
    
}
