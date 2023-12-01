/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.UnpaywallItemController;
import org.dspace.unpaywall.dto.UnpaywallItemVersionDto;

/**
 * The Unpaywall Item REST resource.
 */
public class UnpaywallItemVersionsRest extends BaseObjectRest<String> {

    public static final String VERSIONS = "versions";
    public static final String CATEGORY = RestAddressableModel.CORE;
    private List<UnpaywallItemVersionDto> versions = new ArrayList<>();

    public UnpaywallItemVersionsRest() {
    }

    public UnpaywallItemVersionsRest(List<UnpaywallItemVersionDto> versions) {
        this.versions = versions;
    }

    public String getCategory() {
        return CATEGORY;
    }

    public Class getController() {
        return UnpaywallItemController.class;
    }

    public String getType() {
        return VERSIONS;
    }

    public List<UnpaywallItemVersionDto> getVersions() {
        return versions;
    }

    public void setVersions(List<UnpaywallItemVersionDto> versions) {
        this.versions = versions;
    }
}
