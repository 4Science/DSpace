/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import static org.dspace.app.rest.annotation.AnnotationRestDeserializer.DATETIME_FORMAT;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 *
 * <code>
 * {
 *     // static
 *     "@context": "http://iiif.io/api/presentation/2/context.json",
 *     // static
 *     "@type": "oa:Annotation",
 *     // static
 *     "motivation": "oa:commenting",
 *     "on": {
 *         // static
 *         "@type": "oa:SpecificResource",
 *         // computed at runtime with bitstream uuid
 *         "full": "http://localhost:8080/server/iiif/af5b8b9a-3883-4764-965c-248f1f1f1546/canvas/3c9e76fd-0ef7-4df7
 *         -af7a-7356220e2451",
 *         "selector": {
 *             // static
 *             "@type": "oa:Choice",
 *             "default": {
 *                 // static
 *                 "@type": "oa:FragmentSelector",
 *                 // needs to be stored!
 *                 "value": "xywh=139,29,52,41"
 *             },
 *             "item": {
 *                 // static
 *                 "@type": "oa:SvgSelector",
 *                 // needs to be stored!
 *                 "value": "<svg xmlns='http://www.w3.org/2000/svg'><path xmlns=\"http://www.w3.org/2000/svg\"
 *                 d=\"M139.39024,71.02439v-41.70732h52.68293v41.70732z\" data-paper-data=\"{&quot;state&quot;
 *                 :null}\" fill=\"none\" fill-rule=\"nonzero\" stroke=\"#00bfff\" stroke-width=\"1\"
 *                 stroke-linecap=\"butt\" stroke-linejoin=\"miter\" stroke-miterlimit=\"10\" stroke-dasharray=\"\"
 *                 stroke-dashoffset=\"0\" font-family=\"none\" font-weight=\"none\" font-size=\"none\"
 *                 text-anchor=\"none\" style=\"mix-blend-mode: normal\"/></svg>"
 *             }
 *         }
 *     },
 *     "resource": {
 *         // needs to be stored! - resource.chars & resource.fulltext
 *         "chars": "<p>Test</p>",
 *         // static
 *         "@type": "dctypes:Text"
 *     }
 * }
 * </code>
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@JsonDeserialize(using = AnnotationRestDeserializer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnnotationRest {

    //  {dspace.url}/server/iiif/{item-uuid}/canvas/{bitstream-uuid}
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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATETIME_FORMAT)
    LocalDateTime created;
    @JsonProperty("dcterms:modified")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATETIME_FORMAT)
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
