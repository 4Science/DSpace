/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

/**
 * Implementation of {@link CrisLayoutSectionComponent}
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class CrisGridComponent implements CrisLayoutSectionComponent {

    private String discoveryConfigurationName;
    private String style;
    private String mainContentLink;

    @Override
    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getDiscoveryConfigurationName() {
        return discoveryConfigurationName;
    }

    public void setDiscoveryConfigurationName(String discoveryConfigurationName) {
        this.discoveryConfigurationName = discoveryConfigurationName;
    }

    public String getMainContentLink() {
        return mainContentLink;
    }

    public void setMainContentLink(String mainContentLink) {
        this.mainContentLink = mainContentLink;
    }
}
