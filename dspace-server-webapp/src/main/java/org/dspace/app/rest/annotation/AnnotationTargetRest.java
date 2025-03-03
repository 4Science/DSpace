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
public class AnnotationTargetRest {

    public static final String TYPE = "oa:SpecificResource";

    @JsonProperty("@id")
    String id;
    @JsonProperty(value = "@type", defaultValue = TYPE, required = true)
    String type = TYPE;
    @JsonProperty("full")
    String full;

    @JsonProperty("selector")
    AnnotationTargetSelectorComposite selector = new AnnotationTargetSelectorComposite();
    @JsonProperty("within")
    AnnotationTargetWithin within = new AnnotationTargetWithin();

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getFull() {
        return full;
    }

    public AnnotationTargetSelectorComposite getSelector() {
        return selector;
    }

    public AnnotationTargetWithin getWithin() {
        return within;
    }
}
