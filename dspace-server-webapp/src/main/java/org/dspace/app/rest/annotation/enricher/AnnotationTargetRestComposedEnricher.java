/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import org.dspace.app.rest.annotation.AnnotationTargetRest;
import org.dspace.content.Item;

/**
 * Enricher that will compose a list of {@link Function}s that will be applied to an {@link Item} and then reduce the
 * result of these functions to a single value.
 * The result of the composition will be stored in the target field of the {@link AnnotationTargetRest} object.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class AnnotationTargetRestComposedEnricher<T> extends GenericComposedEnricher<AnnotationTargetRest, T> {

    public AnnotationTargetRestComposedEnricher(
        String targetField,
        List<Function<Item, T>> composer,
        BinaryOperator<T> reducer
    ) {
        super(targetField, composer, reducer);
    }

}
