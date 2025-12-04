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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

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
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class AbstractCurationTaskIT extends AbstractIntegrationTestWithDatabase {

    private static final String MOCK_CURATION_TASK = "mockcurationtask";
    private static final long HALF_YEAR_TIME = 180L * 24L * 60L * 60000L;
    private static final long ONE_YEAR_TIME = 360L * 24L * 60L * 60000L;
    private static final SiteService siteService = ContentServiceFactory.getInstance().getSiteService();
    private static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private static final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    private static final IndexingService indexingService = DSpaceServicesFactory.getInstance().getServiceManager()
                                                                                .getServiceByName(
                                                                                    IndexingService.class.getName(),
                                                                                    IndexingService.class);
    private final Instant nowTime = Instant.now();
    private final Instant oldestModifiedDate = nowTime.minusMillis(ONE_YEAR_TIME);
    private final Instant midModifiedDate = nowTime.minusMillis(HALF_YEAR_TIME);

    @Before
    public void setup() throws Exception {
        // Override with only the mock task
        configurationService.setProperty("plugin.named.org.dspace.curate.CurationTask",
                                         MockDistributiveCurationTask.class.getName() + " = " + MOCK_CURATION_TASK);

    }


    @After
    public void teardown() {
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
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(3));

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
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(3));

        // Second execution — should not process the item again
        TestDSpaceRunnableHandler secondHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), secondHandler, kernelImpl, admin);

        assertThat(secondHandler.getErrorMessages(), empty());
        assertThat(secondHandler.getWarningMessages(), empty());
        assertThat(secondHandler.getInfoMessages(),
                   hasItem(is(
                       "Curation task: " + MOCK_CURATION_TASK + " performed on: " + item.getHandle() +
                           " with status: 0. Result: 'performedItemCount: 0'"))
        );

        String processMetadata = itemService.getMetadata(item, "cris.curation.process");
        assertEquals("cris.curation.process should be set to " + MOCK_CURATION_TASK, MOCK_CURATION_TASK,
                     processMetadata);

        String historyMetadata = itemService.getMetadata(item, "cris.curation.history");
        assertNotNull(historyMetadata);
        String[] history = historyMetadata.split("\n");
        assertThat(history[0], containsString("Executed " + MOCK_CURATION_TASK + " on "));
    }

    @Test
    public void testDistributeProcessItemInForcedSecondRun() throws Exception {
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
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(3));

        // Second forced execution — should process the item again
        args = Stream.concat(Arrays.stream(args), Stream.of("-f")).toArray(String[]::new);
        TestDSpaceRunnableHandler secondHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), secondHandler, kernelImpl, admin);

        assertThat(secondHandler.getErrorMessages(), empty());
        assertThat(secondHandler.getWarningMessages(), empty());
        assertThat(secondHandler.getInfoMessages(),
                   hasItem(is(
                       "Curation task: " + MOCK_CURATION_TASK + " performed on: " + item.getHandle() +
                           " with status: 0. Result: 'Processing item with handle=" + item.getHandle() + " and uuid=" +
                           item.getID() +
                           "\nperformedItemCount: 1'"))
        );

        String[] historyMetadata = itemService.getMetadata(context.reloadEntity(item), "cris.curation.history").split(
            "\n");

        assertThat(historyMetadata[0], containsString("Executed " + MOCK_CURATION_TASK + " on"));
        assertThat(historyMetadata[1], containsString("Executed " + MOCK_CURATION_TASK + " on"));
    }

    @Test
    public void testDistributeWritesProcessMetadataToOnlyOneOfMultipleItems() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Test Community")
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("Test Collection")
                                                 .build();

        // Create 10 items
        List<Item> items = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Item item = ItemBuilder.createItem(context, collection)
                                   .withTitle("Item " + i)
                                   .build();
            items.add(item);
        }

        context.restoreAuthSystemState();

        // Run the script only on the first item
        Item targetItem = items.get(0);
        String[] args = new String[] {"curate", "-t", MOCK_CURATION_TASK, "-i", targetItem.getHandle()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl,
                                    admin);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(3));

        // Validate that only the first item got the curation metadata
        for (int i = 0; i < items.size(); i++) {
            Item current = items.get(i);
            context.reloadEntity(current);

            String processMeta = itemService.getMetadata(current, "cris.curation.process");
            String historyMeta = itemService.getMetadata(current, "cris.curation.history");

            if (i == 0) {
                assertEquals(MOCK_CURATION_TASK, processMeta);
                assertThat(historyMeta, containsString("Executed " + MOCK_CURATION_TASK + " on"));
            } else {
                assertNull("Other items should not have process metadata", processMeta);
                assertNull("Other items should not have history metadata", historyMeta);
            }
        }
    }


    @Test
    public void shouldProcessOnlyMostRecentlyModifiedItemWhenLimitIsOneDay() throws Exception {
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
        Item item3 = ItemBuilder.createItem(context, collection)
                                .withTitle("Item3 in Collection")
                                .build();


        // Create spies
        item2 = Mockito.spy(item2);
        item3 = Mockito.spy(item3);

        // Mock only getLastModified()
        Mockito.when(item2.getLastModified()).thenReturn(midModifiedDate);
        Mockito.when(item3.getLastModified()).thenReturn(oldestModifiedDate);

        // Index the spies
        indexingService.indexContent(context, new IndexableItem(item2), true);
        indexingService.indexContent(context, new IndexableItem(item3), true);
        indexingService.commit();

        context.restoreAuthSystemState();

        String[] args = new String[] {"curate", "-t", MOCK_CURATION_TASK, "-i", community.getHandle(), "-l", "1"};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl,
                                    admin);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(3));

        assertThat(testDSpaceRunnableHandler.getInfoMessages(),
                   hasItem(is(
                       "Curation task: " + MOCK_CURATION_TASK + " performed on: " + community.getHandle() +
                           " with status: 0. Result: 'Processing item with handle=" + item1.getHandle() + " and uuid=" +
                           item1.getID() +
                           "\nperformedItemCount: 1'"))
        );
    }

    @Test
    public void testCurateCommandProcessesOnlyOneRecentlyModifiedItemWhenLimitIsHalfYear() throws Exception {
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
        Item item3 = ItemBuilder.createItem(context, collection)
                                .withTitle("Item3 in Collection")
                                .build();


        // Create spies
        item2 = Mockito.spy(item2);
        item3 = Mockito.spy(item3);

        // Mock only getLastModified()
        Mockito.when(item2.getLastModified()).thenReturn(midModifiedDate);
        Mockito.when(item3.getLastModified()).thenReturn(oldestModifiedDate);

        // Index the spies
        indexingService.indexContent(context, new IndexableItem(item2), true);
        indexingService.indexContent(context, new IndexableItem(item3), true);
        indexingService.commit();

        context.restoreAuthSystemState();

        String[] args = new String[] {"curate", "-t", MOCK_CURATION_TASK, "-i", community.getHandle(), "-l", "180"};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl,
                                    admin);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(3));

        String message = testDSpaceRunnableHandler.getInfoMessages()
                                                  .stream()
                                                  .filter(m -> m.startsWith(
                                                      "Curation task: " + MOCK_CURATION_TASK + " performed on:"))
                                                  .findFirst()
                                                  .orElseThrow(() -> new AssertionError("Expected message not found"));

        assertThat(message,
                   containsString("Curation task: " + MOCK_CURATION_TASK + " performed on: " + community.getHandle()));
        assertThat(message, containsString(
            String.format("Processing item with handle=%s and uuid=%s", item1.getHandle(), item1.getID())));
        assertThat(message, containsString(
            String.format("Processing item with handle=%s and uuid=%s", item2.getHandle(), item2.getID())));
        assertThat(message, not(containsString(
            String.format("Processing item with handle=%s and uuid=%s", item3.getHandle(), item3.getID()))));
    }

    @Test
    public void testCurateCommandProcessesTwoRecentlyModifiedItemsWhenLimitIsOneYear() throws Exception {
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
        Item item3 = ItemBuilder.createItem(context, collection)
                                .withTitle("Item3 in Collection")
                                .build();


        // Create spies
        item2 = Mockito.spy(item2);
        item3 = Mockito.spy(item3);

        // Mock only getLastModified()
        Mockito.when(item2.getLastModified()).thenReturn(midModifiedDate);
        Mockito.when(item3.getLastModified()).thenReturn(oldestModifiedDate);

        // Index the spies
        indexingService.indexContent(context, new IndexableItem(item2), true);
        indexingService.indexContent(context, new IndexableItem(item3), true);
        indexingService.commit();

        context.restoreAuthSystemState();

        String[] args = new String[] {"curate", "-t", MOCK_CURATION_TASK, "-i", community.getHandle(), "-l", "360"};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl,
                                    admin);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(3));

        String message = testDSpaceRunnableHandler.getInfoMessages()
                                                  .stream()
                                                  .filter(m -> m.startsWith(
                                                      "Curation task: " + MOCK_CURATION_TASK + " performed on:"))
                                                  .findFirst()
                                                  .orElseThrow(() -> new AssertionError("Expected message not found"));

        assertThat(message,
                   containsString("Curation task: " + MOCK_CURATION_TASK + " performed on: " + community.getHandle()));
        assertThat(message, containsString(
            String.format("Processing item with handle=%s and uuid=%s", item1.getHandle(), item1.getID())));
        assertThat(message, containsString(
            String.format("Processing item with handle=%s and uuid=%s", item2.getHandle(), item2.getID())));
        assertThat(message, containsString(
            String.format("Processing item with handle=%s and uuid=%s", item3.getHandle(), item3.getID())));
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
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(3));
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
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(3));

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
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(3));

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
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(3));

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

    @Test
    public void testDistributeSearchCalledOncePerItemWithBatchSizeOne() throws Exception {
        // Set batch size to 1 so distribute() executes one item per search call
        configurationService.setProperty("curation.task.batchsize", 1);

        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Test Community")
                                              .build();

        Collection collection1 = CollectionBuilder.createCollection(context, community)
                                                  .withName("Collection 1")
                                                  .build();

        Item item1 = ItemBuilder.createItem(context, collection1)
                                .withTitle("Item 1")
                                .build();
        Item item2 = ItemBuilder.createItem(context, collection1)
                                .withTitle("Item 2")
                                .build();

        Collection collection2 = CollectionBuilder.createCollection(context, community)
                                                  .withName("Collection 2")
                                                  .build();

        Item item3 = ItemBuilder.createItem(context, collection2)
                                .withTitle("Item 3")
                                .build();
        Item item4 = ItemBuilder.createItem(context, collection2)
                                .withTitle("Item 4")
                                .build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"curate", "-t", MOCK_CURATION_TASK, "-i", "all"};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl,
                                    admin);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(6));
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
        assertThat(message, containsString("performedItemCount: 4"));

    }

    @Test
    public void testDistributeWritesProcessItemsListIfOneErrorOccur() throws Exception {
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
                                .withHandle("123456789/BrokenHandle")
                                .withTitle("Item2 in Collection")
                                .build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"curate", "-t", MOCK_CURATION_TASK, "-i", collection.getHandle()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl,
                                    admin);

        item1 = context.reloadEntity(item1);
        item2 = context.reloadEntity(item2);

        String processMetadataItem1 = itemService.getMetadata(item1, "cris.curation.process");
        assertEquals("cris.curation.process should be set to " + MOCK_CURATION_TASK, MOCK_CURATION_TASK,
                     processMetadataItem1);

        String processMetadataItem2 = itemService.getMetadata(item2, "cris.curation.process");
        assertNull("cris.curation.process should not be set", processMetadataItem2);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(3));
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
            String.format("Unable to process item with handle=%s and uuid=%s", item2.getHandle(), item2.getID())));

        String historyMetadataItem1 = itemService.getMetadata(item1, "cris.curation.history");
        assertNotNull(historyMetadataItem1);
        String[] historyItem1 = historyMetadataItem1.split("\n");
        assertThat(historyItem1.length, is(1));
        assertThat(historyItem1[0], containsString("Executed " + MOCK_CURATION_TASK + " on "));

        String historyMetadataItem2 = itemService.getMetadata(item2, "cris.curation.history");
        assertNotNull(historyMetadataItem2);
        String[] historyItem2 = historyMetadataItem2.split("\n");
        assertThat(historyItem2.length, is(1));
        assertThat(historyItem2[0], containsString("Executed " + MOCK_CURATION_TASK + " on "));

    }

    @Test
    public void testDistributeWritesProcessItemsListIfOneErrorOccurAndIsReRun() throws Exception {
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
                                .withHandle("123456789/BrokenHandle")
                                .withTitle("Item2 in Collection")
                                .build();

        context.restoreAuthSystemState();

        String[] args = new String[] {"curate", "-t", MOCK_CURATION_TASK, "-i", collection.getHandle()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl,
                                    admin);

        testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl,
                                    admin);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(3));
        String message = testDSpaceRunnableHandler.getInfoMessages()
                                                  .stream()
                                                  .filter(m -> m.startsWith(
                                                      "Curation task: mockcurationtask performed on:"))
                                                  .findFirst()
                                                  .orElseThrow(() -> new AssertionError("Expected message not found"));

        assertThat(message, containsString("Curation task: mockcurationtask performed on: " + collection.getHandle()));
        assertThat(message, containsString(
            String.format("Unable to process item with handle=%s and uuid=%s", item2.getHandle(), item2.getID())));

        String processMetadataItem1 = itemService.getMetadata(item1, "cris.curation.process");
        assertEquals("cris.curation.process should be set to " + MOCK_CURATION_TASK, MOCK_CURATION_TASK,
                     processMetadataItem1);

        String processMetadataItem2 = itemService.getMetadata(item2, "cris.curation.process");
        assertNull("cris.curation.process should not be set in clean tasks", processMetadataItem2);

        item2 = context.reloadEntity(item2);

        String historyMetadata = itemService.getMetadata(item2, "cris.curation.history");
        assertNotNull(historyMetadata);
        String[] history = historyMetadata.split("\n");
        assertThat(history.length, is(2));
        assertThat(history[0], containsString("Executed " + MOCK_CURATION_TASK + " on "));
        assertThat(history[1], containsString("Executed " + MOCK_CURATION_TASK + " on "));

    }


    @Distributive
    public static class MockDistributiveCurationTask extends AbstractCurationTask {
        private int performItemCount = 0;
        private boolean executed = false;

        @Override
        public int perform(DSpaceObject dso) throws IOException {
            distribute(dso);
            setResult("performedItemCount: " + performItemCount);
            return Curator.CURATE_SUCCESS;
        }

        @Override
        protected void performItem(Item item) throws SQLException, IOException {
            if (item.getHandle().equals("123456789/BrokenHandle")) {
                executed = false;
                throw new SQLException("BrokenHandle");
            }
            String result = "Processing item with handle=" + item.getHandle()
                + " and uuid=" + item.getID();
            setResult(result);
            executed = true;
            performItemCount++;
            // no-op other than metadata writing
        }

        @Override
        protected boolean isSuccessfullyExecuted(Item dso) {
            return executed;
        }
    }


}
