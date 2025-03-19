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

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Generic Functional interface that can be used as an Enricher for a given Item.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@FunctionalInterface
public interface GenericItemEnricher<T> extends Function<Item, BiConsumer<Context, T>> {
}
