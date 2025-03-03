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
public class AnnotationTargetSelectorComposite extends AnnotationTargetSelector {

    @JsonProperty("default")
    AnnotationTargetFragmentSelector defaultSelector = new AnnotationTargetFragmentSelector();
    @JsonProperty("item")
    AnnotationTargetSvgSelector item = new AnnotationTargetSvgSelector();

    public AnnotationTargetSelectorComposite setDefaultSelector(
        AnnotationTargetFragmentSelector defaultSelector) {
        this.defaultSelector = defaultSelector;
        return this;
    }

    public AnnotationTargetSelectorComposite setItem(AnnotationTargetSvgSelector item) {
        this.item = item;
        return this;
    }

    public AnnotationTargetSelector getDefaultSelector() {
        return defaultSelector;
    }

    public AnnotationTargetSelector getItem() {
        return item;
    }
}
