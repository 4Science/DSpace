/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.MetadataFieldName;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class AnnotationRestMapperFactory {

    private AnnotationRestMapperFactory() { }

    static MetadataFieldName dateIssued = new MetadataFieldName("dc", "date", "issued");

    static MetadataFieldName dateModified = new MetadataFieldName("dcterms", "modified");

    static MetadataFieldName fragmentSelectorMetadata =
        new MetadataFieldName("glam", "annotation", "fragmentselector");

    static MetadataFieldName svgSelectorMetadata =
        new MetadataFieldName("glam", "annotation", "svgselector");
    static MetadataFieldName textMetadata =
        new MetadataFieldName("glam", "annotation", "text");
    static MetadataFieldName fulltextMetadata =
        new MetadataFieldName("glam", "annotation", "fulltext");

    public static AnnotationRestMapper annotationRestMapper(@Autowired ConfigurationService configurationService) {
        return new AnnotationRestMapper(
            List.of(
                charsMapper(),
                fullTextMapper()
            ),
            List.of(
                defaultSelectorValueMapper(),
                selectorItemValueMapper(),
                fullMapper(configurationService)
            ),
            List.of(
                modifiedMapper(),
                createdMapper(),
                idMapper(configurationService)
            )
        );
    }


    static AnnotationBodyRestEnricher charsMapper() {
        return new AnnotationBodyRestEnricher(
            "chars",
            textMetadata,
            String.class
        );
    }

    static AnnotationBodyRestEnricher fullTextMapper() {
        return new AnnotationBodyRestEnricher(
            "fullText",
            fulltextMetadata,
            String.class
        );
    }

    static AnnotationTargetRestEnricher defaultSelectorValueMapper() {
        return new AnnotationTargetRestEnricher(
            "selector.defaultSelector.value",
            fragmentSelectorMetadata,
            String.class
        );
    }

    static AnnotationTargetRestEnricher selectorItemValueMapper() {
        return new AnnotationTargetRestEnricher(
            "selector.item.value",
            svgSelectorMetadata,
            String.class
        );
    }


    static GenericItemMetadataEnricher<AnnotationRest> modifiedMapper() {
        return new AnnotationLocalDateTimeMetadataEnricher(
            "modified",
            dateModified,
            LocalDateTime.class
        );
    }

    static GenericItemMetadataEnricher<AnnotationRest> createdMapper() {
        return new AnnotationLocalDateTimeMetadataEnricher(
            "created",
            dateIssued,
            LocalDateTime.class
        );
    }

    static GenericItemEnricher<AnnotationTargetRest> fullMapper(@Autowired ConfigurationService configurationService) {
        return new AnnotationTargetRestComposedEnricher<String>(
            "full",
            List.of(
                (i) -> configurationService.getProperty("dspace.server.url") + "/iiif/",
                (i) -> i.getItemService().getMetadata(i, "glam.item") + "/canvas/",
                (i) -> i.getItemService().getMetadata(i, "glam.bitstream")
            ),
            StringUtils::join
        );
    }

    static GenericItemEnricher<AnnotationRest> idMapper(@Autowired ConfigurationService configurationService) {
        return new AnnotationFieldComposerEnricher<String>(
            "id",
            List.of(
                (i) -> configurationService.getProperty("dspace.server.url") + "/annotation/",
                (i) -> i.getID().toString()
            ),
            StringUtils::join
        );
    }


}
