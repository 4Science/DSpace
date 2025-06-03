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
 *
 * <code>
 *     "default": {
 *          // static
 *          "@type": "oa:FragmentSelector",
 *          // needs to be stored!
 *          "value": "xywh=139,29,52,41"
 *      }
 * </code>
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnnotationTargetSelector {

    @JsonProperty(value = "@type", required = true)
    String type;
    @JsonProperty(value = "value", required = true)
    String value;

    public String getType() {
        return type;
    }

    public AnnotationTargetSelector setType(String type) {
        this.type = type;
        return this;
    }

    public String getValue() {
        return value;
    }

    public AnnotationTargetSelector setValue(String value) {
        this.value = value;
        return this;
    }
}
