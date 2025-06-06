/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.WorkspaceItemDAO;
import org.dspace.content.logic.Filter;
import org.dspace.content.logic.FilterUtils;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.event.Event;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.Identifier;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the WorkspaceItem object.
 * This class is responsible for all business logic calls for the WorkspaceItem object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class WorkspaceItemServiceImpl implements WorkspaceItemService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(WorkspaceItemServiceImpl.class);

    @Autowired(required = true)
    protected WorkspaceItemDAO workspaceItemDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected CollectionService collectionService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected WorkflowService workflowService;

    @Autowired
    private MetadataFieldService metadataFieldService;
    @Autowired
    private MetadataValueService metadataValueService;


    @Autowired(required = true)
    protected DOIService doiService;


    protected WorkspaceItemServiceImpl() {

    }

    @Override
    public WorkspaceItem find(Context context, int id) throws SQLException {
        WorkspaceItem workspaceItem = workspaceItemDAO.findByID(context, WorkspaceItem.class, id);

        if (workspaceItem == null) {
            if (log.isDebugEnabled()) {
                log.debug(LogHelper.getHeader(context, "find_workspace_item",
                                               "not_found,workspace_item_id=" + id));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(LogHelper.getHeader(context, "find_workspace_item",
                                               "workspace_item_id=" + id));
            }
        }
        return workspaceItem;
    }

    @Override
    public WorkspaceItem create(Context context, Collection collection, boolean template)
            throws AuthorizeException, SQLException {
        return create(context, collection, null, template, false);
    }

    @Override
    public WorkspaceItem create(Context context, Collection collection, boolean template, boolean isNewVersion)
            throws AuthorizeException, SQLException {
        return create(context, collection, null, template, isNewVersion);
    }

    @Override
    public WorkspaceItem create(Context context, Collection collection, UUID uuid, boolean template,
                                boolean isNewVersion)
        throws AuthorizeException, SQLException {
        // Check the user has permission to ADD to the collection
        authorizeService.authorizeAction(context, collection, Constants.ADD);

        WorkspaceItem workspaceItem = workspaceItemDAO.create(context, new WorkspaceItem());
        workspaceItem.setCollection(collection);


        // Create an item
        Item item;
        if (uuid != null) {
            item = itemService.create(context, workspaceItem, uuid);
        } else {
            item = itemService.create(context, workspaceItem);
        }
        item.setSubmitter(context.getCurrentUser());

        // Now create the policies for the submitter to modify item and contents (bitstreams, bundles)
        int[] actionIds = { Constants.READ, Constants.WRITE, Constants.ADD, Constants.REMOVE, Constants.DELETE };
        for (int actionId : actionIds) {
            authorizeService.addPolicy(context, item, actionId, item.getSubmitter(), ResourcePolicy.TYPE_SUBMISSION);
        }

        if (collectionService.isSharedWorkspace(context, collection)) {
            addPoliciesToSubmitterGroup(context, item, collection, actionIds);
        }

        // Copy template if appropriate
        itemService.populateWithTemplateItemMetadata(context, collection, template, item);

        itemService.update(context, item);

        // If configured, register identifiers (eg handle, DOI) now. This is typically used with the Show Identifiers
        // submission step which previews minted handles and DOIs during the submission process. Default: false
        // Additional check needed: if we are creating a new version of an existing item we skip the identifier
        // generation here, as this will be performed when the new version is created in VersioningServiceImpl
        if (DSpaceServicesFactory.getInstance().getConfigurationService()
                .getBooleanProperty("identifiers.submission.register", false) && !isNewVersion) {
            try {
                // Get map of filters to use for identifier types, while the item is in progress
                Map<Class<? extends Identifier>, Filter> filters = FilterUtils.getIdentifierFilters(true);
                IdentifierServiceFactory.getInstance().getIdentifierService().register(context, item, filters);
                // Look for a DOI and move it to PENDING
                DOI doi = doiService.findDOIByDSpaceObject(context, item);
                if (doi != null) {
                    doi.setStatus(DOIIdentifierProvider.PENDING);
                    doiService.update(context, doi);
                }
            } catch (IdentifierException e) {
                log.error("Could not register identifier(s) for item {}: {}", item.getID(), e.getMessage());
            }
        }

        workspaceItem.setItem(item);

        log.info(LogHelper.getHeader(context, "create_workspace_item",
                                      "workspace_item_id=" + workspaceItem.getID()
                                          + "item_id=" + item.getID() + "collection_id="
                                          + collection.getID()));

        context.addEvent(new Event(Event.MODIFY, Constants.ITEM, item.getID(), null,
                itemService.getIdentifiers(context, item)));

        return workspaceItem;
    }

    @Override
    public WorkspaceItem create(Context c, WorkflowItem workflowItem) throws SQLException, AuthorizeException {
        WorkspaceItem potentialDuplicate = findByItem(c, workflowItem.getItem());
        if (potentialDuplicate != null) {
            throw new IllegalArgumentException(String.format(
                "A workspace item referring to item %s already exists (%d)",
                workflowItem.getItem().getID(),
                potentialDuplicate.getID()
            ));
        }
        WorkspaceItem workspaceItem = workspaceItemDAO.create(c, new WorkspaceItem());
        workspaceItem.setItem(workflowItem.getItem());
        workspaceItem.setCollection(workflowItem.getCollection());
        update(c, workspaceItem);
        return workspaceItem;
    }

    @Override
    public List<WorkspaceItem> findByEPerson(Context context, EPerson ep) throws SQLException {
        return workspaceItemDAO.findByEPerson(context, ep);
    }

    @Override
    public List<WorkspaceItem> findByEPerson(Context context, EPerson ep, Integer limit, Integer offset)
        throws SQLException {
        return workspaceItemDAO.findByEPerson(context, ep, limit, offset);
    }

    @Override
    public List<WorkspaceItem> findByCollection(Context context, Collection collection) throws SQLException {
        return workspaceItemDAO.findByCollection(context, collection);
    }

    @Override
    public WorkspaceItem findByItem(Context context, Item item) throws SQLException {
        return workspaceItemDAO.findByItem(context, item);
    }

    @Override
    public List<WorkspaceItem> findAll(Context context) throws SQLException {
        return workspaceItemDAO.findAll(context);
    }

    @Override
    public List<WorkspaceItem> findAll(Context context, Integer limit, Integer offset) throws SQLException {
        return workspaceItemDAO.findAll(context, limit, offset);
    }

    @Override
    public void update(Context context, WorkspaceItem workspaceItem) throws SQLException, AuthorizeException {
        // Authorisation is checked by the item.update() method below

        log.info(LogHelper.getHeader(context, "update_workspace_item",
                                      "workspace_item_id=" + workspaceItem.getID()));

        // Update the item
        itemService.update(context, workspaceItem.getItem());

        // Update ourselves
        workspaceItemDAO.save(context, workspaceItem);
    }

    @Override
    public void deleteAll(Context context, WorkspaceItem workspaceItem)
        throws SQLException, AuthorizeException, IOException {

        Item item = workspaceItem.getItem();
        if (isNotAuthorizedToDelete(context, item)) {
            // Not an admit, not the submitter
            throw new AuthorizeException("Must be an administrator or the submitter to delete a workspace item");
        }

        log.info(LogHelper.getHeader(context, "delete_workspace_item",
                                      "workspace_item_id=" + workspaceItem.getID() + "item_id=" + item.getID()
                                          + "collection_id=" + workspaceItem.getCollection().getID()));

        // Need to delete the workspaceitem row first since it refers
        // to item ID
        workspaceItemDAO.delete(context, workspaceItem);

        // Delete item
        itemService.delete(context, item);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return workspaceItemDAO.countRows(context);
    }

    @Override
    public int countByEPerson(Context context, EPerson ep) throws SQLException {
        return workspaceItemDAO.countRows(context, ep);
    }

    @Override
    public List<Map.Entry<Integer, Long>> getStageReachedCounts(Context context) throws SQLException {
        return workspaceItemDAO.getStageReachedCounts(context);
    }

    @Override
    public void deleteWrapper(Context context, WorkspaceItem workspaceItem) throws SQLException, AuthorizeException {
        // Check authorisation. We check permissions on the enclosed item.
        Item item = workspaceItem.getItem();
        authorizeService.authorizeAction(context, item, Constants.WRITE);

        log.info(LogHelper.getHeader(context, "delete_workspace_item",
                                      "workspace_item_id=" + workspaceItem.getID() + "item_id=" + item.getID()
                                          + "collection_id=" + workspaceItem.getCollection().getID()));

        //        deleteSubmitPermissions();

        workspaceItemDAO.delete(context, workspaceItem);

    }

    @Override
    public void move(Context context, WorkspaceItem source, Collection fromCollection, Collection toCollection)
        throws DCInputsReaderException {
        source.setCollection(toCollection);

        List<MetadataValue> remove = new ArrayList<>();
        List<String> diff = Util.differenceInSubmissionFields(fromCollection, toCollection);
        for (String toRemove : diff) {
            for (MetadataValue value : source.getItem().getMetadata()) {
                if (value.getMetadataField().toString('.').equals(toRemove)) {
                    remove.add(value);
                }
            }
        }

        source.getItem().removeMetadata(remove);

    }

    private void addPoliciesToSubmitterGroup(Context context, Item item, Collection collection, int[] actionIds)
        throws SQLException, AuthorizeException {

        Group submitters = collection.getSubmitters();
        if (submitters == null) {
            return;
        }

        for (int actionId : actionIds) {
            authorizeService.addPolicy(context, item, actionId, submitters, ResourcePolicy.TYPE_SUBMISSION);
        }

    }

    private boolean isNotAuthorizedToDelete(Context context, Item item) throws SQLException {
        EPerson submitter = item.getSubmitter();
        EPerson currentUser = context.getCurrentUser();
        return !authorizeService.isAdmin(context)
            && (submitter == null || (currentUser == null) || (!submitter.getID().equals(currentUser.getID())))
            && !authorizeService.authorizeActionBoolean(context, item, Constants.DELETE);
    }

}
