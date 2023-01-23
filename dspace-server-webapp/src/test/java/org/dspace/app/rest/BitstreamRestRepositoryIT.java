/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataDoesNotExist;
import static org.dspace.core.Constants.WRITE;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterators;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.matcher.BitstreamFormatMatcher;
import org.dspace.app.rest.matcher.BitstreamMatcher;
import org.dspace.app.rest.matcher.BundleMatcher;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.test.MetadataPatchSuite;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.util.UUIDUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class BitstreamRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private ResourcePolicyService resourcePolicyService;

    @Autowired
    private BitstreamFormatService bitstreamFormatService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private ItemService itemService;

    @Test
    public void findAllTest() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                            createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        //Add a bitstream to an item
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                             createBitstream(context, publicItem1, is)
                                         .withName("Bitstream1")
                                         .withDescription("description123")
                                         .withMimeType("text/plain")
                                         .build();
        }

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/core/bitstreams/"))
                   .andExpect(status().isMethodNotAllowed());
    }

    //TODO Re-enable test after https://jira.duraspace.org/browse/DS-3774 is fixed
    @Ignore
    @Test
    public void findAllWithDeletedTest() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();

        String bitstreamContent = "This is an archived bitstream";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                            createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withMimeType("text/plain")
                                        .build();
        }

        //Add a bitstream to an item
        bitstreamContent = "This is a deleted bitstream";
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                             createBitstream(context, publicItem1, is)
                                         .withName("Bitstream1")
                                         .withMimeType("text/plain")
                                         .build();
        }

        //Delete the last bitstream
        bitstreamService.delete(context, bitstream1);
        context.commit();

        getClient().perform(get("/api/core/bitstreams/"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.bitstreams", contains(
                       BitstreamMatcher.matchBitstreamEntry(bitstream)
                   )))

        ;
    }

    @Test
    public void findOneBitstreamTest() throws Exception {

        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();


        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                            createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("Description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        //Add a bitstream to an item
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                             createBitstream(context, publicItem1, is)
                                         .withName("Bitstream1")
                                         .withDescription("Description1")
                                         .withMimeType("text/plain")
                                         .build();
        }

        context.restoreAuthSystemState();

        // When full projection is requested, response should include expected properties, links, and embeds.
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID())
                   .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", BitstreamMatcher.matchFullEmbeds()))
                   .andExpect(jsonPath("$", BitstreamMatcher.matchBitstreamEntry(bitstream)))
                   .andExpect(jsonPath("$", not(BitstreamMatcher.matchBitstreamEntry(bitstream1))))
        ;

        // When no projection is requested, response should include expected properties, links, and no embeds.
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
        ;

    }

    @Test
    public void findOneBitstreamTest_WorkspaceItemCoauthorCanAccessOriginalBundle() throws Exception {

        context.turnOffAuthorisationSystem();


        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection publications = CollectionBuilder.createCollection(context, child1)
                                           .withEntityType("Publication")
                                           .withName("Publications").build();

        Collection researchers = CollectionBuilder.createCollection(context, child1)
                                           .withEntityType("Person")
                                           .withName("Researchers").build();

        EPerson coauthor = EPersonBuilder.createEPerson(context)
            .withPassword(password)
            .withEmail("coauthor@example.com").build();

        Item publicationCoauthor = ItemBuilder.createItem(context, researchers)
                                              .withDspaceObjectOwner(coauthor)
                                              .withTitle("Coauthor")
                                              .build();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, publications)
                                                          .withAuthor(publicationCoauthor.getName(),
                                                                      UUIDUtils.toString(publicationCoauthor.getID()))
                                                          .build();

        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream("this is the bitstream", CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                createBitstream(context, workspaceItem.getItem(), is)
                .withName("Bitstream1")
                .withDescription("Description1")
                .withMimeType("text/plain")
                .build();
        }

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                   .andExpect(status().isUnauthorized());

        getClient(getAuthToken(coauthor.getEmail(), password))
            .perform(get("/api/core/bitstreams/" + bitstream.getID()))
            .andExpect(status().isOk());

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(get("/api/core/bitstreams/" + bitstream.getID()))
            .andExpect(status().isOk());
    }

    @Test
    public void findOneBitstreamTest_EmbargoedBitstream_Anon() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();

        // a public item with an embargoed bitstream
        String bitstreamContent = "Embargoed!";

        Item publicItem1;
        Bitstream bitstream;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, org.apache.commons.lang3.CharEncoding.UTF_8)) {

            publicItem1 = ItemBuilder.createItem(context, col1)
                                     .withTitle("Public item 1")
                                     .withIssueDate("2017-10-17")
                                     .withAuthor("Smith, Donald")
                                     .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withEmbargoPeriod("3 months")
                .build();
        }
        context.restoreAuthSystemState();

        // Bitstream metadata should still be accessible by anonymous request
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", BitstreamMatcher.matchProperties(bitstream)))
                   .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
                   .andExpect(jsonPath("$", BitstreamMatcher.matchLinks(bitstream.getID())))
        ;

        // Also accessible as embedded object by anonymous request
        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "?embed=bundles/bitstreams"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.bundles._embedded.bundles[0]._embedded.bitstreams._embedded" +
                                       ".bitstreams[0]", BitstreamMatcher.matchProperties(bitstream)))
        ;
    }

    @Test
    public void findOneBitstreamFormatTest_EmbargoedBitstream_Anon() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection 1")
            .build();

        // a public item with an embargoed bitstream
        String bitstreamContent = "Embargoed!";

        BitstreamFormat bitstreamFormat = bitstreamFormatService.findByMIMEType(context, "text/plain");

        Item publicItem1;
        Bitstream bitstream;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, org.apache.commons.lang3.CharEncoding.UTF_8)) {

            publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType(bitstreamFormat.getMIMEType())
                .withEmbargoPeriod("3 months")
                .build();
        }
        context.restoreAuthSystemState();

        // Bitstream format should still be accessible by anonymous request
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/format"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", BitstreamFormatMatcher.matchBitstreamFormat(
                bitstreamFormat.getID(), bitstreamFormat.getMIMEType(), bitstreamFormat.getDescription())))
            .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
        ;
    }

    @Test
    public void findOneBitstreamTest_NoReadPolicyOnBitstream_Anon() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";

        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                            createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("Description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        // Remove all READ policies on bitstream
        resourcePolicyService.removePolicies(context, bitstream, Constants.READ);

        context.restoreAuthSystemState();

        // Bitstream metadata should still be accessible by anonymous request
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", BitstreamMatcher.matchProperties(bitstream)))
                   .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
                   .andExpect(jsonPath("$", BitstreamMatcher.matchLinks(bitstream.getID())))
        ;

        // Also accessible as embedded object by anonymous request
        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "?embed=bundles/bitstreams"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.bundles._embedded.bundles[0]._embedded.bitstreams._embedded" +
                                       ".bitstreams[0]", BitstreamMatcher.matchProperties(bitstream)))
        ;
    }

    @Test
    public void findOneBitstreamFormatTest_NoReadPolicyOnBitstream_Anon() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        String bitstreamContent = "ThisIsSomeDummyText";

        BitstreamFormat bitstreamFormat = bitstreamFormatService.findByMIMEType(context, "text/plain");

        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                createBitstream(context, publicItem1, is)
                .withName("Bitstream")
                .withDescription("Description")
                .withMimeType(bitstreamFormat.getMIMEType())
                .build();
        }

        // Remove all READ policies on bitstream
        resourcePolicyService.removePolicies(context, bitstream, Constants.READ);

        context.restoreAuthSystemState();

        // Bitstream format should still be accessible by anonymous request
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/format"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", BitstreamFormatMatcher.matchBitstreamFormat(
                bitstreamFormat.getID(), bitstreamFormat.getMIMEType(), bitstreamFormat.getDescription())))
            .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
        ;
    }

    @Test
    public void findOneBitstreamTest_EmbargoedBitstream_NoREADRightsOnBundle() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setCurrentUser(eperson);

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();

        // a public item with an embargoed bitstream
        String bitstreamContent = "Embargoed!";

        Item publicItem1;
        Bitstream bitstream;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, org.apache.commons.lang3.CharEncoding.UTF_8)) {

            publicItem1 = ItemBuilder.createItem(context, col1)
                                     .withTitle("Public item 1")
                                     .withIssueDate("2017-10-17")
                                     .withAuthor("Smith, Donald")
                                     .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withEmbargoPeriod("3 months")
                .build();
        }

        // Remove read policies on bundle of bitstream
        resourcePolicyService.removePolicies(context, bitstream.getBundles().get(0), Constants.READ);

        context.restoreAuthSystemState();

        // Bitstream metadata should not be accessible by anonymous request
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                   .andExpect(status().isUnauthorized())
        ;

        // Bitstream metadata should not be accessible by submitter
        String submitterToken = getAuthToken(context.getCurrentUser().getEmail(), password);
        getClient(submitterToken).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                                 .andExpect(status().isForbidden())
        ;

        // Bitstream metadata should be accessible by admin
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                             .andExpect(status().isOk())
        ;
    }

    @Test
    public void findOneBitstreamFormatTest_EmbargoedBitstream_NoREADRightsOnBundle() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setCurrentUser(eperson);

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection 1")
            .build();

        // a public item with an embargoed bitstream
        String bitstreamContent = "Embargoed!";

        BitstreamFormat bitstreamFormat = bitstreamFormatService.findByMIMEType(context, "text/plain");

        Item publicItem1;
        Bitstream bitstream;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, org.apache.commons.lang3.CharEncoding.UTF_8)) {

            publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType(bitstreamFormat.getMIMEType())
                .withEmbargoPeriod("3 months")
                .build();
        }

        // Remove read policies on bundle of bitstream
        resourcePolicyService.removePolicies(context, bitstream.getBundles().get(0), Constants.READ);

        context.restoreAuthSystemState();

        // Bitstream format should not be accessible by anonymous request
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/format"))
            .andExpect(status().isUnauthorized())
        ;

        // Bitstream format should not be accessible by submitter
        String submitterToken = getAuthToken(context.getCurrentUser().getEmail(), password);
        getClient(submitterToken).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/format"))
            .andExpect(status().isForbidden())
        ;

        // Bitstream format should be accessible by admin
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/format"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", BitstreamFormatMatcher.matchBitstreamFormat(
                bitstreamFormat.getID(), bitstreamFormat.getMIMEType(), bitstreamFormat.getDescription())))
            .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
        ;
    }

    @Test
    public void findOneBitstreamTest_EmbargoedBitstream_ePersonREADRightsOnBundle() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setCurrentUser(eperson);

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();

        // a public item with an embargoed bitstream
        String bitstreamContent = "Embargoed!";

        Item publicItem1;
        Bitstream bitstream;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, org.apache.commons.lang3.CharEncoding.UTF_8)) {

            publicItem1 = ItemBuilder.createItem(context, col1)
                                     .withTitle("Public item 1")
                                     .withIssueDate("2017-10-17")
                                     .withAuthor("Smith, Donald")
                                     .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withEmbargoPeriod("3 months")
                .build();
        }

        // Replace anon read policy on bundle of bitstream with ePerson READ policy
        resourcePolicyService.removePolicies(context, bitstream.getBundles().get(0), Constants.READ);
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(eperson)
                             .withAction(Constants.READ)
                             .withDspaceObject(bitstream.getBundles().get(0)).build();

        context.restoreAuthSystemState();

        // Bitstream metadata should not be accessible by anonymous request
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                   .andExpect(status().isUnauthorized())
        ;

        // Bitstream metadata should be accessible by eperson
        String submitterToken = getAuthToken(context.getCurrentUser().getEmail(), password);
        getClient(submitterToken).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                                 .andExpect(status().isOk())
        ;

        // Bitstream metadata should be accessible by admin
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                             .andExpect(status().isOk())
        ;
    }

    @Test
    public void findOneBitstreamFormatTest_EmbargoedBitstream_ePersonREADRightsOnBundle() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setCurrentUser(eperson);

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection 1")
            .build();

        // a public item with an embargoed bitstream
        String bitstreamContent = "Embargoed!";

        BitstreamFormat bitstreamFormat = bitstreamFormatService.findByMIMEType(context, "text/plain");

        Item publicItem1;
        Bitstream bitstream;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, org.apache.commons.lang3.CharEncoding.UTF_8)) {

            publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType(bitstreamFormat.getMIMEType())
                .withEmbargoPeriod("3 months")
                .build();
        }

        // Replace anon read policy on bundle of bitstream with ePerson READ policy
        resourcePolicyService.removePolicies(context, bitstream.getBundles().get(0), Constants.READ);
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(eperson)
            .withAction(Constants.READ)
            .withDspaceObject(bitstream.getBundles().get(0)).build();

        context.restoreAuthSystemState();

        // Bitstream format should not be accessible by anonymous request
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/format"))
            .andExpect(status().isUnauthorized())
        ;

        // Bitstream format should be accessible by eperson
        String submitterToken = getAuthToken(context.getCurrentUser().getEmail(), password);
        getClient(submitterToken).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/format"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", BitstreamFormatMatcher.matchBitstreamFormat(
                bitstreamFormat.getID(), bitstreamFormat.getMIMEType(), bitstreamFormat.getDescription())))
            .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
        ;

        // Bitstream format should be accessible by admin
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/format"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", BitstreamFormatMatcher.matchBitstreamFormat(
                bitstreamFormat.getID(), bitstreamFormat.getMIMEType(), bitstreamFormat.getDescription())))
            .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
        ;
    }

    @Test
    public void findOneBitstreamTest_EmbargoedBitstream_NoREADRightsOnItem() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setCurrentUser(eperson);

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();

        // a public item with an embargoed bitstream
        String bitstreamContent = "Embargoed!";

        Item publicItem1;
        Bitstream bitstream;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, org.apache.commons.lang3.CharEncoding.UTF_8)) {

            publicItem1 = ItemBuilder.createItem(context, col1)
                                     .withTitle("Public item 1")
                                     .withIssueDate("2017-10-17")
                                     .withAuthor("Smith, Donald")
                                     .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withEmbargoPeriod("3 months")
                .build();
        }

        // Remove read policies on item of bitstream
        resourcePolicyService.removePolicies(context, publicItem1, Constants.READ);

        context.restoreAuthSystemState();

        // Bitstream metadata should not be accessible by anonymous request
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                   .andExpect(status().isUnauthorized())
        ;

        // Bitstream metadata should not be accessible by submitter
        String submitterToken = getAuthToken(context.getCurrentUser().getEmail(), password);
        getClient(submitterToken).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                                 .andExpect(status().isForbidden())
        ;

        // Bitstream metadata should be accessible by admin
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                             .andExpect(status().isOk())
        ;
    }

    @Test
    public void findOneBitstreamTest_EmbargoedBitstream_ePersonREADRightsOnItem() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setCurrentUser(eperson);

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();

        // a public item with an embargoed bitstream
        String bitstreamContent = "Embargoed!";

        Item publicItem1;
        Bitstream bitstream;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, org.apache.commons.lang3.CharEncoding.UTF_8)) {

            publicItem1 = ItemBuilder.createItem(context, col1)
                                     .withTitle("Public item 1")
                                     .withIssueDate("2017-10-17")
                                     .withAuthor("Smith, Donald")
                                     .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withEmbargoPeriod("3 months")
                .build();
        }

        // Replace anon read policy on item of bitstream with ePerson READ policy
        resourcePolicyService.removePolicies(context, publicItem1, Constants.READ);
        ResourcePolicyBuilder.createResourcePolicy(context).withUser(eperson)
                             .withAction(Constants.READ)
                             .withDspaceObject(publicItem1).build();

        context.restoreAuthSystemState();

        // Bitstream metadata should not be accessible by anonymous request
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                   .andExpect(status().isUnauthorized())
        ;

        // Bitstream metadata should be accessible by eperson
        String submitterToken = getAuthToken(context.getCurrentUser().getEmail(), password);
        getClient(submitterToken).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                                 .andExpect(status().isOk())
        ;

        // Bitstream metadata should be accessible by admin
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                             .andExpect(status().isOk())
        ;
    }

    @Test
    public void findOneBitstreamRelsTest() throws Exception {

        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                            createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("Description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        //Add a bitstream to an item
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                             createBitstream(context, publicItem1, is)
                                         .withName("Bitstream1")
                                         .withDescription("Description1234")
                                         .withMimeType("text/plain")
                                         .build();
        }

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/format"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", BitstreamFormatMatcher.matchBitstreamFormatMimeType("text/plain")))
        ;

        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                   .andExpect(status().isOk())
                   .andExpect(content().string("ThisIsSomeDummyText"))
        ;

    }

    @Test
    public void findOneLogoBitstreamTest() throws Exception {

        // We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // ** GIVEN **
        // 1. A community with a logo
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").withLogo("logo_community")
                                          .build();

        // 2. A collection with a logo
        Collection col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection")
                                          .withLogo("logo_collection").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/bitstreams/" + parentCommunity.getLogo().getID()))
                   .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + col.getLogo().getID())).andExpect(status().isOk());

    }

    @Test
    public void findOneLogoBitstreamRelsTest() throws Exception {

        // We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // ** GIVEN **
        // 1. A community with a logo
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").withLogo("logo_community")
                                          .build();

        // 2. A collection with a logo
        Collection col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection")
                                          .withLogo("logo_collection").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/bitstreams/" + parentCommunity.getLogo().getID() + "/content"))
                   .andExpect(status().isOk()).andExpect(content().string("logo_community"));

        getClient().perform(get("/api/core/bitstreams/" + col.getLogo().getID() + "/content"))
                   .andExpect(status().isOk()).andExpect(content().string("logo_collection"));
    }

    @Test
    public void findOneWrongUUID() throws Exception {

        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/core/bitstreams/" + UUID.randomUUID()))
                   .andExpect(status().isNotFound())
        ;

    }

    @Test
    public void deleteOne() throws Exception {

        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                            createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("Description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        // Delete
        getClient(token).perform(delete("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().is(204));

        // Verify 404 after delete
        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void deleteForbidden() throws Exception {

        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                            createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("Description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Delete using an unauthorized user
        getClient(token).perform(delete("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().isForbidden());

        // Verify the bitstream is still here
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteUnauthorized() throws Exception {

        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                            createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("Description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        context.restoreAuthSystemState();

        // Delete as anonymous
        getClient().perform(delete("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().isUnauthorized());

        // Verify the bitstream is still here
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteLogo() throws Exception {
        // We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // ** GIVEN **
        // 1. A community with a logo
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").withLogo("logo_community")
                                          .build();

        // 2. A collection with a logo
        Collection col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection")
                                          .withLogo("logo_collection").build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        // trying to DELETE parentCommunity logo should work
        getClient(token).perform(delete("/api/core/bitstreams/" + parentCommunity.getLogo().getID()))
                   .andExpect(status().is(204));

        // trying to DELETE collection logo should work
        getClient(token).perform(delete("/api/core/bitstreams/" + col.getLogo().getID()))
                   .andExpect(status().is(204));
    }

    @Test
    public void deleteMissing() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        // Delete
        getClient(token).perform(delete("/api/core/bitstreams/1c11f3f1-ba1f-4f36-908a-3f1ea9a557eb"))
                .andExpect(status().isNotFound());

        // Verify 404 after failed delete
        getClient(token).perform(delete("/api/core/bitstreams/1c11f3f1-ba1f-4f36-908a-3f1ea9a557eb"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void deleteDeleted() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald")
                .withSubject("ExtraEntry")
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream")
                    .withDescription("Description")
                    .withMimeType("text/plain")
                    .build();
        }

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        // Delete
        getClient(token).perform(delete("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().is(204));

        // Verify 404 when trying to delete a non-existing bitstream
        getClient(token).perform(delete("/api/core/bitstreams/" + bitstream.getID()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void patchBitstreamMetadataAuthorized() throws Exception {
        runPatchMetadataTests(admin, 200);
    }

    @Test
    public void patchBitstreamMetadataUnauthorized() throws Exception {
        runPatchMetadataTests(eperson, 403);
    }

    private void runPatchMetadataTests(EPerson asUser, int expectedStatus) throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").withLogo("logo_community")
                .build();
        context.restoreAuthSystemState();
        String token = getAuthToken(asUser.getEmail(), password);

        new MetadataPatchSuite().runWith(getClient(token), "/api/core/bitstreams/"
                + parentCommunity.getLogo().getID(), expectedStatus);
    }


    @Test
    public void testHiddenMetadataForAnonymousUser() throws Exception {

        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();


        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                                createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("Description")
                                        .withProvenance("Provenance Data")
                                        .withMimeType("text/plain")
                                        .build();
        }

        context.restoreAuthSystemState();

        // When full projection is requested, response should include expected properties, links, and embeds.
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID())
                                    .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", BitstreamMatcher.matchFullEmbeds()))
                   .andExpect(jsonPath("$", BitstreamMatcher.matchBitstreamEntry(bitstream)))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Bitstream")))
                   .andExpect(jsonPath("$.metadata", matchMetadataDoesNotExist("dc.description.provenance")))
        ;

    }

    @Test
    public void testHiddenMetadataForAdminUser() throws Exception {

        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();


        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                                createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("Description")
                                        .withProvenance("Provenance Data")
                                        .withMimeType("text/plain")
                                        .build();
        }

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        // When full projection is requested, response should include expected properties, links, and embeds.
        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID())
                                    .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", BitstreamMatcher.matchFullEmbeds()))
                   .andExpect(jsonPath("$", BitstreamMatcher.matchBitstreamEntry(bitstream)))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Bitstream")))
                   .andExpect(jsonPath("$.metadata", matchMetadata("dc.description.provenance", "Provenance Data")))
        ;

    }


    @Test
    public void testHiddenMetadataForUserWithWriteRights() throws Exception {

        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();


        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withSubject("ExtraEntry")
                                      .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                                                createBitstream(context, publicItem1, is)
                                        .withName("Bitstream")
                                        .withDescription("Description")
                                        .withProvenance("Provenance Data")
                                        .withMimeType("text/plain")
                                        .build();
        }

        ResourcePolicyBuilder.createResourcePolicy(context)
                             .withUser(eperson)
                             .withAction(WRITE)
                             .withDspaceObject(col1)
                             .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);


        // When full projection is requested, response should include expected properties, links, and embeds.
        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID())
                                         .param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", BitstreamMatcher.matchFullEmbeds()))
                        .andExpect(jsonPath("$", BitstreamMatcher.matchBitstreamEntry(bitstream)))
                        .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Bitstream")))
                        .andExpect(jsonPath("$.metadata", matchMetadataDoesNotExist("dc.description.provenance")))
        ;

    }

    @Test
    public void getEmbeddedBundleForBitstream() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald")
                .withSubject("ExtraEntry")
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";

        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream")
                    .withDescription("Description")
                    .withMimeType("text/plain")
                    .build();
        }

        Bundle bundle = bitstream.getBundles().get(0);

        //Get the bitstream with embedded bundle
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "?embed=bundle"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.bundle",
                        BundleMatcher.matchProperties(
                                bundle.getName(),
                                bundle.getID(),
                                bundle.getHandle(),
                                bundle.getType()
                        )
                ));
    }

    @Test
    /**
     * This test proves that, if a bitstream is linked to multiple bundles, we only ever return the first bundle.
     * **NOTE: DSpace does NOT support or expect to have a bitstream linked to multiple bundles**.
     * But, because the database does allow for it, this test simply proves the REST API will respond without an error.
     */
    public void linksToFirstBundleWhenMultipleBundles() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald")
                .withSubject("ExtraEntry")
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";

        //Add a bitstream to an item
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                    createBitstream(context, publicItem1, is)
                    .withName("Bitstream")
                    .withDescription("Description")
                    .withMimeType("text/plain")
                    .build();
        }

        // Add default content bundle to list of bundles
        List<Bundle> bundles = itemService.getBundles(publicItem1, Constants.CONTENT_BUNDLE_NAME);

        // Add this bitstream to a second bundle & append to list of bundles
        bundles.add(BundleBuilder.createBundle(context, publicItem1)
                .withName("second bundle")
                .withBitstream(bitstream).build());

        // While in DSpace code, Bundles are *unordered*, in Hibernate v5 + H2 v2.x, they are returned sorted by UUID.
        // So, we reorder this list of created Bundles by UUID to get their expected return order.
        // NOTE: Once on Hibernate v6, this might need "toString()" removed as it may sort UUIDs based on RFC 4412.
        Comparator<Bundle> compareByUUID = Comparator.comparing(b -> b.getID().toString());
        bundles.sort(compareByUUID);

        //Get bundle should contain the first bundle in the list
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/bundle"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$",
                        BundleMatcher.matchProperties(
                            bundles.get(0).getName(),
                            bundles.get(0).getID(),
                            bundles.get(0).getHandle(),
                            bundles.get(0).getType()
                    )
                ));
    }

    @Test
    public void linksToEmptyWhenNoBundle() throws Exception {
        // We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // ** GIVEN **
        // 1. A community with a logo
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").withLogo("logo_community")
                .build();

        // 2. A collection with a logo
        Collection col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection")
                .withLogo("logo_collection").build();

        Bitstream bitstream = parentCommunity.getLogo();

        //Get bundle should contain an empty response
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/bundle"))
                .andExpect(status().isNoContent());
    }


    @Test
    public void thumbnailEndpointTest() throws Exception {
        // Given an Item
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Test item -- thumbnail")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .build();

        Bundle originalBundle = BundleBuilder.createBundle(context, item)
                                             .withName(Constants.DEFAULT_BUNDLE_NAME)
                                             .build();
        Bundle thumbnailBundle = BundleBuilder.createBundle(context, item)
                                              .withName("THUMBNAIL")
                                              .build();

        InputStream is = IOUtils.toInputStream("dummy", "utf-8");

        // With an ORIGINAL Bitstream & matching THUMBNAIL Bitstream
        Bitstream bitstream = BitstreamBuilder.createBitstream(context, originalBundle, is)
                                              .withName("test.pdf")
                                              .withMimeType("application/pdf")
                                              .build();
        Bitstream thumbnail = BitstreamBuilder.createBitstream(context, thumbnailBundle, is)
                                              .withName("test.pdf.jpg")
                                              .withMimeType("image/jpeg")
                                              .build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/thumbnail"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.uuid", Matchers.is(thumbnail.getID().toString())))
                             .andExpect(jsonPath("$.type", is("bitstream")));
    }

    @Test
    public void thumbnailEndpointMultipleThumbnailsWithPrimaryBitstreamTest() throws Exception {
        // Given an Item
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Test item -- thumbnail")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .build();

        Bundle originalBundle = BundleBuilder.createBundle(context, item)
                                             .withName(Constants.DEFAULT_BUNDLE_NAME)
                                             .build();
        Bundle thumbnailBundle = BundleBuilder.createBundle(context, item)
                                              .withName("THUMBNAIL")
                                              .build();

        InputStream is = IOUtils.toInputStream("dummy", "utf-8");

        // With multiple ORIGINAL Bitstreams & matching THUMBNAIL Bitstreams
        Bitstream bitstream1 = BitstreamBuilder.createBitstream(context, originalBundle, is)
                                               .withName("test1.pdf")
                                               .withMimeType("application/pdf")
                                               .build();
        Bitstream bitstream2 = BitstreamBuilder.createBitstream(context, originalBundle, is)
                                               .withName("test2.pdf")
                                               .withMimeType("application/pdf")
                                               .build();
        Bitstream primaryBitstream = BitstreamBuilder.createBitstream(context, originalBundle, is)
                                                     .withName("test3.pdf")
                                                     .withMimeType("application/pdf")
                                                     .build();

        Bitstream thumbnail1 = BitstreamBuilder.createBitstream(context, thumbnailBundle, is)
                                               .withName("test1.pdf.jpg")
                                               .withMimeType("image/jpeg")
                                               .build();
        Bitstream thumbnail2 = BitstreamBuilder.createBitstream(context, thumbnailBundle, is)
                                               .withName("test2.pdf.jpg")
                                               .withMimeType("image/jpeg")
                                               .build();
        Bitstream primaryThumbnail = BitstreamBuilder.createBitstream(context, thumbnailBundle, is)
                                                     .withName("test3.pdf.jpg")
                                                     .withMimeType("image/jpeg")
                                                     .build();

        // and a primary Bitstream
        originalBundle.setPrimaryBitstreamID(primaryBitstream);

        context.restoreAuthSystemState();

        // Bitstream thumbnail endpoints should link to the right thumbnail Bitstreams
        getClient().perform(get("/api/core/bitstreams/" + primaryBitstream.getID() + "/thumbnail"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.uuid", Matchers.is(primaryThumbnail.getID().toString())))
                   .andExpect(jsonPath("$.type", is("bitstream")));

        getClient().perform(get("/api/core/bitstreams/" + bitstream1.getID() + "/thumbnail"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.uuid", Matchers.is(thumbnail1.getID().toString())))
                   .andExpect(jsonPath("$.type", is("bitstream")));

        getClient().perform(get("/api/core/bitstreams/" + bitstream2.getID() + "/thumbnail"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.uuid", Matchers.is(thumbnail2.getID().toString())))
                   .andExpect(jsonPath("$.type", is("bitstream")));
    }

    @Test
    public void thumbnailEndpointItemWithoutThumbnailsTest() throws Exception {
        // Given an Item
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1").build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Test item -- thumbnail")
                               .withIssueDate("2017-10-17")
                               .withAuthor("Smith, Donald").withAuthor("Doe, John")
                               .build();

        Bundle originalBundle = BundleBuilder.createBundle(context, item)
                                             .withName(Constants.DEFAULT_BUNDLE_NAME)
                                             .build();
        // With an empty THUMBNAIL bundle
        Bundle thumbnailBundle = BundleBuilder.createBundle(context, item)
                                              .withName("THUMBNAIL")
                                              .build();

        InputStream is = IOUtils.toInputStream("dummy", "utf-8");

        Bitstream bitstream = BitstreamBuilder.createBitstream(context, originalBundle, is)
                        .withName("test.pdf")
                        .withMimeType("application/pdf")
                        .build();

        context.restoreAuthSystemState();

        // Should fail with HTTP 204
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/thumbnail"))
                   .andExpect(status().isNoContent());

        // With a THUMBNAIL bitstream that doesn't match the ORIGINAL Bitstream's name
        context.turnOffAuthorisationSystem();
        BitstreamBuilder.createBitstream(context, thumbnailBundle, is)
                        .withName("random.pdf.jpg")
                        .withMimeType("image/jpeg")
                        .build();
        context.restoreAuthSystemState();

        // Should still fail with HTTP 204
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/thumbnail"))
                   .andExpect(status().isNoContent());
    }

    @Test
    public void findByHandleAndFileNameForPublicItem() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();

        String bitstreamContent = "This is an archived bitstream";
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                                createBitstream(context, publicItem1, is)
                                        .withName("Bitstream1")
                                        .withMimeType("text/plain")
                                        .build();
        }

        bitstreamContent = "This is an archived bitstream";
        Bitstream bitstream2 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream2 = BitstreamBuilder.
                                                 createBitstream(context, publicItem1, is)
                                         .withName("Bitstream2")
                                         .withMimeType("text/plain")
                                         .build();
        }



        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("handle", publicItem1.getHandle())
                                    .param("filename", "Bitstream1")
        )
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$",
                                       BitstreamMatcher.matchProperties(bitstream1)
                   ));

        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("handle", publicItem1.getHandle())
                                    .param("filename", "Bitstream2")
        )
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$",
                                       BitstreamMatcher.matchProperties(bitstream2)
                   ));
    }

    @Test
    public void findByHandleAndFileNameForEmbargoItem() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item embargoItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .withEmbargoPeriod("6 months")
                                      .build();

        String bitstreamContent = "This is an archived bitstream";
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                                createBitstream(context, embargoItem1, is)
                                        .withName("Bitstream1")
                                        .withMimeType("text/plain")
                                        .build();
        }

        Group group = groupService.findByName(context, Group.ANONYMOUS);

        bitstreamContent = "This is an archived bitstream";
        Bitstream bitstream2 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream2 = BitstreamBuilder.
                                                 createBitstream(context, embargoItem1, is)
                                         .withName("Bitstream2")
                                         .withMimeType("text/plain")
                                         .withReaderGroup(group)
                                         .build();
        }


        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("handle", embargoItem1.getHandle())
                                    .param("filename", "Bitstream1")
        )
                   .andExpect(status().isNoContent());

        String token = getAuthToken(eperson.getEmail(), password);
        String admintoken = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("handle", embargoItem1.getHandle())
                                    .param("filename", "Bitstream1")
        )
                   .andExpect(status().isNoContent());

        getClient(admintoken).perform(get("/api/core/bitstreams/search/byItemHandle")
                                         .param("handle", embargoItem1.getHandle())
                                         .param("filename", "Bitstream1")
        )
                             .andExpect(status().isOk())
                             .andExpect(content().contentType(contentType))
                             .andExpect(jsonPath("$",
                                                 BitstreamMatcher.matchProperties(bitstream1)
                             ));

        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("handle", embargoItem1.getHandle())
                                    .param("filename", "Bitstream2")
        )
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$",
                                       BitstreamMatcher.matchProperties(bitstream2)
                   ));
    }
    @Test
    public void findByHandleAndFileNameForPrivateItem() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item privateItem1 = ItemBuilder.createItem(context, col1)
                                       .withTitle("Test")
                                       .withIssueDate("2010-10-17")
                                       .withAuthor("Smith, Donald")
                                       .makeUnDiscoverable()
                                       .build();

        String bitstreamContent = "This is an archived bitstream";
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                                createBitstream(context, privateItem1, is)
                                        .withName("Bitstream1")
                                        .withMimeType("text/plain")
                                        .build();
        }

        bitstreamContent = "This is an archived bitstream";
        Bitstream bitstream2 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream2 = BitstreamBuilder.
                                                 createBitstream(context, privateItem1, is)
                                         .withName("Bitstream2")
                                         .withMimeType("text/plain")
                                         .build();
        }



        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("handle", privateItem1.getHandle())
                                    .param("filename", "Bitstream1")
        )
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$",
                           BitstreamMatcher.matchProperties(bitstream1)
                   ));

        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("handle", privateItem1.getHandle())
                                    .param("filename", "Bitstream2")
        )
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$",
                           BitstreamMatcher.matchProperties(bitstream2)
                   ));
    }

    @Test
    public void findByHandleAndFileNameForPublicItemWithSameFileName() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();

        String bitstreamContent = "This is an archived bitstream";
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                                 createBitstream(context, publicItem1, is)
                                         .withName("BitstreamName")
                                         .withMimeType("text/plain")
                                         .build();
        }

        bitstreamContent = "This is an archived bitstream";
        Bitstream bitstream2 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream2 = BitstreamBuilder.
                                                 createBitstream(context, publicItem1, is)
                                         .withName("BitstreamName")
                                         .withMimeType("text/plain")
                                         .build();
        }



        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("handle", publicItem1.getHandle())
                                    .param("sequence", String.valueOf(bitstream1.getSequenceID()))
        )
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$",
                                       BitstreamMatcher.matchProperties(bitstream1)
                   ));

        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("handle", publicItem1.getHandle())
                                    .param("sequence", String.valueOf(bitstream2.getSequenceID()))

        )
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$",
                                       BitstreamMatcher.matchProperties(bitstream2)
                   ));
        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("handle", publicItem1.getHandle())
                                    .param("filename", "BitstreamName")
        )
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$",
                                       BitstreamMatcher.matchProperties(bitstream1)
                   ));
    }

    @Test
    public void findByHandleAndFileNameForPublicItemInLicenseBundle() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();

        Bundle license = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("LICENSE")
                                      .build();

        String bitstreamContent = "This is an archived bitstream";
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                                 createBitstream(context, license, is)
                                         .withName("BitstreamName")
                                         .withMimeType("text/plain")
                                         .build();
        }


        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("handle", publicItem1.getHandle())
                                    .param("sequence", String.valueOf(bitstream1.getSequenceID()))
        )
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$",
                                       BitstreamMatcher.matchProperties(bitstream1)
                   ));

        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("handle", publicItem1.getHandle())
                                    .param("filename", "BitstreamName")
        )
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$",
                                       BitstreamMatcher.matchProperties(bitstream1)
                   ));
    }

    @Test
    public void findByHandleAndFileNameForPublicItemWithCorrectSequenceButWrongName() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();

        Bundle license = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("LICENSE")
                                      .build();

        String bitstreamContent = "This is an archived bitstream";
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                                 createBitstream(context, license, is)
                                         .withName("BitstreamName")
                                         .withMimeType("text/plain")
                                         .build();
        }


        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("handle", publicItem1.getHandle())
                                    .param("sequence", String.valueOf(bitstream1.getSequenceID()))
                                    .param("filename", "WrongBitstreamName")

        )
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$",
                                       BitstreamMatcher.matchProperties(bitstream1)
                   ));
    }

    @Test
    public void findByHandleAndFileNameForPublicItemWithFaultyHandle() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();

        Bundle license = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("LICENSE")
                                      .build();

        String bitstreamContent = "This is an archived bitstream";
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                                 createBitstream(context, license, is)
                                         .withName("BitstreamName")
                                         .withMimeType("text/plain")
                                         .build();
        }

        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("sequence", String.valueOf(bitstream1.getSequenceID()))
                                    .param("filename", "WrongBitstreamName")

        )
                   .andExpect(status().isBadRequest());

        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("handle", "123456789/999999999999")
                                    .param("sequence", String.valueOf(bitstream1.getSequenceID()))
                                    .param("filename", "WrongBitstreamName")

        )
                   .andExpect(status().isUnprocessableEntity());

        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("handle", parentCommunity.getHandle())
                                    .param("sequence", String.valueOf(bitstream1.getSequenceID()))
                                    .param("filename", "WrongBitstreamName")

        )
                   .andExpect(status().isUnprocessableEntity());

        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("handle", col1.getHandle())
                                    .param("sequence", String.valueOf(bitstream1.getSequenceID()))
                                    .param("filename", "WrongBitstreamName")

        )
                   .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void findByHandleAndFileNameForPublicItemNoFileNameOrSequence() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();

        Bundle license = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("LICENSE")
                                      .build();

        String bitstreamContent = "This is an archived bitstream";
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                                 createBitstream(context, license, is)
                                         .withName("BitstreamName")
                                         .withMimeType("text/plain")
                                         .build();
        }


        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("handle", publicItem1.getHandle())
        )
                   .andExpect(status().isBadRequest());

    }

    @Test
    public void findByHandleAndFileNameForPublicItemNoContent() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();

        Bundle license = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("LICENSE")
                                      .build();

        String bitstreamContent = "This is an archived bitstream";
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                                                 createBitstream(context, license, is)
                                         .withName("BitstreamName")
                                         .withMimeType("text/plain")
                                         .build();
        }

        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("handle", publicItem1.getHandle())
                                    .param("filename", "WrongBitstreamName")

        )
                   .andExpect(status().isNoContent());

        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                    .param("handle", publicItem1.getHandle())
                                    .param("sequence", "999999")

        )
                   .andExpect(status().isNoContent());
    }

    @Test
    public void findByHandleAndFileNameForPublicItemWithEmbargoOnFile() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();

        String bitstreamContent = "Embargoed bitstream";
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withEmbargoPeriod("3 months")
                .build();
        }

        // Embargo on bitstream, but item is public, so has read_metadata access on bitstream
        getClient().perform(get("/api/core/bitstreams/search/byItemHandle")
                                              .param("handle", publicItem1.getHandle())
                                              .param("sequence", String.valueOf(bitstream1.getSequenceID()))

        )
                             .andExpect(status().isOk())
                             .andExpect(content().contentType(contentType))
                             .andExpect(jsonPath("$",
                                                 BitstreamMatcher.matchProperties(bitstream1)
                             ));
    }

    @Test
    public void findByItemIdWithoutRequiredParameters() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/bitstreams/search/byItemId")
                       .param("uuid", publicItem1.getID().toString()))
                   .andExpect(status().isBadRequest());

    }

    @Test
    public void findByItemIdWithMetadataFieldsAndValuesWithDifferentCardinality() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/bitstreams/search/byItemId")
                       .param("uuid", publicItem1.getID().toString())
                       .param("name", "bundle name")
                       .param("filterMetadata", "dc.title"))
                   .andExpect(status().isBadRequest());

        getClient().perform(get("/api/core/bitstreams/search/byItemId")
                       .param("uuid", publicItem1.getID().toString())
                       .param("name", "bundle name")
                       .param("filterMetadata", "dc.title")
                       .param("filterMetadataValue", "Test", "Test 2"))
                   .andExpect(status().isBadRequest());

        getClient().perform(get("/api/core/bitstreams/search/byItemId")
                       .param("uuid", publicItem1.getID().toString())
                       .param("name", "bundle name")
                       .param("filterMetadata", "dc.title", "dc.date.issued", "dc.description")
                       .param("filterMetadataValue", "Test", "Test 2"))
                   .andExpect(status().isBadRequest());

    }

    @Test
    public void findByFakeItemId() throws Exception {

        String fakeId = "9cc8104e-5337-4305-b4ce-b578eb1b24ba";

        getClient().perform(get("/api/core/bitstreams/search/byItemId")
                       .param("uuid", fakeId)
                       .param("name", "bundle name")
                       .param("filterMetadata", "dc.title")
                       .param("filterMetadataValue", "test"))
                   .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void findByItemIdWithNoMatchedBitstreams() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();

        Bundle license = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("LICENSE")
                                      .build();

        String bitstreamContent1 = "This is an archived bitstream";
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent1, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                createBitstream(context, license, is)
                .withName("license bitstream name")
                .withMimeType("text/plain")
                .build();
        }

        getClient().perform(get("/api/core/bitstreams/search/byItemId")
                       .param("uuid", publicItem1.getID().toString())
                       .param("name", license.getName())
                       .param("filterMetadata", "dc.title")
                       .param("filterMetadataValue", "wrong value"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$.page.totalElements", is(0)));

    }

    @Test
    public void findByItemIdAndBundleNameAndMetadataValue() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();

        Bundle license = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("LICENSE")
                                      .build();

        String bitstreamContent1 = "This is an archived bitstream";
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent1, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                createBitstream(context, license, is)
                .withName("this is a test")
                .withMimeType("text/plain")
                .build();
        }

        String bitstreamContent2 = "This is an license bitstream";
        Bitstream bitstream2 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent2, CharEncoding.UTF_8)) {
            bitstream2 = BitstreamBuilder.
                createBitstream(context, license, is)
                .withName("this is a test 2")
                .withMimeType("text/plain")
                .build();
        }

        getClient().perform(get("/api/core/bitstreams/search/byItemId")
                       .param("uuid", publicItem1.getID().toString())
                       .param("name", license.getName())
                       .param("filterMetadata", "dc.title")
                       .param("filterMetadataValue", "this is a test")
                       .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(1)))
                   .andExpect(jsonPath("$._embedded.bitstreams", hasSize(1)))
                   .andExpect(jsonPath("$._embedded.bitstreams", contains(
                       BitstreamMatcher.matchBitstreamEntry(bitstream1)
                   )));

        getClient().perform(get("/api/core/bitstreams/search/byItemId")
                       .param("uuid", publicItem1.getID().toString())
                       .param("name", license.getName())
                       .param("filterMetadata", "dc.title")
                       .param("filterMetadataValue", "([a-z]+ [a-z]+ [a-z]+ [a-z]+)")
                       .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(1)))
                   .andExpect(jsonPath("$._embedded.bitstreams", hasSize(1)))
                   .andExpect(jsonPath("$._embedded.bitstreams", contains(
                       BitstreamMatcher.matchBitstreamEntry(bitstream1)
                   )));

        getClient().perform(get("/api/core/bitstreams/search/byItemId")
                       .param("uuid", publicItem1.getID().toString())
                       .param("name", license.getName())
                       .param("filterMetadata", "dc.title")
                       .param("filterMetadataValue", "(this is a test.*)")
                       .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(2)))
                   .andExpect(jsonPath("$._embedded.bitstreams", hasSize(2)))
                   .andExpect(jsonPath("$._embedded.bitstreams", containsInAnyOrder(
                       BitstreamMatcher.matchBitstreamEntry(bitstream1),
                       BitstreamMatcher.matchBitstreamEntry(bitstream2)
                   )));

    }

    @Test
    public void findByItemIdAndBundleName() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();

        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Test")
                                      .withIssueDate("2010-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();

        Bundle license = BundleBuilder.createBundle(context, publicItem1)
                                      .withName("LICENSE")
                                      .build();

        Bundle original = BundleBuilder.createBundle(context, publicItem1)
                                       .withName("ORIGINAL")
                                       .build();

        String bitstreamContent1 = "This is an archived bitstream";
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent1, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.
                createBitstream(context, license, is)
                .withName("this is a test")
                .withMimeType("text/plain")
                .build();
        }

        String bitstreamContent2 = "This is an original bitstream";
        Bitstream bitstream2 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent2, CharEncoding.UTF_8)) {
            bitstream2 = BitstreamBuilder.
                createBitstream(context, original, is)
                .withName("original bitstream name")
                .withMimeType("text/plain")
                .build();
        }

        getClient().perform(get("/api/core/bitstreams/search/byItemId")
                       .param("uuid", publicItem1.getID().toString())
                       .param("name", license.getName())
                       .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(1)))
                   .andExpect(jsonPath("$._embedded.bitstreams", hasSize(1)))
                   .andExpect(jsonPath("$._embedded.bitstreams", contains(
                       BitstreamMatcher.matchBitstreamEntry(bitstream1)
                   )));

    }

    @Test
    public void searchByItemWithManyFilterMetadata() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection 1")
            .build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .build();

        Bundle license = BundleBuilder.createBundle(context, publicItem1)
            .withName("LICENSE")
            .build();

        Bitstream bitstream1 = BitstreamBuilder.createBitstream(context, license, InputStream.nullInputStream())
            .withName("this is a test")
            .withType("Image")
            .withMimeType("text/plain")
            .build();

        Bitstream bitstream2 = BitstreamBuilder.createBitstream(context, license, InputStream.nullInputStream())
            .withName("this is a test")
            .withType("Personal Picture")
            .withMimeType("text/plain")
            .build();

        Bitstream bitstream3 = BitstreamBuilder.createBitstream(context, license, InputStream.nullInputStream())
            .withName("this is a test 2")
            .withType("Personal Picture")
            .withMimeType("text/plain")
            .build();

        getClient().perform(get("/api/core/bitstreams/search/byItemId")
            .param("uuid", publicItem1.getID().toString())
            .param("name", license.getName())
            .param("filterMetadata", "dc.title")
            .param("filterMetadataValue", "this is a test")
            .param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(2)))
            .andExpect(jsonPath("$._embedded.bitstreams", containsInAnyOrder(
                BitstreamMatcher.matchBitstreamEntry(bitstream1),
                BitstreamMatcher.matchBitstreamEntry(bitstream2))));

        getClient().perform(get("/api/core/bitstreams/search/byItemId")
            .param("uuid", publicItem1.getID().toString())
            .param("name", license.getName())
            .param("filterMetadata", "dc.type")
            .param("filterMetadataValue", "Personal Picture")
            .param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(2)))
            .andExpect(jsonPath("$._embedded.bitstreams", containsInAnyOrder(
                BitstreamMatcher.matchBitstreamEntry(bitstream2),
                BitstreamMatcher.matchBitstreamEntry(bitstream3))));

        getClient().perform(get("/api/core/bitstreams/search/byItemId")
            .param("uuid", publicItem1.getID().toString())
            .param("name", license.getName())
            .param("filterMetadata", "dc.title", "dc.type")
            .param("filterMetadataValue", "this is a test", "Personal Picture")
            .param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(1)))
            .andExpect(jsonPath("$._embedded.bitstreams", hasSize(1)))
            .andExpect(jsonPath("$._embedded.bitstreams", contains(
                BitstreamMatcher.matchBitstreamEntry(bitstream2))));

    }

    @Test
    public void findShowableByItem() throws Exception {

        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and two collections.
        parentCommunity =
            CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. A public item that is readable by Anonymous
        Item publicItem1 =
            ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .build();

        Item publicItem2 =
            ItemBuilder.createItem(context, col1)
            .withTitle("Public item 1")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald")
            .build();

        Bitstream bitstream =
            BitstreamBuilder.createBitstream(context, publicItem1, toInputStream("test", UTF_8))
                .withName("first")
                .withFormat("test format")
                .build();

        Bitstream bitstream2 =
            BitstreamBuilder.createBitstream(context, publicItem1, toInputStream("test2", UTF_8))
                .withName("second")
                .withFormat("test format 2")
                .build();

        final String customBundle = "Bundle Test";
        Bundle bundle = BundleBuilder.createBundle(context, publicItem1).withName(customBundle).build();

        Bitstream bitstream3 =
            BitstreamBuilder.createBitstream(context, bundle, toInputStream("test3", UTF_8))
                .withName("third")
                .withFormat("test format 3")
                .build();

        Bitstream bitstream4 =
            BitstreamBuilder.createBitstream(context, publicItem2, toInputStream("test4", UTF_8))
                .withName("fourth")
                .withFormat("test format 4")
                .isHidden()
                .build();

        context.commit();
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(
                get("/api/core/bitstreams/search/showableByItem")
                    .param("uuid", publicItem1.getID().toString())
                    .param("name", Constants.CONTENT_BUNDLE_NAME)
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(
                jsonPath(
                    "$._embedded.bitstreams",
                    allOf(
                        hasItem(BitstreamMatcher.matchProperties(bitstream)),
                        hasItem(BitstreamMatcher.matchProperties(bitstream2))
                    )
                )
            )
            .andExpect(
                jsonPath(
                    "$._embedded.bitstreams",
                     not(
                         allOf(
                             hasItem(BitstreamMatcher.matchProperties(bitstream3)),
                             hasItem(BitstreamMatcher.matchProperties(bitstream4))
                         )
                     )
                )
            );

        getClient(token).perform(
            get("/api/core/bitstreams/search/showableByItem")
            .param("uuid", publicItem1.getID().toString())
            .param("name", customBundle)
            )
        .andExpect(status().isOk())
        .andExpect(content().contentType(contentType))
        .andExpect(
            jsonPath(
                "$._embedded.bitstreams",
                allOf(
                    hasItem(BitstreamMatcher.matchProperties(bitstream3))
                    )
                )
            )
        .andExpect(
            jsonPath(
                "$._embedded.bitstreams",
                not(
                    allOf(
                        hasItem(BitstreamMatcher.matchProperties(bitstream)),
                        hasItem(BitstreamMatcher.matchProperties(bitstream2)),
                        hasItem(BitstreamMatcher.matchProperties(bitstream4))
                        )
                    )
                )
            );
    }

    @Test
    public void findShowableByItemWithoutBundleName() throws Exception {

        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();

        parentCommunity =
            CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        Item publicItem1 =
            ItemBuilder.createItem(context, col1)
            .withTitle("Public item 1")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald")
            .build();

        Item publicItem2 =
            ItemBuilder.createItem(context, col1)
            .withTitle("Public item 1")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald")
            .build();

        Bitstream bitstream =
            BitstreamBuilder.createBitstream(context, publicItem1, toInputStream("test", UTF_8))
            .withName("first")
            .withFormat("test format")
            .build();

        Bitstream bitstream2 =
            BitstreamBuilder.createBitstream(context, publicItem1, toInputStream("test2", UTF_8))
            .withName("second")
            .withFormat("test format 2")
            .build();

        final String customBundle = "Bundle Test";
        Bundle bundle = BundleBuilder.createBundle(context, publicItem1).withName(customBundle).build();

        Bitstream bitstream3 =
            BitstreamBuilder.createBitstream(context, bundle, toInputStream("test3", UTF_8))
            .withName("third")
            .withFormat("test format 3")
            .build();

        Bitstream bitstream4 =
            BitstreamBuilder.createBitstream(context, publicItem2, toInputStream("test4", UTF_8))
            .withName("fourth")
            .withFormat("test format 4")
            .isHidden()
            .build();

        context.commit();
        context.restoreAuthSystemState();

        List<UUID> bitstreamsID = StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                this.bitstreamService
                .findShowableByItem(context, publicItem1.getID(), Optional.empty()),
                0
                ),
            false
            )
            .map(Bitstream::getID)
            .collect(Collectors.toList());

        MatcherAssert.assertThat(
            bitstreamsID,
            containsInAnyOrder(
                bitstream.getID(),
                bitstream2.getID(),
                bitstream3.getID()
                )
            );

        MatcherAssert.assertThat(
            bitstreamsID,
            not(
                contains(
                    bitstream4.getID()
                    )
                )
            );
    }

}
