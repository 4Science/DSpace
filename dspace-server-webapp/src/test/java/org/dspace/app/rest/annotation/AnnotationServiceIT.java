package org.dspace.app.rest.annotation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.matcher.MetadataValueMatcher;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class AnnotationServiceIT extends AbstractIntegrationTestWithDatabase {

    public static final String ANNOTATION_HANDLE = "123456789/annotation-collection";
    public static final String ANNOTATION_ENTITY_TYPE = "Annotation";
    private static final String BASE_TEST_DIR =
        "/home/vins/dev/projects/DSpace7/dspace-test/dspace/assetstore/annotation/";
    private static final Logger log = LogManager.getLogger(AnnotationServiceIT.class);
    private static ObjectMapper mapper;
    private static AnnotationRest validAnnotation;
    private Collection collection;
    private AnnotationService annotationService =
        DSpaceServicesFactory.getInstance().getServiceManager().getServicesByType(AnnotationService.class).get(0);
    private WorkspaceItemService workspaceItemService =
        ContentServiceFactory.getInstance().getWorkspaceItemService();
    private ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    @BeforeClass
    public static void setupTest() throws Exception {
        mapper = new ObjectMapper();
        try (FileInputStream fileInputStream = getFileInputStream("valid-create.json")) {
            validAnnotation = mapper.readValue(fileInputStream.readAllBytes(), AnnotationRest.class);
        }
    }

    private static FileInputStream getFileInputStream(String name) throws FileNotFoundException {
        return new FileInputStream(new File(BASE_TEST_DIR, name));
    }

    @Before
    public void setup() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        collection =
            CollectionBuilder.createCollection(context, parentCommunity, ANNOTATION_HANDLE)
                             .withEntityType(ANNOTATION_ENTITY_TYPE)
                             .build();
        context.restoreAuthSystemState();
    }

    @Test
    public void testFailedCreationWithoutConfiguration() {
        configurationService.setProperty(AnnotationService.ANNOTATION_COLLECTION, null);
        configurationService.setProperty(AnnotationService.ANNOTATION_ENTITY_TYPE, null);

        MatcherAssert.assertThat(
            configurationService.getProperty(AnnotationService.ANNOTATION_COLLECTION),
            CoreMatchers.nullValue()
        );
        MatcherAssert.assertThat(
            configurationService.getProperty(AnnotationService.ANNOTATION_ENTITY_TYPE),
            CoreMatchers.nullValue()
        );
        Assert.assertThrows(
            IllegalArgumentException.class,
            () -> this.annotationService.create(context, new AnnotationRest())
        );
    }

    @Test
    public void testCreationWithEntityType() {
        configurationService.setProperty(AnnotationService.ANNOTATION_COLLECTION, null);
        configurationService.setProperty(AnnotationService.ANNOTATION_ENTITY_TYPE, ANNOTATION_ENTITY_TYPE);
        WorkspaceItem workspaceItem = null;
        try {
            workspaceItem = this.annotationService.create(context, new AnnotationRest());
            MatcherAssert.assertThat(
                workspaceItem, CoreMatchers.notNullValue()
            );
            MatcherAssert.assertThat(
                workspaceItem.getItem(), CoreMatchers.notNullValue()
            );
            MatcherAssert.assertThat(
                workspaceItem.getCollection().getID(), CoreMatchers.is(this.collection.getID())
            );
        } finally {
            try {
                workspaceItemService.deleteAll(context, workspaceItem);
            } catch (Exception e) {
                log.error("Cannot delete the created annotation workspace item", e);
            }
        }
    }

    @Test
    public void testCreationWithCollectionHandle() {
        configurationService.setProperty(AnnotationService.ANNOTATION_COLLECTION, ANNOTATION_HANDLE);
        configurationService.setProperty(AnnotationService.ANNOTATION_ENTITY_TYPE, null);
        WorkspaceItem workspaceItem = null;
        try {
            workspaceItem = this.annotationService.create(context, new AnnotationRest());
            MatcherAssert.assertThat(
                workspaceItem, CoreMatchers.notNullValue()
            );
            MatcherAssert.assertThat(
                workspaceItem.getItem(), CoreMatchers.notNullValue()
            );
            MatcherAssert.assertThat(
                workspaceItem.getCollection().getID(), CoreMatchers.is(this.collection.getID())
            );
        } finally {
            try {
                workspaceItemService.deleteAll(context, workspaceItem);
            } catch (Exception e) {
                log.error("Cannot delete the created annotation workspace item", e);
            }
        }
    }

    @Test
    public void testCreationWithCollectionIdentifier() {
        configurationService.setProperty(AnnotationService.ANNOTATION_COLLECTION, collection.getID());
        configurationService.setProperty(AnnotationService.ANNOTATION_ENTITY_TYPE, null);
        WorkspaceItem workspaceItem = null;
        try {
            workspaceItem = this.annotationService.create(context, new AnnotationRest());
            MatcherAssert.assertThat(
                workspaceItem, CoreMatchers.notNullValue()
            );
            MatcherAssert.assertThat(
                workspaceItem.getItem(), CoreMatchers.notNullValue()
            );
            MatcherAssert.assertThat(
                workspaceItem.getCollection().getID(), CoreMatchers.is(this.collection.getID())
            );
        } finally {
            try {
                workspaceItemService.deleteAll(context, workspaceItem);
            } catch (Exception e) {
                log.error("Cannot delete the created annotation workspace item", e);
            }
        }
    }

    /*{
        // static
        "@context": "http://iiif.io/api/presentation/2/context.json",
        // static
        "@type": "oa:Annotation",
        // static
        "motivation": "oa:commenting",
        "on": {
            // static
            "@type": "oa:SpecificResource",
            // computed at runtime with bitstream uuid
            "full": "http://localhost:8080/server/iiif/af5b8b9a-3883-4764-965c-248f1f1f1546/canvas/3c9e76fd-0ef7-4df7
            -af7a-7356220e2451",
            "selector": {
                // static
                "@type": "oa:Choice",
                "default": {
                    // static
                    "@type": "oa:FragmentSelector",
                    // needs to be stored!
                    "value": "xywh=139,29,52,41"
                },
                "item": {
                    // static
                    "@type": "oa:SvgSelector",
                    // needs to be stored!
                    "value": "<svg xmlns='http://www.w3.org/2000/svg'><path xmlns=\"http://www.w3.org/2000/svg\"
                    d=\"M139.39024,71.02439v-41.70732h52.68293v41.70732z\" data-paper-data=\"{&quot;state&quot;
                    :null}\" fill=\"none\" fill-rule=\"nonzero\" stroke=\"#00bfff\" stroke-width=\"1\"
                    stroke-linecap=\"butt\" stroke-linejoin=\"miter\" stroke-miterlimit=\"10\" stroke-dasharray=\"\"
                    stroke-dashoffset=\"0\" font-family=\"none\" font-weight=\"none\" font-size=\"none\"
                    text-anchor=\"none\" style=\"mix-blend-mode: normal\"/></svg>"
                }
        }
    },
    "resource": {
        // needs to be stored! - resource.chars & resource.fulltext
        "chars": "<p>Test</p>",
        // static
        "@type": "dctypes:Text"
    }
    }*/
    @Test
    public void testCreationWithMetadata() {
        configurationService.setProperty(AnnotationService.ANNOTATION_COLLECTION, collection.getID());
        configurationService.setProperty(AnnotationService.ANNOTATION_ENTITY_TYPE, null);

        MetadataFieldName dateIssued =
            new MetadataFieldName("dc", "date", "issued");
        String dateIssuedSelector = "created";
        MetadataItemEnricher creationDate =
            new MetadataItemEnricher(
                dateIssuedSelector,
                dateIssued,
                LocalDateTime.class
            );
        MetadataFieldName dateModified =
            new MetadataFieldName("dcterms", "modified");
        String dateModifiedSelector = "modified";

        MetadataItemEnricher modifiedDate =
            new MetadataItemEnricher(
                dateModifiedSelector,
                dateModified,
                LocalDateTime.class
            );

        MetadataFieldName fragmentSelectorMetadata =
            new MetadataFieldName("glam", "annotation", "fragmentselector");
        String defaultSelectorValueSpel = "on.![selector.defaultSelector.value]";
        MetadataItemEnricher fragmentSelector =
            new MetadataItemEnricher(
                defaultSelectorValueSpel,
                fragmentSelectorMetadata,
                List.class
            );
        MetadataFieldName svgSelectorMetadata = new MetadataFieldName("glam", "annotation", "svgselector");
        String itemSelectorValueSpel = "on.![selector.item.value]";
        MetadataItemEnricher svgSelector =
            new MetadataItemEnricher(
                itemSelectorValueSpel,
                svgSelectorMetadata,
                List.class
            );
        MetadataFieldName textMetadata = new MetadataFieldName("glam", "annotation", "text");
        String charsSpel = "resource.![chars]";
        MetadataItemEnricher resourceText =
            new MetadataItemEnricher(
                charsSpel,
                textMetadata,
                List.class
            );
        MetadataFieldName fulltextMetadata = new MetadataFieldName("glam", "annotation", "fulltext");
        String fulltextSpel = "resource.![fullText]";
        MetadataItemEnricher resourceFullText =
            new MetadataItemEnricher(
                fulltextSpel,
                fulltextMetadata,
                List.class
            );

        ItemEnricher itemEnricher =
            new ItemEnricherComposite(
                List.of(
                    fragmentSelector,
                    svgSelector,
                    creationDate,
                    modifiedDate,
                    resourceText,
                    resourceFullText
                )
            );
        WorkspaceItem workspaceItem = null;
        try {
            workspaceItem = this.annotationService.create(context, validAnnotation, itemEnricher);
            MatcherAssert.assertThat(
                workspaceItem, CoreMatchers.notNullValue()
            );
            Item item = workspaceItem.getItem();
            MatcherAssert.assertThat(
                item, CoreMatchers.notNullValue()
            );
            MatcherAssert.assertThat(
                workspaceItem.getCollection().getID(), CoreMatchers.is(this.collection.getID())
            );
            MatcherAssert.assertThat(
                item.getMetadata(),
                CoreMatchers.allOf(
                    CoreMatchers.hasItem(
                        MetadataValueMatcher.with(
                            "dc.date.issued", validAnnotation.created.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    ),
                    CoreMatchers.hasItem(
                        MetadataValueMatcher.with(
                            "dcterms.modified", validAnnotation.modified.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    ),
                    CoreMatchers.hasItem(
                        MetadataValueMatcher.with("glam.annotation.fragmentselector", "xywh=139,29,52,41")
                    ),
                    CoreMatchers.hasItem(
                        MetadataValueMatcher.with(
                            "glam.annotation.svgselector",
                            "<svg xmlns='http://www.w3.org/2000/svg'><path xmlns=\"http://www.w3.org/2000/svg\" " +
                            "d=\"M139.39024,71.02439v-41.70732h52.68293v41.70732z\" data-paper-data=\"{&quot;" +
                            "state&quot;:null}\" fill=\"none\" fill-rule=\"nonzero\" stroke=\"#00bfff\" " +
                            "stroke-width=\"1\" stroke-linecap=\"butt\" stroke-linejoin=\"miter\" " +
                            "stroke-miterlimit=\"10\" stroke-dasharray=\"\" stroke-dashoffset=\"0\" " +
                            "font-family=\"none\" font-weight=\"none\" font-size=\"none\" text-anchor=\"none\" " +
                            "style=\"mix-blend-mode: normal\"/></svg>"
                        )
                    ),
                    CoreMatchers.hasItem(
                        MetadataValueMatcher.with("glam.annotation.text", "<p>Test</p>")
                    ),
                    CoreMatchers.hasItem(
                        MetadataValueMatcher.with("glam.annotation.fulltext", "Test")
                    )
                )
            );
            AnnotationRestMapper annotationRestMapper =
                new AnnotationRestMapper(
                    List.of(
                        new AnnotationBodyRestEnricher(
                            "chars",
                            textMetadata,
                            String.class
                        ),
                        new AnnotationBodyRestEnricher(
                            "fullText",
                            fulltextMetadata,
                            String.class
                        )
                    ),
                    List.of(
                        new AnnotationTargetRestEnricher(
                            "selector.defaultSelector.value",
                            fragmentSelectorMetadata,
                            String.class
                        ),
                        new AnnotationTargetRestEnricher(
                            "selector.item.value",
                            svgSelectorMetadata,
                            String.class
                        )
                    ),
                    List.of(
                        new AnnotationFieldEnricher("getID()", "id"),
                        new AnnotationLocalDateTimeMetadataEnricher(
                            "modified",
                            dateModified,
                            LocalDateTime.class
                        ),
                        new AnnotationLocalDateTimeMetadataEnricher(
                            "created",
                            dateIssued,
                            LocalDateTime.class
                        )
                    )
                );


            AnnotationRest annotationRest = annotationRestMapper.map(context, item);

            MatcherAssert.assertThat(
                annotationRest, CoreMatchers.notNullValue()
            );
            MatcherAssert.assertThat(
                annotationRest.created.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                CoreMatchers.is(
                    item.getItemService().getMetadata(item, dateIssued.toString())
                )
            );
            MatcherAssert.assertThat(
                annotationRest.modified.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                CoreMatchers.is(
                    item.getItemService().getMetadata(item, dateModified.toString())
                )
            );
            MatcherAssert.assertThat(
                annotationRest.on.get(0).selector.item.value,
                CoreMatchers.is(
                    "<svg xmlns='http://www.w3.org/2000/svg'><path xmlns=\"http://www.w3.org/2000/svg\" " +
                    "d=\"M139.39024,71.02439v-41.70732h52.68293v41.70732z\" data-paper-data=\"{&quot;" +
                    "state&quot;:null}\" fill=\"none\" fill-rule=\"nonzero\" stroke=\"#00bfff\" " +
                    "stroke-width=\"1\" stroke-linecap=\"butt\" stroke-linejoin=\"miter\" " +
                    "stroke-miterlimit=\"10\" stroke-dasharray=\"\" stroke-dashoffset=\"0\" " +
                    "font-family=\"none\" font-weight=\"none\" font-size=\"none\" text-anchor=\"none\" " +
                    "style=\"mix-blend-mode: normal\"/></svg>"

                )
            );
            MatcherAssert.assertThat(
                annotationRest.on.get(0).selector.defaultSelector.value,
                CoreMatchers.is(
                    "xywh=139,29,52,41"
                )
            );
            MatcherAssert.assertThat(
                annotationRest.resource.get(0).chars,
                CoreMatchers.is(
                    "<p>Test</p>"
                )
            );
            MatcherAssert.assertThat(
                annotationRest.resource.get(0).fullText,
                CoreMatchers.is(
                    "Test"
                )
            );

        } finally {
            try {
                workspaceItemService.deleteAll(context, workspaceItem);
            } catch (Exception e) {
                log.error("Cannot delete the created annotation workspace item", e);
            }
        }
    }


    @Test
    public void testCreationWithUUIDExtraction() {
        configurationService.setProperty(AnnotationService.ANNOTATION_COLLECTION, collection.getID());
        configurationService.setProperty(AnnotationService.ANNOTATION_ENTITY_TYPE, null);

        ItemEnricherComposite itemEnrichers = getItemEnricherUUID();

        BiConsumer<Context, Item> contextItemBiConsumer = itemEnrichers.apply(validAnnotation);

        WorkspaceItem workspaceItem = null;
        try {
            workspaceItem = this.annotationService.create(context, validAnnotation, itemEnrichers);
            MatcherAssert.assertThat(
                workspaceItem, CoreMatchers.notNullValue()
            );
            Item item = workspaceItem.getItem();
            MatcherAssert.assertThat(
                item, CoreMatchers.notNullValue()
            );
            MatcherAssert.assertThat(
                workspaceItem.getCollection().getID(), CoreMatchers.is(this.collection.getID())
            );

            contextItemBiConsumer.accept(context, item);

            MatcherAssert.assertThat(
                item.getMetadata(),
                CoreMatchers.allOf(
                    CoreMatchers.hasItem(
                        MetadataValueMatcher.with("glam.item", "af5b8b9a-3883-4764-965c-248f1f1f1546")
                    ),
                    CoreMatchers.hasItem(
                        MetadataValueMatcher.with("glam.bitstream", "3c9e76fd-0ef7-4df7-af7a-7356220e2451")
                    )
                )
            );

            configurationService.setProperty("dspace.server.url", "http://localhost:8080/server");

            AnnotationRestMapper mapper =
                new AnnotationRestMapper(
                    List.of(),
                    List.of(
                        new AnnotationTargetRestComposedEnricher<String>(
                            "full",
                            List.of(
                                (i) -> configurationService.getProperty("dspace.ui.url") + "/iiif/",
                                (i) -> i.getItemService().getMetadata(i, "glam.item") + "/canvas/",
                                (i) -> i.getItemService().getMetadata(i, "glam.bitstream")
                            ),
                            (a, b) -> a + b
                        )
                    ),
                    List.of(
                        new AnnotationFieldComposerEnricher<String>(
                            "id",
                            List.of(
                                (i) -> configurationService.getProperty("dspace.server.url") + "/annotation/",
                                (i) -> i.getID().toString()
                            ),
                            (a, b) -> a + b
                        )
                    )
                );

            AnnotationRest annotation = mapper.map(context, item);

            MatcherAssert.assertThat(
                annotation.on.get(0).full,
                CoreMatchers.is(
                    "http://localhost:4000/iiif/af5b8b9a-3883-4764-965c-248f1f1f1546/canvas/3c9e76fd-0ef7-4df7-af7a-7356220e2451"
                )
            );
            MatcherAssert.assertThat(
                annotation.id,
                CoreMatchers.is(
                    "http://localhost:8080/server/annotation/" + item.getID()
                )
            );

        } finally {
            try {
                workspaceItemService.deleteAll(context, workspaceItem);
            } catch (Exception e) {
                log.error("Cannot delete the created annotation workspace item", e);
            }
        }
    }

    private static ItemEnricherComposite getItemEnricherUUID() {
        MetadataFieldName glamItem = new MetadataFieldName("glam", "item");
        String fullIdentifierSelector = "on.![full]";
        MetadataItemEnricher glamItemEnricher =
            new MetadataItemPatternGroupEnricher(
                fullIdentifierSelector,
                glamItem,
                String.class,
                AnnotationService.ITEM_PATTERN
            );

        MetadataFieldName glamBitstream = new MetadataFieldName("glam", "bitstream");
        MetadataItemEnricher glamBitstreamEnricher =
            new MetadataItemPatternGroupEnricher(
                fullIdentifierSelector,
                glamBitstream,
                String.class,
                AnnotationService.BITSTREAM_PATTERN
            );

        return new ItemEnricherComposite(List.of(glamItemEnricher, glamBitstreamEnricher));
    }


}