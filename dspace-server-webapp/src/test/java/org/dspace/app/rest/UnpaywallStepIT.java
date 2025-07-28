/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.core.MediaType;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.submit.step.UnpaywallStep;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.unpaywall.model.Unpaywall;
import org.dspace.unpaywall.model.UnpaywallStatus;
import org.dspace.unpaywall.service.UnpaywallClientAPI;
import org.dspace.unpaywall.service.impl.UnpaywallServiceImpl;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;

/**
 * Integration tests for {@link UnpaywallStep}.
 */
public class UnpaywallStepIT extends AbstractLiveImportIntegrationTest {

    private final UnpaywallServiceImpl unpaywallService =
        (UnpaywallServiceImpl) ContentServiceFactory.getInstance().getUnpaywallService();

    private Collection collection;

    private UnpaywallClientAPI origClientAPI;
    private UnpaywallClientAPI spyClientAPI;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        origClientAPI = unpaywallService.getUnpaywallClientAPI();
        spyClientAPI = spy(origClientAPI);
        unpaywallService.setUnpaywallClientAPI(spyClientAPI);

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

    @After
    public void after() throws SQLException, AuthorizeException {
        List<Unpaywall> unpaywallRecords = unpaywallService.findAll(context);
        for (Unpaywall unpaywall : unpaywallRecords) {
            unpaywallService.delete(context, unpaywall);
        }
        unpaywallService.setUnpaywallClientAPI(origClientAPI);
    }

    @Test
    public void testCallingUnpaywallApi() throws Exception {

        context.turnOffAuthorisationSystem();

        String doi = "10.1504/ijmso.2012.048507";
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                          .withTitle("Test WorkspaceItem")
                                                          .withIssueDate("2020")
                                                          .withDoiIdentifier(doi)
                                                          .build();
        UUID itemUUID = workspaceItem.getItem().getID();

        context.restoreAuthSystemState();

        Operation refreshOperation = new AddOperation("/sections/unpaywall/refresh", false);
        final String authToken = getAuthToken(eperson.getEmail(), password);
        AtomicReference<String> unpayStatus = new AtomicReference<>();
        int numRefresh = 0;
        do {
            pauseRefresh(numRefresh);
            getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                             .content(getPatchContent(List.of(refreshOperation)))
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[*].paths", not(containsString("/section/unpaywall"))))
                .andExpect(jsonPath("$.sections.unpaywall.doi", is(doi)))
                .andExpect(jsonPath("$.sections.unpaywall.itemId", is(itemUUID.toString())))
                .andExpect(jsonPath("$.sections.unpaywall.timestampCreated", notNullValue()))
                .andExpect(jsonPath("$.sections.unpaywall.timestampLastModified", notNullValue()))
                .andDo(result -> unpayStatus.set(read(result.getResponse().getContentAsString(),
                                                      "$.sections.unpaywall.status")));
            assertThat(unpayStatus.get(), Matchers.oneOf(UnpaywallStatus.PENDING.name(),
                                                         UnpaywallStatus.SUCCESSFUL.name()));
            numRefresh++;
        } while (!unpayStatus.get().equals(UnpaywallStatus.SUCCESSFUL.name()) && numRefresh < 3);

        assertThat(unpayStatus.get(), Matchers.equalTo(UnpaywallStatus.SUCCESSFUL.name()));
        verify(spyClientAPI, times(1)).callUnpaywallApi(any());
    }

    @Test
    public void testCallingUnpaywallApiWithRefresh() throws Exception {

        context.turnOffAuthorisationSystem();

        String doi = "10.1504/ijmso.2012.048507";
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                          .withTitle("Test WorkspaceItem")
                                                          .withIssueDate("2020")
                                                          .withDoiIdentifier(doi)
                                                          .build();
        final UUID itemUUID = workspaceItem.getItem().getID();
        Unpaywall unpaywall = new Unpaywall();
        unpaywall.setDoi(doi);
        unpaywall.setItemId(itemUUID);
        unpaywall.setStatus(UnpaywallStatus.NOT_FOUND);
        unpaywall.setJsonRecord(null);
        unpaywall = unpaywallService.create(context, unpaywall);
        context.commit();
        context.restoreAuthSystemState();

        Operation refreshOperation = new AddOperation("/sections/unpaywall/refresh", true);
        final String authToken = getAuthToken(eperson.getEmail(), password);
        AtomicReference<String> unpayStatus = new AtomicReference<>();
        int numRefresh = 0;
        do {
            pauseRefresh(numRefresh);
            getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                             .content(getPatchContent(List.of(refreshOperation)))
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[*].paths", not(containsString("/section/unpaywall"))))
                .andExpect(jsonPath("$.sections.unpaywall.doi", is(doi)))
                .andExpect(jsonPath("$.sections.unpaywall.itemId", is(itemUUID.toString())))
                .andExpect(jsonPath("$.sections.unpaywall.timestampCreated", notNullValue()))
                .andExpect(jsonPath("$.sections.unpaywall.timestampLastModified", notNullValue()))
                .andDo(result -> unpayStatus.set(read(result.getResponse().getContentAsString(),
                                                      "$.sections.unpaywall.status")));
            assertThat(unpayStatus.get(), Matchers.oneOf(UnpaywallStatus.PENDING.name(),
                                                         UnpaywallStatus.SUCCESSFUL.name()));
            refreshOperation = new AddOperation("/sections/unpaywall/refresh", false);
            numRefresh++;
        } while (!unpayStatus.get().equals(UnpaywallStatus.SUCCESSFUL.name()) && numRefresh < 3);

        assertThat(unpayStatus.get(), Matchers.equalTo(UnpaywallStatus.SUCCESSFUL.name()));
        verify(spyClientAPI, times(1)).callUnpaywallApi(any());
    }

    @Test
    public void testCallingUnpaywallApiWithoutRefresh() throws Exception {

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
        context.commit();
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
        verify(spyClientAPI, times(0)).callUnpaywallApi(any());
    }

    @Test
    public void testCallingUnpaywallApiWithAccept() throws Exception {

        context.turnOffAuthorisationSystem();

        String doi = "10.1504/ijmso.2012.048507";
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                          .withTitle("Test WorkspaceItem")
                                                          .withIssueDate("2020")
                                                          .withDoiIdentifier(doi)
                                                          .build();
        final UUID itemUUID = workspaceItem.getItem().getID();
        Unpaywall unpaywall = new Unpaywall();
        unpaywall.setDoi(doi);
        unpaywall.setItemId(itemUUID);
        unpaywall.setStatus(UnpaywallStatus.NOT_FOUND);
        unpaywall.setJsonRecord(null);
        unpaywall = unpaywallService.create(context, unpaywall);
        context.commit();

        context.restoreAuthSystemState();

        Operation refreshOperation = new AddOperation("/sections/unpaywall/refresh", true);
        final String authToken = getAuthToken(eperson.getEmail(), password);
        AtomicReference<String> unpayStatus = new AtomicReference<>();
        int numRefresh = 0;
        do {
            pauseRefresh(numRefresh);
            getClient(authToken)
                .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                             .content(getPatchContent(List.of(refreshOperation)))
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[*].paths", not(containsString("/section/unpaywall"))))
                .andExpect(jsonPath("$.sections.unpaywall.doi", is(doi)))
                .andExpect(jsonPath("$.sections.unpaywall.itemId", is(itemUUID.toString())))
                .andExpect(jsonPath("$.sections.unpaywall.timestampCreated", notNullValue()))
                .andExpect(jsonPath("$.sections.unpaywall.timestampLastModified", notNullValue()))
                .andDo(result -> unpayStatus.set(read(result.getResponse().getContentAsString(),
                                                      "$.sections.unpaywall.status")));
            assertThat(unpayStatus.get(), Matchers.oneOf(UnpaywallStatus.PENDING.name(),
                                                         UnpaywallStatus.SUCCESSFUL.name()));
            refreshOperation = new AddOperation("/sections/unpaywall/refresh", false);
            numRefresh++;
        } while (!unpayStatus.get().equals(UnpaywallStatus.SUCCESSFUL.name()) && numRefresh < 3);

        assertThat(unpayStatus.get(), Matchers.equalTo(UnpaywallStatus.SUCCESSFUL.name()));
        verify(spyClientAPI, times(1)).callUnpaywallApi(any());

        Operation acceptOperation = new AddOperation("/sections/unpaywall/accept", true);
        getClient(authToken)
            .perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
                         .content(getPatchContent(List.of(acceptOperation)))
                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.sections.unpaywall.doi", is(doi)))
            .andExpect(jsonPath("$.sections.unpaywall.itemId", is(itemUUID.toString())))
            .andExpect(jsonPath("$.sections.unpaywall.timestampCreated", notNullValue()))
            .andExpect(jsonPath("$.sections.unpaywall.timestampLastModified", notNullValue()))
            .andExpect(jsonPath("$.sections.unpaywall.status", is(UnpaywallStatus.IMPORTED.name())));
        verify(spyClientAPI, times(1)).downloadResource(any());
    }

    private void pauseRefresh(int numRefresh) {
        try {
            Thread.sleep(numRefresh * numRefresh * 500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
