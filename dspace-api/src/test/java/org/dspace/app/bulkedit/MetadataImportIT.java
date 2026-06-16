/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Strings;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.service.ScriptService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MetadataImportIT extends AbstractIntegrationTestWithDatabase {

    private final ItemService itemService
            = ContentServiceFactory.getInstance().getItemService();
    private final EPersonService ePersonService
            = EPersonServiceFactory.getInstance().getEPersonService();
    private final RelationshipService relationshipService
            = ContentServiceFactory.getInstance().getRelationshipService();
    private final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();
    private final MetadataAuthorityService metadataAuthorityService
            = ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();

    private Collection collection;
    private Collection publicationCollection;
    private Collection personCollection;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Configure authority-controlled fields for testing (since config files may be incomplete)
        // Ensure dc.contributor.author is explicitly configured
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        // Configure dc.contributor.other as authority-controlled for testing
        configurationService.setProperty("authority.controlled.dc.contributor.other", "true");
        configurationService.setProperty("choices.plugin.dc.contributor.other", "AuthorAuthority");
        // Reload configuration service to ensure properties are picked up
        configurationService.reloadConfig();
        // Clear cache to reload authority configuration
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        this.collection = CollectionBuilder.createCollection(context, community).build();
        this.publicationCollection = CollectionBuilder.createCollection(context, community)
                                           .withEntityType("Publication")
                                           .build();
        this.personCollection = CollectionBuilder.createCollection(context, community)
                                                 .withEntityType("Person")
                                                 .build();
        context.restoreAuthSystemState();
    }

    @After
    public void after() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        List<Relationship> relationships = relationshipService.findAll(context);
        for (Relationship relationship : relationships) {
            relationshipService.delete(context, relationship);
        }
        context.restoreAuthSystemState();
    }

    @Test
    public void metadataImportTestWithDuplicateHeader() {
        String[] csv = {"id,collection,dc.title,dc.title,dc.contributor.author",
            "+," + collection.getHandle() + ",\"Test Import 1\",\"Test Import 2\"," + "\"Donald, SmithImported\"," +
            "+," + collection.getHandle() + ",\"Test Import 3\",\"Test Import 4\"," + "\"Donald, SmithImported\""};
        // Should throw an exception because of duplicate header
        try {
            performImportScript(csv);
        } catch (Exception e) {
            assertTrue(e instanceof MetadataImportInvalidHeadingException);
        }
    }

    @Test
    public void metadataImportTestWithAnyLanguage() {
        String[] csv = {"id,collection,dc.title[*],dc.contributor.author",
            "+," + collection.getHandle() + ",\"Test Import 1\"," + "\"Donald, SmithImported\""};
        // Should throw an exception because of invalid ANY language (*) in metadata field
        try {
            performImportScript(csv);
        } catch (Exception e) {
            assertTrue(e instanceof MetadataImportInvalidHeadingException);
        }
    }

    @Test
    public void metadataImportTest() throws Exception {
        String[] csv = {"id,collection,dc.title,dc.contributor.author",
            "+," + collection.getHandle() + ",\"Test Import 1\"," + "\"Donald, SmithImported\""};
        performImportScript(csv);
        Item importedItem = findItemByName("Test Import 1");
        assertTrue(
            Strings.CS.equals(
                itemService.getMetadata(importedItem, "dc", "contributor", "author", Item.ANY).get(0).getValue(),
                "Donald, SmithImported"));
        eperson = ePersonService.findByEmail(context, admin.getEmail());
        assertEquals(importedItem.getSubmitter(), eperson);

        context.turnOffAuthorisationSystem();
        itemService.delete(context, itemService.find(context, importedItem.getID()));
        context.restoreAuthSystemState();
    }

    @Test
    public void metadataImportIntoCollectionWithEntityTypeWithTemplateEnabledTest() throws Exception {
        String[] csv = {"id,collection,dc.title,dc.contributor.author",
            "+," + publicationCollection.getHandle() + ",\"Test Import 1\"," + "\"Donald, SmithImported\""};
        performImportScript(csv, true);
        Item importedItem = findItemByName("Test Import 1");
        assertTrue(Strings.CS.equals(itemService.getMetadata(importedItem, "dc", "contributor", "author", Item.ANY)
                              .get(0).getValue(), "Donald, SmithImported"));
        assertTrue(Strings.CS.equals(itemService.getMetadata(importedItem, "dspace", "entity", "type", Item.ANY)
                              .get(0).getValue(), "Publication"));
        EPerson eperson = ePersonService.findByEmail(context, admin.getEmail());
        assertEquals(importedItem.getSubmitter(), eperson);

        context.turnOffAuthorisationSystem();
        itemService.delete(context, itemService.find(context, importedItem.getID()));
        context.restoreAuthSystemState();
    }

    @Test
    public void metadataImportIntoCollectionWithEntityTypeWithTemplateDisabledTest() throws Exception {
        String[] csv = {"id,collection,dc.title,dc.contributor.author",
            "+," + publicationCollection.getHandle() + ",\"Test Import 1\"," + "\"Donald, SmithImported\""};
        performImportScript(csv, false);
        Item importedItem = findItemByName("Test Import 1");
        assertTrue(Strings.CS.equals(itemService.getMetadata(importedItem, "dc", "contributor", "author", Item.ANY)
            .get(0).getValue(), "Donald, SmithImported"));
        assertEquals(1, itemService.getMetadata(importedItem, "dspace", "entity", "type", Item.ANY)
            .size());
        EPerson eperson = ePersonService.findByEmail(context, admin.getEmail());
        assertEquals(importedItem.getSubmitter(), eperson);

        context.turnOffAuthorisationSystem();
        itemService.delete(context, itemService.find(context, importedItem.getID()));
        context.restoreAuthSystemState();
    }

    @Test(expected = ParseException.class)
    public void metadataImportWithoutEPersonParameterTest()
        throws IllegalAccessException, InstantiationException, ParseException {
        String fileLocation = new File(testProps.get("test.importcsv").toString()).getAbsolutePath();
        String[] args = new String[] {"metadata-import", "-f", fileLocation, "-s"};
        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        ScriptService scriptService = ScriptServiceFactory.getInstance().getScriptService();
        ScriptConfiguration scriptConfiguration = scriptService.getScriptConfiguration(args[0]);

        DSpaceRunnable script = null;
        if (scriptConfiguration != null) {
            script = scriptService.createDSpaceRunnableForScriptConfiguration(scriptConfiguration);
        }
        if (script != null) {
            if (DSpaceRunnable.StepResult.Continue.equals(script.initialize(args, testDSpaceRunnableHandler, null))) {
                script.run();
            }
        }
    }

    @Test
    public void relationshipMetadataImportTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, publicationCollection)
                               .withTitle("Publication1").build();
        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType person = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publication, person, "isAuthorOfPublication",
                                                              "isPublicationOfAuthor", 0, 10, 0, 10);
        context.restoreAuthSystemState();

        String[] csv = {"id,collection,dc.title,relation.isPublicationOfAuthor,dspace.entity.type",
            "+," + personCollection.getHandle() + ",\"Test Import 1\"," + item.getID() + ",Person"};
        performImportScript(csv);
        Item importedItem = findItemByName("Test Import 1");


        assertEquals(1, relationshipService.findByItem(context, importedItem).size());
        context.turnOffAuthorisationSystem();
        itemService.delete(context, itemService.find(context, importedItem.getID()));
        context.restoreAuthSystemState();
    }

    @Test
    public void relationshipMetadataImporAlreadyExistingItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item personItem = ItemBuilder.createItem(context, personCollection)
                                     .withTitle("Person1").build();
        List<Relationship> relationshipList = relationshipService.findByItem(context, personItem);
        assertEquals(0, relationshipList.size());
        Item publicationItem = ItemBuilder.createItem(context, publicationCollection)
                                          .withTitle("Publication1").build();

        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType person = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publication, person, "isAuthorOfPublication",
                                                              "isPublicationOfAuthor", 0, 10, 0, 10);
        context.restoreAuthSystemState();


        String[] csv = {"id,collection,relation.isPublicationOfAuthor",
            personItem.getID() + "," + publicationCollection.getHandle() + "," + publicationItem.getID()};
        performImportScript(csv);
        Item importedItem = findItemByName("Person1");


        assertEquals(1, relationshipService.findByItem(context, importedItem).size());

    }

    @Test
    public void personMetadataImportTest() throws Exception {

        String[] csv = {"id,collection,dc.title,person.birthDate",
            "+," + publicationCollection.getHandle() + ",\"Test Import 2\"," + "2000"};
        performImportScript(csv);
        Item importedItem = findItemByName("Test Import 2");
        assertTrue(
            Strings.CS.equals(
                itemService.getMetadata(importedItem, "person", "birthDate", null, Item.ANY)
                           .get(0).getValue(), "2000"));
        context.turnOffAuthorisationSystem();
        itemService.delete(context, importedItem);
        context.restoreAuthSystemState();
    }

    @Test
    public void metadataImportRemovingValueTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String itemTitle = "Testing removing author";
        Item item = ItemBuilder.createItem(context,personCollection).withAuthor("TestAuthorToRemove")
                               .withTitle(itemTitle)
                               .build();
        context.restoreAuthSystemState();

        assertTrue(
            Strings.CS.equals(
                itemService.getMetadata(item, "dc", "contributor", "author", Item.ANY).get(0).getValue(),
                "TestAuthorToRemove"));

        String[] csv = {"id,collection,dc.title,dc.contributor.author",
            item.getID().toString() + "," + personCollection.getHandle() + "," + item.getName() + ","};
        performImportScript(csv);
        item = findItemByName(itemTitle);
        assertEquals(0, itemService.getMetadata(item, "dc", "contributor", "author", Item.ANY).size());
    }

    @Test
    public void metadataImportWithAuthorityContainingSeparatorTest() throws Exception {
        // Test case that reproduces the bug: authority values containing the separator (::)
        // This test verifies that CSV import correctly handles authorities like:
        // "will be referenced::ORCID::0000-0002-5474-1918"
        String[] csv = {
            "id,collection,dc.title,dc.contributor.author",
            "+," + collection.getHandle() + ",\"Test Import Authority\"," +
            "\"Fischer, Frank::will be referenced::ORCID::0000-0002-5474-1918::600\""
        };
        performImportScript(csv);
        Item importedItem = findItemByName("Test Import Authority");
        List<MetadataValue> authorMetadata =
            itemService.getMetadata(importedItem, "dc", "contributor", "author", Item.ANY);

        assertEquals("Should have exactly one author metadata value", 1, authorMetadata.size());

        MetadataValue authorValue = authorMetadata.get(0);
        assertEquals("Value should be correctly parsed", "Fischer, Frank", authorValue.getValue());
        assertEquals("Authority should contain full authority string with separators",
                     "will be referenced::ORCID::0000-0002-5474-1918", authorValue.getAuthority());
        assertEquals("Confidence should be correctly parsed", 600, authorValue.getConfidence());

        context.turnOffAuthorisationSystem();
        itemService.delete(context, itemService.find(context, importedItem.getID()));
        context.restoreAuthSystemState();
    }

    @Test
    public void metadataImportWithRORAuthorityContainingSeparatorTest() throws Exception {
        // Test case with ROR-ID authority containing separators
        String[] csv = {
            "id,collection,dc.title,dc.contributor.other",
            "+," + collection.getHandle() + ",\"Test Import ROR\"," +
            "\"Chemnitz University of Technology::will be referenced::ROR-ID::https://ror.org/00a208s56::600\""
        };
        performImportScript(csv);
        Item importedItem = findItemByName("Test Import ROR");
        List<MetadataValue> otherMetadata =
            itemService.getMetadata(importedItem, "dc", "contributor", "other", Item.ANY);

        assertEquals("Should have exactly one contributor.other metadata value", 1, otherMetadata.size());

        MetadataValue otherValue = otherMetadata.get(0);
        assertEquals("Value should be correctly parsed", "Chemnitz University of Technology",
                     otherValue.getValue());
        assertEquals("Authority should contain full authority string with separators",
                     "will be referenced::ROR-ID::https://ror.org/00a208s56", otherValue.getAuthority());
        assertEquals("Confidence should be correctly parsed", 600, otherValue.getConfidence());

        context.turnOffAuthorisationSystem();
        itemService.delete(context, itemService.find(context, importedItem.getID()));
        context.restoreAuthSystemState();
    }

    @Test
    public void metadataImportWithTwoPartAuthorityTest() throws Exception {
        // Test case with 2-part format (value::authority, no confidence)
        String[] csv = {
            "id,collection,dc.title,dc.contributor.author",
            "+," + collection.getHandle() + ",\"Test Import 2Part\"," +
            "\"Smith, John::will be referenced::ORCID::0000-0001-2345-6789\""
        };
        performImportScript(csv);
        Item importedItem = findItemByName("Test Import 2Part");
        List<MetadataValue> authorMetadata =
            itemService.getMetadata(importedItem, "dc", "contributor", "author", Item.ANY);

        assertEquals("Should have exactly one author metadata value", 1, authorMetadata.size());

        MetadataValue authorValue = authorMetadata.get(0);
        assertEquals("Value should be correctly parsed", "Smith, John", authorValue.getValue());
        assertEquals("Authority should contain full authority string",
                     "will be referenced::ORCID::0000-0001-2345-6789", authorValue.getAuthority());
        // When no confidence is provided, should default to CF_ACCEPTED (600)
        assertEquals("Confidence should default to CF_ACCEPTED", 600, authorValue.getConfidence());

        context.turnOffAuthorisationSystem();
        itemService.delete(context, itemService.find(context, importedItem.getID()));
        context.restoreAuthSystemState();
    }

    private Item findItemByName(String name) throws Exception {
        List<Item> items =
            IteratorUtils.toList(itemService.findByMetadataField(context, "dc", "title", null, name));

        if (items != null && !items.isEmpty()) {
            // Just return first matching Item. Tests should ensure name/title is unique.
            return items.get(0);
        } else {
            fail("Could not find expected Item with dc.title = '" + name + "'");
            return null;
        }
    }

    public void performImportScript(String[] csv) throws Exception {
        performImportScript(csv, false);
    }

    /**
     * Import mocked CSVs to test item creation behavior, deleting temporary file afterward.
     * @param csv content for test file.
     * @throws java.lang.Exception passed through.
     */
    public void performImportScript(String[] csv, boolean useTemplate) throws Exception {
        File csvFile = File.createTempFile("dspace-test-import", "csv");
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile), "UTF-8"));
        for (String csvLine : csv) {
            out.write(csvLine + "\n");
        }
        out.flush();
        out.close();
        String fileLocation = csvFile.getAbsolutePath();
        try {
            String[] args = new String[] {"metadata-import", "-f", fileLocation, "-e", admin.getEmail(), "-s"};
            if (useTemplate) {
                args = ArrayUtils.add(args, "-t");
            }
            TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
            ScriptLauncher
                .handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);
        } finally {
            csvFile.delete();
        }
    }
}
