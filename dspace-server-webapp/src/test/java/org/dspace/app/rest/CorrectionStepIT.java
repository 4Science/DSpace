/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.services.ConfigurationService;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

/**
 * Integration tests for {@link CorrectionStep}.
 *
 * @author Giuseppe Digilio (giuseppe.digilio at 4science.it)
 *
 */
public class CorrectionStepIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private PoolTaskService poolTaskService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private XmlWorkflowItemService xmlWorkflowItemService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    private Collection collection;

    private Item itemToBeCorrected;

    private EntityType publicationType;

    private String date;
    private String title;
    private String type;
    private String subject;

    private AtomicReference<Integer> workspaceItemIdRef = new AtomicReference<Integer>();

    @Value("classpath:org/dspace/app/rest/simple-article.pdf")
    private Resource simpleArticle;

    @Before
    public void setup() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("item-correction.permit-all", true);

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection")
                .withEntityType("Publication")
                .withWorkflowGroup("editor", admin)
                .withSubmitterGroup(eperson)
                .withSubmissionDefinition("traditional")
                .withCorrectionSubmissionDefinition("traditional-with-correction")
                .build();

        date = "2020-02-20";
        subject = "ExtraEntry";
        title = "Title " + new Date().getTime();
        type = "text";

        itemToBeCorrected = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", simpleArticle.getInputStream())
                .withIssueDate(date)
                .withSubject(subject)
                .withType(type)
                .grantLicense()
                .build();

        publicationType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publicationType, publicationType,
            "isCorrectionOfItem", "isCorrectedByItem", 0, 1, 0, 1);

        context.setCurrentUser(eperson);

        context.restoreAuthSystemState();
    }

    @Override
    @After
    public void destroy() throws Exception {
        //Clean up the database for the next test
        context.turnOffAuthorisationSystem();
        List<RelationshipType> relationshipTypeList = relationshipTypeService.findAll(context);
        List<EntityType> entityTypeList = entityTypeService.findAll(context);
        List<Relationship> relationships = relationshipService.findAll(context);

        Iterator<Relationship> relationshipIterator = relationships.iterator();
        while (relationshipIterator.hasNext()) {
            Relationship relationship = relationshipIterator.next();
            relationshipIterator.remove();
            relationshipService.delete(context, relationship);
        }

        Iterator<RelationshipType> relationshipTypeIterator = relationshipTypeList.iterator();
        while (relationshipTypeIterator.hasNext()) {
            RelationshipType relationshipType = relationshipTypeIterator.next();
            relationshipTypeIterator.remove();
            relationshipTypeService.delete(context, relationshipType);
        }

        Iterator<EntityType> entityTypeIterator = entityTypeList.iterator();
        while (entityTypeIterator.hasNext()) {
            EntityType entityType = entityTypeIterator.next();
            if (!entityType.getLabel().equals(Constants.ENTITY_TYPE_NONE)) {
                entityTypeIterator.remove();
                entityTypeService.delete(context, entityType);
            }
        }

        poolTaskService.findAll(context).forEach(this::deletePoolTask);
        xmlWorkflowItemService.findAll(context).forEach(this::deleteWorkflowItem);

        super.destroy();
    }

    @Test
    public void checkCorrection() throws Exception {

        String tokenSubmitter = getAuthToken(eperson.getEmail(), password);

        //create a correction item
        getClient(tokenSubmitter).perform(post("/api/submission/workspaceitems")
                .param("owningCollection", collection.getID().toString())
                .param("relationship", "isCorrectionOfItem")
                .param("item", itemToBeCorrected.getID().toString())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andDo(result -> workspaceItemIdRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        List<Relationship> relationshipList = relationshipService.findByItem(context, itemToBeCorrected);
        assert relationshipList.size() > 0;
        Item correctedItem  = relationshipList.get(0).getLeftItem();
        WorkspaceItem newWorkspaceItem = workspaceItemService.findByItem(context,correctedItem);

        //make a change on the title
        Map<String, String> value = new HashMap<String, String>();
        final String newTitle = "New Title";
        value.put("value", newTitle);
        List<Operation> addGrant = new ArrayList<Operation>();
        addGrant.add(new ReplaceOperation("/sections/traditionalpageone/dc.title/0", value));
        String patchBody = getPatchContent(addGrant);
        getClient(tokenSubmitter).perform(patch("/api/submission/workspaceitems/" + newWorkspaceItem.getID())
            .content(patchBody)
            .contentType("application/json-patch+json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist());

        //remove subject
        addGrant = new ArrayList<Operation>();
        addGrant.add(new RemoveOperation("/sections/traditionalpagetwo/dc.subject/0"));
        patchBody = getPatchContent(addGrant);
        getClient(tokenSubmitter).perform(patch("/api/submission/workspaceitems/" + newWorkspaceItem.getID())
            .content(patchBody)
            .contentType("application/json-patch+json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist());

        //add an asbtract description
        Map<String, String> addValue = new HashMap<String, String>();
        final String newDescription = "New Description";
        addValue.put("value", newDescription);
        addGrant = new ArrayList<Operation>();
        addGrant.add(new AddOperation("/sections/traditionalpagetwo/dc.description.abstract",  List.of(addValue)));
        patchBody = getPatchContent(addGrant);
        getClient(tokenSubmitter).perform(patch("/api/submission/workspaceitems/" + newWorkspaceItem.getID())
            .content(patchBody)
            .contentType("application/json-patch+json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist());

        getClient(tokenSubmitter).perform(get("/api/submission/workspaceitems/" + newWorkspaceItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.correction.metadata").doesNotExist());

        AtomicReference<Integer> workflowItemIdRef = new AtomicReference<Integer>();

        getClient(tokenSubmitter).perform(post("/api/workflow/workflowitems")
            .content("/api/submission/workspaceitems/" + newWorkspaceItem.getID())
            .contentType(textUriContentType))
            .andExpect(status().isCreated())
            .andDo(result -> workflowItemIdRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        //check if the correction is present
        final String extraEntry = "ExtraEntry";
        getClient(tokenAdmin).perform(get("/api/workflow/workflowitems/" + workflowItemIdRef.get()))
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The array of browse index should have a size equals to 4
            .andExpect(jsonPath("$.sections.correction.metadata", hasSize(equalTo(3))))
            .andExpect(jsonPath("$.sections.correction.empty", is(false)))
            .andExpect(jsonPath("$.sections.correction.metadata",hasItem(matchMetadataCorrection(newTitle))))
            .andExpect(jsonPath("$.sections.correction.metadata",hasItem(matchMetadataCorrection(newDescription))))
            .andExpect(jsonPath("$.sections.correction.metadata",hasItem(matchMetadataCorrection(extraEntry))));

    }

    @Test
    public void checkCorrectionWithDuplicates() throws Exception {

        String tokenSubmitter = getAuthToken(eperson.getEmail(), password);

        //create a correction item
        getClient(tokenSubmitter).perform(post("/api/submission/workspaceitems")
                        .param("owningCollection", collection.getID().toString())
                        .param("relationship", "isCorrectionOfItem")
                        .param("item", itemToBeCorrected.getID().toString())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andDo(result -> workspaceItemIdRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        List<Relationship> relationshipList = relationshipService.findByItem(context, itemToBeCorrected);
        assert relationshipList.size() > 0;
        Item correctedItem  = relationshipList.get(0).getLeftItem();
        WorkspaceItem newWorkspaceItem = workspaceItemService.findByItem(context,correctedItem);

        //make a change on the title
        Map<String, String> value = new HashMap<String, String>();
        final String newDate = "2020-02-21";
        value.put("value", newDate);
        List<Operation> operations = new ArrayList<Operation>();
        operations.add(new ReplaceOperation("/sections/traditionalpageone/dc.date.issued/0", value));
        String patchBody = getPatchContent(operations);
        getClient(tokenSubmitter).perform(patch("/api/submission/workspaceitems/" + newWorkspaceItem.getID())
                        .content(patchBody)
                        .contentType("application/json-patch+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist());

        final String newTitle = "New Title";
        value.put("value", newTitle);
        operations = new ArrayList<Operation>();
        operations.add(new ReplaceOperation("/sections/traditionalpageone/dc.title/0", value));
        patchBody = getPatchContent(operations);
        getClient(tokenSubmitter).perform(patch("/api/submission/workspaceitems/" + newWorkspaceItem.getID())
                        .content(patchBody)
                        .contentType("application/json-patch+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist());

        //remove subject
        operations = new ArrayList<Operation>();
        operations.add(new RemoveOperation("/sections/traditionalpagetwo/dc.subject/0"));
        patchBody = getPatchContent(operations);
        getClient(tokenSubmitter).perform(patch("/api/submission/workspaceitems/" + newWorkspaceItem.getID())
                        .content(patchBody)
                        .contentType("application/json-patch+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist());

        //add an asbtract description
        Map<String, String> addValue = new HashMap<String, String>();
        final String newDescription = "New Description";
        addValue.put("value", newDescription);
        operations = new ArrayList<Operation>();
        operations.add(new AddOperation("/sections/traditionalpagetwo/dc.description.abstract",  List.of(addValue)));
        patchBody = getPatchContent(operations);
        getClient(tokenSubmitter).perform(patch("/api/submission/workspaceitems/" + newWorkspaceItem.getID())
                        .content(patchBody)
                        .contentType("application/json-patch+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist());

        getClient(tokenSubmitter).perform(get("/api/submission/workspaceitems/" + newWorkspaceItem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.correction.metadata").doesNotExist());

        AtomicReference<Integer> workflowItemIdRef = new AtomicReference<Integer>();

        getClient(tokenSubmitter).perform(post("/api/workflow/workflowitems")
                        .content("/api/submission/workspaceitems/" + newWorkspaceItem.getID())
                        .contentType(textUriContentType))
                .andExpect(status().isCreated())
                .andDo(result -> workflowItemIdRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        //check if the correction is present
        final String extraEntry = "ExtraEntry";
        getClient(tokenAdmin).perform(get("/api/workflow/workflowitems/" + workflowItemIdRef.get()))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The array of browse index should have a size equals to 4
                .andExpect(jsonPath("$.sections.correction.metadata", hasSize(equalTo(4))))
                .andExpect(jsonPath("$.sections.correction.empty", is(false)))
                .andExpect(jsonPath("$.sections.correction.metadata",hasItem(matchMetadataCorrection(newTitle))))
                .andExpect(jsonPath("$.sections.correction.metadata",hasItem(matchMetadataCorrection(newDate))))
                .andExpect(jsonPath("$.sections.correction.metadata",hasItem(matchMetadataCorrection(newDescription))))
                .andExpect(jsonPath("$.sections.correction.metadata",hasItem(matchMetadataCorrection(extraEntry))));

    }

    @Test
    public void checkEmptyCorrection() throws Exception {
        String tokenSubmitter = getAuthToken(eperson.getEmail(), password);

        //create a correction item
        getClient(tokenSubmitter).perform(post("/api/submission/workspaceitems")
                .param("owningCollection", collection.getID().toString())
                .param("relationship", "isCorrectionOfItem")
                .param("item", itemToBeCorrected.getID().toString())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andDo(result -> workspaceItemIdRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        List<Relationship> relationshipList = relationshipService.findByItem(context, itemToBeCorrected);
        assert relationshipList.size() > 0;
        Item correctedItem = relationshipList.get(0).getLeftItem();
        WorkspaceItem newWorkspaceItem = workspaceItemService.findByItem(context, correctedItem);

        AtomicReference<Integer> workflowItemIdRef = new AtomicReference<Integer>();

        getClient(tokenSubmitter).perform(post("/api/workflow/workflowitems")
            .content("/api/submission/workspaceitems/" + newWorkspaceItem.getID())
            .contentType(textUriContentType))
            .andExpect(status().isCreated())
            .andDo(result -> workflowItemIdRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        //check if the correction section is empty on relation item
        getClient(tokenAdmin).perform(get("/api/workflow/workflowitems/" + workflowItemIdRef.get()))
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The array of browse index should have a size greater or equals to 1
            .andExpect(jsonPath("$.sections.correction.metadata", empty()))
            .andExpect(jsonPath("$.sections.correction.empty", is(true)));

    }

    private static Matcher<?> matchMetadataCorrection(String value) {
        return Matchers.anyOf(
                hasJsonPath("$.newValues[0]", equalTo(value)),
                hasJsonPath("$.oldValues[0]", equalTo(value)));
    }

    private void deletePoolTask(PoolTask poolTask) {
        try {
            poolTaskService.delete(context, poolTask);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteWorkflowItem(XmlWorkflowItem workflowItem) {
        try {
            xmlWorkflowItemService.delete(context, workflowItem);
        } catch (SQLException | AuthorizeException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
