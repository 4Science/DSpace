/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher.metadata;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;

import org.dspace.app.rest.annotation.AnnotationRest;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.core.Context;

/**
 * Enricher that extracts a LocalDateTime from the AnnotationRest and sets it on the item metadata by converting it
 * using the formatter.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MetadataItemLocalDateTimeEnricher extends MetadataItemEnricher {

    final DateTimeFormatter formatter;

    public MetadataItemLocalDateTimeEnricher(String spel, MetadataFieldName metadata, DateTimeFormatter formatter) {
        super(spel, metadata, LocalDateTime.class);
        this.formatter = formatter;
    }

    @Override
    public BiConsumer<Context, Item> apply(AnnotationRest annotationRest) {
        LocalDateTime value = (LocalDateTime) extractValueFrom(annotationRest);
        if (value == null) {
            return empty();
        }

        return setMetadataValue(value.format(formatter));
    }
}
