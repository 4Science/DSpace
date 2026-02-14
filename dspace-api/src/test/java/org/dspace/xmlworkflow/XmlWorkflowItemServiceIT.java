/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.junit.Test;

/**
 * IT for {@link org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItemServiceImpl}
 *
 * @author K.Kaiser (TU Wien)
 */
public class XmlWorkflowItemServiceIT extends AbstractIntegrationTestWithDatabase {

    protected XmlWorkflowItemService xmlWorkflowItemService = XmlWorkflowServiceFactory.getInstance()
        .getXmlWorkflowItemService();
    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();


    @Test
    public void isCollectionWithWorkflowConfiguredWithNewGroupName() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson submitter = EPersonBuilder.createEPerson(context).withEmail("submitter@example.org").build();
        context.setCurrentUser(submitter);
        Community community = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection colWithWorkflow = CollectionBuilder.createCollection(context, community)
            .withName("Collection WITH workflow")
            .withWorkflowGroup("editor", submitter)
            .build();
        context.restoreAuthSystemState();
        String expectedGroupName = "COLLECTION_" + colWithWorkflow.getID() + "_WORKFLOW_ROLE_editor";
        assertEquals("Expecting automatic group name", expectedGroupName,
            colWithWorkflow.getWorkflowStep2(context).getName());
        assertTrue("Workflow is configured with automatic group name and with a member",
            xmlWorkflowItemService.isWorkflowConfigured(context, colWithWorkflow));
    }

    @Test
    public void isCollectionWithWorkflowConfiguredWithOldGroupName() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson submitter = EPersonBuilder.createEPerson(context).withEmail("submitter@example.org").build();
        context.setCurrentUser(submitter);
        Group reviewer = GroupBuilder.createGroup(context).withName("Reviewers").addMember(submitter).build();
        Community community = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection colWithWorkflow = CollectionBuilder.createCollection(context, community)
            .withName("Collection WITH workflow")
            .build();
        colWithWorkflow.setWorkflowGroup(context, 1, reviewer);
        context.restoreAuthSystemState();
        assertEquals("Group name of Workflow Step 1 must be 'Reviewers'", "Reviewers",
            colWithWorkflow.getWorkflowStep1(context).getName());
        assertTrue("Workflow with group members must be considered a workflow",
            xmlWorkflowItemService.isWorkflowConfigured(context, colWithWorkflow));
    }

    @Test
    public void isCollectionWithoutWorkflowConfiguredWithEmptyGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        Group editor = GroupBuilder.createGroup(context).withName("Editors").build();
        Community community = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection colWithWorkflow = CollectionBuilder.createCollection(context, community)
            .withName("Collection WITH workflow, but empty group")
            .build();
        colWithWorkflow.setWorkflowGroup(context, 2, editor);
        context.restoreAuthSystemState();
        assertEquals("Group name of Workflow Step 2 must be 'Editors'", "Editors",
            colWithWorkflow.getWorkflowStep2(context).getName());
        assertFalse("Workflow without group members must not be considered a workflow",
            xmlWorkflowItemService.isWorkflowConfigured(context, colWithWorkflow));
    }

    @Test
    public void isCollectionWithoutWorkflowConfigured() {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection colWithoutWorkflow = CollectionBuilder.createCollection(context, community)
            .withName("Collection WITHOUT workflow")
            .build();
        context.restoreAuthSystemState();
        assertFalse("Collection without a workflow configured",
            xmlWorkflowItemService.isWorkflowConfigured(context, colWithoutWorkflow));
    }
}
