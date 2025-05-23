/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.content.MetadataField;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.dao.Group2GroupCacheDAO;
import org.dspace.eperson.dao.GroupDAO;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.event.Event;
import org.dspace.util.UUIDUtils;
import org.dspace.xmlworkflow.Role;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the Group object.
 * This class is responsible for all business logic calls for the Group object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class GroupServiceImpl extends DSpaceObjectServiceImpl<Group> implements GroupService {
    private static final Logger log = LogManager.getLogger();

    @Autowired(required = true)
    protected GroupDAO groupDAO;

    @Autowired(required = true)
    protected Group2GroupCacheDAO group2GroupCacheDAO;

    @Autowired(required = true)
    protected CollectionService collectionService;

    @Autowired(required = true)
    protected CollectionRoleService collectionRoleService;

    @Autowired(required = true)
    protected EPersonService ePersonService;

    @Autowired(required = true)
    protected CommunityService communityService;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected ResourcePolicyService resourcePolicyService;

    @Autowired(required = true)
    protected PoolTaskService poolTaskService;
    @Autowired(required = true)
    protected ClaimedTaskService claimedTaskService;
    @Autowired(required = true)
    protected XmlWorkflowFactory workflowFactory;

    protected GroupServiceImpl() {
        super();
    }

    @Override
    public Group create(Context context) throws SQLException, AuthorizeException {
        // FIXME - authorization?
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to create an EPerson Group");
        }

        // Create a table row
        Group g = groupDAO.create(context, new Group());

        log.info(LogHelper.getHeader(context, "create_group", "group_id="
            + g.getID()));

        context.addEvent(new Event(Event.CREATE, Constants.GROUP, g.getID(), null, getIdentifiers(context, g)));
        update(context, g);

        return g;
    }

    @Override
    public void setName(Group group, String name) throws SQLException {
        if (group.isPermanent()) {
            log.error("Attempt to rename permanent Group {} to {}.",
                      group.getName(), name);
            throw new SQLException("Attempt to rename a permanent Group");
        } else {
            group.setName(name);
        }
    }

    @Override
    public void addMember(Context context, Group group, EPerson e) {
        if (isDirectMember(group, e)) {
            return;
        }
        group.addMember(e);
        e.getGroups().add(group);
        context.addEvent(
            new Event(Event.ADD, Constants.GROUP, group.getID(), Constants.EPERSON, e.getID(), e.getEmail(),
                      getIdentifiers(context, group)));
    }

    @Override
    public void addMember(Context context, Group groupParent, Group groupChild) throws SQLException {
        // don't add if it's already a member
        // and don't add itself
        if (groupParent.contains(groupChild) || groupParent.getID().equals(groupChild.getID())) {
            return;
        }

        groupParent.addMember(groupChild);
        groupChild.addParentGroup(groupParent);

        context.addEvent(new Event(Event.ADD, Constants.GROUP, groupParent.getID(), Constants.GROUP, groupChild.getID(),
                                   groupChild.getName(), getIdentifiers(context, groupParent)));
    }

    /**
     * Removes a member of a group.
     * The removal will be refused if the group is linked to a workflow step which has claimed tasks or pool tasks
     * and no other member is present in the group to handle these.
     * @param context DSpace context object
     * @param group   DSpace group
     * @param ePerson eperson
     * @throws SQLException
     */
    @Override
    public void removeMember(Context context, Group group, EPerson ePerson) throws SQLException {
        List<CollectionRole> collectionRoles = collectionRoleService.findByGroup(context, group);
        if (!collectionRoles.isEmpty()) {
            List<PoolTask> poolTasks = poolTaskService.findByGroup(context, group);
            List<ClaimedTask> claimedTasks = claimedTaskService.findByEperson(context, ePerson);
            for (ClaimedTask claimedTask : claimedTasks) {
                Step stepByName = workflowFactory.getStepByName(claimedTask.getStepID());
                Role role = stepByName.getRole();
                for (CollectionRole collectionRole : collectionRoles) {
                    if (StringUtils.equals(collectionRole.getRoleId(), role.getId())
                            && claimedTask.getWorkflowItem().getCollection().equals(collectionRole.getCollection())) {
                        // Count number of EPersons who are *direct* members of this group
                        int totalDirectEPersons = ePersonService.countByGroups(context, Set.of(group));
                        // Count number of Groups which have this groupParent as a direct parent
                        int totalChildGroups = countByParent(context, group);
                        // If this group has only one direct EPerson and *zero* child groups, then we cannot delete the
                        // EPerson or we will leave this group empty.
                        if (totalDirectEPersons == 1 && totalChildGroups == 0) {
                            throw new IllegalStateException(
                                    "Refused to remove user " + ePerson
                                            .getID() + " from workflow group because the group " + group
                                            .getID() + " has tasks assigned and no other members");
                        }

                    }
                }
            }
            if (!poolTasks.isEmpty()) {
                // Count number of EPersons who are *direct* members of this group
                int totalDirectEPersons = ePersonService.countByGroups(context, Set.of(group));
                // Count number of Groups which have this groupParent as a direct parent
                int totalChildGroups = countByParent(context, group);
                // If this group has only one direct EPerson and *zero* child groups, then we cannot delete the
                // EPerson or we will leave this group empty.
                if (totalDirectEPersons == 1 && totalChildGroups == 0) {
                    throw new IllegalStateException(
                            "Refused to remove user " + ePerson
                                    .getID() + " from workflow group because the group " + group
                                    .getID() + " has tasks assigned and no other members");
                }
            }
        }
        if (group.remove(ePerson)) {
            context.addEvent(new Event(Event.REMOVE, Constants.GROUP, group.getID(), Constants.EPERSON, ePerson.getID(),
                                       ePerson.getEmail(), getIdentifiers(context, group)));
        }
    }

    @Override
    public void removeMember(Context context, Group groupParent, Group childGroup) throws SQLException {
        List<CollectionRole> collectionRoles = collectionRoleService.findByGroup(context, groupParent);
        if (!collectionRoles.isEmpty()) {
            List<PoolTask> poolTasks = poolTaskService.findByGroup(context, groupParent);
            if (!poolTasks.isEmpty()) {
                // Count number of Groups which have this groupParent as a direct parent
                int totalChildGroups = countByParent(context, groupParent);
                // Count number of EPersons who are *direct* members of this group
                int totalDirectEPersons = ePersonService.countByGroups(context, Set.of(groupParent));
                // If this group has only one childGroup and *zero* direct EPersons, then we cannot delete the
                // childGroup or we will leave this group empty.
                if (totalChildGroups == 1 && totalDirectEPersons == 0) {
                    throw new IllegalStateException(
                            "Refused to remove sub group " + childGroup
                                    .getID() + " from workflow group because the group " + groupParent
                                    .getID() + " has tasks assigned and no other members");
                }
            }
        }
        if (groupParent.remove(childGroup)) {
            childGroup.removeParentGroup(groupParent);
            context.addEvent(
                new Event(Event.REMOVE, Constants.GROUP, groupParent.getID(), Constants.GROUP, childGroup.getID(),
                          childGroup.getName(), getIdentifiers(context, groupParent)));
        }
    }

    @Override
    public boolean isDirectMember(Group group, EPerson ePerson) {
        // special, group 0 is anonymous
        return StringUtils.equals(group.getName(), Group.ANONYMOUS) || group.contains(ePerson);
    }

    @Override
    public boolean isMember(Group owningGroup, Group childGroup) {
        return owningGroup.contains(childGroup);
    }

    @Override
    public boolean isParentOf(Context context, Group parentGroup, Group childGroup) throws SQLException {
        return group2GroupCacheDAO.findByParentAndChild(context, parentGroup, childGroup) != null;
    }

    @Override
    public boolean isMember(Context context, Group group) throws SQLException {
        return isMember(context, context.getCurrentUser(), group);
    }

    @Override
    public boolean isMember(Context context, EPerson ePerson, Group group)
        throws SQLException {
        if (group == null) {
            return false;

            // special, everyone is member of group 0 (anonymous)
        } else if (StringUtils.equals(group.getName(), Group.ANONYMOUS) ||
                   isParentOf(context, group, findByName(context, Group.ANONYMOUS))) {
            return true;

        } else {
            Boolean cachedGroupMembership = context.getCachedGroupMembership(group, ePerson);

            if (cachedGroupMembership != null) {
                return cachedGroupMembership;

            } else {
                boolean isMember = false;

                //If we have an ePerson, check we can find membership in the database
                if (ePerson != null) {
                    //lookup eperson in normal groups and subgroups with 1 query
                    isMember = isEPersonInGroup(context, group, ePerson);
                }

                //If we did not find the group membership in the database, check the special groups.
                //If there are special groups we need to check direct membership or check if the
                //special group is a subgroup of the provided group.
                //Note that special groups should only be checked if the current user == the ePerson.
                //This also works for anonymous users (ePerson == null) if IP authentication used
                if (!isMember && CollectionUtils.isNotEmpty(context.getSpecialGroups()) &&
                    isAuthenticatedUser(context, ePerson)) {

                    Iterator<Group> it = context.getSpecialGroups().iterator();

                    while (it.hasNext() && !isMember) {
                        Group specialGroup = it.next();
                        //Check if the special group matches the given group or if it is a subgroup (with 1 query)
                        if (specialGroup.equals(group) || isParentOf(context, group, specialGroup)) {
                            isMember = true;
                        }
                    }
                }

                context.cacheGroupMembership(group, ePerson, isMember);
                return isMember;

            }
        }
    }

    private boolean isAuthenticatedUser(final Context context, final EPerson ePerson) {
        return Objects.equals(context.getCurrentUser(), ePerson);
    }

    @Override
    public boolean isMember(final Context context, final String groupName) throws SQLException {
        return isMember(context, findByName(context, groupName));
    }

    @Override
    public boolean isMember(final Context context, EPerson eperson, final String groupName) throws SQLException {
        return isMember(context, eperson, findByName(context, groupName));
    }

    @Override
    public List<Group> allMemberGroups(Context context, EPerson ePerson) throws SQLException {
        return new ArrayList<>(allMemberGroupsSet(context, ePerson));
    }

    @Override
    public Set<Group> allMemberGroupsSet(Context context, EPerson ePerson) throws SQLException {
        Set<Group> cachedGroupMembership = context.getCachedAllMemberGroupsSet(ePerson);
        if (cachedGroupMembership != null) {
            return cachedGroupMembership;
        }

        Set<Group> groups = new HashSet<>();

        if (ePerson != null) {
            // two queries - first to get groups eperson is a member of
            // second query gets parent groups for groups eperson is a member of
            groups.addAll(groupDAO.findByEPerson(context, ePerson));
        }
        // Also need to get all "Special Groups" user is a member of!
        // Otherwise, you're ignoring the user's membership to these groups!
        // However, we only do this is we are looking up the special groups
        // of the current user, as we cannot look up the special groups
        // of a user who is not logged in.
        if ((context.getCurrentUser() == null) || (context.getCurrentUser().equals(ePerson))) {
            List<Group> specialGroups = context.getSpecialGroups();
            for (Group special : specialGroups) {
                groups.add(special);
            }
        }

        // all the users are members of the anonymous group
        groups.add(findByName(context, Group.ANONYMOUS));

        List<Group2GroupCache> groupCache = group2GroupCacheDAO.findByChildren(context, groups);
        // now we have all owning groups, also grab all parents of owning groups
        for (Group2GroupCache group2GroupCache : groupCache) {
            groups.add(group2GroupCache.getParent());
        }

        context.cacheAllMemberGroupsSet(ePerson, groups);
        return groups;
    }

    @Override
    public List<EPerson> allMembers(Context c, Group g) throws SQLException {
        // two queries - first to get all groups which are a member of this group
        // second query gets all members of each group in the first query

        // Get all groups which are a member of this group
        List<Group2GroupCache> group2GroupCaches = group2GroupCacheDAO.findByParent(c, g);
        // Initialize HashSet based on List size to avoid Set resizing. See https://stackoverflow.com/a/21822273
        Set<Group> groups = new HashSet<>((int) (group2GroupCaches.size() / 0.75 + 1));
        for (Group2GroupCache group2GroupCache : group2GroupCaches) {
            groups.add(group2GroupCache.getChild());
        }


        Set<EPerson> childGroupChildren = new HashSet<>(ePersonService.findByGroups(c, groups));
        //Don't forget to add our direct children
        childGroupChildren.addAll(g.getMembers());

        return new ArrayList<>(childGroupChildren);
    }

    @Override
    public int countAllMembers(Context context, Group group) throws SQLException {
        // Get all groups which are a member of this group
        List<Group2GroupCache> group2GroupCaches = group2GroupCacheDAO.findByParent(context, group);
        // Initialize HashSet based on List size + current 'group' to avoid Set resizing.
        // See https://stackoverflow.com/a/21822273
        Set<Group> groups = new HashSet<>((int) ((group2GroupCaches.size() + 1) / 0.75 + 1));
        for (Group2GroupCache group2GroupCache : group2GroupCaches) {
            groups.add(group2GroupCache.getChild());
        }
        // Append current group as well
        groups.add(group);

        // Return total number of unique EPerson objects in any of these groups
        return ePersonService.countByGroups(context, groups);
    }

    @Override
    public Group find(Context context, UUID id) throws SQLException {
        if (id == null) {
            return null;
        } else {
            return groupDAO.findByID(context, Group.class, id);
        }
    }

    @Override
    public Group findByName(Context context, String name) throws SQLException {
        if (name == null) {
            return null;
        }

        return groupDAO.findByName(context, name);
    }

    @Override
    public Group findByNamePrefix(Context context, String namePrefix) throws SQLException {
        if (namePrefix == null) {
            return null;
        }

        return groupDAO.findByNamePrefix(context, namePrefix);
    }

    /**
     * DEPRECATED: Please use {@code findAll(Context context, List<MetadataField> metadataSortFields)} instead
     */
    @Override
    @Deprecated
    public List<Group> findAll(Context context, int sortField) throws SQLException {
        if (sortField == GroupService.NAME) {
            return findAll(context, null);
        } else {
            throw new UnsupportedOperationException("You can only find all groups sorted by name with this method");
        }
    }

    @Override
    public List<Group> findAll(Context context, List<MetadataField> metadataSortFields) throws SQLException {
        return findAll(context, metadataSortFields, -1, -1);
    }

    @Override
    public List<Group> findAll(Context context, List<MetadataField> metadataSortFields, int pageSize, int offset)
        throws SQLException {
        if (CollectionUtils.isEmpty(metadataSortFields)) {
            return groupDAO.findAll(context, pageSize, offset);
        } else {
            return groupDAO.findAll(context, metadataSortFields, pageSize, offset);
        }
    }

    @Override
    public List<Group> search(Context context, String query) throws SQLException {
        return search(context, query, -1, -1);
    }

    @Override
    public List<Group> search(Context context, String query, int offset, int limit) throws SQLException {
        List<Group> groups = new ArrayList<>();
        UUID uuid = UUIDUtils.fromString(query);
        if (uuid == null) {
            //Search by group name
            groups = groupDAO.findByNameLike(context, query, offset, limit);
        } else {
            //Search by group id
            Group group = find(context, uuid);
            if (group != null) {
                groups.add(group);
            }
        }

        return groups;
    }

    @Override
    public int searchResultCount(Context context, String query) throws SQLException {
        int result = 0;
        UUID uuid = UUIDUtils.fromString(query);
        if (uuid == null) {
            //Search by group name
            result = groupDAO.countByNameLike(context, query);
        } else {
            //Search by group id
            Group group = find(context, uuid);
            if (group != null) {
                result = 1;
            }
        }

        return result;
    }

    @Override
    public List<Group> searchNonMembers(Context context, String query, Group excludeParentGroup,
                                        int offset, int limit) throws SQLException {
        List<Group> groups = new ArrayList<>();
        UUID uuid = UUIDUtils.fromString(query);
        if (uuid == null) {
            // Search by group name
            groups = groupDAO.findByNameLikeAndNotMember(context, query, excludeParentGroup, offset, limit);
        } else if (!uuid.equals(excludeParentGroup.getID())) {
            // Search by group id
            Group group = find(context, uuid);
            // Verify it is NOT a member of the given excludeParentGroup before adding
            if (group != null && !isMember(excludeParentGroup, group)) {
                groups.add(group);
            }
        }

        return groups;
    }

    @Override
    public int searchNonMembersCount(Context context, String query, Group excludeParentGroup) throws SQLException {
        int result = 0;
        UUID uuid = UUIDUtils.fromString(query);
        if (uuid == null) {
            // Search by group name
            result = groupDAO.countByNameLikeAndNotMember(context, query, excludeParentGroup);
        } else if (!uuid.equals(excludeParentGroup.getID())) {
            // Search by group id
            Group group = find(context, uuid);
            // Verify it is NOT a member of the given excludeParentGroup before adding
            if (group != null && !isMember(excludeParentGroup, group)) {
                result = 1;
            }
        }
        return result;
    }

    @Override
    public void delete(Context context, Group group) throws SQLException {
        if (group.isPermanent()) {
            log.error("Attempt to delete permanent Group {}", group::getName);
            throw new SQLException("Attempt to delete a permanent Group");
        }

        context.addEvent(new Event(Event.DELETE, Constants.GROUP, group.getID(),
                                   group.getName(), getIdentifiers(context, group)));

        // Remove any ResourcePolicies that reference this group
        authorizeService.removeGroupPolicies(context, group);

        group.getMemberGroups().clear();
        group.getParentGroups().clear();

        //Remove all eperson references from this group
        Iterator<EPerson> ePeople = group.getMembers().iterator();
        while (ePeople.hasNext()) {
            EPerson ePerson = ePeople.next();
            ePeople.remove();
            ePerson.getGroups().remove(group);
        }

        // empty out group2groupcache table (if we do it after we delete our object we get an issue with references)
        group2GroupCacheDAO.deleteAll(context);
        // Remove ourself
        groupDAO.delete(context, group);
        rethinkGroupCache(context, false);

        log.info(LogHelper.getHeader(context, "delete_group", "group_id="
            + group.getID()));
    }

    @Override
    public int getSupportsTypeConstant() {
        return Constants.GROUP;
    }

    /**
     * Return true if group has no direct or indirect members
     */
    @Override
    public boolean isEmpty(Group group) {
        // the only fast check available is on epeople...
        boolean hasMembers = (!group.getMembers().isEmpty());

        if (hasMembers) {
            return false;
        } else {
            // well, groups is never null...
            for (Group subGroup : group.getMemberGroups()) {
                hasMembers = !isEmpty(subGroup);
                if (hasMembers) {
                    return false;
                }
            }
            return !hasMembers;
        }
    }

    @Override
    public void initDefaultGroupNames(Context context) throws SQLException, AuthorizeException {
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        // Check for Anonymous group. If not found, create it
        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);
        if (anonymousGroup == null) {
            anonymousGroup = groupService.create(context);
            anonymousGroup.setName(Group.ANONYMOUS);
            anonymousGroup.setPermanent(true);
            groupService.update(context, anonymousGroup);
        }


        // Check for Administrator group. If not found, create it
        Group adminGroup = groupService.findByName(context, Group.ADMIN);
        if (adminGroup == null) {
            adminGroup = groupService.create(context);
            adminGroup.setName(Group.ADMIN);
            adminGroup.setPermanent(true);
            groupService.update(context, adminGroup);
        }
    }

    /**
     * Get a list of groups with no members.
     *
     * @param context The relevant DSpace Context.
     * @return list of groups with no members
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    @Override
    public List<Group> getEmptyGroups(Context context) throws SQLException {
        return groupDAO.getEmptyGroups(context);
    }

    /**
     * Update the group - writing out group object and EPerson list if necessary
     *
     * @param context The relevant DSpace Context.
     * @param group   Group to update
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     */
    @Override
    public void update(Context context, Group group) throws SQLException, AuthorizeException {

        super.update(context, group);
        // FIXME: Check authorisation
        groupDAO.save(context, group);

        if (group.isMetadataModified()) {
            context.addEvent(new Event(Event.MODIFY_METADATA, Constants.GROUP, group.getID(), group.getDetails(),
                                       getIdentifiers(context, group)));
            group.clearDetails();
        }

        if (group.isGroupsChanged()) {
            rethinkGroupCache(context, true);
            group.clearGroupsChanged();
        }

        log.info(LogHelper.getHeader(context, "update_group", "group_id="
            + group.getID()));
    }


    protected boolean isEPersonInGroup(Context context, Group group, EPerson ePerson)
        throws SQLException {
        return groupDAO.findByIdAndMembership(context, group.getID(), ePerson) != null;
    }


    /**
     * Returns a set with pairs of parent and child group UUIDs, representing the new cache table rows.
     *
     * @param context       The relevant DSpace Context.
     * @param flushQueries  flushQueries Flush all pending queries
     * @return              Pairs of parent and child group UUID of the new cache.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    private Set<Pair<UUID, UUID>> computeNewCache(Context context, boolean flushQueries) throws SQLException {
        Map<UUID, Set<UUID>> parents = new HashMap<>();

        List<Pair<UUID, UUID>> group2groupResults = groupDAO.getGroup2GroupResults(context, flushQueries);
        for (Pair<UUID, UUID> group2groupResult : group2groupResults) {
            UUID parent = group2groupResult.getLeft();
            UUID child = group2groupResult.getRight();

            parents.putIfAbsent(parent, new HashSet<>());
            parents.get(parent).add(child);
        }

        // now parents is a hash of all of the IDs of groups that are parents
        // and each hash entry is a hash of all of the IDs of children of those
        // parent groups
        // so now to establish all parent,child relationships we can iterate
        // through the parents hash
        for (Map.Entry<UUID, Set<UUID>> parent : parents.entrySet()) {
            Set<UUID> myChildren = getChildren(parents, parent.getKey());
            parent.getValue().addAll(myChildren);
        }

        // write out new cache IN MEMORY ONLY and returns it
        Set<Pair<UUID, UUID>> newCache = new HashSet<>();
        for (Map.Entry<UUID, Set<UUID>> parent : parents.entrySet()) {
            UUID key = parent.getKey();
            for (UUID child : parent.getValue()) {
                newCache.add(Pair.of(key, child));
            }
        }
        return newCache;
    }


    /**
     * Regenerate the group cache AKA the group2groupcache table in the database -
     * meant to be called when a group is added or removed from another group
     *
     * @param context      The relevant DSpace Context.
     * @param flushQueries flushQueries Flush all pending queries
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    protected void rethinkGroupCache(Context context, boolean flushQueries) throws SQLException {
        // current cache in the database
        Set<Pair<UUID, UUID>> oldCache = group2GroupCacheDAO.getCache(context);

        // correct cache, computed from the Group table
        Set<Pair<UUID, UUID>> newCache = computeNewCache(context, flushQueries);

        SetUtils.SetView<Pair<UUID, UUID>> toDelete = SetUtils.difference(oldCache, newCache);
        SetUtils.SetView<Pair<UUID, UUID>> toCreate = SetUtils.difference(newCache, oldCache);

        for (Pair<UUID, UUID> pair : toDelete ) {
            group2GroupCacheDAO.deleteFromCache(context, pair.getLeft(), pair.getRight());
        }

        for (Pair<UUID, UUID> pair : toCreate ) {
            group2GroupCacheDAO.addToCache(context, pair.getLeft(), pair.getRight());
        }
    }

    @Override
    public DSpaceObject getParentObject(Context context, Group group) throws SQLException {
        if (group == null) {
            return null;
        }
        // could a collection/community administrator manage related groups?
        // check before the configuration options could give a performance gain
        // if all group management are disallowed
        if (AuthorizeConfiguration.canCollectionAdminManageAdminGroup()
            || AuthorizeConfiguration.canCollectionAdminManageSubmitters()
            || AuthorizeConfiguration.canCollectionAdminManageWorkflows()
            || AuthorizeConfiguration.canCommunityAdminManageAdminGroup()
            || AuthorizeConfiguration
            .canCommunityAdminManageCollectionAdminGroup()
            || AuthorizeConfiguration
            .canCommunityAdminManageCollectionSubmitters()
            || AuthorizeConfiguration
            .canCommunityAdminManageCollectionWorkflows()) {
            // is this a collection related group?
            org.dspace.content.Collection collection = collectionService.findByGroup(context, group);

            if (collection != null) {
                if (group.equals(collection.getSubmitters())) {
                    if (AuthorizeConfiguration.canCollectionAdminManageSubmitters()) {
                        return collection;
                    } else if (AuthorizeConfiguration.canCommunityAdminManageCollectionSubmitters()) {
                        return collectionService.getParentObject(context, collection);
                    }
                }
                if (group.equals(collection.getAdministrators())) {
                    if (AuthorizeConfiguration.canCollectionAdminManageAdminGroup()) {
                        return collection;
                    } else if (AuthorizeConfiguration.canCommunityAdminManageCollectionAdminGroup()) {
                        return collectionService.getParentObject(context, collection);
                    }
                }
            } else {
                if (AuthorizeConfiguration.canCollectionAdminManageWorkflows()
                        || AuthorizeConfiguration.canCommunityAdminManageCollectionWorkflows()) {
                    // if the group is used for one or more roles on a single collection,
                    // admins can eventually manage it
                    List<CollectionRole> collectionRoles = collectionRoleService.findByGroup(context, group);
                    if (collectionRoles != null && !collectionRoles.isEmpty()) {
                        Set<Collection> colls = new HashSet<>();
                        for (CollectionRole cr : collectionRoles) {
                            colls.add(cr.getCollection());
                        }
                        if (colls.size() == 1) {
                            collection = colls.iterator().next();
                            if (AuthorizeConfiguration.canCollectionAdminManageWorkflows()) {
                                return collection;
                            } else {
                                return collectionService.getParentObject(context, collection);
                            }
                        }
                    } else {
                        if (AuthorizeConfiguration.canCollectionAdminManagePolicies()
                            || AuthorizeConfiguration.canCommunityAdminManagePolicies()
                            || AuthorizeConfiguration.canCommunityAdminManageCollectionWorkflows()) {
                            List<Group> groups = new ArrayList<>();
                            groups.add(group);
                            List<ResourcePolicy> policies = resourcePolicyService.find(context, null, groups,
                                                            Constants.DEFAULT_ITEM_READ, Constants.COLLECTION);

                            Optional<ResourcePolicy> defaultPolicy = policies.stream().filter(p -> StringUtils.equals(
                                    collectionService.getDefaultReadGroupName((Collection) p.getdSpaceObject(), "ITEM"),
                                    group.getName())).findFirst();

                            if (defaultPolicy.isPresent()) {
                                return defaultPolicy.get().getdSpaceObject();
                            }
                            policies = resourcePolicyService.find(context, null, groups,
                                                             Constants.DEFAULT_BITSTREAM_READ, Constants.COLLECTION);

                            defaultPolicy = policies.stream()
                                    .filter(p -> StringUtils.equals(collectionService.getDefaultReadGroupName(
                                            (Collection) p.getdSpaceObject(), "BITSTREAM"), group.getName()))
                                    .findFirst();

                            if (defaultPolicy.isPresent()) {
                                return defaultPolicy.get().getdSpaceObject();
                            }
                        }
                    }
                }
                if (AuthorizeConfiguration.canCommunityAdminManageAdminGroup()) {
                    // is the group related to a community and community administrator allowed
                    // to manage it?
                    return communityService.findByAdminGroup(context, group);
                }
            }
        }
        return null;
    }

    @Override
    public void updateLastModified(Context context, Group dso) {
        //Not needed.
    }

    /**
     * Used recursively to generate a map of ALL of the children of the given
     * parent
     *
     * @param parents Map of parent,child relationships
     * @param parent  the parent you're interested in
     * @return Map whose keys are all of the children of a parent
     */
    protected Set<UUID> getChildren(Map<UUID, Set<UUID>> parents, UUID parent) {
        Set<UUID> myChildren = new HashSet<>();

        // degenerate case, this parent has no children
        if (!parents.containsKey(parent)) {
            return myChildren;
        }

        // got this far, so we must have children
        Set<UUID> children = parents.get(parent);

        // now iterate over all of the children

        for (UUID child : children) {
            // add this child's ID to our return set
            myChildren.add(child);

            // and now its children
            myChildren.addAll(getChildren(parents, child));
        }

        return myChildren;
    }

    @Override
    public Group findByIdOrLegacyId(Context context, String id) throws SQLException {
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
    public Group findByLegacyId(Context context, int id) throws SQLException {
        return groupDAO.findByLegacyId(context, id, Group.class);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return groupDAO.countRows(context);
    }

    @Override
    public List<Group> findByMetadataField(final Context context, final String searchValue,
                                           final MetadataField metadataField) throws SQLException {
        return groupDAO.findByMetadataField(context, searchValue, metadataField);
    }

    @Override
    public String getName(Group dso) {
        return dso.getName();
    }

    public boolean exists(Context context, UUID id) throws SQLException {
        return this.groupDAO.exists(context, Group.class, id);
    }

    @Override
    public List<Group> findByParent(Context context, Group parent, int pageSize, int offset) throws SQLException {
        if (parent == null) {
            return null;
        }
        return groupDAO.findByParent(context, parent, pageSize, offset);
    }

    @Override
    public int countByParent(Context context, Group parent) throws SQLException {
        if (parent == null) {
            return 0;
        }
        return groupDAO.countByParent(context, parent);
    }
}
