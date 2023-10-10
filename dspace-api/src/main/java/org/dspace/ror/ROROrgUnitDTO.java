/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.ror;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ROROrgUnitDTO {

    @JsonProperty("id")
    private String url;

    private String name;

    private String[] acronyms;

    private String[] aliases;

    private String status;

    private String[] types;

    public String getIdentifier() {
        if (StringUtils.isBlank(url)) {
            return null;
        }

        String[] splittedUrl = url.split("/");
        return splittedUrl[splittedUrl.length - 1];
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getAcronyms() {
        return acronyms;
    }

    public void setAcronyms(String[] acronyms) {
        this.acronyms = acronyms;
    }

    public String[] getAliases() {
        return aliases;
    }

    public void setAliases(String[] aliases) {
        this.aliases = aliases;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String[] getTypes() {
        return types;
    }

    public void setTypes(String[] types) {
        this.types = types;
    }
}
