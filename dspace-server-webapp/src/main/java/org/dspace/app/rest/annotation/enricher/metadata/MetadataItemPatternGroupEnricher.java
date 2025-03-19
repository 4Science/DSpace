/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher.metadata;

import java.util.function.BiConsumer;

import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.core.Context;

/**
 * Enricher that extracts a value from a given field of the AnnotationRest, then uses a
 * MetadataPatternGroupExtractor to extract a value from the Item's metadata.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MetadataItemPatternGroupEnricher extends MetadataItemEnricher {

    final MetadataPatternGroupExtractor metadataPatternGroupExtractor;

    protected String extract(String value) {
        return metadataPatternGroupExtractor.extract(value);
    }

    @Override
    protected BiConsumer<Context, Item> addMetadata(String value) {
        if (value == null) {
            return empty();
        }
        return super.addMetadata(metadataPatternGroupExtractor.extract(value));
    }

    protected BiConsumer<Context, Item> setMetadataValue(String value) {
        if (value == null) {
            return empty();
        }
        return super.setMetadataValue(metadataPatternGroupExtractor.extract(value));
    }

    public MetadataItemPatternGroupEnricher(
        String spel, MetadataFieldName metadata, Class<?> clazz, String patternWithGroup
    ) {
        super(spel, metadata, clazz);
        this.metadataPatternGroupExtractor = new MetadataPatternGroupExtractor(patternWithGroup);
    }
}
