/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.enhancer.AbstractItemEnhancer;
import org.dspace.content.enhancer.ItemEnhancer;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ItemEnhancer} that add metadata values on the given
 * item from a list of other metadatavalues .
 * The first value of the list that is not null matches and overwrites the existing value.
 * Some default value can be specified if no fields were found
 *
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 *
 */
public class NameEnhancer extends AbstractItemEnhancer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NameEnhancer.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private MetadataFieldService metadatafieldService;

    private String sourceEntityType;

    private String targetItemMetadataField;

    private List<String> relatedItemMetadataFields;
    private String defaultValue;

    @Override
    public boolean canEnhance(Context context, Item item) {
        return sourceEntityType == null || sourceEntityType.equals(itemService.getEntityType(item));
    }

    @Override
    public boolean enhance(Context context, Item item, boolean deepMode) {
        try {
            if (Objects.isNull(relatedItemMetadataFields) || relatedItemMetadataFields.isEmpty() ||
                StringUtils.isBlank(targetItemMetadataField)) {
                return false;
            }
            return checkNames(context, item, deepMode);
        } catch (Exception e) {
            LOGGER.error("An error occurs enhancing item with id {}: {}", item.getID(), e.getMessage(), e);
            //Exception handling not supported by ItemEnhancerService. Thus, just log to continue other enhancers
            //throw new SQLRuntimeException(e);
        }
        return false;
    }

    /**
     * Check the names/values from the specified metadatafield and compare the first value found
     * with the target metadatafield. Updates the target metadatafield when the value is different
     * or when deepMode is set.
     * When the target metadatafield has not been set before, then set this value.
     * @param context current Context
     * @param item current item
     * @param deepMode boolean
     * @return boolean value if some change/update has happened
     * @throws Exception when some error occurs
     */
    private boolean checkNames(Context context, Item item, boolean deepMode) throws Exception {
        // ignore languages of Metadata here. Assume main title is not repeated
        // Could be more simplified
        List<MetadataValue> currentnames = itemService.getMetadataByMetadataString(item, targetItemMetadataField);

        if (!currentnames.isEmpty()) {
            // some name is assigned yet
            for (MetadataValue currentname : currentnames) {
                String val = currentname.getValue();
                for (String field : relatedItemMetadataFields) {
                    List<MetadataValue> fieldnames =
                        itemService.getMetadataByMetadataString(item, field);
                    if (fieldnames.isEmpty()) {
                        continue ; //No Values, try next loop
                    }
                    for (MetadataValue fieldname : fieldnames) {
                        if (StringUtils.isNotBlank(fieldname.getValue())
                            && fieldname.getValue().contentEquals(val)) {
                            //Values are the same. No Update necessary
                            if (deepMode) {
                                // value is recalculated in deepMode
                                return updateTargetMetadata(context, item, fieldname.getValue(), true);
                            }
                            return false;
                        } else {
                            //values differ. We must update the value
                            return updateTargetMetadata(context, item, fieldname.getValue(), true);
                        }
                    }
                }
            }
            if (StringUtils.isNotBlank(defaultValue)
                && !currentnames.get(0).getValue().contentEquals(defaultValue)) {
                // None of the names above matches. Set Default-Value, if it exists. Otherwise, do nothing
                return updateTargetMetadata(context, item, defaultValue, true);
            }

        } else {
            // No Name assigned yet
            // Check existing names
            for (String field : relatedItemMetadataFields) {
                List<MetadataValue> fieldnames = itemService.getMetadataByMetadataString(item, field);
                if (fieldnames.isEmpty()) {
                    continue; //No Values, try next loop
                }
                for (MetadataValue fieldname : fieldnames) {
                    if (StringUtils.isNotBlank(fieldname.getValue())) {
                        //Got some value
                        return updateTargetMetadata(context, item, fieldname.getValue(), false);
                    }
                }
            }
            // If no name exist, set defaultvalue
            if (StringUtils.isNotBlank(defaultValue)) {
                return updateTargetMetadata(context, item, defaultValue, false);
            }
            // otherwise do not assign any value
        }
        return false;
    }

    /**
     * Update/Set the target metadata with option to clear/delete previous metadatavalues
     * @param context current Context
     * @param item item to set metadatavalue
     * @param value value to set
     * @param clear clear/delete existing values byfore
     * @return boolean value if the value has been updated successfully
     * @throws SQLException when some error occurs
     */
    private boolean updateTargetMetadata(Context context, Item item, String value, boolean clear) throws SQLException {
        MetadataField targetmd = metadatafieldService.findByString(context, targetItemMetadataField, '.');
        if (targetmd != null) {
            if (clear) {
                itemService.clearMetadata(context, item, targetmd.getMetadataSchema().getName(), targetmd.getElement(),
                    targetmd.getQualifier(), Item.ANY);
            }
            itemService.addMetadata(context, item, targetmd, null, value);
            return true;
        } else {
            LOGGER.error("No valid metadatavalue to enhance specified");
        }
        return false;
    }

    public void setSourceEntityType(String sourceEntityType) {
        this.sourceEntityType = sourceEntityType;
    }

    public void setTargetItemMetadataField(String targetItemMetadataField) {
        this.targetItemMetadataField = targetItemMetadataField;
    }

    public void setRelatedItemMetadataFields(List<String> relatedItemMetadataFields) {
        this.relatedItemMetadataFields = relatedItemMetadataFields;
    }

    public void setDefaultValue(String defaultvalue) {
        this.defaultValue = defaultvalue;
    }

}

