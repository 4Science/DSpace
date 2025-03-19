/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher;

import java.util.List;
import java.util.function.BiConsumer;

import org.dspace.app.rest.annotation.AnnotationRest;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Enricher that composes a list of {@link ItemEnricher}s.
 * The {@link ItemEnricher}s will be applied in the order they are in the list.
 * The result of the first {@link ItemEnricher} will be passed to the second, and so on.
 * The result of the last {@link ItemEnricher} will be returned.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class ItemEnricherComposite implements ItemEnricher {

    List<ItemEnricher> itemEnrichers;

    public ItemEnricherComposite(List<ItemEnricher> itemEnrichers) {
        this.itemEnrichers = itemEnrichers;
    }

    @Override
    public BiConsumer<Context, Item> apply(AnnotationRest annotation) {
        return this.itemEnrichers
            .stream()
            .map((enricher) -> enricher.apply(annotation))
            .reduce(BiConsumer::andThen)
            .orElseGet(this::empty);
    }
}
