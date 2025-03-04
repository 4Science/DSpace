/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import java.util.function.BiConsumer;

import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.core.Context;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class GenericItemMetadataEnricher<T> extends AbstractMetadataSpelMapper
    implements GenericItemEnricher<T> {

    public GenericItemMetadataEnricher(String spel, MetadataFieldName metadata, Class<?> clazz) {
        super(spel, metadata, clazz);
    }

    @Override
    public BiConsumer<Context, T> apply(Item item) {
        return (context, bodyRest) -> setFieldValue(bodyRest, getMetadataValue(item));
    }

    protected void setFieldValue(T bodyRest, String metadataValue) {
        fieldExpression.setValue(bodyRest, convert(metadataValue));
    }

    protected Object convert(String metadataValue) {
        return metadataValue;
    }

    protected String getMetadataValue(Item item) {
        return item.getItemService().getMetadata(item, metadata.toString());
    }
}
