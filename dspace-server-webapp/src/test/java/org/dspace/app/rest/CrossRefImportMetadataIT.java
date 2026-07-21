/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.importer.external.crossref.CrossRefImportMetadataSourceServiceImpl;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

public class CrossRefImportMetadataIT extends AbstractControllerIntegrationTest {

    @MockitoBean
    private LiveImportClient liveImportClient;

    @Autowired
    @Qualifier("CrossRefImportService")
    private CrossRefImportMetadataSourceServiceImpl crossRefImportService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        ReflectionTestUtils.setField(crossRefImportService, "liveImportClient", liveImportClient);
    }

    @Test
    public void importMetadataFromCrossrefByDoiTest() throws Exception {
        try (InputStream file = getClass().getResourceAsStream("crossRef-doi-food.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());
            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            getClient().perform(get("/api/integration/externalsources/crossref/entries")
                       .param("query", "10.1111/jfbc.13557"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].id",
                                        is("10.1111/jfbc.13557")))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].display",
                                        is("Food‐derived antioxidants and COVID‐19")))
                       .andExpect(jsonPath("$.page.totalElements", is(1)));
            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }

    @Test
    public void importMetadataFromCrossrefByFreeTextTest() throws Exception {
        try (InputStream file = getClass().getResourceAsStream("crossRef-test.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());
            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            getClient().perform(get("/api/integration/externalsources/crossref/entries")
                       .param("query", "1.11/jfbc.1"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$.page.totalElements", greaterThan(1)));
            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }

}
