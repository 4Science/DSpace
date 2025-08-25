/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.viaf.ViafImportMetadataSourceServiceImpl;
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
    private ViafImportMetadataSourceServiceImpl viafService;

    @Test
    public void searchByViafIdUNIMARCtypeTest() throws Exception {
        List<MetadatumDTO> metadatums  = new ArrayList<>();
        MetadatumDTO identifierOther = createMetadatumDTO("dc", "identifier", "other", "24658555");
        MetadatumDTO title = createMetadatumDTO("dc", "title", null, "Albergati Capacelli , Francesco");
        MetadatumDTO gender = createMetadatumDTO("glamperson", "gender", null, "Male");
        MetadatumDTO birthDate = createMetadatumDTO("person", "birthDate", null, "1728-04-19");
        MetadatumDTO deathDate = createMetadatumDTO("glamperson", "deathDate", null, "1804-03-16");
        metadatums.add(identifierOther);
        metadatums.add(title);
        metadatums.add(gender);
        metadatums.add(birthDate);
        metadatums.add(deathDate);
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
        MetadatumDTO identifierOther = createMetadatumDTO("dc", "identifier", "other", "9999159477794927990009");
        MetadatumDTO title = createMetadatumDTO("dc", "title", null, "Sassi, Francesco");
        MetadatumDTO gender = createMetadatumDTO("glamperson", "gender", null, "Undefined");
        metadatums.add(identifierOther);
        metadatums.add(title);
        metadatums.add(gender);
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

}
