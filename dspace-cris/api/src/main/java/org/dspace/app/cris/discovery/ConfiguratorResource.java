package org.dspace.app.cris.discovery;

import java.util.List;
import java.util.Map;

import org.dspace.app.cris.configuration.RelationConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;

public class ConfiguratorResource
{
    private Map<String, List<ConfiguratorProfile>> profile;
    
    private Map<String, Boolean> enabled;

    private Map<String, RelationConfiguration> relation;

    private Map<String, List<DiscoverySortFieldConfiguration>> sort;

    private Map<String, String> sortOrder;

    public Map<String, Boolean> getEnabled()
    {
        return enabled;
    }

    public void setEnabled(Map<String, Boolean> enabled)
    {
        this.enabled = enabled;
    }

    public Map<String, List<ConfiguratorProfile>> getProfile()
    {
        return profile;
    }

    public void setProfile(Map<String, List<ConfiguratorProfile>> profile)
    {
        this.profile = profile;
    }

    public Map<String, RelationConfiguration> getRelation()
    {
        return relation;
    }

    public void setRelation(Map<String, RelationConfiguration> relation)
    {
        this.relation = relation;
    }

    public Map<String, List<DiscoverySortFieldConfiguration>> getSort()
    {
        return sort;
    }

    public void setSort(Map<String, List<DiscoverySortFieldConfiguration>> sort)
    {
        this.sort = sort;
    }

    public Map<String, String> getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Map<String, String> sortOrder)
    {
        this.sortOrder = sortOrder;
    }

}
