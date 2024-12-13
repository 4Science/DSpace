/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class CrisLayoutSliderComponent implements CrisLayoutSectionComponent {

    private String style;
    private String discoveryConfigurationName;
    private String sortField;
    private String order;
    private int numberOfItems;
    private int maxNumberOfElements;
    private int maxNumberOfVisiblePages;

    public void setStyle(String style) {
        this.style = style;
    }

    @Override
    public String getStyle() {
        return style;
    }

    public String getDiscoveryConfigurationName() {
        return discoveryConfigurationName;
    }

    public void setDiscoveryConfigurationName(String discoveryConfigurationName) {
        this.discoveryConfigurationName = discoveryConfigurationName;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public int getNumberOfItems() {
        return numberOfItems;
    }

    public void setNumberOfItems(int numberOfItems) {
        this.numberOfItems = numberOfItems;
    }

    public int getMaxNumberOfElements() {
        return maxNumberOfElements;
    }

    public void setMaxNumberOfElements(int maxNumberOfElements) {
        this.maxNumberOfElements = maxNumberOfElements;
    }

    public int getMaxNumberOfVisiblePages() {
        return maxNumberOfVisiblePages;
    }


    public void setMaxNumberOfVisiblePages(int maxNumberOfVisiblePages) {
        this.maxNumberOfVisiblePages = maxNumberOfVisiblePages;
    }
}
