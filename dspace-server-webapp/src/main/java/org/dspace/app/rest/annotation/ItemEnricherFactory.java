/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;


import static org.dspace.annotation.AnnotationItemConsumer.getAnnotationTitleReducer;
import static org.dspace.app.rest.annotation.AnnotationRestDeserializer.DATETIME_FORMATTER;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.annotation.enricher.ItemEnricher;
import org.dspace.app.rest.annotation.enricher.ItemEnricherComposite;
import org.dspace.app.rest.annotation.enricher.metadata.ItemEnricherFilter;
import org.dspace.app.rest.annotation.enricher.metadata.MetadataAuthorityItemEnricher;
import org.dspace.app.rest.annotation.enricher.metadata.MetadataItemContextUserEnricher;
import org.dspace.app.rest.annotation.enricher.metadata.MetadataItemEnricher;
import org.dspace.app.rest.annotation.enricher.metadata.MetadataItemLocalDateTimeEnricher;
import org.dspace.app.rest.annotation.enricher.metadata.MetadataPatternGroupExtractor;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.services.ConfigurationService;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class ItemEnricherFactory {

    static final String ITEM_PATTERN = "/iiif/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})";
    static final String BITSTREAM_PATTERN = "/canvas/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})";


    public static final String FULL_SELECTOR = "on.![full]";
    public static final MetadataFieldName glamItem = new MetadataFieldName("glam", "item");
    public static final MetadataFieldName glamBitstream = new MetadataFieldName("glam", "bitstream");
    public static final MetadataFieldName glamContributor =
        new MetadataFieldName("glam", "contributor", "annotation");
    public static final MetadataFieldName glamAnnotationPosition =
        new MetadataFieldName("glam", "annotation", "position");

    public static final String CREATED_SELECTOR = "created";
    public static final MetadataFieldName dateIssued = new MetadataFieldName("dc", "date", "issued");

    public static final String MODIFIED_SELECTOR = "modified";
    public static final MetadataFieldName dateModified = new MetadataFieldName("dcterms", "modified");

    public static final String DEFAULTSELECTOR_VALUE = "on.![selector.defaultSelector.value]";
    public static final MetadataFieldName annotationPosition =
        new MetadataFieldName("glam", "annotation", "position");

    public static final String ON_SELECTOR_ITEM_VALUE = "on.![selector.item.value]";
    public static final MetadataFieldName svgSelector = new MetadataFieldName("glam", "annotation", "svgselector");

    public static final String RESOURCE_CHARS = "resource.![chars]";
    public static final MetadataFieldName descriptionAbstract = new MetadataFieldName("dc", "description", "abstract");

    public static final String RESOURCE_FULLTEXT = "resource.![fullText]";
    public static final MetadataFieldName annotationFulltext = new MetadataFieldName("glam", "annotation", "fulltext");
    public static final MetadataFieldName dcTitle = new MetadataFieldName("dc", "title");

    private ItemEnricherFactory() { }

    public static ItemEnricher annotationItemEnricher(ConfigurationService configurationService) {
        return new ItemEnricherComposite(
            List.of(
                skipFullfilledMetadata(glamItem, glamItemMetadataEnricher()),
                skipFullfilledMetadata(glamBitstream, glamBitstreamMetadataEnricher()),
                issueDateEnricher(),
                modifiedDateEnricher(),
                fragmentSelectorEnricher(),
                svgSelectorEnricher(),
                resourceTextEnricher(),
                fulltextEnricher(),
                dcTitleEnricher(configurationService)
            )
        );
    }

    public static ItemEnricher skipFullfilledMetadata(MetadataFieldName metadata, ItemEnricher itemEnricher) {
        return new ItemEnricherFilter(hasEmptyMetadata(metadata), itemEnricher);
    }

    public static Predicate<Item> hasEmptyMetadata(MetadataFieldName metadata) {
        return (item) -> StringUtils.isEmpty(item.getItemService().getMetadata(item, metadata.toString()));
    }

    public static ItemEnricher glamBitstreamMetadataEnricher() {
        return new MetadataAuthorityItemEnricher(
            glamBitstream,
            (context, annotation) ->
                new MetadataPatternGroupExtractor(BITSTREAM_PATTERN)
                    .extract(annotation.on.get(0).full),
            (context, annotation) -> {
                String uuid = new MetadataPatternGroupExtractor(BITSTREAM_PATTERN)
                    .extract(annotation.on.get(0).full);
                if (uuid == null) {
                    return null;
                }
                Bitstream bitstream = null;
                try {
                    BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
                    bitstream = bitstreamService.find(context, UUID.fromString(uuid));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                if (bitstream == null) {
                    return null;
                }
                return bitstream.getName();
            }

        );
    }

    public static ItemEnricher glamItemMetadataEnricher() {
        return new MetadataAuthorityItemEnricher(
            glamItem,
            (context, annotation) ->
                new MetadataPatternGroupExtractor(ITEM_PATTERN)
                    .extract(annotation.on.get(0).full),
            (context, annotation) -> {
                String uuid = new MetadataPatternGroupExtractor(ITEM_PATTERN)
                    .extract(annotation.on.get(0).full);
                if (uuid == null) {
                    return null;
                }
                Item item = null;
                try {
                    item = ContentServiceFactory.getInstance().getItemService().find(context, UUID.fromString(uuid));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                if (item == null) {
                    return null;
                }
                return item.getName();
            }

        );
    }

    public static ItemEnricher glamContributorEnricher() {
        return new MetadataItemContextUserEnricher(glamContributor);
    }

    public static ItemEnricher issueDateEnricher() {
        return new MetadataItemLocalDateTimeEnricher(
            CREATED_SELECTOR, dateIssued, DATETIME_FORMATTER
        );
    }

    public static ItemEnricher modifiedDateEnricher() {
        return new MetadataItemLocalDateTimeEnricher(
            MODIFIED_SELECTOR, dateModified, DATETIME_FORMATTER
        );
    }

    public static ItemEnricher fragmentSelectorEnricher() {
        return new MetadataItemEnricher(
            DEFAULTSELECTOR_VALUE,
            annotationPosition,
            List.class
        );
    }

    public static ItemEnricher svgSelectorEnricher() {
        return new MetadataItemEnricher(
            ON_SELECTOR_ITEM_VALUE,
            svgSelector,
            List.class
        );
    }

    public static ItemEnricher resourceTextEnricher() {
        return new MetadataItemEnricher(
            RESOURCE_CHARS,
            descriptionAbstract,
            List.class
        );
    }

    public static ItemEnricher fulltextEnricher() {
        return  new MetadataItemEnricher(
            RESOURCE_FULLTEXT,
            annotationFulltext,
            List.class
        );
    }

    public static ItemEnricher dcTitleEnricher(ConfigurationService configurationService) {
        return new MetadataItemEnricher(
            RESOURCE_FULLTEXT,
            dcTitle,
            List.class,
            getAnnotationTitleReducer(configurationService)
        );
    }

}
