/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.marcxml2item;

import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.dspace.app.marcxml2item.XmlToItemImportScript.XML_TO_ITEM_SCRIPT_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link XmlToItemImportScript}
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 **/
public class XmlToItemImportScriptIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_XLS_DIR_PATH = "./target/testing/dspace/assetstore/xml2itemimport";

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

    private Collection publicationCol;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        this.publicationCol = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withEntityType("Publication")
                                               .withName("Publication Collection")
                                               .build();
        context.restoreAuthSystemState();
    }

    @Test
    public void importSingleItemFromXmlTest() throws Exception {
        Set<String> expectedMetadata = Set.of(
                "dc.contributor.author : Rand, Ted,",
                "dc.date.issued : c1993.",
                "dc.identifier.isbn : 0152038655",
                "dc.description.abstract : A poem about numbers and their characteristics. Features anamorphic, or " +
                        "distorted, drawings which can be restored to normal by viewing from a particular angle or " +
                        "by viewing the image's reflection in the provided Mylar cone.",
                "dc.format.extent : 1 v. (unpaged)",
                "dc.publisher : Harcourt Brace Jovanovich,",
                "dc.publisher.place : San Diego",
                "dc.subject : Arithmetic",
                "dc.subject : American poetry.",
                "dc.subject : Visual perception.",
                "dc.subject.ddc : 811/.52",
                "dc.subject.lcc : PS3537.A618",
                "dc.subject.lcsh : Arithmetic",
                "dc.subject.lcsh : Children's poetry, American.",
                "dc.title : Arithmetic",
                "dc.description.edition : 1st ed.",
                "dc.relation.edition : 1st ed.",
                "cris.legacyId : 92005291",
                "dspace.entity.type : Publication",
                "dc.date.modified : 1993-05-21",
                "dc.description.notes : One Mylar sheet included in pocket."
        );

        String fileLocation = getXmlFilePath(BASE_XLS_DIR_PATH,"marc-xml-example.xml");
        String[] args = new String[]{ XML_TO_ITEM_SCRIPT_NAME, "-c", publicationCol.getID().toString(),
                                                               "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertEquals(0, status);
        assertThat(handler.getErrorMessages(), empty());

        Iterator<Item> items = itemService.findAll(context);
        assertTrue("Expected at least one item in the collection after import.", items.hasNext());
        Item importedItem = items.next();
        List<MetadataValue> metadataValues = importedItem.getMetadata();
        assertEquals(25, metadataValues.size());

        checkMetadata(metadataValues, expectedMetadata);
        assertFalse("There should be only one imported item, but more were found.", items.hasNext());
    }

    @Test
    public void importMultipleItemsFromXmlTest() throws Exception {
        Set<String> expectedMetadataItem1 = Set.of(
            "dc.contributor.author : Oberli, D. Y.",
            "dc.contributor.author : Byszewski, M.",
            "dc.contributor.author : Chalupar, B.",
            "dc.contributor.author : Pelucchi, E.",
            "dc.contributor.author : Rudra, A.",
            "dc.contributor.author : Kapon, E.",
            "dc.date.issued : 2009",
            "dc.identifier.doi : 10.1103/PhysRevB.80.165312",
            "dc.identifier.isi : WOS:000271352100087",
            "dc.description.abstract : The emission pattern of charged excitons in a semiconductor quantum dot (QD).",
            "dc.subject : Fine Structure Splitting",
            "dc.subject : Spin",
            "dc.subject : Excitons",
            "dc.subject : Semiconductor nanostructures",
            "dc.title : Coulomb correlations of charged excitons in semiconductor quantum dots",
            "cris.legacyId : 147853",
            "dspace.entity.type : Publication",
            "dc.date.modified : 2023-05-05"
            );

        Set<String> expectedMetadataItem2 = Set.of(
                "dc.contributor.author : Clua Longas, Angela",
                "dc.contributor.author : Rey, Emmanuel",
                "dc.date.issued : 2017-09-01",
                "dc.format : BIPV panels",
                "dc.format : BIPV panel fastening system",
                "dc.format : Fibrociment panels",
                "dc.format : Blown cellulose insultation",
                "dc.format : Timber substructure",
                "dc.format : OSB panels",
                "dc.format : Wood fiber insulation",
                "dc.format : Plaster board",
                "dc.format : Window façade module",
                "dc.format : Translucent BIPV panel (balustrade)",
                "dc.title : Advanced Active Facade - Demonstrator",
                "dc.contributor.corporatebody : Swiss Centre for Electronics and Microtechnology (CSEM)",
                "dc.contributor.corporatebody : H.Glass",
                "cris.legacyId : 293489",
                "dspace.entity.type : Publication",
                "dc.date.modified : 2023-05-06"
                );

        String fileLocation = getXmlFilePath(BASE_XLS_DIR_PATH,"marc-xml-multiple-records.xml");
        String[] args = new String[]{ XML_TO_ITEM_SCRIPT_NAME, "-c", publicationCol.getID().toString(),
                                                               "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertEquals(0, status);
        assertThat(handler.getErrorMessages(), empty());


        Iterator<Item> items = itemService.findAll(context);
        assertTrue("Expected first item in the collection after import.", items.hasNext());
        Item tempFirstImportedItem = items.next();
        assertTrue("Expected second item in the collection after import.", items.hasNext());
        Item tempSecondImportedItem = items.next();
        assertFalse("There should be only two imported items, but more were found.", items.hasNext());

        Item firstImportedItem;
        Item secondImportedItem;
        var legacyId = itemService.getMetadataFirstValue(tempFirstImportedItem, "cris", "legacyId", null, Item.ANY);
        if (StringUtils.equals("147853", legacyId)) {
            firstImportedItem = tempFirstImportedItem;
            secondImportedItem = tempSecondImportedItem;
        } else {
            firstImportedItem = tempSecondImportedItem;
            secondImportedItem = tempFirstImportedItem;
        }

        // check first item
        List<MetadataValue> firstItemMetadataValues = firstImportedItem.getMetadata();
        assertEquals(22, firstItemMetadataValues.size());
        checkMetadata(firstItemMetadataValues, expectedMetadataItem1);
        // check second item
        List<MetadataValue> secondItemMetadataValues = secondImportedItem.getMetadata();
        assertEquals(23, secondItemMetadataValues.size());
        checkMetadata(secondItemMetadataValues, expectedMetadataItem2);
    }

    @Test
    public void valitadeBadXmlFileTest() throws Exception {
        var dir = "./target/testing/dspace/assetstore/scopusFilesForTests";
        String fileLocation = getXmlFilePath(dir, "scopusMetrics.xml");
        String[] args = new String[]{ XML_TO_ITEM_SCRIPT_NAME, "-c", publicationCol.getID().toString(),
                                                               "-f", fileLocation,
                                                               "-v" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertEquals(0, status);
        assertTrue("Contains 2 error messages", handler.getErrorMessages().size() == 2);
        var error1 = "Marc XML validation failed with error: cvc-elt.1.a: " +
                     "Cannot find the declaration of element 'search-results'.";
        var error2 = "IllegalArgumentException: The XML file is not well-formed or valid";
        assertEquals(handler.getErrorMessages().get(0), error1);
        assertEquals(handler.getErrorMessages().get(1), error2);

        Iterator<Item> items = itemService.findAll(context);
        assertFalse("Expected zero item!", items.hasNext());
    }

    @Test
    public void valitadeXmlWithoutTitleTest() throws Exception {
        String fileLocation = getXmlFilePath(BASE_XLS_DIR_PATH, "xml-without-title.xml");
        String[] args = new String[]{ XML_TO_ITEM_SCRIPT_NAME, "-c", publicationCol.getID().toString(),
                                                               "-f", fileLocation,
                                                               "-v" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertEquals(0, status);
        assertTrue("Contains 2 error messages", handler.getErrorMessages().size() == 2);
        var error1 = "Required field: ./datafield[@tag = '245'] not found in record";
        var error2 = "IllegalArgumentException: The XML file is not well-formed or valid";
        assertEquals(handler.getErrorMessages().get(0), error1);
        assertEquals(handler.getErrorMessages().get(1), error2);

        Iterator<Item> items = itemService.findAll(context);
        assertFalse("Expected zero item!", items.hasNext());
    }

    @Test
    public void importSingleItemFromXmlWithJournalFondsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection journalFondsCollection = CollectionBuilder.createCollection(context, parentCommunity)
                                                             .withEntityType("JournalFonds")
                                                             .withName("JournalFonds Collection")
                                                             .build();

        Item journalFonds1 = ItemBuilder.createItem(context, journalFondsCollection)
                                        .withTitle("JournalFonds item 1")
                                        .withIssnIdentifier("3035-3467")
                                        .build();

        Collection publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
                                                            .withEntityType("Publication")
                                                            .withName("Publication Collection TEST")
                                                            .build();

        context.restoreAuthSystemState();
        Set<String> expectedMetadata = Set.of(
            "dc.contributor.author : Rand, Ted,",
            "dc.contributor.author : Smith, Jhon,",
            "dc.date.issued : c1993.",
            "dc.identifier.isbn : 0152038655 :",
            "dc.identifier.issn : 3035-3467",
            "dc.identifier.ismn : M571100511",
            "dc.identifier.ismn : M571100512",
            "dc.description.abstract : A poem about numbers and their characteristics.",
            "dc.description.tableofcontents : pt. 1. Carbon -- pt. 2. Nitrogen -- pt. 3. Sulphur -- pt. 4. Metals.",
            "dc.format.extent : 1 v. (unpaged) :",
            "dc.language.iso : it",
            "dc.publisher : Harcourt Brace Jovanovich,",
            "dc.publisher.place : San Diego :",
            "dc.relation.ispartof : West Virginia University bulletin ;; no. 35",
            "dc.relation.hasversion : Communist",
            "dc.relation.isreferencedby : Algae abstracts, v. 3, W73-11952",
            "dc.relation.conference : Conference on Philosophy and Its History",
            "dc.rights : Restricted: Information on reproduction rights available at Reference Desk. CC BY-NC-ND 4.0",
            "dc.subject : Arithmetic",
            "dc.subject : American poetry.",
            "dc.subject : Visual perception.",
            "dc.subject.ddc : 811/.52",
            "dc.subject.lcc : PS3537.A618",
            "dc.subject.lcsh : Arithmetic",
            "dc.subject.lcsh : Children's poetry, American.",
            "dc.subject.other : TEST.811/1052",
            "dc.title : Arithmetic /",
            "dc.title.alternative : Democrazia e definizioni : esempi pratici",
            "dc.relation.isbn : 9781234567890",
            "dc.relation.issn : 3035-3467",
            "dc.relation.references : George Orwell's Animal farm /",
            "dc.description.edition : 1st ed.",
            "dc.subject.rvm : Boissons alcoolisées – Fiscalité – Droit et législation",
            "dc.relation.place : Cali, Colombia)",
            "dc.identifier.oclc : 1276909548",
            "dc.relation.journalfonds : Pacific news.",
            "dc.description.bibliography : Bibliography: p. 238-239.",
            "dc.relation.stpieceperother : no. 35",
            "dc.relation.edition : 1st ed.",
            "cris.legacyId : 92005291",
            "oaire.citation.volume : no. 35",
            "dspace.entity.type : Publication",
            "glamjournalfonds.parent : Pacific news.",
            "dc.date.modified : 1993-05-21",
            "dc.description.notes : One Mylar sheet included in pocket."
        );

        String fileLocation = getXmlFilePath(BASE_XLS_DIR_PATH,"xml-with-JournalFonds.xml");
        String[] args = new String[]{ XML_TO_ITEM_SCRIPT_NAME, "-c", publicationCollection.getID().toString(),
                                                               "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertEquals(0, status);
        assertThat(handler.getErrorMessages(), empty());

        Iterator<Item> items = itemService.findByCollection(context, publicationCollection);
        assertTrue("Expected at least one item in the collection after import.", items.hasNext());
        Item importedItem = items.next();
        List<MetadataValue> metadataValues = importedItem.getMetadata();
        assertThat(metadataValues.size(), is(48));

        checkMetadata(metadataValues, expectedMetadata);
        // verify that metadataValues dc.relation.journalfonds & glamjournalfonds.parent has authority
        MetadataValue journalfonds =  findMetadata(metadataValues, "dc.relation.journalfonds");
        assertThat(journalfonds.getMetadataField().toString('.'), is("dc.relation.journalfonds"));
        assertThat(journalfonds.getAuthority(), is(journalFonds1.getID().toString()));
        assertThat(journalfonds.getValue(), is("Pacific news."));

        MetadataValue glamjournalfonds =  findMetadata(metadataValues, "glamjournalfonds.parent");
        assertThat(glamjournalfonds.getMetadataField().toString('.'), is("glamjournalfonds.parent"));
        assertThat(glamjournalfonds.getAuthority(), is(journalFonds1.getID().toString()));
        assertThat(glamjournalfonds.getValue(), is("Pacific news."));

        assertFalse("There should be only one imported item, but more were found.", items.hasNext());
    }

    private MetadataValue findMetadata(List<MetadataValue> metadataValues, String s) {
        return metadataValues.stream()
                             .filter(m -> StringUtils.equals(m.getMetadataField().toString('.'), s))
                             .findFirst()
                             .orElse(null);
    }

    private void checkMetadata(List<MetadataValue> metadataValues, Set<String> expectedItemMetadata) {
        for (MetadataValue metadataValue : metadataValues) {
            var field = metadataValue.getMetadataField().toString('.');
            var value = metadataValue.getValue();
            if (isTecnicalMetadata(field)) {
                continue;
            }
            var valueToCheck = field + " : " + value;
            assertTrue("Missing value:" + valueToCheck, expectedItemMetadata.contains(valueToCheck));
        }
    }

    private boolean isTecnicalMetadata(String field) {
        List<String> tecnicalMetadata = List.of("dc.date.accessioned",
                                                "dc.date.available",
                                                "dc.identifier.uri",
                                                "dc.description.provenance"
                                                );
        return tecnicalMetadata.contains(field);
    }

    private String getXmlFilePath(String dir, String name) {
        return new File(dir, name).getAbsolutePath();
    }

}
