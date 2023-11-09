/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link CrisLayoutSectionComponent} that allows definition
 * of a section containing a list of twittes.
 */
public class CrisLayoutTwitterComponent implements CrisLayoutSectionComponent {

    private String style = "";

    private List<TwitterSettings> twitterSettings = new ArrayList<>();

    @Override
    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public List<TwitterSettings> getTwitterSettings() {
        return twitterSettings;
    }

    public void setTwitterSettings(List<TwitterSettings> twitterSettings) {
        this.twitterSettings = twitterSettings;
    }

    public static class TwitterSettings {

        private String theme = "light";

        private String url;

        private boolean optOut = true;

        public String getTheme() {
            return theme;
        }

        public void setTheme(String theme) {
            this.theme = theme;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public boolean getOptOut() {
            return optOut;
        }

        public void setOptOut(boolean optOut) {
            this.optOut = optOut;
        }
    }
}
