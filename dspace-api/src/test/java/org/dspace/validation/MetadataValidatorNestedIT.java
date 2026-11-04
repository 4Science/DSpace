/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation;

import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.CrisConstants;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.validation.model.ValidationError;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for the nested-group (group / inline-group) required validation added to
 * {@link MetadataValidator}
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class MetadataValidatorNestedIT extends AbstractIntegrationTestWithDatabase {

    private static final String AUTHOR_FIELD = "dc.contributor.author";
    private static final String AFFILIATION_FIELD = "oairecerif.author.affiliation";
    private static final String AUTHOR_PATH_PREFIX = "/sections/publication/dc.contributor.author";
    private static final String PLACEHOLDER = CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private Community community;
    private Collection collection;

    @Before
    public void setUpNestedTests() throws Exception {
        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community)
                           .withEntityType("Publication")
                           .withSubmissionDefinition("publication")
                           .withAdminGroup(eperson)
                           .build();
        context.restoreAuthSystemState();
    }

    /**
     * Row filled correctly (required field present): no error is raised.
     */
    @Test
    public void testValidRowDoesNotRaiseError() throws Exception {
        WorkspaceItem workspaceItem = buildWorkspaceItem();
        addAuthorRow(workspaceItem.getItem(), "Doe, John", "University A");
        List<String> paths = validationPaths(workspaceItem);
        assertThat(paths, not(hasItem(AUTHOR_PATH_PREFIX + "/0")));
    }

    /**
     * Row with some data but the required field empty: an error is raised on that row.
     */
    @Test
    public void testMissingRequiredNestedFieldRaisesError() throws Exception {
        WorkspaceItem workspaceItem = buildWorkspaceItem();
        // only the optional affiliation is filled, the required author is absent on that row
        addMetadata(workspaceItem.getItem(), AFFILIATION_FIELD, "University A");
        List<String> paths = validationPaths(workspaceItem);
        assertThat(paths, hasItem(AUTHOR_PATH_PREFIX + "/0"));
    }

    /**
     * The alignment placeholder must be treated as an empty value on a required nested field.
     */
    @Test
    public void testPlaceholderIsTreatedAsEmpty() throws Exception {
        WorkspaceItem workspaceItem = buildWorkspaceItem();
        // author carries only the alignment placeholder while the affiliation carries a real value
        addAuthorRow(workspaceItem.getItem(), PLACEHOLDER, "University A");
        List<String> paths = validationPaths(workspaceItem);
        assertThat(paths, hasItem(AUTHOR_PATH_PREFIX + "/0"));
    }

    /**
     * In a repeatable group only the incomplete row must be reported.
     */
    @Test
    public void testRepeatableGroupReportsOnlyInvalidRow() throws Exception {
        WorkspaceItem workspaceItem = buildWorkspaceItem();
        Item item = workspaceItem.getItem();
        // row 0: author present, row 1: author missing (placeholder) but affiliation present
        addMetadata(item, AUTHOR_FIELD, "Doe, John");
        addMetadata(item, AUTHOR_FIELD, PLACEHOLDER);
        addMetadata(item, AFFILIATION_FIELD, PLACEHOLDER);
        addMetadata(item, AFFILIATION_FIELD, "University B");

        List<String> paths = validationPaths(workspaceItem);
        // only the incomplete row (place 1) is reported; the valid row (place 0) is not
        assertThat(paths, not(hasItem(AUTHOR_PATH_PREFIX + "/0")));
        assertThat(paths, hasItem(AUTHOR_PATH_PREFIX + "/1"));
    }

    /**
     * All rows valid: no required error for the nested field.
     */
    @Test
    public void testAllRowsValidDoesNotRaiseError() throws Exception {
        WorkspaceItem workspaceItem = buildWorkspaceItem();
        Item item = workspaceItem.getItem();
        addMetadata(item, AUTHOR_FIELD, "Doe, John");
        addMetadata(item, AUTHOR_FIELD, "Smith, Jane");

        List<String> paths = validationPaths(workspaceItem);

        assertThat(paths, not(hasItem(AUTHOR_PATH_PREFIX + "/0")));
        assertThat(paths, not(hasItem(AUTHOR_PATH_PREFIX + "/1")));
    }

    /**
     * An empty (non-required) group must not raise any nested required error.
     */
    @Test
    public void testEmptyGroupDoesNotRaiseError() throws Exception {
        WorkspaceItem workspaceItem = buildWorkspaceItem();
        List<String> paths = validationPaths(workspaceItem);
        assertThat(paths, not(hasItem(AUTHOR_PATH_PREFIX + "/0")));
    }

    /**
     * Row that is completely empty (only placeholders): no error is raised.
     */
    @Test
    public void testAllPlaceholderRowDoesNotRaiseError() throws Exception {
        WorkspaceItem workspaceItem = buildWorkspaceItem();
        addAuthorRow(workspaceItem.getItem(), PLACEHOLDER, PLACEHOLDER);
        List<String> paths = validationPaths(workspaceItem);
        assertThat(paths, not(hasItem(AUTHOR_PATH_PREFIX + "/0")));
    }

    private WorkspaceItem buildWorkspaceItem() {
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection).build();
        context.restoreAuthSystemState();
        return workspaceItem;
    }

    private void addAuthorRow(Item item, String author, String affiliation) throws Exception {
        addMetadata(item, AUTHOR_FIELD, author);
        addMetadata(item, AFFILIATION_FIELD, affiliation);
    }

    private void addMetadata(Item item, String field, String value) throws Exception {
        context.turnOffAuthorisationSystem();
        String[] parts = field.split("\\.");
        String qualifier = parts.length > 2 ? parts[2] : null;
        itemService.addMetadata(context, item, parts[0], parts[1], qualifier, null, value);
        itemService.update(context, item);
        context.restoreAuthSystemState();
    }

    private List<String> validationPaths(WorkspaceItem workspaceItem) throws Exception {
        List<ValidationError> errors = newValidator().validate(context, workspaceItem, getPublicationFormStep());
        List<String> paths = new ArrayList<>();
        for (ValidationError error : errors) {
            paths.addAll(error.getPaths());
        }
        return paths;
    }

    private MetadataValidator newValidator() {
        MetadataValidator validator = new MetadataValidator();
        validator.setName("submission-form");
        validator.setItemService(itemService);
        validator.setMetadataAuthorityService(
            ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService());
        validator.setConfigurationService(DSpaceServicesFactory.getInstance().getConfigurationService());
        return validator;
    }

    private SubmissionStepConfig getPublicationFormStep() throws SubmissionConfigReaderException {
        SubmissionConfig submissionConfig = new SubmissionConfigReader().getSubmissionConfigByCollection(collection);
        for (int i = 0; i < submissionConfig.getNumberOfSteps(); i++) {
            SubmissionStepConfig step = submissionConfig.getStep(i);
            if ("submission-form".equals(step.getType()) && "publication".equals(step.getId())) {
                return step;
            }
        }
        throw new IllegalStateException("No 'publication' submission-form step found");
    }
}
