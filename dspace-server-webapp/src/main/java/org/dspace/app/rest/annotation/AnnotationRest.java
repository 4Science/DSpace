/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@JsonDeserialize(using = AnnotationRestDeserializer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnnotationRest {

    static final String ANNOTATION = "annotation";

    @JsonProperty("@id")
    String id;
    @JsonProperty("@type")
    String type;
    @JsonProperty("@context")
    String context;

    @JsonProperty("dcterms:created")
    LocalDateTime created;
    @JsonProperty("dcterms:modified")
    LocalDateTime modified;

    @JsonProperty("motivation")
    List<String> motivation;

    // maps `resource` field
    @JsonProperty("resource")
    List<AnnotationBodyRest> resource;
    // maps `on` field
    @JsonProperty("on")
    List<AnnotationTargetRest> on;

    public AnnotationRest setId(String id) {
        this.id = id;
        return this;
    }

    public AnnotationRest setType(String type) {
        this.type = type;
        return this;
    }

    public AnnotationRest setContext(String context) {
        this.context = context;
        return this;
    }

    public AnnotationRest setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public AnnotationRest setModified(LocalDateTime modified) {
        this.modified = modified;
        return this;
    }

    public AnnotationRest setMotivation(List<String> motivation) {
        this.motivation = motivation;
        return this;
    }

    public AnnotationRest setResource(List<AnnotationBodyRest> resource) {
        this.resource = resource;
        return this;
    }

    public AnnotationRest setOn(List<AnnotationTargetRest> on) {
        this.on = on;
        return this;
    }
}
