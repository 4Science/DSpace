/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.unpaywall.service.impl;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import joptsimple.internal.Strings;
import org.apache.commons.io.IOUtils;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.tools.ant.filters.StringInputStream;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.unpaywall.model.Unpaywall;
import org.dspace.unpaywall.model.UnpaywallStatus;
import org.dspace.unpaywall.service.UnpaywallService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/**
 * Integration tests for {@link UnpaywallServiceImpl}.
 */
public class UnpaywallServiceImplIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_UNPAYWALL_DIR_PATH = "org/dspace/app/unpaywall/";

    private final UnpaywallService unpaywallService = ContentServiceFactory.getInstance().getUnpaywallService();

    @After
    public void after() throws SQLException, AuthorizeException {
        List<Unpaywall> unpaywallRecords = unpaywallService.findAll(context);
        for (Unpaywall unpaywall : unpaywallRecords) {
            unpaywallService.delete(context, unpaywall);
        }
    }

    @Test
    public void testFindUnpaywall() {
        UUID item1Id = UUID.randomUUID();
        String doi1 = "testDoi1";
        Unpaywall unpaywall1 = new Unpaywall();
        unpaywall1.setItemId(item1Id);
        unpaywall1.setDoi(doi1);
        unpaywall1 = unpaywallService.create(context, unpaywall1);

        Unpaywall unpaywall2 = new Unpaywall();
        unpaywall2.setItemId(UUID.randomUUID());
        unpaywall2.setDoi("testDoi2");
        unpaywallService.create(context, unpaywall2);

        Optional<Unpaywall> unpaywall = unpaywallService.findUnpaywall(context, doi1, item1Id);

        assertTrue(unpaywall.isPresent());
        Assert.assertEquals(unpaywall1.getID(), unpaywall.get().getID());
    }

    @Test
    public void testFindAll() {
        Unpaywall unpaywall1 = new Unpaywall();
        unpaywall1.setItemId(UUID.randomUUID());
        unpaywall1.setDoi("testDoi1");
        unpaywall1 = unpaywallService.create(context, unpaywall1);

        Unpaywall unpaywall2 = new Unpaywall();
        unpaywall2.setItemId(UUID.randomUUID());
        unpaywall2.setDoi("testDoi2");
        unpaywallService.create(context, unpaywall2);

        List<Unpaywall> unpaywallRecords = unpaywallService.findAll(context);

        Assert.assertEquals(2, unpaywallRecords.size());
        Assert.assertEquals(unpaywall1.getID(), unpaywallRecords.get(0).getID());
        Assert.assertEquals(unpaywall2.getID(), unpaywallRecords.get(1).getID());
    }

    @Test
    public void testDelete() {
        UUID itemId = UUID.randomUUID();
        String doi = "testDoi1";
        Unpaywall unpaywall = new Unpaywall();
        unpaywall.setItemId(itemId);
        unpaywall.setDoi(doi);
        unpaywall = unpaywallService.create(context, unpaywall);

        unpaywallService.delete(context, unpaywall);

        Optional<Unpaywall> result = unpaywallService.findUnpaywall(context, doi, itemId);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testInitUnpaywallCallSuccessful()
            throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        UUID itemId = UUID.randomUUID();
        String doi = "10.1504/ijmso.2012.048507";

        context.turnOffAuthorisationSystem();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        mockHttpClient(unpaywallService, httpClient);

        String responseFileName = BASE_UNPAYWALL_DIR_PATH.concat("unpaywall-api-response.json");
        try (InputStream unpaywallResponseStream = getClass().getClassLoader().getResourceAsStream(responseFileName)) {
            String unpaywallResponse = IOUtils.toString(unpaywallResponseStream, Charset.defaultCharset());
            CloseableHttpResponse response = mockResponse(unpaywallResponse, SC_OK, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);
            context.restoreAuthSystemState();

            unpaywallService.initUnpaywallCall(context, doi, itemId);
            assertTrue(isRequestInitialized(unpaywallService));
            Thread.sleep(1000);

            Optional<Unpaywall> unpaywall = unpaywallService.findUnpaywall(context, doi, itemId);
            assertTrue(unpaywall.isPresent());
            Assert.assertEquals(UnpaywallStatus.SUCCESSFUL, unpaywall.get().getStatus());
            Assert.assertEquals(unpaywallResponse, unpaywall.get().getJsonRecord());
            verify(httpClient).execute(any());
        }
    }

    @Test
    public void testInitUnpaywallCallNotFound()
            throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        UUID itemId = UUID.randomUUID();
        String doi = "10.1504/ijmso.2012.048507";

        context.turnOffAuthorisationSystem();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        mockHttpClient(unpaywallService, httpClient);
        CloseableHttpResponse response = mockResponse(Strings.EMPTY, SC_NOT_FOUND, "NOT_FOUND");
        when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);
        context.restoreAuthSystemState();

        unpaywallService.initUnpaywallCall(context, doi, itemId);
        assertTrue(isRequestInitialized(unpaywallService));
        Thread.sleep(1000);

        Optional<Unpaywall> unpaywall = unpaywallService.findUnpaywall(context, doi, itemId);
        assertTrue(unpaywall.isPresent());
        Assert.assertEquals(UnpaywallStatus.NOT_FOUND, unpaywall.get().getStatus());
        Assert.assertNull(unpaywall.get().getJsonRecord());
        verify(httpClient).execute(any());
    }

    @Test
    public void testInitUnpaywallCallIfNeededWithInitialization()
            throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        UUID itemId = UUID.randomUUID();
        String doi = "10.1504/ijmso.2012.048507";

        context.turnOffAuthorisationSystem();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        mockHttpClient(unpaywallService, httpClient);

        String responseFileName = BASE_UNPAYWALL_DIR_PATH.concat("unpaywall-api-response.json");
        try (InputStream unpaywallResponseStream = getClass().getClassLoader().getResourceAsStream(responseFileName)) {
            String unpaywallResponse = IOUtils.toString(unpaywallResponseStream, Charset.defaultCharset());
            CloseableHttpResponse response = mockResponse(unpaywallResponse, SC_OK, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);
            context.restoreAuthSystemState();

            unpaywallService.initUnpaywallCallIfNeeded(context, doi, itemId);
            assertTrue(isRequestInitialized(unpaywallService));
            Thread.sleep(1000);

            Optional<Unpaywall> unpaywall = unpaywallService.findUnpaywall(context, doi, itemId);
            assertTrue(unpaywall.isPresent());
            Assert.assertEquals(UnpaywallStatus.SUCCESSFUL, unpaywall.get().getStatus());
            Assert.assertEquals(unpaywallResponse, unpaywall.get().getJsonRecord());
            verify(httpClient, times(1)).execute(any());
        }
    }

    @Test
    public void testInitUnpaywallCallIfNeededWithoutInitialization()
            throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        UUID itemId = UUID.randomUUID();
        String doi = "10.1504/ijmso.2012.048507";

        context.turnOffAuthorisationSystem();
        Unpaywall newUnpaywall = new Unpaywall();
        newUnpaywall.setDoi(doi);
        newUnpaywall.setItemId(itemId);
        newUnpaywall.setStatus(UnpaywallStatus.SUCCESSFUL);
        unpaywallService.create(context, newUnpaywall);

        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        mockHttpClient(unpaywallService, httpClient);
        context.restoreAuthSystemState();

        unpaywallService.initUnpaywallCallIfNeeded(context, doi, itemId);
        assertFalse(isRequestInitialized(unpaywallService));
        Thread.sleep(1000);

        Optional<Unpaywall> unpaywall = unpaywallService.findUnpaywall(context, doi, itemId);
        assertTrue(unpaywall.isPresent());
        Assert.assertEquals(UnpaywallStatus.SUCCESSFUL, unpaywall.get().getStatus());
        verify(httpClient, times(0)).execute(any());
    }

    private static CloseableHttpResponse mockResponse(String xmlExample, int statusCode, String reason) {
        BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
        basicHttpEntity.setChunked(true);
        basicHttpEntity.setContent(new StringInputStream(xmlExample));

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(statusLine(statusCode, reason));
        when(response.getEntity()).thenReturn(basicHttpEntity);
        return response;
    }

    private static StatusLine statusLine(int statusCode, String reason) {
        return new StatusLine() {
            @Override
            public ProtocolVersion getProtocolVersion() {
                return new ProtocolVersion("http", 1, 1);
            }

            @Override
            public int getStatusCode() {
                return statusCode;
            }

            @Override
            public String getReasonPhrase() {
                return reason;
            }
        };
    }

    private static void mockHttpClient(UnpaywallService unpaywallService, CloseableHttpClient mock)
            throws NoSuchFieldException, IllegalAccessException {
        Class<?> objectClass = unpaywallService.getClass();
        Field propertyField = objectClass.getDeclaredField("client");
        propertyField.setAccessible(true);
        propertyField.set(unpaywallService, mock);
        propertyField.setAccessible(false);
    }

    private static boolean isRequestInitialized(UnpaywallService unpaywallService)
            throws NoSuchFieldException, IllegalAccessException {
        Class<?> myClass = unpaywallService.getClass();
        Field myField;
        myField = myClass.getDeclaredField("requestMap");
        myField.setAccessible(true);
        Map<String, CompletableFuture<Void>> requests =
                (Map<String, CompletableFuture<Void>>) myField.get(unpaywallService);
        myField.setAccessible(false);
        return !requests.isEmpty();
    }
}
