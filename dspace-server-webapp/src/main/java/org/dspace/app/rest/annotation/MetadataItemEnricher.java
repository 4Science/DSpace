/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.core.Context;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MetadataItemEnricher extends AbstractMetadataSpelMapper implements ItemEnricher {

    public MetadataItemEnricher(String spel, MetadataFieldName metadata, Class<?> clazz) {
        super(spel, metadata, clazz);
    }

    @Override
    public BiConsumer<Context, Item> apply(AnnotationRest annotationRest) {
        Object value = extractValueFrom(annotationRest);
        if (value == null) {
            return empty();
        }

        if (value instanceof java.util.Collection) {
            return ((java.util.Collection<?>) value).stream()
                                                    .map(element -> addMetadata(element.toString()))
                                                    .reduce(BiConsumer::andThen)
                                                    .orElse(empty());
        }
        return addMetadata(value.toString());
    }

    protected Object extractValueFrom(AnnotationRest annotationRest) {
        return fieldExpression.getValue(annotationRest, clazz);
    }

    protected BiConsumer<Context, Item> addMetadata(String value) {
        return (context, item) -> {
            try {
                item.getItemService()
                    .addMetadata(
                        context,
                        item,
                        metadata.schema,
                        metadata.element,
                        metadata.qualifier,
                        null,
                        value
                    );
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
