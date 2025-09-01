/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.dspace.app.rest.annotation.AnnotationRest;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * This functional interface can be used as an Enricher for a given Item.
 * This enricher will take an {@link AnnotationRest}, extracts details from it and returns a {@link BiConsumer} that
 * will apply the extracted details to the {@link Item}.
 * You can compose these {@link ItemEnricher}s together, in order to apply them all at once.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@FunctionalInterface
public interface ItemEnricher extends Function<AnnotationRest, BiConsumer<Context, Item>> {
    default BiConsumer<Context, Item> empty() {
        return (context, item) -> { };
    }
}
