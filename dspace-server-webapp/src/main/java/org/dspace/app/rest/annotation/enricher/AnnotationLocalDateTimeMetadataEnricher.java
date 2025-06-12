/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher;

import static org.dspace.app.rest.annotation.AnnotationRestDeserializer.DATETIME_FORMATTER;

import java.time.LocalDateTime;

import org.dspace.app.rest.annotation.AnnotationRest;
import org.dspace.app.rest.annotation.enricher.metadata.GenericItemMetadataEnricher;
import org.dspace.content.MetadataFieldName;

/**
 * This mapper transforms a String value to a LocalDateTime value using a spel expression and a given formatter.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class AnnotationLocalDateTimeMetadataEnricher extends GenericItemMetadataEnricher<AnnotationRest> {

    public AnnotationLocalDateTimeMetadataEnricher(String spel, MetadataFieldName metadata, Class<?> clazz) {
        super(spel, metadata, clazz);
    }

    @Override
    protected Object convert(String metadataValue) {
        if (metadataValue == null) {
            return null;
        }
        return LocalDateTime.parse(metadataValue, DATETIME_FORMATTER);
    }
}
