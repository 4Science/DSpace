/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.dspace.discovery.IndexClientOptions.TYPE_OPTION;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.factory.IndexObjectFactoryFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.metrics.UpdateCrisMetricsInSolrDocService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Class used to reindex dspace communities/collections/items into discovery
 */
public class IndexClient extends DSpaceRunnable<IndexDiscoveryScriptConfiguration> {

    private Context context;
    private IndexingService indexer = DSpaceServicesFactory.getInstance().getServiceManager()
                                               .getServiceByName(IndexingService.class.getName(),
                                                                 IndexingService.class);

    private IndexObjectFactoryFactory indexObjectServiceFactory = IndexObjectFactoryFactory.getInstance();

    private IndexClientOptions indexClientOptions;

    private UpdateCrisMetricsInSolrDocService updateCrisMetricsInSolrDocService;

    @Override
    public void internalRun() throws Exception {
        if (indexClientOptions == IndexClientOptions.HELP) {
            printHelp();
            return;
        }

        String type = null;
        if (commandLine.hasOption(TYPE_OPTION)) {
            List<String> indexableObjectTypes = IndexObjectFactoryFactory.getInstance().getIndexFactories().stream()
                    .map((indexFactory -> indexFactory.getType())).collect(Collectors.toList());
            type = commandLine.getOptionValue(TYPE_OPTION);
            if (!indexableObjectTypes.contains(type)) {
                handler.handleException(String.format("%s is not a valid indexable object type, options: %s",
                        type, Arrays.toString(indexableObjectTypes.toArray())));
            }
        }

        /** Acquire from dspace-services in future */
        /**
         * new DSpace.getServiceManager().getServiceByName("org.dspace.discovery.SolrIndexer");
         */

        Optional<List<IndexableObject>> indexableObjects = Optional.empty();

        if (indexClientOptions == IndexClientOptions.UPDATE
                || indexClientOptions == IndexClientOptions.UPDATEANDSPELLCHECK
                || indexClientOptions == IndexClientOptions.FORCEUPDATE
                || indexClientOptions == IndexClientOptions.FORCEUPDATEANDSPELLCHECK) {
            final String param = commandLine.getOptionValue('t');
            if (param != null) {
                // check if the param is a valid indable object type
                if (indexObjectServiceFactory.getIndexFactoryByType(param) != null) {
                    type = param;
                } else {
                    throw new IllegalArgumentException(param + " is not a valid Indexable Object Type");
                }
            }
        }
        if (indexClientOptions == IndexClientOptions.REMOVE || indexClientOptions == IndexClientOptions.INDEX) {
            final String param = indexClientOptions == IndexClientOptions.REMOVE ? commandLine.getOptionValue('r') :
                    commandLine.getOptionValue('i');
            UUID uuid = null;
            try {
                uuid = UUID.fromString(param);
            } catch (Exception e) {
                // nothing to do, it should be a handle
            }

            if (uuid != null) {
                final Item item = ContentServiceFactory.getInstance().getItemService().find(context, uuid);
                if (item != null) {
                    indexableObjects = Optional.of(indexObjectServiceFactory.getIndexableObjects(context, item));
                } else {
                    // it could be a community
                    final Community community = ContentServiceFactory.getInstance().
                            getCommunityService().find(context, uuid);
                    if (community != null) {
                        indexableObjects = Optional
                                .of(indexObjectServiceFactory.getIndexableObjects(context, community));
                    } else {
                        // it could be a collection
                        final Collection collection = ContentServiceFactory.getInstance().
                                getCollectionService().find(context, uuid);
                        if (collection != null) {
                            indexableObjects = Optional
                                    .of(indexObjectServiceFactory.getIndexableObjects(context, collection));
                        }
                    }
                }
            } else {
                final DSpaceObject dso = HandleServiceFactory.getInstance()
                        .getHandleService().resolveToObject(context, param);
                if (dso != null) {
                    indexableObjects = Optional.of(indexObjectServiceFactory.getIndexableObjects(context, dso));
                }
            }
            if (!indexableObjects.isPresent()) {
                throw new IllegalArgumentException("Cannot resolve " + param + " to a DSpace object");
            }
        }

        boolean metricUpdate = true;
        if (commandLine.hasOption("m")) {
            metricUpdate = false;
        }
        if (indexClientOptions == IndexClientOptions.REMOVE) {
            handler.logInfo("Removing " + commandLine.getOptionValue("r") + " from Index");
            for (IndexableObject idxObj : indexableObjects.get()) {
                indexer.unIndexContent(context, idxObj.getUniqueIndexID());
            }
        } else if (indexClientOptions == IndexClientOptions.CLEAN) {
            handler.logInfo("Cleaning Index");
            indexer.cleanIndex();
        } else if (indexClientOptions == IndexClientOptions.DELETE) {
            handler.logInfo("Deleting Index");
            indexer.deleteIndex();
        } else if (indexClientOptions == IndexClientOptions.BUILD ||
            indexClientOptions == IndexClientOptions.BUILDANDSPELLCHECK) {
            handler.logInfo("(Re)building index from scratch.");
            if (StringUtils.isNotBlank(type)) {
                handler.logWarning(String.format("Type option, %s, not applicable for entire index rebuild option, b" +
                        ", type will be ignored", TYPE_OPTION));
            }
            indexer.deleteIndex();
            indexer.createIndex(context);
            if (indexClientOptions == IndexClientOptions.BUILDANDSPELLCHECK) {
                checkRebuildSpellCheck(commandLine, indexer);
            }
        } else if (indexClientOptions == IndexClientOptions.OPTIMIZE) {
            handler.logInfo("Optimizing search core.");
            indexer.optimize();
        } else if (indexClientOptions == IndexClientOptions.SPELLCHECK) {
            checkRebuildSpellCheck(commandLine, indexer);
        } else if (indexClientOptions == IndexClientOptions.INDEX) {
            handler.logInfo("Indexing " + commandLine.getOptionValue('i') + " force " + commandLine.hasOption("f"));
            final long startTimeMillis = System.currentTimeMillis();
            final long count = indexAll(indexer, ContentServiceFactory.getInstance().
                    getItemService(), context, indexableObjects.get());
            final long seconds = (System.currentTimeMillis() - startTimeMillis) / 1000;
            handler.logInfo("Indexed " + count + " object" + (count > 1 ? "s" : "") + " in " + seconds + " seconds");
        } else if (indexClientOptions == IndexClientOptions.UPDATE ||
            indexClientOptions == IndexClientOptions.UPDATEANDSPELLCHECK) {
            handler.logInfo("Updating Index");
            indexer.updateIndex(context, false, type);
            if (indexClientOptions == IndexClientOptions.UPDATEANDSPELLCHECK) {
                checkRebuildSpellCheck(commandLine, indexer);
            }
        } else if (indexClientOptions == IndexClientOptions.FORCEUPDATE ||
            indexClientOptions == IndexClientOptions.FORCEUPDATEANDSPELLCHECK) {
            handler.logInfo("Updating Index");
            indexer.updateIndex(context, true, type);
            if (indexClientOptions == IndexClientOptions.FORCEUPDATEANDSPELLCHECK) {
                checkRebuildSpellCheck(commandLine, indexer);
            }
        }

        handler.logInfo("Done with indexing");
        if (metricUpdate) {
            if (indexableObjects.isPresent()) {
                final String param = indexClientOptions == IndexClientOptions.REMOVE ?
                        commandLine.getOptionValue('r') :
                        commandLine.getOptionValue('i');
                UUID uuid = null;
                try {
                    uuid = UUID.fromString(param);
                } catch (Exception e) {
                    uuid = HandleServiceFactory.getInstance()
                            .getHandleService().resolveToObject(context, param).getID();
                }

                updateCrisMetricsInSolrDocService.performUpdate(context, handler, true, uuid);

            } else {
                updateCrisMetricsInSolrDocService.performUpdate(context, handler, true);
            }
        }
    }

    @Override
    public IndexDiscoveryScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("index-discovery",
                                                                 IndexDiscoveryScriptConfiguration.class);
    }

    public void setup() throws ParseException {
        try {
            context = new Context(Context.Mode.READ_ONLY);
            context.turnOffAuthorisationSystem();
        } catch (Exception e) {
            throw new ParseException("Unable to create a new DSpace Context: " + e.getMessage());
        }
        indexClientOptions = IndexClientOptions.getIndexClientOption(commandLine);
        updateCrisMetricsInSolrDocService = new DSpace().getServiceManager().getServiceByName(
                UpdateCrisMetricsInSolrDocService.class.getName(), UpdateCrisMetricsInSolrDocService.class);
    }
    /**
     * Indexes the given object and all children, if applicable.
     *
     * @param indexingService
     * @param itemService
     * @param context                 The relevant DSpace Context.
     * @param indexableObjects        DSpace objects to index recursively
     * @throws IOException            A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SearchServiceException in case of a solr exception
     * @throws SQLException           An exception that provides information on a database access error or other errors.
     */
    private static long indexAll(final IndexingService indexingService,
                                 final ItemService itemService,
                                 final Context context,
                                 final List<IndexableObject> indexableObjects)
        throws IOException, SearchServiceException, SQLException {
        long count = 0;
        IndexObjectFactoryFactory indexObjectServiceFactory = IndexObjectFactoryFactory.getInstance();
        for (IndexableObject iObj : indexableObjects) {
            indexingService.indexContent(context, iObj, true, true);
            count++;
            if (iObj.getIndexedObject() instanceof Community) {
                final Community community = (Community) iObj.getIndexedObject();
                final String communityHandle = community.getHandle();
                for (final Community subcommunity : community.getSubcommunities()) {
                    count += indexAll(indexingService, itemService, context,
                            indexObjectServiceFactory.getIndexableObjects(context, subcommunity));
                    //To prevent memory issues, discard an object from the cache after processing
                    context.uncacheEntity(subcommunity);
                }
                final Community reloadedCommunity = (Community) HandleServiceFactory.getInstance().getHandleService()
                                                                                    .resolveToObject(context,
                                                                                                     communityHandle);
                for (final Collection collection : reloadedCommunity.getCollections()) {
                    count++;
                    indexingService.indexContent(context, new IndexableCollection(collection), true, true);
                    count += indexItems(indexingService, itemService, context, collection);
                    //To prevent memory issues, discard an object from the cache after processing
                    context.uncacheEntity(collection);
                }
            } else if (iObj instanceof IndexableCollection) {
                count += indexItems(indexingService, itemService, context, (Collection) iObj.getIndexedObject());
            }
        }
        return count;
    }

    /**
     * Indexes all items in the given collection.
     *
     * @param indexingService
     * @param itemService
     * @param context         The relevant DSpace Context.
     * @param collection      collection to index
     * @throws IOException            A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SearchServiceException in case of a solr exception
     * @throws SQLException           An exception that provides information on a database access error or other errors.
     */
    private static long indexItems(final IndexingService indexingService,
                                   final ItemService itemService,
                                   final Context context,
                                   final Collection collection)
        throws IOException, SearchServiceException, SQLException {
        long count = 0;

        final Iterator<Item> itemIterator = itemService.findByCollection(context, collection);
        while (itemIterator.hasNext()) {
            Item item = itemIterator.next();
            indexingService.indexContent(context, new IndexableItem(item), true, false);
            count++;
            //To prevent memory issues, discard an object from the cache after processing
            context.uncacheEntity(item);
        }
        indexingService.commit();

        return count;
    }

    /**
     * Check the command line options and rebuild the spell check if active.
     *
     * @param line    the command line options
     * @param indexer the solr indexer
     * @throws SearchServiceException in case of a solr exception
     * @throws IOException passed through
     */
    protected void checkRebuildSpellCheck(CommandLine line, IndexingService indexer)
        throws SearchServiceException, IOException {
        handler.logInfo("Rebuilding spell checker.");
        indexer.buildSpellCheck();
    }

}
