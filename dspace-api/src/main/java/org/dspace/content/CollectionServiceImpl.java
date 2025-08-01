/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.apache.commons.lang3.BooleanUtils.toBoolean;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.browse.ItemCounter;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogHelper;
import org.dspace.core.service.LicenseService;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverQuery.SORT_ORDER;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.SubscribeService;
import org.dspace.event.Event;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.services.ConfigurationService;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the Collection object.
 * This class is responsible for all business logic calls for the Collection object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class CollectionServiceImpl extends DSpaceObjectServiceImpl<Collection> implements CollectionService {

    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(CollectionServiceImpl.class);

    @Autowired(required = true)
    protected CollectionDAO collectionDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected ResourcePolicyService resourcePolicyService;
    @Autowired(required = true)
    protected BitstreamService bitstreamService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected CommunityService communityService;
    @Autowired(required = true)
    protected GroupService groupService;
    @Autowired(required = true)
    protected IdentifierService identifierService;

    @Autowired(required = true)
    protected LicenseService licenseService;
    @Autowired(required = true)
    protected SubscribeService subscribeService;
    @Autowired(required = true)
    protected CrisMetricsService crisMetricsService;
    @Autowired(required = true)
    protected WorkspaceItemService workspaceItemService;
    @Autowired(required = true)
    protected HarvestedCollectionService harvestedCollectionService;

    @Autowired(required = true)
    protected XmlWorkflowFactory workflowFactory;

    @Autowired(required = true)
    protected CollectionRoleService collectionRoleService;

    @Autowired(required = true)
    protected SearchService searchService;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Autowired(required = true)
    protected RelationshipService relationshipService;

    @Autowired(required = true)
    protected WorkflowItemService<?> workflowItemService;

    @Autowired
    protected ItemCounter itemCounter;

    protected CollectionServiceImpl() {
        super();
    }

    @Override
    public Collection create(Context context, Community community) throws SQLException, AuthorizeException {
        return create(context, community, null);
    }

    @Override
    public Collection create(Context context, Community community, String handle)
            throws SQLException, AuthorizeException {
        return create(context, community, handle, null);
    }

    @Override
    public Collection create(Context context, Community community,
                             String handle, UUID uuid) throws SQLException, AuthorizeException {
        if (community == null) {
            throw new IllegalArgumentException("Community cannot be null when creating a new collection.");
        }

        Collection newCollection;
        if (uuid != null) {
            newCollection = collectionDAO.create(context, new Collection(uuid));
        }  else {
            newCollection = collectionDAO.create(context, new Collection());
        }
        //Add our newly created collection to our community, authorization checks occur in THIS method
        communityService.addCollection(context, community, newCollection);

        // create the default authorization policy for collections
        // of 'anonymous' READ
        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);


        authorizeService.createResourcePolicy(context, newCollection, anonymousGroup, null, Constants.READ, null);
        // now create the default policies for submitted items
        authorizeService
                .createResourcePolicy(context, newCollection, anonymousGroup, null, Constants.DEFAULT_ITEM_READ, null);
        authorizeService
                .createResourcePolicy(context, newCollection, anonymousGroup, null,
                        Constants.DEFAULT_BITSTREAM_READ, null);

        collectionDAO.save(context, newCollection);

        //Update our collection so we have a collection identifier
        try {
            if (handle == null) {
                identifierService.register(context, newCollection);
            } else {
                identifierService.register(context, newCollection, handle);
            }
        } catch (IllegalStateException | IdentifierException ex) {
            throw new IllegalStateException(ex);
        }

        context.addEvent(new Event(Event.CREATE, Constants.COLLECTION,
                newCollection.getID(), newCollection.getHandle(),
                getIdentifiers(context, newCollection)));

        log.info(LogHelper.getHeader(context, "create_collection",
                "collection_id=" + newCollection.getID())
                + ",handle=" + newCollection.getHandle());

        return newCollection;
    }

    @Override
    public List<Collection> findAll(Context context) throws SQLException {
        MetadataField nameField = metadataFieldService.findByElement(context, MetadataSchemaEnum.DC.getName(),
                                                                     "title", null);
        if (nameField == null) {
            throw new IllegalArgumentException(
                "Required metadata field '" + MetadataSchemaEnum.DC.getName() + ".title' doesn't exist!");
        }

        return collectionDAO.findAll(context, nameField);
    }

    @Override
    public List<Collection> findAll(Context context, Integer limit, Integer offset) throws SQLException {
        MetadataField nameField = metadataFieldService.findByElement(context, MetadataSchemaEnum.DC.getName(),
                                                                     "title", null);
        if (nameField == null) {
            throw new IllegalArgumentException(
                "Required metadata field '" + MetadataSchemaEnum.DC.getName() + ".title' doesn't exist!");
        }

        return collectionDAO.findAll(context, nameField, limit, offset);
    }

    @Override
    public List<Collection> findAuthorizedOptimized(Context context, int actionID) throws SQLException {
        if (!configurationService
            .getBooleanProperty("org.dspace.content.Collection.findAuthorizedPerformanceOptimize", false)) {
            // Fallback to legacy query if config says so. The rationale could be that a site found a bug.
            return findAuthorized(context, null, actionID);
        }

        List<Collection> myResults = new ArrayList<>();

        if (authorizeService.isAdmin(context)) {
            return findAll(context);
        }

        //Check eperson->policy
        List<Collection> directToCollection = findDirectMapped(context, actionID);
        for (int i = 0; i < directToCollection.size(); i++) {
            if (!myResults.contains(directToCollection.get(i))) {
                myResults.add(directToCollection.get(i));
            }
        }

        //Check eperson->groups->policy
        List<Collection> groupToCollection = findGroupMapped(context, actionID);

        for (Collection aGroupToCollection : groupToCollection) {
            if (!myResults.contains(aGroupToCollection)) {
                myResults.add(aGroupToCollection);
            }
        }

        //Check eperson->groups->groups->policy->collection
        //i.e. Malcolm Litchfield is a member of OSU_Press_Embargo,
        // which is a member of: COLLECTION_24_ADMIN, COLLECTION_24_SUBMIT
        List<Collection> group2GroupToCollection = findGroup2GroupMapped(context, actionID);

        for (Collection aGroup2GroupToCollection : group2GroupToCollection) {
            if (!myResults.contains(aGroup2GroupToCollection)) {
                myResults.add(aGroup2GroupToCollection);
            }
        }

        //TODO Check eperson->groups->groups->policy->community


        //TODO Check eperson->groups->policy->community
        // i.e. Typical Community Admin -- name.# > COMMUNITY_10_ADMIN > Ohio State University Press

        //Check eperson->comm-admin
        List<Collection> group2commCollections = findGroup2CommunityMapped(context);
        for (Collection group2commCollection : group2commCollections) {
            if (!myResults.contains(group2commCollection)) {
                myResults.add(group2commCollection);
            }
        }


        // Return the collections, sorted alphabetically
        Collections.sort(myResults, new CollectionNameComparator());

        return myResults;
    }

    @Override
    public List<Collection> findDirectMapped(Context context, int actionID) throws SQLException {
        return collectionDAO
            .findAuthorized(context, context.getCurrentUser(), Arrays.asList(Constants.ADD, Constants.ADMIN));
    }

    @Override
    public List<Collection> findGroup2CommunityMapped(Context context) throws SQLException {
        List<Community> communities = communityService
            .findAuthorizedGroupMapped(context, Arrays.asList(Constants.ADD, Constants.ADMIN));
        List<Collection> collections = new ArrayList<>();
        for (Community community : communities) {
            collections.addAll(community.getCollections());
        }
        return collections;
    }

    @Override
    public List<Collection> findGroup2GroupMapped(Context context, int actionID) throws SQLException {
        return collectionDAO
            .findAuthorizedByGroup(context, context.getCurrentUser(), Collections.singletonList(actionID));
    }

    @Override
    public List<Collection> findGroupMapped(Context context, int actionID) throws SQLException {
        List<Community> communities = communityService
            .findAuthorized(context, Arrays.asList(Constants.ADD, Constants.ADMIN));
        List<Collection> collections = new ArrayList<>();
        for (Community community : communities) {
            collections.addAll(community.getCollections());
        }
        return collections;
    }

    @Override
    public Collection find(Context context, UUID id) throws SQLException {
        return collectionDAO.findByID(context, Collection.class, id);
    }

    @Override
    public void setMetadataSingleValue(Context context, Collection collection,
            MetadataFieldName field, String language, String value)
            throws MissingResourceException, SQLException {
        if (field.equals(MD_NAME) && (value == null || value.trim().equals(""))) {
            try {
                value = I18nUtil.getMessage("org.dspace.content.untitled");
            } catch (MissingResourceException e) {
                value = "Untitled";
            }
        }

        /*
         * Set metadata field to null if null
         * and trim strings to eliminate excess
         * whitespace.
         */
        if (value == null) {
            clearMetadata(context, collection, field.schema, field.element, field.qualifier, Item.ANY);
            collection.setMetadataModified();
        } else {
            super.setMetadataSingleValue(context, collection, field, null, value);
        }

        collection.addDetails(field.toString());
    }

    @Override
    public Bitstream setLogo(Context context, Collection collection, InputStream is)
        throws AuthorizeException, IOException, SQLException {
        // Check authorisation
        // authorized to remove the logo when DELETE rights
        // authorized when canEdit
        if (!((is == null) && authorizeService.authorizeActionBoolean(
            context, collection, Constants.DELETE))) {
            canEdit(context, collection, true);
        }

        // First, delete any existing logo
        if (collection.getLogo() != null) {
            bitstreamService.delete(context, collection.getLogo());
        }

        if (is == null) {
            collection.setLogo(null);
            log.info(LogHelper.getHeader(context, "remove_logo",
                                          "collection_id=" + collection.getID()));
        } else {
            Bitstream newLogo = bitstreamService.create(context, is);
            collection.setLogo(newLogo);

            // now create policy for logo bitstream
            // to match our READ policy
            List<ResourcePolicy> policies = authorizeService.getPolicies(context, collection);
            authorizeService.addPolicies(context, policies, newLogo);

            log.info(LogHelper.getHeader(context, "set_logo",
                                          "collection_id=" + collection.getID() + "logo_bitstream_id="
                                              + newLogo.getID()));
        }

        collection.setModified();
        return collection.getLogo();
    }

    @Override
    public Group createWorkflowGroup(Context context, Collection collection, int step)
        throws SQLException, AuthorizeException {
        // Check authorisation - Must be an Admin to create Workflow Group
        AuthorizeUtil.authorizeManageWorkflowsGroup(context, collection);

        if (getWorkflowGroup(context, collection, step) == null) {
            //turn off authorization so that Collection Admins can create Collection Workflow Groups
            context.turnOffAuthorisationSystem();
            Group g = groupService.create(context);
            groupService.setName(g,
                                 "COLLECTION_" + collection.getID() + "_WORKFLOW_STEP_" + step);
            groupService.update(context, g);
            context.restoreAuthSystemState();
            setWorkflowGroup(context, collection, step, g);
        }

        return getWorkflowGroup(context, collection, step);
    }

    @Override
    public void setWorkflowGroup(Context context, Collection collection, int step, Group group)
        throws SQLException {
        Workflow workflow = null;
        try {
            workflow = workflowFactory.getWorkflow(collection);
        } catch (WorkflowConfigurationException e) {
            log.error(LogHelper.getHeader(context, "setWorkflowGroup",
                    "collection_id=" + collection.getID() + " " + e.getMessage()), e);
        }
        if (!StringUtils.equals(workflowFactory.getDefaultWorkflow().getID(), workflow.getID())) {
            throw new IllegalArgumentException(
                    "setWorkflowGroup can be used only on collection with the default basic dspace workflow. "
                    + "Instead, the collection: "
                            + collection.getID() + " has the workflow: " + workflow.getID());
        }
        String roleId;

        switch (step) {
            case 1:
                roleId = CollectionRoleService.LEGACY_WORKFLOW_STEP1_NAME;
                break;
            case 2:
                roleId = CollectionRoleService.LEGACY_WORKFLOW_STEP2_NAME;
                break;
            case 3:
                roleId = CollectionRoleService.LEGACY_WORKFLOW_STEP3_NAME;
                break;
            default:
                throw new IllegalArgumentException("Illegal step count: " + step);
        }

        CollectionRole colRole = collectionRoleService.find(context, collection, roleId);
        if (colRole == null) {
            if (group != null) {
                colRole = collectionRoleService.create(context, collection, roleId, group);
            }
        } else {
            if (group != null) {
                colRole.setGroup(group);
                collectionRoleService.update(context, colRole);
            } else {
                collectionRoleService.delete(context, colRole);
            }
        }
        collection.setModified();
    }

    @Override
    public Group getWorkflowGroup(Context context, Collection collection, int step) {
        String roleId;

        switch (step) {
            case 1:
                roleId = CollectionRoleService.LEGACY_WORKFLOW_STEP1_NAME;
                break;
            case 2:
                roleId = CollectionRoleService.LEGACY_WORKFLOW_STEP2_NAME;
                break;
            case 3:
                roleId = CollectionRoleService.LEGACY_WORKFLOW_STEP3_NAME;
                break;
            default:
                throw new IllegalArgumentException("Illegal step count: " + step);
        }

        CollectionRole colRole;
        try {
            colRole = collectionRoleService.find(context, collection, roleId);
            if (colRole != null) {
                return colRole.getGroup();
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Group createSubmitters(Context context, Collection collection) throws SQLException, AuthorizeException {
        // Check authorisation - Must be an Admin to create Submitters Group
        AuthorizeUtil.authorizeManageSubmittersGroup(context, collection);

        Group submitters = collection.getSubmitters();
        if (submitters == null) {
            //turn off authorization so that Collection Admins can create Collection Submitters
            context.turnOffAuthorisationSystem();
            submitters = groupService.create(context);
            context.restoreAuthSystemState();

            groupService.setName(submitters,
                                 "COLLECTION_" + collection.getID() + "_SUBMIT");
            groupService.update(context, submitters);
        }

        // register this as the submitter group
        collection.setSubmitters(submitters);

        authorizeService.addPolicy(context, collection, Constants.ADD, submitters);

        return submitters;
    }

    @Override
    public void removeSubmitters(Context context, Collection collection) throws SQLException, AuthorizeException {
        // Check authorisation - Must be an Admin to delete Submitters Group
        AuthorizeUtil.authorizeManageSubmittersGroup(context, collection);

        // just return if there is no administrative group.
        if (collection.getSubmitters() == null) {
            return;
        }

        // Remove the link to the collection table.
        collection.setSubmitters(null);
    }

    @Override
    public Group createAdministrators(Context context, Collection collection) throws SQLException, AuthorizeException {
        // Check authorisation - Must be an Admin to create more Admins
        AuthorizeUtil.authorizeManageAdminGroup(context, collection);

        Group admins = collection.getAdministrators();
        if (admins == null) {
            //turn off authorization so that Community Admins can create Collection Admins
            context.turnOffAuthorisationSystem();
            admins = groupService.create(context);
            context.restoreAuthSystemState();

            groupService.setName(admins, "COLLECTION_" + collection.getID() + "_ADMIN");
            groupService.update(context, admins);
        }

        authorizeService.addPolicy(context, collection,
                                   Constants.ADMIN, admins);

        // register this as the admin group
        collection.setAdmins(admins);
        context.addEvent(new Event(Event.MODIFY, Constants.COLLECTION, collection.getID(),
                                              null, getIdentifiers(context, collection)));
        return admins;
    }

    @Override
    public void removeAdministrators(Context context, Collection collection) throws SQLException, AuthorizeException {
        // Check authorisation - Must be an Admin of the parent community to delete Admin Group
        AuthorizeUtil.authorizeRemoveAdminGroup(context, collection);

        Group admins = collection.getAdministrators();
        // just return if there is no administrative group.
        if (admins == null) {
            return;
        }

        // Remove the link to the collection table.
        collection.setAdmins(null);
        context.addEvent(new Event(Event.MODIFY, Constants.COLLECTION, collection.getID(),
                                              null, getIdentifiers(context, collection)));
    }

    @Override
    public String getLicense(Collection collection) {
        String license = getMetadataFirstValue(collection, CollectionService.MD_LICENSE, Item.ANY);

        if (license == null || license.trim().equals("")) {
            // Fallback to site-wide default
            license = licenseService.getDefaultSubmissionLicense();
        }

        return license;
    }

    @Override
    public boolean hasCustomLicense(Collection collection) {
        String license = collection.getLicenseCollection();
        return StringUtils.isNotBlank(license);
    }

    @Override
    public void createTemplateItem(Context context, Collection collection) throws SQLException, AuthorizeException {
        // Check authorisation
        AuthorizeUtil.authorizeManageTemplateItem(context, collection);

        if (collection.getTemplateItem() == null) {
            Item template = itemService.createTemplateItem(context, collection);
            collection.setTemplateItem(template);

            log.info(LogHelper.getHeader(context, "create_template_item",
                                          "collection_id=" + collection.getID() + ",template_item_id="
                                              + template.getID()));
        }
    }

    @Override
    public void removeTemplateItem(Context context, Collection collection)
        throws SQLException, AuthorizeException, IOException {
        // Check authorisation
        AuthorizeUtil.authorizeManageTemplateItem(context, collection);

        Item template = collection.getTemplateItem();

        if (template != null) {
            log.info(LogHelper.getHeader(context, "remove_template_item",
                                          "collection_id=" + collection.getID() + ",template_item_id="
                                              + template.getID()));
            // temporarily turn off auth system, we have already checked the permission on the top of the method
            // check it again will fail because we have already broken the relation between the collection and the item
            context.turnOffAuthorisationSystem();
            collection.setTemplateItem(null);
            itemService.delete(context, template);
            context.restoreAuthSystemState();
        }

        context.addEvent(new Event(Event.MODIFY, Constants.COLLECTION,
                                   collection.getID(), "remove_template_item", getIdentifiers(context, collection)));
    }

    @Override
    public void addItem(Context context, Collection collection, Item item) throws SQLException, AuthorizeException {
        // Check authorisation
        authorizeService.authorizeAction(context, collection, Constants.ADD);

        log.info(LogHelper.getHeader(context, "add_item", "collection_id="
            + collection.getID() + ",item_id=" + item.getID()));

        // Create mapping
        // We do NOT add the item to the collection template since we would have to load in all our items
        // Instead we add the collection to an item which works in the same way.
        if (!item.getCollections().contains(collection)) {
            item.addCollection(collection);
        }

        context.addEvent(new Event(Event.ADD, Constants.COLLECTION, collection.getID(),
                                   Constants.ITEM, item.getID(), item.getHandle(),
                                   getIdentifiers(context, collection)));
    }

    @Override
    public void removeItem(Context context, Collection collection, Item item)
        throws SQLException, AuthorizeException, IOException {
        // Check authorisation
        authorizeService.authorizeAction(context, collection, Constants.REMOVE);

        //Check if we orphaned our poor item
        if (item.getCollections().size() == 1) {
            // Orphan; delete it
            itemService.delete(context, item);
        } else {
            //Remove the item from the collection if we have multiple collections
            item.removeCollection(collection);

        }

        context.addEvent(new Event(Event.REMOVE, Constants.COLLECTION,
                                   collection.getID(), Constants.ITEM, item.getID(), item.getHandle(),
                                   getIdentifiers(context, collection)));
    }

    @Override
    public void update(Context context, Collection collection) throws SQLException, AuthorizeException {
        // Check authorisation
        canEdit(context, collection, true);

        log.info(LogHelper.getHeader(context, "update_collection",
                                      "collection_id=" + collection.getID()));

        super.update(context, collection);
        collectionDAO.save(context, collection);

        if (collection.isModified()) {
            context.addEvent(new Event(Event.MODIFY, Constants.COLLECTION,
                                       collection.getID(), null, getIdentifiers(context, collection)));
            collection.clearModified();
        }
        if (collection.isMetadataModified()) {
            context.addEvent(new Event(Event.MODIFY_METADATA, Constants.COLLECTION, collection.getID(),
                                         collection.getDetails(),getIdentifiers(context, collection)));
            collection.clearModified();
        }
        collection.clearDetails();
    }

    @Override
    public boolean canEditBoolean(Context context, Collection collection) throws SQLException {
        return canEditBoolean(context, collection, true);
    }

    @Override
    public boolean canEditBoolean(Context context, Collection collection, boolean useInheritance) throws SQLException {
        try {
            canEdit(context, collection, useInheritance);

            return true;
        } catch (AuthorizeException e) {
            return false;
        }
    }

    @Override
    public void canEdit(Context context, Collection collection) throws SQLException, AuthorizeException {
        canEdit(context, collection, true);
    }

    @Override
    public void canEdit(Context context, Collection collection, boolean useInheritance)
        throws SQLException, AuthorizeException {
        List<Community> parents = communityService.getAllParents(context, collection);
        for (Community parent : parents) {
            if (authorizeService.authorizeActionBoolean(context, parent,
                                                        Constants.WRITE, useInheritance)) {
                return;
            }

            if (authorizeService.authorizeActionBoolean(context, parent,
                                                        Constants.ADD, useInheritance)) {
                return;
            }
        }
        authorizeService.authorizeAction(context, collection, Constants.WRITE, useInheritance);
    }

    @Override
    public void delete(Context context, Collection collection) throws SQLException, AuthorizeException, IOException {
        crisMetricsService.deleteByResourceID(context, collection);
        log.info(LogHelper.getHeader(context, "delete_collection", "collection_id=" + collection.getID()));

        // remove harvested collections.
        HarvestedCollection hc = harvestedCollectionService.find(context, collection);
        if (hc != null) {
            harvestedCollectionService.delete(context, hc);
        }

        context.addEvent(new Event(Event.DELETE, Constants.COLLECTION,
                                   collection.getID(), collection.getHandle(), getIdentifiers(context, collection)));

        // remove subscriptions - hmm, should this be in Subscription.java?
        subscribeService.deleteByDspaceObject(context, collection);
        crisMetricsService.deleteByResourceID(context, collection);
        // Remove Template Item
        removeTemplateItem(context, collection);

        // Remove items
        // Remove items
        Iterator<Item> items = itemService.findAllByCollection(context, collection);
        while (items.hasNext()) {
            Item item = items.next();
//            items.remove();
            if (itemService.isOwningCollection(item, collection)) {
                // the collection to be deleted is the owning collection, thus remove
                // the item from all collections it belongs to
                itemService.delete(context, item);
            } else {
                // the item was only mapped to this collection, so just remove it
                removeItem(context, collection, item);
            }
        }


        // Delete bitstream logo
        setLogo(context, collection, null);

        Iterator<WorkspaceItem> workspaceItems = workspaceItemService.findByCollection(context, collection).iterator();
        while (workspaceItems.hasNext()) {
            WorkspaceItem workspaceItem = workspaceItems.next();
            workspaceItems.remove();
            workspaceItemService.deleteAll(context, workspaceItem);
        }


        WorkflowServiceFactory.getInstance().getWorkflowService().deleteCollection(context, collection);
        WorkflowServiceFactory.getInstance().getWorkflowItemService().deleteByCollection(context, collection);

        //  get rid of the content count cache if it exists
        // Remove any Handle
        handleService.unbindHandle(context, collection);

        // Remove any workflow roles
        collectionRoleService.deleteByCollection(context, collection);

        collection.getResourcePolicies().clear();

        // Remove default administrators group
        Group g = collection.getAdministrators();

        if (g != null) {
            collection.setAdmins(null);
            groupService.delete(context, g);
        }

        // Remove default submitters group
        g = collection.getSubmitters();

        if (g != null) {
            collection.setSubmitters(null);
            groupService.delete(context, g);
        }

        Iterator<Community> owningCommunities = collection.getCommunities().iterator();
        while (owningCommunities.hasNext()) {
            Community owningCommunity = owningCommunities.next();
            collection.removeCommunity(owningCommunity);
            owningCommunity.removeCollection(collection);
        }

        collectionDAO.delete(context, collection);
    }

    @Override
    public int getSupportsTypeConstant() {
        return Constants.COLLECTION;
    }

    @Override
    public List<Collection> findAuthorized(Context context, Community community, int actionID) throws SQLException {
        List<Collection> myResults = new ArrayList<>();

        List<Collection> myCollections;

        if (community != null) {
            myCollections = community.getCollections();
        } else {
            myCollections = findAll(context);
        }

        // now build a list of collections you have authorization for
        for (Collection myCollection : myCollections) {
            if (authorizeService.authorizeActionBoolean(context,
                                                        myCollection, actionID)) {
                myResults.add(myCollection);
            }
        }
        return myResults;
    }

    @Override
    public Collection findByGroup(Context context, Group group) throws SQLException {
        return collectionDAO.findByGroup(context, group);
    }

    @Override
    public List<Collection> findCollectionsWithSubscribers(Context context) throws SQLException {
        return collectionDAO.findCollectionsWithSubscribers(context);
    }

    @Override
    public DSpaceObject getAdminObject(Context context, Collection collection, int action) throws SQLException {
        DSpaceObject adminObject = null;
        Community community = null;
        List<Community> communities = collection.getCommunities();
        if (CollectionUtils.isNotEmpty(communities)) {
            community = communities.get(0);
        }

        switch (action) {
            case Constants.REMOVE:
                if (AuthorizeConfiguration.canCollectionAdminPerformItemDeletion()) {
                    adminObject = collection;
                } else if (AuthorizeConfiguration.canCommunityAdminPerformItemDeletion()) {
                    adminObject = community;
                }
                break;

            case Constants.DELETE:
                if (AuthorizeConfiguration.canCommunityAdminPerformSubelementDeletion()) {
                    adminObject = community;
                }
                break;
            default:
                adminObject = collection;
                break;
        }
        return adminObject;
    }

    @Override
    public DSpaceObject getParentObject(Context context, Collection collection) throws SQLException {
        List<Community> communities = collection.getCommunities();
        if (CollectionUtils.isNotEmpty(communities)) {
            return communities.get(0);
        } else {
            return null;
        }
    }

    @Override
    public void updateLastModified(Context context, Collection collection) throws SQLException, AuthorizeException {
        //Also fire a modified event since the collection HAS been modified
        context.addEvent(new Event(Event.MODIFY, Constants.COLLECTION,
                                   collection.getID(), null, getIdentifiers(context, collection)));
    }

    @Override
    public Collection findByIdOrLegacyId(Context context, String id) throws SQLException {
        try {
            if (StringUtils.isNumeric(id)) {
                return findByLegacyId(context, Integer.parseInt(id));
            } else {
                return find(context, UUID.fromString(id));
            }
        } catch (IllegalArgumentException e) {
            // Not a valid legacy ID or valid UUID
            return null;
        }
    }

    @Override
    public Collection findByLegacyId(Context context, int id) throws SQLException {
        return collectionDAO.findByLegacyId(context, id, Collection.class);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return collectionDAO.countRows(context);
    }

    @Override
    public List<Map.Entry<Collection, Long>> getCollectionsWithBitstreamSizesTotal(Context context)
        throws SQLException {
        return collectionDAO.getCollectionsWithBitstreamSizesTotal(context);
    }

    @Override
    public Group createDefaultReadGroup(Context context, Collection collection, String typeOfGroupString,
                                        int defaultRead)
        throws SQLException, AuthorizeException {
        Group role = groupService.create(context);
        groupService.setName(role, getDefaultReadGroupName(collection, typeOfGroupString));

        // Remove existing privileges from the anonymous group.
        authorizeService.removePoliciesActionFilter(context, collection, defaultRead);

        // Grant our new role the default privileges.
        authorizeService.addPolicy(context, collection, defaultRead, role);
        groupService.update(context, role);
        return role;
    }

    @Override
    public String getDefaultReadGroupName(Collection collection, String typeOfGroupString) {
        return "COLLECTION_" + collection.getID().toString() + "_" + typeOfGroupString +
            "_DEFAULT_READ";
    }

    @Override
    public List<Collection> findCollectionsWithSubmit(String q, Context context, Community community,
                                                      String entityType, int offset,
                                                      int limit) throws SQLException, SearchServiceException {

        List<Collection> collections = new ArrayList<>();
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableCollection.TYPE);
        discoverQuery.setStart(offset);
        discoverQuery.setMaxResults(limit);
        discoverQuery.setSortField(SOLR_SORT_FIELD, SORT_ORDER.asc);
        discoverQuery.setSortField(SOLR_SORT_FIELD, SORT_ORDER.asc);
        DiscoverResult resp = retrieveCollectionsWithSubmit(context, discoverQuery, entityType, community, q);
        for (IndexableObject solrCollections : resp.getIndexableObjects()) {
            Collection c = ((IndexableCollection) solrCollections).getIndexedObject();
            collections.add(c);
        }
        return collections;
    }

    @Override
    public int countCollectionsWithSubmit(String q, Context context, Community community, String entityType)
        throws SQLException, SearchServiceException {

        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setMaxResults(0);
        discoverQuery.setDSpaceObjectFilter(IndexableCollection.TYPE);
        DiscoverResult resp = retrieveCollectionsWithSubmit(context, discoverQuery, entityType, community, q);
        return (int) resp.getTotalSearchResults();
    }

    @Override
    public boolean isSharedWorkspace(Context context, Collection collection) {
        return toBoolean(getMetadataFirstValue(collection, "cris", "workspace", "shared", Item.ANY));
    }

    @Override
    public String getEntityType(Collection collection) {
        return getMetadataFirstValue(collection, new MetadataFieldName("dspace.entity.type"), Item.ANY);
    }

    /**
     * Finds all Indexed Collections where the current user has submit rights. If the user is an Admin,
     * this is all Indexed Collections. Otherwise, it includes those collections where
     * an indexed "submit" policy lists either the eperson or one of the eperson's groups
     *
     * @param context                    DSpace context
     * @param discoverQuery
     * @param entityType                 limit the returned collection to those related to given entity type
     * @param community                  parent community, could be null
     * @param q                          limit the returned collection to those with metadata values matching the query
     *                                   terms. The terms are used to make also a prefix query on SOLR
     *                                   so it can be used to implement an autosuggest feature over the collection name
     * @return                           discovery search result objects
     * @throws SQLException              if something goes wrong
     * @throws SearchServiceException    if search error
     */
    private DiscoverResult retrieveCollectionsWithSubmit(Context context, DiscoverQuery discoverQuery,
        String entityType, Community community, String q)
        throws SQLException, SearchServiceException {

        StringBuilder query = new StringBuilder();
        EPerson currentUser = context.getCurrentUser();
        if (!authorizeService.isAdmin(context)) {
            String userId = "";
            if (currentUser != null) {
                userId = currentUser.getID().toString();
            }
            query.append("submit:(e").append(userId);

            Set<Group> groups = groupService.allMemberGroupsSet(context, currentUser);
            for (Group group : groups) {
                query.append(" OR g").append(group.getID());
            }
            query.append(")");
            discoverQuery.addFilterQueries(query.toString());
        }
        StringBuilder buildFilter = new StringBuilder();
        if (community != null) {
            buildFilter.append("location.comm:").append(community.getID().toString());
        }
        if (StringUtils.isNotBlank(entityType)) {
            if (buildFilter.length() > 0) {
                buildFilter.append(" AND ");
            }
            buildFilter.append("search.entitytype:").append(entityType);
        }

        if (Objects.nonNull(community)) {
            discoverQuery.addFilterQueries("location.comm:" + community.getID().toString());
        }
        if (StringUtils.isNotBlank(entityType)) {
            discoverQuery.addFilterQueries("search.entitytype:" + entityType);
        }
        if (StringUtils.isNotBlank(q)) {
            StringBuilder buildQuery = new StringBuilder();
            String escapedQuery = ClientUtils.escapeQueryChars(q);
            buildQuery.append("(").append(escapedQuery).append(" OR dc.title_sort:*")
                .append(escapedQuery).append("*").append(")");
            discoverQuery.setQuery(buildQuery.toString());
        }
        discoverQuery.addFilterQueries(buildFilter.toString());
        DiscoverResult resp = searchService.search(context, discoverQuery);
        return resp;
    }

    @Override
    public Collection retrieveCollectionWithSubmitByEntityType(Context context, Item item,
        String entityType) throws SQLException {
        Collection ownCollection = item.getOwningCollection();
        return retrieveWithSubmitCollectionByEntityType(context, ownCollection.getCommunities(), entityType);
    }

    private Collection retrieveWithSubmitCollectionByEntityType(Context context, List<Community> communities,
        String entityType) {

        for (Community community : communities) {
            Collection collection = retrieveCollectionWithSubmitByCommunityAndEntityType(context, community,
                entityType);
            if (collection != null) {
                return collection;
            }
        }

        for (Community community : communities) {
            List<Community> parentCommunities = community.getParentCommunities();
            Collection collection = retrieveWithSubmitCollectionByEntityType(context, parentCommunities, entityType);
            if (collection != null) {
                return collection;
            }
        }

        return retrieveCollectionWithSubmitByCommunityAndEntityType(context, null, entityType);
    }

    @Override
    public Collection retrieveCollectionWithSubmitByCommunityAndEntityType(Context context, Community community,
        String entityType) {
        context.turnOffAuthorisationSystem();
        List<Collection> collections;
        try {
            collections = findCollectionsWithSubmit(null, context, community, entityType, 0, 1);
        } catch (SQLException | SearchServiceException e) {
            throw new RuntimeException(e);
        }
        context.restoreAuthSystemState();
        if (collections != null && collections.size() > 0) {
            return collections.get(0);
        }
        if (community != null) {
            for (Community subCommunity : community.getSubcommunities()) {
                Collection collection = retrieveCollectionWithSubmitByCommunityAndEntityType(context,
                    subCommunity, entityType);
                if (collection != null) {
                    return collection;
                }
            }
        }
        return null;
    }

    @Override
    public Collection retrieveCollectionByEntityType(Context context, Item item, String entityType)
            throws SQLException {
        Collection ownCollection = item.getOwningCollection();
        return retrieveCollectionByEntityType(context, ownCollection.getCommunities(), entityType);
    }

    private Collection retrieveCollectionByEntityType(Context context, List<Community> communities, String entityType) {

        for (Community community : communities) {
            Collection collection = retriveCollectionByEntityType(context, community, entityType);
            if (collection != null) {
                return collection;
            }
        }

        for (Community community : communities) {
            List<Community> parentCommunities = community.getParentCommunities();
            Collection collection = retrieveCollectionByEntityType(context, parentCommunities, entityType);
            if (collection != null) {
                return collection;
            }
        }

        return retriveCollectionByEntityType(context, null, entityType);
    }

    @Override
    public Collection retriveCollectionByEntityType(Context context, Community community, String entityType) {
        context.turnOffAuthorisationSystem();
        List<Collection> collections;
        try {
            collections = findCollectionsWithSubmit(null, context, community, entityType, 0, 1);
        } catch (SQLException | SearchServiceException e) {
            throw new RuntimeException(e);
        }
        context.restoreAuthSystemState();
        if (collections != null && collections.size() > 0) {
            return collections.get(0);
        }
        if (community != null) {
            for (Community subCommunity : community.getSubcommunities()) {
                Collection collection = retriveCollectionByEntityType(context, subCommunity, entityType);
                if (collection != null) {
                    return collection;
                }
            }
        }
        return null;
    }

    @Override
    public Collection findByItem(Context context, Item item) throws SQLException {
        if (item.getOwningCollection() != null) {
            return item.getOwningCollection();
        }

        InProgressSubmission<Integer> inProgressSubmission = findInProgressSubmission(context, item);
        return inProgressSubmission != null ? inProgressSubmission.getCollection() : null;
    }

    private InProgressSubmission<Integer> findInProgressSubmission(Context context, Item item) throws SQLException {
        WorkspaceItem workspaceItem = workspaceItemService.findByItem(context, item);
        return workspaceItem != null ? workspaceItem : workflowItemService.findByItem(context, item);
    }

    @Override
    public List<Collection> findCollectionsAdministered(String query, Context context, int offset, int limit)
        throws SQLException, SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableCollection.TYPE);
        discoverQuery.setStart(offset);
        discoverQuery.setMaxResults(limit);

        return retrieveCollectionsAdministered(context, discoverQuery, query).getIndexableObjects().stream()
            .map(indexableObject -> ((IndexableCollection) indexableObject).getIndexedObject())
            .collect(Collectors.toList());
    }

    @Override
    public List<Collection> findCollectionsAdministeredByEntityType(String query, String entityType,
                                                                    Context context, int offset, int limit)
            throws SQLException, SearchServiceException {

        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableCollection.TYPE);
        discoverQuery.setStart(offset);
        discoverQuery.setMaxResults(limit);
        discoverQuery.setSortField(SOLR_SORT_FIELD, SORT_ORDER.asc);

        return retrieveCollectionsAdministeredByEntityType(context, discoverQuery,
                query, entityType).getIndexableObjects().stream()
                .map(indexableObject -> ((IndexableCollection) indexableObject).getIndexedObject())
                .collect(Collectors.toList());
    }

    @Override
    public int countCollectionsAdministered(String query, Context context) throws SQLException, SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setMaxResults(0);
        discoverQuery.setDSpaceObjectFilter(IndexableCollection.TYPE);
        return (int) retrieveCollectionsAdministered(context, discoverQuery, query).getTotalSearchResults();
    }

    private DiscoverResult retrieveCollectionsAdministered(Context context, DiscoverQuery discoverQuery, String query)
        throws SQLException, SearchServiceException {

        if (!authorizeService.isAdmin(context)) {

            String filterQuery = groupService.allMemberGroupsSet(context, context.getCurrentUser()).stream()
                .map(group -> "g" + group.getID())
                .collect(Collectors.joining(" OR ", "admin:(", ")"));

            discoverQuery.addFilterQueries(filterQuery);
        }

        if (StringUtils.isNotBlank(query)) {
            StringBuilder buildQuery = new StringBuilder();
            String escapedQuery = ClientUtils.escapeQueryChars(query);
            buildQuery.append(escapedQuery).append(" OR ").append(escapedQuery).append("*");
            discoverQuery.setQuery(buildQuery.toString());
        }

        return searchService.search(context, discoverQuery);
    }

    private DiscoverResult retrieveCollectionsAdministeredByEntityType(Context context,
                                                                       DiscoverQuery discoverQuery,
                                                                       String query, String entityType)
            throws SQLException, SearchServiceException {
        String filterQuery = "";
        if (!authorizeService.isAdmin(context)) {
            filterQuery = groupService.allMemberGroupsSet(context, context.getCurrentUser()).stream()
                    .map(group -> "g" + group.getID())
                    .collect(Collectors.joining(" OR ", "admin:(", ")"));
            discoverQuery.addFilterQueries(filterQuery);
        }
        if (StringUtils.isNoneBlank(entityType)) {
            if (filterQuery.length() > 0) {
                filterQuery += " AND ";
            }
            filterQuery += "search.entitytype: " + entityType;
            discoverQuery.addFilterQueries(filterQuery);
        }
        if (StringUtils.isNotBlank(query)) {
            StringBuilder buildQuery = new StringBuilder();
            String escapedQuery = ClientUtils.escapeQueryChars(query);
            buildQuery.append(escapedQuery).append(" OR ").append(escapedQuery).append("*");
            discoverQuery.setQuery(buildQuery.toString());
        }
        return searchService.search(context, discoverQuery);
    }
    @Override
    public int countCollectionsAdministeredByEntityType(String query, String entityType, Context context)
            throws SQLException, SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setMaxResults(0);
        discoverQuery.setDSpaceObjectFilter(IndexableCollection.TYPE);
        return (int) retrieveCollectionsAdministeredByEntityType(context,
                discoverQuery, query, entityType).getTotalSearchResults();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Collection> findAllCollectionsByEntityType(Context context, String entityType)
            throws SearchServiceException {
        List<Collection> collectionList = new ArrayList<>();

        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableCollection.TYPE);
        discoverQuery.addFilterQueries("dspace.entity.type:" + entityType);

        DiscoverResult discoverResult = searchService.search(context, discoverQuery);
        List<IndexableObject> solrIndexableObjects = discoverResult.getIndexableObjects();

        for (IndexableObject solrCollection : solrIndexableObjects) {
            Collection c = ((IndexableCollection) solrCollection).getIndexedObject();
            collectionList.add(c);
        }
        return collectionList;
    }

    /**
     * Returns total collection archived items
     *
     * @param context          DSpace Context
     * @param collection       Collection
     * @return                 total collection archived items
     */
    @Override
    public int countArchivedItems(Context context, Collection collection) {
        return itemCounter.getCount(context, collection);
    }

    @Override
    public boolean exists(Context context, UUID id) throws SQLException {
        return this.collectionDAO.exists(context, Collection.class, id);
    }
}
