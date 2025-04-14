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
 * <code>
 * "on": {
 *      // static
 *      "@type": "oa:SpecificResource",
 *      // computed at runtime with bitstream uuid
 *      "full": "http://localhost:8080/server/iiif/af5b8b9a-3883-4764-965c-248f1f1f1546/canvas/3c9e76fd-0ef7-4df7-af7a-7356220e2451",
 *      "selector": {
 *          // static
 *          "@type": "oa:Choice",
 *          "default": {
 *              // static
 *              "@type": "oa:FragmentSelector",
 *              // needs to be stored!
 *              "value": "xywh=139,29,52,41"
 *          },
 *          "item": {
 *              // static
 *              "@type": "oa:SvgSelector",
 *              // needs to be stored!
 *              "value": "<svg xmlns='http://www.w3.org/2000/svg'><path xmlns=\"http://www.w3.org/2000/svg\"
 *              d=\"M139.39024,71.02439v-41.70732h52.68293v41.70732z\" data-paper-data=\"{&quot;state&quot;
 *              :null}\" fill=\"none\" fill-rule=\"nonzero\" stroke=\"#00bfff\" stroke-width=\"1\"
 *              stroke-linecap=\"butt\" stroke-linejoin=\"miter\" stroke-miterlimit=\"10\" stroke-dasharray=\"\"
 *              stroke-dashoffset=\"0\" font-family=\"none\" font-weight=\"none\" font-size=\"none\"
 *              text-anchor=\"none\" style=\"mix-blend-mode: normal\"/></svg>"
 *          }
 *      }
 * }
 * </code>
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
    AnnotationTargetWithin within;

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

    public AnnotationTargetRest setId(String id) {
        this.id = id;
        return this;
    }

    public AnnotationTargetRest setType(String type) {
        this.type = type;
        return this;
    }

    public AnnotationTargetRest setFull(String full) {
        this.full = full;
        return this;
    }

    public AnnotationTargetRest setSelector(AnnotationTargetSelectorComposite selector) {
        this.selector = selector;
        return this;
    }

    public AnnotationTargetRest setWithin(AnnotationTargetWithin within) {
        this.within = within;
        return this;
    }
}
