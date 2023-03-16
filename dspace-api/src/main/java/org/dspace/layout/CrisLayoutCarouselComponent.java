/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

/**
 * @author Stefano Maffei (steph-ieffam @ 4Science)
 *
 */
public class CrisLayoutCarouselComponent implements CrisLayoutSectionComponent {

    private String discoveryConfigurationName;

    private String title;

    private String link;

    private String description;

    private String style;

    private String order;

    private String sortField;

    private int numberOfItems;

    private boolean targetBlank;

    private boolean fitWidth;

    private boolean fitHeight;

    private boolean keepAspectRatio;

    private double aspectRatio;

    private int carouselHeightPx;

    private String captionStyle;

    private String titleStyle;

    private boolean showBlurryBackdrop;

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
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the link
     */
    public String getLink() {
        return link;
    }

    /**
     * @param link the link to set
     */
    public void setLink(String link) {
        this.link = link;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getStyle() {
        return style;
    }

    /**
     * @param style the configured style
     */
    public void setStyle(String style) {
        this.style = style;
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
     * @return the numberOfItems
     */
    public int getNumberOfItems() {
        return numberOfItems;
    }

    /**
     * @param numberOfItems the numberOfItems to set
     */
    public void setNumberOfItems(int numberOfItems) {
        this.numberOfItems = numberOfItems;
    }

    /**
     * @return the targetBlank
     */
    public boolean isTargetBlank() {
        return targetBlank;
    }

    /**
     * @param targetBlank the targetBlank to set
     */
    public void setTargetBlank(boolean targetBlank) {
        this.targetBlank = targetBlank;
    }

    /**
     * @return the fitWidth
     */
    public boolean isFitWidth() {
        return fitWidth;
    }

    /**
     * @param fitWidth the fitWidth to set
     */
    public void setFitWidth(boolean fitWidth) {
        this.fitWidth = fitWidth;
    }

    /**
     * @return the fitHeight
     */
    public boolean isFitHeight() {
        return fitHeight;
    }

    /**
     * @param fitHeight the fitHeight to set
     */
    public void setFitHeight(boolean fitHeight) {
        this.fitHeight = fitHeight;
    }

    /**
     * @return the keepAspectRatio
     */
    public boolean isKeepAspectRatio() {
        return keepAspectRatio;
    }

    /**
     * @param keepAspectRatio the keepAspectRatio to set
     */
    public void setKeepAspectRatio(boolean keepAspectRatio) {
        this.keepAspectRatio = keepAspectRatio;
    }

    /**
     * @return the aspectRatio
     */
    public double getAspectRatio() {
        return aspectRatio;
    }

    /**
     * @param aspectRatio the aspectRatio to set
     */
    public void setAspectRatio(double aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    /**
     * @return the carouselHeightPx
     */
    public int getCarouselHeightPx() {
        return carouselHeightPx;
    }

    /**
     * @param carouselHeightPx the carouselHeightPx to set
     */
    public void setCarouselHeightPx(int carouselHeightPx) {
        this.carouselHeightPx = carouselHeightPx;
    }

    /**
     * @return the captionStyle
     */
    public String getCaptionStyle() {
        return captionStyle;
    }

    /**
     * @param captionStyle the captionStyle to set
     */
    public void setCaptionStyle(String captionStyle) {
        this.captionStyle = captionStyle;
    }

    /**
     * @return the titleStyle
     */
    public String getTitleStyle() {
        return titleStyle;
    }

    /**
     * @param titleStyle the titleStyle to set
     */
    public void setTitleStyle(String titleStyle) {
        this.titleStyle = titleStyle;    }

    /**
     * @return isShowBlurryBackdrop
     */
    public boolean isShowBlurryBackdrop() {
        return showBlurryBackdrop;
    }

    /**
     * @param showBlurryBackdrop the showBlurryBackdrop to set
     */
    public void setShowBlurryBackdrop(boolean showBlurryBackdrop) {
        this.showBlurryBackdrop = showBlurryBackdrop;
    }
}
