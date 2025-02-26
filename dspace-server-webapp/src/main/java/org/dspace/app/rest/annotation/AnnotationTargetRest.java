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

    @JsonProperty("@id")
    String id;
    @JsonProperty("@type")
    String type;
    @JsonProperty("full")
    String full;

    @JsonProperty("selector")
    AnnotationTargetSelector selector;
    @JsonProperty("within")
    AnnotationTargetWithin within;


}
