/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.service.impl;

import static org.apache.commons.lang3.ArrayUtils.contains;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResultItemIterator;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.orcid.OrcidOperation;
import org.dspace.orcid.OrcidQueue;
import org.dspace.orcid.dao.OrcidQueueDAO;
import org.dspace.orcid.model.OrcidEntityType;
import org.dspace.orcid.service.OrcidHistoryService;
import org.dspace.orcid.service.OrcidQueueService;
import org.dspace.profile.OrcidEntitySyncPreference;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OrcidQueueService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidQueueServiceImpl implements OrcidQueueService {

    @Autowired
    private OrcidQueueDAO orcidQueueDAO;

    @Autowired
    private OrcidHistoryService orcidHistoryService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public List<OrcidQueue> findByProfileItemId(Context context, UUID profileItemId) throws SQLException {
        return orcidQueueDAO.findByProfileItemId(context, profileItemId, -1, 0);
    }

    @Override
    public List<OrcidQueue> findByProfileItemId(Context context, UUID profileItemId, Integer limit, Integer offset)
        throws SQLException {
        return orcidQueueDAO.findByProfileItemId(context, profileItemId, limit, offset);
    }

    @Override
    public List<OrcidQueue> findByProfileItemAndEntity(Context context, Item profileItem, Item entity)
        throws SQLException {
        return orcidQueueDAO.findByProfileItemAndEntity(context, profileItem, entity);
    }

    @Override
    public List<OrcidQueue> findByProfileItemOrEntity(Context context, Item item) throws SQLException {
        return orcidQueueDAO.findByProfileItemOrEntity(context, item);
    }

    @Override
    public List<OrcidQueue> findByEntity(Context context, Item item) throws SQLException {
        return orcidQueueDAO.findByEntity(context, item);
    }

    @Override
    public long countByProfileItemId(Context context, UUID profileItemId) throws SQLException {
        return orcidQueueDAO.countByProfileItemId(context, profileItemId);
    }

    @Override
    public List<OrcidQueue> findAll(Context context) throws SQLException {
        return orcidQueueDAO.findAll(context, OrcidQueue.class);
    }

    @Override
    public OrcidQueue create(Context context, Item profileItem, Item entity) throws SQLException {
        Optional<String> putCode = orcidHistoryService.findLastPutCode(context, profileItem, entity);
        if (putCode.isPresent()) {
            return createEntityUpdateRecord(context, profileItem, entity, putCode.get());
        } else {
            return createEntityInsertionRecord(context, profileItem, entity);
        }
    }

    @Override
    public OrcidQueue createEntityInsertionRecord(Context context, Item profileItem, Item entity) throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setEntity(entity);
        orcidQueue.setRecordType(itemService.getEntityTypeLabel(entity));
        orcidQueue.setProfileItem(profileItem);
        orcidQueue.setDescription(getMetadataValue(entity, "dc.title"));
        orcidQueue.setOperation(OrcidOperation.INSERT);
        return orcidQueueDAO.create(context, orcidQueue);
    }

    @Override
    public OrcidQueue createEntityUpdateRecord(Context context, Item profileItem, Item entity, String putCode)
        throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setProfileItem(profileItem);
        orcidQueue.setEntity(entity);
        orcidQueue.setPutCode(putCode);
        orcidQueue.setRecordType(itemService.getEntityTypeLabel(entity));
        orcidQueue.setDescription(getMetadataValue(entity, "dc.title"));
        orcidQueue.setOperation(OrcidOperation.UPDATE);
        return orcidQueueDAO.create(context, orcidQueue);
    }

    @Override
    public OrcidQueue createEntityDeletionRecord(Context context, Item profileItem, String description, String type,
        String putCode)
        throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setRecordType(type);
        orcidQueue.setProfileItem(profileItem);
        orcidQueue.setPutCode(putCode);
        orcidQueue.setDescription(description);
        orcidQueue.setOperation(OrcidOperation.DELETE);
        return orcidQueueDAO.create(context, orcidQueue);
    }

    @Override
    public OrcidQueue createProfileInsertionRecord(Context context, Item profile, String description, String recordType,
        String metadata) throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setEntity(profile);
        orcidQueue.setRecordType(recordType);
        orcidQueue.setProfileItem(profile);
        orcidQueue.setDescription(description);
        orcidQueue.setMetadata(metadata);
        orcidQueue.setOperation(OrcidOperation.INSERT);
        return orcidQueueDAO.create(context, orcidQueue);
    }

    @Override
    public OrcidQueue createProfileDeletionRecord(Context context, Item profile, String description, String recordType,
        String metadata, String putCode) throws SQLException {
        OrcidQueue orcidQueue = new OrcidQueue();
        orcidQueue.setEntity(profile);
        orcidQueue.setRecordType(recordType);
        orcidQueue.setProfileItem(profile);
        orcidQueue.setDescription(description);
        orcidQueue.setPutCode(putCode);
        orcidQueue.setMetadata(metadata);
        orcidQueue.setOperation(OrcidOperation.DELETE);
        return orcidQueueDAO.create(context, orcidQueue);
    }

    @Override
    public void deleteById(Context context, Integer id) throws SQLException {
        OrcidQueue orcidQueue = orcidQueueDAO.findByID(context, OrcidQueue.class, id);
        if (orcidQueue != null) {
            delete(context, orcidQueue);
        }
    }

    @Override
    public List<OrcidQueue> findByAttemptsLessThan(Context context, int attempts) throws SQLException {
        return orcidQueueDAO.findByAttemptsLessThan(context, attempts);
    }

    @Override
    public void delete(Context context, OrcidQueue orcidQueue) throws SQLException {
        orcidQueueDAO.delete(context, orcidQueue);
    }

    @Override
    public void deleteByEntityAndRecordType(Context context, Item entity, String recordType) throws SQLException {
        List<OrcidQueue> records = orcidQueueDAO.findByEntityAndRecordType(context, entity, recordType);
        for (OrcidQueue record : records) {
            orcidQueueDAO.delete(context, record);
        }
    }

    @Override
    public void deleteByProfileItemAndRecordType(Context context, Item profileItem, String recordType)
        throws SQLException {
        List<OrcidQueue> records = orcidQueueDAO.findByProfileItemAndRecordType(context, profileItem, recordType);
        for (OrcidQueue record : records) {
            orcidQueueDAO.delete(context, record);
        }
    }

    @Override
    public OrcidQueue find(Context context, int id) throws SQLException {
        return orcidQueueDAO.findByID(context, OrcidQueue.class, id);
    }

    @Override
    public void update(Context context, OrcidQueue orcidQueue) throws SQLException {
        orcidQueueDAO.save(context, orcidQueue);
    }

    @Override
    public void recalculateOrcidQueue(Context context, Item profileItem, OrcidEntityType orcidEntityType,
        OrcidEntitySyncPreference preference) throws SQLException {

        String entityType = orcidEntityType.getEntityType();
        if (preference == OrcidEntitySyncPreference.DISABLED) {
            deleteByProfileItemAndRecordType(context, profileItem, entityType);
        } else {
            Iterator<Item> entities = findAllEntitiesLinkableWith(context, profileItem, entityType);
            while (entities.hasNext()) {
                create(context, profileItem, entities.next());
            }
        }

    }

    private Iterator<Item> findAllEntitiesLinkableWith(Context context, Item owner, String entityType) {

        String ownerType = itemService.getMetadataFirstValue(owner, "dspace", "entity", "type", Item.ANY);

        String query = choiceAuthorityService.getAuthorityControlledFieldsByEntityType(ownerType).stream()
            .map(metadataField -> metadataField.replaceAll("_", "."))
            .filter(metadataField -> shouldNotBeIgnoredForOrcid(metadataField))
            .map(metadataField -> metadataField + "_allauthority: \"" + owner.getID().toString() + "\"")
            .collect(Collectors.joining(" OR "));

        if (StringUtils.isEmpty(query)) {
            return Collections.emptyIterator();
        }

        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.addDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.addFilterQueries(query);
        discoverQuery.addFilterQueries("search.entitytype:" + entityType);

        return new DiscoverResultItemIterator(context, discoverQuery);

    }

    private boolean shouldNotBeIgnoredForOrcid(String metadataField) {
        return !contains(configurationService.getArrayProperty("orcid.linkable-metadata-fields.ignore"), metadataField);
    }

    private String getMetadataValue(Item item, String metadatafield) {
        return itemService.getMetadataFirstValue(item, new MetadataFieldName(metadatafield), Item.ANY);
    }
}
