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
import javax.el.MethodNotFoundException;

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

            ImportRecord recordImported = coreServiceImpl.getRecord("542484");
            assertNotNull(recordImported);

            Collection<ImportRecord> recordsImported = Arrays.asList(recordImported);
            matchRecords(new ArrayList<>(recordsImported), expected);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    /**
     * Verifies that a DOI search via getRecords() can return more than one result.
     *
     * CORE is an aggregator: it harvests the same paper from multiple independent data providers
     * (institutional repositories, publishers, preprint servers). Each ingestion creates a separate
     * CORE "work" entity with its own numeric CORE ID, even when all copies share the same DOI.
     * As a result, /search/works?q=doi:"..." may return N records — one per data provider.
     *
     * This is the fundamental reason why getRecord(String doi) is unsafe for DOI-based lookups:
     * the DOI does not uniquely identify a single CORE work.
     *
     * In this fixture DOI 10.1177/0004563220921888 maps to two distinct CORE works:
     * - CORE ID 8387107  : harvested from Westminster Research (institutional repository),
     *                      full-text PDF available
     * - CORE ID 246897624: harvested from the publisher feed via Crossref,
     *                      metadata-only (empty downloadUrl, no documentType field)
     */
    @Test
    public void coreImportMetadataGetRecordsByDoiTest() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try (InputStream coreResp = getClass().getResourceAsStream("core-by-doi.json")) {
            String coreRespStr = IOUtils.toString(coreResp, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(coreRespStr, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();

            ArrayList<ImportRecord> expected = getExpectedRecordsByDoi();
            Collection<ImportRecord> recordsImported = coreServiceImpl.getRecords("10.1177/0004563220921888", 0, 10);

            assertEquals(2, recordsImported.size());
            matchRecords(new ArrayList<>(recordsImported), expected);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    @Test(expected = MethodNotFoundException.class)
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
        m1.add(createMetadatumDTO("dc", "identifier", "uri", "http://arxiv.org/abs/0710.3894"));

        records.add(new ImportRecord(m1));

        List<MetadatumDTO> m2 = new ArrayList<>();

        m2.add(createMetadatumDTO("dc", "title", null, "Distribution of interstitial stem cells in Hydra"));

        String[] authors2 = new String[] { "Bode", "Bode", "Bode", "Bode", "Campbell", "David", "David", "David",
                                           "David", "David", "David", "David", "Diehl", "Gierer", "Herlands", "Sproull",
                                           "Yaross"
                                         };
        Arrays.stream(authors2)
              .forEach(author -> m2.add(createMetadatumDTO("dc", "contributor", "author", author)));

        m2.add(createMetadatumDTO("dc", "date", "issued", "1979"));
        m2.add(createMetadatumDTO("dc", "identifier", "doi", "10.1016/0012-1606(80)90370-x"));
        m2.add(createMetadatumDTO("dc", "identifier", "other", "5226210"));
        m2.add(createMetadatumDTO("dc", "description", "abstract", "test"));
        m2.add(createMetadatumDTO("dc", "publisher", null, "'Elsevier BV'"));
        m2.add(createMetadatumDTO("dc", "type", null, "research"));
        m2.add(createMetadatumDTO("dc", "identifier", "uri", "https://core.ac.uk/download/12164257.pdf"));

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
        md.add(createMetadatumDTO("dc", "description", "abstract",
                "We review the current status of the theoretical pentaquark search from the direct QCD calculation."));
        md.add(createMetadatumDTO("dc", "publisher", null, "'Japan Society of Applied Physics'"));
        md.add(createMetadatumDTO("dc", "type", null, "research"));
        md.add(createMetadatumDTO("dc", "identifier", "uri", "http://arxiv.org/abs/0704.0959"));

        records.add(new ImportRecord(md));
        return records;
    }

    private ArrayList<ImportRecord> getExpectedRecordsByDoi() {
        ArrayList<ImportRecord> records = new ArrayList<>();

        // Record 1 — CORE ID 8387107, harvested from Westminster Research (institutional repo)
        List<MetadatumDTO> r1 = new ArrayList<>();
        r1.add(createMetadatumDTO("dc", "title", null,
                "Clinical utility of cardiac troponin measurement in COVID-19 infection"));
        r1.add(createMetadatumDTO("dc", "contributor", "author", "Gaze, D.C."));
        r1.add(createMetadatumDTO("dc", "contributor", "author", "Gaze, D.C."));
        r1.add(createMetadatumDTO("dc", "date", "issued", "2020"));
        r1.add(createMetadatumDTO("dc", "identifier", "doi", "10.1177/0004563220921888"));
        r1.add(createMetadatumDTO("dc", "identifier", "other", "8387107"));
        r1.add(createMetadatumDTO("dc", "description", "abstract",
                "The novel coronavirus SARS-CoV-2 causes the disease COVID-19, a severe acute respiratory syndrome."));
        r1.add(createMetadatumDTO("dc", "publisher", null, "'SAGE Publications'"));
        r1.add(createMetadatumDTO("dc", "type", null, "research"));
        r1.add(createMetadatumDTO("dc", "identifier", "uri", "https://core.ac.uk/download/305119246.pdf"));
        records.add(new ImportRecord(r1));

        // Record 2 — CORE ID 246897624, harvested from publisher feed (Crossref).
        // documentType is absent in the API response → dc.type not mapped.
        // downloadUrl is empty string → dc.identifier.uri not mapped (skipped by isNotBlank check).
        List<MetadatumDTO> r2 = new ArrayList<>();
        r2.add(createMetadatumDTO("dc", "title", null,
                "Clinical utility of cardiac troponin measurement in COVID-19 infection"));
        r2.add(createMetadatumDTO("dc", "contributor", "author", "David C Gaze"));
        r2.add(createMetadatumDTO("dc", "date", "issued", "2020"));
        r2.add(createMetadatumDTO("dc", "identifier", "doi", "10.1177/0004563220921888"));
        r2.add(createMetadatumDTO("dc", "identifier", "other", "246897624"));
        r2.add(createMetadatumDTO("dc", "description", "abstract",
                " The novel coronavirus SARS-CoV-2 causes the disease COVID-19, a severe acute respiratory syndrome."));
        r2.add(createMetadatumDTO("dc", "publisher", null, "SAGE Publications"));
        records.add(new ImportRecord(r2));

        return records;
    }

}
