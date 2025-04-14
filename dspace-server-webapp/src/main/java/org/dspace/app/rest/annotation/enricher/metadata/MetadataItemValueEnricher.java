/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher.metadata;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import org.dspace.app.rest.annotation.AnnotationRest;
import org.dspace.app.rest.annotation.enricher.ItemEnricher;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;

/**
 * Enricher that adds a metadata value to an item with the given metadata field name.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MetadataItemValueEnricher<T> implements ItemEnricher {

    private final MetadataFieldName metadata;
    private final T value;

    public MetadataItemValueEnricher(MetadataFieldName metadata, T value) {
        this.metadata = metadata;
        this.value = value;
    }

    @Override
    public BiConsumer<Context, Item> apply(AnnotationRest annotationRest) {
        return (context, item) -> {
            if (value == null) {
                return;
            }
            String sValue;
            if (value instanceof String) {
                sValue = (String) value;
            } else {
                sValue = value.toString();
            }
            try {
                item.getItemService().addMetadata(
                    context,
                    item,
                    metadata.schema,
                    metadata.element,
                    metadata.qualifier,
                    null,
                    sValue
                );
            } catch (SQLException e) {
                throw new SQLRuntimeException("Cannot add the given value as metadata (" + metadata + ") to the Item",
                                              e);
            }
        };
    }
}
