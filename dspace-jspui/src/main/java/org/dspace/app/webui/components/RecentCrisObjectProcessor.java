/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.components;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.discovery.SearchUtils;
import org.dspace.plugin.SiteHomeProcessor;
import org.dspace.utils.DSpace;
import org.dspace.plugin.PluginException;

/**
 * This class obtains recent submissions to the site by
 * implementing the SiteHomeProcessor.
 * 
 * @author Keiji Suzuki 
 *
 */
public class RecentCrisObjectProcessor implements SiteHomeProcessor
{
	private RecentCrisObjectProcessorConfiguration recentCrisObjectProcessorConfiguration = new DSpace().getServiceManager()
			.getServiceByName("recentCrisObjectProcessorConfiguration", RecentCrisObjectProcessorConfiguration.class);
	
    /**
     * blank constructor - does nothing.
     *
     */
    public RecentCrisObjectProcessor()
    {
        
    }
    
    @Override
    public void process(Context context, HttpServletRequest request, HttpServletResponse response) 
        throws PluginException, AuthorizeException
    {
        try
        {
            RecentSubmissionsManager rsm = new RecentSubmissionsManager(context);
            String indexName = recentCrisObjectProcessorConfiguration.getBrowseIndex();
            if(StringUtils.isNotBlank(indexName)) {            
                rsm.setIndexName(indexName);
            }
            
            RecentSubmissions recent = rsm.getRecentSubmissions(null,
            		recentCrisObjectProcessorConfiguration.getSortOption(),
            		recentCrisObjectProcessorConfiguration.getCount());

            String discoveryConfiguration = recentCrisObjectProcessorConfiguration.getDiscoveryConfiguration();
            recent.setConfiguration(SearchUtils.getRecentSubmissionConfiguration(discoveryConfiguration).getMetadataFields());
            
            request.setAttribute("recent.submissions", recent);
            request.setAttribute("recent.link", discoveryConfiguration);
        }
        catch (RecentSubmissionsException e)
        {
            throw new PluginException(e);
        }
    }
}
