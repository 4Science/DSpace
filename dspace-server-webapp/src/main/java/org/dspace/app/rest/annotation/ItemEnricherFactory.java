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

import org.dspace.content.MetadataFieldName;

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

    public static final String CREATED_SELECTOR = "created";
    public static final MetadataFieldName dateIssued = new MetadataFieldName("dc", "date", "issued");

    public static final String MODIFIED_SELECTOR = "modified";
    public static final MetadataFieldName dateModified = new MetadataFieldName("dcterms", "modified");

    public static final String DEFAULTSELECTOR_VALUE = "on.![selector.defaultSelector.value]";
    public static final MetadataFieldName fragmentSelector =
        new MetadataFieldName("glam", "annotation", "fragmentselector");

    public static final String ON_SELECTOR_ITEM_VALUE = "on.![selector.item.value]";
    public static final MetadataFieldName svgSelector = new MetadataFieldName("glam", "annotation", "svgselector");

    public static final String RESOURCE_CHARS = "resource.![chars]";
    public static final MetadataFieldName annotationText = new MetadataFieldName("glam", "annotation", "text");

    public static final String RESOURCE_FULLTEXT = "resource.![fullText]";
    public static final MetadataFieldName annotationFulltext = new MetadataFieldName("glam", "annotation", "fulltext");
    public static final MetadataFieldName dcTitle = new MetadataFieldName("dc", "title");

    private ItemEnricherFactory() { }

    public static ItemEnricher annotationItemEnricher() {
        return new ItemEnricherComposite(
            List.of(
                glamItemMetadataEnricher(),
                glamBitstreamMetadataEnricher(),
                issueDateEnricher(),
                modifiedDateEnricher(),
                fragmentSelectorEnricher(),
                svgSelectorEnricher(),
                resourceTextEnricher(),
                fulltextEnricher(),
                dcTitleEnricher()
            )
        );
    }

    public static ItemEnricher glamItemMetadataEnricher() {
        return new MetadataItemPatternGroupEnricher(
            FULL_SELECTOR, glamItem, String.class, ITEM_PATTERN
        );
    }

    public static ItemEnricher glamBitstreamMetadataEnricher() {
        return new MetadataItemPatternGroupEnricher(
            FULL_SELECTOR, glamBitstream, String.class, BITSTREAM_PATTERN
        );
    }

    // TODO-VINS: add enhancer for the glam.annotation.position based on number of related annotations
    // TODO-VINS: add enhancer for the glam.contributor.annotation based on logged-in user

    public static ItemEnricher issueDateEnricher() {
        return new MetadataItemEnricher(
            CREATED_SELECTOR, dateIssued, LocalDateTime.class
        );
    }

    public static ItemEnricher modifiedDateEnricher() {
        return new MetadataItemEnricher(
            MODIFIED_SELECTOR, dateModified, LocalDateTime.class
        );
    }

    public static ItemEnricher fragmentSelectorEnricher() {
        return new MetadataItemEnricher(
            DEFAULTSELECTOR_VALUE,
            fragmentSelector,
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
            annotationText,
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

    public static ItemEnricher dcTitleEnricher() {
        return  new MetadataItemEnricher(
            RESOURCE_FULLTEXT,
            dcTitle,
            List.class,
            (s) -> s.length() > 10 ? s.substring(0, 10) + "..." : s
        );
    }

}
