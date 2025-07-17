/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemimport;

import static com.jayway.jsonpath.JsonPath.read;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.matcher.ProcessMatcher;
import org.dspace.app.rest.matcher.RelationshipMatcher;
import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ProcessBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.ProcessStatus;
import org.dspace.content.Relationship;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.eperson.EPerson;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.Process;
import org.dspace.scripts.service.ProcessService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Basic integration testing for the SAF Import feature via UI {@link ItemImport}.
 * https://wiki.lyrasis.org/display/DSDOC7x/Importing+and+Exporting+Items+via+Simple+Archive+Format
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemImportIT extends AbstractEntityIntegrationTest {

    private static final String publicationTitle = "A Tale of Two Cities";
    private static final String personTitle = "Person Test";

    @Autowired
    private ItemService itemService;
    @Autowired
    private RelationshipService relationshipService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private ProcessService processService;
    @Autowired
    private DSpaceRunnableParameterConverter dSpaceRunnableParameterConverter;
    private Collection collection;
    private Path workDir;
    private static final String TEMP_DIR = ItemImport.TEMP_DIR;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection")
                .withEntityType("Publication")
                .build();
        context.restoreAuthSystemState();

        File file = new File(configurationService.getProperty("org.dspace.app.batchitemimport.work.dir"));
        if (!file.exists()) {
            Files.createDirectory(Path.of(file.getAbsolutePath()));
        }
        workDir = Path.of(file.getAbsolutePath());
    }

    @After
    @Override
    public void destroy() throws Exception {
        for (Path path : Files.list(workDir).collect(Collectors.toList())) {
            PathUtils.delete(path);
        }
        super.destroy();
    }

    @Test
    public void importItemByZipSafWithBitstreams() throws Exception {
        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-a", ""));
        parameters.add(new DSpaceCommandLineParameter("-c", collection.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-z", "saf-bitstreams.zip"));
        MockMultipartFile bitstreamFile = new MockMultipartFile("file", "saf-bitstreams.zip",
                MediaType.APPLICATION_OCTET_STREAM_VALUE, getClass().getResourceAsStream("saf-bitstreams.zip"));
        perfomImportScript(parameters, bitstreamFile, admin);

        checkMetadata();
        checkMetadataWithAnotherSchema();
        checkBitstream();

        // confirm that TEMP_DIR still exists
        File workTempDir = new File(workDir + File.separator + TEMP_DIR);
        assertTrue(workTempDir.exists());
    }

    @Test
    public void importItemIntoAdministeredCollectionByZipSafWithBitstreams() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection epersonCollection =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Collection")
                             .withEntityType("Publication")
                             .withAdminGroup(eperson)
                             .build();
        context.restoreAuthSystemState();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-a", ""));
        parameters.add(new DSpaceCommandLineParameter("-c", epersonCollection.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-z", "saf-bitstreams.zip"));
        MockMultipartFile bitstreamFile =
            new MockMultipartFile(
                "file", "saf-bitstreams.zip",
                MediaType.APPLICATION_OCTET_STREAM_VALUE, getClass().getResourceAsStream("saf-bitstreams.zip")
            );
        perfomImportScript(parameters, bitstreamFile, eperson);

        checkMetadata();
        checkMetadataWithAnotherSchema();
        checkBitstream();

        // confirm that TEMP_DIR still exists
        File workTempDir = new File(workDir + File.separator + TEMP_DIR);
        assertTrue(workTempDir.exists());
    }

    @Test
    public void importMultipleItemsByZipSafWithBitstreams() throws Exception {
        // use the pool executor to run multiple scripts in parallel
        String oldExecutor = configurationService.getProperty("dspace.task.executor");
        configurationService.setProperty("dspace.task.executor", "dspaceRunnableThreadPoolExecutor");

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-a", ""));
        parameters.add(new DSpaceCommandLineParameter("-c", collection.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-z", "saf-bitstreams.zip"));
        MockMultipartFile bitstreamFile = new MockMultipartFile("file", "saf-bitstreams.zip",
            MediaType.APPLICATION_OCTET_STREAM_VALUE, getClass().getResourceAsStream("saf-bitstreams.zip"));

        // perform multiple imports
        AtomicReference<Integer> idRefProcess1 = scheduleImportScript(parameters, bitstreamFile);
        AtomicReference<Integer> idRefProcess2 = scheduleImportScript(parameters, bitstreamFile);

        // wait until the scheduled processes are finished
        boolean isFirstProcessCompleted = false;
        boolean isSecondProcessCompleted = false;
        do {
            try {
                if (!isFirstProcessCompleted) {
                    isProcessCompleted(idRefProcess1.get(), parameters);
                    isFirstProcessCompleted = true;
                }
                if (!isSecondProcessCompleted) {
                    isProcessCompleted(idRefProcess2.get(), parameters);
                    isSecondProcessCompleted = true;
                }
            } catch (AssertionError e) {
                // nothing to do since we are looping until the process are finished
            }
        } while (!isFirstProcessCompleted || !isSecondProcessCompleted);

        // check results
        Iterator<Item> items = itemService.findArchivedByMetadataField(
            context, "dc", "title", null, publicationTitle);
        assertTrue(items.hasNext());
        Item item1 = items.next();
        assertTrue(items.hasNext());
        Item item2 = items.next();
        checkMetadata(item1);
        checkMetadata(item2);
        checkMetadataWithAnotherSchema(item1);
        checkMetadataWithAnotherSchema(item2);
        item1 = context.reloadEntity(item1);
        item2 = context.reloadEntity(item2);
        Bitstream bitstreamOfItem1 = itemService.getBundles(item1, "ORIGINAL").get(0).getBitstreams().get(0);
        Bitstream bitstreamOfItem2 = itemService.getBundles(item2, "ORIGINAL").get(0).getBitstreams().get(0);
        checkBitstream(bitstreamOfItem1);
        checkBitstream(bitstreamOfItem2);

        // confirm that TEMP_DIR still exists
        File workTempDir = new File(workDir + File.separator + TEMP_DIR);
        assertTrue(workTempDir.exists());

        // reinstate old configuration
        configurationService.setProperty("dspace.task.executor", oldExecutor);
    }

    @Test
    public void importItemByZipSafWithRelationships() throws Exception {
        context.turnOffAuthorisationSystem();
        // create collection that contains person
        Collection collectionPerson = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection Person")
                .withEntityType("Person")
                .build();
        // create person
        Item person = ItemBuilder.createItem(context, collectionPerson)
                .withTitle(personTitle)
                .build();
        context.restoreAuthSystemState();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-a", ""));
        parameters.add(new DSpaceCommandLineParameter("-p", ""));
        parameters.add(new DSpaceCommandLineParameter("-c", collection.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-z", "saf-relationships.zip"));
        MockMultipartFile bitstreamFile = new MockMultipartFile("file", "saf-relationships.zip",
                MediaType.APPLICATION_OCTET_STREAM_VALUE, getClass().getResourceAsStream("saf-relationships.zip"));
        perfomImportScript(parameters, bitstreamFile, admin);

        checkMetadata();
        checkRelationship();
    }

    /**
     * Check metadata on imported item
     * @throws Exception
     */
    private void checkMetadata() throws Exception {
        Item item = itemService.findArchivedByMetadataField(
                context, "dc", "title", null, publicationTitle).next();
        checkMetadata(item);
    }

    /**
     * Check metadata on imported item
     * @param item the imported item
     * @throws Exception
     */
    private void checkMetadata(Item item) throws Exception {
        getClient().perform(get("/api/core/items/" + item.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata", allOf(
                matchMetadata("dc.title", publicationTitle),
                matchMetadata("dc.date.issued", "1990"),
                matchMetadata("dc.title.alternative", "J'aime les Printemps"))));
    }

    /**
     * Check metadata on imported item
     * @throws Exception
     */
    private void checkMetadataWithAnotherSchema() throws Exception {
        Item item = itemService.findArchivedByMetadataField(
                context, "dc", "title", null, publicationTitle).next();
        checkMetadataWithAnotherSchema(item);
    }

    /**
     * Check metadata on imported item
     * @param item the imported item
     * @throws Exception
     */
    private void checkMetadataWithAnotherSchema(Item item) throws Exception {
        getClient().perform(get("/api/core/items/" + item.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata", allOf(
                matchMetadata("dcterms.title", publicationTitle))));
    }

    /**
     * Check bitstreams on imported item
     * @throws Exception
     */
    private void checkBitstream() throws Exception {
        Bitstream bitstream = itemService.findArchivedByMetadataField(
                context, "dc", "title", null, publicationTitle).next()
                .getBundles("ORIGINAL").get(0).getBitstreams().get(0);
        checkBitstream(bitstream);
    }

    /**
     * Check bitstreams on imported bitstream
     * @param bitstream the imported bitstream
     * @throws Exception
     */
    private void checkBitstream(Bitstream bitstream) throws Exception {
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata", allOf(
                matchMetadata("dc.title", "file1.txt"))));
    }

    /**
     * Check relationships between imported items
     * @throws Exception
     */
    private void checkRelationship() throws Exception {
        Item item = itemService.findArchivedByMetadataField(
                context, "dc", "title", null, publicationTitle).next();
        Item author = itemService.findArchivedByMetadataField(
                context, "dc", "title", null, personTitle).next();
        List<Relationship> relationships = relationshipService.findByItem(context, item);
        assertEquals(1, relationships.size());
        getClient().perform(get("/api/core/relationships/" + relationships.get(0).getID()).param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.leftPlace", is(0)))
                   .andExpect(jsonPath("$._links.rightItem.href", containsString(author.getID().toString())))
                   .andExpect(jsonPath("$.rightPlace", is(0)))
                   .andExpect(jsonPath("$", Matchers.is(RelationshipMatcher.matchRelationship(relationships.get(0)))));
    }

    private void perfomImportScript(
        LinkedList<DSpaceCommandLineParameter> parameters, MockMultipartFile bitstreamFile, EPerson user)
        throws Exception {
        Process process = null;

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        try {
            AtomicReference<Integer> idRef = new AtomicReference<>();

            getClient(getAuthToken(user.getEmail(), password))
                .perform(multipart("/api/system/scripts/import/processes")
                             .file(bitstreamFile)
                             .param("properties", new ObjectMapper().writeValueAsString(list)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", is(
                    ProcessMatcher.matchProcess("import",
                                                String.valueOf(user.getID()), parameters,
                                                ProcessStatus.COMPLETED))))
                .andDo(result -> idRef
                    .set(read(result.getResponse().getContentAsString(), "$.processId")));

            process = processService.find(context, idRef.get());
            checkProcess(process);
        } finally {
            ProcessBuilder.deleteProcess(process.getID());
        }
    }

    private AtomicReference<Integer> scheduleImportScript(
                LinkedList<DSpaceCommandLineParameter> parameters, MockMultipartFile bitstreamFile)
        throws Exception {
        AtomicReference<Integer> idRef = new AtomicReference<>();

        List<ParameterValueRest> list = parameters.stream()
            .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
            .collect(Collectors.toList());

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(multipart("/api/system/scripts/import/processes")
                .file(bitstreamFile)
                .param("properties", new ObjectMapper().writeValueAsString(list)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$", is(
                ProcessMatcher.matchProcess("import",
                    String.valueOf(admin.getID()), parameters,
                    ProcessStatus.SCHEDULED))))
            .andDo(result -> idRef
                .set(read(result.getResponse().getContentAsString(), "$.processId")));

        return idRef;
    }

    private void checkProcess(Process process) {
        assertNotNull(process.getBitstreams());
        assertEquals(3, process.getBitstreams().size());
        assertEquals(1, process.getBitstreams().stream()
                .filter(b -> StringUtils.equals(b.getName(), ItemImport.MAPFILE_FILENAME))
                .count());
        assertEquals(1,
                process.getBitstreams().stream()
                .filter(b -> StringUtils.contains(b.getName(), ".log"))
                .count());
        assertEquals(1,
                process.getBitstreams().stream()
                .filter(b -> StringUtils.contains(b.getName(), ".zip"))
                .count());
    }

    private void isProcessCompleted(
                Integer processId, LinkedList<DSpaceCommandLineParameter> parameters)
        throws Exception {
        getClient(getAuthToken(admin.getEmail(), password))
            .perform(get("/api/system/processes/" + processId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.is(
                ProcessMatcher.matchProcess("import", String.valueOf(admin.getID()),
                    processId, parameters, ProcessStatus.COMPLETED))));
        ProcessBuilder.deleteProcess(processId);
    }
}
