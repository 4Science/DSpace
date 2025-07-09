/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.IOException;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * AbstractCurationTask encapsulates a few common patterns of task use,
 * resources, and convenience methods.
 *
 * @author richardrodgers
 */
public abstract class AbstractCurationTask implements CurationTask {
    // invoking curator
    protected Curator curator = null;
    // curator-assigned taskId
    protected String taskId = null;
    protected CommunityService communityService;
    protected ItemService itemService;
    protected HandleService handleService;
    protected ConfigurationService configurationService;
    protected SearchService searchService;

    private void addOrUpdateProcessMetadata(Context context, Item item) throws SQLException {
        List<MetadataValue> existingProcesses = itemService.getMetadata(item, "cris", "curation", "process", Item.ANY);

        // Check if processName already exists
        boolean alreadyExists = existingProcesses.stream()
                                                 .anyMatch(md -> md.getValue().equalsIgnoreCase(taskId));

        if (!alreadyExists) {
            itemService.addMetadata(context, item, "cris", "curation", "process", null, taskId);
        }
    }

    private void appendHistoryMetadata(Context context, Item item) throws SQLException {
        // Prepare timestamp in ISO 8601 format with GMT zone
        String now = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);

        String newEntry = "Executed " + taskId + " on " + now + " (GMT).";

        List<MetadataValue> existing = itemService.getMetadata(item, "cris", "curation", "history", Item.ANY);

        String combinedValue;
        if (existing.isEmpty()) {
            combinedValue = newEntry;
        } else {
            // Assume only one value exists and we want to append to it
            String currentValue = existing.get(0).getValue();
            combinedValue = currentValue + "\n" + newEntry;
        }

        // Remove old metadata
        itemService.clearMetadata(context, item, "cris", "curation", "history", Item.ANY);

        // Add the new combined value
        itemService.addMetadata(context, item, "cris", "curation", "history", null, combinedValue);
    }

    protected boolean isSuccessfullyExecuted(Item dso) {
        return true;
    }

    private void setExecutionMetadata(Item item) throws SQLException, AuthorizeException {

        Context context = Curator.curationContext();

        // 1. Add or update cris.curation.process metadata (repetitive)
        if (isSuccessfullyExecuted(item)) {
            addOrUpdateProcessMetadata(context, item);
        }

        // 2. Append to cris.curation.history metadata
        appendHistoryMetadata(context, item);

        // Commit changes
        itemService.update(context, item);
    }

    @Override
    public void init(Curator curator, String taskId) throws IOException {
        this.curator = curator;
        this.taskId = taskId;
        communityService = ContentServiceFactory.getInstance().getCommunityService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        handleService = HandleServiceFactory.getInstance().getHandleService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        searchService = SearchUtils.getSearchService();
    }

    @Override
    public abstract int perform(DSpaceObject dso) throws IOException;

    /**
     * Distributes a task through a DSpace container - a convenience method
     * for tasks declaring the <code>@Distributive</code> property.
     * <P>
     * This method invokes the 'performObject()' method on the current DSO, and
     * then recursively invokes the 'performObject()' method on all DSOs contained
     * within the current DSO. For example: if a Community is passed in, then
     * 'performObject()' will be called on that Community object, as well as
     * on all SubCommunities/Collections/Items contained in that Community.
     * <P>
     * Individual tasks MUST override either the <code>performObject</code> method or
     * the <code>performItem</code> method to ensure the task is run on either all
     * DSOs or just all Items, respectively.
     *
     * @param dso current DSpaceObject
     * @throws IOException if IO error
     */
    protected void distribute(DSpaceObject dso) throws IOException {
        try {
            Context ctx = Curator.curationContext();
            int type = dso.getType();
            final int batchSize = configurationService.getIntProperty("curation.task.batchsize", 100);
            curator.logInfo(String.format("Curation task %s using batch size of %d", this.taskId, batchSize));

            UUID lastProcessedId = null;
            List<IndexableObject> indexables;

            do {
                DiscoverQuery query = new DiscoverQuery();
                query.setMaxResults(batchSize);
                query.setSortField("search.resourceid", DiscoverQuery.SORT_ORDER.asc);

                // Only query for items
                query.addFilterQueries("search.resourcetype:Item");

                // Add location filter based on object type
                switch (type) {
                    case Constants.ITEM:
                        query.addFilterQueries("search.resourceid:" + dso.getID());
                        break;
                    case Constants.COLLECTION:
                        query.addFilterQueries("location.coll:" + dso.getID());
                        break;
                    case Constants.COMMUNITY:
                        query.addFilterQueries("location.comm:" + dso.getID());
                        break;
                    case Constants.SITE:
                        // No additional filter needed: all items
                        break;
                    default:
                        break;
                }

                if (curator.getModifiedSinceDays() > 0) {
                    query.addFilterQueries("lastModified_dt:[NOW-" + curator.getModifiedSinceDays() + "DAYS/DAY TO *]");
                }

                if (!curator.isForce()) {
                    // Exclude items already processed by this curation task
                    query.addFilterQueries("-cris.curation.process:" + taskId);
                }

                if (lastProcessedId != null) {
                    // Simulate cursor by skipping all IDs <= lastProcessedId
                    query.addFilterQueries("search.resourceid:{" + lastProcessedId + " TO *]");
                }

                DiscoverResult result = searchService.search(ctx, query);
                indexables = result.getIndexableObjects();
                curator.logInfo(String.format("Curation task %s found %d processable items",
                                              this.taskId,
                                              result.getIndexableObjects().size()));

                if (indexables != null && !indexables.isEmpty()) {
                    for (IndexableObject idxObj : indexables) {
                        if (idxObj instanceof IndexableItem) {
                            Item item = ((IndexableItem) idxObj).getIndexedObject();
                            if (item != null) {
                                try {
                                    performObject(item);
                                } catch (Exception e) {
                                    String msg = "Unable to process item with handle=" + item.getHandle()
                                        + " and uuid=" + item.getID();
                                    setResult(msg);
                                    curator.logError(msg, e);
                                }
                                try {
                                    setExecutionMetadata(item);
                                } catch (Exception e) {
                                    String msg = "Unable to set metadata for item with handle=" + item.getHandle()
                                        + " and uuid=" + item.getID();
                                    setResult(msg);
                                    curator.logError(msg, e);
                                }
                                lastProcessedId = item.getID();
                            }
                        }
                    }

                    ctx.commit();
                }

            } while (indexables != null && !indexables.isEmpty());

        } catch (SQLException | SearchServiceException e) {
            throw new IOException("Error distributing task [" + taskId + "] for object " + dso.getHandle(), e);
        }
    }


    /**
     * Performs task upon a single DSpaceObject. Used in conjunction with the
     * <code>distribute</code> method to run a single task across multiple DSpaceObjects.
     * <P>
     * By default, this method just wraps a call to <code>performItem</code>
     * for each Item Object.
     * <P>
     * You should override this method if you want to use
     * <code>distribute</code> to run your task across multiple DSpace Objects.
     * <P>
     * Either this method or <code>performItem</code> should be overridden if
     * <code>distribute</code> method is used.
     *
     * @param dso the DSpaceObject
     * @throws SQLException if database error
     * @throws IOException  if IO error
     */
    protected void performObject(DSpaceObject dso) throws SQLException, IOException {
        // By default this method only performs tasks on Items
        // (You should override this method if you want to perform task on all objects)
        if (dso.getType() == Constants.ITEM) {
            Item item = (Item) dso;
            performItem(item);
        }

        //no-op for all other types of DSpace Objects
    }

    /**
     * Performs task upon a single DSpace Item. Used in conjunction with the
     * <code>distribute</code> method to run a single task across multiple Items.
     * <P>
     * You should override this method if you want to use
     * <code>distribute</code> to run your task across multiple DSpace Items.
     * <P>
     * Either this method or <code>performObject</code> should be overridden if
     * <code>distribute</code> method is used.
     *
     * @param item the DSpace Item
     * @throws SQLException if database error
     * @throws IOException  if IO error
     */
    protected void performItem(Item item) throws SQLException, IOException {
        // no-op - override when using 'distribute' method
    }

    @Override
    public int perform(Context ctx, String id) throws IOException {
        DSpaceObject dso = dereference(ctx, id);
        return (dso != null) ? perform(dso) : Curator.CURATE_FAIL;
    }

    /**
     * Returns a DSpaceObject for passed identifier, if it exists
     *
     * @param ctx DSpace context
     * @param id  canonical id of object
     * @return dso
     * DSpace object, or null if no object with id exists
     * @throws IOException if IO error
     */
    protected DSpaceObject dereference(Context ctx, String id) throws IOException {
        try {
            return handleService.resolveToObject(ctx, id);
        } catch (SQLException sqlE) {
            throw new IOException(sqlE.getMessage(), sqlE);
        }
    }

    /**
     * Sends message to the reporting stream
     *
     * @param message the message to stream
     */
    protected void report(String message) {
        curator.report(message);
    }

    /**
     * Assigns the result of the task performance
     *
     * @param result the result string
     */
    protected void setResult(String result) {
        String current = curator.getResult(taskId);
        if (StringUtils.isNotBlank(current)) {
            curator.setResult(taskId, current + System.lineSeparator() + result);
        } else {
            curator.setResult(taskId, result);
        }
    }


    /**
     * Returns task configuration property value for passed name, else
     * <code>null</code> if no properties defined or no value for passed key.
     * If a taskID/Name is specified, prepend it on the configuration name.
     *
     * @param name the property name
     * @return value
     * the property value, or null
     */
    protected String taskProperty(String name) {
        String parameter = curator.getRunParameter(name);
        if (null != parameter) {
            return parameter;
        } else if (StringUtils.isNotBlank(taskId)) {
            return configurationService.getProperty(taskId + "." + name);
        } else {
            return configurationService.getProperty(name);
        }
    }

    /**
     * Returns task configuration integer property value for passed name, else
     * passed default value if no properties defined or no value for passed key.
     * If a taskID/Name is specified, prepend it on the configuration name.
     *
     * @param name         the property name
     * @param defaultValue value
     *                     the default value
     * @return value
     * the property value, or default value
     */
    protected int taskIntProperty(String name, int defaultValue) {
        String parameter = curator.getRunParameter(name);
        if (null != parameter) {
            return Integer.valueOf(parameter);
        } else if (StringUtils.isNotBlank(taskId)) {
            return configurationService.getIntProperty(taskId + "." + name, defaultValue);
        } else {
            return configurationService.getIntProperty(name, defaultValue);
        }
    }

    /**
     * Returns task configuration long property value for passed name, else
     * passed default value if no properties defined or no value for passed key.
     * If a taskID/Name is specified, prepend it on the configuration name.
     *
     * @param name         the property name
     * @param defaultValue value
     *                     the default value
     * @return value
     * the property value, or default
     */
    protected long taskLongProperty(String name, long defaultValue) {
        String parameter = curator.getRunParameter(name);
        if (null != parameter) {
            return Long.valueOf(parameter);
        } else if (StringUtils.isNotBlank(taskId)) {
            return configurationService.getLongProperty(taskId + "." + name, defaultValue);
        } else {
            return configurationService.getLongProperty(name, defaultValue);
        }
    }

    /**
     * Returns task configuration boolean property value for passed name, else
     * passed default value if no properties defined or no value for passed key.
     * If a taskID/Name is specified, prepend it on the configuration name.
     *
     * @param name         the property name
     * @param defaultValue value
     *                     the default value
     * @return value
     * the property value, or default
     */
    protected boolean taskBooleanProperty(String name, boolean defaultValue) {
        String parameter = curator.getRunParameter(name);
        if (null != parameter) {
            return Boolean.valueOf(parameter);
        } else if (StringUtils.isNotBlank(taskId)) {
            return configurationService.getBooleanProperty(taskId + "." + name, defaultValue);
        } else {
            return configurationService.getBooleanProperty(name, defaultValue);
        }
    }

    /**
     * Returns task configuration Array property value for passed name, else
     * <code>null</code> if no properties defined or no value for passed key.
     * If a taskID/Name is specified, prepend it on the configuration name.
     *
     * @param name the property name
     * @return value
     * the property value, or null
     */
    protected String[] taskArrayProperty(String name) {
        String parameter = curator.getRunParameter(name);
        if (null != parameter) {
            return new String[] {parameter};
        } else if (StringUtils.isNotBlank(taskId)) {
            return configurationService.getArrayProperty(taskId + "." + name);
        } else {
            return configurationService.getArrayProperty(name);
        }
    }
}
