/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.sql.SQLException;

import com.jayway.jsonpath.matchers.JsonPathMatchers;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.eperson.EPerson;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test suite for testing the Show Identifiers submission stesp
 * 
 * @author Kim Shepherd
 *
 */
public class SubmissionShowIdentifiersRestIT extends AbstractControllerIntegrationTest {

    @Autowired
    private WorkspaceItemService workspaceItemService;

    private Collection collection;
    private EPerson submitter;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Root community").build();

        submitter = EPersonBuilder.createEPerson(context)
                                  .withEmail("submitter.em@test.com")
                                  .withPassword(password)
                                  .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
                                      .withName("Collection")
                                      .withEntityType("Publication")
                                      .withSubmissionDefinition("traditional")
                                      .withSubmitterGroup(submitter).build();

        context.restoreAuthSystemState();
    }

    @After
    public void after() throws SQLException, IOException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        workspaceItemService.findAll(context).forEach(this::deleteWorkspaceItem);
        context.restoreAuthSystemState();
    }

    private void deleteWorkspaceItem(WorkspaceItem workspaceItem) {
        try {
            workspaceItemService.deleteAll(context, workspaceItem);
        } catch (SQLException | AuthorizeException | IOException e) {
            throw new RuntimeException();
        }
    }

    @Test
    public void testItemHandleReservation() throws Exception {
        // Test publication that should get Handle and DOI
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = createWorkspaceItem("Test publication", collection);
        context.restoreAuthSystemState();
        String submitterToken = getAuthToken(submitter.getEmail(), password);
        getClient(submitterToken).perform(get("/api/submission/workspaceitems/" + workspaceItem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.identifiers.handle").exists());
    }

    @Test
    public void testItemDoiReservation() throws Exception {
        // Test publication that should get Handle and DOI
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = createWorkspaceItem("Test publication", collection);
        context.restoreAuthSystemState();

        String submitterToken = getAuthToken(submitter.getEmail(), password);
        getClient(submitterToken).perform(get("/api/submission/workspaceitems/" + workspaceItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.identifiers.doi").exists());
    }

    private WorkspaceItem createWorkspaceItem(String title, Collection collection) {
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle(title)
                .withSubmitter(submitter)
                .build();
        return workspaceItem;
    }

}