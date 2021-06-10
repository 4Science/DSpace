package org.dspace.app.webui.cris.components;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.components.PathEntries;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseDSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.plugin.PluginException;
import org.dspace.plugin.SiteHomeProcessor;
import org.dspace.utils.DSpace;

public class HomePathProcessor implements SiteHomeProcessor 
{
	private static final Logger log = Logger.getLogger(HomePathProcessor.class);
	
	@Override
	public void process(Context context, HttpServletRequest httprequest, HttpServletResponse httpresponse)
			throws PluginException, AuthorizeException
	{
		try
		{
		    PathExploreProcessor processorsMap = new DSpace().getSingletonService(PathExploreProcessor.class);
		    
		    String queryDefault = processorsMap.getQueryDefault();
		    String sortOrder = processorsMap.getSortOrder();
		    String sortCriteria = processorsMap.getSortCriteria();
		    int maxResults = processorsMap.getMaxResults();
		    List<String> fq = processorsMap.getFq();
		    
	        SolrQuery query = new SolrQuery();
	        query.setQuery(queryDefault);

	        if (sortOrder == null || "ASC".equals(sortOrder))
	        {
	        	query.setSort(sortCriteria, ORDER.asc);
	        }
	        else
	        {
	        	query.setSort(sortCriteria, ORDER.desc);
	        }

	        if (fq != null)
	        {
	            for (String f : fq)
	            {
	                query.addFilterQuery(f);
	            }
	        }	        
	        query.setFields("search.resourceid");
	        query.setRows(maxResults);
	        
	        QueryResponse response = processorsMap.getSearchService().search(query);
	        SolrDocumentList results = response.getResults();
	        List<IGlobalSearchResult> items = new ArrayList<IGlobalSearchResult>();
	        for (SolrDocument doc : results)
	        {
	            Integer resourceId = (Integer) doc
	                    .getFirstValue("search.resourceid");
	            
	            ResearchObject item = processorsMap.getApplicationService().get(ResearchObject.class, resourceId);
	            if (item != null)
	            {
	                items.add(new BrowseDSpaceObject(context, item));
	            }
	            else
	            {
	                log.warn("A DELETED OBJECT IS IN SOLR INDEX? identifier:"
	                        + resourceId);
	            }
	            
	        }
		    
			
			PathEntries result = new PathEntries(items);
			result.setConfiguration(processorsMap.getConfiguration());
			
			httprequest.setAttribute("paths_list", result);
			
			httprequest.setAttribute("paths_list_max", processorsMap.getCarouselMax());
		}
		catch (SearchServiceException e)
		{
			log.error("caught exception: ", e);
		}
	}
}
