/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher.metadata;

import java.sql.SQLException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

import org.dspace.app.rest.annotation.AnnotationRest;
import org.dspace.app.rest.annotation.enricher.ItemEnricher;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.core.Context;

/**
 * Metadata Item Enricher that extracts the value from the AnnotationRest using a spel expression
 * and then applies it to the Item.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MetadataItemEnricher extends AbstractMetadataSpelMapper implements ItemEnricher {

    private UnaryOperator<String> metadataValueMapper;

    public MetadataItemEnricher(String spel, MetadataFieldName metadata, Class<?> clazz) {
        this(spel, metadata, clazz, null);
    }

    public MetadataItemEnricher(
        String spel, MetadataFieldName metadata, Class<?> clazz, UnaryOperator<String> metadataValueMapper
    ) {
        super(spel, metadata, clazz);
        this.metadataValueMapper = metadataValueMapper;
    }

    @Override
    public BiConsumer<Context, Item> apply(AnnotationRest annotationRest) {
        Object value = extractValueFrom(annotationRest);
        if (value == null) {
            return empty();
        }

        if (value instanceof java.util.Collection) {
            return ((java.util.Collection<?>) value).stream()
                                                    .filter(Objects::nonNull)
                                                    .map(element -> addMetadata(element.toString()))
                                                    .reduce(BiConsumer::andThen)
                                                    .map(metadataAdder ->
                                                             clearMetadata().andThen(metadataAdder)
                                                    )
                                                    .orElse(empty());
        }
        return setMetadataValue(value.toString());
    }

    protected Object extractValueFrom(AnnotationRest annotationRest) {
        return fieldExpression.getValue(annotationRest, clazz);
    }

    protected BiConsumer<Context, Item> addMetadata(String value) {
        return (context, item) -> {
            try {
                String metadataValue = value;
                if (metadataValueMapper != null) {
                    metadataValue = metadataValueMapper.apply(metadataValue);
                }
                item.getItemService()
                    .addMetadata(
                        context,
                        item,
                        metadata.schema,
                        metadata.element,
                        metadata.qualifier,
                        null,
                        metadataValue
                    );
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        };
    }

    protected BiConsumer<Context, Item> setMetadataValue(String value) {
        return (context, item) -> {
            try {
                String metadataValue = value;
                if (metadataValueMapper != null) {
                    metadataValue = metadataValueMapper.apply(metadataValue);
                }
                item.getItemService()
                    .setMetadataSingleValue(
                        context, item, metadata.schema, metadata.element, metadata.qualifier,null, metadataValue
                    );
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        };
    }

    protected BiConsumer<Context, Item> clearMetadata() {
        return (context, item) -> {
            try {
                item.getItemService()
                    .clearMetadata(
                        context, item, metadata.schema, metadata.element, metadata.qualifier,null
                    );
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        };
    }

}
