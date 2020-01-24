/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.components;

public class RecentCrisObjectProcessorConfiguration {

	private String browseIndex;
	private String discoveryConfiguration = "site";
	private String sortOption;
	private String count = "5";
	
	public String getBrowseIndex() {
		return browseIndex;
	}
	public void setBrowseIndex(String browseIndex) {
		this.browseIndex = browseIndex;
	}
	
	public String getDiscoveryConfiguration() {
		return discoveryConfiguration;
	}
	public void setDiscoveryConfiguration(String discoveryConfiguration) {
		this.discoveryConfiguration = discoveryConfiguration;
	}
	
	public String getSortOption() {
		return sortOption;
	}
	public void setSortOption(String sortOption) {
		this.sortOption = sortOption;
	}
	
	public String getCount() {
		return count;
	}
	public void setCount(String count) {
		this.count = count;
	}
	
}
