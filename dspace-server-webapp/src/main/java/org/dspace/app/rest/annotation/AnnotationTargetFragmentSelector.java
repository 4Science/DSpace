/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 *
 * <code>
 * "default": {
 *     // static
 *     "@type": "oa:FragmentSelector",
 *     // needs to be stored!
 *     "value": "xywh=139,29,52,41"
 * }
 * </code>
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnnotationTargetFragmentSelector extends AnnotationTargetSelector {

    public static final String TYPE = "oa:FragmentSelector";

    {
        this.type = TYPE;
    }
}
