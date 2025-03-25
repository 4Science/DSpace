/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnnotationTargetWithin {

    @JsonProperty("@id")
    String id;
    @JsonProperty("@type")
    String type;

    public String getId() {
        return id;
    }

    public AnnotationTargetWithin setId(String id) {
        this.id = id;
        return this;
    }

    public String getType() {
        return type;
    }

    public AnnotationTargetWithin setType(String type) {
        this.type = type;
        return this;
    }
}
