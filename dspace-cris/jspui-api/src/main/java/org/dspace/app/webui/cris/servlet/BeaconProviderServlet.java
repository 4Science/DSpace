/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.cris.util.BeaconMaker;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;

public class BeaconProviderServlet extends DSpaceServlet {


    private Logger log = Logger.getLogger(BeaconProviderServlet.class);
    private BeaconMaker beaconMaker = BeaconMaker.getInstance();

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        response.setHeader("Content-Type", "text/plain; charset=UTF-8");
        
        try(PrintWriter pw = response.getWriter()) {
           pw.write(beaconMaker.getData(false, false));
           pw.flush();
        }catch(Exception e) {
        	e.printStackTrace();
        	response.sendError(response.SC_INTERNAL_SERVER_ERROR);
        }
    }
	
}
