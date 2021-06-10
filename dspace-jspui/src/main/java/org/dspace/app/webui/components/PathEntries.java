package org.dspace.app.webui.components;

import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.discovery.configuration.DiscoveryViewConfiguration;

public class PathEntries
{
	
	private IGlobalSearchResult[] paths;
	private DiscoveryViewConfiguration configuration;
	
	/**
	 * Construct a new PathEntries object to represent the passed
	 * array of paths
	 * 
	 * @param items
	 */
	public PathEntries(List<IGlobalSearchResult> paths)
	{
		this.paths = paths.toArray(new IGlobalSearchResult[paths.size()]);
	}

	/**
	 * obtain the number of paths available
	 * 
	 * @return	the number of paths
	 */
	public int count()
	{
		if (paths == null) {
			return 0;
		}
		return paths.length;
	}
	
	/**
	 * Obtain the array of paths
	 * 
	 * @return	an array of paths
	 */
	public IGlobalSearchResult[] getPaths()
	{
		return (IGlobalSearchResult[])ArrayUtils.clone(paths);
	}
	
	/**
	 * Get the item which is in the i'th position
	 * 
	 * @param i		the position of the paths to retrieve
	 * @return		the paths
	 */
	public IGlobalSearchResult getPath(int i)
	{
		if (i < paths.length)
		{
			return paths[i];
		}
		else
		{
			return null;
		}
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
