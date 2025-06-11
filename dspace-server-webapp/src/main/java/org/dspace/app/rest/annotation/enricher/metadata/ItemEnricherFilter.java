/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher.metadata;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.dspace.app.rest.annotation.AnnotationRest;
import org.dspace.app.rest.annotation.enricher.ItemEnricher;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * This class is used to filter the items to which the enricher should be applied.
 * It takes a predicate and an enricher as input and returns a new ItemEnricherFilter instance.
 * The apply method of the returned ItemEnricherFilter instance applies the enricher to the items
 * that satisfy the predicate.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class ItemEnricherFilter implements ItemEnricher {

    private final ItemEnricher itemEnricher;
    private final Predicate<Item> filter;

    public ItemEnricherFilter(Predicate<Item> filter, ItemEnricher itemEnricher) {
        this.filter = filter;
        this.itemEnricher = itemEnricher;
    }

    @Override
    public BiConsumer<Context, Item> apply(AnnotationRest annotationRest) {
        return (context, item) -> {
            if (filter.test(item)) {
                itemEnricher.apply(annotationRest).accept(context, item);
            }
        };
    }
}
