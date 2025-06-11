/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import java.util.List;
import java.util.function.BiConsumer;

import org.dspace.app.rest.annotation.enricher.GenericItemEnricher;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class AnnotationRestMapper {

    List<GenericItemEnricher<AnnotationBodyRest>> bodyRestEnricher;
    List<GenericItemEnricher<AnnotationTargetRest>> targetEnricher;
    List<GenericItemEnricher<AnnotationRest>> annotationEnrichers;

    public AnnotationRestMapper(
        List<GenericItemEnricher<AnnotationBodyRest>> bodyRestEnricher,
        List<GenericItemEnricher<AnnotationTargetRest>> targetEnricher,
        List<GenericItemEnricher<AnnotationRest>> annotationEnrichers
    ) {
        this.bodyRestEnricher = bodyRestEnricher;
        this.targetEnricher = targetEnricher;
        this.annotationEnrichers = annotationEnrichers;
    }

    public AnnotationRest map(Context context, Item item) {

        AnnotationRest annotationRest = new AnnotationRest();

        BiConsumer<Context, AnnotationBodyRest> bodyRestConsumer =
            bodyRestEnricher.stream()
                            .map(enricher -> enricher.apply(item))
                            .reduce(BiConsumer::andThen)
                            .orElse(empty());
        BiConsumer<Context, AnnotationTargetRest> targetRestConsumer =
            targetEnricher.stream()
                          .map(enricher -> enricher.apply(item))
                          .reduce(BiConsumer::andThen)
                          .orElse(empty());
        BiConsumer<Context, AnnotationRest> annotationRestConsumer =
            annotationEnrichers.stream()
                               .map(enricher -> enricher.apply(item))
                               .reduce(BiConsumer::andThen)
                               .orElse(empty());

        // As of now we expect each annotation to have just:
        // - ONE Body element
        // - ONE Target element
        if (annotationRest.resource.size() > 1 || annotationRest.on.size() > 1) {
            throw new IllegalArgumentException(
                "Cannot convert the annotationRest with id: " + annotationRest.getId() + " since it has " +
                " more than one element for body (resource) or target (on) fields"
            );
        }
        bodyRestConsumer.accept(context, annotationRest.resource.get(0));
        targetRestConsumer.accept(context, annotationRest.on.get(0));
        annotationRestConsumer.accept(context, annotationRest);

        return annotationRest;
    }

    private <T> BiConsumer<Context, T> empty() {
        return (context, item) -> { };
    }
}
