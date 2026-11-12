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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
import org.dspace.content.authority.AuthorityBackedRelationshipServiceImpl;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.AuthorityBackedRelationshipService;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ReloadableEntity;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.event.factory.EventServiceFactory;
import org.dspace.event.service.EventService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ItemEnhancerScriptIT extends AbstractIntegrationTestWithDatabase {

    private static ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private static final EventService eventService = EventServiceFactory.getInstance().getEventService();

    private PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();

    private ChoiceAuthorityService choiceAuthorityService = ContentAuthorityServiceFactory
        .getInstance().getChoiceAuthorityService();

    private MetadataAuthorityService metadataAuthorityService = ContentAuthorityServiceFactory
        .getInstance().getMetadataAuthorityService();

    private static boolean isEnabled;
    private static String[] consumers;
    private ItemService itemService;

    private AuthorityBackedRelationshipService authorityBackedRelationshipService;

    private Collection collection;


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
    public void setup() throws Exception {
        itemService = ContentServiceFactory.getInstance().getItemService();

        DSpace dspace = new DSpace();
        authorityBackedRelationshipService = dspace.getServiceManager().getServiceByName(
            AuthorityBackedRelationshipServiceImpl.class.getCanonicalName(), AuthorityBackedRelationshipService.class);

        // Configure the ItemAuthority used by the promotion of authority-backed metadata to relationships
        choiceAuthorityService.getChoiceAuthoritiesNames();
        configService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] { "org.dspace.content.authority.ItemAuthority = AuthorAuthority" });
        configService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configService.setProperty("choices.plugin.dc.contributor.editor", "AuthorAuthority");
        configService.setProperty("authority.controlled.dc.contributor.author", "true");
        configService.setProperty("authority.controlled.dc.contributor.editor", "true");
        configService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();
        configService.setProperty(ITEMENHANCER_ENABLED, false);

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .build();
        context.restoreAuthSystemState();

    }

    @Test
    public void testItemsEnhancement() throws Exception {
        context.turnOffAuthorisationSystem();

        Item firstAuthor = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withEntityType("Person")
            .withPersonMainAffiliation("4Science")
            .build();

        String firstAuthorId = firstAuthor.getID().toString();

        Item secondAuthor = ItemBuilder.createItem(context, collection)
            .withTitle("Jesse Pinkman")
            .withEntityType("Person")
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

        final String randomUUID = UUID.randomUUID().toString();
        Item thirdPublication = ItemBuilder.createItem(context, collection)
                .withTitle("Test publication 3")
                .withEntityType("Publication")
                .withAuthor("Walter White", firstAuthorId)
                .withAuthor("Jesse Pinkman", secondAuthorId)
                // same author multiple time
                .withAuthor("Walter White", firstAuthorId)
                // an author is also an editor
                .withEditor("Jesse Pinkman", secondAuthorId)
                .withEditor("Editor WithExternaAuthority", "external-authority")
                .withEditor("Editor WithExternaUUIDAuthority", randomUUID)
                .build();

        WorkspaceItem wsPublication = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle("Test workspace publication")
            .withEntityType("Publication")
            .withEditor("Jesse Pinkman", secondAuthorId)
            .build();

        promoteRelationships(firstPublication, firstAuthor);
        promoteRelationships(secondPublication, firstAuthor, secondAuthor);
        promoteRelationships(thirdPublication, firstAuthor, secondAuthor);

        context.commit();

        firstPublication = reload(firstPublication);
        secondPublication = reload(secondPublication);
        wsPublication = reload(wsPublication);

        assertThat(getMetadataValues(firstPublication, "dspace.virtual.department"), empty());
        assertThat(getMetadataValues(firstPublication, "dspace.virtual.orcid"), empty());
        assertThat(getMetadataValues(secondPublication, "dspace.virtual.department"), empty());
        assertThat(getMetadataValues(secondPublication, "dspace.virtual.orcid"), empty());
        assertThat(getMetadataValues(wsPublication, "dspace.virtual.department"), empty());
        assertThat(getMetadataValues(wsPublication, "dspace.virtual.orcid"), empty());

        TestDSpaceRunnableHandler runnableHandler = runScript(false);

        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), contains("Enhancement completed with success"));

        firstPublication = reload(firstPublication);
        secondPublication = reload(secondPublication);
        thirdPublication = reload(thirdPublication);
        wsPublication = reload(wsPublication);

        assertThat(getMetadataValues(firstPublication, "dspace.virtual.department"), hasSize(1));
        assertThat(getMetadataValues(firstPublication, "dspace.virtual.orcid"), empty());
        assertThat(getMetadataValueStrings(firstPublication, "dspace.virtual.department"),
                containsInAnyOrder("4Science"));

        assertThat(getMetadataValues(secondPublication, "dspace.virtual.department"), hasSize(2));
        assertThat(getMetadataValues(secondPublication, "dspace.virtual.orcid"), empty());
        assertThat(getMetadataValueStrings(secondPublication, "dspace.virtual.department"),
                containsInAnyOrder("4Science", "Company"));

        // The same person is both author and editor, so each occurrence produces its own additional values.
        assertThat(getMetadataValues(thirdPublication, "dspace.virtual.department"), hasSize(4));
        assertThat(getMetadataValues(thirdPublication, "dspace.virtual.orcid"), empty());
        assertThat(getMetadataValueStrings(thirdPublication, "dspace.virtual.department"),
                containsInAnyOrder("4Science", "4Science", "Company", "Company"));

        assertThat(getMetadataValues(wsPublication, "dspace.virtual.department"), empty());
        assertThat(getMetadataValues(wsPublication, "dspace.virtual.orcid"), empty());

    }

    @Test
    public void testItemEnhancementWithoutForce() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstAuthor = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withEntityType("Person")
            .withPersonMainAffiliation("4Science")
            .build();

        String firstAuthorId = firstAuthor.getID().toString();

        Item secondAuthor = ItemBuilder.createItem(context, collection)
            .withTitle("Jesse Pinkman")
            .withEntityType("Person")
            .withPersonMainAffiliation("Company")
            .build();

        String secondAuthorId = secondAuthor.getID().toString();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 2 ")
            .withEntityType("Publication")
            .withAuthor("Walter White", firstAuthorId)
            .withAuthor("Jesse Pinkman", secondAuthorId)
            .build();

        promoteRelationships(publication, firstAuthor, secondAuthor);

        context.commit();
        publication = reload(publication);

        assertThat(getMetadataValues(publication, "dspace.virtual.department"), empty());
        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), empty());

        TestDSpaceRunnableHandler runnableHandler = runScript(false);

        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), contains("Enhancement completed with success"));

        publication = reload(publication);

        assertThat(getMetadataValues(publication, "dspace.virtual.department"), hasSize(2));
        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), empty());

        assertThat(getMetadataValueStrings(publication, "dspace.virtual.department"),
                containsInAnyOrder("4Science", "Company"));

        context.turnOffAuthorisationSystem();

        MetadataValue authorToRemove = getMetadataValues(publication, "dc.contributor.author").get(1);
        itemService.removeMetadataValues(context, publication, List.of(authorToRemove));

        replaceMetadata(firstAuthor, "person", "affiliation", "name", "University");

        context.commit();
        context.restoreAuthSystemState();

        runnableHandler = runScript(false);
        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), contains("Enhancement completed with success"));

        publication = reload(publication);

        assertThat(getMetadataValues(publication, "dspace.virtual.department"), hasSize(1));
        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), empty());

        assertThat(getMetadataValueStrings(publication, "dspace.virtual.department"),
                containsInAnyOrder("University"));

    }

    @Test
    public void testItemEnhancementWithForce() throws Exception {
        context.turnOffAuthorisationSystem();

        Item editor = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withEntityType("Person")
            .withPersonMainAffiliation("4Science")
            .build();

        String editorId = editor.getID().toString();

        Item author = ItemBuilder.createItem(context, collection)
            .withTitle("Jesse Pinkman")
            .withEntityType("Person")
            .withPersonMainAffiliation("Company")
            .withPersonMainAffiliation("Another Company")
            .build();

        String authorId = author.getID().toString();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication 2 ")
            .withEntityType("Publication")
            .withEditor("Walter White", editorId)
            .withAuthor("Jesse Pinkman", authorId)
            .build();

        promoteRelationships(publication, editor, author);

        context.commit();
        publication = reload(publication);

        assertThat(getMetadataValues(publication, "dspace.virtual.department"), empty());
        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), empty());

        TestDSpaceRunnableHandler runnableHandler = runScript(false);

        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), contains("Enhancement completed with success"));

        publication = reload(publication);

        assertThat(getMetadataValues(publication, "dspace.virtual.department"), hasSize(3));
        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), empty());

        assertThat(getMetadataValueStrings(publication, "dspace.virtual.department"),
                containsInAnyOrder("4Science", "Company", "Another Company"));

        context.turnOffAuthorisationSystem();

        MetadataValue authorToRemove = getMetadataValues(publication, "dc.contributor.author").get(0);
        itemService.removeMetadataValues(context, publication, List.of(authorToRemove));

        replaceMetadata(editor, "person", "affiliation", "name", "University");

        context.commit();
        context.restoreAuthSystemState();

        runnableHandler = runScript(true);
        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), contains("Enhancement completed with success"));

        publication = reload(publication);

        assertThat(getMetadataValues(publication, "dspace.virtual.department"), hasSize(1));
        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), empty());

        assertThat(getMetadataValueStrings(publication, "dspace.virtual.department"),
                containsInAnyOrder("University"));
    }

    @Test
    public void testItemEnhancementMetadataPositions() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstAuthor = ItemBuilder.createItem(context, collection)
                                       .withTitle("John Doe")
                                       .withEntityType("Person")
                                       .build();
        String firstAuthorId = firstAuthor.getID().toString();

        Item secondAuthor = ItemBuilder.createItem(context, collection)
                                      .withTitle("Walter White")
                                      .withEntityType("Person")
                                      .withPersonMainAffiliation("4Science")
                                      .build();
        String secondAuthorId = secondAuthor.getID().toString();

        Item thirdAuthor = ItemBuilder.createItem(context, collection)
                                       .withTitle("Jesse Pinkman")
                                       .withEntityType("Person")
                                       .withPersonMainAffiliation("Company")
                                       .build();

        String thirdAuthorId = thirdAuthor.getID().toString();

        Item fourthAuthor = ItemBuilder.createItem(context, collection)
                                      .withTitle("Jesse Smith")
                                      .withEntityType("Person")
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

        promoteRelationships(publication, firstAuthor, secondAuthor, thirdAuthor, fourthAuthor);

        context.commit();
        publication = reload(publication);

        assertThat(getMetadataValues(publication, "dspace.virtual.department"), empty());
        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), empty());

        TestDSpaceRunnableHandler runnableHandler = runScript(false);

        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), contains("Enhancement completed with success"));

        publication = reload(publication);

        assertThat(getMetadataValues(publication, "dspace.virtual.department"), hasSize(2));
        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), empty());

        assertThat(getMetadataValueStrings(publication, "dspace.virtual.department"), containsInAnyOrder(
                "4Science", "Company"));
    }

    @Test
    public void testItemEnhancementSourceWithoutAuthority() throws Exception {
        context.turnOffAuthorisationSystem();

        Item secondAuthor = ItemBuilder.createItem(context, collection)
                                       .withTitle("Jesse Smith")
                                       .withEntityType("Person")
                                       .withPersonMainAffiliation("4Science")
                                       .build();

        String secondAuthorId = secondAuthor.getID().toString();

        Item publication = ItemBuilder.createItem(context, collection)
                                      .withTitle("Test publication 2 ")
                                      .withEntityType("Publication")
                                      .withAuthor("Jesse Pinkman")
                                      .withEditor("Jesse Smith", secondAuthorId)
                                      .build();

        promoteRelationships(publication, secondAuthor);

        context.commit();
        publication = reload(publication);

        assertThat(getMetadataValues(publication, "dspace.virtual.department"), empty());
        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), empty());

        TestDSpaceRunnableHandler runnableHandler = runScript(false);

        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), contains("Enhancement completed with success"));

        publication = reload(publication);

        assertThat(getMetadataValues(publication, "dspace.virtual.department"), hasSize(1));
        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), empty());

        assertThat(publication.getMetadata(), hasItem(
                with("dspace.virtual.department", "4Science", null, secondAuthorId, null, -1)));

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

        assertThat(getMetadataValues(publication, "dspace.virtual.department"), empty());
        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), empty());

        TestDSpaceRunnableHandler runnableHandler = runScript(false);

        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(), contains("Enhancement completed with success"));

        publication = reload(publication);

        assertThat(getMetadataValues(publication, "dspace.virtual.department"), empty());
        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), empty());

    }

    private TestDSpaceRunnableHandler runScript(boolean force) throws InstantiationException, IllegalAccessException {
        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = force ? new String[] { "item-enhancer", "-f" } : new String[] { "item-enhancer" };
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

    private void promoteRelationships(Item publication, Item... people) throws SQLException, AuthorizeException {
        for (Item person : people) {
            promoteField(publication, person, "author");
            promoteField(publication, person, "editor");
        }
    }

    private void promoteField(Item publication, Item person, String element)
        throws SQLException, AuthorizeException {
        String authority = person.getID().toString();
        for (MetadataValue value : getMetadataValues(publication, "dc.contributor." + element)) {
            if (authority.equals(value.getAuthority())) {
                authorityBackedRelationshipService.promoteResolvedAuthority(context, publication, value, person);
            }
        }
    }

    private List<String> getMetadataValueStrings(Item item, String metadataField) {
        return getMetadataValues(item, metadataField).stream()
            .map(MetadataValue::getValue)
            .collect(Collectors.toList());
    }

    private List<MetadataValue> getMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField);
    }

    private List<MetadataValue> getMetadataValues(WorkspaceItem item, String metadataField) {
        return itemService.getMetadataByMetadataString(item.getItem(), metadataField);
    }

}
