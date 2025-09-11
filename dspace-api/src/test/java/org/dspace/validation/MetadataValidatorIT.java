/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.validation.model.ValidationError;
import org.dspace.workflow.WorkflowItem;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for {@link MetadataValidator} to ensure it respects
 * readonly and hidden scopes for required fields.
 *
 * This test class relies on a custom submission definition ("test-metadata-validator")
 * which must be configured in dspace-api/src/test/data/dspaceFolder/config/submission-forms.xml and
 * dspace-api/src/test/data/dspaceFolder/config/item-submission.xml.
 *
 */
public class MetadataValidatorIT extends AbstractIntegrationTestWithDatabase {

    private Collection collection;
    private MetadataValidator validator;
    private SubmissionStepConfig submissionStepConfig;
    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context).build();
        collection = CollectionBuilder.createCollection(context, community)
            .withSubmissionDefinition("test-metadata-validator")
            .build();

        context.restoreAuthSystemState();

        SubmissionConfigReader submissionConfigReader = new SubmissionConfigReader();
        SubmissionConfig submissionConfig = submissionConfigReader.getSubmissionConfigByCollection(collection);
        // Assumes the test submission process has one step defined
        submissionStepConfig = submissionConfig.getStep(0);

        validator = new MetadataValidator();
        validator.setName("test-validator");
        validator.setItemService(itemService);
        validator.setMetadataAuthorityService(ContentAuthorityServiceFactory.getInstance()
            .getMetadataAuthorityService());
        validator.setConfigurationService(configurationService);
        validator.setInputReader(new DCInputsReader());
    }

    /**
     * In SUBMISSION scope, ensures validation reports only required fields
     * not marked readonly/hidden for submission. Missing dc.title and
     * dc.contributor.author should be flagged; dc.date.issued is ignored.
     */
    @Test
    public void testValidationInSubmissionScopeWithErrors() throws Exception {
        context.turnOffAuthorisationSystem();
        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection).build();
        context.restoreAuthSystemState();

        List<ValidationError> errors = validator.validate(context, wsi, submissionStepConfig);

        assertThat(errors, hasSize(1));

        List<String> errorPaths = getErrorPaths(errors);
        assertThat(errorPaths, containsInAnyOrder(
            "/sections/test-metadata-validator-step/dc.title",
            "/sections/test-metadata-validator-step/dc.contributor.author"
        ));
    }

    /**
     * In SUBMISSION scope, ensures no errors when all required fields
     * for this scope are present. Fields readonly/hidden in submission
     * can remain empty without errors.
     */
    @Test
    public void testValidationInSubmissionScopeWithoutErrors() throws Exception {
        context.turnOffAuthorisationSystem();
        WorkspaceItem wsi = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle("Test Title")
            .withAuthor("Test, Author")
            .withIssueDate("2025-08-14")
            .build();
        context.restoreAuthSystemState();

        List<ValidationError> errors = validator.validate(context, wsi, submissionStepConfig);

        assertThat(errors, empty());
    }

    /**
     * In WORKFLOW scope, ensures validation reports only required fields
     * not marked readonly/hidden for workflow. Missing dc.title and
     * dc.description.abstract should be flagged; others are ignored.
     */
    @Test
    public void testValidationInWorkflowScopeWithErrors() throws Exception {
        context.turnOffAuthorisationSystem();
        WorkflowItem wfi = WorkflowItemBuilder.createWorkflowItem(context, collection).build();
        context.restoreAuthSystemState();

        List<ValidationError> errors = validator.validate(context, wfi, submissionStepConfig);

        assertThat(errors, hasSize(1));

        List<String> errorPaths = getErrorPaths(errors);
        assertThat(errorPaths, containsInAnyOrder(
            "/sections/test-metadata-validator-step/dc.title",
            "/sections/test-metadata-validator-step/dc.description.abstract"
        ));
    }

    /**
     * In WORKFLOW scope, ensures no errors when all required fields
     * for this scope are present. Fields readonly/hidden in workflow
     * can remain empty without errors.
     */
    @Test
    public void testValidationInWorkflowScopeWithoutErrors() throws Exception {
        context.turnOffAuthorisationSystem();
        WorkflowItem wfi = WorkflowItemBuilder.createWorkflowItem(context, collection).build();

        // Add metadata for fields required in the workflow scope
        itemService.addMetadata(context, wfi.getItem(), "dc", "title", null, null, "Test Title");
        itemService.addMetadata(context, wfi.getItem(), "dc", "description", "abstract", null, "Test Abstract");
        itemService.addMetadata(context, wfi.getItem(), "dc", "subject", null, null, "Test Subject");

        context.restoreAuthSystemState();

        List<ValidationError> errors = validator.validate(context, wfi, submissionStepConfig);

        assertThat(errors, empty());
    }

    private List<String> getErrorPaths(List<ValidationError> errors) {
        return errors.stream()
            .flatMap(error -> error.getPaths().stream())
            .collect(Collectors.toList());
    }
}
