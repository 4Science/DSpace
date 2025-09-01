/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Sample consumer to link a dspace item with another (i.e a publication with
 * the corresponding dataset or viceversa)
 *
 * @author Andrea Bollini
 * @version $Revision $
 */
public class ReciprocalItemAuthorityConsumer implements Consumer {
    private static final Logger log = LogManager.getLogger(ReciprocalItemAuthorityConsumer.class);

    private final ConfigurationService configurationService = new DSpace().getConfigurationService();
    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private final Map<String, String> reciprocalMetadataMap = new ConcurrentHashMap<>();
    private final transient Set<UUID> processedHandles = new HashSet<>();

    private final IndexingService indexer = DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName(IndexingService.class.getName(), IndexingService.class);

    @Override
    public void initialize() throws Exception {
        iniReciprocalMetadata();
    }

    @Override
    public void consume(Context ctx, Event event) throws Exception {
        try {
            ctx.turnOffAuthorisationSystem();
            Item item = (Item) event.getSubject(ctx);
            if (item == null || !item.isArchived()) {
                return;
            }
            if (processedHandles.contains(item.getID())) {
                return;
            } else {
                processedHandles.add(item.getID());
            }
            if (!reciprocalMetadataMap.isEmpty()) {
                for (String k : reciprocalMetadataMap.keySet()) {
                    String entityType = k.split("\\.", 2)[0];
                    String metadata = k.split("\\.", 2)[1];
                    checkItemRefs(ctx, item, entityType, metadata, reciprocalMetadataMap.get(k));
                }
            }
        } finally {
            ctx.restoreAuthSystemState();
        }
    }

    private void checkItemRefs(Context ctx, Item item, String entityType, String metadata, String reciprocalMetadata)
            throws SQLException {
        // only process the reciprocal metadata for the appropriate entity type
        if (!StringUtils.equalsIgnoreCase(itemService.getEntityType(item), entityType)) {
            return;
        }

        List<MetadataValue> meta = itemService.getMetadataByMetadataString(item, metadata);
        if (meta != null) {
            for (MetadataValue md : meta) {
                if (md.getAuthority() != null && md.getConfidence() == Choices.CF_ACCEPTED) {
                    try {
                        UUID id = UUID.fromString(md.getAuthority());
                        Item target = itemService.find(ctx, id);
                        if (target != null) {
                            assureReciprocalLink(ctx, target, reciprocalMetadata, item.getName(),
                                    item.getID().toString()
                            );
                        }
                    } catch (IllegalArgumentException e) {
                        // if the authority is not an uuid nothing is needed
                        log.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

    private void assureReciprocalLink(Context ctx, Item target, String mdString, String name, String sourceUuid)
            throws SQLException {
        List<MetadataValue> meta = target.getItemService().getMetadataByMetadataString(target, mdString);
        String[] mdSplit = mdString.split("\\.");
        if (meta != null) {
            for (MetadataValue md : meta) {
                if (StringUtils.equals(md.getAuthority(), sourceUuid)) {
                    return;
                }
            }
        }

        itemService.addMetadata(ctx, target, mdSplit[0], mdSplit[1], mdSplit.length > 2 ? mdSplit[2] : null, null,
                name, sourceUuid, Choices.CF_ACCEPTED);
        reindexItem(ctx, target);
    }

    private void reindexItem(Context ctx, Item target) throws SQLException {
        IndexableItem item = new IndexableItem(target);
        item.setIndexedObject(ctx.reloadEntity(item.getIndexedObject()));
        String uniqueIndexID = item.getUniqueIndexID();
        if (uniqueIndexID != null) {
            try {
                indexer.indexContent(ctx, item, true, false, false);
                log.debug("Indexed "
                        + item.getTypeText()
                        + ", id=" + item.getID()
                        + ", unique_id=" + uniqueIndexID);
            } catch (Exception e) {
                log.error("Failed while indexing object: ", e);
            }
        }
    }

    private void iniReciprocalMetadata() {
        List<String> properties = configurationService.getPropertyKeys("ItemAuthority.reciprocalMetadata");
        for (String conf : properties) {
            reciprocalMetadataMap.put(conf.substring("ItemAuthority.reciprocalMetadata.".length()),
                    configurationService.getProperty(conf));
            reciprocalMetadataMap.put(configurationService.getProperty(conf),
                    conf.substring("ItemAuthority.reciprocalMetadata.".length()));
        }
    }

    @Override
    public void end(Context ctx) throws Exception {
        processedHandles.clear();
    }

    @Override
    public void finish(Context ctx) {
        // nothing
    }

}