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

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choices;
import org.dspace.content.dto.MetadataValueDTO;
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
    public boolean enhance(Context context, Item item, boolean deepMode) {
        boolean result = false;
        if (!deepMode) {
            try {
                result = cleanObsoleteVirtualFields(context, item);
                result = updateVirtualFieldsPlaces(context, item) || result;
                result = performEnhancement(context, item) || result;
            } catch (SQLException e) {
                LOGGER.error("An error occurs enhancing item with id {}: {}", item.getID(), e.getMessage(), e);
                throw new SQLRuntimeException(e);
            }
        } else {
            List<MetadataValue> currMetadataValues = getCurrentVirtualMetadata(context, item);
            List<MetadataValueDTO> toBeMetadataValues = getToBeVirtualMetadata(context, item);
            if (!equivalent(currMetadataValues, toBeMetadataValues)) {
                try {
                    itemService.removeMetadataValues(context, item, currMetadataValues);
                    addMetadata(context, item, toBeMetadataValues);
                } catch (SQLException e) {
                    throw new SQLRuntimeException(e);
                }
                result = true;
            }
        }
        return result;
    }

    private void addMetadata(Context context, Item item, List<MetadataValueDTO> toBeMetadataValues)
            throws SQLException {
        for (MetadataValueDTO dto : toBeMetadataValues) {
            itemService.addMetadata(context, item, dto.getSchema(), dto.getElement(), dto.getQualifier(), null,
                    dto.getValue(), dto.getAuthority(), dto.getConfidence());
        }
    }

    private boolean equivalent(List<MetadataValue> currMetadataValues, List<MetadataValueDTO> toBeMetadataValues) {
        if (currMetadataValues.size() != toBeMetadataValues.size()) {
            return false;
        } else {
            for (int idx = 0; idx < currMetadataValues.size(); idx++) {
                if (!equivalent(currMetadataValues.get(idx), toBeMetadataValues.get(idx))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean equivalent(MetadataValue metadataValue, MetadataValueDTO metadataValueDTO) {
        return StringUtils.equals(metadataValue.getMetadataField().getMetadataSchema().getName(),
                metadataValueDTO.getSchema())
                && StringUtils.equals(metadataValue.getMetadataField().getElement(), metadataValueDTO.getElement())
                && StringUtils.equals(metadataValue.getMetadataField().getQualifier(), metadataValueDTO.getQualifier())
                && StringUtils.equals(metadataValue.getValue(), metadataValueDTO.getValue())
                && StringUtils.equals(metadataValue.getAuthority(), metadataValueDTO.getAuthority());
    }

    private List<MetadataValueDTO> getToBeVirtualMetadata(Context context, Item item) {
        List<MetadataValueDTO> tobeVirtualMetadata = new ArrayList<>();
        List<MetadataValue> virtualSourceFields = getEnhanceableMetadataValue(item);
        for (MetadataValue virtualSourceField : virtualSourceFields) {
            MetadataValueDTO mv = new MetadataValueDTO();
            mv.setSchema(VIRTUAL_METADATA_SCHEMA);
            mv.setElement(VIRTUAL_SOURCE_METADATA_ELEMENT);
            mv.setQualifier(getVirtualQualifier());
            String authority = virtualSourceField.getAuthority();
            Item relatedItem = null;
            if (StringUtils.isNotBlank(authority)) {
                mv.setValue(authority);
                relatedItem = findRelatedEntityItem(context, virtualSourceField);
            } else {
                mv.setValue(PLACEHOLDER_PARENT_METADATA_VALUE);
            }
            tobeVirtualMetadata.add(mv);
            if (relatedItem == null) {
                MetadataValueDTO mvRelated = new MetadataValueDTO();
                mvRelated.setSchema(VIRTUAL_METADATA_SCHEMA);
                mvRelated.setElement(VIRTUAL_METADATA_ELEMENT);
                mvRelated.setQualifier(getVirtualQualifier());
                mvRelated.setValue(PLACEHOLDER_PARENT_METADATA_VALUE);
                tobeVirtualMetadata.add(mvRelated);
                continue;
            }

            List<MetadataValue> relatedItemMetadataValues = getMetadataValues(relatedItem, relatedItemMetadataField);
            if (relatedItemMetadataValues.isEmpty()) {
                MetadataValueDTO mvRelated = new MetadataValueDTO();
                mvRelated.setSchema(VIRTUAL_METADATA_SCHEMA);
                mvRelated.setElement(VIRTUAL_METADATA_ELEMENT);
                mvRelated.setQualifier(getVirtualQualifier());
                mvRelated.setValue(PLACEHOLDER_PARENT_METADATA_VALUE);
                tobeVirtualMetadata.add(mvRelated);
                continue;
            }
            for (MetadataValue relatedItemMetadataValue : relatedItemMetadataValues) {
                MetadataValueDTO mvRelated = new MetadataValueDTO();
                mvRelated.setSchema(VIRTUAL_METADATA_SCHEMA);
                mvRelated.setElement(VIRTUAL_METADATA_ELEMENT);
                mvRelated.setQualifier(getVirtualQualifier());
                mvRelated.setValue(relatedItemMetadataValue.getValue());
                String authorityRelated = relatedItemMetadataValue.getAuthority();
                if (StringUtils.isNotBlank(authorityRelated)) {
                    mvRelated.setAuthority(authorityRelated);
                    mvRelated.setConfidence(Choices.CF_ACCEPTED);
                }
                tobeVirtualMetadata.add(mvRelated);
            }
        }
        return tobeVirtualMetadata;
    }

    private List<MetadataValue> getCurrentVirtualMetadata(Context context, Item item) {
        List<MetadataValue> currentVirtualMetadata = new ArrayList<>();
        List<MetadataValue> virtualSourceFields = getVirtualSourceFields(item);
        for (MetadataValue virtualSourceField : virtualSourceFields) {
            currentVirtualMetadata.add(virtualSourceField);
            getRelatedVirtualField(item, virtualSourceField).ifPresent(currentVirtualMetadata::add);
        }
        return currentVirtualMetadata;
    }

    private boolean cleanObsoleteVirtualFields(Context context, Item item) throws SQLException {
        boolean result = false;
        List<MetadataValue> metadataValuesToDelete = getObsoleteVirtualFields(item);
        if (!metadataValuesToDelete.isEmpty()) {
            itemService.removeMetadataValues(context, item, metadataValuesToDelete);
            result = true;
        }
        return result;
    }

    private boolean updateVirtualFieldsPlaces(Context context, Item item) {
        boolean result = false;
        List<MetadataValue> virtualSourceFields = getVirtualSourceFields(item);
        List<MetadataValue> enhanceableMetadataValue = getEnhanceableMetadataValue(item);
        for (MetadataValue virtualSourceField : virtualSourceFields) {
            Optional<MetadataValue> metadataWithPlaceToUpdate = metadataWithPlaceToUpdate(item,
                    enhanceableMetadataValue, virtualSourceField);
            if (metadataWithPlaceToUpdate.isPresent()) {
                updatePlaces(item, metadataWithPlaceToUpdate.get(), virtualSourceField);
                result = true;
            }
        }
        return result;
    }

    private Optional<MetadataValue> metadataWithPlaceToUpdate(Item item, List<MetadataValue> enhanceableMetadataValue,
            MetadataValue virtualSourceField) {
        return findMetadataValueToUpdatePlace(enhanceableMetadataValue, virtualSourceField,
                item);
    }

    private boolean hasToUpdatePlace(MetadataValue metadataValue, MetadataValue virtualSourceField) {
        return metadataValue.getPlace() != virtualSourceField.getPlace();
    }

    private void updatePlaces(Item item, MetadataValue mv, MetadataValue virtualSourceField) {
        virtualSourceField.setPlace(mv.getPlace());
        getRelatedVirtualField(item, mv)
            .ifPresent(relatedMv -> relatedMv.setPlace(mv.getPlace()));
    }

    private Optional<MetadataValue> findMetadataValueToUpdatePlace(List<MetadataValue> enhanceableMetadataValue,
            MetadataValue virtualSourceField, Item item) {
        Optional<MetadataValue> exactMatch = enhanceableMetadataValue.stream()
                .filter(metadataValue -> hasAuthorityEqualsTo(metadataValue,
                        virtualSourceField.getValue()) && !hasToUpdatePlace(metadataValue, virtualSourceField))
                .findFirst();
        if (exactMatch.isPresent()) {
            enhanceableMetadataValue.remove(exactMatch.get());
            return Optional.empty();
        } else {
            Optional<MetadataValue> authorityOnlyMatch = enhanceableMetadataValue.stream()
                    .filter(metadataValue -> hasAuthorityEqualsTo(metadataValue,
                            virtualSourceField.getValue()) && hasToUpdatePlace(metadataValue, virtualSourceField))
                    .findFirst();
            enhanceableMetadataValue.remove(authorityOnlyMatch.get());
            return authorityOnlyMatch;
        }
    }

    private List<MetadataValue> getObsoleteVirtualFields(Item item) {

        List<MetadataValue> obsoleteVirtualFields = new ArrayList<>();

        List<MetadataValue> virtualSourceFields = getVirtualSourceFields(item);
        List<MetadataValue> enhanceableMetadata = getEnhanceableMetadataValue(item);
        for (MetadataValue virtualSourceField : virtualSourceFields) {
            if (isRelatedSourceNoMorePresent(item, enhanceableMetadata, virtualSourceField)) {
                obsoleteVirtualFields.add(virtualSourceField);
                getRelatedVirtualField(item, virtualSourceField).ifPresent(obsoleteVirtualFields::add);
            }
        }

        return obsoleteVirtualFields;

    }

    /**
     * This method will look in the enhanceableMetadata if the source metadata is still present. If so, it will remove
     * form the list as it would not be used to validate other potential duplicate source metadata
     * 
     * @param item
     * @param enhanceableMetadata
     * @param virtualSourceField
     * @return true if the metadata containing a source of enhancement is still present in the list of the metadata to
     * use to enhance the item
     */
    private boolean isRelatedSourceNoMorePresent(Item item, List<MetadataValue> enhanceableMetadata,
            MetadataValue virtualSourceField) {
        Optional<MetadataValue> mv = enhanceableMetadata.stream()
                .filter(metadataValue -> hasAuthorityEqualsTo(metadataValue, virtualSourceField.getValue()))
                .findFirst();
        if (mv.isPresent()) {
            enhanceableMetadata.remove(mv.get());
            return false;
        }
        return true;
    }

    private Optional<MetadataValue> getRelatedVirtualField(Item item, MetadataValue virtualSourceField) {
        return getVirtualFields(item).stream()
            .filter(metadataValue -> metadataValue.getPlace() == virtualSourceField.getPlace())
            .findFirst();
    }

    private boolean performEnhancement(Context context, Item item) throws SQLException {
        boolean result = false;
        if (noEnhanceableMetadata(context, item)) {
            return false;
        }

        for (MetadataValue metadataValue : getEnhanceableMetadataValue(item)) {

            if (wasValueAlreadyUsedForEnhancement(item, metadataValue)) {
                continue;
            }

            Item relatedItem = findRelatedEntityItem(context, metadataValue);
            if (relatedItem == null) {
                addVirtualField(context, item, PLACEHOLDER_PARENT_METADATA_VALUE);
                addVirtualSourceField(context, item, metadataValue);
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
            result = true;
        }
        return result;
    }

    private boolean noEnhanceableMetadata(Context context, Item item) {

        return getEnhanceableMetadataValue(item)
            .stream()
            .noneMatch(metadataValue -> validAuthority(context, metadataValue));
    }

    private boolean validAuthority(Context context, MetadataValue metadataValue) {
        Item relatedItem = findRelatedEntityItem(context, metadataValue);
        return Objects.nonNull(relatedItem);
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
        return Objects.equals(metadataValue.getAuthority(), authority)
                || (StringUtils.isBlank(metadataValue.getAuthority())
                        && Objects.equals(PLACEHOLDER_PARENT_METADATA_VALUE, authority));
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
        if (StringUtils.isNotBlank(sourceValue.getAuthority())) {
            addVirtualSourceField(context, item, sourceValue.getAuthority());
        } else {
            addVirtualSourceField(context, item, PLACEHOLDER_PARENT_METADATA_VALUE);
        }
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
