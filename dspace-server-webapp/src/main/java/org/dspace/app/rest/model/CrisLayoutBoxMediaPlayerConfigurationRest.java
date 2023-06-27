/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Stefano Maffei
 *
 */
public class CrisLayoutBoxMediaPlayerConfigurationRest implements CrisLayoutBoxConfigurationRest {

    public static final String NAME = "mediaplayerconfiguration";

    private Set<String> videoResUUID = new HashSet<String>();

    private String type = NAME;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Set<String> getVideoResUUID() {
        return videoResUUID;
    }

    public void setVideoResUUID(Set<String> videoResUUID) {
        this.videoResUUID = videoResUUID;
    }

}
