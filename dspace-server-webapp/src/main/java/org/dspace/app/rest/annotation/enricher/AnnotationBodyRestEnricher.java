/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher;

import org.dspace.app.rest.annotation.AnnotationBodyRest;
import org.dspace.app.rest.annotation.enricher.metadata.GenericItemMetadataEnricher;
import org.dspace.content.MetadataFieldName;

/**
 * Enricher for the annotation body metadata field
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class AnnotationBodyRestEnricher extends GenericItemMetadataEnricher<AnnotationBodyRest> {

    public AnnotationBodyRestEnricher(String spel, MetadataFieldName metadata, Class<?> clazz) {
        super(spel, metadata, clazz);
    }
}
