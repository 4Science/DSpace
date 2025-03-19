/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher;

import org.dspace.app.rest.annotation.AnnotationTargetRest;
import org.dspace.app.rest.annotation.enricher.metadata.GenericItemMetadataEnricher;
import org.dspace.content.MetadataFieldName;

/**
 * Enricher for the annotation target metadata field.
 * The field is composed by the target metadata field and the target metadata field value.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class AnnotationTargetRestEnricher extends GenericItemMetadataEnricher<AnnotationTargetRest> {

    public AnnotationTargetRestEnricher(String spel, MetadataFieldName metadata, Class<?> clazz) {
        super(spel, metadata, clazz);
    }
}
