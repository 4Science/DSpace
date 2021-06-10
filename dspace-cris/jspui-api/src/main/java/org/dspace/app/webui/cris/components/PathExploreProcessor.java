package org.dspace.app.webui.cris.components;

import java.util.List;

import org.dspace.app.cris.service.ApplicationService;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.configuration.DiscoveryViewConfiguration;

public class PathExploreProcessor
{
    private int maxResults = Integer.MAX_VALUE;
    
    private int carouselMax = 12;

    private String queryDefault = "*:*";

    private String sortCriteria;

    private String sortOrder = "ASC";

    private List<String> fq;
    
    private DiscoveryViewConfiguration configuration;

    private ApplicationService applicationService;
    
    private SearchService searchService;
    
    public int getMaxResults()
    {
        return maxResults;
    }

    public void setMaxResults(int maxResults)
    {
        this.maxResults = maxResults;
    }

    public String getQueryDefault()
    {
        return queryDefault;
    }

    public void setQueryDefault(String queryDefault)
    {
        this.queryDefault = queryDefault;
    }

    public String getSortCriteria()
    {
        return sortCriteria;
    }

    public void setSortCriteria(String sortCriteria)
    {
        this.sortCriteria = sortCriteria;
    }

    public String getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public List<String> getFq()
    {
        return fq;
    }

    public void setFq(List<String> fq)
    {
        this.fq = fq;
    }

    public DiscoveryViewConfiguration getConfiguration()
    {
        return configuration;
    }

    public void setConfiguration(DiscoveryViewConfiguration configuration)
    {
        this.configuration = configuration;
    }

	public ApplicationService getApplicationService() {
		return applicationService;
	}

	public void setApplicationService(ApplicationService applicationService) {
		this.applicationService = applicationService;
	}

	public SearchService getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public int getCarouselMax() {
		return carouselMax;
	}

	public void setCarouselMax(int carouselMax) {
		this.carouselMax = carouselMax;
	}
}
