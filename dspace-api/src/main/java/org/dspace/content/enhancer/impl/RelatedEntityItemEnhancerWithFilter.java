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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choices;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.logic.Filter;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Extension of {@link RelatedEntityItemEnhancer}
 * to retrieve items that respect {@link Filter}.
 *
 * @author Francesco Pio Scognamiglio  (francescopio.scognamiglio at 4science.com)
 * @author Francesco Molinaro (francesco.molinaro at 4science.com)
 *
 */
public class RelatedEntityItemEnhancerWithFilter extends RelatedEntityItemEnhancer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedEntityItemEnhancerWithFilter.class);

    @Autowired
    private RelatedEntityItemEnhancerUtils relatedEntityItemEnhancerUtils;

    private Filter filter;

    protected Map<String, List<MetadataValueDTO>> getToBeVirtualMetadata(Context context, Item item) {
        Map<String, List<MetadataValueDTO>> tobeVirtualMetadataMap = new HashMap<String, List<MetadataValueDTO>>();

        Set<String> virtualSources = getVirtualSources(item);
        for (String authority : virtualSources) {
            List<MetadataValueDTO> tobeVirtualMetadata = new ArrayList<>();

            boolean isValid = validate(context, item);
            if (isValid) {
                MetadataValueDTO mvRelated = new MetadataValueDTO();
                mvRelated.setSchema(VIRTUAL_METADATA_SCHEMA);
                mvRelated.setElement(VIRTUAL_METADATA_ELEMENT);
                mvRelated.setQualifier(getVirtualQualifier());
                mvRelated.setValue(item.getName() + "::" + item.getID());
                tobeVirtualMetadata.add(mvRelated);
                tobeVirtualMetadataMap.put(authority, tobeVirtualMetadata);
            } else {
                Item relatedItem = null;
                relatedItem = findRelatedEntityItem(context, authority);
                if (relatedItem == null) {
                    MetadataValueDTO mvRelated = new MetadataValueDTO();
                    mvRelated.setSchema(VIRTUAL_METADATA_SCHEMA);
                    mvRelated.setElement(VIRTUAL_METADATA_ELEMENT);
                    mvRelated.setQualifier(getVirtualQualifier());
                    mvRelated.setValue(PLACEHOLDER_PARENT_METADATA_VALUE);
                    tobeVirtualMetadata.add(mvRelated);
                } else {
                    boolean foundAtLeastOneValue = false;
                    for (String relatedItemMetadataField : relatedItemMetadataFields) {
                        List<MetadataValue> relatedItemMetadataValues = getMetadataValues(relatedItem,
                                relatedItemMetadataField);
                        for (MetadataValue relatedItemMetadataValue : relatedItemMetadataValues) {
                            String relatedValue = relatedItemMetadataValue.getValue();
                            String relatedAuthority = relatedItemMetadataValue.getAuthority();
                            if (StringUtils.contains(relatedValue, "::")) {
                                String[] related = relatedValue.split("::");
                                relatedValue = related[0];
                                relatedAuthority = related[1];
                            }
                            MetadataValueDTO mvRelated = new MetadataValueDTO();
                            mvRelated.setSchema(VIRTUAL_METADATA_SCHEMA);
                            mvRelated.setElement(VIRTUAL_METADATA_ELEMENT);
                            mvRelated.setQualifier(getVirtualQualifier());
                            mvRelated.setValue(relatedValue);
                            if (StringUtils.isNotBlank(relatedAuthority)) {
                                mvRelated.setAuthority(relatedAuthority);
                                mvRelated.setConfidence(Choices.CF_ACCEPTED);
                            }
                            tobeVirtualMetadata.add(mvRelated);
                            foundAtLeastOneValue = true;
                        }
                    }
                    if (!foundAtLeastOneValue) {
                        // check if the parent is valid
                        boolean isRelatedValid = validate(context, relatedItem);
                        if (isRelatedValid) {
                            MetadataValueDTO mvRelated = new MetadataValueDTO();
                            mvRelated.setSchema(VIRTUAL_METADATA_SCHEMA);
                            mvRelated.setElement(VIRTUAL_METADATA_ELEMENT);
                            mvRelated.setQualifier(getVirtualQualifier());
                            mvRelated.setValue(relatedItem.getName() + "::" + relatedItem.getID());
                            tobeVirtualMetadata.add(mvRelated);
                            tobeVirtualMetadataMap.put(authority, tobeVirtualMetadata);
                        } else {
                            MetadataValueDTO mvRelated = new MetadataValueDTO();
                            mvRelated.setSchema(VIRTUAL_METADATA_SCHEMA);
                            mvRelated.setElement(VIRTUAL_METADATA_ELEMENT);
                            mvRelated.setQualifier(getVirtualQualifier());
                            mvRelated.setValue(PLACEHOLDER_PARENT_METADATA_VALUE);
                            tobeVirtualMetadata.add(mvRelated);
                        }
                    }
                }
            }
            tobeVirtualMetadataMap.put(authority, tobeVirtualMetadata);
        }
        return tobeVirtualMetadataMap;
    }

    @Override
    public boolean enhance(Context context, Item item, boolean deepMode) {
        boolean result = false;
        if (!deepMode) {
            try {
                result = cleanObsoleteVirtualFields(context, item);
                result = performEnhancement(context, item) || result;
            } catch (SQLException e) {
                LOGGER.error("An error occurs enhancing item with id {}: {}", item.getID(), e.getMessage(), e);
                throw new SQLRuntimeException(e);
            }
        } else {
            Map<String, List<MetadataValue>> currMetadataValues = relatedEntityItemEnhancerUtils
                    .getCurrentVirtualsMap(item, getVirtualQualifier());
            Map<String, List<MetadataValueDTO>> toBeMetadataValues = getToBeVirtualMetadata(context, item);
            if (!equivalent(currMetadataValues, toBeMetadataValues)) {
                try {
                    clearAllVirtualMetadata(context, item);
                    addMetadata(context, item, toBeMetadataValues);
                } catch (SQLException e) {
                    throw new SQLRuntimeException(e);
                }
                result = true;
            }
        }
        return result;
    }
    protected boolean performEnhancement(Context context, Item item) throws SQLException {
        boolean result = false;
        Map<String, List<MetadataValue>> currentVirtualsMap = relatedEntityItemEnhancerUtils
                .getCurrentVirtualsMap(item, getVirtualQualifier());
        Set<String> virtualSources = getVirtualSources(item);
        for (String authority : virtualSources) {
            boolean foundAtLeastOne = false;
            if (!currentVirtualsMap.containsKey(authority)) {
                result = true;

                boolean isValid = validate(context, item);
                if (isValid) {
                    addVirtualField(context, item, item.getName() + "::" + item.getID(),
                            null, null, Choices.CF_UNSET);
                    addVirtualSourceField(context, item, authority);
                } else {
                    Item relatedItem = findRelatedEntityItem(context, authority);
                    if (relatedItem == null) {
                        addVirtualField(context, item, PLACEHOLDER_PARENT_METADATA_VALUE, null, null, Choices.CF_UNSET);
                        addVirtualSourceField(context, item, authority);
                        continue;
                    }

                    for (String relatedItemMetadataField : relatedItemMetadataFields) {
                        List<MetadataValue> relatedItemMetadataValues = getMetadataValues(relatedItem,
                                relatedItemMetadataField);
                        for (MetadataValue relatedItemMetadataValue : relatedItemMetadataValues) {
                            foundAtLeastOne = true;
                            String relatedValue = relatedItemMetadataValue.getValue();
                            String relatedAuthority = relatedItemMetadataValue.getAuthority();
                            int relatedConfidence = relatedItemMetadataValue.getConfidence();
                            if (StringUtils.contains(relatedValue, "::")) {
                                String[] related = relatedValue.split("::");
                                relatedValue = related[0];
                                relatedAuthority = related[1];
                                relatedConfidence = Choices.CF_ACCEPTED;
                            }
                            addVirtualField(context, item, relatedValue,
                                    relatedAuthority, relatedItemMetadataValue.getLanguage(),
                                    relatedConfidence);
                            addVirtualSourceField(context, item, authority);
                        }
                    }
                    if (!foundAtLeastOne) {
                        // check if the parent is valid
                        boolean isRelatedValid = validate(context, relatedItem);
                        if (isRelatedValid) {
                            addVirtualField(context, item, relatedItem.getName() + "::" + relatedItem.getID(),
                                    null, null, -1);
                            addVirtualSourceField(context, item, authority);
                        } else {
                            addVirtualField(context, item, PLACEHOLDER_PARENT_METADATA_VALUE,
                                    null, null, Choices.CF_UNSET);
                            addVirtualSourceField(context, item, authority);
                        }
                        continue;
                    }
                }
            }
        }
        return result;
    }

    private boolean validate(Context context, Item item) {
        try {
            return filter.getResult(context, item);
        } catch (LogicalStatementException e) {
            throw new RuntimeException(e);
        }
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

}
