/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.enhancer.AbstractItemEnhancer;
import org.dspace.content.enhancer.ItemEnhancer;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.util.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ItemEnhancer} that add metadata values on the given
 * item taking informations from linked entities.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class RelatedEntityItemEnhancer extends AbstractItemEnhancer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedEntityItemEnhancer.class);

    @Autowired
    private ItemService itemService;

    private String sourceEntityType;

    private String sourceItemMetadataField;

    private String relatedItemMetadataField;

    @Override
    public boolean canEnhance(Context context, Item item) {
        return sourceEntityType == null || sourceEntityType.equals(itemService.getEntityType(item));
    }

    @Override
    public void enhance(Context context, Item item) {
        try {
            cleanObsoleteVirtualFields(context, item);
            performEnhancement(context, item);
        } catch (SQLException e) {
            LOGGER.error("An error occurs enhancing item with id {}: {}", item.getID(), e.getMessage(), e);
            throw new SQLRuntimeException(e);
        }
    }

    private void cleanObsoleteVirtualFields(Context context, Item item) throws SQLException {

        List<MetadataValue> metadataValuesToDelete = getObsoleteVirtualFields(item);
        if (!metadataValuesToDelete.isEmpty()) {
            itemService.removeMetadataValues(context, item, metadataValuesToDelete);
        }

    }

    private List<MetadataValue> getObsoleteVirtualFields(Item item) {

        List<MetadataValue> obsoleteVirtualFields = new ArrayList<>();

        List<MetadataValue> virtualSourceFields = getMetadataValues(item, getVirtualSourceMetadataField());
        for (MetadataValue virtualSourceField : virtualSourceFields) {
            if (isRelatedSourceNoMorePresent(item, virtualSourceField)) {
                obsoleteVirtualFields.add(virtualSourceField);
                getRelatedVirtualField(item, virtualSourceField).ifPresent(obsoleteVirtualFields::add);
            }
        }

        return obsoleteVirtualFields;

    }

    private boolean isRelatedSourceNoMorePresent(Item item, MetadataValue virtualSourceField) {
        return getEnhanceableMetadataValue(item).stream()
            .noneMatch(metadataValue -> hasAuthorityEqualsTo(metadataValue, virtualSourceField.getValue()));
    }

    private Optional<MetadataValue> getRelatedVirtualField(Item item, MetadataValue virtualSourceField) {
        return getMetadataValues(item, getVirtualMetadataField()).stream()
            .filter(metadataValue -> metadataValue.getPlace() == virtualSourceField.getPlace())
            .findFirst();
    }

    private void performEnhancement(Context context, Item item) throws SQLException {

        for (MetadataValue metadataValue : getEnhanceableMetadataValue(item)) {

            if (wasValueAlreadyUsedForEnhancement(item, metadataValue)) {
                continue;
            }

            Item relatedItem = findRelatedEntityItem(context, metadataValue);
            if (relatedItem == null) {
                continue;
            }

            List<MetadataValue> relatedItemMetadataValues = getMetadataValues(relatedItem, relatedItemMetadataField);
            for (MetadataValue relatedItemMetadataValue : relatedItemMetadataValues) {
                addVirtualField(context, item, relatedItemMetadataValue.getValue());
                addVirtualSourceField(context, item, metadataValue);
            }

        }

    }

    private List<MetadataValue> getEnhanceableMetadataValue(Item item) {
        return getMetadataValues(item, sourceItemMetadataField).stream()
            .filter(metadataValue -> StringUtils.isNotBlank(metadataValue.getAuthority()))
            .collect(Collectors.toList());
    }

    private boolean wasValueAlreadyUsedForEnhancement(Item item, MetadataValue metadataValue) {
        return getMetadataValues(item, getVirtualSourceMetadataField()).stream()
            .anyMatch(virtualSourceField -> hasAuthorityEqualsTo(metadataValue, virtualSourceField.getValue()));
    }

    private boolean hasAuthorityEqualsTo(MetadataValue metadataValue, String authority) {
        return Objects.equals(metadataValue.getAuthority(), authority);
    }

    private Item findRelatedEntityItem(Context context, MetadataValue metadataValue) {
        try {
            UUID relatedItemUUID = UUIDUtils.fromString(metadataValue.getAuthority());
            return relatedItemUUID != null ? itemService.find(context, relatedItemUUID) : null;
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private List<MetadataValue> getMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField);
    }

    private void addVirtualField(Context context, Item item, String value) throws SQLException {
        itemService.addMetadata(context, item, VIRTUAL_METADATA_SCHEMA, VIRTUAL_METADATA_ELEMENT,
            getVirtualQualifier(), null, value);
    }

    private void addVirtualSourceField(Context context, Item item, MetadataValue sourceValue) throws SQLException {
        itemService.addMetadata(context, item, VIRTUAL_METADATA_SCHEMA, VIRTUAL_SOURCE_METADATA_ELEMENT,
            getVirtualQualifier(), null, sourceValue.getAuthority());
    }

    public void setSourceEntityType(String sourceEntityType) {
        this.sourceEntityType = sourceEntityType;
    }

    public void setSourceItemMetadataField(String sourceItemMetadataField) {
        this.sourceItemMetadataField = sourceItemMetadataField;
    }

    public void setRelatedItemMetadataField(String relatedItemMetadataField) {
        this.relatedItemMetadataField = relatedItemMetadataField;
    }

}
