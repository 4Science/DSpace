package org.dspace.app.webui.cris.components;

import java.util.List;

import org.dspace.discovery.configuration.DiscoveryViewConfiguration;

public class PathExploreProcessor
{
    private int maxResults = 10;

    private String queryDefault = "*:*";

    private String sortCriteria;

    private String sortOrder = "DESC";

    private List<String> fq;
    
    private DiscoveryViewConfiguration configuration;

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
    
    
}
