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
import org.dspace.app.cris.metrics.common.model.ConstantMetrics;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.components.MostViewedItem;
import org.dspace.app.webui.components.PathEntries;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.plugin.PluginException;
import org.dspace.plugin.SiteHomeProcessor;
import org.dspace.utils.DSpace;

public class HomePathProcessor implements SiteHomeProcessor 
{
	private static final Logger log = Logger.getLogger(HomePathProcessor.class);
	
    private SearchService searchService;
    
    private ApplicationService applicationService;
	
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

	        if (sortOrder == null || "DESC".equals(sortOrder))
	        {
	            query.setSort(ConstantMetrics.PREFIX_FIELD + sortCriteria,
	                    ORDER.desc);
	        }
	        else
	        {
	            query.setSort(ConstantMetrics.PREFIX_FIELD + sortCriteria,
	                    ORDER.asc);
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
	        
	        QueryResponse response = searchService.search(query);
	        SolrDocumentList results = response.getResults();
	        List<IGlobalSearchResult> items = new ArrayList<IGlobalSearchResult>();
	        for (SolrDocument doc : results)
	        {
	            Integer resourceId = (Integer) doc
	                    .getFirstValue("search.resourceid");
	            
	            ResearchObject item = applicationService.get(ResearchObject.class, resourceId);
	            if (item != null)
	            {
	                items.add(item);
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
		}
		catch (SearchServiceException e)
		{
			log.error("caught exception: ", e);
		}
	}

    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }
	
}
