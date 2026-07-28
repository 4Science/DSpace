/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Integration tests for {@link DepositRestPermissionEvaluatorPlugin}, the permission evaluator that gates the
 * deposit action (POST /api/workflow/workflowitems, WRITE on the WORKFLOWITEM target type).
 * <p>
 * These tests encode the <em>expected secure behavior</em> of the deposit gate. They form a matrix over two axes:
 * <ul>
 *   <li>who performs the deposit (the original submitter, another submitter of the same collection, or a user who
 *       is not a submitter at all); and</li>
 *   <li>whether the target collection is a shared workspace ({@code cris.workspace.shared = true}) or not.</li>
 * </ul>
 * The intended model, mirrored by the create-time policies in {@code WorkspaceItemServiceImpl.create} and the
 * deposit-time policies in {@code XmlWorkflowServiceImpl.start}, is:
 * <ul>
 *   <li>the original submitter may always deposit their own workspace item;</li>
 *   <li>a different submitter may deposit another submitter's workspace item <strong>only</strong> when the
 *       collection is a shared workspace (collaborative deposit); and</li>
 *   <li>a user who is not a submitter of the collection may never deposit.</li>
 * </ul>
 * <p>
 * <strong>Vulnerability demonstrator:</strong>
 * {@link #otherSubmitterCannotDepositAnotherSubmittersItemInNonSharedWorkspace()} encodes the secure expectation
 * (403 Forbidden). Against the current implementation it will <em>fail</em>, because the plugin authorizes the
 * deposit purely on the basis of collection ADD permission without checking whether the collection is a shared
 * workspace, so any submitter of a non-shared collection can deposit another submitter's workspace item. The
 * remaining tests are controls that must pass both before and after the fix, proving that the fix neither weakens
 * the legitimate flows nor over-restricts collaborative (shared-workspace) deposit.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class DepositRestPermissionEvaluatorPluginIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private XmlWorkflowItemService xmlWorkflowItemService;

    /** The original owner/submitter of the workspace item under test. */
    private EPerson submitterA;

    /** A second submitter of the same collection who is NOT the owner of the workspace item. */
    private EPerson submitterB;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Make a minimal workspace item valid for deposit (no mandatory bitstream upload).
        configurationService.setProperty("webui.submit.upload.required", false);

        context.turnOffAuthorisationSystem();

        submitterA = EPersonBuilder.createEPerson(context)
                                   .withEmail("submitter-a@example.com")
                                   .withNameInMetadata("Submitter", "A")
                                   .withPassword(password)
                                   .build();

        submitterB = EPersonBuilder.createEPerson(context)
                                   .withEmail("submitter-b@example.com")
                                   .withNameInMetadata("Submitter", "B")
                                   .withPassword(password)
                                   .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        context.restoreAuthSystemState();
    }

    /**
     * Control: the original submitter can deposit their own workspace item in a non-shared-workspace collection.
     * Expected: 201 Created. Passes before and after the fix.
     */
    @Test
    public void submitterCanDepositOwnItemInNonSharedWorkspace() throws Exception {
        Collection collection = createCollection(false);
        WorkspaceItem workspaceItem = createWorkspaceItemOwnedBySubmitterA(collection);
        Item item = workspaceItem.getItem();
        try {
            performDeposit(submitterA, workspaceItem)
                .andExpect(status().isCreated());
        } finally {
            cleanupWorkflowItem(item);
        }
    }

    /**
     * Control: the original submitter can deposit their own workspace item in a shared-workspace collection.
     * Expected: 201 Created. Passes before and after the fix.
     */
    @Test
    public void submitterCanDepositOwnItemInSharedWorkspace() throws Exception {
        Collection collection = createCollection(true);
        WorkspaceItem workspaceItem = createWorkspaceItemOwnedBySubmitterA(collection);
        Item item = workspaceItem.getItem();
        try {
            performDeposit(submitterA, workspaceItem)
                .andExpect(status().isCreated());
        } finally {
            cleanupWorkflowItem(item);
        }
    }

    /**
     * A different submitter of the same collection must NOT be able to deposit another submitter's workspace
     * item when the collection is NOT a shared workspace.
     *
     * Expected (secure): 403 Forbidden. Against the current implementation this test FAILS because the deposit
     * actually succeeds (201 Created): the plugin only checks collection ADD permission, which every member of the
     * submitters group holds, and ignores the shared-workspace flag. This failing assertion is the demonstration
     * of the vulnerability. After the fix it will pass.
     */
    @Test
    public void otherSubmitterCannotDepositAnotherSubmittersItemInNonSharedWorkspace() throws Exception {
        Collection collection = createCollection(false);
        WorkspaceItem workspaceItem = createWorkspaceItemOwnedBySubmitterA(collection);
        Item item = workspaceItem.getItem();
        try {
            performDeposit(submitterB, workspaceItem)
                .andExpect(status().isForbidden());
        } finally {
            // Clean up defensively: against current (vulnerable) code the deposit succeeds and creates a
            // workflow item as a side effect even though the assertion above fails.
            cleanupWorkflowItem(item);
        }
    }

    /**
     * Control: a different submitter of the same collection CAN deposit another submitter's workspace item when the
     * collection IS a shared workspace. This is the legitimate collaborative-deposit scenario.
     * Expected: 201 Created. Passes before and after the fix (guards against the fix over-restricting).
     */
    @Test
    public void otherSubmitterCanDepositAnotherSubmittersItemInSharedWorkspace() throws Exception {
        Collection collection = createCollection(true);
        WorkspaceItem workspaceItem = createWorkspaceItemOwnedBySubmitterA(collection);
        Item item = workspaceItem.getItem();
        try {
            performDeposit(submitterB, workspaceItem)
                .andExpect(status().isCreated());
        } finally {
            cleanupWorkflowItem(item);
        }
    }

    /**
     * Control: a user who is not a submitter of the collection cannot deposit another submitter's workspace item in
     * a non-shared-workspace collection. Expected: 403 Forbidden. Passes before and after the fix.
     */
    @Test
    public void nonSubmitterCannotDepositAnotherSubmittersItemInNonSharedWorkspace() throws Exception {
        Collection collection = createCollection(false);
        WorkspaceItem workspaceItem = createWorkspaceItemOwnedBySubmitterA(collection);
        Item item = workspaceItem.getItem();
        try {
            // "eperson" is a valid authenticated user but is NOT a member of the collection's submitters group.
            performDeposit(eperson, workspaceItem)
                .andExpect(status().isForbidden());
        } finally {
            cleanupWorkflowItem(item);
        }
    }

    /**
     * Control: a user who is not a submitter of the collection cannot deposit another submitter's workspace item
     * even in a shared-workspace collection. Shared workspace enables collaboration among submitters, not open
     * deposit for everyone. Expected: 403 Forbidden. Passes before and after the fix.
     */
    @Test
    public void nonSubmitterCannotDepositAnotherSubmittersItemInSharedWorkspace() throws Exception {
        Collection collection = createCollection(true);
        WorkspaceItem workspaceItem = createWorkspaceItemOwnedBySubmitterA(collection);
        Item item = workspaceItem.getItem();
        try {
            performDeposit(eperson, workspaceItem)
                .andExpect(status().isForbidden());
        } finally {
            cleanupWorkflowItem(item);
        }
    }

    /**
     * Create a collection whose submitters group contains {@link #submitterA} and {@link #submitterB} and whose
     * first workflow step is owned by {@code admin} (so a successful deposit lands in the workflow pool and returns
     * 201 Created rather than being auto-archived).
     *
     * @param sharedWorkspace whether the collection is flagged as a shared workspace.
     * @return the created collection.
     * @throws Exception passed through from the builders.
     */
    private Collection createCollection(boolean sharedWorkspace) throws Exception {
        context.turnOffAuthorisationSystem();
        CollectionBuilder builder = CollectionBuilder.createCollection(context, parentCommunity)
                                                     .withName(sharedWorkspace
                                                                   ? "Shared Workspace Collection"
                                                                   : "Non-Shared Workspace Collection")
                                                     .withEntityType("Publication")
                                                     .withSubmitterGroup(submitterA, submitterB)
                                                     .withWorkflowGroup(1, admin);
        if (sharedWorkspace) {
            builder = builder.withSharedWorkspace();
        }
        Collection collection = builder.build();
        context.restoreAuthSystemState();
        return collection;
    }

    /**
     * Create a valid, deposit-ready workspace item whose submitter is {@link #submitterA}. The current user is set
     * to submitterA at creation time so that submitterA becomes the item's submitter and the holder of the
     * create-time submission policies, exactly as in a real submission.
     *
     * @param collection the collection to submit into.
     * @return the created workspace item.
     */
    private WorkspaceItem createWorkspaceItemOwnedBySubmitterA(Collection collection) {
        context.turnOffAuthorisationSystem();
        context.setCurrentUser(submitterA);
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                          .withTitle("Submitter A's submission")
                                                          .withIssueDate("2017-10-17")
                                                          .withType("other")
                                                          .grantLicense()
                                                          .build();
        context.restoreAuthSystemState();
        return workspaceItem;
    }

    /**
     * Perform the deposit request (start the workflow for a workspace item) as the given actor.
     *
     * @param actor         the authenticated user performing the deposit.
     * @param workspaceItem the workspace item to deposit.
     * @return the {@link ResultActions} for further assertions.
     * @throws Exception passed through from the mock request.
     */
    private ResultActions performDeposit(EPerson actor, WorkspaceItem workspaceItem) throws Exception {
        String token = getAuthToken(actor.getEmail(), password);
        return getClient(token).perform(post(BASE_REST_SERVER_URL + "/api/workflow/workflowitems")
                                            .content("/api/submission/workspaceitems/" + workspaceItem.getID())
                                            .contentType(textUriContentType));
    }

    /**
     * Delete any workflow item created for the given item, cascading to the underlying item. Safe to call when no
     * workflow item exists (e.g. when the deposit was correctly forbidden); in that case the workspace item is
     * cleaned up automatically by its builder.
     *
     * @param item the item that may have been wrapped by a workflow item.
     * @throws Exception passed through from the services.
     */
    private void cleanupWorkflowItem(Item item) throws Exception {
        context.turnOffAuthorisationSystem();
        try {
            Item reloaded = context.reloadEntity(item);
            if (reloaded != null) {
                XmlWorkflowItem workflowItem = xmlWorkflowItemService.findByItem(context, reloaded);
                if (workflowItem != null) {
                    WorkflowItemBuilder.deleteWorkflowItem(workflowItem.getID());
                }
            }
        } finally {
            context.restoreAuthSystemState();
        }
    }
}
