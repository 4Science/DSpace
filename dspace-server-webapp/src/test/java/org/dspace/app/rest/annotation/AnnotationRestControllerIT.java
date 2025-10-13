/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.dspace.app.rest.annotation.AnnotationRestControllerIT.ResourcePolicyMatcher.isResourcePolicy;
import static org.dspace.app.rest.annotation.AnnotationRestDeserializer.DATETIME_FORMAT;
import static org.dspace.app.rest.annotation.AnnotationRestDeserializer.DATETIME_FORMATTER;
import static org.dspace.app.rest.annotation.AnnotationService.PERSONAL_ANNOTATION_COLLECTION;
import static org.dspace.app.rest.annotation.AnnotationService.PERSONAL_ANNOTATION_GROUP;
import static org.dspace.core.Constants.ADD;
import static org.dspace.core.Constants.ADMIN;
import static org.dspace.core.Constants.DEFAULT_BITSTREAM_READ;
import static org.dspace.core.Constants.DEFAULT_ITEM_READ;
import static org.dspace.core.Constants.DELETE;
import static org.dspace.core.Constants.READ;
import static org.dspace.core.Constants.REMOVE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.dspace.app.matcher.MetadataValueMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class contains some ITs for the {@link AnnotationRestController} responsible
 * for all the HTTP calls made through {@linkplain /api/annotation}
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class AnnotationRestControllerIT extends AbstractControllerIntegrationTest {


    private static final String BASE_TEST_DIR = "./target/testing/dspace/assetstore/annotation/";
    public static final String PERSONAL_ANNOTATION = "PersonalAnnotation";
    public static final String WEB_ANNOTATION = "WebAnnotation";

    ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    ItemService itemService =
        ContentServiceFactory.getInstance().getItemService();

    Item item1;
    Bitstream bitstream1;
    AtomicReference<String> idRef;

    ObjectMapper mapper;
    AnnotationRest validAnnotation;

    @Autowired
    AnnotationService annotationService;
    @Autowired
    AuthorizeService authorizeService;

    @Before
    public void setup() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("Test Community")
                            .build();

        Collection annotations =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Annotations")
                             .withEntityType("WebAnnotation")
                             .build();

        item1 =
            ItemBuilder.createItem(context, annotations)
                       .withTitle("Item 1")
                       .build();

        bitstream1 =
            BitstreamBuilder.createBitstream(context, item1, toInputStream("TEST", StandardCharsets.UTF_8))
                            .withName("Item 1")
                            .build();

        context.restoreAuthSystemState();

        configurationService.setProperty(AnnotationService.ANNOTATION_COLLECTION, annotations.getID());
        configurationService.setProperty(AnnotationService.ANNOTATION_ENTITY_TYPE, WEB_ANNOTATION);
        configurationService.setProperty(AnnotationService.PERSONAL_ANNOTATION_ENTITY_TYPE, PERSONAL_ANNOTATION);
        idRef = new AtomicReference<>();

        mapper = new ObjectMapper();

        // Create a simple module to register custom serializer/deserializer
        SimpleModule module = new SimpleModule();

        // Register custom deserializer for AnnotationRest if needed
        module.addDeserializer(AnnotationRest.class, new AnnotationRestDeserializer());
        module.addDeserializer(AnnotationBodyRest.class, new AnnotationBodyRestDeserializer());

        // Register the module with the mapper
        mapper.registerModule(module);

        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setTimeZone(TimeZone.getDefault());

        SimpleDateFormat dateFormat = new SimpleDateFormat(DATETIME_FORMAT);
        mapper.setDateFormat(dateFormat);

    }

    @After
    public void tearDown() throws Exception {
        context.turnOffAuthorisationSystem();

        deleteIdRef(idRef);

        context.restoreAuthSystemState();
    }

    private void deleteIdRef(AtomicReference<String> idRef) {
        String id = idRef.get();
        if (id != null) {
            Item dso = this.annotationService.findById(context, id);
            if (dso != null) {
                this.annotationService.delete(context, dso);
            }
        }
    }

    @Test
    public void testCreate() throws Exception {

        String full =
            configurationService.getProperty("dspace.server.url") +
                "/iiif/" + item1.getID() + "/canvas/" + bitstream1.getID();
        try (FileInputStream fileInputStream = getFileInputStream("valid-create.json")) {
            validAnnotation = mapper.readValue(fileInputStream.readAllBytes(), AnnotationRest.class);
            validAnnotation.on.get(0).full = full;
        }

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token)
            .perform(
                post("/annotation/create")
                    .content(mapper.writeValueAsBytes(validAnnotation))
                    .contentType("application/json;charset=UTF-8")
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(
                jsonPath("$",
                    allOf(
                        hasJsonPath("$.['@id']", notNullValue()),
                        hasJsonPath("$.['@type']", is("oa:Annotation")),
                        hasJsonPath("$.['@context']", is("http://iiif.io/api/presentation/2/context.json")),
                        hasJsonPath("$.['motivation']", hasItem("oa:commenting"))
                    )
                )
            )
            .andExpect(
                jsonPath(
                    "$.on",
                    hasItem(
                        allOf(
                            hasJsonPath("$.['@type']", is("oa:SpecificResource")),
                            hasJsonPath(
                                "$.['full']", is(full)
                            ),
                            hasJsonPath("$.['selector']",
                                allOf(
                                    hasJsonPath("$.['@type']", is("oa:Choice")),
                                    hasJsonPath(
                                        "$.['default']",
                                        allOf(
                                            hasJsonPath("$.['@type']", is("oa:FragmentSelector")),
                                            hasJsonPath("$.['value']", is("xywh=139,29,52,41"))
                                        )
                                    ),
                                    hasJsonPath(
                                        "$.['item']",
                                        allOf(
                                            hasJsonPath("$.['@type']", is("oa:SvgSelector")),
                                            hasJsonPath("$.['value']", not(emptyOrNullString()))
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
            .andExpect(
                jsonPath(
                    "$.resource",
                    hasItem(
                        allOf(
                            hasJsonPath("$.['@type']", is("dctypes:Text")),
                            hasJsonPath("$.['chars']", is("<p>Test</p>")),
                            hasJsonPath("$.['http://dev.llgc.org.uk/sas/full_text']", is("Test"))
                        )
                    )
                )
            )
            .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.['@id']")));
        ;
    }

    @Test
    public void testSearch() throws Exception {

        String full =
            configurationService.getProperty("dspace.server.url") +
                "/iiif/" + item1.getID() + "/canvas/" + bitstream1.getID();
        try (FileInputStream fileInputStream = getFileInputStream("valid-create.json")) {
            validAnnotation = mapper.readValue(fileInputStream.readAllBytes(), AnnotationRest.class);
            validAnnotation.on.get(0).full = full;
        }

        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = this.annotationService.create(context, validAnnotation);
        context.restoreAuthSystemState();
        context.commit();
        Item annotation = null;
        try {
            context.reloadEntity(workspaceItem);

            annotation = workspaceItem.getItem();

            String dateIssued = itemService.getMetadata(annotation, "dc.date.issued");
            String modified = itemService.getMetadata(annotation, "dcterms.modified");
            String svgSelector = itemService.getMetadata(annotation, "glam.annotation.svgselector");
            String fragmentSelector = itemService.getMetadata(annotation, "glam.annotation.position");
            String descriptionAbstract = itemService.getMetadata(annotation, "dc.description.abstract");
            String fullText = itemService.getMetadata(annotation, "glam.annotation.fulltext");


            getClient()
                .perform(
                    get("/annotation/search")
                        .param("uri", full)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(
                    jsonPath(
                        "$[*]",
                        hasItem(
                            allOf(
                                hasJsonPath("$.['@id']", containsString(annotation.getID().toString())),
                                hasJsonPath("$.['@type']", is("oa:Annotation")),
                                hasJsonPath("$.['@context']", is("http://iiif.io/api/presentation/2/context.json")),
                                hasJsonPath("$.['motivation']", hasItem("oa:commenting")),
                                hasJsonPath("$.['dcterms:created']", is(dateIssued)),
                                hasJsonPath("$.['dcterms:modified']", is(modified))
                            )
                        )
                    )
                )
                .andExpect(
                    jsonPath(
                        "$[*].on[*]",
                        hasItem(
                            allOf(
                                hasJsonPath("$.['@type']", is("oa:SpecificResource")),
                                hasJsonPath(
                                    "$.['full']", is(full)
                                ),
                                hasJsonPath(
                                    "$.['selector']",
                                    allOf(
                                        hasJsonPath("$.['@type']", is("oa:Choice")),
                                        hasJsonPath(
                                            "$.['default']",
                                            allOf(
                                                hasJsonPath("$.['@type']", is("oa:FragmentSelector")),
                                                hasJsonPath("$.['value']", is(fragmentSelector))
                                            )
                                        ),
                                        hasJsonPath(
                                            "$.['item']",
                                            allOf(
                                                hasJsonPath("$.['@type']", is("oa:SvgSelector")),
                                                hasJsonPath("$.['value']", is(svgSelector))
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                ).andExpect(
                    jsonPath(
                        "$[*].resource[*]",
                        hasItem(
                            allOf(
                                hasJsonPath("$.['@type']", is("dctypes:Text")),
                                hasJsonPath("$.['chars']", is(descriptionAbstract)),
                                hasJsonPath("$.['http://dev.llgc.org.uk/sas/full_text']", is(fullText))
                            )
                        )
                    )
                );
        } finally {
            context.turnOffAuthorisationSystem();
            this.annotationService.delete(context, this.itemService.find(context, annotation.getID()));
            context.restoreAuthSystemState();
        }


    }

    @Test
    public void testUpdate() throws Exception {
        String full =
            configurationService.getProperty("dspace.server.url") +
                "/iiif/" + item1.getID() + "/canvas/" + bitstream1.getID();
        try (FileInputStream fileInputStream = getFileInputStream("valid-create.json")) {
            validAnnotation = mapper.readValue(fileInputStream.readAllBytes(), AnnotationRest.class);
            validAnnotation.on.get(0).full = full;
        }

        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = this.annotationService.create(context, validAnnotation);
        context.restoreAuthSystemState();
        context.commit();

        Item annotationItem = this.annotationService.findByItemId(context, workspaceItem.getItem().getID());
        AnnotationRest updatedAnnotation = this.annotationService.convert(context, annotationItem);

        updatedAnnotation.resource.get(0).chars = "<p>Updated Comment</p>";
        updatedAnnotation.resource.get(0).fullText = "Updated Comment";
        LocalDateTime created = updatedAnnotation.created;
        LocalDateTime modified = updatedAnnotation.modified;

        // update with no dates
        updatedAnnotation.created = null;
        updatedAnnotation.modified = null;

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(
                            post("/annotation/update")
                                .content(mapper.writeValueAsBytes(updatedAnnotation))
                                .contentType("application/json;charset=UTF-8")
                        )
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath(
                                "$",
                                 allOf(
                                     hasJsonPath(
                                         "$.['dcterms:created']",
                                         is(created.format(DATETIME_FORMATTER))
                                     )
                                 )
                            )
                        )
                        .andExpect(
                            jsonPath(
                                "$.resource",
                                hasItem(
                                    allOf(
                                        hasJsonPath("$.['@type']", is("dctypes:Text")),
                                        hasJsonPath("$.['chars']", is("<p>Updated Comment</p>")),
                                        hasJsonPath("$.['http://dev.llgc.org.uk/sas/full_text']",
                                                    is("Updated Comment")
                                        )
                                    )
                                )
                            )
                        )
                        .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.['@id']")));

        // update with modified date
        updatedAnnotation.modified = LocalDateTime.of(2025, 3, 19, 12, 0);
        getClient(token).perform(
                            post("/annotation/update")
                                .content(mapper.writeValueAsBytes(updatedAnnotation))
                                .contentType("application/json;charset=UTF-8")
                        )
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath(
                                "$",
                                allOf(
                                    hasJsonPath(
                                        "$.['dcterms:created']",
                                        is(created.format(DATETIME_FORMATTER))
                                    ),
                                    // gets reupdated if no creation date specified
                                    hasJsonPath(
                                        "$.['dcterms:modified']",
                                        not(is(updatedAnnotation.modified.format(DATETIME_FORMATTER)))
                                    )
                                )
                            )
                        )
                        .andExpect(
                            jsonPath(
                                "$.resource",
                                hasItem(
                                    allOf(
                                        hasJsonPath("$.['@type']", is("dctypes:Text")),
                                        hasJsonPath("$.['chars']", is("<p>Updated Comment</p>")),
                                        hasJsonPath("$.['http://dev.llgc.org.uk/sas/full_text']",
                                                    is("Updated Comment")
                                        )
                                    )
                                )
                            )
                        );

        // update with both date
        updatedAnnotation.created = created;
        updatedAnnotation.modified = LocalDateTime.of(2025, 3, 19, 14, 0);
        getClient(token).perform(
                            post("/annotation/update")
                                .content(mapper.writeValueAsBytes(updatedAnnotation))
                                .contentType("application/json;charset=UTF-8")
                        )
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath(
                                "$",
                                allOf(
                                    hasJsonPath(
                                        "$.['dcterms:created']",
                                        is(created.format(DATETIME_FORMATTER))
                                    ),
                                    // gets updated with the value provided
                                    hasJsonPath(
                                        "$.['dcterms:modified']",
                                        is(updatedAnnotation.modified.format(DATETIME_FORMATTER))
                                    )
                                )
                            )
                        )
                        .andExpect(
                            jsonPath(
                                "$.resource",
                                hasItem(
                                    allOf(
                                        hasJsonPath("$.['@type']", is("dctypes:Text")),
                                        hasJsonPath("$.['chars']", is("<p>Updated Comment</p>")),
                                        hasJsonPath("$.['http://dev.llgc.org.uk/sas/full_text']",
                                                    is("Updated Comment")
                                        )
                                    )
                                )
                            )
                        );

        annotationItem = this.annotationService.findByItemId(context, annotationItem.getID());
        assertThat(
            annotationItem.getMetadata(),
            allOf(
                hasItem(MetadataValueMatcher.with("dc.description.abstract", "<p>Updated Comment</p>")),
                hasItem(MetadataValueMatcher.with("glam.annotation.fulltext", "Updated Comment")),
                hasItem(MetadataValueMatcher.with("dc.title", "Updated Comment"))
            )
        );

        getClient(token)
            .perform(
                get("/annotation/search")
                    .param("uri", full)
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(
                jsonPath(
                    "$[*].resource[*]",
                    hasItem(
                        allOf(
                            hasJsonPath("$.['@type']", is("dctypes:Text")),
                            hasJsonPath("$.['chars']", is("<p>Updated Comment</p>")),
                            hasJsonPath("$.['http://dev.llgc.org.uk/sas/full_text']", is("Updated Comment"))
                        )
                    )
                )
            );

    }

    @Test
    public void testDifferentUsersUpdate() throws Exception {

        context.turnOffAuthorisationSystem();
        // submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                                         .withEmail("submitter@example.com")
                                         .withPassword("changeme")
                                         .withCanLogin(true)
                                         .build();
        context.restoreAuthSystemState();

        // impersonate submitter
        context.setCurrentUser(submitter);

        String full =
            configurationService.getProperty("dspace.server.url") +
                "/iiif/" + item1.getID() + "/canvas/" + bitstream1.getID();
        try (FileInputStream fileInputStream = getFileInputStream("valid-create.json")) {
            validAnnotation = mapper.readValue(fileInputStream.readAllBytes(), AnnotationRest.class);
            validAnnotation.on.get(0).full = full;
        }

        // create annotation with submitter
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = this.annotationService.create(context, validAnnotation);
        context.restoreAuthSystemState();
        context.commit();

        Item annotationItem = this.annotationService.findByItemId(context, workspaceItem.getItem().getID());
        AnnotationRest updatedAnnotation = this.annotationService.convert(context, annotationItem);
        idRef.set(updatedAnnotation.id);

        updatedAnnotation.resource.get(0).chars = "<p>Updated Comment</p>";
        updatedAnnotation.resource.get(0).fullText = "Updated Comment";

        // create another user
        context.turnOffAuthorisationSystem();
        String otherPassword = "changeme";
        EPerson otherUser =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("other@example.com")
                          .withPassword(otherPassword)
                          .build();
        context.restoreAuthSystemState();

        // try to update annotation with a different user
        String otherToken = getAuthToken(otherUser.getEmail(), otherPassword);
        getClient(otherToken).perform(
                            post("/annotation/update")
                                .content(mapper.writeValueAsBytes(updatedAnnotation))
                                .contentType("application/json;charset=UTF-8")
                        )
                        .andExpect(status().isUnauthorized());

        // check no update has been made
        getClient()
            .perform(
                get("/annotation/search")
                    .param("uri", full)
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(
                jsonPath(
                    "$[*].resource[*]",
                    hasItem(
                        allOf(
                            hasJsonPath("$.['@type']", is("dctypes:Text")),
                            hasJsonPath("$.['chars']", not(is("<p>Updated Comment</p>"))),
                            hasJsonPath("$.['http://dev.llgc.org.uk/sas/full_text']", not(is("Updated Comment")))
                        )
                    )
                )
            );

        // update using the submitter
        String token = getAuthToken(submitter.getEmail(), otherPassword);
        getClient(token).perform(
                                 post("/annotation/update")
                                     .content(mapper.writeValueAsBytes(updatedAnnotation))
                                     .contentType("application/json;charset=UTF-8")
                             )
                             .andExpect(status().isOk());

        // check update has been made correctly
        getClient()
            .perform(
                get("/annotation/search")
                    .param("uri", full)
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(
                jsonPath(
                    "$[*].resource[*]",
                    hasItem(
                        allOf(
                            hasJsonPath("$.['@type']", is("dctypes:Text")),
                            hasJsonPath("$.['chars']", is("<p>Updated Comment</p>")),
                            hasJsonPath("$.['http://dev.llgc.org.uk/sas/full_text']", is("Updated Comment"))
                        )
                    )
                )
            );

    }


    @Test
    public void testDelete() throws Exception {
        String full =
            configurationService.getProperty("dspace.server.url") +
                "/iiif/" + item1.getID() + "/canvas/" + bitstream1.getID();
        try (FileInputStream fileInputStream = getFileInputStream("valid-create.json")) {
            validAnnotation = mapper.readValue(fileInputStream.readAllBytes(), AnnotationRest.class);
            validAnnotation.on.get(0).full = full;
        }

        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = this.annotationService.create(context, validAnnotation);
        context.restoreAuthSystemState();
        context.commit();

        UUID annotationUUID = workspaceItem.getItem().getID();
        Item annotationItem = this.annotationService.findByItemId(context, annotationUUID);
        AnnotationRest annotationToDelete = this.annotationService.convert(context, annotationItem);

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token)
                .perform(
                    delete("/annotation/destroy")
                        .param("uri", annotationToDelete.id)
                )
                .andExpect(status().isNoContent());

        assertThat(
            this.annotationService.findByItemId(context, annotationUUID),
            nullValue()
        );
    }

    @Test
    public void testInvalidUserDelete() throws Exception {
        String full =
            configurationService.getProperty("dspace.server.url") +
                "/iiif/" + item1.getID() + "/canvas/" + bitstream1.getID();
        try (FileInputStream fileInputStream = getFileInputStream("valid-create.json")) {
            validAnnotation = mapper.readValue(fileInputStream.readAllBytes(), AnnotationRest.class);
            validAnnotation.on.get(0).full = full;
        }

        EPerson contextCurrentUser = context.getCurrentUser();
        context.setCurrentUser(admin);
        WorkspaceItem workspaceItem = this.annotationService.create(context, validAnnotation);
        context.commit();

        context.setCurrentUser(contextCurrentUser);

        UUID annotationUUID = workspaceItem.getItem().getID();
        Item annotationItem = this.annotationService.findByItemId(context, annotationUUID);
        AnnotationRest annotationToDelete = this.annotationService.convert(context, annotationItem);
        idRef.set(annotationToDelete.id);

        context.turnOffAuthorisationSystem();
        String otherPassword = "changeme";
        EPerson other = EPersonBuilder.createEPerson(context)
                                      .withCanLogin(true)
                                      .withEmail("other@example.com")
                                      .withPassword(otherPassword)
                                      .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(other.getEmail(), otherPassword);
        getClient(token)
            .perform(
                delete("/annotation/destroy")
                    .param("uri", annotationToDelete.id)
            )
            .andExpect(status().isUnauthorized());

        assertThat(
            this.annotationService.findByItemId(context, annotationUUID),
            notNullValue()
        );
    }

    @Test
    public void testDeleteNotFound() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token)
                .perform(
                    delete("/annotation/destroy")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDeleteInvalidURI() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token)
            .perform(
                delete("/annotation/destroy")
                    .param("uri", "invalid")
            )
            .andExpect(status().isBadRequest());

        getClient(token)
            .perform(
                delete("/annotation/destroy")
                    .param("uri", "")
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    public void testPersonalAnnotationCreate() throws Exception {

        context.turnOffAuthorisationSystem();

        Group annotazioniPersonali =
            GroupBuilder.createGroup(context)
                        .withName("Annotazioni Personali")
                        .build();


        EPerson userA =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("user.a@example.com")
                          .withPassword("changeme")
                          .withGroupMembership(annotazioniPersonali)
                          .build();

        EPerson userB =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("user.b@example.com")
                          .withPassword("changeme")
                          .withGroupMembership(annotazioniPersonali)
                          .build();

        EPerson userC =
            EPersonBuilder.createEPerson(context)
                          .withCanLogin(true)
                          .withEmail("user.c@example.com")
                          .withPassword("changeme")
                          .build();

        Group noUsers =
            GroupBuilder.createGroup(context)
                        .withName("NO USERS")
                        .build();

        Collection annotazioniPersonaliCol =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Annotazioni Personali")
                             .withEntityType(PERSONAL_ANNOTATION)
                             .build();

        // remove all default policies
        authorizeService.removeAllPolicies(context, annotazioniPersonaliCol);
        // these policy must be set manually on the collection Annotazioni Personali
        authorizeService.addPolicy(context, annotazioniPersonaliCol, READ, annotazioniPersonali);
        authorizeService.addPolicy(context, annotazioniPersonaliCol, ADD, annotazioniPersonali);
        authorizeService.addPolicy(context, annotazioniPersonaliCol, REMOVE, annotazioniPersonali);
        authorizeService.addPolicy(context, annotazioniPersonaliCol, DEFAULT_ITEM_READ, noUsers);
        authorizeService.addPolicy(context, annotazioniPersonaliCol, DEFAULT_BITSTREAM_READ, noUsers);

        context.commit();
        context.restoreAuthSystemState();

        configurationService.setProperty(PERSONAL_ANNOTATION_GROUP, annotazioniPersonali.getName());
        configurationService.setProperty(PERSONAL_ANNOTATION_COLLECTION, annotazioniPersonaliCol.getID());

        String full =
            configurationService.getProperty("dspace.server.url") +
                "/iiif/" + item1.getID() + "/canvas/" + bitstream1.getID();
        try (FileInputStream fileInputStream = getFileInputStream("valid-create.json")) {
            validAnnotation = mapper.readValue(fileInputStream.readAllBytes(), AnnotationRest.class);
            validAnnotation.on.get(0).full = full;
        }
        AtomicReference<String> idRefC = new AtomicReference<>();
        AtomicReference<String> idRefB = new AtomicReference<>();
        AtomicReference<String> idRefA = new AtomicReference<>();

        try {
            // create an annotation using the userC
            validAnnotation.resource.get(0).chars = "<p>USER C COMMENT</p>";
            validAnnotation.resource.get(0).fullText = "USER C COMMENT";

            String tokenC = getAuthToken(userC.getEmail(), "changeme");
            getClient(tokenC)
                .perform(
                    post("/annotation/create")
                        .content(mapper.writeValueAsBytes(validAnnotation))
                        .contentType("application/json;charset=UTF-8")
                )
                .andExpect(status().isOk())
                .andDo(result -> idRefC.set(read(result.getResponse().getContentAsString(), "$.['@id']")));

            Item annotationC = annotationService.findById(context, idRefC.get());
            ResourcePolicyService resourcePolicyService =
                AuthorizeServiceFactory.getInstance().getResourcePolicyService();
            List<ResourcePolicy> resourcePolicies = resourcePolicyService.find(context, annotationC);
            GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
            Group anonymous = groupService.findByName(context, "Anonymous");

            // create an annotation using the userB
            validAnnotation.resource.get(0).chars = "<p>USER B COMMENT</p>";
            validAnnotation.resource.get(0).fullText = "USER B COMMENT";

            String tokenB = getAuthToken(userB.getEmail(), "changeme");
            getClient(tokenB)
                .perform(
                    post("/annotation/create")
                        .content(mapper.writeValueAsBytes(validAnnotation))
                        .contentType("application/json;charset=UTF-8")
                )
                .andExpect(status().isOk())
                .andDo(result -> idRefB.set(read(result.getResponse().getContentAsString(), "$.['@id']")));

            Item annotationB = annotationService.findById(context, idRefB.get());
            resourcePolicies = resourcePolicyService.find(context, annotationB);
            assertThat(
                resourcePolicies,
                allOf(
                    hasItem(isResourcePolicy(READ, userB.getID())),
                    hasItem(isResourcePolicy(ADMIN, userB.getID())),
                    hasItem(isResourcePolicy(REMOVE, userB.getID())),
                    hasItem(isResourcePolicy(DELETE, userB.getID()))
                )
            );

            // create an annotation using the userA
            validAnnotation.resource.get(0).chars = "<p>USER A COMMENT</p>";
            validAnnotation.resource.get(0).fullText = "USER A COMMENT";

            String tokenA = getAuthToken(userA.getEmail(), "changeme");
            getClient(tokenA)
                .perform(
                    post("/annotation/create")
                        .content(mapper.writeValueAsBytes(validAnnotation))
                        .contentType("application/json;charset=UTF-8")
                )
                .andExpect(status().isOk())
                .andDo(result -> idRefA.set(read(result.getResponse().getContentAsString(), "$.['@id']")));

            Item annotationA = annotationService.findById(context, idRefA.get());
            resourcePolicies = resourcePolicyService.find(context, annotationA);
            assertThat(
                resourcePolicies,
                allOf(
                    hasItem(isResourcePolicy(READ, userA.getID())),
                    hasItem(isResourcePolicy(ADMIN, userA.getID())),
                    hasItem(isResourcePolicy(REMOVE, userA.getID())),
                    hasItem(isResourcePolicy(DELETE, userA.getID()))
                )
            );


            // check the annotation with tokenC contains only the one that are not personal
            getClient(tokenC)
                .perform(
                    get("/annotation/search")
                        .param("uri", full)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(
                    jsonPath(
                        "$[*].resource[*]",
                        hasItem(
                            allOf(
                                hasJsonPath("$.['@type']", is("dctypes:Text")),
                                hasJsonPath("$.['chars']", is("<p>USER C COMMENT</p>")),
                                hasJsonPath("$.['http://dev.llgc.org.uk/sas/full_text']", is("USER C COMMENT"))
                            )
                        )
                    )
                )
                .andExpect(
                    jsonPath(
                        "$.length()",
                        is(1)
                    )
                );

            // check the annotation with tokenB contains personal of USER B and public of USER C
            getClient(tokenB)
                .perform(
                    get("/annotation/search")
                        .param("uri", full)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(
                    jsonPath(
                        "$[*].resource[*]",
                        allOf(
                            hasItem(
                                allOf(
                                    hasJsonPath("$.['@type']", is("dctypes:Text")),
                                    hasJsonPath("$.['chars']", is("<p>USER C COMMENT</p>")),
                                    hasJsonPath("$.['http://dev.llgc.org.uk/sas/full_text']", is("USER C COMMENT"))
                                )
                            ),
                            hasItem(
                                allOf(
                                    hasJsonPath("$.['@type']", is("dctypes:Text")),
                                    hasJsonPath("$.['chars']", is("<p>USER B COMMENT</p>")),
                                    hasJsonPath("$.['http://dev.llgc.org.uk/sas/full_text']", is("USER B COMMENT"))
                                )
                            )
                        )
                    )
                )
                .andExpect(
                    jsonPath(
                        "$.length()",
                        is(2)
                    )
                );

            // check the annotation with tokenB contains personal of USER A and public of USER C
            getClient(tokenA)
                .perform(
                    get("/annotation/search")
                        .param("uri", full)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(
                    jsonPath(
                        "$[*].resource[*]",
                        allOf(
                            hasItem(
                                allOf(
                                    hasJsonPath("$.['@type']", is("dctypes:Text")),
                                    hasJsonPath("$.['chars']", is("<p>USER C COMMENT</p>")),
                                    hasJsonPath("$.['http://dev.llgc.org.uk/sas/full_text']", is("USER C COMMENT"))
                                )
                            ),
                            hasItem(
                                allOf(
                                    hasJsonPath("$.['@type']", is("dctypes:Text")),
                                    hasJsonPath("$.['chars']", is("<p>USER A COMMENT</p>")),
                                    hasJsonPath("$.['http://dev.llgc.org.uk/sas/full_text']", is("USER A COMMENT"))
                                )
                            )
                        )
                    )
                )
                .andExpect(
                    jsonPath(
                        "$.length()",
                        is(2)
                    )
                );
        } finally {
            context.turnOffAuthorisationSystem();

            deleteIdRef(idRefA);
            deleteIdRef(idRefB);
            deleteIdRef(idRefC);

            context.restoreAuthSystemState();
        }
    }

    private FileInputStream getFileInputStream(String name) throws FileNotFoundException {
        return new FileInputStream(new File(BASE_TEST_DIR, name));
    }

    static class ResourcePolicyMatcher extends TypeSafeDiagnosingMatcher<ResourcePolicy> {
        private final int action;
        private final UUID epersonId;
        private final UUID groupId;

        private ResourcePolicyMatcher(int action, UUID epersonId, UUID groupId) {
            this.action = action;
            this.epersonId = epersonId;
            this.groupId = groupId;
        }

        public static Matcher<ResourcePolicy> isResourcePolicy(int action, UUID epersonId, UUID groupId) {
            return new ResourcePolicyMatcher(action, epersonId, groupId);
        }

        public static Matcher<ResourcePolicy> isResourcePolicy(int action, UUID epersonId) {
            return isResourcePolicy(action, epersonId, null);
        }

        @Override
        protected boolean matchesSafely(ResourcePolicy policy, Description mismatchDescription) {
            if (policy.getAction() != action) {
                mismatchDescription.appendText("action was ").appendValue(policy.getAction());
                return false;
            }

            if (epersonId != null && (policy.getEPerson() == null || !epersonId.equals(policy.getEPerson().getID()))) {
                mismatchDescription.appendText("eperson ID was ")
                                   .appendValue(policy.getEPerson() == null ? null : policy.getEPerson().getID());
                return false;
            }

            if (groupId != null && (policy.getGroup() == null || !groupId.equals(policy.getGroup().getID()))) {
                mismatchDescription.appendText("group ID was ")
                                   .appendValue(policy.getGroup() == null ? null : policy.getGroup().getID());
                return false;
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("ResourcePolicy with action ")
                       .appendValue(action)
                       .appendText(" and eperson ID ")
                       .appendValue(epersonId)
                       .appendText(" and group ID ")
                       .appendValue(groupId);
        }
    }

}