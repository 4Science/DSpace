/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static jakarta.mail.internet.MimeUtility.encodeText;
import static java.util.UUID.randomUUID;
import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.dspace.app.rest.matcher.BitstreamFormatMatcher.matchBitstreamFormat;
import static org.dspace.builder.BitstreamFormatBuilder.createBitstreamFormat;
import static org.dspace.builder.ResourcePolicyBuilder.createResourcePolicy;
import static org.dspace.content.BitstreamFormat.KNOWN;
import static org.dspace.content.BitstreamFormat.SUPPORTED;
import static org.dspace.core.Constants.DEFAULT_BITSTREAM_READ;
import static org.dspace.core.Constants.READ;
import static org.dspace.core.Constants.WRITE;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.time.Period;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.disseminate.CitationDocumentServiceImpl;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLoggerServiceImpl;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;
import org.dspace.storage.bitstore.factory.StorageServiceFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

/**
 * Integration test to test the /api/core/bitstreams/[id]/* endpoints
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 */
public class BitstreamRestControllerIT extends AbstractControllerIntegrationTest {

    public static final String[] PASS_ONLY = {"org.dspace.authenticate.PasswordAuthentication"};

    protected SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private CitationDocumentServiceImpl citationDocumentService;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private ResourcePolicyService resourcePolicyService;

    @Autowired
    private BitstreamFormatService bitstreamFormatService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private CollectionService collectionService;

    private Bitstream bitstream;
    private BitstreamFormat supportedFormat;
    private BitstreamFormat knownFormat;
    private BitstreamFormat unknownFormat;

    @BeforeClass
    public static void clearStatistics() throws Exception {
        // To ensure these tests start "fresh", clear out any existing statistics data.
        // NOTE: this is committed immediately in removeIndex()
        StatisticsServiceFactory.getInstance().getSolrLoggerService().removeIndex("*:*");
    }

    @Before
    @Override
    public void setUp() throws Exception {

        super.setUp();

        configurationService.setProperty("citation-page.enable_globally", false);

        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).build();

        bitstream = BitstreamBuilder.createBitstream(context, item, toInputStream("test", UTF_8))
                .withFormat("test format")
                .build();

        unknownFormat = bitstreamFormatService.findUnknown(context);

        knownFormat = createBitstreamFormat(context)
                                            .withMimeType("known test mime type")
                                            .withDescription("known test description")
                                            .withShortDescription("known test short description")
                                            .withSupportLevel(KNOWN)
                                            .build();

        supportedFormat = createBitstreamFormat(context)
                .withMimeType("supported mime type")
                .withDescription("supported description")
                .withShortDescription("supported short description")
                .withSupportLevel(SUPPORTED)
                .build();

        bitstream.setFormat(context, supportedFormat);

        context.restoreAuthSystemState();
    }

    @Test
    public void retrieveFullBitstream() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. A public item with a bitstream
        String bitstreamContent = "0123456789";

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

            Item publicItem1 = ItemBuilder.createItem(context, col1)
                                          .withTitle("Public item 1")
                                          .withIssueDate("2017-10-17")
                                          .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                          .build();

             bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test bitstream")
                .withDescription("This is a bitstream to test range requests")
                .withMimeType("text/plain")
                .build();
        }
        context.restoreAuthSystemState();

            //** WHEN **
            // we want to know what we are downloading before we download it
            getClient().perform(head("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                       //** THEN **
                       .andExpect(status().isOk())

                       //The Content Length must match the full length
                       .andExpect(header().longValue("Content-Length", bitstreamContent.getBytes().length))
                       .andExpect(header().string("Content-Type", "text/plain;charset=UTF-8"))
                       .andExpect(header().string("ETag", "\"" + bitstream.getChecksum() + "\""))
                       .andExpect(content().bytes(new byte[] {}));

            //** WHEN **
            //We download the bitstream
            getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))

                       //** THEN **
                       .andExpect(status().isOk())

                       //The Content Length must match the full length
                       .andExpect(header().longValue("Content-Length", bitstreamContent.getBytes().length))
                       //The server should indicate we support Range requests
                       .andExpect(header().string("Accept-Ranges", "bytes"))
                       //The ETag has to be based on the checksum
                       // We're checking this with quotes because it is required:
                       // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag
                       .andExpect(header().string("ETag", "\"" + bitstream.getChecksum() + "\""))
                       //We expect the content type to match the bitstream mime type
                       .andExpect(content().contentType("text/plain;charset=UTF-8"))
                       //THe bytes of the content must match the original content
                       .andExpect(content().bytes(bitstreamContent.getBytes()));

            //A If-None-Match HEAD request on the ETag must tell is the bitstream is not modified
            getClient().perform(head("/api/core/bitstreams/" + bitstream.getID() + "/content")
                                    .header("If-None-Match", bitstream.getChecksum()))
                       .andExpect(status().isNotModified());

            //The download and head request should also be logged as a statistics record
            checkNumberOfStatsRecords(bitstream, 3);
    }

    @Test
    public void retrieveRangeBitstream() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. A public item with a bitstream
        String bitstreamContent = "0123456789";

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

            Item publicItem1 = ItemBuilder.createItem(context, col1)
                                          .withTitle("Public item 1")
                                          .withIssueDate("2017-10-17")
                                          .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                          .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test bitstream")
                .withDescription("This is a bitstream to test range requests")
                .withMimeType("text/plain")
                .build();
        }
        context.restoreAuthSystemState();

            //** WHEN **
            //We download only a specific byte range of the bitstream
            getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content")
                                    .header("Range", "bytes=1-3"))

                       //** THEN **
                       .andExpect(status().is(206))

                       //The Content Length must match the requested range
                       .andExpect(header().longValue("Content-Length", 3))
                       //The server should indicate we support Range requests
                       .andExpect(header().string("Accept-Ranges", "bytes"))
                       //The ETag has to be based on the checksum
                       .andExpect(header().string("ETag", "\"" + bitstream.getChecksum() + "\""))
                       //The response should give us details about the range
                       .andExpect(header().string("Content-Range", "bytes 1-3/10"))
                       //We expect the content type to match the bitstream mime type
                       .andExpect(content().contentType("text/plain;charset=UTF-8"))
                       //We only expect the bytes 1, 2 and 3
                       .andExpect(content().bytes("123".getBytes()));

            //** WHEN **
            //We download the rest of the range
            getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content")
                                    .header("Range", "bytes=4-"))

                       //** THEN **
                       .andExpect(status().is(206))

                       //The Content Length must match the requested range
                       .andExpect(header().longValue("Content-Length", 6))
                       //The server should indicate we support Range requests
                       .andExpect(header().string("Accept-Ranges", "bytes"))
                       //The ETag has to be based on the checksum
                       .andExpect(header().string("ETag", "\"" + bitstream.getChecksum() + "\""))
                       //The response should give us details about the range
                       .andExpect(header().string("Content-Range", "bytes 4-9/10"))
                       //We expect the content type to match the bitstream mime type
                       .andExpect(content().contentType("text/plain;charset=UTF-8"))
                       //We all remaining bytes, starting at byte 4
                       .andExpect(content().bytes("456789".getBytes()));

            //Check that NO statistics record was logged for the Range requests
            checkNumberOfStatsRecords(bitstream, 0);
    }

    @Test
    public void testBitstreamName() throws Exception {

        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collection

        parentCommunity = CommunityBuilder
            .createCommunity(context)
            .build();

        Collection collection = CollectionBuilder
            .createCollection(context, parentCommunity)
            .build();

        //2. A public item with a bitstream

        String bitstreamContent = "0123456789";
        String bitstreamName = "ภาษาไทย";

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

            Item item = ItemBuilder
                .createItem(context, collection)
                .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, item, is)
                .withName(bitstreamName)
                .build();
        }

        context.restoreAuthSystemState();

        //** WHEN **
        //We download the bitstream
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
            //** THEN **
            .andExpect(status().isOk())
            //We expect the content disposition to have the encoded bitstream name
            .andExpect(header().string(
                "Content-Disposition",
                "attachment;filename=\"" + encodeText(bitstreamName) + "\""
            ));
    }

    @Test
    public void testBitstreamNotFound() throws Exception {
        getClient().perform(get("/api/core/bitstreams/" + UUID.randomUUID() + "/content"))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void testEmbargoedBitstream() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. A public item with an embargoed bitstream
        String bitstreamContent = "Embargoed!";

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

            Item publicItem1 = ItemBuilder.createItem(context, col1)
                                          .withTitle("Public item 1")
                                          .withIssueDate("2017-10-17")
                                          .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                          .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withEmbargoPeriod(Period.ofMonths(6))
                .build();
        }
        context.restoreAuthSystemState();

            //** WHEN **
            //We download the bitstream
            getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))

                       //** THEN **
                       .andExpect(status().isUnauthorized());

            //An unauthorized request should not log statistics
            checkNumberOfStatsRecords(bitstream, 0);
    }

    @Test
    public void embargoedBitstreamForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .build();

        // a public item with an embargoed bitstream
        String bitstreamContent = "Embargoed!";

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

            Item publicItem1 = ItemBuilder.createItem(context, col1)
                                          .withTitle("Public item 1")
                                          .withIssueDate("2017-10-17")
                                          .withAuthor("Smith, Donald")
                                          .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withEmbargoPeriod(Period.ofMonths(3))
                .build();
        }
            context.restoreAuthSystemState();

            String authToken = getAuthToken(eperson.getEmail(), password);
            getClient(authToken).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                       .andExpect(status().isForbidden());

            getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                       .andExpect(status().isUnauthorized());

            checkNumberOfStatsRecords(bitstream, 0);
    }

    @Test
    public void expiredEmbargoedBitstreamTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .build();


        String bitstreamContent = "Embargoed!";

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

            Item publicItem1 = ItemBuilder.createItem(context, col1)
                    .withTitle("Public item 1")
                    .withIssueDate("2015-10-17")
                    .withAuthor("Smith, Donald")
                    .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withEmbargoPeriod(Period.ofMonths(-3))
                .build();
        }
            context.restoreAuthSystemState();

            // all  are allowed access to item with embargoed expired

            String authToken = getAuthToken(eperson.getEmail(), password);
            getClient(authToken).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                       .andExpect(status().isOk());

            getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                       .andExpect(status().isOk());

            checkNumberOfStatsRecords(bitstream, 2);
    }

    @Test
    public void embargoedBitstreamAccessGrantByAdminsTest() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson adminParentCommunity = EPersonBuilder.createEPerson(context)
                .withEmail("adminCommunity@mail.com")
                .withPassword("qwerty02")
                .build();
        parentCommunity = CommunityBuilder.createCommunity(context)
                   .withName("Parent Community")
                   .withAdminGroup(adminParentCommunity)
                   .build();

        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                   .withName("Sub Community")
                   .build();

        EPerson adminChild2 = EPersonBuilder.createEPerson(context)
                .withEmail("adminChil2@mail.com")
                .withPassword("qwerty05")
                .build();
        Community child2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community 2")
                .withAdminGroup(adminChild2)
                .build();

        EPerson adminCollection1 = EPersonBuilder.createEPerson(context)
                .withEmail("adminCollection1@mail.com")
                .withPassword("qwerty03")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                   .withName("Collection 1")
                   .withAdminGroup(adminCollection1)
                   .build();

        EPerson adminCollection2 = EPersonBuilder.createEPerson(context)
                .withEmail("adminCol2@mail.com")
                .withPassword("qwerty01")
                .build();
        Collection col2 = CollectionBuilder.createCollection(context, child1)
                   .withName("Collection 2")
                   .withAdminGroup(adminCollection2)
                   .build();


        String bitstreamContent = "Embargoed!";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

            Item item = ItemBuilder.createItem(context, col1)
                    .withTitle("Test")
                    .withIssueDate("2018-10-18")
                    .withAuthor("Smith, Donald")
                    .withSubject("ExtraEntry")
                    .build();

            bitstream = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream")
                    .withDescription("Description")
                    .withMimeType("text/plain")
                    .withEmbargoPeriod(Period.ofWeeks(2))
                    .build();
        }
        context.restoreAuthSystemState();

        // parent community's admin user is allowed access to embargoed item
        String tokenAdminParentCommunity = getAuthToken(adminParentCommunity.getEmail(), "qwerty02");
        getClient(tokenAdminParentCommunity).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                   .andExpect(status().isOk());

        // collection1's admin user is allowed access to embargoed item
        String tokenAdminCollection1 = getAuthToken(adminCollection1.getEmail(), "qwerty03");
        getClient(tokenAdminCollection1).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                   .andExpect(status().isOk());

        checkNumberOfStatsRecords(bitstream, 2);

        // admin of second collection is NOT allowed access to embargoed item  of first collection
        String tokenAdminCollection2 = getAuthToken(adminCollection2.getEmail(), "qwerty01");
        getClient(tokenAdminCollection2).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                   .andExpect(status().isForbidden());

        // admin of child2 community is NOT allowed access to embargoed item  of first collection
        String tokenAdminChild2 = getAuthToken(adminChild2.getEmail(), "qwerty05");
        getClient(tokenAdminCollection2).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                   .andExpect(status().isForbidden());

        checkNumberOfStatsRecords(bitstream, 2);
    }

    @Test
    public void testPrivateBitstream() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. A public item with a private bitstream
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .build();

        Group internalGroup = GroupBuilder.createGroup(context)
                                          .withName("Internal Group")
                                          .build();

        String bitstreamContent = "Private!";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

             bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withReaderGroup(internalGroup)
                .build();
        }
            context.restoreAuthSystemState();
            //** WHEN **
            //We download the bitstream
            getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))

                       //** THEN **
                       .andExpect(status().isUnauthorized());

            //An unauthorized request should not log statistics
            checkNumberOfStatsRecords(bitstream, 0);


    }

    @Test
    public void testBitstreamDefaultReadInheritanceFromCollection() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Group internalGroup = GroupBuilder.createGroup(context)
                .withName("Internal Group")
                .build();
        // Explicitly create a restrictive default bitstream read policy on the collection
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        authorizeService.removePoliciesActionFilter(context, col1, DEFAULT_BITSTREAM_READ);
        authorizeService.addPolicy(context, col1, DEFAULT_BITSTREAM_READ, internalGroup);

        //2. A public item with a new bitstream that is not explicitly restricted
        // but should instead inherit
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .build();
        // make sure this item has no default policies for a new bundle to inherit
        authorizeService.removePoliciesActionFilter(context, publicItem1, DEFAULT_BITSTREAM_READ);

        String bitstreamContent = "Private!";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Test Restricted Bitstream")
                    .withDescription("This bitstream is restricted")
                    .withMimeType("text/plain")
                    .build();
        }
        context.restoreAuthSystemState();
        //** WHEN **
        //We download the bitstream
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                //** THEN **
                .andExpect(status().isUnauthorized());

        //An unauthorized request should not log statistics
        checkNumberOfStatsRecords(bitstream, 0);
    }

    @Test
    public void restrictedGroupBitstreamForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson2 = EPersonBuilder.createEPerson(context)
                .withEmail("eperson2@mail.com")
                .withPassword("qwerty02")
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .build();

        Group restrictedGroup = GroupBuilder.createGroup(context)
                .withName("Restricted Group")
                .addMember(eperson)
                .build();

        String bitstreamContent = "Private!";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

            Item item = ItemBuilder.createItem(context, col1)
                    .withTitle("item 1")
                    .withIssueDate("2013-01-17")
                    .withAuthor("Doe, John")
                    .build();
            bitstream = BitstreamBuilder
                .createBitstream(context, item, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withReaderGroup(restrictedGroup)
                .build();
        }
            context.restoreAuthSystemState();
            // download the bitstream
            // eperson that belong to restricted group is allowed access to the item
            String authToken = getAuthToken(eperson.getEmail(), password);
            getClient(authToken).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                                .andExpect(status().isOk());

            checkNumberOfStatsRecords(bitstream, 1);

            String tokenEPerson2 = getAuthToken(eperson2.getEmail(), "qwerty02");
            getClient(tokenEPerson2).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                                .andExpect(status().isForbidden());

            // Anonymous users CANNOT access/download Bitstreams that are restricted
            getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                                .andExpect(status().isUnauthorized());

            checkNumberOfStatsRecords(bitstream, 1);
    }

    @Test
    public void restrictedSpecialGroupBitstreamTest() throws Exception {

        String authenticationMethod =
            configurationService.getProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod");

        try {

            configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", PASS_ONLY);

            context.turnOffAuthorisationSystem();

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community")
                                              .build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withName("Collection 1")
                                               .build();

            Group restrictedGroup = GroupBuilder.createGroup(context)
                                                .withName("Restricted Group")
                                                .build();

            String bitstreamContent = "Private!";
            try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

                Item item = ItemBuilder.createItem(context, col1)
                                       .withTitle("item 1")
                                       .withIssueDate("2013-01-17")
                                       .withAuthor("Doe, John")
                                       .build();

                bitstream = BitstreamBuilder
                    .createBitstream(context, item, is)
                    .withName("Test Embargoed Bitstream")
                    .withDescription("This bitstream is embargoed")
                    .withMimeType("text/plain")
                    .withReaderGroup(restrictedGroup)
                    .build();
            }

            context.restoreAuthSystemState();

            String authToken = getAuthToken(eperson.getEmail(), password);
            getClient(authToken).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                                .andExpect(status().isForbidden());

            getClient(authToken).perform(post("/api/authn/logout")).andExpect(status().isNoContent());

            configurationService.setProperty("authentication-password.login.specialgroup", "Restricted Group");

            authToken = getAuthToken(eperson.getEmail(), password);
            getClient(authToken).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                                .andExpect(status().isOk());

            checkNumberOfStatsRecords(bitstream, 1);

        } finally {
            configurationService.setProperty(
                "plugin.sequence.org.dspace.authenticate.AuthenticationMethod",
                authenticationMethod
            );
        }
    }

    @Test
    public void restrictedGroupBitstreamAccessGrantByAdminsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson adminParentCommunity = EPersonBuilder.createEPerson(context)
                .withEmail("adminCommunity@mail.com")
                .withPassword("qwerty00")
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .withAdminGroup(adminParentCommunity)
                .build();

        EPerson adminChild1 = EPersonBuilder.createEPerson(context)
                .withEmail("adminChild1@mail.com")
                .withPassword("qwerty05")
                .build();
        Community child1 = CommunityBuilder.createCommunity(context)
                .withName("Sub Community")
                .withAdminGroup(adminChild1)
                .build();

        EPerson adminCol1 = EPersonBuilder.createEPerson(context)
                .withEmail("admin1@mail.com")
                .withPassword("qwerty01")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .withAdminGroup(adminCol1)
                .build();

        EPerson adminCol2 = EPersonBuilder.createEPerson(context)
                .withEmail("admin2@mail.com")
                .withPassword("qwerty02")
                .build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 2")
                .withAdminGroup(adminCol2)
                .build();

        Group restrictedGroup = GroupBuilder.createGroup(context)
                .withName("Restricted Group")
                .addMember(eperson)
                .build();

        String bitstreamContent = "Private!";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

            Item item = ItemBuilder.createItem(context, col1)
                    .withTitle("item")
                    .withIssueDate("2018-10-17")
                    .withAuthor("Doe, John")
                    .build();
            bitstream = BitstreamBuilder
                .createBitstream(context, item, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withReaderGroup(restrictedGroup)
                .build();
        }
            context.restoreAuthSystemState();
            // download the bitstream
            // parent community's admin user is allowed access to the item belong restricted group
            String tokenAdminParentCommuity = getAuthToken(adminParentCommunity.getEmail(), "qwerty00");
            getClient(tokenAdminParentCommuity).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                                .andExpect(status().isOk());

            // collection1's admin user is allowed access to the item belong restricted group
            String tokenAdminCol1 = getAuthToken(adminCol1.getEmail(), "qwerty01");
            getClient(tokenAdminCol1).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                                .andExpect(status().isOk());

            checkNumberOfStatsRecords(bitstream, 2);

            // collection2's admin user is NOT allowed access to the item belong collection1
            String tokenAdminCol2 = getAuthToken(adminCol2.getEmail(), "qwerty02");
            getClient(tokenAdminCol2).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                                .andExpect(status().isForbidden());

            // child1's admin user is NOT allowed access to the item belong collection1
            String tokenAdminChild1 = getAuthToken(adminChild1.getEmail(), "qwerty05");
            getClient(tokenAdminChild1).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                                .andExpect(status().isForbidden());

            checkNumberOfStatsRecords(bitstream, 2);
    }

    // Verify number of hits/views of Bitstream is as expected
    private void checkNumberOfStatsRecords(Bitstream bitstream, int expectedNumberOfStatsRecords)
        throws SolrServerException, IOException {
        // Use the SolrLoggerServiceImpl.ResultProcessor inner class to force a Solr commit to occur.
        // This is required because statistics hits will not be committed to Solr until autoCommit next runs.
        SolrLoggerServiceImpl.ResultProcessor rs = ((SolrLoggerServiceImpl) solrLoggerService).new ResultProcessor();
        rs.commit();

        // Find all hits/views of bitstream
        ObjectCount objectCount = solrLoggerService.queryTotal("type:" + Constants.BITSTREAM +
                                                               " AND id:" + bitstream.getID(), null, 1);
        assertEquals(expectedNumberOfStatsRecords, objectCount.getCount());
    }

    @Test
    public void retrieveCitationCoverpageOfBitstream() throws Exception {
        configurationService.setProperty("citation-page.enable_globally", true);
        citationDocumentService.afterPropertiesSet();
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. A public item with a bitstream
        File originalPdf = new File(testProps.getProperty("test.bitstream"));

        try (InputStream is = new FileInputStream(originalPdf)) {

            Item publicItem1 = ItemBuilder.createItem(context, col1)
                    .withTitle("Public item citation cover page test 1")
                    .withIssueDate("2017-10-17")
                    .withAuthor("Smith, Donald").withAuthor("Doe, John")
                    .build();

            bitstream = BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Test bitstream")
                    .withDescription("This is a bitstream to test the citation cover page.")
                    .withMimeType("application/pdf")
                    .build();
        }
            context.restoreAuthSystemState();
            //** WHEN **
            //We download the bitstream
            byte[] content = getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))

                    //** THEN **
                    .andExpect(status().isOk())

                    // exact content-length and etag values are verified in s separate test
                    .andExpect(header().string("Content-Length", not(nullValue())))
                    .andExpect(header().string("ETag", not(nullValue())))
                    //The server should indicate we support Range requests
                    .andExpect(header().string("Accept-Ranges", "bytes"))
                    //We expect the content type to match the bitstream mime type
                    .andExpect(content().contentType("application/pdf;charset=UTF-8"))
                    //THe bytes of the content must match the original content
                    .andReturn().getResponse().getContentAsByteArray();

            // The citation cover page contains the item title.
            // We will now verify that the pdf text contains this title.
            String pdfText = extractPDFText(content);
            assertTrue(StringUtils.contains(pdfText,"Public item citation cover page test 1"));

            // The dspace-api/src/test/data/dspaceFolder/assetstore/ConstitutionofIreland.pdf file contains 64 pages,
            // manually counted + 1 citation cover page
            assertEquals(65,getNumberOfPdfPages(content));

            var etagHeader = getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                .andReturn().getResponse().getHeader("ETag");

            //A If-None-Match HEAD request on the ETag must tell is the bitstream is not modified
            getClient().perform(head("/api/core/bitstreams/" + bitstream.getID() + "/content")
                    .header("If-None-Match", etagHeader))
                    .andExpect(status().isNotModified());

            //The download and head request should also be logged as a statistics record
            checkNumberOfStatsRecords(bitstream, 3);
    }

    private String extractPDFText(byte[] content) throws IOException {
        PDFTextStripper pts = new PDFTextStripper();
        pts.setSortByPosition(true);

        try (ByteArrayInputStream source = new ByteArrayInputStream(content);
             Writer writer = new StringWriter();
             PDDocument pdfDoc = PDDocument.load(source)) {

            pts.writeText(pdfDoc, writer);
            return writer.toString();
        }
    }

    private int getNumberOfPdfPages(byte[] content) throws IOException {
        try (ByteArrayInputStream source = new ByteArrayInputStream(content);
             PDDocument pdfDoc = PDDocument.load(source)) {
            return pdfDoc.getNumberOfPages();
        }
    }

    @Test
    public void getBitstreamFormatUnauthorized() throws Exception {

        resourcePolicyService.removePolicies(context, bitstream, READ);
        resourcePolicyService.removePolicies(context, bitstream.getBundles().get(0), READ);

        getClient()
                .perform(get("/api/core/bitstreams/" + bitstream.getID() + "/format"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getBitstreamFormatForbidden() throws Exception {

        resourcePolicyService.removePolicies(context, bitstream, READ);
        resourcePolicyService.removePolicies(context, bitstream.getBundles().get(0), READ);

        getClient(getAuthToken(eperson.getEmail(), password))
                .perform(get("/api/core/bitstreams/" + bitstream.getID() + "/format"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getBitstreamFormat() throws Exception {

        getClient()
                .perform(get("/api/core/bitstreams/" + bitstream.getID() + "/format"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", matchBitstreamFormat(
                        supportedFormat.getID(),
                        supportedFormat.getMIMEType(),
                        supportedFormat.getDescription(),
                        supportedFormat.getShortDescription(),
                        "SUPPORTED"
                )));
    }

    @Test
    public void updateBitstreamFormatBadRequest() throws Exception {

        getClient(getAuthToken(admin.getEmail(), password)).perform(
                put("/api/core/bitstreams/" + bitstream.getID() + "/format")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(
                                REST_SERVER_URL + "/api/core/bitstreamformat/-1"
                        )
        ).andExpect(status().isBadRequest());

        getClient(getAuthToken(admin.getEmail(), password)).perform(
                put("/api/core/bitstreams/" + bitstream.getID() + "/format")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(
                                REST_SERVER_URL + "/api/core/bitstreamformat/" + knownFormat.getID() + "\n"
                                        + REST_SERVER_URL + "/api/core/bitstreamformat/"
                                        + supportedFormat.getID()
                        )
        ).andExpect(status().isBadRequest());
    }

    @Test
    public void updateBitstreamFormatNotFound() throws Exception {

        getClient(getAuthToken(admin.getEmail(), password)).perform(
                put("/api/core/bitstreams/" + randomUUID() + "/format")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(
                                REST_SERVER_URL + "/api/core/bitstreamformat/" + unknownFormat.getID()
                        )
        ).andExpect(status().isNotFound());
    }

    @Test
    public void updateBitstreamFormatUnauthorized() throws Exception {

        getClient().perform(
                put("/api/core/bitstreams/" + bitstream.getID() + "/format")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(
                                REST_SERVER_URL + "/api/core/bitstreamformat/" + knownFormat.getID()
                        )
        ).andExpect(status().isUnauthorized());
    }

    @Test
    public void updateBitstreamFormatForbidden() throws Exception {

        getClient(getAuthToken(eperson.getEmail(), password)).perform(
                put("/api/core/bitstreams/" + bitstream.getID() + "/format")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(
                                REST_SERVER_URL + "/api/core/bitstreamformat/" + knownFormat.getID()
                        )
        ).andExpect(status().isForbidden());
    }

    @Test
    public void updateBitstreamFormatEPerson() throws Exception {

        context.turnOffAuthorisationSystem();

        createResourcePolicy(context, eperson, null)
                .withAction(WRITE)
                .withDspaceObject(bitstream)
                .build();

        context.restoreAuthSystemState();

        getClient(getAuthToken(eperson.getEmail(), password)).perform(
                put("/api/core/bitstreams/" + bitstream.getID() + "/format")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(
                                REST_SERVER_URL + "/api/core/bitstreamformat/" + knownFormat.getID()
                        )
        ).andExpect(status().isOk());

        bitstream = context.reloadEntity(bitstream);

        assertThat(knownFormat, equalTo(bitstream.getFormat(context)));
        assertTrue(isEmpty(
                bitstreamService.getMetadataByMetadataString(bitstream, "dc.format")
        ));
    }

    @Test
    public void updateBitstreamFormatAdmin() throws Exception {

        context.turnOffAuthorisationSystem();

        context.restoreAuthSystemState();

        getClient(getAuthToken(admin.getEmail(), password)).perform(
                put("/api/core/bitstreams/" + bitstream.getID() + "/format")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(
                                REST_SERVER_URL + "/api/core/bitstreamformat/" + unknownFormat.getID()
                        )
        ).andExpect(status().isOk());

        bitstream = context.reloadEntity(bitstream);

        assertThat(unknownFormat, equalTo(bitstream.getFormat(context)));
        assertTrue(isEmpty(
                bitstreamService.getMetadataByMetadataString(bitstream, "dc.format")
        ));
    }


    @Test
    public void closeInputStreamsRegularDownload() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. A public item with a bitstream
        String bitstreamContent = "0123456789";

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {

            Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test bitstream")
                .withDescription("This is a bitstream to test range requests")
                .withMimeType("text/plain")
                .build();
        }
        context.restoreAuthSystemState();

        var bitstreamStorageService = StorageServiceFactory.getInstance().getBitstreamStorageService();
        var inputStream = bitstreamStorageService.retrieve(context, bitstream);
        var inputStreamSpy = spy(inputStream);
        var bitstreamStorageServiceSpy = spy(bitstreamStorageService);
        ReflectionTestUtils.setField(bitstreamService, "bitstreamStorageService", bitstreamStorageServiceSpy);
        doReturn(inputStreamSpy).when(bitstreamStorageServiceSpy).retrieve(any(), eq(bitstream));

        //** WHEN **
        //We download the bitstream
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
            //** THEN **
            .andExpect(status().isOk());

        Mockito.verify(bitstreamStorageServiceSpy, times(1)).retrieve(any(), eq(bitstream));
        Mockito.verify(inputStreamSpy, times(1)).close();
    }

    @Test
    public void closeInputStreamsDownloadWithCoverPage() throws Exception {
        configurationService.setProperty("citation-page.enable_globally", true);
        citationDocumentService.afterPropertiesSet();
        context.turnOffAuthorisationSystem();


        //** GIVEN **
        //1. A community-collection structure with one parent community and one collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection col1 =
            CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. A public item with a bitstream
        File originalPdf = new File(testProps.getProperty("test.bitstream"));

        try (InputStream is = new FileInputStream(originalPdf)) {

            Item publicItem1 = ItemBuilder.createItem(context, col1)
                    .withTitle("Public item citation cover page test 1")
                    .withIssueDate("2017-10-17")
                    .withAuthor("Smith, Donald").withAuthor("Doe, John")
                    .build();

            bitstream = BitstreamBuilder
                    .createBitstream(context, publicItem1, is)
                    .withName("Test bitstream")
                    .withDescription("This is a bitstream to test the citation cover page.")
                    .withMimeType("application/pdf")
                    .build();
        }
        context.restoreAuthSystemState();

        var bitstreamStorageService = StorageServiceFactory.getInstance().getBitstreamStorageService();
        var inputStreamSpy = spy(bitstreamStorageService.retrieve(context, bitstream));
        var bitstreamStorageServiceSpy = spy(bitstreamStorageService);
        ReflectionTestUtils.setField(bitstreamService, "bitstreamStorageService", bitstreamStorageServiceSpy);
        doReturn(inputStreamSpy).when(bitstreamStorageServiceSpy).retrieve(any(), eq(bitstream));

        //** WHEN **
        //We download the bitstream
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
            //** THEN **
            .andExpect(status().isOk());

        Mockito.verify(bitstreamStorageServiceSpy, times(1)).retrieve(any(), eq(bitstream));
        Mockito.verify(inputStreamSpy, times(1)).close();
    }

    @Test
    public void testEmbargoedBitstreamWithCrisSecurity() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        // this should be a publication collection but right now no control are enforced
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1").build();
        // this should be a person collection
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Person").build();

        //2. A public item with an embargoed bitstream
        String bitstreamContent = "Embargoed!";
        EPerson authorEp = EPersonBuilder.createEPerson(context)
                .withEmail("author@example.com")
                .withPassword(password)
                .build();
        Item profile = ItemBuilder.createItem(context, col2)
                .withTitle("Author")
                .withDspaceObjectOwner(authorEp)
                .build();
        // set our submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();
        context.setCurrentUser(submitter);
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            // we need a publication to check our cris enhanced security
            Item publicItem1 =
                    ItemBuilder.createItem(context, col1)
                            .withEntityType("Publication")
                            .withTitle("Public item 1")
                            .withIssueDate("2017-10-17")
                            .withAuthor("Just an author without profile")
                            .withAuthor("A profile not longer in the system",
                                    UUID.randomUUID().toString())
                            .withAuthor("An author with invalid authority",
                                    "this is not an uuid")
                            .withAuthor("Author",
                                    profile.getID().toString())
                            .build();

            bitstream = BitstreamBuilder
                .createBitstream(context, publicItem1, is)
                .withName("Test Embargoed Bitstream")
                .withDescription("This bitstream is embargoed")
                .withMimeType("text/plain")
                .withEmbargoPeriod(Period.ofMonths(6))
                .build();
        }
        context.restoreAuthSystemState();

        //** WHEN **
        //anonymous try to download the bitstream
        getClient()
                .perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                // ** THEN **
                .andExpect(status().isUnauthorized());

        // another unrelated eperson should get forbidden
        getClient(getAuthToken(eperson.getEmail(), password))
                .perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                // ** THEN **
                .andExpect(status().isForbidden());

        // the submitter should be able to download according to our custom cris policy
        getClient(getAuthToken(submitter.getEmail(), password))
                .perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                // ** THEN **
                .andExpect(status().isOk());

        // the author should be able to download according to our custom cris policy
        getClient(getAuthToken(authorEp.getEmail(), password))
                .perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                // ** THEN **
                .andExpect(status().isOk());

        // unauthorized request should not log statistics so we have only 2 successful visits
        checkNumberOfStatsRecords(bitstream, 2);
    }

    @Test
    public void checkContentDispositionOfFormats() throws Exception {
        configurationService.setProperty("webui.content_disposition_format", new String[] {
            "text/richtext",
            "text/xml",
            "txt"
        });

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).build();
        String content = "Test Content";
        Bitstream rtf;
        Bitstream xml;
        Bitstream txt;
        Bitstream csv;
        try (InputStream is = IOUtils.toInputStream(content, CharEncoding.UTF_8)) {
            rtf = BitstreamBuilder.createBitstream(context, item, is)
                                  .withMimeType("text/richtext").build();
            xml = BitstreamBuilder.createBitstream(context, item, is)
                                  .withMimeType("text/xml").build();
            txt = BitstreamBuilder.createBitstream(context, item, is)
                                  .withMimeType("text/plain").build();
            csv = BitstreamBuilder.createBitstream(context, item, is)
                                   .withMimeType("text/csv").build();
        }
        context.restoreAuthSystemState();

        // these formats are configured and files should be downloaded
        verifyBitstreamDownload(rtf, "text/richtext;charset=UTF-8", true);
        verifyBitstreamDownload(xml, "text/xml;charset=UTF-8", true);
        verifyBitstreamDownload(txt, "text/plain;charset=UTF-8", true);
        // this format is not configured and should open inline
        verifyBitstreamDownload(csv, "text/csv;charset=UTF-8", false);
    }

    @Test
    public void checkHardcodedContentDispositionFormats() throws Exception {
        // This test is similar to the above test, but it verifies that our *hardcoded settings* for
        // webui.content_disposition_format are protecting us from loading specific formats *inline*.
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).build();
        String content = "Test Content";
        Bitstream html;
        Bitstream js;
        Bitstream rdf;
        Bitstream xml;
        Bitstream unknown;
        Bitstream svg;
        try (InputStream is = IOUtils.toInputStream(content, CharEncoding.UTF_8)) {
            html = BitstreamBuilder.createBitstream(context, item, is)
                                   .withMimeType("text/html").build();
            js = BitstreamBuilder.createBitstream(context, item, is)
                                 .withMimeType("text/javascript").build();
            // NOTE: RDF has a MIME Type with a "charset" in our bitstream-formats.xml
            rdf = BitstreamBuilder.createBitstream(context, item, is)
                                  .withMimeType("application/rdf+xml; charset=utf-8").build();
            xml = BitstreamBuilder.createBitstream(context, item, is)
                                  .withMimeType("text/xml").build();
            unknown = BitstreamBuilder.createBitstream(context, item, is)
                                      .withMimeType("application/octet-stream").build();
            svg = BitstreamBuilder.createBitstream(context, item, is)
                                  .withMimeType("image/svg+xml").build();

        }
        context.restoreAuthSystemState();

        // By default, HTML, JS & XML should all download. This protects us from possible XSS attacks, as
        // each of these formats can embed JavaScript which may execute when the file is loaded *inline*.
        verifyBitstreamDownload(html, "text/html;charset=UTF-8", true);
        verifyBitstreamDownload(js, "text/javascript;charset=UTF-8", true);
        verifyBitstreamDownload(rdf, "application/rdf+xml;charset=UTF-8", true);
        verifyBitstreamDownload(xml, "text/xml;charset=UTF-8", true);
        // Unknown file formats should also always download
        verifyBitstreamDownload(unknown, "application/octet-stream;charset=UTF-8", true);
        // SVG does NOT currently exist as a recognized format in DSpace's bitstream-formats.xml, but it's another
        // format that allows embedded JavaScript. This test is a reminder that SVGs should NOT be opened inline.
        verifyBitstreamDownload(svg, "application/octet-stream;charset=UTF-8", true);
    }

    @Test
    public void checkWildcardContentDispositionFormats() throws Exception {
        // Setting "*" should result in all formats being downloaded (nothing will be opened inline)
        configurationService.setProperty("webui.content_disposition_format", "*");

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).build();
        String content = "Test Content";
        Bitstream csv;
        Bitstream jpg;
        Bitstream mpg;
        Bitstream pdf;
        try (InputStream is = IOUtils.toInputStream(content, CharEncoding.UTF_8)) {
            csv = BitstreamBuilder.createBitstream(context, item, is)
                                  .withMimeType("text/csv").build();
            jpg = BitstreamBuilder.createBitstream(context, item, is)
                                   .withMimeType("image/jpeg").build();
            mpg = BitstreamBuilder.createBitstream(context, item, is)
                                  .withMimeType("video/mpeg").build();
            pdf = BitstreamBuilder.createBitstream(context, item, is)
                                  .withMimeType("application/pdf").build();
        }
        context.restoreAuthSystemState();

        // All formats should be download only
        verifyBitstreamDownload(csv, "text/csv;charset=UTF-8", true);
        verifyBitstreamDownload(jpg, "image/jpeg;charset=UTF-8", true);
        verifyBitstreamDownload(mpg, "video/mpeg;charset=UTF-8", true);
        verifyBitstreamDownload(pdf, "application/pdf;charset=UTF-8", true);
    }


    private void verifyBitstreamDownload(Bitstream file, String contentType, boolean shouldDownload) throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        String header = getClient(token).perform(get("/api/core/bitstreams/" + file.getID() + "/content")
                                                     .header("Accept", contentType))
                                         .andExpect(status().isOk())
                                         .andExpect(content().contentType(contentType))
                                         .andReturn().getResponse().getHeader("content-disposition");
        if (shouldDownload) {
            assertTrue("Content-Disposition should contain 'attachment' for " + contentType,
                       header.contains("attachment"));
            assertFalse("Content-Disposition should NOT contain 'inline' for " + contentType,
                        header.contains("inline"));
        } else {
            assertTrue("Content-Disposition should contain 'inline' for " + contentType,
                       header.contains("inline"));
            assertFalse("Content-Disposition should NOT contain 'attachment' for " + contentType,
                        header.contains("attachment"));
        }
    }

    @Test
    public void contentLengthAndEtagUsesOriginalBitstream() throws Exception {
        givenPdf(false, originalPdf -> {
            var originalMd5 = md5Checksum(originalPdf);
            long originalLength = Files.size(originalPdf.toPath());

            assertThat(originalLength, greaterThan(0L));

            getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(header().longValue("Content-Length", originalLength))
                    .andExpect(header().string("ETag", "\"" + originalMd5 + "\""));
        });
    }

    private static String md5Checksum(File file) throws IOException {
        final String md5;
        try (InputStream is = new FileInputStream(file)) {
            md5 = DigestUtils.md5Hex(is);
        }
        return md5;
    }

    @Test
    public void withCoverPageContentLengthAndEtagChanges() throws Exception {
        givenPdf(true, originalPdf -> {
            var originalMd5 = md5Checksum(originalPdf);
            long originalLength = Files.size(originalPdf.toPath());

            getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Length", not(Long.toString(originalLength))))
                    .andExpect(header().string("ETag", not("\"" + originalMd5 + "\"")));
        });
    }

    @Test
    public void etagAndContentLengthIsStable() {
        givenPdf(false, ignored -> {
            var etag1 = getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                    .andReturn().getResponse().getHeader("Etag");

            var etag2 = getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                    .andReturn().getResponse().getHeader("Etag");

            assertThat(etag1, equalTo(etag2));
        });
    }

    @Test
    public void withCoverPageEtagAndContentLengthIsStable() {
        givenPdf(true, ignored -> {
            var etag1 = getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                    .andReturn().getResponse().getHeader("Etag");

            var etag2 = getClient().perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                    .andReturn().getResponse().getHeader("Etag");

            assertThat(etag1, equalTo(etag2));
        });
    }

    @FunctionalInterface
    interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    private void givenPdf(boolean coverPageEnabled, ThrowingConsumer<File> block) {
        configurationService.setProperty("citation-page.enable_globally", coverPageEnabled);

        try {
            citationDocumentService.afterPropertiesSet();
            context.turnOffAuthorisationSystem();

            //** GIVEN **
            //1. A community-collection structure with one parent community and one collections.
            parentCommunity = CommunityBuilder.createCommunity(context)
                    .withName("Parent Community")
                    .build();

            Collection col1 =
                    CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

            //2. A public item with a bitstream
            File originalPdf = new File(testProps.getProperty("test.bitstream"));

            try (InputStream is = new FileInputStream(originalPdf)) {
                Item publicItem1 = ItemBuilder.createItem(context, col1)
                        .withTitle("Public item citation cover page test 1")
                        .withIssueDate("2017-10-17")
                        .withAuthor("Smith, Donald").withAuthor("Doe, John")
                        .build();

                bitstream = BitstreamBuilder
                        .createBitstream(context, publicItem1, is)
                        .withName("Test bitstream")
                        .withDescription("This is a bitstream to test the citation cover page.")
                        .withMimeType("application/pdf")
                        .build();
            }
            context.restoreAuthSystemState();

            block.accept(originalPdf);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
