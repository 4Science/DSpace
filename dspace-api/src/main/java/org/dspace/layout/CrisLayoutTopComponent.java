/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

/**
 * An implementation of {@link CrisLayoutSectionComponent} that model the Top
 * section.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CrisLayoutTopComponent implements CrisLayoutSectionComponent {

    private String discoveryConfigurationName;

    private String sortField;

    private String order;

    private String style;

    private String titleKey;

    private Integer numberOfItems;

    private Boolean showThumbnails;

    private boolean showAsCard;

    private boolean showLayoutSwitch;

    private LayoutMode defaultLayoutMode;

    private String cardStyle;

    private String cardColumnStyle;

    private String itemListStyle;

    private boolean showAllResults;

    /**
     * @return the discoveryConfigurationName
     */
    public String getDiscoveryConfigurationName() {
        return discoveryConfigurationName;
    }

    /**
     * @param discoveryConfigurationName the discoveryConfigurationName to set
     */
    public void setDiscoveryConfigurationName(String discoveryConfigurationName) {
        this.discoveryConfigurationName = discoveryConfigurationName;
    }

    /**
     * @return the sortField
     */
    public String getSortField() {
        return sortField;
    }

    /**
     * @param sortField the sortField to set
     */
    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    /**
     * @return the order
     */
    public String getOrder() {
        return order;
    }

    /**
     * @param order the order to set
     */
    public void setOrder(String order) {
        this.order = order;
    }

    @Override
    public String getStyle() {
        return this.style;
    }

    /**
     * @param style the style to set
     */
    public void setStyle(String style) {
        this.style = style;
    }

    /**
     *
     * @return titleKey param value
     */
    public String getTitleKey() {
        return titleKey;
    }

    /**
     * a key containing title of this section, in case is missing
     * sortField value is used
     * @param titleKey
     */
    public void setTitleKey(String titleKey) {
        this.titleKey = titleKey;
    }

    /**
     *
     * @return Number of items to be contained in layout section
     */
    public Integer getNumberOfItems() {
        return numberOfItems;
    }

    public void setNumberOfItems(Integer numberOfItems) {
        this.numberOfItems = numberOfItems;
    }

    /**
     * @return the showAsCard
     */
    public boolean isShowAsCard() {
        return showAsCard;
    }

    /**
     * @param showAsCard the showAsCard to set
     */
    public void setShowAsCard(boolean showAsCard) {
        this.showAsCard = showAsCard;
    }

    /**
     * @return the showLayoutSwitch
     */
    public boolean isShowLayoutSwitch() {
        return showLayoutSwitch;
    }

    /**
     * @param showLayoutSwitch the showLayoutSwitch to set
     */
    public void setShowLayoutSwitch(boolean showLayoutSwitch) {
        this.showLayoutSwitch = showLayoutSwitch;
    }

    /**
     * @return the defaultLayoutMode
     */
    public LayoutMode getDefaultLayoutMode() {
        return defaultLayoutMode;
    }

    /**
     * @param defaultLayoutMode the defaultLayoutMode to set
     */
    public void setDefaultLayoutMode(LayoutMode defaultLayoutMode) {
        this.defaultLayoutMode = defaultLayoutMode;
    }

    /**
     * @return the cardStyle
     */
    public String getCardStyle() {
        return cardStyle;
    }

    /**
     * @param cardStyle the cardStyle to set
     */
    public void setCardStyle(String cardStyle) {
        this.cardStyle = cardStyle;
    }

    /**
     * @return the cardColumnStyle
     */
    public String getCardColumnStyle() {
        return cardColumnStyle;
    }

    /**
     * @param cardColumnStyle the cardColumnStyle to set
     */
    public void setCardColumnStyle(String cardColumnStyle) {
        this.cardColumnStyle = cardColumnStyle;
    }

    /**
     * @return the itemListStyle
     */
    public String getItemListStyle() {
        return itemListStyle;
    }

    /**
     * @param itemListStyle the itemListStyle to set
     */
    public void setItemListStyle(String itemListStyle) {
        this.itemListStyle = itemListStyle;
    }

    /**
     * @return the showAllResults
     */
    public boolean isShowAllResults() {
        return showAllResults;
    }

    /**
     * @param showAllResults the showAllResults to set
     */
    public void setShowAllResults(boolean showAllResults) {
        this.showAllResults = showAllResults;
    }

    /**
     * @author Stefano Maffei (steph-ieffam @ 4Science)
     *  Defines the layout mode used in the CrisLayoutTopComponent
     */
    public static enum LayoutMode {

        LIST, CARD;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    public Boolean getShowThumbnails() {
        return showThumbnails;
    }

    public void setShowThumbnails(Boolean showThumbnails) {
        this.showThumbnails = showThumbnails;
    }
}
