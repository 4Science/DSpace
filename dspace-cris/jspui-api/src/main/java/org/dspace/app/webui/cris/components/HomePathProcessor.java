package org.dspace.app.webui.cris.components;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.components.PathEntries;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowserScope;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.discovery.SearchUtils;
import org.dspace.plugin.PluginException;
import org.dspace.plugin.SiteHomeProcessor;
import org.dspace.sort.SortOption;
import org.dspace.utils.DSpace;

public class HomePathProcessor implements SiteHomeProcessor 
{
	private static final Logger log = Logger.getLogger(HomePathProcessor.class);
	
	private static final String BROWSE_PATH_NAME = "pathname";
	private static final int MAX = ConfigurationManager.getIntProperty("path-list.results.show", Integer.MAX_VALUE);
	private static final int SORT_OPTION = 10;

	@Override
	public void process(Context context, HttpServletRequest request, HttpServletResponse response)
			throws PluginException, AuthorizeException
	{
		try
		{
			// prep our engine and scope			
			BrowserScope bs = new BrowserScope(context);
			bs.setUserLocale(context.getCurrentLocale().getLanguage());
			BrowseIndex bi = BrowseIndex.getBrowseIndex(BROWSE_PATH_NAME);
			
            boolean isMultilanguage = new DSpace()
                    .getConfigurationService()
                    .getPropertyAsType(
                            "discovery.browse.authority.multilanguage."
                                    + BROWSE_PATH_NAME,
                            new DSpace()
                                    .getConfigurationService()
                                    .getPropertyAsType(
                                            "discovery.browse.authority.multilanguage",
                                            new Boolean(false)),
                            false);
            
            // gather & add items to the feed.
            BrowseEngine be = new BrowseEngine(context, isMultilanguage? 
                    bs.getUserLocale():null);
			
			// fill in the scope with the relevant gubbins
			bs.setBrowseIndex(bi);
			bs.setOrder(SortOption.ASCENDING);
			bs.setResultsPerPage(MAX);
            bs.setSortBy(SORT_OPTION);
			
			BrowseInfo results = be.browse(bs);
			
			IGlobalSearchResult[] items = results.getBrowseItemResults();
			
			PathEntries result = new PathEntries(items);
			result.setConfiguration(SearchUtils.getRecentSubmissionConfiguration("crispath").getMetadataFields());
			
			request.setAttribute("paths_list", result);
		}
		catch (BrowseException e)
		{
			log.error("caught exception: ", e);
		}
	}
	
}
