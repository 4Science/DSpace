/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.marcxml2item;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.dspace.app.marcxml2item.XmlToItemImportScript.XML_TO_ITEM_SCRIPT_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for {@link XmlToItemImportScript}
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 **/
public class XmlToItemImportScriptIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_XLS_DIR_PATH = "./target/testing/dspace/assetstore/xml2itemimport";

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

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
                "dc.date.modified : 19930521155141.9",
                "dc.description.notes : One Mylar sheet included in pocket."
        );

        String fileLocation = getXmlFilePath("marc-xml-example.xml");
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
        for (MetadataValue metadataValue : metadataValues) {
            System.out.println(metadataValue.getMetadataField().toString('.') + " : " + metadataValue.getValue());
        }

        assertEquals(25, metadataValues.size());

        for (MetadataValue metadataValue : metadataValues) {
            var field = metadataValue.getMetadataField().toString('.');
            var value = metadataValue.getValue();
            if (isTecnicalMetadata(field)) {
                continue;
            }
            assertTrue(expectedMetadata.contains(field + " : " + value));
        }

        assertFalse("There should be only one imported item, but more were found.", items.hasNext());
    }

    private boolean isTecnicalMetadata(String field) {
        List<String> tecnicalMetadata = List.of("dc.date.accessioned",
                                                "dc.date.available",
                                                "dc.identifier.uri",
                                                "dc.description.provenance"
                                                );
        return tecnicalMetadata.contains(field);
    }

    private String getXmlFilePath(String name) {
        return new File(BASE_XLS_DIR_PATH, name).getAbsolutePath();
    }

}
