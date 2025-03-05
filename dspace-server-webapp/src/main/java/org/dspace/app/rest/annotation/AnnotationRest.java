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

    //  {dspace.url}/server/iiif/{item-uuid}/canvas/{canvas-uuid}
    // "https://dspaceglam7dev.4science.cloud/server/iiif/caa0e16c-bdfd-40e5-ba75-3ae6da639903/canvas/70090869-18e1-425f-94fe-574d750cef57"
    static final String ANNOTATION = "annotation";
    static final String CONTEXT = "http://iiif.io/api/presentation/2/context.json";
    static final String TYPE = "oa:Annotation";
    static final String MOTIVATION = "oa:commenting";
    static final String MOTIVATION_ARRAY = "[\"oa:commenting\"]";

    @JsonProperty("@id")
    String id;
    @JsonProperty(value = "@type", defaultValue = TYPE, required = true)
    String type = TYPE;
    @JsonProperty(value = "@context", defaultValue = CONTEXT, required = true)
    String context = CONTEXT;

    @JsonProperty("dcterms:created")
    LocalDateTime created;
    @JsonProperty("dcterms:modified")
    LocalDateTime modified;

    @JsonProperty(value = "motivation", defaultValue = MOTIVATION_ARRAY, required = true)
    List<String> motivation = List.of(MOTIVATION);

    // maps `resource` field
    @JsonProperty("resource")
    List<AnnotationBodyRest> resource = List.of(new AnnotationBodyRest());
    // maps `on` field
    @JsonProperty("on")
    List<AnnotationTargetRest> on = List.of(new AnnotationTargetRest());

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

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getContext() {
        return context;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public LocalDateTime getModified() {
        return modified;
    }

    public List<String> getMotivation() {
        return motivation;
    }

    public List<AnnotationBodyRest> getResource() {
        return resource;
    }

    public List<AnnotationTargetRest> getOn() {
        return on;
    }
}
