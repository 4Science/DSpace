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
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choices;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.logic.Filter;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Extension of {@link RelatedEntityItemEnhancer} that creates {@link RelatedEntityItemEnhancer#virtualQualifier}
 * metadata
 * on the items that respects the {@link RelatedEntityItemEnhancerWithFilter#filter}, called <em>child</em>, and
 * propagates
 * it to all the other linked items connected by the {@link RelatedEntityItemEnhancer#sourceItemMetadataFields}, called
 * <em>grandchildren</em>.
 *
 * This class extends the enhancing algorithm of {@link RelatedEntityItemEnhancer} by implementing the followings:
 * <ol>
 *     <li>
 *         Retrieves all the virtual-source metadata (i.e. {@link RelatedEntityItemEnhancer#sourceItemMetadataFields})
 *     </li>
 *     <li>
 *         For each <em>source-metadata</em> performs the following:
 *     <ul>
 *         <li>
 *             Retrieves the linked item, called <em>related-item</em>
 *             <li>
 *                 If it doesn't exist creates adds a <em>virtual-metadata</em> with a
 *                 {@link org.dspace.core.CrisConstants#PLACEHOLDER_PARENT_METADATA_VALUE},
 *                 and a source with the actual current item (for future enhancing).
 *             </li>
 *             <li>
 *                 If founds any value inside the {@link RelatedEntityItemEnhancer#relatedItemMetadataFields} that will
 *                 be propagated inside the current item (it's a <em>granchild</em>)
 *             </li>
 *             <li>
 *                 Otherwise it's the <b>BASE CASE</b>, no relation has been created,
 *                 we need to create the <em>child</em> metadata.
 *             </li>
 *         </li>
 *     </ul>
 *     </li>
 * </ol>
 *
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

    private static RelatedMetadataValue mapToRelatedMetadataValue(
        MetadataValue relatedItemMetadataValue
    ) {
        String relatedValue = relatedItemMetadataValue.getValue();
        String relatedAuthority = relatedItemMetadataValue.getAuthority();
        int relatedConfidence = relatedItemMetadataValue.getConfidence();
        return new RelatedMetadataValue(
            relatedValue, relatedAuthority, relatedItemMetadataValue.getLanguage(), relatedConfidence
        );
    }


    @Override
    protected Map<String, List<MetadataValueDTO>> getToBeVirtualMetadata(Context context, Item item) {
        Map<String, List<MetadataValueDTO>> tobeVirtualMetadataMap = new HashMap<>();

        Set<String> virtualSources = getVirtualSources(item);
        for (String authority : virtualSources) {
            List<MetadataValueDTO> tobeVirtualMetadata = new ArrayList<>();

            boolean isValid = validate(context, item);
            if (isValid) {
                tobeVirtualMetadata.add(createVirtualMetadata(item));
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
                            tobeVirtualMetadata.add(createVirtualMetadata(relatedItem));
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

    private MetadataValueDTO createVirtualMetadata(Item item) {
        MetadataValueDTO mvRelated = new MetadataValueDTO();
        mvRelated.setSchema(VIRTUAL_METADATA_SCHEMA);
        mvRelated.setElement(VIRTUAL_METADATA_ELEMENT);
        mvRelated.setQualifier(getVirtualQualifier());
        mvRelated.setValue(item.getName());
        mvRelated.setAuthority(item.getID().toString());
        mvRelated.setConfidence(Choices.CF_ACCEPTED);
        return mvRelated;
    }

    @Override
    protected boolean performEnhancement(Context context, Item item) throws SQLException {
        boolean result = false;
        Map<String, List<MetadataValue>> currentVirtualsMap =
            relatedEntityItemEnhancerUtils.getCurrentVirtualsMap(item, getVirtualQualifier());
        Set<String> virtualSources = getVirtualSources(item);
        for (String sourceAuthority : virtualSources) {
            boolean propagated;

            if (currentVirtualsMap.containsKey(sourceAuthority)) {
                continue;
            }

            result = true;
            boolean isValid = validate(context, item);
            if (isValid) {
                addVirtualField(context, item, item.getName(), item.getID().toString(), null, Choices.CF_ACCEPTED);
                addVirtualSourceField(context, item, sourceAuthority);
                continue;
            }

            // adds parent placeholder, since the authority cannot be resolveZd!
            Item relatedItem = findRelatedEntityItem(context, sourceAuthority);
            if (relatedItem == null) {
                addVirtualField(context, item, PLACEHOLDER_PARENT_METADATA_VALUE, null, null, Choices.CF_UNSET);
                addVirtualSourceField(context, item, sourceAuthority);
            } else {

                // checks virtual-fields on relatedItem, and propagates them on the item,
                // propagates grandchildren's virtual-metadata to child
                propagated = propagateVirtualFromRelatedItem(context, item, relatedItem);

                // no virtual-fields propagated, checks if can enhance the item as child
                if (!propagated) {
                    createVirtualField(context, item, relatedItem, relatedItem.getID().toString());
                }
            }
        }
        return result;
    }

    private void createVirtualField(
        Context context, Item item, Item relatedItem, String sourceAuthority
    ) throws SQLException {
        // check if the related item is a valid root-enhancement (this is the child case)
        boolean isRelatedValid = validate(context, relatedItem);
        if (isRelatedValid) {
            addVirtualField(context, item, relatedItem.getName(), relatedItem.getID().toString(), null,
                            Choices.CF_ACCEPTED);
            addVirtualSourceField(context, item, sourceAuthority);
        } else {
            addVirtualField(context, item, PLACEHOLDER_PARENT_METADATA_VALUE, null, null, Choices.CF_UNSET);
            addVirtualSourceField(context, item, sourceAuthority);
        }
    }

    private boolean propagateVirtualFromRelatedItem(Context context, Item item, Item relatedItem) {
        boolean foundAtLeastOne = false;
        for (String relatedItemMetadataField : relatedItemMetadataFields) {
            List<RelatedMetadataValue> relatedItemMetadataValues =
                createRelatedItemMetadataValues(getMetadataValues(relatedItem, relatedItemMetadataField));

            relatedItemMetadataValues
                .forEach(rmv -> addVirtualMetadata(context, item, relatedItem.getID().toString(), rmv));

            foundAtLeastOne = foundAtLeastOne || !relatedItemMetadataValues.isEmpty();
        }
        return foundAtLeastOne;
    }

    private void addVirtualMetadata(Context context, Item item, String sourceAuthority, RelatedMetadataValue rmv) {
        try {
            addVirtualField(context, item, rmv.value, rmv.authority, rmv.language, rmv.confidence);
            addVirtualSourceField(context, item, sourceAuthority);
        } catch (SQLException e) {
            LOGGER.error(
                "Cannot add the virtual field to item {} retrieved from linked item {}",
                item.getID(), sourceAuthority, e
            );
            throw new RuntimeException(e);
        }
    }

    private List<RelatedMetadataValue> createRelatedItemMetadataValues(
        List<MetadataValue> relatedItemMetadataValues
    ) {
        return relatedItemMetadataValues.stream()
                                        .map(RelatedEntityItemEnhancerWithFilter::mapToRelatedMetadataValue)
                                        .collect(Collectors.toList());
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

    static final class RelatedMetadataValue {

        private final String value;
        private final String authority;
        private final int confidence;
        private final String language;

        private RelatedMetadataValue(
            String value, String authority, String language, int confidence
        ) {
            this.value = value;
            this.authority = authority;
            this.language = language;
            this.confidence = confidence;
        }
    }

}
