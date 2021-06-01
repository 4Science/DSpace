package org.dspace.app.webui.discovery;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.webui.util.CarouselNewsObject;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.plugin.PluginException;
import org.dspace.plugin.SiteHomeProcessor;
import org.dspace.utils.DSpace;

import it.cilea.osd.jdyna.web.tag.JDynATagLibraryFunctions;

public class HomeCarouselProcessor implements SiteHomeProcessor 
{
	Logger log = Logger.getLogger(getClass());


	public static final String SOLR_NEWS_LINK = "crisnews.newslink";
	public static final String SOLR_NEWS_TEXT = "crisnews.newstext";
	public static final String SOLR_NEWS_NAME = "crisnews.newsname";
	public static final String SOLR_NEWS_IMAGE = "crisnews.newsimage";
	private static final String EMPTY = "";
	private static final String SOLR_NEWS_QUERY = "crisnews.newsshow:true";
	private static SortClause SORT_CLAUSE;
	
	SearchService searcher = new DSpace().getSingletonService(SearchService.class);
	
	public HomeCarouselProcessor() 
	{
		String field = ConfigurationManager.getProperty("carousel.sort.field");
		String order = ConfigurationManager.getProperty("carousel.sort.order");
		
		SORT_CLAUSE = new SortClause( 
				StringUtils.isNotBlank(field) ? field : "crisnews.time_creation_dt",
				StringUtils.isNotBlank(order) ? ORDER.valueOf(order) : ORDER.asc);
	}
	
	@Override
	public void process(Context context, HttpServletRequest request, HttpServletResponse response)
			throws PluginException, AuthorizeException 
	{
		
		List<CarouselNewsObject> news = new ArrayList<>();
		
		SolrQuery sq = new SolrQuery(SOLR_NEWS_QUERY);
		sq.setRows(Integer.MAX_VALUE);
		sq.addField(SOLR_NEWS_LINK);
		sq.addField(SOLR_NEWS_TEXT);
		sq.addField(SOLR_NEWS_NAME);
		sq.addField(SOLR_NEWS_IMAGE);
		sq.addSort(SORT_CLAUSE);
		
		QueryResponse qResp;
        try {
            qResp = searcher.search(sq);
            if (qResp.getResults() != null && qResp.getResults().size() > 0) 
            {
                for (SolrDocument sd : qResp.getResults()) 
                {
                	CarouselNewsObject cno = new CarouselNewsObject();
                	
                	cno.setLink(sd.containsKey(SOLR_NEWS_LINK)   ? (String)sd.getFirstValue(SOLR_NEWS_LINK) : null);
            		cno.setName(sd.containsKey(SOLR_NEWS_NAME)   ? (String)sd.getFirstValue(SOLR_NEWS_NAME) : EMPTY);
            		cno.setText(sd.containsKey(SOLR_NEWS_TEXT)   ? (String)sd.getFirstValue(SOLR_NEWS_TEXT) : null);
            		cno.setImage(sd.containsKey(SOLR_NEWS_IMAGE) ? getImageLink((String)sd.getFirstValue(SOLR_NEWS_IMAGE), request.getContextPath()) : EMPTY);
            		
					news.add(cno);
				}
            }
        } catch (SearchServiceException e) {
            log.error(e);
        }
        
        if (!news.isEmpty()) 
        {
        	request.setAttribute("carousel_news", news);
		}
		
	}

	/**
	 * get the image's link from the string
	 * 
	 * @param aco
	 * @param metadata
	 * @param contextPath
	 * @return
	 */
	public static String getImageLink(String link, String contextPath)
	{
		return contextPath
					+ "/cris/do/fileservice/"
					+ JDynATagLibraryFunctions.getFileFolder(link)
					+ "?filename="
					+ JDynATagLibraryFunctions.getFileName(link);
	}

}
