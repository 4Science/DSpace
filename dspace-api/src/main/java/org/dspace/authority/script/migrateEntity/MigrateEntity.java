/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.script.migrateEntity;

import static org.dspace.core.Constants.COLLECTION;
import static org.dspace.core.Constants.COMMUNITY;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.DSpaceObjectUtilsImpl;
import org.dspace.app.util.service.DSpaceObjectUtils;
import org.dspace.content.Collection;
import org.dspace.content.CollectionServiceImpl;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemServiceImpl;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.kernel.ServiceManager;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * @author mykhaylo boychuk
 */
public class MigrateEntity extends DSpaceRunnable<MigrateEntityScriptConfiguration<MigrateEntity>> {

    private static final Logger log = LogManager.getLogger(MigrateEntity.class);

    private Context context;
    private String uuid;
    private String handle;
    private String newEntityType;
    private String newFormName;

    private DSpaceObjectUtils dspaceObjectUtil;
    private ItemService itemService;
    private CollectionService collectionService;
    protected HandleService handleService;

    @Override
    public void setup() throws ParseException {
        ServiceManager serviceManager = new DSpace().getServiceManager();
        itemService = serviceManager.getServiceByName(ItemServiceImpl.class.getName(), ItemServiceImpl.class);
        collectionService = serviceManager.getServiceByName(CollectionServiceImpl.class.getName(),
                                                            CollectionServiceImpl.class);
        dspaceObjectUtil = serviceManager.getServiceByName(DSpaceObjectUtilsImpl.class.getName(),
                DSpaceObjectUtilsImpl.class);
        handleService = HandleServiceFactory.getInstance().getHandleService();
        uuid = commandLine.getOptionValue('u');
        handle = commandLine.getOptionValue("i");
        newEntityType = commandLine.getOptionValue('n');
        newFormName = commandLine.getOptionValue('f');
    }

    @Override
    public void internalRun() throws Exception {
        if (!commandLine.hasOption('i') && !commandLine.hasOption('u')) {
            throw new RuntimeException("Collection or Community handle/uuid must be provided");
        }
        boolean changeType = commandLine.hasOption('n');
        boolean changeForm = commandLine.hasOption('f');
        if (!changeType && !changeForm) {
            throw new RuntimeException("New entity type or form must be provided");
        }
        context = new Context();
        try {
            context.turnOffAuthorisationSystem();
            List<Collection> collections = new ArrayList<>();
            DSpaceObject dSpaceObject = null;
            if (StringUtils.isNotBlank(handle)) {
                dSpaceObject = handleService.resolveToObject(context, handle);
            } else {
                dSpaceObject = dspaceObjectUtil.findDSpaceObject(context, UUID.fromString(uuid));
            }
            if (Objects.isNull(dSpaceObject)) {
                throw new RuntimeException("Provided uuid isn't a Collection or Community");
            } else if (dSpaceObject.getType() == COMMUNITY ) {
                Community community = (Community) dSpaceObject;
                collections.addAll(community.getCollections());
            } else if (dSpaceObject.getType() == COLLECTION) {
                Collection col = (Collection) dSpaceObject;
                collections.add(col);
            }

            for (Collection collection : collections) {
                collection = context.reloadEntity(collection);
                int processedItems = 0;
                String currentEt = collection.getEntityType();
                String currentForm = collectionService.getMetadataFirstValue(collection,
                        MetadataSchemaEnum.CRIS.getName(), "submission", "definition", Item.ANY);
                System.out
                    .println("Current EntityType:" + currentEt + " and current form " +
                            currentForm + " of collection with name:" + collection.getName());
                if (changeForm) {
                    collectionService.setMetadataSingleValue(context, collection, MetadataSchemaEnum.CRIS.getName(),
                                                             "submission", "definition", null,this.newFormName);
                }
                if (changeType) {
                    collectionService.setMetadataSingleValue(context, collection, "dspace", "entity", "type", null,
                            this.newEntityType);
                    Iterator<Item> itemIterator = itemService.findAllByOwningCollection(context, collection);
                    handler.logInfo("Script start");
                    while (itemIterator.hasNext()) {
                        Item item = itemIterator.next();
                        itemService.setMetadataSingleValue(context, item, "dspace", "entity", "type", null,
                                this.newEntityType);
                        processedItems++;
                        context.uncacheEntity(item);
                        if (processedItems % 100 == 0) {
                            context.commit();
                        }
                    }
                }
                handler.logInfo("Processed " + processedItems + " items");
                context.uncacheEntity(collection);
                context.commit();
            }
            handler.logInfo("Script end");
        } catch (SQLException e) {
            if (context != null && context.isValid()) {
                context.abort();
            }
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (context != null && context.isValid()) {
                context.restoreAuthSystemState();
                context.complete();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public MigrateEntityScriptConfiguration<MigrateEntity> getScriptConfiguration() {
        ServiceManager serviceManager = new DSpace().getServiceManager();
        return serviceManager.getServiceByName("migrate-entity", MigrateEntityScriptConfiguration.class);
    }

}
