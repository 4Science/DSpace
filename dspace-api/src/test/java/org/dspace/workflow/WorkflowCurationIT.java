/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.regex.Pattern;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.ctask.testing.MarkerTask;
import org.dspace.curate.Curator;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the attachment of curation tasks to workflows.
 *
 * @author mwood
 */
@Ignore
public class WorkflowCurationIT extends AbstractIntegrationTestWithDatabase {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    /**
     * Basic smoke test of a curation task attached to a workflow step.
     * See {@link MarkerTask}.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void curationTest()
            throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **

        // A submitter;
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .withLanguage("en")
                .build();

        // A containment hierarchy;
        Community community = CommunityBuilder.createCommunity(context)
                .withName("Community")
                .build();
        final String CURATION_COLLECTION_HANDLE = "123456789/curation-test-1";
        Collection collection = CollectionBuilder
                .createCollection(context, community, CURATION_COLLECTION_HANDLE)
                .withName("Collection")
                .build();

        // A workflow configuration for the test Collection;
        // See test/dspaceFolder/config/spring/api/workflow.xml

        // A curation task attached to the workflow;
        // See test/dspaceFolder/config/workflow-curation.xml for the attachment.
        // This should include MarkerTask.

        // A workflow item;
        context.setCurrentUser(submitter);
        XmlWorkflowItem wfi = WorkflowItemBuilder.createWorkflowItem(context, collection)
                .withTitle("Test of workflow curation")
                .withIssueDate("2021-05-14")
                .withSubject("Testing")
                .build();

        context.restoreAuthSystemState();

        //** THEN **

        // Search the Item's provenance for MarkerTask's name.
        List<MetadataValue> provenance = itemService.getMetadata(wfi.getItem(),
                MarkerTask.SCHEMA, MarkerTask.ELEMENT, MarkerTask.QUALIFIER, MarkerTask.LANGUAGE);
        Pattern markerPattern = Pattern.compile(MarkerTask.class.getCanonicalName());
        boolean found = false;
        for (MetadataValue record : provenance) {
            if (markerPattern.matcher(record.getValue()).find()) {
                found = true;
                break;
            }
        }
        assertThat("Item should have been curated", found);
    }

    /**
     * Test method which include calling
     * {@link org.dspace.curate.XmlWorkflowCuratorServiceImpl#curate(Curator, Context, XmlWorkflowItem)}.
     * <p>
     * Verifies that the curate process is executed correctly given a specific
     * {@link Curator}, {@link Context}, and {@link XmlWorkflowItem}.
     * </p>
     * <p>
     * This method depends on setting the transaction scope of the curator
     * to {@code Curator.TxScope.CURATION}. If this is not set, an exception
     * will be thrown during the curation process.
     * </p>
     */
    @Test
    public void changeScopeToCurationBeforeCurateTest() throws Exception {
        context.turnOffAuthorisationSystem();

        final String CURATION_COLLECTION_HANDLE = "123456789/curation-test-2";

        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .withLanguage("en")
                .build();

        Community community = CommunityBuilder.createCommunity(context)
                .withName("Community")
                .build();

        Collection collection = CollectionBuilder
                .createCollection(context, community, CURATION_COLLECTION_HANDLE)
                .withName("Collection")
                .build();

        context.setCurrentUser(submitter);

        //
        WorkflowItemBuilder.createWorkflowItem(context, collection)
                .withTitle("Test of workflow curation")
                .withIssueDate("2021-05-14")
                .withSubject("Testing")
                .build();

        context.restoreAuthSystemState();
    }
}
