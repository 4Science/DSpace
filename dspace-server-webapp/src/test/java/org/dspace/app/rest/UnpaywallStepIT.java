/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.List;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.submit.step.UnpaywallStep;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.unpaywall.model.Unpaywall;
import org.dspace.unpaywall.model.UnpaywallStatus;
import org.dspace.unpaywall.service.UnpaywallService;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/**
 * Integration tests for {@link UnpaywallStep}.
 */
public class UnpaywallStepIT extends AbstractLiveImportIntegrationTest {

    private static final String BASE_UNPAYWALL_DIR_PATH = "org/dspace/app/unpaywall/";

    private final UnpaywallService unpaywallService = ContentServiceFactory.getInstance().getUnpaywallService();

    private final ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();

    private Collection collection;

    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .withEntityType("Publication")
                .withSubmissionDefinition("traditional-with-unpaywall")
                .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void testCallingUnpaywallApi() throws Exception {

        configurationService.setProperty("unpaywall.email", "test@mail.com");
        context.turnOffAuthorisationSystem();

        String testApiResponse = getUnpaywallResponse();
        String doi = "10.1504/ijmso.2012.048507";
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Test WorkspaceItem")
                .withIssueDate("2020")
                .withDoiIdentifier(doi)
                .build();

        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        mockHttpClient(unpaywallService, httpClient);
        CloseableHttpResponse httpResponse = mockResponse(testApiResponse, SC_OK, "OK");
        when(httpClient.execute(ArgumentMatchers.any())).thenReturn(httpResponse);

        context.restoreAuthSystemState();

        Operation addOperation = new AddOperation("/sections/unpaywall/refresh", false);

        getClient(getAuthToken(eperson.getEmail(), password))
                .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                        .content(getPatchContent(List.of(addOperation)))
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[*].paths", not(containsString("/section/unpaywall"))))
                .andExpect(jsonPath("$.sections.unpaywall.doi", is(doi)))
                .andExpect(jsonPath("$.sections.unpaywall.itemId", is(workspaceItem.getItem().getID().toString())))
                .andExpect(jsonPath("$.sections.unpaywall.timestampCreated", notNullValue()))
                .andExpect(jsonPath("$.sections.unpaywall.timestampLastModified", notNullValue()))
                .andExpect(jsonPath("$.sections.unpaywall.status", is(UnpaywallStatus.SUCCESSFUL.name())));
        verify(httpClient, times(1)).execute(any());
        configurationService.setProperty("unpaywall.email", null);
    }

    @Test
    public void testCallingUnpaywallApiWithRefresh() throws Exception {

        configurationService.setProperty("unpaywall.email", "test@mail.com");
        context.turnOffAuthorisationSystem();

        String testApiResponse = getUnpaywallResponse();
        String doi = "10.1504/ijmso.2012.048507";
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Test WorkspaceItem")
                .withIssueDate("2020")
                .withDoiIdentifier(doi)
                .build();

        Unpaywall unpaywall = new Unpaywall();
        unpaywall.setDoi(doi);
        unpaywall.setItemId(workspaceItem.getItem().getID());
        unpaywall.setStatus(UnpaywallStatus.NOT_FOUND);
        unpaywall.setJsonRecord(null);
        unpaywall = unpaywallService.create(context, unpaywall);

        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        mockHttpClient(unpaywallService, httpClient);
        CloseableHttpResponse httpResponse = mockResponse(testApiResponse, SC_OK, "OK");
        when(httpClient.execute(ArgumentMatchers.any())).thenReturn(httpResponse);

        context.restoreAuthSystemState();

        Operation addOperation = new AddOperation("/sections/unpaywall/refresh", true);

        getClient(getAuthToken(eperson.getEmail(), password))
                .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                        .content(getPatchContent(List.of(addOperation)))
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[*].paths", not(containsString("/section/unpaywall"))))
                .andExpect(jsonPath("$.sections.unpaywall.doi", is(doi)))
                .andExpect(jsonPath("$.sections.unpaywall.itemId", is(workspaceItem.getItem().getID().toString())))
                .andExpect(jsonPath("$.sections.unpaywall.timestampCreated", notNullValue()))
                .andExpect(jsonPath("$.sections.unpaywall.timestampLastModified", notNullValue()))
                .andExpect(jsonPath("$.sections.unpaywall.status", is(UnpaywallStatus.SUCCESSFUL.name())));
        verify(httpClient, times(1)).execute(any());
        configurationService.setProperty("unpaywall.email", null);
    }

    @Test
    public void testCallingUnpaywallApiWithoutRefresh() throws Exception {

        configurationService.setProperty("unpaywall.email", "test@mail.com");
        context.turnOffAuthorisationSystem();

        String doi = "10.1504/ijmso.2012.048507";
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Test WorkspaceItem")
                .withIssueDate("2020")
                .withDoiIdentifier(doi)
                .build();

        Unpaywall unpaywall = new Unpaywall();
        unpaywall.setDoi(doi);
        unpaywall.setItemId(workspaceItem.getItem().getID());
        unpaywall.setStatus(UnpaywallStatus.NOT_FOUND);
        unpaywall.setJsonRecord(null);
        unpaywall = unpaywallService.create(context, unpaywall);

        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        mockHttpClient(unpaywallService, httpClient);

        context.restoreAuthSystemState();

        Operation addOperation = new AddOperation("/sections/unpaywall/refresh", false);

        getClient(getAuthToken(eperson.getEmail(), password))
                .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                        .content(getPatchContent(List.of(addOperation)))
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[*].paths", not(containsString("/section/unpaywall"))))
                .andExpect(jsonPath("$.sections.unpaywall.doi", is(doi)))
                .andExpect(jsonPath("$.sections.unpaywall.itemId", is(workspaceItem.getItem().getID().toString())))
                .andExpect(jsonPath("$.sections.unpaywall.timestampCreated", notNullValue()))
                .andExpect(jsonPath("$.sections.unpaywall.timestampLastModified", notNullValue()))
                .andExpect(jsonPath("$.sections.unpaywall.status", is(unpaywall.getStatus().name())));
        verify(httpClient, times(0)).execute(any());
        configurationService.setProperty("unpaywall.email", null);
    }

    @Test
    public void testCallingUnpaywallApiWithAccept() throws Exception {

        configurationService.setProperty("unpaywall.email", "test@mail.com");
        context.turnOffAuthorisationSystem();

        String testApiResponse = getUnpaywallResponse();
        String pdfUrl = new JSONObject(testApiResponse).getJSONObject("best_oa_location").getString("url_for_pdf");
        String doi = "10.1504/ijmso.2012.048507";
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle("Test WorkspaceItem")
            .withIssueDate("2020")
            .withDoiIdentifier(doi)
            .build();

        Unpaywall unpaywall = new Unpaywall();
        unpaywall.setDoi(doi);
        unpaywall.setItemId(workspaceItem.getItem().getID());
        unpaywall.setStatus(UnpaywallStatus.NOT_FOUND);
        unpaywall.setJsonRecord(null);
        unpaywall = unpaywallService.create(context, unpaywall);

        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        mockHttpClient(unpaywallService, httpClient);
        CloseableHttpResponse httpResponse = mockResponse(testApiResponse, SC_OK, "OK");
        when(httpClient.execute(ArgumentMatchers.any())).thenReturn(httpResponse);

        context.restoreAuthSystemState();

        Operation refreshOperation = new AddOperation("/sections/unpaywall/refresh", true);

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                .content(getPatchContent(List.of(refreshOperation)))
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors[*].paths", not(containsString("/section/unpaywall"))))
            .andExpect(jsonPath("$.sections.unpaywall.doi", is(doi)))
            .andExpect(jsonPath("$.sections.unpaywall.itemId", is(workspaceItem.getItem().getID().toString())))
            .andExpect(jsonPath("$.sections.unpaywall.timestampCreated", notNullValue()))
            .andExpect(jsonPath("$.sections.unpaywall.timestampLastModified", notNullValue()))
            .andExpect(jsonPath("$.sections.unpaywall.status", equalTo(UnpaywallStatus.SUCCESSFUL.name())));
        verify(httpClient, times(1)).execute(any());

        Operation acceptOperation = new AddOperation("/sections/unpaywall/accept", true);
        try (InputStream resource = getUnpaywallTestResource()) {
            CloseableHttpResponse pdfResponse = mockResponse(resource, SC_OK, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(pdfResponse);
            getClient(getAuthToken(eperson.getEmail(), password))
                .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                    .content(getPatchContent(List.of(acceptOperation)))
                    .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[*].paths", not(containsString("/section/unpaywall"))))
                .andExpect(jsonPath("$.sections.unpaywall.doi", is(doi)))
                .andExpect(jsonPath("$.sections.unpaywall.itemId", is(workspaceItem.getItem().getID().toString())))
                .andExpect(jsonPath("$.sections.unpaywall.timestampCreated", notNullValue()))
                .andExpect(jsonPath("$.sections.unpaywall.timestampLastModified", notNullValue()))
                .andExpect(jsonPath("$.sections.unpaywall.status", is(UnpaywallStatus.PENDING.name())));
            verify(httpClient, times(1)).execute(any());

            pauseSeconds(5);

            refreshOperation = new AddOperation("/sections/unpaywall/refresh", false);

            getClient(getAuthToken(eperson.getEmail(), password))
                .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                    .content(getPatchContent(List.of(refreshOperation)))
                    .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.sections.upload.files", notNullValue()))
                .andExpect(jsonPath("$.sections.upload.files[0].metadata", matchMetadata("dc.source", pdfUrl)))
                .andExpect(jsonPath("$.sections.unpaywall.doi", is(doi)))
                .andExpect(jsonPath("$.sections.unpaywall.itemId", is(workspaceItem.getItem().getID().toString())))
                .andExpect(jsonPath("$.sections.unpaywall.timestampCreated", notNullValue()))
                .andExpect(jsonPath("$.sections.unpaywall.timestampLastModified", notNullValue()))
                .andExpect(jsonPath("$.sections.unpaywall.status", equalTo(UnpaywallStatus.IMPORTED.name())));

        } finally {
            configurationService.setProperty("unpaywall.email", null);
        }
    }

    private static void mockHttpClient(UnpaywallService unpaywallService, CloseableHttpClient mock)
            throws NoSuchFieldException, IllegalAccessException {
        Class<?> objectClass = unpaywallService.getClass();
        Field propertyField = objectClass.getDeclaredField("client");
        propertyField.setAccessible(true);
        propertyField.set(unpaywallService, mock);
        propertyField.setAccessible(false);
    }

    private String getUnpaywallResponse() throws IOException {
        String responseFileName = BASE_UNPAYWALL_DIR_PATH.concat("unpaywall-api-response.json");
        try (InputStream unpaywallResponseStream = getClass().getClassLoader().getResourceAsStream(responseFileName)) {
            return IOUtils.toString(unpaywallResponseStream, Charset.defaultCharset());
        }
    }

    private InputStream getUnpaywallTestResource() throws IOException {
        String responseFileName = BASE_UNPAYWALL_DIR_PATH.concat("unpaywall-api-resource.pdf");
        return getClass().getClassLoader().getResourceAsStream(responseFileName);
    }

    private void pauseSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
