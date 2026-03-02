/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Item;
import org.dspace.importer.external.core.CoreImportMetadataSourceServiceImpl;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link CoreImportMetadataSourceServiceImpl}
 * (pattern based on DataCiteImportMetadataSourceServiceIT)
 * @author Antonio Fasanella (antonio.fasanella@4science.com)
 */
public class CoreImportMetadataSourceServiceIT extends AbstractLiveImportIntegrationTest {

    @Autowired
    private LiveImportClientImpl liveImportClientImpl;

    @Autowired
    private CoreImportMetadataSourceServiceImpl coreServiceImpl;

    @Test
    public void coreImportMetadataGetNoRecordsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try {
            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse("", 404, "Not Found");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            Collection<ImportRecord> recordsImported = coreServiceImpl.getRecords("test query", 0, 2);
            assertEquals(0, recordsImported.size());
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void coreImportMetadataGetRecordsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try (InputStream coreResp = getClass().getResourceAsStream("core-test.json")) {
            String coreRespStr = IOUtils.toString(coreResp, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(coreRespStr, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();

            ArrayList<ImportRecord> expected = getExpectedRecordsFromCoreTest();
            Collection<ImportRecord> recordsImported = coreServiceImpl.getRecords("test query", 0, 2);

            assertEquals(2, recordsImported.size());
            matchRecords(new ArrayList<>(recordsImported), expected);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void coreImportMetadataGetRecordsCountTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try (InputStream coreResp = getClass().getResourceAsStream("core-test.json")) {
            String coreRespStr = IOUtils.toString(coreResp, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(coreRespStr, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();

            int tot = coreServiceImpl.getRecordsCount("test query");
            assertEquals(3754174, tot);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void coreImportMetadataGetRecordByIdTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try (InputStream coreResp = getClass().getResourceAsStream("core-by-id.json")) {
            String coreRespStr = IOUtils.toString(coreResp, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(coreRespStr, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();

            ArrayList<ImportRecord> expected = getExpectedRecordFromCoreById();

            ImportRecord recordImported = coreServiceImpl.getRecord("10.1143/ptps.168.45");
            assertNotNull(recordImported);

            Collection<ImportRecord> recordsImported = Arrays.asList(recordImported);
            matchRecords(new ArrayList<>(recordsImported), expected);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void coreImportMetadataFindMatchingRecordsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        org.dspace.content.Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .build();

        Item testItem = ItemBuilder.createItem(context, col1)
                .withTitle("test item")
                .withIssueDate("2021")
                .build();

        context.restoreAuthSystemState();
        coreServiceImpl.findMatchingRecords(testItem);
    }

    private ArrayList<ImportRecord> getExpectedRecordsFromCoreTest() {
        ArrayList<ImportRecord> records = new ArrayList<>();

        List<MetadatumDTO> m1 = new ArrayList<>();

        m1.add(createMetadatumDTO("dc", "title", null,
                "Upper and lower bounds on resonances for manifolds hyperbolic near\n  infinity"));

        m1.add(createMetadatumDTO("dc", "contributor", "author", "David Borthwick"));
        m1.add(createMetadatumDTO("dc", "contributor", "author", "David Borthwick"));

        m1.add(createMetadatumDTO("dc", "date", "issued", "2008"));

        m1.add(createMetadatumDTO("dc", "identifier", "other", "569623"));

        m1.add(createMetadatumDTO("dc", "description", "abstract", "test"));

        m1.add(createMetadatumDTO("dc", "type", null, "research"));

        m1.add(createMetadatumDTO("dc", "identifier", "uri",
                "http://arxiv.org/abs/0710.3894"));

        records.add(new ImportRecord(m1));

        List<MetadatumDTO> m2 = new ArrayList<>();

        m2.add(createMetadatumDTO("dc", "title", null,
                "Distribution of interstitial stem cells in Hydra"));

        String[] authors2 = new String[] {
                "Bode","Bode","Bode","Bode","Campbell","David","David","David","David","David","David","David",
                "Diehl","Gierer","Herlands","Sproull","Yaross"
        };
        for (String a : authors2) {
            m2.add(createMetadatumDTO("dc", "contributor", "author", a));
        }

        m2.add(createMetadatumDTO("dc", "date", "issued", "1979"));

        m2.add(createMetadatumDTO("dc", "identifier", "doi", "10.1016/0012-1606(80)90370-x"));

        m2.add(createMetadatumDTO("dc", "identifier", "other", "5226210"));

        m2.add(createMetadatumDTO("dc", "description", "abstract", "test"));

        m2.add(createMetadatumDTO("dc", "publisher", null, "'Elsevier BV'"));

        m2.add(createMetadatumDTO("dc", "type", null, "research"));

        m2.add(createMetadatumDTO("dc", "identifier", "uri",
                "https://core.ac.uk/download/12164257.pdf"));

        records.add(new ImportRecord(m2));

        return records;
    }



    private ArrayList<ImportRecord> getExpectedRecordFromCoreById() {

        ArrayList<ImportRecord> records = new ArrayList<>();

        List<MetadatumDTO> md = new ArrayList<>();

        md.add(createMetadatumDTO("dc", "title", null, "Theoretical Status of Pentaquarks"));

        md.add(createMetadatumDTO("dc", "contributor", "author", "Doi, Takumi"));

        md.add(createMetadatumDTO("dc", "date", "issued", "2007"));

        md.add(createMetadatumDTO("dc", "identifier", "doi", "10.1143/ptps.168.45"));

        md.add(createMetadatumDTO("dc", "identifier", "other", "542484"));

        md.add(createMetadatumDTO("dc", "description", "abstract", "test"));

        md.add(createMetadatumDTO("dc", "publisher", null,
                "'Japan Society of Applied Physics'"));

        md.add(createMetadatumDTO("dc", "type", null, "research"));

        md.add(createMetadatumDTO("dc", "identifier", "uri",
                "http://arxiv.org/abs/0704.0959"));

        records.add(new ImportRecord(md));

        return records;
    }



}
