/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.eperson.EPerson;
import org.dspace.event.factory.EventServiceFactory;
import org.dspace.event.service.EventService;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class DoiConsumerIT extends AbstractIntegrationTestWithDatabase {

    private static final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    private static final EventService eventService = EventServiceFactory.getInstance().getEventService();
    private static String[] consumers;
    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private final DOIService doiService = IdentifierServiceFactory.getInstance().getDOIService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    private Collection collection;
    private EPerson submitter;


    /**
     * This method will be run before the first test as per @BeforeClass. It will
     * configure the event.dispatcher.default.consumers property to add the DoiConsumer.
     */
    @BeforeClass
    public static void initConsumers() {
        consumers = configurationService.getArrayProperty("event.dispatcher.default.consumers");
        Set<String> consumersSet = new HashSet<String>(Arrays.asList(consumers));
        if (!consumersSet.contains("doi")) {
            consumersSet.add("doi");
            configurationService.setProperty("event.dispatcher.default.consumers", consumersSet.toArray());
            eventService.reloadConfiguration();
        }
    }

    /**
     * Reset the event.dispatcher.default.consumers property value.
     */
    @AfterClass
    public static void resetDefaultConsumers() {
        configurationService.setProperty("event.dispatcher.default.consumers", consumers);
        eventService.reloadConfiguration();
    }

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
        submitter =
            EPersonBuilder.createEPerson(context)
                          .withNameInMetadata("Vins", "4Science")
                          .build();

        collection = CollectionBuilder.createCollection(context, community)
                                      .withName("Test Collection")
                                      .withEntityType("Publication")
                                      .withSubmitterGroup(submitter)
                                      .build();


        configurationService.setProperty("identifiers.submission.register", "true");
        configurationService.setProperty("identifiers.submission.filter.workspace", "always_true_filter");

        context.restoreAuthSystemState();
    }

    @Test
    public void testDoiCreationDuringSubmission() throws SQLException {
        context.turnOffAuthorisationSystem();

        WorkspaceItem wsItem = createWorkspaceItem();

        context.commit();
        context.restoreAuthSystemState();

        DOI doi = doiService.findDOIByDSpaceObject(context, wsItem.getItem());
        assertThat(
            "DOI cannot be created with Consumer!",
            doi, notNullValue()
        );

        assertThat(
            "DOI is not PENDING!",
            doi.getStatus(),
            is(DOIIdentifierProvider.PENDING)
        );

    }


    @Test
    public void testDOIStatusRestoredPendingIfChangedDuringSubmission() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();

        WorkspaceItem wsItem = createWorkspaceItem();

        context.commit();
        context.restoreAuthSystemState();

        // check that the doi has been created by the DOiConsumer.
        DOI doi = doiService.findDOIByDSpaceObject(context, wsItem.getItem());
        assertThat(
            "DOI cannot be created with Consumer!",
            doi, notNullValue()
        );

        // check its status!
        assertThat(
            "DOI is not PENDING!",
            doi.getStatus(),
            is(DOIIdentifierProvider.PENDING)
        );


        Item item = wsItem.getItem();
        context.turnOffAuthorisationSystem();
        // set doi status to be minted
        doi.setStatus(DOIIdentifierProvider.MINTED);
        doiService.update(context, doi);

        context.commit();
        context.restoreAuthSystemState();

        item = context.reloadEntity(item); // refresh
        doi = doiService.findDOIByDSpaceObject(context, item);
        assertThat(
            "DOI is not MINTED!",
            doi.getStatus(),
            is(DOIIdentifierProvider.MINTED)
        );

        context.turnOffAuthorisationSystem();

        // try to change a value on the item
        itemService.setMetadataModified(item);
        itemService.update(context, item);

        context.commit();
        context.restoreAuthSystemState();

        item = context.reloadEntity(item); // refresh
        // check that the status would be restored to PENDING!
        doi = doiService.findDOIByDSpaceObject(context, item);
        // check its status!
        assertThat(
            "DOI is not PENDING!",
            doi.getStatus(),
            is(DOIIdentifierProvider.PENDING)
        );

    }


    @Test
    public void testDoiMintingAfterSubmission() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();

        WorkspaceItem wsItem = createWorkspaceItem();

        context.commit();
        context.restoreAuthSystemState();

        // proceed with the install of the item!
        context.turnOffAuthorisationSystem();

        Item item = installItemService.installItem(context, context.reloadEntity(wsItem));
        DOI doi = doiService.findDOIByDSpaceObject(context, item);

        assertThat("DOI was not updated to be registered!",
                   doi.getStatus(),
                   is(DOIIdentifierProvider.TO_BE_REGISTERED)
        );

        // commit and wait for the DOIConsumer to execute!
        context.commit();
        context.restoreAuthSystemState();

        // now check that the DOIConsumer has been executed and updated the doi
        doi = doiService.findDOIByDSpaceObject(context, item);

        // The DOI should be update to be registered!
        assertThat(
            "DOI was not minted by the DOIConsumer",
            doi.getStatus(),
            is(DOIIdentifierProvider.UPDATE_BEFORE_REGISTRATION)
        );

    }

    private WorkspaceItem createWorkspaceItem() {
        return WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                   .withSubmitter(submitter)
                                   .withTitle("workspace item")
                                   .withIssueDate("2025-06-09")
                                   .grantLicense()
                                   .withFulltext("test", "test.txt", "This is a test".getBytes())
                                   .build();
    }


}
