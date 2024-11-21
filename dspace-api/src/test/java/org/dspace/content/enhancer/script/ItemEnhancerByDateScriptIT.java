/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.script;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.content.Item.ANY;
import static org.dspace.content.enhancer.consumer.ItemEnhancerConsumer.ITEMENHANCER_ENABLED;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ReloadableEntity;
import org.dspace.event.factory.EventServiceFactory;
import org.dspace.event.service.EventService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ItemEnhancerByDateScriptIT extends AbstractIntegrationTestWithDatabase {

    private static ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private static final EventService eventService = EventServiceFactory.getInstance().getEventService();
    private static boolean isEnabled;
    private static String[] consumers;

    private ItemService itemService;

    private Collection collection;

    private Collection persons;

    /**
     * This method will be run before the first test as per @BeforeClass. It will
     * configure the event.dispatcher.default.consumers property to remove the
     * ItemEnhancerConsumer.
     */
    @BeforeClass
    public static void initConsumers() {
        consumers = configService.getArrayProperty("event.dispatcher.default.consumers");
        Set<String> consumersSet = new HashSet<String>(Arrays.asList(consumers));
        if (!consumersSet.contains("itemenhancer")) {
            consumersSet.add("itemenhancer");
            configService.setProperty("event.dispatcher.default.consumers", consumersSet.toArray());
            eventService.reloadConfiguration();
        }
    }

    /**
     * Reset the event.dispatcher.default.consumers property value.
     */
    @AfterClass
    public static void resetDefaultConsumers() {
        configService.setProperty("event.dispatcher.default.consumers", consumers);
        eventService.reloadConfiguration();
    }

    @Before
    public void setup() {

        configService.setProperty(ITEMENHANCER_ENABLED, false);

        itemService = ContentServiceFactory.getInstance().getItemService();

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .build();

        persons = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .withEntityType("Person")
            .build();

        context.restoreAuthSystemState();

    }

    @Test
    public void testItemsEnhancement() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstAuthor = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withPersonMainAffiliation("4Science")
            .build();

        String firstAuthorId = firstAuthor.getID().toString();

        Item secondAuthor = ItemBuilder.createItem(context, collection)
            .withTitle("Jesse Pinkman")
            .withPersonMainAffiliation("Company")
            .build();

        String secondAuthorId = secondAuthor.getID().toString();

        Item firstPublication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withAuthor("Walter White", firstAuthorId)
            .build();

        Item secondPublication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 2")
            .withEntityType("Publication")
            .withAuthor("Walter White", firstAuthorId)
            .withAuthor("Jesse Pinkman", secondAuthorId)
            .build();

        WorkspaceItem thirdPublication = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle("Test publication 3")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman", secondAuthorId)
            .build();

        context.commit();

        firstPublication = reload(firstPublication);
        secondPublication = reload(secondPublication);
        thirdPublication = reload(thirdPublication);

        assertThat(getMetadataValues(firstPublication, "cris.virtual.department"), empty());
        assertThat(getMetadataValues(firstPublication, "cris.virtualsource.department"), empty());
        assertThat(getMetadataValues(secondPublication, "cris.virtual.department"), empty());
        assertThat(getMetadataValues(secondPublication, "cris.virtualsource.department"), empty());
        assertThat(getMetadataValues(thirdPublication, "cris.virtual.department"), empty());
        assertThat(getMetadataValues(thirdPublication, "cris.virtualsource.department"), empty());

        TestDSpaceRunnableHandler runnableHandler = runScript(false);

        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), hasItem("Enhancement completed with success"));

        firstPublication = reload(firstPublication);
        secondPublication = reload(secondPublication);

        assertThat(getMetadataValues(firstPublication, "cris.virtual.department"), hasSize(1));
        assertThat(getMetadataValues(firstPublication, "cris.virtualsource.department"), hasSize(1));

        assertThat(getMetadataValues(secondPublication, "cris.virtual.department"), hasSize(2));
        assertThat(getMetadataValues(secondPublication, "cris.virtualsource.department"), hasSize(2));

        assertThat(firstPublication.getMetadata(), hasItem(with("cris.virtual.department", "4Science")));
        assertThat(firstPublication.getMetadata(), hasItem(with("cris.virtualsource.department", firstAuthorId)));
        assertThat(secondPublication.getMetadata(), hasItem(with("cris.virtual.department", "4Science")));
        assertThat(secondPublication.getMetadata(), hasItem(with("cris.virtualsource.department", firstAuthorId)));
        assertThat(secondPublication.getMetadata(), hasItem(with("cris.virtual.department", "Company", 1)));
        assertThat(secondPublication.getMetadata(), hasItem(with("cris.virtualsource.department", secondAuthorId, 1)));

        assertThat(getMetadataValues(thirdPublication, "cris.virtual.department"), empty());
        assertThat(getMetadataValues(thirdPublication, "cris.virtualsource.department"), empty());

    }

    @Test
    public void testItemEnhancementWithoutForce() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstAuthor = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withPersonMainAffiliation("4Science")
            .build();

        String firstAuthorId = firstAuthor.getID().toString();

        Item secondAuthor = ItemBuilder.createItem(context, collection)
            .withTitle("Jesse Pinkman")
            .withPersonMainAffiliation("Company")
            .build();

        String secondAuthorId = secondAuthor.getID().toString();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 2 ")
            .withEntityType("Publication")
            .withAuthor("Walter White", firstAuthorId)
            .withAuthor("Jesse Pinkman", secondAuthorId)
            .build();

        context.commit();
        publication = reload(publication);

        assertThat(getMetadataValues(publication, "cris.virtual.department"), empty());
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), empty());

        TestDSpaceRunnableHandler runnableHandler = runScript(false);

        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), hasItem("Enhancement completed with success"));

        publication = reload(publication);

        assertThat(getMetadataValues(publication, "cris.virtual.department"), hasSize(2));
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), hasSize(2));

        assertThat(publication.getMetadata(), hasItem(with("cris.virtual.department", "4Science")));
        assertThat(publication.getMetadata(), hasItem(with("cris.virtualsource.department", firstAuthorId)));
        assertThat(publication.getMetadata(), hasItem(with("cris.virtual.department", "Company", 1)));
        assertThat(publication.getMetadata(), hasItem(with("cris.virtualsource.department", secondAuthorId, 1)));

        context.turnOffAuthorisationSystem();

        MetadataValue authorToRemove = getMetadataValues(publication, "dc.contributor.author").get(1);
        itemService.removeMetadataValues(context, publication, List.of(authorToRemove));

        replaceMetadata(firstAuthor, "person", "affiliation", "name", "University");

        context.restoreAuthSystemState();

        runnableHandler = runScript(false);
        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), hasItem("Enhancement completed with success"));

        publication = reload(publication);

        assertThat(getMetadataValues(publication, "cris.virtual.department"), hasSize(1));
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), hasSize(1));

        assertThat(publication.getMetadata(), hasItem(with("cris.virtual.department", "4Science")));
        assertThat(publication.getMetadata(), hasItem(with("cris.virtualsource.department", firstAuthorId)));

    }

    @Test
    public void testItemEnhancementWithForce() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstAuthor = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withPersonMainAffiliation("4Science")
            .build();

        String firstAuthorId = firstAuthor.getID().toString();

        Item secondAuthor = ItemBuilder.createItem(context, collection)
            .withTitle("Jesse Pinkman")
            .withPersonMainAffiliation("Company")
            .build();

        String secondAuthorId = secondAuthor.getID().toString();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 2 ")
            .withEntityType("Publication")
            .withAuthor("Walter White", firstAuthorId)
            .withAuthor("Jesse Pinkman", secondAuthorId)
            .build();

        context.commit();
        publication = reload(publication);

        assertThat(getMetadataValues(publication, "cris.virtual.department"), empty());
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), empty());

        TestDSpaceRunnableHandler runnableHandler = runScript(false);

        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), hasItem("Enhancement completed with success"));

        publication = reload(publication);

        assertThat(getMetadataValues(publication, "cris.virtual.department"), hasSize(2));
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), hasSize(2));

        assertThat(publication.getMetadata(), hasItem(with("cris.virtual.department", "4Science")));
        assertThat(publication.getMetadata(), hasItem(with("cris.virtualsource.department", firstAuthorId)));
        assertThat(publication.getMetadata(), hasItem(with("cris.virtual.department", "Company", 1)));
        assertThat(publication.getMetadata(), hasItem(with("cris.virtualsource.department", secondAuthorId, 1)));

        context.turnOffAuthorisationSystem();

        MetadataValue authorToRemove = getMetadataValues(publication, "dc.contributor.author").get(1);
        itemService.removeMetadataValues(context, publication, List.of(authorToRemove));

        replaceMetadata(firstAuthor, "person", "affiliation", "name", "University");

        context.restoreAuthSystemState();

        runnableHandler = runScript(true);
        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), hasItem("Enhancement completed with success"));

        publication = reload(publication);

        assertThat(getMetadataValues(publication, "cris.virtual.department"), hasSize(1));
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), hasSize(1));

        assertThat(publication.getMetadata(), hasItem(with("cris.virtual.department", "University")));
        assertThat(publication.getMetadata(), hasItem(with("cris.virtualsource.department", firstAuthorId)));

    }

    @Test
    public void testItemEnhancementMetadataPositions() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstAuthor = ItemBuilder.createItem(context, collection)
                                       .withTitle("John Doe")
                                       .build();
        String firstAuthorId = firstAuthor.getID().toString();

        Item secondAuthor = ItemBuilder.createItem(context, collection)
                                      .withTitle("Walter White")
                                      .withPersonMainAffiliation("4Science")
                                      .build();
        String secondAuthorId = secondAuthor.getID().toString();

        Item thirdAuthor = ItemBuilder.createItem(context, collection)
                                       .withTitle("Jesse Pinkman")
                                       .withPersonMainAffiliation("Company")
                                       .build();

        String thirdAuthorId = thirdAuthor.getID().toString();

        Item fourthAuthor = ItemBuilder.createItem(context, collection)
                                      .withTitle("Jesse Smith")
                                      .build();

        String fourthAuthorId = fourthAuthor.getID().toString();

        Item publication = ItemBuilder.createItem(context, collection)
                                      .withTitle("Test publication 2 ")
                                      .withEntityType("Publication")
                                      .withAuthor("John Doe", firstAuthorId)
                                      .withAuthor("Walter White", secondAuthorId)
                                      .withAuthor("Jesse Pinkman", thirdAuthorId)
                                      .withAuthor("Jesse Smith", fourthAuthorId)
                                      .build();

        context.commit();
        publication = reload(publication);

        assertThat(getMetadataValues(publication, "cris.virtual.department"), empty());
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), empty());

        TestDSpaceRunnableHandler runnableHandler = runScript(false);

        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), hasItem("Enhancement completed with success"));

        publication = reload(publication);

        assertThat(getMetadataValues(publication, "cris.virtual.department"), hasSize(4));
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), hasSize(4));

        assertThat(publication.getMetadata(), hasItem(with("cris.virtual.department",
                                                           PLACEHOLDER_PARENT_METADATA_VALUE, 0)));
        assertThat(publication.getMetadata(), hasItem(with("cris.virtualsource.department", firstAuthorId,0)));
        assertThat(publication.getMetadata(), hasItem(with("cris.virtual.department", "4Science", 1)));
        assertThat(publication.getMetadata(), hasItem(with("cris.virtualsource.department", secondAuthorId,1)));
        assertThat(publication.getMetadata(), hasItem(with("cris.virtual.department", "Company", 2)));
        assertThat(publication.getMetadata(), hasItem(with("cris.virtualsource.department", thirdAuthorId, 2)));
        assertThat(publication.getMetadata(), hasItem(with("cris.virtual.department",
                                                           PLACEHOLDER_PARENT_METADATA_VALUE, 3)));
        assertThat(publication.getMetadata(), hasItem(with("cris.virtualsource.department", fourthAuthorId,3)));

    }

    @Test
    public void testItemEnhancementSourceWithoutAuthority() throws Exception {

        context.turnOffAuthorisationSystem();

        Item secondAuthor = ItemBuilder.createItem(context, collection)
                                       .withTitle("Jesse Smith")
                                       .withPersonMainAffiliation("4Science")
                                       .build();

        String secondAuthorId = secondAuthor.getID().toString();

        Item publication = ItemBuilder.createItem(context, collection)
                                      .withTitle("Test publication 2 ")
                                      .withEntityType("Publication")
                                      .withAuthor("Jesse Pinkman")
                                      .withAuthor("Jesse Smith", secondAuthorId)
                                      .build();

        context.commit();
        publication = reload(publication);

        assertThat(getMetadataValues(publication, "cris.virtual.department"), empty());
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), empty());

        TestDSpaceRunnableHandler runnableHandler = runScript(false);

        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), hasItem("Enhancement completed with success"));

        publication = reload(publication);

        assertThat(getMetadataValues(publication, "cris.virtual.department"), hasSize(2));
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), hasSize(2));

        assertThat(publication.getMetadata(), hasItem(with("cris.virtual.department",
                                                           PLACEHOLDER_PARENT_METADATA_VALUE, 0)));
        assertThat(publication.getMetadata(), hasItem(with("cris.virtualsource.department",
                                                           PLACEHOLDER_PARENT_METADATA_VALUE,0)));
        assertThat(publication.getMetadata(), hasItem(with("cris.virtual.department", "4Science", 1)));
        assertThat(publication.getMetadata(), hasItem(with("cris.virtualsource.department", secondAuthorId,1)));

    }

    @Test
    public void testItemEnhancementWithoutAuthorities() throws Exception {

        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, collection)
                                      .withTitle("Test publication 2 ")
                                      .withEntityType("Publication")
                                      .withAuthor("Jesse Pinkman")
                                      .withAuthor("Jesse Smith")
                                      .build();

        context.commit();
        publication = reload(publication);

        assertThat(getMetadataValues(publication, "cris.virtual.department"), empty());
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), empty());

        TestDSpaceRunnableHandler runnableHandler = runScript(false);

        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), hasItem("Enhancement completed with success"));

        publication = reload(publication);

        assertThat(getMetadataValues(publication, "cris.virtual.department"), empty());
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), empty());

    }

    @Test
    public void testItemEnhancementEntityTypeInvalidCollectionUUID() throws Exception {

        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 2 ")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Smith")
            .build();

        context.commit();
        publication = reload(publication);

        TestDSpaceRunnableHandler runnableHandler =
            runScript(false, publication.getID().toString(), null, "*", null, null);

        assertThat(runnableHandler.getException(), notNullValue());
        assertThat(runnableHandler.getException().getMessage(),
            equalToIgnoringCase("specified Collection does not exist"));
    }

    @Test
    public void testItemEnhancementEntityTypeAbortWhenInvalidSolrQuery() throws Exception {

        context.turnOffAuthorisationSystem();
        context.commit();
        String query = "(test : info";

        TestDSpaceRunnableHandler runnableHandler =
            runScript(false, null, null, query, null, null);

        assertThat(runnableHandler.getErrorMessages(), hasItems());
        assertThat(runnableHandler.getErrorMessages(), hasItem(containsString("ParseException")));
        assertThat(runnableHandler.getErrorMessages(),
            hasItem("An error occurs during enhancement. The process is aborted"));
    }

    @Test
    public void testItemEnhancementEntityTypeAbortWhenEmptySolrQuery() throws Exception {

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 2 ")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Smith")
            .build();

        context.commit();
        String query = "neverexistingdsolrindexfield:true";

        TestDSpaceRunnableHandler runnableHandler =
            runScript(false, null, null, query, null, null);

        assertThat(runnableHandler.getInfoMessages(), hasItems());
        assertThat(runnableHandler.getInfoMessages(), hasItem("No results in solr-Query"));
    }

    @Test
    public void testItemEnhancementEntityTypeInvalidEntityType() throws Exception {

        context.turnOffAuthorisationSystem();
        context.commit();

        TestDSpaceRunnableHandler runnableHandler
            = runScript(false, null, "ResearchData", "*", null, null);

        assertThat(runnableHandler.getException(), notNullValue());
        assertThat(runnableHandler.getException().getMessage(), equalToIgnoringCase("unknown EntityType ResearchData"));
    }

    @Test
    public void testItemEnhancementEntityTypeMaxMatches() throws Exception {

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Smith")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 2 ")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Smith")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 3")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Smith")
            .build();

        context.commit();
        String max = "2";

        TestDSpaceRunnableHandler runnableHandler
            = runScript(false, collection.getID().toString(), null, "*", null, max);

        assertThat(runnableHandler.getInfoMessages(), hasItems());
        assertThat(runnableHandler.getInfoMessages(), hasItem("enhanced " + max + " items"));
    }

    @Test
    public void testItemEnhancementEntityTypeLimitAndMaxMatches() throws Exception {

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Smith")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 2 ")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Smith")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 3")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Smith")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 4")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Smith")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 5")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Smith")
            .build();

        context.commit();

        String limit = "2";
        String max = "3";

        TestDSpaceRunnableHandler runnableHandler
            = runScript(false, collection.getID().toString(), null, "*", limit, max);

        assertThat(runnableHandler.getInfoMessages(), hasItems());
        assertThat(runnableHandler.getInfoMessages(), hasItem("enhanced 2 out of max " + max + " items"));
        assertThat(runnableHandler.getInfoMessages(), hasItem("enhanced 3 out of max " + max + " items"));
        assertThat(runnableHandler.getInfoMessages(), hasItem("enhanced " + max + " items"));
    }

    @Test
    public void testItemEnhancementEntityTypeLimitAndMaxMatches2() throws Exception {

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Smith")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 2 ")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Smith")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 3")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Smith")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 4")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Smith")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 5")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Smith")
            .build();

        context.commit();

        String limit = "2";
        String max = "3";

        TestDSpaceRunnableHandler runnableHandler
            = runScript(false, collection.getID().toString(), null, "*", limit, max);

        assertThat(runnableHandler.getInfoMessages(), hasItems());
        assertThat(runnableHandler.getInfoMessages(), hasItem("enhanced 2 out of max " + max + " items"));
        assertThat(runnableHandler.getInfoMessages(), hasItem("enhanced 3 out of max " + max + " items"));
        assertThat(runnableHandler.getInfoMessages(), hasItem("enhanced " + max + " items"));
    }

    @Test
    public void testItemEnhancementEntityTypeLimitAndMaxMatches3() throws Exception {

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Smith")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 2 ")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Smith")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 3")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Smith")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 4")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Smith")
            .build();

        context.commit();

        String limit = "2";
        String max = "5";

        TestDSpaceRunnableHandler runnableHandler
            = runScript(false, collection.getID().toString(), null, "*", limit, max);

        assertThat(runnableHandler.getInfoMessages(), hasItems());
        assertThat(runnableHandler.getInfoMessages(), hasItem("enhanced 2 out of max " + max + " items"));
        assertThat(runnableHandler.getInfoMessages(), hasItem("enhanced 4 out of max " + max + " items"));
        assertThat(runnableHandler.getInfoMessages(), hasItem("enhanced 4 items"));
    }


    private TestDSpaceRunnableHandler runScript(boolean force) throws InstantiationException, IllegalAccessException {
        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = force ? new String[] { "item-enhancer-date", "-f" } : new String[] { "item-enhancer-date" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);
        return runnableHandler;
    }

    private TestDSpaceRunnableHandler runScript(boolean force, String collectionuuid, String entitytype, String query,
                                                String limit, String max)
        throws InstantiationException, IllegalAccessException {
        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        List<String> argslist = new ArrayList<>();
        argslist.add("item-enhancer-date");
        if (force) {
            argslist.add("-f");
        }
        if (StringUtils.isNotBlank(collectionuuid)) {
            argslist.add("-c " + collectionuuid);
        }
        if (StringUtils.isNotBlank(entitytype)) {
            argslist.add("-e " + entitytype);
        }
        if (StringUtils.isNotBlank(query)) {
            argslist.add("-q " + query);
        }
        if (StringUtils.isNotBlank(limit)) {
            argslist.add("-l " + limit);
        }
        if (StringUtils.isNotBlank(max)) {
            argslist.add("-m " + max);
        }
        String[] args = argslist.toArray(new String[0]);

        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);
        return runnableHandler;
    }

    @SuppressWarnings("rawtypes")
    private <T extends ReloadableEntity> T reload(T entity) throws SQLException, AuthorizeException {
        return context.reloadEntity(entity);
    }

    private void replaceMetadata(Item item, String schema, String element, String qualifier, String newValue)
        throws SQLException, AuthorizeException {
        itemService.replaceMetadata(context, reload(item), schema, element, qualifier, ANY, newValue, null, -1, 0);
    }

    private List<MetadataValue> getMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField);
    }

    private List<MetadataValue> getMetadataValues(WorkspaceItem item, String metadataField) {
        return itemService.getMetadataByMetadataString(item.getItem(), metadataField);
    }

}
