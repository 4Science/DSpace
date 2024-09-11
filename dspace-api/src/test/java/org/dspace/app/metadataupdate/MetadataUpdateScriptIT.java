/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metadataupdate;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.junit.Before;
import org.junit.Test;

public class MetadataUpdateScriptIT extends AbstractIntegrationTestWithDatabase {

    private Collection collection;

    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection")
                .withEntityType("Publication")
                .build();
        context.restoreAuthSystemState();

    }

    @Test
    public void testUpdatingMetadata() throws Exception {

        context.turnOffAuthorisationSystem();

        Item itemOne = createItem("Test publication one", "english");

        Item itemTwo = createItem("Test publication two", "English");

        Item itemThree = createItem("Test publication three", "ENGLISH");

        Item itemFour = createItem("Test publication four", "eng");

        Item itemFive = createItem("Test publication five", "ita");

        Item itemSix = createItem("Test publication six", "fra");

        Item itemSeven = createItem("Test publication seven", "English:(French and German)");

        Item itemEight = createItem("Test publication seven", "English:(French and German and Russian)");

        context.restoreAuthSystemState();

        TestDSpaceRunnableHandler runnableHandler = runScript("Publication");

        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(),
                contains("Metadata dc.language.iso has been updated successfully"));

        itemOne = context.reloadEntity(itemOne);
        itemTwo = context.reloadEntity(itemTwo);
        itemThree = context.reloadEntity(itemThree);
        itemFour = context.reloadEntity(itemFour);
        itemFive = context.reloadEntity(itemFive);
        itemSix = context.reloadEntity(itemSix);
        itemSeven = context.reloadEntity(itemSeven);
        itemEight = context.reloadEntity(itemEight);

        assertThat(itemOne.getMetadata(), hasItem(with("dc.language.iso", "en", "2", 0, 500)));
        assertThat(itemTwo.getMetadata(), hasItem(with("dc.language.iso", "en", "2", 0, 500)));
        assertThat(itemThree.getMetadata(), hasItem(with("dc.language.iso", "en", "2", 0, 500)));
        assertThat(itemFour.getMetadata(), hasItem(with("dc.language.iso", "en", "2", 0, 500)));
        assertThat(itemFive.getMetadata(), hasItem(with("dc.language.iso", "it", "6", 0, 500)));
        assertThat(itemSix.getMetadata(), hasItem(with("dc.language.iso", "fr", "5", 0, 500)));
        assertThat(itemSeven.getMetadata(), hasItem(with("dc.language.iso", "en | fr | de", null, 0, 300)));
        assertThat(itemEight.getMetadata(), hasItem(with("dc.language.iso", "en | fr | de | ru", null, 0, 300)));
    }

    @Test
    public void testUpdatingMetadataNotExistingMappingKeys() throws Exception {

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Test publication one")
                .withLanguage("fake")
                .build();
        context.restoreAuthSystemState();

        TestDSpaceRunnableHandler runnableHandler = runScript("Publication");

        assertThat(runnableHandler.getErrorMessages(), empty());
        assertThat(runnableHandler.getInfoMessages(),
                contains("Metadata dc.language.iso has been updated successfully"));

        item = context.reloadEntity(item);

        assertThat(item.getMetadata(), hasItem(with("dc.language.iso", "fake")));
    }

    private TestDSpaceRunnableHandler runScript(String entityType)
            throws InstantiationException, IllegalAccessException {
        TestDSpaceRunnableHandler runnableHandler = new TestDSpaceRunnableHandler();
        String[] args = new String[] { "metadata-update", "-m", "dc.language.iso",
                "-f", "mapConverter-metadataValues.properties",
                "-en", entityType, "-e", admin.getEmail()};
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), runnableHandler, kernelImpl);
        return runnableHandler;
    }

    private Item createItem(String title, String language) {
        return ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withLanguage(language)
                .build();
    }

}
