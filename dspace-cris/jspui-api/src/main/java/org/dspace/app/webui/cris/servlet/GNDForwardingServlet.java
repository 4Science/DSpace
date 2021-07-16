/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.cris.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.util.QueryBuilder;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowItem;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.WorkspaceItem;

/** 
 * Implementation for Uniba Professors Catalogue
 * 
 * Class to forward to specific Cris Entity using the gnd identifier.
 * 
 * @author florian gantner (florian.gantner@uni-bamberg.de)
 * @version  $Revision$
 */
public class GNDForwardingServlet 
    extends DSpaceServlet 
{

    /** log4j logger */
    private static Logger log = Logger.getLogger(GNDForwardingServlet.class);
	
	DSpace dspace = new DSpace();
	ConfigurationService conf = dspace.getConfigurationService();
    ApplicationService applicationService = dspace.getServiceManager().getServiceByName("applicationService",
            ApplicationService.class);
    SearchService searchService = dspace.getSingletonService(SearchService.class);

    protected void doDSGet(Context c, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // pass all requests to the same place for simplicty
        doDSPost(c, request, response);
    }
    
    protected void doDSPost(Context c, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
    	String path = request.getPathInfo();
    	int lastSlash = path.lastIndexOf('/');
    	
    	//Check Path, if it matches the Identifier Pattern
    	try {
    	path = path.substring(lastSlash + 1);
    	}catch(Exception e) {
        	response.sendError(500);
        	return;
    	}
    	
    	String gndid = path;
    	if(!gndid.matches(conf.getPropertyAsType("gnd.redirect.regex", "[0-9X-]{6,11}"))){
        	response.addHeader("message", "invalid Pattern of identifier");
    		response.sendError(404);
        	return;
    	}
    	gndid = conf.getPropertyAsType("gnd.redirect.path", "http://d-nb.info/gnd/") + gndid;
    	
    	    	
    	//Check Solr-Queryfields to perform lookup on
    	String queryfields = "";
    	
    	String[] fields = conf.getProperty("gnd.redirect.fields").split(",");
    	if(fields == null) {
    		log.info("No fields for GNDForwardingServlet configured");
        	response.sendError(500);
        	return;
    	}
    	for(int i  =0; i < fields.length; i++) {
    		queryfields += fields[i].trim() + ":\""+gndid+ "\"";
    		if(i != fields.length-1) {
    			queryfields += " OR ";
    		}
    			
    	}
    	
        SolrQuery query = new SolrQuery(queryfields);
        query.setRows(Integer.MAX_VALUE);
        
        ACrisObject aco = null;

        try {
            QueryResponse qresponse = searchService.search(query);
            SolrDocumentList docList = qresponse.getResults();
            Iterator<SolrDocument> solrDoc = docList.iterator();
            while (solrDoc.hasNext()) {
            	//resolve found Solr-Doc Entities to Cris-Objects
               SolrDocument doc = solrDoc.next();
               try {
               String crisID = (String) doc.getFirstValue("cris-id");
               
               aco = applicationService.getEntityByCrisId(crisID);
               if(aco != null) {
            	   //if multiple entries exist, take the first one
            	   break;
               }
               }catch(Exception e) {
            	   log.error(e);
               }
            }
            if(aco == null) {
            	response.sendError(404);
            	return;
            }
            	try {
            	//Redirect to Cris-Entitiy by crisid
            	String location = request.getContextPath() + "/cris/" + aco.getPublicPath() +"/"+ aco.getCrisID();
            	response.sendRedirect(location);
                return;
            	}catch(Exception e) {
            	log.error(e);	
            	throw e;
            }
            
        } catch (SearchServiceException e) {
            log.error(e);
        	response.sendError(404);
        	return;
        } catch(Exception e) {
        	log.error(e);
        	response.sendError(404);
        	return;
        }
    }
}
