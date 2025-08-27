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
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.viaf.ViafImportMetadataSourceServiceImpl;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link ViafImportMetadataSourceServiceImpl}
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class ViafImportMetadataSourceServiceImplIT extends AbstractLiveImportIntegrationTest {

    @Autowired
    private LiveImportClientImpl liveImportClientImpl;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private ViafImportMetadataSourceServiceImpl viafService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        String[] preferSources = {"ICCU", "DNB"};
        configurationService.setProperty("viaf.prefer.sources", preferSources);
        context.restoreAuthSystemState();
    }

    @Test
    public void searchByViafIdUNIMARCtypeTest() throws Exception {
        List<MetadatumDTO> metadatums  = new ArrayList<>();
        MetadatumDTO identifierOther = createMetadatumDTO("person", "identifier", null, "24658555");
        MetadatumDTO title = createMetadatumDTO("dc", "title", null, "Albergati Capacelli , Francesco");
        MetadatumDTO gender = createMetadatumDTO("glamperson", "gender", null, "Male");
        MetadatumDTO birthDate = createMetadatumDTO("person", "birthDate", null, "1728-04-19");
        MetadatumDTO deathDate = createMetadatumDTO("glamperson", "deathDate", null, "1804-03-16");
        MetadatumDTO birthYear = createMetadatumDTO("glamperson", "birthYear", null, "1728");
        MetadatumDTO deathYear = createMetadatumDTO("glamperson", "deathYear", null, "1804");
        MetadatumDTO link = createMetadatumDTO("glam", "link", "viaf", "http://viaf.org/viaf/24658555");
        MetadatumDTO wikipedia1 = createMetadatumDTO("glam", "link", "wikipedia",
                                               "https://en.wikipedia.org/wiki/Francesco_Albergati_Capacelli");
        MetadatumDTO wikipedia2 = createMetadatumDTO("glam", "link", "wikipedia",
                                               "https://it.wikipedia.org/wiki/Francesco_Albergati_Capacelli");
        MetadatumDTO nationality = createMetadatumDTO("person", "nationality", null, "IT");
        metadatums.add(identifierOther);
        metadatums.add(title);
        metadatums.add(gender);
        metadatums.add(birthDate);
        metadatums.add(birthYear);
        metadatums.add(deathDate);
        metadatums.add(deathYear);
        metadatums.add(link);
        metadatums.add(wikipedia1);
        metadatums.add(wikipedia2);
        metadatums.add(nationality);
        ImportRecord record2match = new ImportRecord(metadatums);

        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try (InputStream viafResponceIS = getClass().getResourceAsStream("viaf-findByIdResponce.json")) {

            String viafResp = IOUtils.toString(viafResponceIS, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(viafResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            ImportRecord importedRecord = viafService.getRecord("24658555");
            assertNotNull(importedRecord);
            matchRecord(importedRecord, record2match);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void searchByViafIdMARC21typeTest() throws Exception {
        List<MetadatumDTO> metadatums  = new ArrayList<>();
        MetadatumDTO identifierOther = createMetadatumDTO("person", "identifier", null, "9999159477794927990009");
        MetadatumDTO title = createMetadatumDTO("dc", "title", null, "Sassi, Francesco");
        MetadatumDTO gender = createMetadatumDTO("glamperson", "gender", null, "Undefined");
        MetadatumDTO link = createMetadatumDTO("glam", "link", "viaf", "http://viaf.org/viaf/9999159477794927990009");
        metadatums.add(identifierOther);
        metadatums.add(title);
        metadatums.add(gender);
        metadatums.add(link);
        ImportRecord record2match = new ImportRecord(metadatums);

        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try (InputStream viafResponceIS = getClass().getResourceAsStream("viaf-findByIdMARC21Responce.json")) {

            String viafResp = IOUtils.toString(viafResponceIS, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response = mockResponse(viafResp, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            context.restoreAuthSystemState();
            ImportRecord importedRecord = viafService.getRecord("9999159477794927990009");
            assertNotNull(importedRecord);
            matchRecord(importedRecord, record2match);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void searchByNameTest() throws Exception {
        ArrayList<ImportRecord> records = new ArrayList<>();
        List<MetadatumDTO> metadatums  = new ArrayList<>();
        MetadatumDTO identifierOther = createMetadatumDTO("person", "identifier", null, "8441159477949227990009");
        MetadatumDTO title = createMetadatumDTO("dc", "title", null,
                                          "Hohenlohe-Waldenburg-Schillingsfürst, Carl Albrecht I.");
        MetadatumDTO gender = createMetadatumDTO("glamperson", "gender", null, "Male");
        MetadatumDTO birthDate = createMetadatumDTO("person", "birthDate", null, "1719-09-22");
        MetadatumDTO deathDate = createMetadatumDTO("glamperson", "deathDate", null, "1793-01-25");
        MetadatumDTO birthYear = createMetadatumDTO("glamperson", "birthYear", null, "1719");
        MetadatumDTO deathYear = createMetadatumDTO("glamperson", "deathYear", null, "1793");
        MetadatumDTO link = createMetadatumDTO("glam", "link", "viaf", "http://viaf.org/viaf/8441159477949227990009");
        MetadatumDTO wikipedia = createMetadatumDTO("glam", "link", "wikipedia",
                                              "https://it.wikipedia.org/wiki/Carlo_Alberto_I_di_Hohenlohe-Waldenburg-Schillingsfürst");
        MetadatumDTO nationality = createMetadatumDTO("person", "nationality", null, "DE");
        metadatums.add(identifierOther);
        metadatums.add(title);
        metadatums.add(gender);
        metadatums.add(birthDate);
        metadatums.add(birthYear);
        metadatums.add(deathDate);
        metadatums.add(deathYear);
        metadatums.add(link);
        metadatums.add(wikipedia);
        metadatums.add(nationality);
        records.add(new ImportRecord(metadatums));

        List<MetadatumDTO> metadatums2  = new ArrayList<>();
        MetadatumDTO identifierOther2 = createMetadatumDTO("person", "identifier", null, "7646174414001308700008");
        MetadatumDTO title2 = createMetadatumDTO("dc", "title", null, "Farina, Carlo");
        MetadatumDTO gender2 = createMetadatumDTO("glamperson", "gender", null, "Male");
        MetadatumDTO deathDate2 = createMetadatumDTO("glamperson", "deathDate", null, "1639");
        MetadatumDTO deathYear2 = createMetadatumDTO("glamperson", "deathYear", null, "1639");
        MetadatumDTO link2 = createMetadatumDTO("glam", "link", "viaf", "http://viaf.org/viaf/7646174414001308700008");
        MetadatumDTO nationality2 = createMetadatumDTO("person", "nationality", null, "IT");
        metadatums2.add(identifierOther2);
        metadatums2.add(title2);
        metadatums2.add(gender2);
        metadatums2.add(deathDate2);
        metadatums2.add(deathYear2);
        metadatums2.add(link2);
        metadatums2.add(nationality2);
        records.add(new ImportRecord(metadatums2));

        List<MetadatumDTO> metadatums3  = new ArrayList<>();
        MetadatumDTO identifierOther3 = createMetadatumDTO("person", "identifier", null, "7196150325547210090003");
        MetadatumDTO title3 = createMetadatumDTO("dc", "title", null, "Leo I");
        MetadatumDTO gender3 = createMetadatumDTO("glamperson", "gender", null, "Male");
        MetadatumDTO birthDate3 = createMetadatumDTO("person", "birthDate", null, "1806-12-13");
        MetadatumDTO deathDate3 = createMetadatumDTO("glamperson", "deathDate", null, "1881-04-14");
        MetadatumDTO birthYear3 = createMetadatumDTO("glamperson", "birthYear", null, "1806");
        MetadatumDTO deathYear3 = createMetadatumDTO("glamperson", "deathYear", null, "1881");
        MetadatumDTO link3 = createMetadatumDTO("glam", "link", "viaf", "http://viaf.org/viaf/7196150325547210090003");
        MetadatumDTO wikipedia2 = createMetadatumDTO("glam", "link", "wikipedia",
                                               "https://en.wikipedia.org/wiki/Charles_Léon");
        MetadatumDTO wikipedia3 = createMetadatumDTO("glam", "link", "wikipedia",
                                               "https://it.wikipedia.org/wiki/Carlo_Leone_Denuelle");
        MetadatumDTO nationality3 = createMetadatumDTO("person", "nationality", null, "IT");
        MetadatumDTO role = createMetadatumDTO("glamperson", "role", null, "<papa>");
        metadatums3.add(identifierOther3);
        metadatums3.add(title3);
        metadatums3.add(gender3);
        metadatums3.add(birthDate3);
        metadatums3.add(birthYear3);
        metadatums3.add(deathDate3);
        metadatums3.add(deathYear3);
        metadatums3.add(link3);
        metadatums3.add(wikipedia2);
        metadatums3.add(wikipedia3);
        metadatums3.add(nationality3);
        metadatums3.add(role);
        records.add(new ImportRecord(metadatums3));

        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try (InputStream viafResponceIS = getClass().getResourceAsStream("viaf-searchByName.json");
             InputStream carlo1IS = getClass().getResourceAsStream("viaf-Carlo-responce1.json");
             InputStream carlo2IS = getClass().getResourceAsStream("viaf-Carlo-responce2.json");
             InputStream carlo3IS = getClass().getResourceAsStream("viaf-Carlo-responce3.json")) {

            String viafSearchByNameResp = IOUtils.toString(viafResponceIS, Charset.defaultCharset());
            String viafEnrico1Resp = IOUtils.toString(carlo1IS, Charset.defaultCharset());
            String viafEnrico2Resp = IOUtils.toString(carlo2IS, Charset.defaultCharset());
            String viafEnrico3Resp = IOUtils.toString(carlo3IS, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);
            CloseableHttpResponse response1 = mockResponse(viafSearchByNameResp, 200, "OK");
            CloseableHttpResponse response2 = mockResponse(viafEnrico1Resp, 200, "OK");
            CloseableHttpResponse response3 = mockResponse(viafEnrico2Resp, 200, "OK");
            CloseableHttpResponse response4 = mockResponse(viafEnrico3Resp, 200, "OK");

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response1, response2, response3, response4);

            context.restoreAuthSystemState();
            Collection<ImportRecord> importedRecords = viafService.getRecords("Carlo", 1, 3);
            assertEquals(3, importedRecords.size());
            matchRecords(new ArrayList<>(importedRecords), records);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void viafGetRecordsCount() throws Exception {
        context.turnOffAuthorisationSystem();
        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        try (InputStream viafResponceIS = getClass().getResourceAsStream("viaf-searchByName.json")) {

            String viafSearchByNameResp = IOUtils.toString(viafResponceIS, Charset.defaultCharset());
            CloseableHttpResponse response1 = mockResponse(viafSearchByNameResp, 200, "OK");

            liveImportClientImpl.setHttpClient(httpClient);
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response1);

            context.restoreAuthSystemState();
            int numberOfRecords = viafService.getRecordsCount("Carlo");
            assertEquals(634, numberOfRecords);
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

}
