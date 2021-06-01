package org.dspace.app.webui.cris.components;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.webui.discovery.HomeCarouselProcessor;
import org.dspace.app.webui.util.PathEntryObject;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.plugin.PluginException;
import org.dspace.plugin.SiteHomeProcessor;
import org.dspace.utils.DSpace;

public class HomePathProcessor implements SiteHomeProcessor 
{
	private static final Logger log = Logger.getLogger(HomePathProcessor.class);
	
	public static final String SOLR_PATH_TEXT = "crispath.pathname";
	public static final String SOLR_PATH_ID = "cris-id";
	public static final String SOLR_PATH_IMAGE = "crispath.pathpicture";
	private static final String SOLR_PATH_QUERY = "crisdo.type:path";
	private static SortClause SORT_CLAUSE = new SortClause("pathindex_sort", ORDER.asc);
	
	private SearchService searcher = new DSpace().getSingletonService(SearchService.class);

	@Override
	public void process(Context context, HttpServletRequest request, HttpServletResponse response)
			throws PluginException, AuthorizeException
	{
		
		List<PathEntryObject> paths = new ArrayList<>();
		
		SolrQuery sq = new SolrQuery(SOLR_PATH_QUERY);
		sq.setRows(Integer.MAX_VALUE);
		sq.addField(SOLR_PATH_TEXT);
		sq.addField(SOLR_PATH_ID);
		sq.addField(SOLR_PATH_IMAGE);
		sq.addSort(SORT_CLAUSE);
		
		QueryResponse qResp;
        try {
            qResp = searcher.search(sq);
            if (qResp.getResults() != null && qResp.getResults().size() > 0) 
            {
                for (SolrDocument sd : qResp.getResults()) 
                {
                	PathEntryObject peo = new PathEntryObject();
                	
            		peo.setUrl( getUrl(request, (String)sd.getFirstValue(SOLR_PATH_ID)));
            		peo.setText(sd.containsKey(SOLR_PATH_TEXT)   ? (String)sd.getFirstValue(SOLR_PATH_TEXT) : null);
            		peo.setImage(sd.containsKey(SOLR_PATH_IMAGE) ? HomeCarouselProcessor.getImageLink((String)sd.getFirstValue(SOLR_PATH_IMAGE), request.getContextPath()) : null);
            		
					paths.add(peo);
				}
            }
        } catch (SearchServiceException e) {
            log.error(e);
        }
        
        if (!paths.isEmpty()) 
        {
        	request.setAttribute("paths_list", paths);
		}
		
	}
	
	private String getUrl(HttpServletRequest request, String crisID)
	{
		return request.getContextPath()
				+ "/cris/path/"
				+ crisID;
	}
	
}
