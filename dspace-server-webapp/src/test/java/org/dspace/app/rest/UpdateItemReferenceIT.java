/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.event.factory.EventServiceFactory;
import org.dspace.event.service.EventService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
* @author Mykhaylo Boychuk (mykhaylo.boychuk at 4Science.it)
*/
public class UpdateItemReferenceIT extends AbstractControllerIntegrationTest {


    private static final ConfigurationService configService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    private static final EventService eventService = EventServiceFactory.getInstance().getEventService();
    private static String[] consumers;

    @Autowired
    private ItemService itemService;

    /**
     * This method will be run before the first test as per @BeforeClass. It will
     * configure the event.dispatcher.default.consumers property to add the
     * CrisConsumer.
     */
    @BeforeClass
    public static void initCrisConsumer() {
        consumers = configService.getArrayProperty("event.dispatcher.default.consumers");
        Set<String> consumersSet = new HashSet<String>(Arrays.asList(consumers));
        consumersSet.remove("referenceresolver");
        consumersSet.remove("crisconsumer");
        configService.setProperty("event.dispatcher.default.consumers", consumersSet.toArray());
        eventService.reloadConfiguration();
    }

    /**
     * Reset the event.dispatcher.default.consumers property value.
     */
    @AfterClass
    public static void resetDefaultConsumers() {
        configService.setProperty("event.dispatcher.default.consumers", consumers);
        eventService.reloadConfiguration();
    }

    @Test
    public void updateItemReferenceTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community rootCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("My Root Community")
                                                  .build();
        Collection collection = CollectionBuilder.createCollection(context, rootCommunity)
                                                 .withName("My collection")
                                                 .build();

        Item publication1 = ItemBuilder.createItem(context, collection)
                                       .withEntityType("Publication")
                                       .withTitle("Title Publication 1")
                                       .withAuthor("M.Boychuk", "will be referenced::ORCID::0000-0000-0012-3456")
                                       .withAuthor("V.Stus", "will be referenced::ORCID::0000-0000-0078-9101")
                                       .build();

        Item publication2 = ItemBuilder.createItem(context, collection)
                                       .withEntityType("Publication")
                                       .withTitle("Title Publication 2")
                                       .withAuthor("M.Boychuk", "will be referenced::ORCID::0000-0000-0012-3456")
                                       .build();

        Item publication3 = ItemBuilder.createItem(context, collection)
                                       .withEntityType("Publication")
                                       .withTitle("Title Publication 3")
                                       .withAuthor("O.Beketov", "will be referenced::ORCID::0000-0000-5612-5555")
                                       .build();

        Item publicationNotArchived = ItemBuilder.createItem(context, collection)
                                             .withEntityType("Publication")
                                             .withTitle("Publication not archived")
                                             .withdrawn()
                                             .withAuthor("M.Boychuk", "will be referenced::ORCID::0000-0000-0012-3456")
                                             .build();

        Item misha = ItemBuilder.createItem(context, collection)
                                .withEntityType("Person")
                                .withFullName("Misha Boychuk")
                                .withTitle("Misha Boychuk")
                                .withOrcidIdentifier("0000-0000-0012-3456")
                                .build();

        Item viktor = ItemBuilder.createItem(context, collection)
                                 .withEntityType("Person")
                                 .withFullName("Viktor Stus")
                                 .withTitle("Viktor Stus")
                                 .withOrcidIdentifier("0000-0000-0078-9101")
                                 .build();

        context.restoreAuthSystemState();

        //verify that the authority values was not resolved yet
        publication1 = context.reloadEntity(publication1);
        publication2 = context.reloadEntity(publication2);
        publication3 = context.reloadEntity(publication3);
        publicationNotArchived = context.reloadEntity(publicationNotArchived);
        List<MetadataValue> values = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);
        List<MetadataValue> values2 = itemService.getMetadata(publication2, "dc", "contributor", "author", Item.ANY);
        List<MetadataValue> values3 = itemService.getMetadata(publication3, "dc", "contributor", "author", Item.ANY);
        List<MetadataValue> values4 = itemService.getMetadata(publicationNotArchived, "dc", "contributor", "author",
                Item.ANY);

        assertEquals(values.size(), 2);
        // check authority value
        assertTrue(StringUtils.equalsAny("will be referenced::ORCID::0000-0000-0012-3456", values.get(0).getAuthority(),
                values.get(1).getAuthority()));
        assertTrue(StringUtils.equalsAny("will be referenced::ORCID::0000-0000-0078-9101", values.get(0).getAuthority(),
                values.get(1).getAuthority()));
        // check metadata value
        assertTrue(StringUtils.equalsAny("M.Boychuk", values.get(0).getValue(), values.get(1).getValue()));
        assertTrue(StringUtils.equalsAny("V.Stus", values.get(0).getValue(), values.get(1).getValue()));

        // check authority value
        assertEquals(values2.size(), 1);
        assertEquals(values2.get(0).getAuthority(), "will be referenced::ORCID::0000-0000-0012-3456");
        // check metadata value
        assertEquals("M.Boychuk", values2.get(0).getValue());

        // check authority value
        assertEquals(values3.size(), 1);
        assertEquals(values3.get(0).getAuthority(), "will be referenced::ORCID::0000-0000-5612-5555");
        // check metadata value
        assertEquals("O.Beketov", values3.get(0).getValue());

        // check authority value
        assertEquals(values4.size(), 1);
        assertEquals(values4.get(0).getAuthority(), "will be referenced::ORCID::0000-0000-0012-3456");
        // check metadata value
        assertEquals("M.Boychuk", values4.get(0).getValue());

        // perform the script
        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] { "update-item-references" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        //verify that the authority values was resolved
        publication1 = context.reloadEntity(publication1);
        publication2 = context.reloadEntity(publication2);
        publication3 = context.reloadEntity(publication3);
        publicationNotArchived = context.reloadEntity(publicationNotArchived);
        values = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);
        values2 = itemService.getMetadata(publication2, "dc", "contributor", "author", Item.ANY);
        values3 = itemService.getMetadata(publication3, "dc", "contributor", "author", Item.ANY);
        values4 = itemService.getMetadata(publicationNotArchived, "dc", "contributor", "author",Item.ANY);

        assertEquals(values.size(), 2);
        assertTrue(StringUtils.equalsAny(misha.getID().toString(), values.get(0).getAuthority(),
                values.get(1).getAuthority()));
        assertTrue(StringUtils.equalsAny(viktor.getID().toString(), values.get(0).getAuthority(),
                values.get(1).getAuthority()));
        // check metadata value
        assertTrue(StringUtils.equalsAny("M.Boychuk", values.get(0).getValue(), values.get(1).getValue()));
        assertTrue(StringUtils.equalsAny("V.Stus", values.get(0).getValue(), values.get(1).getValue()));

        assertEquals(values2.size(), 1);
        assertEquals(values2.get(0).getAuthority(), misha.getID().toString());
        // check metadata value
        assertEquals("M.Boychuk", values2.get(0).getValue());

        // publication3 was not resolved
        assertEquals(values3.size(), 1);
        assertEquals(values3.get(0).getAuthority(), "will be referenced::ORCID::0000-0000-5612-5555");
        // check metadata value
        assertEquals("O.Beketov", values3.get(0).getValue());

        // publication withdrawn was not resolved
        assertEquals(values4.size(), 1);
        assertEquals(values4.get(0).getAuthority(), "will be referenced::ORCID::0000-0000-0012-3456");
        // check metadata value
        assertEquals("M.Boychuk", values4.get(0).getValue());
    }

    @Test
    public void updateItemReferenceAndEnableOverrideMetadataValueTest() throws Exception {
        ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        configService.setProperty("cris.item-reference-resolution.override-metadata-value", true);
        context.turnOffAuthorisationSystem();

        Community rootCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("My Root Community")
                                                  .build();
        Collection collection = CollectionBuilder.createCollection(context, rootCommunity)
                                                 .withName("My collection")
                                                 .build();

        Item misha = ItemBuilder.createItem(context, collection)
                                .withEntityType("Person")
                                .withFullName("Misha Boychuk")
                                .withTitle("Misha Boychuk")
                                .withOrcidIdentifier("0000-0000-0012-3456")
                                .build();

        Item viktor = ItemBuilder.createItem(context, collection)
                                 .withEntityType("Person")
                                 .withFullName("Viktor Stus")
                                 .withTitle("Viktor Stus")
                                 .withOrcidIdentifier("0000-0000-0078-9101")
                                 .build();

        Item publication1 = ItemBuilder.createItem(context, collection)
                                       .withEntityType("Publication")
                                       .withTitle("Title Publication 1")
                                       .withAuthor("M.Boychuk", "will be referenced::ORCID::0000-0000-0012-3456")
                                       .withAuthor("V.Stus", viktor.getID().toString())
                                       .build();

        Item publication2 = ItemBuilder.createItem(context, collection)
                                       .withEntityType("Publication")
                                       .withTitle("Title Publication 2")
                                       .withAuthor("V.Stus", "will be referenced::ORCID::0000-0000-0078-9101")
                                       .build();

        context.restoreAuthSystemState();

        //verify that the authority values was not resolved yet
        publication1 = context.reloadEntity(publication1);
        publication2 = context.reloadEntity(publication2);
        List<MetadataValue> values = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);
        List<MetadataValue> values2 = itemService.getMetadata(publication2, "dc", "contributor", "author", Item.ANY);

        assertEquals(values.size(), 2);
        // check authority value
        assertTrue(StringUtils.equalsAny("will be referenced::ORCID::0000-0000-0012-3456", values.get(0).getAuthority(),
                values.get(1).getAuthority()));
        assertTrue(StringUtils.equalsAny(viktor.getID().toString(), values.get(0).getAuthority(),
                values.get(1).getAuthority()));
        // check metadata value
        assertTrue(StringUtils.equalsAny("M.Boychuk", values.get(0).getValue(), values.get(1).getValue()));
        assertTrue(StringUtils.equalsAny("V.Stus", values.get(0).getValue(), values.get(1).getValue()));

        // check authority value
        assertEquals(values2.size(), 1);
        assertEquals(values2.get(0).getAuthority(), "will be referenced::ORCID::0000-0000-0078-9101");
        // check metadata value
        assertEquals("V.Stus", values2.get(0).getValue());

        // perform the script
        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] { "update-item-references" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        //verify that the authority values was resolved
        publication1 = context.reloadEntity(publication1);
        publication2 = context.reloadEntity(publication2);
        values = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);
        values2 = itemService.getMetadata(publication2, "dc", "contributor", "author", Item.ANY);

        assertEquals(values.size(), 2);
        assertTrue(StringUtils.equalsAny(misha.getID().toString(), values.get(0).getAuthority(),
                values.get(1).getAuthority()));
        assertTrue(StringUtils.equalsAny(viktor.getID().toString(), values.get(0).getAuthority(),
                values.get(1).getAuthority()));
        // check metadata value
        assertTrue(StringUtils.equalsAny(misha.getName(), values.get(0).getValue(), values.get(1).getValue()));
        assertTrue(StringUtils.equalsAny("V.Stus", values.get(0).getValue(), values.get(1).getValue()));

        assertEquals(values2.size(), 1);
        assertEquals(values2.get(0).getAuthority(), viktor.getID().toString());
        // check metadata value
        assertEquals(viktor.getName(), values2.get(0).getValue());
    }

    @Test
    public void updateItemReferenceAndReferenceValueBadFormedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community rootCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("My Root Community")
                                                  .build();
        Collection collection = CollectionBuilder.createCollection(context, rootCommunity)
                                                 .withName("My collection")
                                                 .build();

        ItemBuilder.createItem(context, collection)
                   .withEntityType("Person")
                   .withFullName("Misha Boychuk")
                   .withTitle("Misha Boychuk")
                   .withOrcidIdentifier("0000-0000-0012-3456")
                   .build();

        Item publication = ItemBuilder.createItem(context, collection)
                                      .withEntityType("Publication")
                                      .withTitle("Title Publication 1")
                                      .withAuthor("M.Boychuk", "will be referenced::ORCID")
                                      .build();

        context.restoreAuthSystemState();

        //verify that the authority values was not resolved yet
        publication = context.reloadEntity(publication);
        List<MetadataValue> values = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        assertEquals(values.size(), 1);
        // check authority value
        assertEquals("will be referenced::ORCID", values.get(0).getAuthority());
        // check metadata value
        assertEquals("M.Boychuk", values.get(0).getValue());

        // perform the script
        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] { "update-item-references" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        publication = context.reloadEntity(publication);
        values = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        assertEquals(values.size(), 1);
        // check authority value
        assertEquals("will be referenced::ORCID", values.get(0).getAuthority());
        // check metadata value
        assertEquals("M.Boychuk", values.get(0).getValue());
    }

    @Test
    public void updateItemReferenceAndEntityTypeDoesNotMatchTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community rootCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("My Root Community")
                                                  .build();
        Collection collection = CollectionBuilder.createCollection(context, rootCommunity)
                                                 .withName("My collection")
                                                 .build();

        ItemBuilder.createItem(context, collection)
                   .withFullName("Misha Boychuk")
                   .withTitle("Misha Boychuk")
                   .withOrcidIdentifier("0000-0000-0012-3456")
                   .build();

        Item publication = ItemBuilder.createItem(context, collection)
                                      .withEntityType("Publication")
                                      .withTitle("Title Publication 1")
                                      .withAuthor("M.Boychuk", "will be referenced::ORCID::0000-0000-0012-3456")
                                      .build();

        context.restoreAuthSystemState();

        //verify that the authority values was not resolved yet
        publication = context.reloadEntity(publication);
        List<MetadataValue> values = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        assertEquals(values.size(), 1);
        // check authority value
        assertEquals("will be referenced::ORCID::0000-0000-0012-3456", values.get(0).getAuthority());
        // check metadata value
        assertEquals("M.Boychuk", values.get(0).getValue());

        // perform the script
        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] { "update-item-references", "-a" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        //verify that the authority values was not resolved
        publication = context.reloadEntity(publication);
        values = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);

        assertEquals(values.size(), 1);
        // check authority value
        assertEquals("will be referenced::ORCID::0000-0000-0012-3456", values.get(0).getAuthority());
        // check metadata value
        assertEquals("M.Boychuk", values.get(0).getValue());
    }

    @Test
    public void updateAllItemsReferenceTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community rootCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("My Root Community")
                                                  .build();
        Collection collection = CollectionBuilder.createCollection(context, rootCommunity)
                                                 .withName("My collection")
                                                 .build();

        Item publication1 = ItemBuilder.createItem(context, collection)
                                       .withEntityType("Publication")
                                       .withTitle("Title Publication 1")
                                       .withAuthor("M.Boychuk", "will be referenced::ORCID::0000-0000-0012-3456")
                                       .withdrawn()
                                       .build();

        Item publication2 = ItemBuilder.createItem(context, collection)
                                       .withEntityType("Publication")
                                       .withTitle("Title Publication 2")
                                       .withAuthor("V.Stus", "will be referenced::ORCID::0000-0000-0078-9101")
                                       .build();

        Item misha = ItemBuilder.createItem(context, collection)
                                .withEntityType("Person")
                                .withFullName("Misha Boychuk")
                                .withTitle("Misha Boychuk")
                                .withOrcidIdentifier("0000-0000-0012-3456")
                                .build();

        Item viktor = ItemBuilder.createItem(context, collection)
                                 .withEntityType("Person")
                                 .withFullName("Viktor Stus")
                                 .withTitle("Viktor Stus")
                                 .withOrcidIdentifier("0000-0000-0078-9101")
                                 .build();

        context.restoreAuthSystemState();

        //verify that the authority values was not resolved yet
        publication1 = context.reloadEntity(publication1);
        publication2 = context.reloadEntity(publication2);
        List<MetadataValue> values = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);
        List<MetadataValue> values2 = itemService.getMetadata(publication2, "dc", "contributor", "author", Item.ANY);

        assertEquals(1, values.size());
        // check authority value
        assertEquals("will be referenced::ORCID::0000-0000-0012-3456", values.get(0).getAuthority());
        // check metadata value
        assertEquals("M.Boychuk", values.get(0).getValue());

        // check authority value
        assertEquals(1, values2.size());
        assertEquals("will be referenced::ORCID::0000-0000-0078-9101", values2.get(0).getAuthority());
        // check metadata value
        assertEquals("V.Stus", values2.get(0).getValue());

        // perform the script
        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] { "update-item-references", "-a" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        //verify that the authority values was resolved only for withdrawn items
        publication1 = context.reloadEntity(publication1);
        publication2 = context.reloadEntity(publication2);
        values = itemService.getMetadata(publication1, "dc", "contributor", "author", Item.ANY);
        values2 = itemService.getMetadata(publication2, "dc", "contributor", "author", Item.ANY);

        assertEquals(1, values.size());
        assertEquals(misha.getID().toString(), values.get(0).getAuthority());
        // check metadata value
        assertEquals("M.Boychuk", values.get(0).getValue());

        // check authority value
        assertEquals(1, values2.size());
        assertEquals(viktor.getID().toString(), values2.get(0).getAuthority());
        // check metadata value
        assertEquals("V.Stus", values2.get(0).getValue());
    }

}