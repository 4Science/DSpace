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
 * "item": {
 *     // static
 *     "@type": "oa:SvgSelector",
 *     // needs to be stored!
 *     "value": "<svg xmlns='http://www.w3.org/2000/svg'><path xmlns=\"http://www.w3.org/2000/svg\"
 *     d=\"M139.39024,71.02439v-41.70732h52.68293v41.70732z\" data-paper-data=\"{&quot;state&quot;
 *     :null}\" fill=\"none\" fill-rule=\"nonzero\" stroke=\"#00bfff\" stroke-width=\"1\"
 *     stroke-linecap=\"butt\" stroke-linejoin=\"miter\" stroke-miterlimit=\"10\" stroke-dasharray=\"\"
 *     stroke-dashoffset=\"0\" font-family=\"none\" font-weight=\"none\" font-size=\"none\"
 *     text-anchor=\"none\" style=\"mix-blend-mode: normal\"/></svg>"
 * }
 * </code>
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnnotationTargetSvgSelector extends AnnotationTargetSelector {

    public static final String TYPE = "oa:SvgSelector";

    {
        this.type = TYPE;
    }

}
