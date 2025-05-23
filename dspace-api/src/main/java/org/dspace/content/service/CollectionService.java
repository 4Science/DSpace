/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.Group;

/**
 * Service interface class for the Collection object.
 * The implementation of this class is responsible for all business logic calls
 * for the Collection object and is autowired by Spring.
 *
 * @author kevinvandevelde at atmire.com
 */
public interface CollectionService
        extends DSpaceObjectService<Collection>, DSpaceObjectLegacySupportService<Collection> {

    /*
     * Field used to sort community and collection lists at solr
     */
    public static final String SOLR_SORT_FIELD = "dc.title_sort";

    /**
     * Create a new collection with a new ID.
     * Once created the collection is added to the given community
     *
     * @param context   DSpace context object
     * @param community DSpace Community (parent)
     * @return the newly created collection
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public Collection create(Context context, Community community) throws SQLException,
        AuthorizeException;

    /**
     * Create a new collection with the supplied handle and with a new ID.
     * Once created the collection is added to the given community
     *
     * @param context   DSpace context object
     * @param community DSpace Community (parent)
     * @param handle    the pre-determined Handle to assign to the new community
     * @return the newly created collection
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public Collection create(Context context, Community community, String handle) throws SQLException,
        AuthorizeException;

    /**
     * Create a new collection with the supplied handle and ID.
     * Once created the collection is added to the given community
     *
     * @param context DSpace context object
     * @param community DSpace Community (parent)
     * @param handle the pre-determined Handle to assign to the new collection
     * @param uuid the pre-determined UUID to assign to the new collection
     * @return the newly created collection
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public Collection create(Context context, Community community, String handle, UUID uuid) throws SQLException,
            AuthorizeException;

    /**
     * Get all collections in the system. These are alphabetically sorted by
     * collection name.
     *
     * @param context DSpace context object
     * @return the collections in the system
     * @throws SQLException if database error
     */
    public List<Collection> findAll(Context context) throws SQLException;

    /**
     * Get all collections in the system. Adds support for limit and offset.
     *
     * @param context The relevant DSpace Context.
     * @param limit   paging limit
     * @param offset  paging offset
     * @return List of Collections
     * @throws SQLException if database error
     */
    public List<Collection> findAll(Context context, Integer limit, Integer offset) throws SQLException;

    public List<Collection> findAuthorizedOptimized(Context context, int actionID) throws java.sql.SQLException;

    public List<Collection> findDirectMapped(Context context, int actionID) throws java.sql.SQLException;

    public List<Collection> findGroup2CommunityMapped(Context context) throws SQLException;

    public List<Collection> findGroup2GroupMapped(Context context, int actionID) throws SQLException;

    public List<Collection> findGroupMapped(Context context, int actionID) throws java.sql.SQLException;

    /**
     * Give the collection a logo. Passing in <code>null</code> removes any
     * existing logo. You will need to set the format of the new logo bitstream
     * before it will work, for example to "JPEG". Note that
     * <code>update</code> will need to be called for the change to take
     * effect.  Setting a logo and not calling <code>update</code> later may
     * result in a previous logo lying around as an "orphaned" bitstream.
     *
     * @param context    DSpace Context
     * @param collection Collection
     * @param is         the stream to use as the new logo
     * @return the new logo bitstream, or <code>null</code> if there is no
     * logo (<code>null</code> was passed in)
     * @throws AuthorizeException if authorization error
     * @throws IOException        if IO error
     * @throws SQLException       if database error
     */
    public Bitstream setLogo(Context context, Collection collection, InputStream is) throws AuthorizeException,
        IOException, SQLException;

    /**
     * Create a workflow group for the given step if one does not already exist.
     * Returns either the newly created group or the previously existing one.
     * Note that while the new group is created in the database, the association
     * between the group and the collection is not written until
     * <code>update</code> is called.
     *
     * @param context    DSpace Context
     * @param collection Collection
     * @param step       the step (1-3) of the workflow to create or get the group for
     * @return the workflow group associated with this collection
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public Group createWorkflowGroup(Context context, Collection collection, int step) throws SQLException,
        AuthorizeException;

    /**
     * Set the workflow group corresponding to a particular workflow step.
     * <code>null</code> can be passed in if there should be no associated
     * group for that workflow step; any existing group is NOT deleted.
     *
     * @param context    current DSpace session.
     * @param collection Collection
     * @param step       the workflow step (1-3)
     * @param group      the new workflow group, or <code>null</code>
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     */
    public void setWorkflowGroup(Context context, Collection collection, int step, Group group)
        throws SQLException, AuthorizeException;

    /**
     * Get the the workflow group corresponding to a particular workflow step.
     * This returns <code>null</code> if there is no group associated with
     * this collection for the given step.
     *
     * @param context    DSpace Context
     * @param collection Collection
     * @param step       the workflow step (1-3)
     * @return the group of reviewers or <code>null</code>
     */
    public Group getWorkflowGroup(Context context, Collection collection, int step);

    /**
     * Create a default submitters group if one does not already exist. Returns
     * either the newly created group or the previously existing one. Note that
     * other groups may also be allowed to submit to this collection by the
     * authorization system.
     *
     * @param context    DSpace Context
     * @param collection Collection
     * @return the default group of submitters associated with this collection
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public Group createSubmitters(Context context, Collection collection) throws SQLException, AuthorizeException;

    /**
     * Remove the submitters group, if no group has already been created
     * then return without error. This will merely dereference the current
     * submitters group from the collection so that it may be deleted
     * without violating database constraints.
     *
     * @param context    DSpace Context
     * @param collection Collection
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void removeSubmitters(Context context, Collection collection) throws SQLException, AuthorizeException;


    /**
     * Create a default administrators group if one does not already exist.
     * Returns either the newly created group or the previously existing one.
     * Note that other groups may also be administrators.
     *
     * @param context    DSpace Context
     * @param collection Collection
     * @return the default group of editors associated with this collection
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public Group createAdministrators(Context context, Collection collection) throws SQLException, AuthorizeException;

    /**
     * Remove the administrators group, if no group has already been created
     * then return without error. This will merely dereference the current
     * administrators group from the collection so that it may be deleted
     * without violating database constraints.
     *
     * @param context    DSpace Context
     * @param collection Collection
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void removeAdministrators(Context context, Collection collection) throws SQLException, AuthorizeException;

    /**
     * Get the license that users must grant before submitting to this
     * collection. If the collection does not have a specific license, the
     * site-wide default is returned.
     *
     * @param collection Collection
     * @return the license for this collection
     */
    public String getLicense(Collection collection);

    /**
     * Find out if the collection has a custom license
     *
     * @param collection Collection
     * @return <code>true</code> if the collection has a custom license
     */
    public boolean hasCustomLicense(Collection collection);

    /**
     * Create an empty template item for this collection. If one already exists,
     * no action is taken. Caution: Make sure you call <code>update</code> on
     * the collection after doing this, or the item will have been created but
     * the collection record will not refer to it.
     *
     * @param context    DSpace Context
     * @param collection Collection
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void createTemplateItem(Context context, Collection collection) throws SQLException, AuthorizeException;

    /**
     * Remove the template item for this collection, if there is one. Note that
     * since this has to remove the old template item ID from the collection
     * record in the database, the collection record will be changed, including
     * any other changes made; in other words, this method does an
     * <code>update</code>.
     *
     * @param context    DSpace Context
     * @param collection Collection
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException        if IO error
     */
    public void removeTemplateItem(Context context, Collection collection)
        throws SQLException, AuthorizeException, IOException;

    /**
     * Add an item to the collection. This simply adds a relationship between
     * the item and the collection - it does nothing like set an issue date,
     * remove a personal workspace item etc. This has instant effect;
     * <code>update</code> need not be called.
     *
     * @param context    DSpace Context
     * @param collection Collection
     * @param item       item to add
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void addItem(Context context, Collection collection, Item item) throws SQLException, AuthorizeException;

    /**
     * Remove an item. If the item is then orphaned, it is deleted.
     *
     * @param context    DSpace Context
     * @param collection Collection
     * @param item       item to remove
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException        if IO error
     */
    public void removeItem(Context context, Collection collection, Item item) throws SQLException, AuthorizeException,
        IOException;

    public boolean canEditBoolean(Context context, Collection collection) throws SQLException;

    public boolean canEditBoolean(Context context, Collection collection, boolean useInheritance)
        throws java.sql.SQLException;

    public void canEdit(Context context, Collection collection) throws SQLException, AuthorizeException;

    public void canEdit(Context context, Collection collection, boolean useInheritance)
        throws SQLException, AuthorizeException;

    /**
     * return an array of collections that user has a given permission on
     * (useful for trimming 'select to collection' list) or figuring out which
     * collections a person is an editor for.
     *
     * @param context   DSpace Context
     * @param community (optional) restrict search to a community, else null
     * @param actionID  of the action
     * @return Collection [] of collections with matching permissions
     * @throws SQLException if database error
     */
    public List<Collection> findAuthorized(Context context, Community community, int actionID)
        throws java.sql.SQLException;

    /**
     *
     * @param context DSpace Context
     * @param group EPerson Group
     * @return the collection, if any, that has the specified group as administrators or submitters
     * @throws SQLException
     */
    public Collection findByGroup(Context context, Group group) throws SQLException;

    List<Collection> findCollectionsWithSubscribers(Context context) throws SQLException;

    int countTotal(Context context) throws SQLException;

    /**
     * The map entry returned contains a collection as a key and sum of bitstream sizes in bytes as a value
     *
     * @param context DSpace Context
     * @return List of Collections and bitstream sizes map
     * @throws SQLException if database error
     */
    List<Map.Entry<Collection, Long>> getCollectionsWithBitstreamSizesTotal(Context context) throws SQLException;

    /**
     * This method will create a default read group for the given Collection. It'll create either a defaultItemRead or
     * a defaultBitstreamRead group depending on the given parameters
     *
     * @param context           The relevant DSpace context
     * @param collection        The collection for which it'll be created
     * @param typeOfGroupString The type of group to be made, item or bitstream
     * @param defaultRead       The defaultRead int, item or bitstream
     * @return                  The created Group
     * @throws SQLException     If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    Group createDefaultReadGroup(Context context, Collection collection, String typeOfGroupString, int defaultRead)
        throws SQLException, AuthorizeException;

    /**
     * This method will return the name to give to the group created by the
     * {@link #createDefaultReadGroup(Context, Collection, String, int)} method
     *
     * @param collection        The DSpace collection to use in the name generation
     * @param typeOfGroupString The type of group to use in the name generation
     * @return the name to give to the group that hold default read for the collection
     */
    String getDefaultReadGroupName(Collection collection, String typeOfGroupString);

    /**
     * Returns Collections for which the current user has 'submit' privileges.
     * NOTE: for better performance, this method retrieves its results from an
     *       index (cache) and does not query the database directly.
     *       This means that results may be stale or outdated until https://github.com/DSpace/DSpace/issues/2853 is resolved"
     * 
     * @param q                limit the returned collection to those with metadata values matching the query terms.
     *                         The terms are used to make also a prefix query on SOLR so it can be used to implement
     *                         an autosuggest feature over the collection name
     * @param context          DSpace Context
     * @param community        parent community
     * @param entityType       limit the returned collection to those related to given entity type
     * @param offset           the position of the first result to return
     * @param limit            paging limit
     * @return                 discovery search result objects
     * @throws SQLException              if something goes wrong
     * @throws SearchServiceException    if search error
     */
    public List<Collection> findCollectionsWithSubmit(String q, Context context, Community community,
            String entityType, int offset, int limit) throws SQLException, SearchServiceException;

    /**
     * Returns true if the given collection is configured so that all items are
     * shared among all submitters of the collection itself.
     *
     * @param  context    the DSpace context
     * @param  collection the collection to test
     * @return            true if the given collection's workspace is shared, false
     *                    otherwise
     */
    boolean isSharedWorkspace(Context context, Collection collection);


    /**
     * Retrieve the first collection in the community or its descending that support
     * the provided entityType
     *
     * @param  context    the DSpace context
     * @param  community  the root from where the search start
     * @param  entityType the requested entity type
     * @return            the first collection in the community or its descending
     *                    that support the provided entityType
     */
    public Collection retrieveCollectionWithSubmitByCommunityAndEntityType(Context context, Community community,
        String entityType);

    /**
     * Retrieve the close collection to the item for which the current user has
     * 'submit' privileges that support the provided entityType. Close mean the
     * collection that can be reach with the minimum steps starting from the item
     * (owningCollection, brothers collections, etc)
     *
     * @param  context    the DSpace context
     * @param  item       the item from where the search start
     * @param  entityType the requested entity type
     * @return            the first collection in the community or its descending
     *                    that support the provided entityType
     */
    public Collection retrieveCollectionWithSubmitByEntityType(Context context, Item item, String entityType)
        throws SQLException;

    /**
     * Counts the number of Collection for which the current user has 'submit' privileges.
     * NOTE: for better performance, this method retrieves its results from an index (cache)
     *       and does not query the database directly.
     *       This means that results may be stale or outdated until
     *       https://github.com/DSpace/DSpace/issues/2853 is resolved."
     * 
     * @param context          DSpace Context
     * @param community        parent community
     * @return                 total collections found
     * @throws SQLException              if something goes wrong
     * @throws SearchServiceException    if search error
     */
    public Collection retriveCollectionByEntityType(Context context, Community community, String entityType);

    /**
     * Retrieve the close collection to the item that support the provided
     * entityType. Close mean the collection that can be reach with the minimum
     * steps starting from the item (owningCollection, brothers collections, etc)
     *
     * @param  context    the DSpace context
     * @param  item       the item from where the search start
     * @param  entityType the requested entity type
     * @return            the first collection in the community or its descending
     *                    that support the provided entityType
     */
    public Collection retrieveCollectionByEntityType(Context context, Item item, String entityType)
            throws SQLException;

    /**
     * Returns the collections that are administered by the current user.
     *
     * @param  query                  limit the returned collection to those with
     *                                metadata values matching the query terms. The
     *                                terms are used to make also a prefix query on
     *                                SOLR so it can be used to implement an
     *                                autosuggest feature over the collection name
     * @param  context                DSpace Context
     * @param  offset                 the position of the first result to return
     * @param  limit                  paging limit
     * @return                        discovery search result objects
     * @throws SQLException           if something goes wrong
     * @throws SearchServiceException if search error
     */
    List<Collection> findCollectionsAdministered(String query, Context context, int offset, int limit)
        throws SQLException, SearchServiceException;
    /**
     * Returns the collections that are administered by the current user.
     *
     * @param  query                  limit the returned collection to those with
     *                                metadata values matching the query terms. The
     *                                terms are used to make also a prefix query on
     *                                SOLR so it can be used to implement an
     *                                autosuggest feature over the collection name
     * @param  entityType             entityType of the collection
     * @param  context                DSpace Context
     * @param  offset                 the position of the first result to return
     * @param  limit                  paging limit
     * @return                        discovery search result objects
     * @throws SQLException           if something goes wrong
     * @throws SearchServiceException if search error
     */
    List<Collection> findCollectionsAdministeredByEntityType(String query,String entityType,
                                                             Context context, int offset, int limit)
            throws SQLException, SearchServiceException;
    /**
     * Counts the collections that are administered by the current user.
     *
     * @param  query                  limit the returned collection to those with
     *                                metadata values matching the query terms. The
     *                                terms are used to make also a prefix query on
     *                                SOLR so it can be used to implement an
     *                                autosuggest feature over the collection name
     * @param  context                DSpace Context
     * @return                        discovery search result objects
     * @throws SQLException           if something goes wrong
     * @throws SearchServiceException if search error
     */
    int countCollectionsAdministered(String query, Context context) throws SQLException, SearchServiceException;

    /**
     * Returns the collection related to the given item. If the item is archived,
     * this method returns the own collection of that item, otherwise returns the
     * collection related to the current InProgressSubmission related to that item.
     *
     * @param  context      the DSpace context
     * @param  item         the item from where the search start
     * @return              the collection related to the given item
     * @throws SQLException if an SQL error occurs
     */
    public Collection findByItem(Context context, Item item) throws SQLException;
    /**
     * Returns thenu number of collections administered by user of an entity type
     *
     * @param  context      the DSpace context
     * @param  query        the query to be filtered
     * @return entityType   the entity type of collection
     * @throws SQLException if an SQL error occurs
     * @throws SearchServiceException if an Solr error occurs
     */
    public int countCollectionsAdministeredByEntityType(String query, String entityType,
                                                        Context context) throws SQLException, SearchServiceException;
    /**
     * Counts the number of Collection for which the current user has 'submit' privileges.
     * NOTE: for better performance, this method retrieves its results from an index (cache)
     *       and does not query the database directly.
     *       This means that results may be stale or outdated until
     *       https://github.com/DSpace/DSpace/issues/2853 is resolved."
     * 
     * @param q                limit the returned collection to those with metadata values matching the query terms.
     *                         The terms are used to make also a prefix query on SOLR so it can be used to implement
     *                         an autosuggest feature over the collection name
     * @param context          DSpace Context
     * @param community        parent community
     * @param entityType       limit the returned collection to those related to given entity type
     * @return                 total collections found
     * @throws SQLException              if something goes wrong
     * @throws SearchServiceException    if search error
     */
    public int countCollectionsWithSubmit(String q, Context context, Community community, String entityType)
        throws SQLException, SearchServiceException;

    /**
     * Returns the collection's entity type, if any.
     *
     * @param  collection the collection
     * @return            the entity type as string, if any
     */
    public String getEntityType(Collection collection);

    /**
     * Returns a list of all collections for a specific entity type.
     * NOTE: for better performance, this method retrieves its results from an index (cache)
     *       and does not query the database directly.
     *       This means that results may be stale or outdated until
     *       https://github.com/DSpace/DSpace/issues/2853 is resolved."
     *
     * @param context          DSpace Context
     * @param entityType       limit the returned collection to those related to given entity type
     * @return                 list of collections found
     * @throws SearchServiceException    if search error
     */
    public List<Collection> findAllCollectionsByEntityType(Context context, String entityType)
        throws SearchServiceException;

    /**
     * Returns total collection archived items
     *
     * @param context          DSpace context
     * @param collection       Collection
     * @return                 total collection archived items
     */
    int countArchivedItems(Context context, Collection collection);
}
