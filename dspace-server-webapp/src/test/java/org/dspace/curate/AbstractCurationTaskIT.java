/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.SiteService;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class AbstractCurationTaskIT extends AbstractIntegrationTestWithDatabase {

    private static final String MOCK_CURATION_TASK = "mockcurationtask";

    private static final SiteService siteService = ContentServiceFactory.getInstance().getSiteService();
    private static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    @Before
    public void setup() throws Exception {
        DSpaceServicesFactory.getInstance().getConfigurationService()
                             .setProperty("plugin.named.org.dspace.curate.CurationTask",
                                          MockDistributiveCurationTask.class.getName() + " = " +
                                              MOCK_CURATION_TASK);

        CoreServiceFactory.getInstance().getPluginService().clearNamedPluginClasses();
    }

    @Test
    public void testDistributeWritesProcessMetadataToItem() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Test Community")
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("Test Collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Item in Collection")
                               .build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"curate", "-t", MOCK_CURATION_TASK, "-i", item.getHandle()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl,
                                    admin);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(1));

        context.reloadEntity(item);
        String processMeta = itemService.getMetadata(item, "cris.curation.process");
        String historyMeta = itemService.getMetadata(item, "cris.curation.history");

        assertEquals(MOCK_CURATION_TASK, processMeta);
        assertThat(historyMeta, containsString("Executed " + MOCK_CURATION_TASK + " on"));
    }

    @Test
    public void testDistributeSkipItemInSecondRun() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Test Community")
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("Test Collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Item in Collection")
                               .build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"curate", "-t", MOCK_CURATION_TASK, "-i", item.getHandle()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl,
                                    admin);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(1));

        // Second execution — should not process the item again
        TestDSpaceRunnableHandler secondHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), secondHandler, kernelImpl, admin);

        assertThat(secondHandler.getErrorMessages(), empty());
        assertThat(secondHandler.getWarningMessages(), empty());
        assertThat(secondHandler.getInfoMessages(),
                   hasItem(containsString(
                       "Curation task: " + MOCK_CURATION_TASK + " performed on: " + item.getHandle() +
                           " with status: 0"))
        );
    }

    @Test
    public void testDistributeWritesProcessMetadataToItemsInCollection() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Test Community")
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("Test Collection")
                                                 .build();

        Item item1 = ItemBuilder.createItem(context, collection)
                                .withTitle("Item1 in Collection")
                                .build();

        Item item2 = ItemBuilder.createItem(context, collection)
                                .withTitle("Item2 in Collection")
                                .build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"curate", "-t", MOCK_CURATION_TASK, "-i", collection.getHandle()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl,
                                    admin);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(1));
        String message = testDSpaceRunnableHandler.getInfoMessages()
                                                  .stream()
                                                  .filter(m -> m.startsWith(
                                                      "Curation task: mockcurationtask performed on:"))
                                                  .findFirst()
                                                  .orElseThrow(() -> new AssertionError("Expected message not found"));

        assertThat(message, containsString("Curation task: mockcurationtask performed on: " + collection.getHandle()));
        assertThat(message, containsString(
            String.format("Processing item with handle=%s and uuid=%s", item1.getHandle(), item1.getID())));
        assertThat(message, containsString(
            String.format("Processing item with handle=%s and uuid=%s", item2.getHandle(), item2.getID())));

        context.reloadEntity(item1);
        String processMeta1 = itemService.getMetadata(item1, "cris.curation.process");
        String historyMeta1 = itemService.getMetadata(item1, "cris.curation.history");

        assertEquals(MOCK_CURATION_TASK, processMeta1);
        assertThat(historyMeta1, containsString("Executed " + MOCK_CURATION_TASK + " on"));

        context.reloadEntity(item2);
        String processMeta2 = itemService.getMetadata(item2, "cris.curation.process");
        String historyMeta2 = itemService.getMetadata(item2, "cris.curation.history");

        assertEquals(MOCK_CURATION_TASK, processMeta2);
        assertThat(historyMeta2, containsString("Executed " + MOCK_CURATION_TASK + " on"));
    }

    @Test
    public void testDistributeWritesProcessMetadataToItemsInCommunity() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Test Community")
                                              .build();

        Collection collection1 = CollectionBuilder.createCollection(context, community)
                                                  .withName("Test Collection1")
                                                  .build();

        Item item1 = ItemBuilder.createItem(context, collection1)
                                .withTitle("Item1 in Collection1")
                                .build();

        Item item2 = ItemBuilder.createItem(context, collection1)
                                .withTitle("Item2 in Collection1")
                                .build();

        Collection collection2 = CollectionBuilder.createCollection(context, community)
                                                  .withName("Test Collection1")
                                                  .build();

        Item item3 = ItemBuilder.createItem(context, collection2)
                                .withTitle("Item3 in Collection2")
                                .build();

        Item item4 = ItemBuilder.createItem(context, collection2)
                                .withTitle("Item4 in Collection2")
                                .build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"curate", "-t", MOCK_CURATION_TASK, "-i", community.getHandle()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl,
                                    admin);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(1));

        String message = testDSpaceRunnableHandler.getInfoMessages()
                                                  .stream()
                                                  .filter(m -> m.startsWith(
                                                      "Curation task: mockcurationtask performed on:"))
                                                  .findFirst()
                                                  .orElseThrow(() -> new AssertionError("Expected message not found"));

        assertThat(message, containsString("Curation task: mockcurationtask performed on: " + community.getHandle()));
        assertThat(message, containsString(
            String.format("Processing item with handle=%s and uuid=%s", item1.getHandle(), item1.getID())));
        assertThat(message, containsString(
            String.format("Processing item with handle=%s and uuid=%s", item2.getHandle(), item2.getID())));
        assertThat(message, containsString(
            String.format("Processing item with handle=%s and uuid=%s", item3.getHandle(), item3.getID())));
        assertThat(message, containsString(
            String.format("Processing item with handle=%s and uuid=%s", item4.getHandle(), item4.getID())));


        context.reloadEntity(item1);
        String processMeta1 = itemService.getMetadata(item1, "cris.curation.process");
        String historyMeta1 = itemService.getMetadata(item1, "cris.curation.history");

        assertEquals(MOCK_CURATION_TASK, processMeta1);
        assertThat(historyMeta1, containsString("Executed " + MOCK_CURATION_TASK + " on"));

        context.reloadEntity(item2);
        String processMeta2 = itemService.getMetadata(item2, "cris.curation.process");
        String historyMeta2 = itemService.getMetadata(item2, "cris.curation.history");

        assertEquals(MOCK_CURATION_TASK, processMeta2);
        assertThat(historyMeta2, containsString("Executed " + MOCK_CURATION_TASK + " on"));

        context.reloadEntity(item3);
        String processMeta3 = itemService.getMetadata(item3, "cris.curation.process");
        String historyMeta3 = itemService.getMetadata(item3, "cris.curation.history");

        assertEquals(MOCK_CURATION_TASK, processMeta3);
        assertThat(historyMeta3, containsString("Executed " + MOCK_CURATION_TASK + " on"));

        context.reloadEntity(item4);
        String processMeta4 = itemService.getMetadata(item4, "cris.curation.process");
        String historyMeta4 = itemService.getMetadata(item4, "cris.curation.history");

        assertEquals(MOCK_CURATION_TASK, processMeta4);
        assertThat(historyMeta4, containsString("Executed " + MOCK_CURATION_TASK + " on"));
    }

    @Test
    public void testDistributeWritesProcessMetadataToItemsInSubCommunity() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Test Community")
                                              .build();

        Community subCommunity = CommunityBuilder.createCommunity(context)
                                                 .withName("Test SubCommunity")
                                                 .addParentCommunity(context, community)
                                                 .build();

        Collection collection1 = CollectionBuilder.createCollection(context, community)
                                                  .withName("Test Collection1")
                                                  .build();

        Item item1 = ItemBuilder.createItem(context, collection1)
                                .withTitle("Item1 in Collection1")
                                .build();

        Item item2 = ItemBuilder.createItem(context, collection1)
                                .withTitle("Item2 in Collection1")
                                .build();

        Collection collection2 = CollectionBuilder.createCollection(context, subCommunity)
                                                  .withName("Test Collection1")
                                                  .build();

        Item item3 = ItemBuilder.createItem(context, collection2)
                                .withTitle("Item3 in Collection2")
                                .build();

        Item item4 = ItemBuilder.createItem(context, collection2)
                                .withTitle("Item4 in Collection2")
                                .build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"curate", "-t", MOCK_CURATION_TASK, "-i", community.getHandle()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl,
                                    admin);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(1));

        String message = testDSpaceRunnableHandler.getInfoMessages()
                                                  .stream()
                                                  .filter(m -> m.startsWith(
                                                      "Curation task: mockcurationtask performed on:"))
                                                  .findFirst()
                                                  .orElseThrow(() -> new AssertionError("Expected message not found"));

        assertThat(message, containsString("Curation task: mockcurationtask performed on: " + community.getHandle()));
        assertThat(message, containsString(
            String.format("Processing item with handle=%s and uuid=%s", item1.getHandle(), item1.getID())));
        assertThat(message, containsString(
            String.format("Processing item with handle=%s and uuid=%s", item2.getHandle(), item2.getID())));
        assertThat(message, containsString(
            String.format("Processing item with handle=%s and uuid=%s", item3.getHandle(), item3.getID())));
        assertThat(message, containsString(
            String.format("Processing item with handle=%s and uuid=%s", item4.getHandle(), item4.getID())));


        context.reloadEntity(item1);
        String processMeta1 = itemService.getMetadata(item1, "cris.curation.process");
        String historyMeta1 = itemService.getMetadata(item1, "cris.curation.history");

        assertEquals(MOCK_CURATION_TASK, processMeta1);
        assertThat(historyMeta1, containsString("Executed " + MOCK_CURATION_TASK + " on"));

        context.reloadEntity(item2);
        String processMeta2 = itemService.getMetadata(item2, "cris.curation.process");
        String historyMeta2 = itemService.getMetadata(item2, "cris.curation.history");

        assertEquals(MOCK_CURATION_TASK, processMeta2);
        assertThat(historyMeta2, containsString("Executed " + MOCK_CURATION_TASK + " on"));

        context.reloadEntity(item3);
        String processMeta3 = itemService.getMetadata(item3, "cris.curation.process");
        String historyMeta3 = itemService.getMetadata(item3, "cris.curation.history");

        assertEquals(MOCK_CURATION_TASK, processMeta3);
        assertThat(historyMeta3, containsString("Executed " + MOCK_CURATION_TASK + " on"));

        context.reloadEntity(item4);
        String processMeta4 = itemService.getMetadata(item4, "cris.curation.process");
        String historyMeta4 = itemService.getMetadata(item4, "cris.curation.history");

        assertEquals(MOCK_CURATION_TASK, processMeta4);
        assertThat(historyMeta4, containsString("Executed " + MOCK_CURATION_TASK + " on"));
    }

    @Test
    public void testDistributeWritesProcessMetadataToItemsInSite() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Test Community")
                                              .build();

        Collection collection1 = CollectionBuilder.createCollection(context, community)
                                                  .withName("Test Collection1")
                                                  .build();

        Item item1 = ItemBuilder.createItem(context, collection1)
                                .withTitle("Item1 in Collection1")
                                .build();

        Item item2 = ItemBuilder.createItem(context, collection1)
                                .withTitle("Item2 in Collection1")
                                .build();

        Collection collection2 = CollectionBuilder.createCollection(context, community)
                                                  .withName("Test Collection1")
                                                  .build();

        Item item3 = ItemBuilder.createItem(context, collection2)
                                .withTitle("Item3 in Collection2")
                                .build();

        Item item4 = ItemBuilder.createItem(context, collection2)
                                .withTitle("Item4 in Collection2")
                                .build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"curate", "-t", MOCK_CURATION_TASK, "-i", "all"};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl,
                                    admin);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(1));

        String message = testDSpaceRunnableHandler.getInfoMessages()
                                                  .stream()
                                                  .filter(m -> m.startsWith(
                                                      "Curation task: mockcurationtask performed on:"))
                                                  .findFirst()
                                                  .orElseThrow(() -> new AssertionError("Expected message not found"));

        assertThat(message, containsString(
            "Curation task: mockcurationtask performed on: " + siteService.findSite(context).getHandle()));
        assertThat(message, containsString(
            String.format("Processing item with handle=%s and uuid=%s", item1.getHandle(), item1.getID())));
        assertThat(message, containsString(
            String.format("Processing item with handle=%s and uuid=%s", item2.getHandle(), item2.getID())));
        assertThat(message, containsString(
            String.format("Processing item with handle=%s and uuid=%s", item3.getHandle(), item3.getID())));
        assertThat(message, containsString(
            String.format("Processing item with handle=%s and uuid=%s", item4.getHandle(), item4.getID())));


        context.reloadEntity(item1);
        String processMeta1 = itemService.getMetadata(item1, "cris.curation.process");
        String historyMeta1 = itemService.getMetadata(item1, "cris.curation.history");

        assertEquals(MOCK_CURATION_TASK, processMeta1);
        assertThat(historyMeta1, containsString("Executed " + MOCK_CURATION_TASK + " on"));

        context.reloadEntity(item2);
        String processMeta2 = itemService.getMetadata(item2, "cris.curation.process");
        String historyMeta2 = itemService.getMetadata(item2, "cris.curation.history");

        assertEquals(MOCK_CURATION_TASK, processMeta2);
        assertThat(historyMeta2, containsString("Executed " + MOCK_CURATION_TASK + " on"));

        context.reloadEntity(item3);
        String processMeta3 = itemService.getMetadata(item3, "cris.curation.process");
        String historyMeta3 = itemService.getMetadata(item3, "cris.curation.history");

        assertEquals(MOCK_CURATION_TASK, processMeta3);
        assertThat(historyMeta3, containsString("Executed " + MOCK_CURATION_TASK + " on"));

        context.reloadEntity(item4);
        String processMeta4 = itemService.getMetadata(item4, "cris.curation.process");
        String historyMeta4 = itemService.getMetadata(item4, "cris.curation.history");

        assertEquals(MOCK_CURATION_TASK, processMeta4);
        assertThat(historyMeta4, containsString("Executed " + MOCK_CURATION_TASK + " on"));
    }


    @Distributive
    public static class MockDistributiveCurationTask extends AbstractCurationTask {
        private boolean executed = false;

        @Override
        public int perform(DSpaceObject dso) throws IOException {
            distribute(dso);
            return Curator.CURATE_SUCCESS;
        }

        @Override
        protected void performItem(Item item) throws SQLException, IOException {
            String result = "Processing item with handle=" + item.getHandle()
                + " and uuid=" + item.getID();
            setResult(result);
            executed = true;
            // no-op other than metadata writing
        }

        @Override
        protected boolean isSuccessfullyExecuted(Item dso) {
            return executed;
        }
    }


}
