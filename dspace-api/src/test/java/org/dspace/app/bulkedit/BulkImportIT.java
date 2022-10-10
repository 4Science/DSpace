/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static java.lang.String.join;
import static java.util.regex.Pattern.compile;
import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.dspace.builder.ItemBuilder.createItem;
import static org.dspace.builder.WorkspaceItemBuilder.createWorkspaceItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.authority.CrisConsumer;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.CrisConstants;
import org.dspace.event.factory.EventServiceFactory;
import org.dspace.event.service.EventService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link BulkImport}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class BulkImportIT extends AbstractIntegrationTestWithDatabase {

    private static final String CRIS_CONSUMER = CrisConsumer.CONSUMER_NAME;

    private static final String BASE_XLS_DIR_PATH = "./target/testing/dspace/assetstore/bulk-import/";

    private static final String PLACEHOLDER = CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

    private static final Pattern UUID_PATTERN = compile(
        "[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}");

    private static final String BITSTREAM_METADATA = "bitstream-metadata";

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

    private BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();

    private WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();

    private Community community;

    private Collection collection;

    private static Set<String> temporaryFiles = new HashSet<>();

    @Before
    public void beforeTests() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community).withAdminGroup(eperson).build();
        context.restoreAuthSystemState();
    }

    @Test
    public void testEmptyImport() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFilePath("empty.xls");
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("The sheet Main Entity of the Workbook is empty"));
    }

    @Test
    public void testEmptyHeadersImport() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFilePath("empty-headers.xls");
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("The header of sheet Main Entity of the Workbook is empty"));
    }

    @Test
    public void testOneHeaderEmptyImport() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFilePath("one-header-empty.xls");
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("The following metadata fields of the sheet named "
            + "'Main Entity' are invalid:[Empty metadata]"));
    }

    @Test
    public void testWithoutHeadersImport() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFilePath("without-headers.xls");
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("Wrong ID header on sheet Main Entity: RID::123456789"));
    }

    @Test
    public void testInvalidHeadersImport() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFilePath("invalid-headers.xls");
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("The following metadata fields of the sheet named "
            + "'Main Entity' are invalid:[unknown is not valid for the given collection, "
            + "person.identifier is not valid for the given collection]"));
    }

    @Test
    public void testInvalidSheetNameImport() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFilePath("invalid-sheet-name.xlsx");
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("The sheet name wrongdc.contributor.author "
            + "is not a valid metadata group"));
    }

    @Test
    public void testMetadataGroupRowWithManyValuesImport() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("metadata-group-row-with-many-values.xlsx");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> warningMessages = handler.getWarningMessages();
        assertThat("Expected 2 warning messages", warningMessages, hasSize(2));
        assertThat(warningMessages.get(0), containsString("Row 2 - Invalid item left in workspace"));
        assertThat(warningMessages.get(1), containsString("Row 3 - Invalid item left in workspace"));

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("Multiple metadata value on the same cell not allowed in the "
            + "metadata group sheets: Author1 || Author2"));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 3 info message", infoMessages, hasSize(3));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 1 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 2 items to process"));
    }

    @Test
    public void testHeadersDuplicatedImport() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("headers-duplicated.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("Sheet Main Entity - Duplicated headers found "
            + "on cells 3 and 4"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreatePatent() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("create-patent.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());

        List<String> warningMessages = handler.getWarningMessages();
        assertThat("Expected 1 warning message", warningMessages, hasSize(1));
        assertThat(warningMessages.get(0), containsString("Row 2 - Invalid item left in workspace"));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 3 info messages", infoMessages, hasSize(3));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));

        String id = getItemUuidFromMessage(warningMessages.get(0));
        Item createdItem = itemService.findByIdOrLegacyId(context, id);
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(false));
        assertThat(findWorkspaceItem(createdItem), notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Luca Stone", 1)));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", 2)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher")));
        assertThat(metadata, hasItems(with("dc.type", "Patent")));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdatePatent() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .build();

        Item patentToUpdate = createItem(context, patents)
            .withTitle("Patent to update")
            .withAuthor("Luca G.")
            .withIsniIdentifier("54321")
            .withPatentNo("888-444-333")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("update-patent.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 4 info messages", infoMessages, hasSize(4));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item updated successfully"));

        Item updatedItem = itemService.find(context, patentToUpdate.getID());

        List<MetadataValue> metadata = updatedItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", 1)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher")));
        assertThat(metadata, not(hasItems(with("dc.identifier.patentno", "888-444-333"))));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreatePublicationWithAuthority() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("create-publication-with-authority.xlsx");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());

        List<String> warningMessages = handler.getWarningMessages();
        assertThat("Expected 1 warning message", warningMessages, hasSize(1));
        assertThat(warningMessages.get(0), containsString("Row 2 - Invalid item left in workspace"));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 3 info messages", infoMessages, hasSize(3));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 2 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));

        String id = getItemUuidFromMessage(warningMessages.get(0));
        Item createdItem = itemService.findByIdOrLegacyId(context, id);
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(false));
        assertThat(findWorkspaceItem(createdItem), notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.contributor.author", "Author1", null, "authority1", 0, 600)));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Author2", 1)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", "OrgUnit1")));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", "OrgUnit2", null, "authority2", 1, 400)));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testManyPublicationImport() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item itemToUpdateByRid = createItem(context, publications)
            .withTitle("My Publication")
            .withTitleForLanguage("My Publication English", "en")
            .withResearcherIdentifier("123456789")
            .build();

        Item itemToDelete = createItem(context, publications).withDoiIdentifier("10.1000/182").build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("many-publications.xls");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());

        List<String> warningMessages = handler.getWarningMessages();
        assertThat("Expected 1 warning message", warningMessages, hasSize(1));
        assertThat(warningMessages.get(0), containsString("Row 2 - Invalid item left in workspace"));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 5 info messages", infoMessages, hasSize(5));

        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 6 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 3 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 3 - Item updated successfully"));
        assertThat(infoMessages.get(4), containsString("Row 4 - Item deleted successfully"));

        // verify created item (ROW 2)
        String createdItemId = getItemUuidFromMessage(warningMessages.get(0));
        Item createdItem = itemService.findByIdOrLegacyId(context, createdItemId);
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(false));
        assertThat(findWorkspaceItem(createdItem), notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "First publication")));
        assertThat(metadata, hasItems(with("dc.title", "First publication English", "en", null, 1, -1)));
        assertThat(metadata, hasItems(with("dc.date.issued", "12/12/65")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Thomas Edison")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Morgan Pitt", 1)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", "Company")));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", PLACEHOLDER, 1)));
        assertThat(metadata, hasItems(with("dc.contributor.editor", "Editor")));
        assertThat(metadata, hasItems(with("oairecerif.editor.affiliation", "EditorAffiliation")));
        assertThat(metadata, hasItems(with("dc.identifier.doi", "10.1000/183")));
        assertThat(metadata, hasItems(with("dc.type", "Article")));

        // verify updated item (ROW 3)
        Item itemUpdated = itemService.find(context, itemToUpdateByRid.getID());
        metadata = itemUpdated.getMetadata();

        assertThat(metadata, hasItems(with("dc.title", "Publication")));
        assertThat(metadata, hasItems(with("dc.title", "Publication English", "en", null, 1, -1)));
        assertThat(metadata, hasItems(with("dc.title", "English Publication", "en", null, 2, -1)));
        assertThat(metadata, hasItems(with("dc.date.issued", "01/07/95")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "John Smith")));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", PLACEHOLDER)));
        assertThat(metadata, hasItems(with("dc.identifier.doi", "10.1000/184")));
        assertThat(metadata, hasItems(with("dc.relation.project", "Test Project")));
        assertThat(metadata, hasItems(with("dc.relation.grantno", "1")));
        assertThat(metadata, hasItems(with("dc.relation.project", "Another Test Project", 1)));
        assertThat(metadata, hasItems(with("dc.relation.grantno", PLACEHOLDER, 1)));
        assertThat(metadata, hasItems(with("dc.type", "Book")));

        // verify deleted item (ROW 4)
        assertThat("Item expected to be deleted", itemService.find(context, itemToDelete.getID()), nullValue());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testManyPublicationImportWithErrorAndNotAbortOnError() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item itemToDelete = createItem(context, publications).withDoiIdentifier("10.1000/182").build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("many-publications.xls");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("No item to update found for entity with id RID::123456789"));

        List<String> warningMessages = handler.getWarningMessages();
        assertThat("Expected 1 warning message", warningMessages, hasSize(1));
        assertThat(warningMessages.get(0), containsString("Row 2 - Invalid item left in workspace"));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 4 info messages", infoMessages, hasSize(4));

        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 6 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 3 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 4 - Item deleted successfully"));

        // verify created item (ROW 2)
        String createdItemId = getItemUuidFromMessage(warningMessages.get(0));
        Item createdItem = itemService.findByIdOrLegacyId(context, createdItemId);
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(false));
        assertThat(findWorkspaceItem(createdItem), notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "First publication")));
        assertThat(metadata, hasItems(with("dc.title", "First publication English", "en", null, 1, -1)));
        assertThat(metadata, hasItems(with("dc.date.issued", "12/12/65")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Thomas Edison")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Morgan Pitt", 1)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", "Company")));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", PLACEHOLDER, 1)));
        assertThat(metadata, hasItems(with("dc.contributor.editor", "Editor")));
        assertThat(metadata, hasItems(with("oairecerif.editor.affiliation", "EditorAffiliation")));
        assertThat(metadata, hasItems(with("dc.identifier.doi", "10.1000/183")));

        // verify deleted item (ROW 4)
        assertThat("Item expected to be deleted", itemService.find(context, itemToDelete.getID()), nullValue());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testManyPublicationImportWithErrorAndAbortOnError() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item itemToDelete = createItem(context, publications).withDoiIdentifier("10.1000/182").build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("many-publications.xls");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation, "-e" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("No item to update found for entity with id RID::123456789"));

        List<String> warningMessages = handler.getWarningMessages();
        assertThat("Expected 1 warning message", warningMessages, hasSize(1));
        assertThat(warningMessages.get(0), containsString("Row 2 - Invalid item left in workspace"));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 3 info messages", infoMessages, hasSize(3));

        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 6 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 3 items to process"));

        // verify created item (ROW 2)
        String createdItemId = getItemUuidFromMessage(warningMessages.get(0));
        Item createdItem = itemService.findByIdOrLegacyId(context, createdItemId);
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(false));
        assertThat(findWorkspaceItem(createdItem), notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "First publication")));
        assertThat(metadata, hasItems(with("dc.title", "First publication English", "en", null, 1, -1)));
        assertThat(metadata, hasItems(with("dc.date.issued", "12/12/65")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Thomas Edison")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Morgan Pitt", 1)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", "Company")));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", PLACEHOLDER, 1)));
        assertThat(metadata, hasItems(with("dc.contributor.editor", "Editor")));
        assertThat(metadata, hasItems(with("oairecerif.editor.affiliation", "EditorAffiliation")));
        assertThat(metadata, hasItems(with("dc.identifier.doi", "10.1000/183")));

        // verify deleted item (ROW 4)
        assertThat("Item expected not to be deleted", itemService.find(context, itemToDelete.getID()), notNullValue());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreatePublicationWithOneInvalidAuthorityAndNoAbortOnError() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("create-publication-with-one-invalid-authority.xlsx");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("Sheet dc.contributor.author - Row 2 - Invalid metadata "
            + "value Author1$$authority1$$xxx: invalid confidence value xxx"));

        List<String> warningMessages = handler.getWarningMessages();
        assertThat("Expected 3 warning messages", warningMessages, hasSize(3));
        assertThat(warningMessages.get(0), containsString("Row 2 - Invalid item left in workspace"));
        assertThat(warningMessages.get(1), containsString("Row 3 - Invalid item left in workspace"));
        assertThat(warningMessages.get(2), containsString("Row 4 - Invalid item left in workspace"));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 3 info messages", infoMessages, hasSize(3));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 1 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 3 items to process"));

        String id = getItemUuidFromMessage(warningMessages.get(1));
        Item createdItem = itemService.findByIdOrLegacyId(context, id);
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(false));
        assertThat(findWorkspaceItem(createdItem), notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.contributor.author", "Author2")));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", "OrgUnit2", null, "authority2", 0, 400)));

    }

    @Test
    public void testCreatePublicationWithOneInvalidAuthorityAndAbortOnError() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("create-publication-with-one-invalid-authority.xlsx");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation, "-e" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("Sheet dc.contributor.author - Row 2 - Invalid metadata "
            + "value Author1$$authority1$$xxx: invalid confidence value xxx"));

        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 1 info messages", infoMessages, hasSize(1));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreatePublicationWithWillBeGeneratedAuthority() throws Exception {
        String[] defaultConsumers = activateCrisConsumer();
        try {

            context.turnOffAuthorisationSystem();

            Item person = ItemBuilder.createItem(context, collection)
                .withEntityType("Person")
                .withTitle("Walter White")
                .withOrcidIdentifier("0000-0002-9079-593X")
                .build();

            Collection publications = createCollection(context, community)
                .withSubmissionDefinition("publication")
                .withAdminGroup(eperson)
                .build();

            context.commit();
            context.restoreAuthSystemState();

            String publicationCollectionId = publications.getID().toString();
            String fileLocation = getXlsFilePath("create-publication-with-will-be-generated-authority.xls");
            String[] args = new String[] { "bulk-import", "-c", publicationCollectionId, "-f", fileLocation, "-e" };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
            assertThat("Expected no errors", handler.getErrorMessages(), empty());
            assertThat("Expected no warnings", handler.getWarningMessages(), empty());

            List<String> infoMessages = handler.getInfoMessages();
            assertThat("Expected 4 info messages", infoMessages, hasSize(4));

            assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
            assertThat(infoMessages.get(1), containsString("Found 1 metadata groups to process"));
            assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
            assertThat(infoMessages.get(3), containsString("Row 2 - Item archived successfully"));

            // verify created item (ROW 2)
            String createdItemId = getItemUuidFromMessage(infoMessages.get(3));

            Item createdItem = itemService.findByIdOrLegacyId(context, createdItemId);
            assertThat("Item expected to be created", createdItem, notNullValue());

            String personId = person.getID().toString();

            List<MetadataValue> metadata = createdItem.getMetadata();
            assertThat(metadata, hasItems(with("dc.contributor.author", "Walter White", null, personId, 0, 600)));
            assertThat(metadata, hasItems(with("dc.title", "Wonderful Publication")));

        } finally {
            resetConsumers(defaultConsumers);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreatePublicationWithWillBeGeneratedAuthorityAndNoRelatedItemFound() throws Exception {
        String[] defaultConsumers = activateCrisConsumer();
        try {

            context.turnOffAuthorisationSystem();

            createCollection(context, community)
                .withEntityType("Person")
                .withAdminGroup(eperson)
                .build();

            Collection publications = createCollection(context, community)
                .withSubmissionDefinition("publication")
                .withAdminGroup(eperson)
                .build();

            context.commit();
            context.restoreAuthSystemState();

            String publicationCollectionId = publications.getID().toString();
            String fileLocation = getXlsFilePath("create-publication-with-will-be-generated-authority.xls");
            String[] args = new String[] { "bulk-import", "-c", publicationCollectionId, "-f", fileLocation, "-e" };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
            assertThat("Expected no errors", handler.getErrorMessages(), empty());
            assertThat("Expected no warnings", handler.getWarningMessages(), empty());

            List<String> infoMessages = handler.getInfoMessages();
            assertThat("Expected 4 info messages", infoMessages, hasSize(4));

            assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
            assertThat(infoMessages.get(1), containsString("Found 1 metadata groups to process"));
            assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
            assertThat(infoMessages.get(3), containsString("Row 2 - Item archived successfully"));

            // verify created item (ROW 2)
            String createdItemId = getItemUuidFromMessage(infoMessages.get(3));

            Item relatedPersonItem = findItemByMetadata("dc", "title", null, "Walter White");
            assertThat("Related Person item expected to be created", relatedPersonItem, notNullValue());

            Item createdItem = itemService.findByIdOrLegacyId(context, createdItemId);
            assertThat("Item expected to be created", createdItem, notNullValue());

            String personId = relatedPersonItem.getID().toString();

            List<MetadataValue> metadata = createdItem.getMetadata();
            assertThat(metadata, hasItems(with("dc.contributor.author", "Walter White", null, personId, 0, 600)));
            assertThat(metadata, hasItems(with("dc.title", "Wonderful Publication")));

        } finally {
            resetConsumers(defaultConsumers);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreatePublicationWithWillBeReferencedAuthority() throws Exception {
        String[] defaultConsumers = activateCrisConsumer();
        try {

            context.turnOffAuthorisationSystem();

            Item person = ItemBuilder.createItem(context, collection)
                .withEntityType("Person")
                .withTitle("Walter White")
                .withOrcidIdentifier("0000-0002-9079-593X")
                .build();

            Collection publications = createCollection(context, community)
                .withSubmissionDefinition("publication")
                .withAdminGroup(eperson)
                .build();

            context.commit();
            context.restoreAuthSystemState();

            String publicationCollectionId = publications.getID().toString();
            String fileLocation = getXlsFilePath("create-publication-with-will-be-referenced-authority.xls");
            String[] args = new String[] { "bulk-import", "-c", publicationCollectionId, "-f", fileLocation, "-e" };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
            assertThat("Expected no errors", handler.getErrorMessages(), empty());
            assertThat("Expected no warnings", handler.getWarningMessages(), empty());

            List<String> infoMessages = handler.getInfoMessages();
            assertThat("Expected 4 info messages", infoMessages, hasSize(4));

            assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
            assertThat(infoMessages.get(1), containsString("Found 1 metadata groups to process"));
            assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
            assertThat(infoMessages.get(3), containsString("Row 2 - Item archived successfully"));

            // verify created item (ROW 2)
            String createdItemId = getItemUuidFromMessage(infoMessages.get(3));

            Item createdItem = itemService.findByIdOrLegacyId(context, createdItemId);
            assertThat("Item expected to be created", createdItem, notNullValue());

            String personId = person.getID().toString();

            List<MetadataValue> metadata = createdItem.getMetadata();
            assertThat(metadata, hasItems(with("dc.contributor.author", "Walter White", null, personId, 0, 600)));
            assertThat(metadata, hasItems(with("dc.title", "Wonderful Publication")));

        } finally {
            resetConsumers(defaultConsumers);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreatePublicationWithWillBeReferencedAuthorityAndNoRelatedItemFound() throws Exception {
        String[] defaultConsumers = activateCrisConsumer();
        try {

            context.turnOffAuthorisationSystem();

            createCollection(context, community)
                .withEntityType("Person")
                .withAdminGroup(eperson)
                .build();

            Collection publications = createCollection(context, community)
                .withSubmissionDefinition("publication")
                .withAdminGroup(eperson)
                .build();

            context.commit();
            context.restoreAuthSystemState();

            String publicationCollectionId = publications.getID().toString();
            String fileLocation = getXlsFilePath("create-publication-with-will-be-referenced-authority.xls");
            String[] args = new String[] { "bulk-import", "-c", publicationCollectionId, "-f", fileLocation, "-e" };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
            assertThat("Expected no errors", handler.getErrorMessages(), empty());
            assertThat("Expected no warnings", handler.getWarningMessages(), empty());

            List<String> infoMessages = handler.getInfoMessages();
            assertThat("Expected 4 info messages", infoMessages, hasSize(4));

            assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
            assertThat(infoMessages.get(1), containsString("Found 1 metadata groups to process"));
            assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
            assertThat(infoMessages.get(3), containsString("Row 2 - Item archived successfully"));

            // verify created item (ROW 2)
            String createdItemId = getItemUuidFromMessage(infoMessages.get(3));

            Item createdItem = itemService.findByIdOrLegacyId(context, createdItemId);
            assertThat("Item expected to be created", createdItem, notNullValue());

            List<MetadataValue> metadata = createdItem.getMetadata();
            assertThat(metadata, hasItems(with("dc.contributor.author", "Walter White", null,
                "will be referenced::ORCID::0000-0002-9079-593X", 0, -1)));
            assertThat(metadata, hasItems(with("dc.title", "Wonderful Publication")));

        } finally {
            resetConsumers(defaultConsumers);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreatePublicationInWorkspace() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .withWorkflowGroup(1, eperson)
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String publicationCollectionId = publications.getID().toString();
        String fileLocation = getXlsFilePath("create-workspace-publication.xls");
        String[] args = new String[] { "bulk-import", "-c", publicationCollectionId, "-f", fileLocation, "-e" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 4 info messages", infoMessages, hasSize(4));

        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - WorkspaceItem created successfully"));

        // verify created item (ROW 2)
        String createdItemId = getItemUuidFromMessage(infoMessages.get(3));

        Item createdItem = itemService.findByIdOrLegacyId(context, createdItemId);
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(false));
        assertThat(findWorkspaceItem(createdItem), notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Test publication")));
        assertThat(metadata, hasItems(with("dc.date.issued", "12/12/65")));
        assertThat(metadata, hasItems(with("dc.identifier.doi", "10.1000/183")));
        assertThat(metadata, hasItems(with("dc.type", "Article")));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateArchivedPublication() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String publicationCollectionId = publications.getID().toString();
        String fileLocation = getXlsFilePath("create-archived-publication.xls");
        String[] args = new String[] { "bulk-import", "-c", publicationCollectionId, "-f", fileLocation, "-e" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 4 info messages", infoMessages, hasSize(4));

        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item archived successfully"));

        // verify created item (ROW 2)
        String createdItemId = getItemUuidFromMessage(infoMessages.get(3));

        Item createdItem = itemService.findByIdOrLegacyId(context, createdItemId);
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(true));

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Test publication")));
        assertThat(metadata, hasItems(with("dc.date.issued", "12/12/65")));
        assertThat(metadata, hasItems(with("dc.identifier.doi", "10.1000/183")));
        assertThat(metadata, hasItems(with("dc.type", "Article")));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateWorkflowPatentWithValidWorkspaceItem() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .withWorkflowGroup(1, eperson)
            .build();

        WorkspaceItem patentToUpdate = createWorkspaceItem(context, patents)
            .withTitle("Patent to update")
            .withAuthor("Luca G.")
            .withIsniIdentifier("54321")
            .withPatentNo("888-444-333")
            .withFulltext("test.txt", null, "test.txt".getBytes())
            .grantLicense()
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("update-workflow-patent.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 5 info messages", infoMessages, hasSize(5));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item updated successfully"));
        assertThat(infoMessages.get(4), containsString("Row 2 - WorkflowItem created successfully"));

        Item updatedItem = itemService.find(context, patentToUpdate.getItem().getID());
        assertThat(updatedItem.isArchived(), is(false));
        assertThat(findWorkspaceItem(updatedItem), nullValue());

        List<MetadataValue> metadata = updatedItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", 1)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher")));
        assertThat(metadata, not(hasItems(with("dc.identifier.patentno", "888-444-333"))));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateWorkflowPatentWithInvalidWorkspaceItem() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .withWorkflowGroup(1, eperson)
            .build();

        WorkspaceItem patentToUpdate = createWorkspaceItem(context, patents)
            .withTitle("Patent to update")
            .withAuthor("Luca G.")
            .withIsniIdentifier("54321")
            .withPatentNo("888-444-333")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("update-workflow-patent.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());

        List<String> warningMessages = handler.getWarningMessages();
        assertThat("Expected 1 warning message", warningMessages, hasSize(1));
        assertThat(warningMessages.get(0), containsString("Row 2 - Invalid item left in workspace"));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 4 info messages", infoMessages, hasSize(4));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item updated successfully"));

        Item updatedItem = itemService.find(context, patentToUpdate.getItem().getID());
        assertThat(updatedItem.isArchived(), is(false));
        assertThat(findWorkspaceItem(updatedItem), notNullValue());

        List<MetadataValue> metadata = updatedItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", 1)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher")));
        assertThat(metadata, not(hasItems(with("dc.identifier.patentno", "888-444-333"))));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateWorkflowPatentWithoutWorkspaceItem() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .withWorkflowGroup(1, eperson)
            .build();

        Item patentToUpdate = createItem(context, patents)
            .withTitle("Patent to update")
            .withAuthor("Luca G.")
            .withIsniIdentifier("54321")
            .withPatentNo("888-444-333")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("update-workflow-patent.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 5 info messages", infoMessages, hasSize(5));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item updated successfully"));
        assertThat(infoMessages.get(4), containsString("Row 2 - No workspace item to start found"));

        Item updatedItem = itemService.find(context, patentToUpdate.getID());

        List<MetadataValue> metadata = updatedItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", 1)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher")));
        assertThat(metadata, not(hasItems(with("dc.identifier.patentno", "888-444-333"))));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateArchivePatentWithWorkspaceItem() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .withWorkflowGroup(1, eperson)
            .build();

        WorkspaceItem patentToUpdate = createWorkspaceItem(context, patents)
            .withTitle("Patent to update")
            .withAuthor("Luca G.")
            .withIsniIdentifier("54321")
            .withPatentNo("888-444-333")
            .withFulltext("test.txt", null, "test.txt".getBytes())
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("update-archive-patent.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 5 info messages", infoMessages, hasSize(5));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item updated successfully"));
        assertThat(infoMessages.get(4), containsString("Row 2 - Item archived successfully "));

        Item updatedItem = itemService.find(context, patentToUpdate.getItem().getID());
        assertThat(updatedItem.isArchived(), is(true));

        List<MetadataValue> metadata = updatedItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", 1)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher")));
        assertThat(metadata, not(hasItems(with("dc.identifier.patentno", "888-444-333"))));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateArchivePatentWithWorkflowItem() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .withWorkflowGroup(1, eperson)
            .build();

        WorkflowItem patentToUpdate = WorkflowItemBuilder.createWorkflowItem(context, patents)
            .withTitle("Patent to update")
            .withAuthor("Luca G.")
            .withIsniIdentifier("54321")
            .withPatentNo("888-444-333")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("update-archive-patent.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 5 info messages", infoMessages, hasSize(5));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item updated successfully"));
        assertThat(infoMessages.get(4), containsString("Row 2 - Item archived successfully "));

        Item updatedItem = itemService.find(context, patentToUpdate.getItem().getID());
        assertThat(updatedItem.isArchived(), is(true));

        List<MetadataValue> metadata = updatedItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", 1)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher")));
        assertThat(metadata, not(hasItems(with("dc.identifier.patentno", "888-444-333"))));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateArchivePatentWithAlreadyArchivedItem() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .withWorkflowGroup(1, eperson)
            .build();

        Item patentToUpdate = createItem(context, patents)
            .withTitle("Patent to update")
            .withAuthor("Luca G.")
            .withIsniIdentifier("54321")
            .withPatentNo("888-444-333")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("update-archive-patent.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 5 info messages", infoMessages, hasSize(5));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item updated successfully"));
        assertThat(infoMessages.get(4), containsString("Row 2 - No workspace/workflow item to archive found"));

        Item updatedItem = itemService.find(context, patentToUpdate.getID());

        List<MetadataValue> metadata = updatedItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", 1)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher")));
        assertThat(metadata, not(hasItems(with("dc.identifier.patentno", "888-444-333"))));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAutomaticReferenceResolution() throws Exception {

        context.turnOffAuthorisationSystem();

        createCollection(context, community)
            .withEntityType("Person")
            .withAdminGroup(eperson)
            .build();

        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Collection persons = createCollection(context, community)
            .withSubmissionDefinition("person")
            .withAdminGroup(eperson)
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String publicationCollectionId = publications.getID().toString();
        String fileLocation = getXlsFilePath("create-publication-with-will-be-referenced-authority.xls");
        String[] args = new String[] { "bulk-import", "-c", publicationCollectionId, "-f", fileLocation, "-e" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());
        assertThat("Expected 4 info messages", handler.getInfoMessages(), hasSize(4));

        assertThat(handler.getInfoMessages().get(0), containsString("Start reading all the metadata group rows"));
        assertThat(handler.getInfoMessages().get(1), containsString("Found 1 metadata groups to process"));
        assertThat(handler.getInfoMessages().get(2), containsString("Found 1 items to process"));
        assertThat(handler.getInfoMessages().get(3), containsString("Row 2 - Item archived successfully"));

        String createdPublicationId = getItemUuidFromMessage(handler.getInfoMessages().get(3));

        Item publication = itemService.findByIdOrLegacyId(context, createdPublicationId);
        assertThat("Item expected to be created", publication, notNullValue());

        assertThat(publication.getMetadata(), hasItems(with("dc.contributor.author", "Walter White", null,
            "will be referenced::ORCID::0000-0002-9079-593X", 0, 600)));

        String personsCollectionId = persons.getID().toString();
        fileLocation = getXlsFilePath("create-person.xls");
        args = new String[] { "bulk-import", "-c", personsCollectionId, "-f", fileLocation, "-e" };
        handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());
        assertThat("Expected 4 info messages", handler.getInfoMessages(), hasSize(4));

        assertThat(handler.getInfoMessages().get(0), containsString("Start reading all the metadata group rows"));
        assertThat(handler.getInfoMessages().get(1), containsString("Found 0 metadata groups to process"));
        assertThat(handler.getInfoMessages().get(2), containsString("Found 1 items to process"));
        assertThat(handler.getInfoMessages().get(3), containsString("Row 2 - Item archived successfully"));

        String createdPersonId = getItemUuidFromMessage(handler.getInfoMessages().get(3));
        publication = context.reloadEntity(publication);

        assertThat(publication.getMetadata(), hasItems(with("dc.contributor.author", "Walter White", null,
            createdPersonId, 0, 600)));

    }

    private WorkspaceItem findWorkspaceItem(Item item) throws SQLException {
        return workspaceItemService.findByItem(context, item);
    }

    private String[] activateCrisConsumer() {
        ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        String[] consumers = configService.getArrayProperty("event.dispatcher.default.consumers");
        if (!ArrayUtils.contains(consumers, CRIS_CONSUMER)) {
            String newConsumers = consumers.length > 0 ? join(",", consumers) + "," + CRIS_CONSUMER : CRIS_CONSUMER;
            configService.setProperty("event.dispatcher.default.consumers", newConsumers);
            EventService eventService = EventServiceFactory.getInstance().getEventService();
            eventService.reloadConfiguration();
        }

        return consumers;
    }

    private void resetConsumers(String[] consumers) {
        ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        configService.setProperty("event.dispatcher.default.consumers", consumers);
        EventService eventService = EventServiceFactory.getInstance().getEventService();
        eventService.reloadConfiguration();
    }

    private Item findItemByMetadata(String schema, String element, String qualifier, String value) throws Exception {
        Iterator<Item> iterator = itemService.findArchivedByMetadataField(context, schema, element, qualifier, value);
        return iterator.hasNext() ? iterator.next() : null;
    }

    private List<String> getItemUuidFromMessage(List<String> message) {
        List<String> itemIds = new ArrayList<>();
        message.forEach(m -> itemIds.add(getItemUuidFromMessage(m)));
        return itemIds;
    }

    private String getItemUuidFromMessage(String message) {
        Matcher matcher = UUID_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }

        return matcher.group(0);
    }

    private String getXlsFilePath(String name) {
        return new File(BASE_XLS_DIR_PATH, name).getAbsolutePath();
    }

    @Test
    public void uploadSingleBitstreamTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection publication = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileName = "add-bitstream-to-item.xls";
        String fileLocation = getXlsFilePath(fileName);
        String bitstreamLocation = "file://test.txt";

        String tmpFileLocation = createTemporaryExcelFile(fileLocation, fileName,
                                                          null, Arrays.asList(bitstreamLocation));

        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", tmpFileLocation};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());

        String itemUuid = getItemUuidFromMessage(handler.getWarningMessages().get(0));
        Item item = itemService.find(context, UUID.fromString(itemUuid));
        List<Bitstream> bitstreams = new ArrayList<>();
        bitstreamService.getItemBitstreams(context, item).forEachRemaining(bitstreams::add);
        String bundleName = "TEST-BUNDLE";
        Bitstream bitstream = getBitstreamByBundleName(bitstreams, bundleName);
        InputStream inputStream = bitstreamService.retrieve(context, bitstream);
        String bitstreamContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        Map<String, String> metadataMap = getMetadataFromBitStream(bitstream);

        assertThat(metadataMap.get("dc.title"), is("Test title"));
        assertThat(metadataMap.get("dc.description"), is("test file descr"));
        assertThat(bitstreamContent, is("this is a test file for uploading bitstreams"));
        assertThat(bitstream.getBundles().get(0).getName(), is(bundleName));
    }

    @Test
    public void uploadMultipleBitstreamTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection publication = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileName = "add-multiple-bitstreams-to-items.xls";
        String fileLocation = getXlsFilePath(fileName);

        String bitstreamLocation1 = "file://test.txt";
        String bitstreamLocation2 = "file://test_2.txt";
        String bitstreamLocation3 = "file://subfolder/test_3.txt";

        String tmpFileLocation = createTemporaryExcelFile(fileLocation, fileName, null,
                                                              Arrays.asList(bitstreamLocation1,
                                                                       bitstreamLocation2,
                                                                       bitstreamLocation3));

        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", tmpFileLocation};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());

        List<String> itemUuids = getItemUuidFromMessage(handler.getWarningMessages());

        Item item = itemService.find(context, UUID.fromString(itemUuids.get(0)));
        Item item2 = itemService.find(context, UUID.fromString(itemUuids.get(1)));

        // Get bitstream from items (first item has three bitstreams)
        List<Bitstream> item1Bitstreams = new ArrayList<>();
        List<Bitstream> item2Bitstreams = new ArrayList<>();
        bitstreamService.getItemBitstreams(context, item).forEachRemaining(item1Bitstreams::add);
        bitstreamService.getItemBitstreams(context, item2).forEachRemaining(item2Bitstreams::add);

        String item1bundle1 = "TEST-BUNDLE";
        String item1bundle2 = "TEST-BUNDLE2";
        String item2bundle1 = "SECOND-BUNDLE";

        Bitstream item1Bitstream1 = getBitstreamByBundleName(item1Bitstreams, item1bundle1);
        Bitstream item1Bitstream2 = getBitstreamByBundleName(item1Bitstreams, item1bundle2);
        Bitstream item2bitstream1 = getBitstreamByBundleName(item2Bitstreams, item2bundle1);

        // Get content of first item bitstreams
        InputStream inputStream = bitstreamService.retrieve(context, item1Bitstream1);
        String bitstreamContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        InputStream inputStream1 = bitstreamService.retrieve(context, item1Bitstream2);
        String bitstreamContent1 = IOUtils.toString(inputStream1, StandardCharsets.UTF_8);

        // Get content of second item bitstream
        InputStream inputStream2 = bitstreamService.retrieve(context, item2bitstream1);
        String bitstreamContent2 = IOUtils.toString(inputStream2, StandardCharsets.UTF_8);

        // Get metadata map of items bitstreams
        Map<String, String> metadataMap = getMetadataFromBitStream(item1Bitstream1);
        Map<String, String> metadataMap2 = getMetadataFromBitStream(item1Bitstream2);
        Map<String, String> metadataMap3 = getMetadataFromBitStream(item2bitstream1);

        // First bitstream of item 1
        assertThat(metadataMap.get("dc.title"), is("Test title"));
        assertThat(metadataMap.get("dc.description"), is("test file description"));
        assertThat(bitstreamContent, is("this is a test file for uploading bitstreams"));

        // Second bitstream of item 1
        assertThat(metadataMap2.get("dc.title"), is("Test title 2"));
        assertThat(metadataMap2.get("dc.description"), is("test file description 2"));
        assertThat(bitstreamContent1, is("this is a second test file for uploading bitstreams"));

        // First item bundles
        assertThat(item1Bitstream1.getBundles().get(0).getName(), is(item1bundle1));
        assertThat(item1Bitstream2.getBundles().get(0).getName(), is(item1bundle2));

        // Second item
        assertThat(metadataMap3.get("dc.title"), is("Test title 3"));
        assertThat(metadataMap3.get("dc.description"), is("test file description 3"));
        assertThat(bitstreamContent2, is("this is a third test file for uploading bitstreams"));
        assertThat(item2bitstream1.getBundles().get(0).getName(), is("SECOND-BUNDLE"));
    }

    @Test
    public void uploadMultipleBitstreamWithPathTraversalTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection publication = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileName = "add-multiple-bitstreams-to-items.xls";
        String fileLocation = getXlsFilePath(fileName);

        String bitstreamLocation1 = "file://../config/dspace.cfg";
        String bitstreamLocation2 = "file:///home/ubuntu/.ssh/config";
        // this is unusual but valid
        String bitstreamLocation3 = "file://subfolder/../subfolder/test_3.txt";

        String tmpFileLocation = createTemporaryExcelFile(fileLocation, fileName, null,
                                                              Arrays.asList(bitstreamLocation1,
                                                                       bitstreamLocation2,
                                                                       bitstreamLocation3));

        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", tmpFileLocation};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected two errors (4 messages)", handler.getErrorMessages(), not(empty()));

        List<String> itemUuids = getItemUuidFromMessage(handler.getWarningMessages());

        Item item = itemService.find(context, UUID.fromString(itemUuids.get(0)));
        Item item2 = itemService.find(context, UUID.fromString(itemUuids.get(1)));

        // Get bitstream from items (first item has three bitstreams)
        List<Bitstream> item1Bitstreams = new ArrayList<>();
        List<Bitstream> item2Bitstreams = new ArrayList<>();
        bitstreamService.getItemBitstreams(context, item).forEachRemaining(item1Bitstreams::add);
        bitstreamService.getItemBitstreams(context, item2).forEachRemaining(item2Bitstreams::add);

        String item1bundle1 = "TEST-BUNDLE";
        String item1bundle2 = "TEST-BUNDLE2";
        String item2bundle1 = "SECOND-BUNDLE";

        Bitstream item1Bitstream1 = getBitstreamByBundleName(item1Bitstreams, item1bundle1);
        Bitstream item1Bitstream2 = getBitstreamByBundleName(item1Bitstreams, item1bundle2);
        Bitstream item2bitstream1 = getBitstreamByBundleName(item2Bitstreams, item2bundle1);

        assertThat("item1bitstream1 must be null", item1Bitstream1 == null);
        assertThat("item1bitstream2 must be null", item1Bitstream2 == null);
        assertThat("item21bitstream1 must be NOT null", item2bitstream1 != null);

        // Get content of second item bitstream
        InputStream inputStream2 = bitstreamService.retrieve(context, item2bitstream1);
        String bitstreamContent2 = IOUtils.toString(inputStream2, StandardCharsets.UTF_8);

        // Get metadata map of items bitstreams
        Map<String, String> metadataMap3 = getMetadataFromBitStream(item2bitstream1);

        // Second item
        assertThat(metadataMap3.get("dc.title"), is("Test title 3"));
        assertThat(metadataMap3.get("dc.description"), is("test file description 3"));
        assertThat(bitstreamContent2, is("this is a third test file for uploading bitstreams"));
        assertThat(item2bitstream1.getBundles().get(0).getName(), is("SECOND-BUNDLE"));
    }

    @Test
    public void uploadSingleBitstreamUpdateTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection publication = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item publicationItem = createItem(context, publication)
            .withTitle("Test Publication")
            .withAuthor("Luca G.")
            .withDescription("This is a test for bulk import")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileName = "add-bitstream-to-item-update.xls";
        String fileLocation = getXlsFilePath(fileName);
        String bitstreamLocation = "file://test.txt";

        String tmpFileLocation = createTemporaryExcelFile(fileLocation, fileName,
                                                          Arrays.asList(publicationItem.getID().toString()),
                                                          Arrays.asList(bitstreamLocation));

        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", tmpFileLocation};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        Bitstream bitstream = bitstreamService.getItemBitstreams(context, publicationItem).next();
        InputStream inputStream = bitstreamService.retrieve(context, bitstream);
        String bitstreamContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        Map<String, String> metadataMap = getMetadataFromBitStream(bitstream);

        assertThat(metadataMap.get("dc.title"), is("Test title"));
        assertThat(metadataMap.get("dc.description"), is("test file description"));
        assertThat(bitstreamContent, is("this is a test file for uploading bitstreams"));
        assertThat(bitstream.getBundles().get(0).getName(), is("TEST-BUNDLE"));
    }

    @Test
    public void uploadMultipleBitstreamsUpdateMultipleTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection publication = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item publicationItem = createItem(context, publication)
            .withTitle("Test Publication")
            .withAuthor("Luca G.")
            .withDescription("This is a test for bulk import")
            .build();

        Item publicationItem2 = createItem(context, publication)
            .withTitle("Test Publication 2")
            .withAuthor("Luca G.")
            .withDescription("This is a test for bulk import")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileName = "add-bitstream-to-multiple-items-update.xls";
        String fileLocation = getXlsFilePath(fileName);
        String bitstreamLocation = "file://test.txt";
        String bitstreamLocation2 = "file://test_2.txt";

        String tmpFileLocation = createTemporaryExcelFile(fileLocation, fileName,
                                                          Arrays.asList(publicationItem.getID().toString(),
                                                                        publicationItem2.getID().toString()),
                                                          Arrays.asList(bitstreamLocation, bitstreamLocation2));

        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", tmpFileLocation};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());

        Bitstream bitstream = bitstreamService.getItemBitstreams(context, publicationItem).next();
        InputStream inputStream = bitstreamService.retrieve(context, bitstream);
        String bitstreamContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        Bitstream bitstream2 = bitstreamService.getItemBitstreams(context, publicationItem2).next();
        InputStream inputStream2 = bitstreamService.retrieve(context, bitstream2);
        String bitstreamContent2 = IOUtils.toString(inputStream2, StandardCharsets.UTF_8);

        Map<String, String> metadataMap = getMetadataFromBitStream(bitstream);
        Map<String, String> metadataMap2 = getMetadataFromBitStream(bitstream2);

        assertThat(metadataMap.get("dc.title"), is("Test title"));
        assertThat(metadataMap.get("dc.description"), is("test file description"));
        assertThat(bitstreamContent, is("this is a test file for uploading bitstreams"));
        assertThat(bitstream.getBundles().get(0).getName(), is("TEST-BUNDLE"));

        assertThat(metadataMap2.get("dc.title"), is("Test title 2"));
        assertThat(metadataMap2.get("dc.description"), is("test file description 2"));
        assertThat(bitstreamContent2, is("this is a second test file for uploading bitstreams"));
        assertThat(bitstream2.getBundles().get(0).getName(), is("TEST-BUNDLE2"));
    }

    @Test
    public void uploadSingleBitstreamUpdateTestWithExistingBundle() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection publication = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item publicationItem = createItem(context, publication)
            .withTitle("Test Publication")
            .withAuthor("Luca G.")
            .withDescription("This is a test for bulk import")
            .build();

        bundleService.create(context, publicationItem, "JM-BUNDLE");

        context.commit();
        context.restoreAuthSystemState();

        String fileName = "add-bitstream-to-item-bundle.xls";
        String fileLocation = getXlsFilePath(fileName);
        String bitstreamLocation = "file://test.txt";

        String tmpFileLocation = createTemporaryExcelFile(fileLocation, fileName,
                                                          Arrays.asList(publicationItem.getID().toString()),
                                                          Arrays.asList(bitstreamLocation));

        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", tmpFileLocation};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());

        // Assert that no new bundle was created from script
        assertThat(publicationItem.getBundles(), hasSize(1));

        Bitstream bitstream = bitstreamService.getItemBitstreams(context, publicationItem).next();
        InputStream inputStream = bitstreamService.retrieve(context, bitstream);
        String bitstreamContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        Map<String, String> metadataMap = getMetadataFromBitStream(bitstream);

        assertThat(metadataMap.get("dc.title"), is("Test title"));
        assertThat(metadataMap.get("dc.description"), is("test file description"));
        assertThat(bitstreamContent, is("this is a test file for uploading bitstreams"));
        assertThat(bitstream.getBundles().get(0).getName(), is("JM-BUNDLE"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreatePublicationInWorkspaceItemsAndItemHasLicense() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileName = "items-with-bitstreams.xlsx";
        String fileLocation = getXlsFilePath(fileName);
        String bitstreamLocation = "file://test.txt";

        String tmpFileLocation = createTemporaryExcelFile(fileLocation, fileName, null,
            Arrays.asList(bitstreamLocation));

        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", tmpFileLocation};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 4 info messages", infoMessages, hasSize(4));

        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 2 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - WorkflowItem created successfully"));

        // verify created item (ROW 2)
        String itemUuid = getItemUuidFromMessage(infoMessages.get(3));

        Item createdItem = itemService.findByIdOrLegacyId(context, itemUuid);
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(true));
        assertThat(findWorkspaceItem(createdItem), nullValue());

        List<Bitstream> bitstreams = new ArrayList<>();
        bitstreamService.getItemBitstreams(context, createdItem).forEachRemaining(bitstreams::add);

        Bitstream bitstream = getBitstreamByBundleName(bitstreams, "LICENSE");
        InputStream inputStream = bitstreamService.retrieve(context, bitstream);
        String bitstreamContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        assertThat(bitstream.getBundles().get(0).getName(), is("LICENSE"));
        assertThat(bitstreamContent, containsString("NOTE: PLACE YOUR OWN LICENSE HERE\n" +
            "This sample license is provided for informational purposes only."));

        Bitstream bitstream1 = getBitstreamByBundleName(bitstreams, "ORIGINAL");
        inputStream = bitstreamService.retrieve(context, bitstream1);
        bitstreamContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        Map<String, String> metadataMap = getMetadataFromBitStream(bitstream1);

        assertThat(metadataMap.get("dc.title"), is("Test title.txt"));
        assertThat(metadataMap.get("dc.description"), is("test file descr"));
        assertThat(bitstreamContent, is("this is a test file for uploading bitstreams"));
        assertThat(bitstream1.getBundles().get(0).getName(), is("ORIGINAL"));

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "publication with attachment uploaded part second")));
        assertThat(metadata, hasItems(with("dc.title.alternative", "lorem ipsum new new new")));
        assertThat(metadata, hasItems(with("dc.date.issued", "2022-05-31")));
        assertThat(metadata, hasItems(with("dc.type", "Resource Types::text::manuscript")));
        assertThat(metadata, hasItems(with("dc.language.iso", "en")));
        assertThat(metadata, hasItems(with("dc.contributor.author",
            "Lombardi, Corrado", "b5ad6864-012d-4989-8e0d-4acfa1156fd9", 0, 600)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", "4Science",
            "a14ba215-c0f0-4b74-b21a-06359bfabd45", 0, 600)));
        assertThat(metadata, hasItems(with("dc.contributor.editor",
            "Corrado Francesco, Lombardi", "29177bec-ff50-4428-aa43-1fdf932f0d33", 0, 600)));

    }

    /**
     * Test Bitstream format of created Bitstreams.
     */
    @Test
    public void testCreatePublicationInWorkspaceItemsWithBitstreams() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection publication = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileName = "items-with-bitstreams.xlsx";
        String fileLocation = getXlsFilePath(fileName);
        String bitstreamLocation = "file://test.txt";

        String tmpFileLocation = createTemporaryExcelFile(fileLocation, fileName, null,
            Arrays.asList(bitstreamLocation));

        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", tmpFileLocation};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 4 info messages", infoMessages, hasSize(4));

        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 2 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - WorkflowItem created successfully"));

        // verify created item (ROW 2)
        String itemUuid = getItemUuidFromMessage(infoMessages.get(3));

        Item createdItem = itemService.findByIdOrLegacyId(context, itemUuid);
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(true));
        assertThat(findWorkspaceItem(createdItem), nullValue());

        List<Bitstream> bitstreams = new ArrayList<>();
        bitstreamService.getItemBitstreams(context, createdItem).forEachRemaining(bitstreams::add);

        Bitstream bitstream = getBitstreamByBundleName(bitstreams, "LICENSE");
        InputStream inputStream = bitstreamService.retrieve(context, bitstream);
        String bitstreamContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        assertThat(bitstream.getBundles().get(0).getName(), is("LICENSE"));
        assertThat(bitstreamContent, containsString("NOTE: PLACE YOUR OWN LICENSE HERE\n" +
            "This sample license is provided for informational purposes only."));

        BitstreamFormat bf = bitstream.getFormat(context);

        assertThat(bf.getMIMEType(), is("text/plain; charset=utf-8"));
        assertThat(bf.getShortDescription(), is("License"));
        assertThat(bf.getDescription(), is("Item-specific license agreed to upon submission"));

        Bitstream bitstream1 = getBitstreamByBundleName(bitstreams, "ORIGINAL");
        inputStream = bitstreamService.retrieve(context, bitstream1);
        bitstreamContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        Map<String, String> metadataMap = getMetadataFromBitStream(bitstream1);

        assertThat(metadataMap.get("dc.title"), is("Test title.txt"));
        assertThat(metadataMap.get("dc.description"), is("test file descr"));
        assertThat(bitstreamContent, is("this is a test file for uploading bitstreams"));
        assertThat(bitstream1.getBundles().get(0).getName(), is("ORIGINAL"));

        BitstreamFormat bf1 = bitstream1.getFormat(context);

        assertThat(bf1.getMIMEType(), is("text/plain"));
        assertThat(bf1.getShortDescription(), is("Text"));
        assertThat(bf1.getDescription(), is("Plain Text"));

    }

    /*
     * Creates a temporary Excel file which is a copy of the one provided
     * but set's the FILE_PATH column to the new bitstream path.
     */
    private String createTemporaryExcelFile(String excelFilePath, String excelFileName,
                                            List<String> idList, List<String> bitstreamFilePaths) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook(excelFilePath);
        setIdsToExcelFile(workbook, idList);
        setBitstreamPathToExcelFile(workbook, bitstreamFilePaths);

        // Write file to disk
        File file = new File(getXlsFilePath("tmp_" + excelFileName));
        FileOutputStream outputStream = new FileOutputStream(file);
        workbook.write(outputStream);
        outputStream.close();

        temporaryFiles.add(file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    private void setIdsToExcelFile(Workbook workbook, List<String> idList) {
        if (idList == null) {
            return;
        }

        Sheet sheet = workbook.getSheetAt(0);
        for (int i = 0; i < idList.size(); i++) {
            sheet.getRow(i + 1).getCell(0).setCellValue(idList.get(i));
        }
    }

    private void setBitstreamPathToExcelFile(Workbook workbook, List<String> bitstreams) {
        if (bitstreams == null) {
            return;
        }

        Sheet sheet = workbook.getSheet(BITSTREAM_METADATA);
        for (int i = 0; i < bitstreams.size(); i++) {
            sheet.getRow(i + 1).getCell(1).setCellValue(bitstreams.get(i));
        }
    }

    private void cleanUpTemporaryFiles() {
        temporaryFiles.forEach(f -> new File(f).delete());
    }

    private Map<String, String> getMetadataFromBitStream(Bitstream bitstream) {
        Map<String, String> map = new HashMap<>();
        bitstream.getMetadata().forEach(m -> map.put(getMetadataName(m), m.getValue()));
        return map;
    }

    private String getMetadataName(MetadataValue m) {
        if (StringUtils.isBlank(m.getQualifier())) {
            return String.format("%s.%s", m.getSchema(), m.getElement());
        }

        return String.format("%s.%s.%s", m.getSchema(),
                             m.getQualifier(),
                             m.getElement());
    }

    private Bitstream getBitstreamByBundleName(List<Bitstream> bitstreams, String bundleName) throws SQLException {
        for (Bitstream bitstream : bitstreams) {
            if (bitstream.getBundles().get(0).getName().equals(bundleName)) {
                return bitstream;
            }
        }
        return null;
    }

    @After
    public void cleanUp() {
        cleanUpTemporaryFiles();
    }

}
