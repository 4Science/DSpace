/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.cris.rdf.storage;

import java.sql.SQLException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.rdf.RDFConfiguration;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Site;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.app.cris.rdf.storage.LocalURIGenerator;
import org.dspace.app.cris.rdf.storage.URIGenerator;
import org.dspace.utils.DSpace;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class LocalURIGenerator implements URIGenerator {
    private static final Logger log = Logger.getLogger(LocalURIGenerator.class);

    @Override
    public String generateIdentifier(Context context, int type, int id, 
            String handle, String[] identifiers)
            throws SQLException
    {
        String urlPrefix = RDFConfiguration.getDSpaceRDFModuleURI() + "/resource/";
        
        if (type == Constants.SITE)
        {
            return urlPrefix + Site.getSiteHandle();
        }
        
        if (type == Constants.COMMUNITY 
                || type == Constants.COLLECTION 
                || type == Constants.ITEM)
        {
            if (StringUtils.isEmpty(handle))
            {
                throw new IllegalArgumentException("Handle is null");
            }
            return urlPrefix + handle;
        }
        
        return null;
    }

    @Override
    public String generateIdentifier(Context context, DSpaceObject dso) throws SQLException {
    	if(dso instanceof ACrisObject) {
    	ACrisObject aco = (ACrisObject) dso;
    	//Adapt here for URI-Generation
    	return RDFConfiguration.getDSpaceRDFModuleURI() + "/" + aco.getAuthorityPrefix() + "/" + aco.getCrisID() ;
    	//here, "self-speking contains" can be added to the URL, e.g. researcher instead of rp
    	
    	//if(aco.getAuthorityPrefix().equals("rp")) {
    		//return RDFConfiguration.getDSpaceRDFModuleURI() + "/" + aco.getCrisID() ;
    		//return generateIdentifier(context, 0, 0, dso.getHandle(), stringArray);
    		
    	    //}
    	//define own researchObject Prefixes
    	//if(aco.getAuthorityPrefix().equals("bsp")) {
    		//return RDFConfiguration.getDSpaceRDFModuleURI() + "/" + aco.getCrisID();
    		//return generateIdentifier(context, 0, 0, dso.getHandle(), stringArray);
    		
    	    //}    	
    	}
    	
    	if (dso.getType() != Constants.SITE
                && dso.getType() != Constants.COMMUNITY
                && dso.getType() != Constants.COLLECTION
                && dso.getType() != Constants.ITEM)
        {
            return null;
        }
        
        return generateIdentifier(context, dso.getType(), dso.getID(), dso.getHandle(), dso.getIdentifiers(context));
    }

}
