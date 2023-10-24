/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.impl;

import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.collections.CollectionUtils;
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
        return sourceEntityType == null || sourceEntityType.equals(itemService.getEntityTypeLabel(item));
    }

    @Override
    public void enhance(Context context, Item item) {
        try {
            cleanObsoleteVirtualFields(context, item);
            updateVirtualFieldsPlaces(context, item);
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

    private void updateVirtualFieldsPlaces(Context context, Item item) {
        List<MetadataValue> virtualSourceFields = getVirtualSourceFields(item);
        for (MetadataValue virtualSourceField : virtualSourceFields) {
            metadataWithPlaceToUpdate(item, virtualSourceField)
                .ifPresent(updatePlaces(item, virtualSourceField));
        }
    }

    private Optional<MetadataValue> metadataWithPlaceToUpdate(Item item, MetadataValue virtualSourceField) {
        return findEnhanceableValue(virtualSourceField, item)
            .filter(hasToUpdatePlace(virtualSourceField))
            .stream().findFirst();
    }

    private Predicate<MetadataValue> hasToUpdatePlace(MetadataValue virtualSourceField) {
        return metadataValue -> metadataValue.getPlace() != virtualSourceField.getPlace();
    }

    private Consumer<MetadataValue> updatePlaces(Item item, MetadataValue virtualSourceField) {
        return mv -> {
            virtualSourceField.setPlace(mv.getPlace());
            getRelatedVirtualField(item, mv)
                .ifPresent(relatedMv -> relatedMv.setPlace(mv.getPlace()));
        };
    }

    private Optional<MetadataValue> findEnhanceableValue(MetadataValue virtualSourceField, Item item) {
        return getEnhanceableMetadataValue(item).stream()
            .filter(metadataValue -> hasAuthorityEqualsTo(metadataValue, virtualSourceField.getValue()))
            .findFirst();
    }

    private List<MetadataValue> getObsoleteVirtualFields(Item item) {

        List<MetadataValue> obsoleteVirtualFields = new ArrayList<>();

        List<MetadataValue> virtualSourceFields = getVirtualSourceFields(item);
        for (MetadataValue virtualSourceField : virtualSourceFields) {
            if (!isPlaceholder(virtualSourceField) && isRelatedSourceNoMorePresent(item, virtualSourceField)) {
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
        return getVirtualFields(item).stream()
            .filter(metadataValue -> metadataValue.getPlace() == virtualSourceField.getPlace())
            .findFirst();
    }

    private void performEnhancement(Context context, Item item) throws SQLException {

        if (noEnhanceableMetadata(context, item)) {
            return;
        }

        for (MetadataValue metadataValue : getEnhanceableMetadataValue(item)) {

            if (wasValueAlreadyUsedForEnhancement(item, metadataValue)) {
                continue;
            }

            Item relatedItem = findRelatedEntityItem(context, metadataValue);
            if (relatedItem == null) {
                addVirtualField(context, item, PLACEHOLDER_PARENT_METADATA_VALUE);
                addVirtualSourceField(context, item, PLACEHOLDER_PARENT_METADATA_VALUE);
                continue;
            }

            List<MetadataValue> relatedItemMetadataValues = getMetadataValues(relatedItem, relatedItemMetadataField);
            if (relatedItemMetadataValues.isEmpty()) {
                addVirtualField(context, item, PLACEHOLDER_PARENT_METADATA_VALUE);
                addVirtualSourceField(context, item, metadataValue);
                continue;
            }
            for (MetadataValue relatedItemMetadataValue : relatedItemMetadataValues) {
                addVirtualField(context, item, relatedItemMetadataValue.getValue());
                addVirtualSourceField(context, item, metadataValue);
            }

        }

    }

    private boolean noEnhanceableMetadata(Context context, Item item) {

        return getEnhanceableMetadataValue(item)
            .stream()
            .noneMatch(metadataValue -> validAuthority(context, metadataValue));
    }

    private boolean validAuthority(Context context, MetadataValue metadataValue) {

        // FIXME: we could find a more efficient way, here we are doing twice the same action
        //  to understand if the enhanced item has at least an item whose references should be put in virtual fields.
        Item relatedItem = findRelatedEntityItem(context, metadataValue);
        return Objects.nonNull(relatedItem) &&
                                   CollectionUtils.isNotEmpty(
                                       getMetadataValues(relatedItem, relatedItemMetadataField));
    }

    private List<MetadataValue> getEnhanceableMetadataValue(Item item) {
        return getMetadataValues(item, sourceItemMetadataField);
    }

    private boolean wasValueAlreadyUsedForEnhancement(Item item, MetadataValue metadataValue) {

        if (isPlaceholderAtPlace(getVirtualFields(item), metadataValue.getPlace())) {
            return true;
        }

        return getVirtualSourceFields(item).stream()
            .anyMatch(virtualSourceField -> virtualSourceField.getPlace() == metadataValue.getPlace()
                && hasAuthorityEqualsTo(metadataValue, virtualSourceField.getValue()));

    }

    private boolean isPlaceholderAtPlace(List<MetadataValue> metadataValues, int place) {
        return place < metadataValues.size() ? isPlaceholder(metadataValues.get(place)) : false;
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

    private boolean isPlaceholder(MetadataValue metadataValue) {
        return PLACEHOLDER_PARENT_METADATA_VALUE.equals(metadataValue.getValue());
    }

    private List<MetadataValue> getMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField);
    }

    private List<MetadataValue> getVirtualSourceFields(Item item) {
        return getMetadataValues(item, getVirtualSourceMetadataField());
    }

    private List<MetadataValue> getVirtualFields(Item item) {
        return getMetadataValues(item, getVirtualMetadataField());
    }

    private void addVirtualField(Context context, Item item, String value) throws SQLException {
        itemService.addMetadata(context, item, VIRTUAL_METADATA_SCHEMA, VIRTUAL_METADATA_ELEMENT,
            getVirtualQualifier(), null, value);
    }

    private void addVirtualSourceField(Context context, Item item, MetadataValue sourceValue) throws SQLException {
        addVirtualSourceField(context, item, sourceValue.getAuthority());
    }

    private void addVirtualSourceField(Context context, Item item, String sourceValueAuthority) throws SQLException {
        itemService.addMetadata(context, item, VIRTUAL_METADATA_SCHEMA, VIRTUAL_SOURCE_METADATA_ELEMENT,
                                getVirtualQualifier(), null, sourceValueAuthority);
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
