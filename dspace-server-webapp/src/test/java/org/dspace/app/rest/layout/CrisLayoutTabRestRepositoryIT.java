/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.layout;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.matcher.CrisLayoutBoxMatcher.matchBox;
import static org.dspace.app.rest.matcher.CrisLayoutTabMatcher.matchRest;
import static org.dspace.app.rest.matcher.CrisLayoutTabMatcher.matchTab;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.matcher.CrisLayoutTabMatcher;
import org.dspace.app.rest.model.CrisLayoutTabRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.CrisLayoutBoxBuilder;
import org.dspace.builder.CrisLayoutFieldBuilder;
import org.dspace.builder.CrisLayoutMetric2BoxBuilder;
import org.dspace.builder.CrisLayoutTabBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBoxTypes;
import org.dspace.layout.CrisLayoutCell;
import org.dspace.layout.CrisLayoutRow;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.CrisLayoutTabService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This test class verify the REST Services for the Layout Tabs functionality (endpoint /api/layout/tabs)
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CrisLayoutTabRestRepositoryIT extends AbstractControllerIntegrationTest {

    private static final String BASE_TEST_DIR = "./target/testing/dspace/assetstore/layout/";

    @Autowired
    private MetadataSchemaService mdss;

    @Autowired
    private MetadataFieldService mfss;

    @Autowired
    private CrisLayoutTabService crisLayoutTabService;

    private final String METADATASECURITY_URL = "http://localhost:8080/api/core/metadatafield/";

    /**
     * Test for endpoint /api/layout/tabs/<ID_TAB>.
     * @throws Exception
     */
    @Test
    public void testFindOne() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        MetadataSchema schema = mdss.find(context, "dc");
        MetadataField isbn = mfss.findByElement(context, schema, "identifier", "isbn");
        MetadataField uri = mfss.findByElement(context, schema, "identifier", "uri");
        MetadataField lastName = mfss.findByElement(context, schema, "familyName", null);
        MetadataField givenName = mfss.findByElement(context, schema, "givenName", null);

        CrisLayoutBox boxOne = CrisLayoutBoxBuilder.createBuilder(context, eType, false, false)
                .withHeader("First New Box Header")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withShortname("Shortname for new first box")
                .withStyle("STYLE")
                .build();

        CrisLayoutBox boxTwo = CrisLayoutBoxBuilder.createBuilder(context, eType, false, false)
                .withHeader("Second New Box Header")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withShortname("Shortname for new second box")
                .withStyle("STYLE")
                .withType(CrisLayoutBoxTypes.METADATA.name())
                .build();

        CrisLayoutFieldBuilder.createMetadataField(context, lastName, 0, 1)
            .withLabel("LAST NAME")
            .withRendering("TEXT")
            .withBox(boxTwo)
            .build();

        CrisLayoutFieldBuilder.createMetadataField(context, givenName, 0, 1)
            .withLabel("GIVEN NAME")
            .withRendering("TEXT")
            .withBox(boxTwo)
            .build();

        CrisLayoutBox boxThree = CrisLayoutBoxBuilder.createBuilder(context, eType, false, false)
                .withHeader("Third New Box Header - priority 0")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withShortname("orgUnits")
                .withStyle("STYLE")
                .addMetadataSecurityField(isbn)
                .withType(CrisLayoutBoxTypes.RELATION.name())
                .build();

        CrisLayoutBox boxFour = CrisLayoutBoxBuilder.createBuilder(context, eType, false, false)
                .withHeader("Fourth New Box Header - priority 1")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withShortname("Shortname 4")
                .withStyle("STYLE")
                .withType(CrisLayoutBoxTypes.METRICS.name())
                .withMaxColumns(2)
                .build();

        CrisLayoutBox boxFive = CrisLayoutBoxBuilder.createBuilder(context, eType, false, false)
                .withHeader("Fifth New Box Header - priority 2")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withShortname("Shortname 5")
                .withStyle("STYLE")
                .addMetadataSecurityField(isbn)
                .addMetadataSecurityField(uri)
                .build();

        CrisLayoutBox boxSix = CrisLayoutBoxBuilder.createBuilder(context, eType, false, false)
                .withHeader("Sixth New Box Header - priority 2")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withShortname("Shortname 6")
                .withStyle("STYLE")
                .addMetadataSecurityField(uri)
                .build();

        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                .withShortName("Another New Tab shortname")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withHeader("New Tab header")
                .withLeading(true)
                .addBoxIntoNewRow(boxOne)
                .addBoxIntoNewRow(boxTwo, "rowTwoStyle", "cellOfRowTwoStyle")
                .addBoxIntoLastRow(boxThree, "style")
                .addBoxIntoLastCell(boxFour)
                .addBoxIntoNewRow(boxFive)
                .addBoxIntoLastCell(boxSix)
                .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/layout/tabs/" + tab.getID()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.id", is(tab.getID())))
            .andExpect(jsonPath("$.shortname", is("Another New Tab shortname")))
            .andExpect(jsonPath("$.header", is("New Tab header")))
            .andExpect(jsonPath("$.leading", is(true)))
            .andExpect(jsonPath("$.security", is(LayoutSecurity.PUBLIC.getValue())))
            .andExpect(jsonPath("$.rows", hasSize(3)))
            .andExpect(jsonPath("$.rows[0].style").doesNotExist())
            .andExpect(jsonPath("$.rows[0].cells", hasSize(1)))
            .andExpect(jsonPath("$.rows[0].cells[0].style").doesNotExist())
            .andExpect(jsonPath("$.rows[0].cells[0].boxes", contains(matchBox(boxOne))))
            .andExpect(jsonPath("$.rows[1].style", is("rowTwoStyle")))
            .andExpect(jsonPath("$.rows[1].cells", hasSize(2)))
            .andExpect(jsonPath("$.rows[1].cells[0].style", is("cellOfRowTwoStyle")))
            .andExpect(jsonPath("$.rows[1].cells[0].boxes", contains(matchBox(boxTwo))))
            .andExpect(jsonPath("$.rows[1].cells[1].style", is("style")))
            .andExpect(jsonPath("$.rows[1].cells[1].boxes", contains(matchBox(boxThree), matchBox(boxFour))))
            .andExpect(jsonPath("$.rows[2].style").doesNotExist())
            .andExpect(jsonPath("$.rows[2].cells", hasSize(1)))
            .andExpect(jsonPath("$.rows[2].cells[0].style").doesNotExist())
            .andExpect(jsonPath("$.rows[2].cells[0].boxes", contains(matchBox(boxFive), matchBox(boxSix))));
    }

    /**
     * Test for endpoint /api/layout/tabs/<ID_TAB>/securitymetadata.
     * It returns all the metadatafields that define the security.
     * This endpoint is reseved for the admin user
     * @throws Exception
     */
    @Test
    public void getTabMetadatasecurity() throws Exception {
        context.turnOffAuthorisationSystem();
        // get metadata field
        MetadataSchema schema = mdss.find(context, "dc");
        MetadataField isbn = mfss.findByElement(context, schema, "identifier", "isbn");
        MetadataField uri = mfss.findByElement(context, schema, "identifier", "uri");
        MetadataField abs = mfss.findByElement(context, schema, "description", "abstract");
        MetadataField provenance = mfss.findByElement(context, schema, "description", "provenance");
        MetadataField sponsorship = mfss.findByElement(context, schema, "description", "sponsorship");
        MetadataField extent = mfss.findByElement(context, schema, "format", "extent");
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create tabs
        CrisLayoutTabBuilder.createTab(context, eType, 0)
            .withShortName("New Tab 1")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addMetadatasecurity(isbn)
            .addMetadatasecurity(uri)
            .build();
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
            .withShortName("New Tab 2")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addMetadatasecurity(abs)
            .addMetadatasecurity(provenance)
            .addMetadatasecurity(sponsorship)
            .build();
        CrisLayoutTabBuilder.createTab(context, eType, 0)
            .withShortName("New Tab 3")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addMetadatasecurity(extent)
            .build();
        context.restoreAuthSystemState();
        // Test without authentication
        getClient().perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
            .andExpect(status().isUnauthorized()); // 401 Unauthorized;
        // Test with non admin user
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
            .andExpect(status().isForbidden()); // 403 - user haven't sufficient permission
        // Test with admin user
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.not(Matchers.empty())))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));
    }

    @Test
    public void addSecurityMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create tab
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                            .withShortName("New Tab")
                            .withSecurity(LayoutSecurity.PUBLIC)
                            .build();

        // get metadata field isbn
        MetadataSchema schema = mdss.find(context, "dc");
        MetadataField isbn = mfss.findByElement(context, schema, "identifier", "isbn");

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
                             .andExpect(status().isOk())
                             .andExpect(content().contentType(contentType))
                             .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.is(Matchers.empty())))
                             .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));

        getClient(tokenAdmin).perform(post("/api/layout/tabs/" + tab.getID() + "/securitymetadata")
                            .contentType(org.springframework.http.MediaType.parseMediaType
                                    (org.springframework.data.rest.webmvc.RestMediaTypes
                                         .TEXT_URI_LIST_VALUE))
                            .content(METADATASECURITY_URL + isbn.getID())
                            ).andExpect(status().isNoContent());

        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
                             .andExpect(status().isOk())
                             .andExpect(content().contentType(contentType))
                             // Expect a not empty collection in $._embedded.securitymetadata because
                             // the previous POST invocation add the ISBN element
                             .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.not(Matchers.empty())))
                             .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));
    }

    @Test
    public void addSecurityMetadataUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create tab
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                            .withShortName("New Tab")
                            .withSecurity(LayoutSecurity.PUBLIC)
                            .build();

        // get metadata field isbn
        MetadataSchema schema = mdss.find(context, "dc");
        MetadataField isbn = mfss.findByElement(context, schema, "identifier", "isbn");

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.is(Matchers.empty())))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));

        getClient().perform(post("/api/layout/tabs/" + tab.getID() + "/securitymetadata")
                    .contentType(org.springframework.http.MediaType.parseMediaType
                            (org.springframework.data.rest.webmvc.RestMediaTypes
                                 .TEXT_URI_LIST_VALUE))
                    .content(METADATASECURITY_URL + isbn.getID())
                    ).andExpect(status().isUnauthorized());

        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.is(Matchers.empty())))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
    }

    @Test
    public void addSecurityMetadataisForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create tab
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                            .withShortName("New Tab")
                            .withSecurity(LayoutSecurity.PUBLIC)
                            .build();

        // get metadata field isbn
        MetadataSchema schema = mdss.find(context, "dc");
        MetadataField isbn = mfss.findByElement(context, schema, "identifier", "isbn");

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.is(Matchers.empty())))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(post("/api/layout/tabs/" + tab.getID() + "/securitymetadata")
                                .contentType(org.springframework.http.MediaType.parseMediaType
                                        (org.springframework.data.rest.webmvc.RestMediaTypes
                                             .TEXT_URI_LIST_VALUE))
                                .content(METADATASECURITY_URL + isbn.getID()))
                               .andExpect(status().isForbidden());

        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.is(Matchers.empty())))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
    }

    @Test
    public void addSecurityMetadataisNotFoundTabTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // get metadata field isbn
        MetadataSchema schema = mdss.find(context, "dc");
        MetadataField isbn = mfss.findByElement(context, schema, "identifier", "isbn");
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(post("/api/layout/tabs/" + Integer.MAX_VALUE + "/securitymetadata")
                .contentType(org.springframework.http.MediaType.parseMediaType
                        (org.springframework.data.rest.webmvc.RestMediaTypes
                             .TEXT_URI_LIST_VALUE))
                .content(METADATASECURITY_URL + isbn.getID()))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void addSecurityMetadataMissingMetadataTest() throws Exception {
       context.turnOffAuthorisationSystem();
       // Create entity type Publication
       EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
       // Create tab
       CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                           .withShortName("New Tab")
                           .withSecurity(LayoutSecurity.PUBLIC)
                           .build();

       context.restoreAuthSystemState();

       String tokenAdmin = getAuthToken(admin.getEmail(), password);
       getClient(tokenAdmin).perform(
               post("/api/layout/tabs/" + tab.getID() + "/securitymetadata")
               .contentType(org.springframework.http.MediaType.parseMediaType
                       (org.springframework.data.rest.webmvc.RestMediaTypes
                            .TEXT_URI_LIST_VALUE)))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void removeSecurityMetadata() throws Exception {
        context.turnOffAuthorisationSystem();
        // get metadata field
        MetadataSchema schema = mdss.find(context, "dc");
        MetadataField isbn = mfss.findByElement(context, schema, "identifier", "isbn");
        MetadataField uri = mfss.findByElement(context, schema, "identifier", "uri");
        MetadataField abs = mfss.findByElement(context, schema, "description", "abstract");
        MetadataField provenance = mfss.findByElement(context, schema, "description", "provenance");
        MetadataField sponsorship = mfss.findByElement(context, schema, "description", "sponsorship");
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create tabs
        CrisLayoutTab tabOne = CrisLayoutTabBuilder.createTab(context, eType, 0)
            .withShortName("New Tab 1")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addMetadatasecurity(uri)
            .build();
        CrisLayoutTab tabTwo = CrisLayoutTabBuilder.createTab(context, eType, 0)
            .withShortName("New Tab 2")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addMetadatasecurity(abs)
            .addMetadatasecurity(provenance)
            .addMetadatasecurity(sponsorship)
            .build();
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        // try to remove a not existing metadata
        getClient(tokenAdmin)
                .perform(delete("/api/layout/tabs/" + tabOne.getID() + "/securitymetadata/" + Integer.MAX_VALUE))
                .andExpect(status().isNoContent());

        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tabOne.getID() + "/securitymetadata"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.not(Matchers.empty())))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));

        // try to remove a not associated metadata
        getClient(tokenAdmin)
                .perform(delete("/api/layout/tabs/" + tabOne.getID() + "/securitymetadata/" + isbn.getID()))
                .andExpect(status().isNoContent());
        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tabOne.getID() + "/securitymetadata"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.not(Matchers.empty())))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));

        // remove the only associated metadata
        getClient(tokenAdmin)
                .perform(delete("/api/layout/tabs/" + tabOne.getID() + "/securitymetadata/" + uri.getID()))
                .andExpect(status().isNoContent());

        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tabOne.getID() + "/securitymetadata"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));

        // remove one of the many associated metadata
        getClient(tokenAdmin)
                .perform(delete("/api/layout/tabs/" + tabTwo.getID() + "/securitymetadata/" + abs.getID()))
                .andExpect(status().isNoContent());

        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tabTwo.getID() + "/securitymetadata"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.not(Matchers.empty())))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));
    }

    /**
     * Test for endpoint /api/layout/tabs/search/findByItem?uuid=<ITEM-UUID>
     * The tabs are sorted by priority ascending. This are filtered based on the permission of the
     * current user and available data.
     * @throws Exception
     */
    @Test
    public void findByItem() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create new community
        Community community = CommunityBuilder.createCommunity(context)
            .withName("Test Community")
            .withTitle("Title test community")
            .build();
        // Create new collection
        Collection collection = CollectionBuilder.createCollection(context, community)
            .withName("Test Collection")
            .build();
        // Create entity Type
        EntityTypeBuilder.createEntityTypeBuilder(context, "Publication")
            .build();
        EntityType eTypePer = EntityTypeBuilder.createEntityTypeBuilder(context, "Person")
            .build();
        // Create new person item
        Item item = ItemBuilder.createItem(context, collection)
            .withPersonIdentifierFirstName("Danilo")
            .withPersonIdentifierLastName("Di Nuzzo")
            .withEntityType(eTypePer.getLabel())
            .build();
        MetadataSchema schema = mdss.find(context, "person");
        MetadataField firstName = mfss.findByElement(context, schema, "givenName", null);
        MetadataField lastName = mfss.findByElement(context, schema, "familyName", null);
        MetadataField provenance = mfss.findByElement(context, schema, "description", "provenance");
        MetadataField sponsorship = mfss.findByElement(context, schema, "description", "sponsorship");

        // Create tabs for Person Entity
       CrisLayoutBox boxOne = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
           .withShortname("Box shortname 1")
           .withSecurity(LayoutSecurity.PUBLIC)
            .withContainer(false)
           .build();
       CrisLayoutFieldBuilder.createMetadataField(context, firstName, 0, 1)
           .withLabel("LAST NAME")
           .withRendering("TEXT")
           .withBox(boxOne)
           .build();
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eTypePer, 0)
            .withShortName("TabOne For Person - priority 0")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addBoxIntoNewRow(boxOne)
            .build();

        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
            .withShortname("Box shortname 2")
            .withSecurity(LayoutSecurity.PUBLIC)
            .build();
        CrisLayoutBox boxTwo = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
            .withShortname("Box shortname 33")
            .withSecurity(LayoutSecurity.ADMINISTRATOR)
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, lastName, 0, 1)
            .withLabel("LAST NAME")
            .withRendering("TEXT")
            .withBox(box)
            .build();
        CrisLayoutTab tabTwo = CrisLayoutTabBuilder.createTab(context, eTypePer, 1)
            .withShortName("TabTwo For Person - priority 1")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addBoxIntoNewRow(box)
            .addBoxIntoNewRow(boxTwo)
            .build();

        // tab without data
        CrisLayoutTabBuilder.createTab(context, eTypePer, 2)
            .withShortName("New Tab For Person 2")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addMetadatasecurity(provenance)
            .addMetadatasecurity(sponsorship)
            .build();

        CrisLayoutBox boxThree = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
            .withShortname("Box shortname 3")
            .withSecurity(LayoutSecurity.PUBLIC)
            .build();

        CrisLayoutTab administratorSecuredTab = CrisLayoutTabBuilder.createTab(context, eTypePer, 2)
              .withShortName("AdministratorTab")
              .withSecurity(LayoutSecurity.ADMINISTRATOR)
              .withHeader("Administrator Tab header")
              .addMetadatasecurity(provenance)
              .addMetadatasecurity(sponsorship)
              .addBoxIntoNewRow(boxThree)
              .build();

        CrisLayoutBox administratorSecuredBox = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
            .withShortname("Box shortname secured")
            .withSecurity(LayoutSecurity.ADMINISTRATOR)
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, lastName, 0, 1)
              .withLabel("LAST NAME")
              .withRendering("TEXT")
              .withBox(administratorSecuredBox)
              .build();
        CrisLayoutTab tabWithOnlySecuredBox = CrisLayoutTabBuilder.createTab(context, eTypePer, 1)
           .withShortName("secured box holder - priority 1")
           .withSecurity(LayoutSecurity.PUBLIC)
           .withHeader("secured box holder")
           .addBoxIntoNewRow(administratorSecuredBox)
           .build();

        context.restoreAuthSystemState();
        // Test
        getClient().perform(get("/api/layout/tabs/search/findByItem").param("uuid", item.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(2))) // only two tabs have contents to show
            .andExpect(jsonPath("$._embedded.tabs", contains(matchTab(tab), matchTab(tabTwo))))
            .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes", contains(matchBox(boxOne))))
            .andExpect(jsonPath("$._embedded.tabs[1].rows[0].cells[0].boxes", contains(matchBox(box))));
    }

    /**
     * Test for endpoint /api/layout/tabs/search/findByEntityType?type=<:string>. It returns all the tabs
     * that are available for the items of the specified type. This endpoint is reserved to system administrators
     * @throws Exception
     */
    @Test
    public void findByEntityType() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type
        EntityType eTypePer = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        EntityType eTypePub = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        // Create new Tab for Publication Entity
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eTypePub, 0)
            .withShortName("New Tab shortname priority 0")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .build();
        CrisLayoutTab tabTwo = CrisLayoutTabBuilder.createTab(context, eTypePub, 1)
            .withShortName("New Tab shortname priority 1")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .build();
        CrisLayoutTab tabThree = CrisLayoutTabBuilder.createTab(context, eTypePub, 2)
            .withShortName("New Tab shortname priority 2")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .build();
        // Create tabs for Person
        CrisLayoutTabBuilder.createTab(context, eTypePer, 0)
            .withShortName("First Person Tab")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .build();
        CrisLayoutTabBuilder.createTab(context, eTypePer, 0)
            .withShortName("Second Person Tab")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .build();
        context.restoreAuthSystemState();
        // Test without authentication
        getClient().perform(get("/api/layout/tabs/search/findByEntityType")
            .param("type", tab.getEntity().getLabel()))
            .andExpect(status().isUnauthorized()); // 401 Unauthorized;
        // Test with a non admin user
        String token = getAuthToken(eperson.getEmail(), password);
        // Get created tab by id from REST service and check its response
        getClient(token).perform(get("/api/layout/tabs/search/findByEntityType")
            .param("type", tab.getEntity().getLabel()))
            .andExpect(status().isForbidden()); // 403 - user haven't sufficient permission
        // Get auth token of an admin user
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        // Get created tab by id from REST service and check its response
        getClient(tokenAdmin).perform(get("/api/layout/tabs/search/findByEntityType")
            .param("type", eTypePub.getLabel()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)))
            .andExpect(jsonPath("$._embedded.tabs[0]", Matchers.is(
                CrisLayoutTabMatcher.matchTab(tab))))
            .andExpect(jsonPath("$._embedded.tabs[1]", Matchers.is(
                    CrisLayoutTabMatcher.matchTab(tabTwo))))
            .andExpect(jsonPath("$._embedded.tabs[2]", Matchers.is(
                    CrisLayoutTabMatcher.matchTab(tabThree))));
    }

    /**
     * Test for endpoint POST /api/layout/tabs, Its create a new tab
     * This endpoint is reserved to system administrators
     * @throws Exception
     */
    @Test
    public void createTab() throws Exception {
        context.turnOffAuthorisationSystem();
        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {
            // Create entity type
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
            context.restoreAuthSystemState();

            CrisLayoutTabRest rest = parseJson("tab.json");

            ObjectMapper mapper = new ObjectMapper();

            // Test without authentication
            getClient().perform(post("/api/layout/tabs")
                .content(mapper.writeValueAsBytes(rest))
                .contentType(contentType))
                .andExpect(status().isUnauthorized());

            // Test with a non admin user
            String token = getAuthToken(eperson.getEmail(), password);
            getClient(token).perform(post("/api/layout/tabs")
                .content(mapper.writeValueAsBytes(rest))
                .contentType(contentType))
                .andExpect(status().isForbidden());

            // Test with admin user
            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(post("/api/layout/tabs")
                .content(mapper.writeValueAsBytes(rest))
                .contentType(contentType))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", Matchers.is(matchRest(rest))))
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            // Get created tab by id from REST service and check its response
            getClient().perform(get("/api/layout/tabs/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", Matchers.is(matchRest(rest))));

            CrisLayoutTab tab = crisLayoutTabService.find(context, idRef.get());
            assertThat(tab, notNullValue());
            assertThat(tab.getEntity().getLabel(), is("Publication"));
            assertThat(tab.getHeader(), is("Publication HEADER"));
            assertThat(tab.getShortName(), is("info"));
            assertThat(tab.getPriority(), is(1));
            assertThat(tab.getSecurity(), is(0));
            assertThat(tab.isLeading(), is(true));

            assertThat(tab.getRows(), hasSize(2));

            CrisLayoutRow firstRow = tab.getRows().get(0);
            assertThat(firstRow.getStyle(), nullValue());
            assertThat(firstRow.getCells(), hasSize(2));

            CrisLayoutCell firstCell = firstRow.getCells().get(0);
            assertThat(firstCell.getStyle(), is("col-md-6"));
            assertThat(firstCell.getBoxes(), hasSize(1));

            CrisLayoutBox firstBox = firstCell.getBoxes().get(0);
            assertThat(firstBox.getShortname(), is("primary"));
            assertThat(firstBox.getHeader(), is("Primary Information"));
            assertThat(firstBox.getEntitytype().getLabel(), is("Publication"));
            assertThat(firstBox.getCollapsed(), is(true));
            assertThat(firstBox.isContainer(), is(false));
            assertThat(firstBox.getStyle(), is("col-md-6"));
            assertThat(firstBox.getSecurity(), is(0));
            assertThat(firstBox.getType(), is("METADATA"));
            assertThat(firstBox.getMetadataSecurityFields(), hasSize(1));
            assertThat(firstBox.getLayoutFields(), hasSize(2));

            CrisLayoutCell secondCell = firstRow.getCells().get(1);
            assertThat(secondCell.getStyle(), is("col-md-6"));
            assertThat(secondCell.getBoxes(), hasSize(1));

            CrisLayoutBox secondBox = secondCell.getBoxes().get(0);
            assertThat(secondBox.getShortname(), is("orgUnits"));
            assertThat(secondBox.getHeader(), is("OrgUnits"));
            assertThat(secondBox.getEntitytype().getLabel(), is("Publication"));
            assertThat(secondBox.getCollapsed(), is(false));
            assertThat(secondBox.isContainer(), is(true));
            assertThat(secondBox.getStyle(), is("col-md-6"));
            assertThat(secondBox.getSecurity(), is(0));
            assertThat(secondBox.getType(), is("RELATION"));
            assertThat(secondBox.getMetadataSecurityFields(), empty());
            assertThat(secondBox.getLayoutFields(), empty());

            CrisLayoutRow secondRow = tab.getRows().get(1);
            assertThat(secondRow.getStyle(), is("bg-light"));
            assertThat(secondRow.getCells(), hasSize(1));

            CrisLayoutCell thirdCell = secondRow.getCells().get(0);
            assertThat(thirdCell.getStyle(), is("col-md-12"));
            assertThat(thirdCell.getBoxes(), hasSize(1));

            CrisLayoutBox thirdBox = thirdCell.getBoxes().get(0);
            assertThat(thirdBox.getShortname(), is("metrics"));
            assertThat(thirdBox.getHeader(), is("Metrics"));
            assertThat(thirdBox.getEntitytype().getLabel(), is("Publication"));
            assertThat(thirdBox.getCollapsed(), is(false));
            assertThat(thirdBox.isContainer(), is(true));
            assertThat(thirdBox.getStyle(), nullValue());
            assertThat(thirdBox.getSecurity(), is(0));
            assertThat(thirdBox.getType(), is("METRICS"));
            assertThat(thirdBox.getMetadataSecurityFields(), empty());
            assertThat(thirdBox.getLayoutFields(), empty());
            assertThat(thirdBox.getMaxColumns(), is(2));
            assertThat(thirdBox.getMetric2box(), hasSize(2));

        } finally {
            if (idRef.get() != null) {
                CrisLayoutTabBuilder.delete(idRef.get());
            }
        }
    }

    /**
     * Test for endpoint DELETE /api/layout/tabs/<:id>, Its delete a tab
     * This endpoint is reserved to system administrators
     * @throws Exception
     */
    @Test
    public void deleteTab() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create new Tab for Person Entity
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                .withShortName("New Person Tab")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withHeader("New Person Tab header")
                .build();
        context.restoreAuthSystemState();

        getClient().perform(
                get("/api/layout/tabs/" + tab.getID())
        ).andExpect(status().isOk())
        .andExpect(content().contentType(contentType));

        // Delete with anonymous user
        getClient().perform(
                delete("/api/layout/tabs/" + tab.getID())
        ).andExpect(status().isUnauthorized());

        // Delete with non admin user
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
                delete("/api/layout/tabs/" + tab.getID())
        ).andExpect(status().isForbidden());

        // delete with admin
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(
                delete("/api/layout/tabs/" + tab.getID())
        ).andExpect(status().isNoContent());

        getClient(tokenAdmin).perform(
                get("/api/layout/tabs/" + tab.getID())
        ).andExpect(status().isNotFound());
    }

    @Test
    public void patchTabReplaceShortnameTest() throws Exception {

        context.turnOffAuthorisationSystem();
        // Create new EntityType Person
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                            .withShortName("Tab shortname")
                            .withHeader("Tab Header")
                            .withSecurity(LayoutSecurity.PUBLIC)
                            .build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String shortname = "New Shortname";
        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/shortname", shortname);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient(tokenAdmin).perform(patch("/api/layout/tabs/" + tab.getID())
                 .content(patchBody)
                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$", Matchers.allOf(
                         hasJsonPath("$.shortname", is(shortname)),
                         hasJsonPath("$.header", is(tab.getHeader()))
                         )));

        getClient().perform(get("/api/layout/tabs/" + tab.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.shortname", is(shortname)),
                hasJsonPath("$.header", is(tab.getHeader())))));

        tab = context.reloadEntity(tab);
        assertThat(tab.getShortName(), is(shortname));
    }

    @Test
    public void patchTabReplacePriorityTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create new EntityType Person
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                            .withShortName("Tab shortname")
                            .withHeader("Tab Header")
                            .withSecurity(LayoutSecurity.PUBLIC)
                            .build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        int newPriority = 99;
        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/priority", newPriority);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient(tokenAdmin).perform(patch("/api/layout/tabs/" + tab.getID())
                 .content(patchBody)
                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$", Matchers.allOf(
                         hasJsonPath("$.shortname", is("Tab shortname")),
                         hasJsonPath("$.priority", is(99)),
                         hasJsonPath("$.header", is(tab.getHeader()))
                         )));

        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.allOf(
                           hasJsonPath("$.shortname", is("Tab shortname")),
                           hasJsonPath("$.priority", is(99)),
                           hasJsonPath("$.header", is(tab.getHeader()))
                           )));
    }

    @Test
    public void patchTabReplacePriorityBadRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create new EntityType Person
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                            .withShortName("Tab shortname")
                            .withHeader("Tab Header")
                            .withSecurity(LayoutSecurity.PUBLIC)
                            .build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String newPriority = "wrongPriority";
        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/priority", newPriority);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient(tokenAdmin).perform(patch("/api/layout/tabs/" + tab.getID())
                 .content(patchBody)
                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                 .andExpect(status().isBadRequest());

        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.allOf(
                           hasJsonPath("$.shortname", is("Tab shortname")),
                           hasJsonPath("$.priority", is(0)),
                           hasJsonPath("$.header", is(tab.getHeader()))
                           )));
    }

    @Test
    public void testGetTabWithMetadataBox() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // get metadata field
        MetadataSchema schema = mdss.find(context, "dc");
        MetadataSchema schemaOaire = mdss.find(context, "oairecerif");
        MetadataField isbn = mfss.findByElement(context, schema, "identifier", "isbn");
        MetadataField uri = mfss.findByElement(context, schema, "identifier", "uri");
        MetadataField abs = mfss.findByElement(context, schema, "description", "abstract");
        MetadataField provenance = mfss.findByElement(context, schema, "description", "provenance");
        MetadataField sponsorship = mfss.findByElement(context, schema, "description", "sponsorship");
        MetadataField extent = mfss.findByElement(context, schema, "format", "extent");
        // nested metadata
        MetadataField author = mfss.findByElement(context, schema, "contributor", "author");
        MetadataField affiliation = mfss.findByElement(context, schemaOaire, "author", "affiliation");
        List<MetadataField> nestedMetadata = new ArrayList<>();
        nestedMetadata.add(author);
        nestedMetadata.add(affiliation);
        // Create boxes
        CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
            .withShortname("box-shortname-one")
            .build();
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
            .withShortname("box-shortname-two")
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, isbn, 0, 0)
            .withLabel("LABEL ISBN")
            .withRendering("RENDERIGN ISBN")
            .withRowStyle("row")
            .withLabelStyle("col-6")
            .withValueStyle("col-6")
            .withBox(box)
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, uri, 0, 1)
            .withLabel("LABEL URI")
            .withRendering("RENDERIGN URI")
            .withRowStyle("row")
            .withLabelStyle("col-6")
            .withValueStyle("col-6")
            .withBox(box)
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, abs, 1, 0)
            .withLabel("LABEL ABS")
            .withRendering("RENDERIGN ABS")
            .withRowStyle("row")
            .withLabelStyle("col-6")
            .withValueStyle("col-6")
            .withBox(box)
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, provenance, 1, 1)
            .withLabel("LABEL PROVENANCE")
            .withRendering("RENDERIGN PROVENANCE")
            .withRowStyle("row")
            .withLabelStyle("col-6")
            .withValueStyle("col-6")
            .withBox(box)
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, sponsorship, 1, 2)
            .withLabel("LABEL SPRONSORSHIP")
            .withRendering("RENDERIGN SPRONSORSHIP")
            .withRowStyle("row")
            .withLabelStyle("col-6")
            .withValueStyle("col-6")
            .withBox(box)
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, extent, 2, 0)
            .withLabel("LABEL EXTENT")
            .withRendering("RENDERIGN EXTENT")
            .withRowStyle("row")
            .withLabelStyle("col-6")
            .withValueStyle("col-6")
            .withBox(box)
            .build();
        // nested field
        CrisLayoutFieldBuilder.createMetadataField(context, author, 0, 1)
            .withLabel("Authors")
            .withRendering("table")
            .withRowStyle("row")
            .withLabelStyle("col-6")
            .withValueStyle("col-6")
            .withNestedField(nestedMetadata)
            .withBox(box)
            .build();

        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
            .withShortName("TabOne For Person - priority 0")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addBoxIntoNewRow(box)
            .build();

        context.restoreAuthSystemState();

        String firstConfigurationCell = "$.rows[0].cells[0].boxes[0].configuration.rows[0].cells[0]";

        getClient().perform(get("/api/layout/tabs/" + tab.getID()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.id", is(tab.getID())))
            .andExpect(jsonPath("$.rows", hasSize(1)))
            .andExpect(jsonPath("$.rows[0].style").doesNotExist())
            .andExpect(jsonPath("$.rows[0].cells", hasSize(1)))
            .andExpect(jsonPath("$.rows[0].cells[0].style").doesNotExist())
            .andExpect(jsonPath("$.rows[0].cells[0].boxes", hasSize(1)))
            .andExpect(jsonPath("$.rows[0].cells[0].boxes[0].configuration.rows[0].cells[0].fields", hasSize(3)))
            .andExpect(jsonPath("$.rows[0].cells[0].boxes[0].configuration.rows[1].cells[0].fields", hasSize(3)))
            .andExpect(jsonPath("$.rows[0].cells[0].boxes[0].configuration.rows[2].cells[0].fields", hasSize(1)))
            .andExpect(jsonPath(firstConfigurationCell + ".fields[2].metadata", is("dc.contributor.author")))
            .andExpect(jsonPath(firstConfigurationCell + ".fields[2].label", is("Authors")))
            .andExpect(jsonPath(firstConfigurationCell + ".fields[2].rendering", is("table")))
            .andExpect(jsonPath(firstConfigurationCell + ".fields[2].styleLabel", is("col-6")))
            .andExpect(jsonPath(firstConfigurationCell + ".fields[2].styleValue", is("col-6")))
            .andExpect(jsonPath(firstConfigurationCell + ".fields[2].metadataGroup.leading",
                is("dc.contributor.author")))
            .andExpect(jsonPath(firstConfigurationCell + ".fields[2].metadataGroup.elements", hasSize(2)))
            .andExpect(jsonPath(firstConfigurationCell + ".fields[2].metadataGroup.elements[1].metadata",
                is("oairecerif.author.affiliation")));
    }

    @Test
    public void testGetTabWithMetricsBox() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        // Create boxes
        CrisLayoutBoxBuilder.createBuilder(context, eType, CrisLayoutBoxTypes.METRICS.name(), true, true)
            .withShortname("box-shortname-one")
            .build();
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType,
            CrisLayoutBoxTypes.METRICS.name(), true, true)
            .withShortname("box-shortname-two")
            .withMaxColumns(2)
            .build();
        // Add metrics
        CrisLayoutMetric2BoxBuilder.create(context, box, "metric1", 0).build();
        CrisLayoutMetric2BoxBuilder.create(context, box, "metric2", 1).build();

        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
            .withShortName("TabOne For Person - priority 0")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addBoxIntoNewRow(box)
            .build();

        CrisLayoutBoxBuilder.createBuilder(context, eType, CrisLayoutBoxTypes.METRICS.name(), true, true)
            .withShortname("box-shortname-three")
            .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/layout/tabs/" + tab.getID()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.id", is(tab.getID())))
            .andExpect(jsonPath("$.rows", hasSize(1)))
            .andExpect(jsonPath("$.rows[0].style").doesNotExist())
            .andExpect(jsonPath("$.rows[0].cells", hasSize(1)))
            .andExpect(jsonPath("$.rows[0].cells[0].style").doesNotExist())
            .andExpect(jsonPath("$.rows[0].cells[0].boxes", hasSize(1)))
            .andExpect(jsonPath("$.rows[0].cells[0].boxes[0].configuration.maxColumns", Matchers.is(2)))
            .andExpect(jsonPath("$.rows[0].cells[0].boxes[0].configuration.metrics", hasSize(2)))
            .andExpect(jsonPath("$.rows[0].cells[0].boxes[0].configuration.metrics[0]", Matchers.is("metric1")))
            .andExpect(jsonPath("$.rows[0].cells[0].boxes[0].configuration.metrics[1]", Matchers.is("metric2")));
    }

    @Test
    public void testGetTabWithRelationBox() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        CrisLayoutBoxBuilder.createBuilder(context, eType, CrisLayoutBoxTypes.RELATION.name(), true, true)
            .withShortname("box-shortname-one")
            .build();
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType,
            CrisLayoutBoxTypes.RELATION.name(), true, true)
            .withShortname("authors")
            .build();

        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
            .withShortName("TabOne For Person - priority 0")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addBoxIntoNewRow(box)
            .build();

        CrisLayoutBoxBuilder.createBuilder(context, eType, CrisLayoutBoxTypes.RELATION.name(), true, true)
            .withShortname("box-shortname-three")
            .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/layout/tabs/" + tab.getID()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.id", is(tab.getID())))
            .andExpect(jsonPath("$.rows", hasSize(1)))
            .andExpect(jsonPath("$.rows[0].style").doesNotExist())
            .andExpect(jsonPath("$.rows[0].cells", hasSize(1)))
            .andExpect(jsonPath("$.rows[0].cells[0].style").doesNotExist())
            .andExpect(jsonPath("$.rows[0].cells[0].boxes", hasSize(1)))
            .andExpect(jsonPath("$.rows[0].cells[0].boxes[0].configuration.discovery-configuration",
                is("RELATION.Publication.authors")));
    }

    private CrisLayoutTabRest parseJson(String name) throws Exception {
        return new ObjectMapper().readValue(getFileInputStream(name), CrisLayoutTabRest.class);
    }

    private FileInputStream getFileInputStream(String name) throws FileNotFoundException {
        return new FileInputStream(new File(BASE_TEST_DIR, name));
    }
}
