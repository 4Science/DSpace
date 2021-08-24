package org.dspace.app.cris.discovery.tree;

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.configuration.RelationConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;

public class TreeViewConfigurator
{
    private Map<String, List<TreeViewResourceConfigurator>> profile;
    
    private Map<String, Boolean> enabled;

    private Map<String, List<RelationConfiguration>> relations;
    
    private Map<String, Boolean> showRelationOnLeaf;
    
    private Map<String, Boolean> showRelationCount;
    
    private Map<String, String> icons;
    
    private Map<String, List<String>> parents;
    
    private Map<String, List<String>> leafs;
    
    private Map<String, Map<String, String>> closed;
    
    private Map<String, List<DiscoverySortFieldConfiguration>> sort;

    private Map<String, String> sortOrder;
    
    private List<String> leafTitleMetadata;
    
    private Map<String, Integer> displayDepth;
    
    public Map<String, Boolean> getEnabled()
    {
        return enabled;
    }

    public void setEnabled(Map<String, Boolean> enabled)
    {
        this.enabled = enabled;
    }

    public Map<String, List<TreeViewResourceConfigurator>> getProfile()
    {
        return profile;
    }

    public void setProfile(Map<String, List<TreeViewResourceConfigurator>> profile)
    {
        this.profile = profile;
    }

    public Map<String, List<String>> getParents()
    {
        return parents;
    }

    public void setParents(Map<String, List<String>> parents)
    {
        this.parents = parents;
    }

    public Map<String, List<String>> getLeafs()
    {
        return leafs;
    }

    public void setLeafs(Map<String, List<String>> leafs)
    {
        this.leafs = leafs;
    }

    public Map<String, Boolean> getShowRelationOnLeaf()
    {
        return showRelationOnLeaf;
    }

    public void setShowRelationOnLeaf(Map<String, Boolean> showRelation)
    {
        this.showRelationOnLeaf = showRelation;
    }

    public Map<String, String> getIcons()
    {
        return icons;
    }

    public void setIcons(Map<String, String> icons)
    {
        this.icons = icons;
    }

    public Map<String, List<RelationConfiguration>> getRelations()
    {
        return relations;
    }

    public void setRelations(Map<String, List<RelationConfiguration>> relations)
    {
        this.relations = relations;
    }

    public Map<String, Boolean> getShowRelationCount()
    {
        return showRelationCount;
    }

    public void setShowRelationCount(Map<String, Boolean> showRelationCount)
    {
        this.showRelationCount = showRelationCount;
    }

    public Map<String, Map<String, String>> getClosed()
    {
        return closed;
    }

    public void setClosed(Map<String, Map<String, String>> closed)
    {
        this.closed = closed;
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

	public List<String> getLeafTitleMetadata()
	{
		return leafTitleMetadata;
	}

	public void setLeafTitleMetadata(List<String> leafTitleMetadata)
	{
		this.leafTitleMetadata = leafTitleMetadata;
	}
	
	public String getLeafLabel(SolrDocument docItem)
	{
		String result = (String) docItem.getFirstValue(this.leafTitleMetadata.get(0));
		if (result == null) {
			return "";
		}
		return result;
	}

	public Map<String, Integer> getDisplayDepth() {
		return displayDepth;
	}

	public void setDisplayDepth(Map<String, Integer> displayDepth) {
		this.displayDepth = displayDepth;
	}

}
