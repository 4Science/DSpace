/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.DOIBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class DOIOrganiserIT extends AbstractIntegrationTestWithDatabase {

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    private final DOIService doiService = IdentifierServiceFactory.getInstance().getDOIService();
    private Collection collection;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Set DOI prefix for testing
        configurationService.setProperty("identifier.doi.prefix", "10.12345");

        // Build test content
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Test Community")
                                              .build();
        collection = CollectionBuilder.createCollection(context, community)
                                      .withName("Test Collection")
                                      .withEntityType("Publication")
                                      .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void testDOIRegistrationCreatesEntryInDOITable() throws Exception {
        configurationService.setProperty("doi-organiser-limit", -1);

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Title")
                               .withIssueDate("2024-01-01")
                               .build();
        context.restoreAuthSystemState();

        assertThat(runDSpaceScript("doi-organiser", "-r"), is(0));

        String doiString = itemService.getMetadataFirstValue(item, "dc", "identifier", "doi", Item.ANY);
        assertThat("DOI should be created and assigned to item", doiString, not(isEmptyOrNullString()));

        doiService.findDOIByDSpaceObject(context, item);

        assertThat(doiService.findDOIByDSpaceObject(context, item).getStatus(),
                   is(DOIIdentifierProvider.IS_REGISTERED));
    }


    @Test
    public void testDOIReservationCreatesEntryInDOITable() throws Exception {

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Title")
                               .withIssueDate("2024-01-01")
                               .withType("JournalArticle")
                               .build();
        context.restoreAuthSystemState();

        // Run the DOI organiser script in "lookup" mode to trigger DOI creation
        assertThat(runDSpaceScript("doi-organiser", "--reserve-doi", item.getHandle()), is(0));

        assertThat(doiService.findDOIByDSpaceObject(context, item).getStatus(),
                   is(DOIIdentifierProvider.IS_RESERVED));
    }


    @Test
    public void testListPrintsRegisteredDOIEntryCorrectly() throws Exception {

        configurationService.setProperty("doi-organiser-limit", -1);

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Title")
                               .withIssueDate("2024-01-01")
                               .build();
        Item item2 = ItemBuilder.createItem(context, collection)
                                .withTitle("Title2")
                                .withIssueDate("2024-01-01")
                                .build();
        Item item3 = ItemBuilder.createItem(context, collection)
                                .withTitle("Title3")
                                .withIssueDate("2024-01-01")
                                .build();
        Item item4 = ItemBuilder.createItem(context, collection)
                                .withTitle("Title4")
                                .withIssueDate("2024-01-01")
                                .build();
        context.restoreAuthSystemState();

        DOIBuilder.createDOI(context, item)
                  .withStatus(DOIIdentifierProvider.TO_BE_RESERVED)
                  .withDoiString("test")
                  .build();

        DOIBuilder.createDOI(context, item2)
                  .withStatus(DOIIdentifierProvider.TO_BE_REGISTERED)
                  .withDoiString("test2")
                  .build();

        DOIBuilder.createDOI(context, item3)
                  .withStatus(DOIIdentifierProvider.UPDATE_BEFORE_REGISTRATION)
                  .withDoiString("test3")
                  .build();

        DOIBuilder.createDOI(context, item4)
                  .withStatus(DOIIdentifierProvider.TO_BE_DELETED)
                  .withDoiString("test4")
                  .build();

        // Capture original System.out
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(outContent, true, StandardCharsets.UTF_8)) {
            System.setOut(out);
            // Run the DOI organiser script in "lookup" mode
            int exitCode = runDSpaceScript("doi-organiser", "-l");
            assertThat(exitCode, is(0));

            String output = outContent.toString(StandardCharsets.UTF_8);

            assertThat(output, containsString("DOIs queued for reservation:"));

            assertThat(output, containsString("doi:test (belongs to item with handle " + item.getHandle()));

            assertThat(output, containsString("DOIs queued for registration:"));
            assertThat(output, containsString("doi:test2 (belongs to item with handle " + item2.getHandle()));

            assertThat(output, containsString("DOIs queued for update:"));
            assertThat(output, containsString("doi:test3 (belongs to item with handle " + item3.getHandle()));

            assertThat(output, containsString("DOIs queued for deletion:"));
            assertThat(output, containsString("doi:test4 (belongs to item with handle " + item4.getHandle()));

        } finally {
            System.setOut(originalOut);
        }

    }


    @Test
    public void testRegisterDOIContinuesWhenOneItemFails() throws Exception {
        configurationService.setProperty("doi-organiser-limit", -1);

        context.turnOffAuthorisationSystem();

        // Item 1 - valid
        Item item1 = ItemBuilder.createItem(context, collection)
                                .withTitle("Valid Item")
                                .withIssueDate("2024-01-01")
                                .build();

        // DOI for valid item
        DOIBuilder.createDOI(context, item1)
                  .withStatus(DOIIdentifierProvider.TO_BE_REGISTERED)
                  .withDoiString("valid-doi")
                  .build();

        // DOI for invalid (null) item
        DOIBuilder.createDOI(context, null)
                  .withStatus(DOIIdentifierProvider.TO_BE_REGISTERED)
                  .withDoiString("invalid-doi")
                  .build();

        context.restoreAuthSystemState();

        // Capture System.out and System.err
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(outContent, true, StandardCharsets.UTF_8);
             PrintStream err = new PrintStream(errContent, true, StandardCharsets.UTF_8)
        ) {
            System.setOut(out);
            System.setErr(err);

            assertThat(runDSpaceScript("doi-organiser", "-r"), is(0));

            String output = outContent.toString(StandardCharsets.UTF_8);
            String error = errContent.toString(StandardCharsets.UTF_8);

            assertThat(output, containsString("This identifier: doi:valid-doi is successfully registered."));
            assertThat(doiService.findDOIByDSpaceObject(context, item1).getStatus(),
                       is(DOIIdentifierProvider.IS_REGISTERED));

            assertTrue("Expected error output for the invalid DOI", error.contains(
                "Error registering DOI identifier"));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    @Test
    public void testReservationDOIContinuesWhenOneItemFails() throws Exception {
        configurationService.setProperty("doi-organiser-limit", -1);

        context.turnOffAuthorisationSystem();

        // Item 1 - valid
        Item item1 = ItemBuilder.createItem(context, collection)
                                .withTitle("Valid Item")
                                .withIssueDate("2024-01-01")
                                .withType("JournalArticle")
                                .build();

        // DOI for valid item
        DOIBuilder.createDOI(context, item1)
                  .withStatus(DOIIdentifierProvider.TO_BE_RESERVED)
                  .withDoiString("valid-doi")
                  .build();

        // Item 1 - valid
        Item item2 = ItemBuilder.createItem(context, collection)
                                .withTitle("Valid Item")
                                .withIssueDate("2024-01-01")
                                .withType("JournalArticle")
                                .build();

        // DOI for invalid item
        DOIBuilder.createDOI(context, item2)
                  .withStatus(DOIIdentifierProvider.TO_BE_RESERVED)
                  .withDoiString("invalid-doi")
                  .build();

        context.restoreAuthSystemState();

        // Capture System.out and System.err
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(outContent, true, StandardCharsets.UTF_8);
             PrintStream err = new PrintStream(errContent, true, StandardCharsets.UTF_8)
        ) {
            System.setOut(out);
            System.setErr(err);

            assertThat(runDSpaceScript("doi-organiser", "-s"), is(0));

            String output = outContent.toString(StandardCharsets.UTF_8);
            String error = errContent.toString(StandardCharsets.UTF_8);

            assertThat(output, containsString("This identifier : doi:valid-doi is successfully reserved."));
            assertThat(doiService.findDOIByDSpaceObject(context, item1).getStatus(),
                       is(DOIIdentifierProvider.IS_RESERVED));

            assertTrue("Expected error output for the invalid DOI", error.contains(
                "It wasn't possible to reserve this identifier: doi:invalid-doi"));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }


    @Test
    public void testUpdateDOIContinuesWhenOneItemFails() throws Exception {
        configurationService.setProperty("doi-organiser-limit", -1);
        context.turnOffAuthorisationSystem();

        // Item 1 - valid
        Item item1 = ItemBuilder.createItem(context, collection)
                                .withTitle("Valid Item")
                                .withIssueDate("2024-01-01")
                                .withType("JournalArticle")
                                .build();

        // DOI for valid item
        DOIBuilder.createDOI(context, item1)
                  .withStatus(DOIIdentifierProvider.UPDATE_REGISTERED)
                  .withDoiString("valid-doi")
                  .build();

        // Item 2 - invalid
        Item item2 = ItemBuilder.createItem(context, collection)
                                .withTitle("Invalid Item 2")
                                .withIssueDate("2024-01-01")
                                .withType("JournalArticle")
                                .build();
        // DOI for invalid (null) item
        DOIBuilder.createDOI(context, item2)
                  .withStatus(DOIIdentifierProvider.UPDATE_RESERVED)
                  .withDoiString("invalid-doi1")
                  .build();

        // Item 3 - valid
        Item item3 = ItemBuilder.createItem(context, collection)
                                .withTitle("Invalid Item 3")
                                .withIssueDate("2024-01-01")
                                .withType("JournalArticle")
                                .build();
        // DOI for invalid (null) item
        DOIBuilder.createDOI(context, item3)
                  .withStatus(DOIIdentifierProvider.UPDATE_BEFORE_REGISTRATION)
                  .withDoiString("invalid-doi2")
                  .build();

        context.restoreAuthSystemState();

        // Capture System.out and System.err
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(outContent, true, StandardCharsets.UTF_8);
             PrintStream err = new PrintStream(errContent, true, StandardCharsets.UTF_8)
        ) {
            System.setOut(out);
            System.setErr(err);
            assertThat(runDSpaceScript("doi-organiser", "-u"), is(0));

            String output = outContent.toString(StandardCharsets.UTF_8);
            String error = errContent.toString(StandardCharsets.UTF_8);

            assertEquals("Successfully updated metadata of DOI doi:valid-doi.\n", output);
            assertThat(doiService.findDOIByDSpaceObject(context, item1).getStatus(),
                       is(DOIIdentifierProvider.IS_REGISTERED));

            assertTrue("Expected error output for the invalid DOI", error.contains(
                "It wasn't possible to update this identifier: doi:invalid-doi1"));
            assertTrue("Expected error output for the invalid DOI", error.contains(
                "It wasn't possible to update this identifier: doi:invalid-doi2"));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }


    @Test
    public void testLimitRegisterDoiWhenLimitParameterIsSet() throws Exception {
        context.turnOffAuthorisationSystem();

        // Item 1 - valid
        Item item1 = ItemBuilder.createItem(context, collection)
                                .withTitle("Item1")
                                .withIssueDate("2024-01-01")
                                .build();
        DOIBuilder.createDOI(context, item1)
                  .withStatus(DOIIdentifierProvider.TO_BE_REGISTERED)
                  .withDoiString("item1")
                  .build();

        // Item 2 - valid
        Item item2 = ItemBuilder.createItem(context, collection)
                                .withTitle("Item2")
                                .withIssueDate("2024-01-01")
                                .build();
        DOIBuilder.createDOI(context, item2)
                  .withStatus(DOIIdentifierProvider.TO_BE_REGISTERED)
                  .withDoiString("item2")
                  .build();

        context.restoreAuthSystemState();

        // Capture System.out and System.err
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(outContent, true, StandardCharsets.UTF_8);
             PrintStream err = new PrintStream(errContent, true, StandardCharsets.UTF_8)
        ) {
            System.setOut(out);
            System.setErr(err);
            assertThat(runDSpaceScript("doi-organiser", "-r", "-li", "1"), is(0));

            String output = outContent.toString(StandardCharsets.UTF_8);
            String error = errContent.toString(StandardCharsets.UTF_8);

            assertEquals("This identifier: doi:item1 is successfully registered.\n", output);
            assertTrue(error.isEmpty());

        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }


    @Test
    public void testLimitRegisterDoiWhenLimitPropertyIsSet() throws Exception {
        configurationService.setProperty("doi-organiser-limit", "1");

        context.turnOffAuthorisationSystem();

        // Item 1 - valid
        Item item1 = ItemBuilder.createItem(context, collection)
                                .withTitle("Item1")
                                .withIssueDate("2024-01-01")
                                .build();
        DOIBuilder.createDOI(context, item1)
                  .withStatus(DOIIdentifierProvider.TO_BE_REGISTERED)
                  .withDoiString("item1")
                  .build();

        // Item 2 - valid
        Item item2 = ItemBuilder.createItem(context, collection)
                                .withTitle("Item2")
                                .withIssueDate("2024-01-01")
                                .build();
        DOIBuilder.createDOI(context, item2)
                  .withStatus(DOIIdentifierProvider.TO_BE_REGISTERED)
                  .withDoiString("item2")
                  .build();

        context.restoreAuthSystemState();

        // Capture System.out and System.err
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(outContent, true, StandardCharsets.UTF_8);
             PrintStream err = new PrintStream(errContent, true, StandardCharsets.UTF_8)
        ) {
            System.setOut(out);
            System.setErr(err);
            assertThat(runDSpaceScript("doi-organiser", "-r"), is(0));

            String output = outContent.toString(StandardCharsets.UTF_8);
            String error = errContent.toString(StandardCharsets.UTF_8);

            assertEquals(
                "This identifier: doi:item1 is successfully registered.\n" +
                 "This identifier: doi:item2 is successfully registered.\n",
                output
            );
            assertTrue(error.isEmpty());

        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }


    @Test
    public void testRegisterAllDoiWhenLimitPropertyIsUnlimited() throws Exception {
        configurationService.setProperty("doi-organiser-limit", "-1");

        context.turnOffAuthorisationSystem();

        // Item 1 - valid
        Item item1 = ItemBuilder.createItem(context, collection)
                                .withTitle("Item1")
                                .withIssueDate("2024-01-01")
                                .build();
        DOIBuilder.createDOI(context, item1)
                  .withStatus(DOIIdentifierProvider.TO_BE_REGISTERED)
                  .withDoiString("item1")
                  .build();

        // Item 2 - valid
        Item item2 = ItemBuilder.createItem(context, collection)
                                .withTitle("Item2")
                                .withIssueDate("2024-01-01")
                                .build();
        DOIBuilder.createDOI(context, item2)
                  .withStatus(DOIIdentifierProvider.TO_BE_REGISTERED)
                  .withDoiString("item2")
                  .build();

        context.restoreAuthSystemState();

        // Capture System.out and System.err
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();


        try (PrintStream out = new PrintStream(outContent, true, StandardCharsets.UTF_8);
             PrintStream err = new PrintStream(errContent, true, StandardCharsets.UTF_8)
        ) {
            System.setOut(out);
            System.setErr(err);

            assertThat(runDSpaceScript("doi-organiser", "-r"), is(0));

            String output = outContent.toString(StandardCharsets.UTF_8);
            String error = errContent.toString(StandardCharsets.UTF_8);

            assertThat(output, containsString("This identifier: doi:item1 is successfully registered."));
            assertThat(output, containsString("This identifier: doi:item2 is successfully registered."));
            assertTrue(error.isEmpty());

        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    @Test
    public void testRegisterSingleDOI() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("identifier.doi.resolver", "https://my-resolver/");

        // Item 1 - valid
        Item item1 = ItemBuilder.createItem(context, collection)
                                .withTitle("Valid Item")
                                .withIssueDate("2024-01-01")
                                .build();

        // DOI for valid item
        DOI doi =
            DOIBuilder.createDOI(context, item1)
                      .withStatus(DOIIdentifierProvider.TO_BE_REGISTERED)
                      .withDoiString("valid-doi")
                      .build();

        context.restoreAuthSystemState();

        // Capture System.out and System.err
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(outContent, true, StandardCharsets.UTF_8);
             PrintStream err = new PrintStream(errContent, true, StandardCharsets.UTF_8)
        ) {
            System.setOut(out);
            System.setErr(err);

            assertThat(runDSpaceScript("doi-organiser", "-register-doi", "doi:valid-doi"), is(0));

            doi = context.reloadEntity(doi);

            assertThat(
                "DOI was not registered!",
                doi.getStatus(),
                is(DOIIdentifierProvider.IS_REGISTERED)
            );

            assertThat(
                "The metadata dc.identifier.doi wasn't updated properly",
                itemService.getMetadata(item1, "dc.identifier.doi"),
                is("https://my-resolver/valid-doi")
            );

            String output = outContent.toString(StandardCharsets.UTF_8);
            String error = errContent.toString(StandardCharsets.UTF_8);

            assertThat(output, containsString("This identifier: doi:valid-doi is successfully registered."));
            assertThat(doiService.findDOIByDSpaceObject(context, item1).getStatus(),
                       is(DOIIdentifierProvider.IS_REGISTERED));

            assertTrue("Expected no-output for the valid DOI",
                       error.isEmpty()
            );
            assertTrue("Expected output for the valid DOI",
                       output.contains("This identifier: doi:valid-doi is successfully registered.")
            );
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    @Test
    public void testUpdateSingleDoiAfterRegistration() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("identifier.doi.resolver", "https://my-resolver/");

        // Item 1 - valid
        Item item1 = ItemBuilder.createItem(context, collection)
                                .withTitle("Valid Item")
                                .withIssueDate("2024-01-01")
                                .build();

        // DOI for valid item
        DOI doi =
            DOIBuilder.createDOI(context, item1)
                      .withStatus(DOIIdentifierProvider.TO_BE_REGISTERED)
                      .withDoiString("valid-doi")
                      .build();

        context.restoreAuthSystemState();

        // Capture System.out and System.err
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(outContent, true, StandardCharsets.UTF_8);
             PrintStream err = new PrintStream(errContent, true, StandardCharsets.UTF_8)
        ) {
            System.setOut(out);
            System.setErr(err);

            assertThat(runDSpaceScript("doi-organiser", "-register-doi", "doi:valid-doi"), is(0));

            doi = context.reloadEntity(doi);

            assertThat(
                "DOI was not registered!",
                doi.getStatus(),
                is(DOIIdentifierProvider.IS_REGISTERED)
            );

            assertThat(
                "The metadata dc.identifier.doi wasn't updated properly",
                itemService.getMetadata(item1, "dc.identifier.doi"),
                is("https://my-resolver/valid-doi")
            );

            String output = outContent.toString(StandardCharsets.UTF_8);
            String error = errContent.toString(StandardCharsets.UTF_8);

            assertThat(output, containsString("This identifier: doi:valid-doi is successfully registered."));
            assertThat(doiService.findDOIByDSpaceObject(context, item1).getStatus(),
                       is(DOIIdentifierProvider.IS_REGISTERED));

            assertTrue("Expected no-output for the valid DOI",
                       error.isEmpty()
            );
            assertTrue("Expected output for the valid DOI",
                       output.contains("This identifier: doi:valid-doi is successfully registered.")
            );

        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(outContent, true, StandardCharsets.UTF_8);
             PrintStream err = new PrintStream(errContent, true, StandardCharsets.UTF_8)
        ) {
            System.setOut(out);
            System.setErr(err);

            // flush output streams before updating the doi
            out.flush();
            err.flush();

            // try to update the doi after the registration
            assertThat(runDSpaceScript("doi-organiser", "-update-doi", "doi:valid-doi"), is(0));

            String output = outContent.toString(StandardCharsets.UTF_8);
            String error = errContent.toString(StandardCharsets.UTF_8);

            assertThat(output, containsString("Successfully updated metadata of DOI doi:valid-doi."));
            assertThat(
                doiService.findDOIByDSpaceObject(context, item1).getStatus(),
                is(DOIIdentifierProvider.IS_REGISTERED)
            );
            assertTrue("Expected no-output for the valid DOI",
                       error.isEmpty()
            );

        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }


}
