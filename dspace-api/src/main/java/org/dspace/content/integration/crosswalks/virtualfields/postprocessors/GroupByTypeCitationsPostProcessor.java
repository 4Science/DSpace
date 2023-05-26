/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields.postprocessors;

import static org.apache.commons.lang3.StringUtils.capitalize;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.integration.crosswalks.csl.CSLResult;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.util.SimpleMapConverter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link VirtualFieldCitationsPostProcessor} that sort and
 * group the citations by item's type.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class GroupByTypeCitationsPostProcessor implements VirtualFieldCitationsPostProcessor {

    @Autowired
    private ItemService itemService;

    private boolean typeHeaderAdditionEnabled = true;

    private List<String> fixedTypesOrder;

    private SimpleMapConverter typeConverter;

    private String defaultType = "Other";

    @Override
    public CSLResult process(Context context, Item item, CSLResult cslResult) {

        if (typeHeaderAdditionEnabled && !cslResult.getFormat().equals("fo")) {
            throw new IllegalArgumentException("Only CSLResult related to fo format is supports type header addition");
        }

        String[] citationEntries = cslResult.getCitationEntries();
        UUID[] itemIds = cslResult.getItemIds();

        String[] newCitationEntries = new String[citationEntries.length];
        UUID[] newItemsIds = new UUID[itemIds.length];

        Item[] sortedItems = sortItemsByType(context, itemIds);

        String lastType = null;

        for (int i = 0; i < sortedItems.length; i++) {

            Item sortedItem = sortedItems[i];

            int originalItemIndex = ArrayUtils.indexOf(itemIds, sortedItem.getID());
            String citationEntry = citationEntries[originalItemIndex];

            String type = getItemTypeOrDefault(sortedItem);
            if (typeHeaderAdditionEnabled && !type.equalsIgnoreCase(lastType)) {
                citationEntry = addTypeHeaderToCitationEntry(citationEntry, type);
            }

            lastType = type;
            newCitationEntries[i] = citationEntry;
            newItemsIds[i] = sortedItem.getID();

        }

        return new CSLResult(cslResult.getFormat(), newItemsIds, newCitationEntries);
    }

    private String addTypeHeaderToCitationEntry(String citationEntry, String type) {
        return "<fo:block font-size=\"12pt\" font-weight=\"bold\" margin-top=\"4mm\" >"
            + capitalize(type.toLowerCase()) + "</fo:block>" + citationEntry;
    }

    @Override
    public String getName() {
        return "group-by-type";
    }

    private Item[] sortItemsByType(Context context, UUID[] itemIds) {
        return Arrays.stream(itemIds)
            .map(itemId -> findItemById(context, itemId))
            .sorted(this::compareItemsByType)
            .toArray(Item[]::new);
    }

    private int compareItemsByType(Item firstItem, Item secondItem) {

        String firstItemType = getItemType(firstItem);
        String secondItemType = getItemType(secondItem);

        if (anyOfTypesHasFixedOrder(firstItemType, secondItemType)) {
            return compareItemTypesWithFixedValues(firstItemType, secondItemType);
        }

        return ObjectUtils.compare(firstItemType, secondItemType, true);
    }

    private int compareItemTypesWithFixedValues(String firstItemType, String secondItemType) {

        int firstItemTypeIndex = firstItemType != null ? fixedTypesOrder.indexOf(firstItemType) : -1;
        int secondItemTypeIndex = secondItemType != null ? fixedTypesOrder.indexOf(secondItemType) : -1;

        // if the first type is not present in the fixed values and the second type is
        // present then the second type must be placed before the first one
        if (firstItemTypeIndex == -1 && secondItemTypeIndex != -1) {
            return 1;
        }

        // if the second type is not present in the fixed values and the first type is
        // present then the first type must be placed before the second one
        if (firstItemTypeIndex != -1 && secondItemTypeIndex == -1) {
            return -1;
        }

        // if both the types are present in the fixed values then sorting depends on
        // their position in the fixed types list
        return ObjectUtils.compare(firstItemTypeIndex, secondItemTypeIndex);

    }

    private boolean anyOfTypesHasFixedOrder(String firstItemType, String secondItemType) {
        return typeHasFixedOrder(firstItemType) || typeHasFixedOrder(secondItemType);
    }

    private boolean typeHasFixedOrder(String type) {
        if (StringUtils.isBlank(type) || CollectionUtils.isEmpty(fixedTypesOrder)) {
            return false;
        }
        return fixedTypesOrder.contains(type);
    }

    private Item findItemById(Context context, UUID id) {
        try {
            Item item = itemService.find(context, id);
            if (item == null) {
                throw new IllegalArgumentException("No item found by id " + id);
            }
            return item;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String getItemType(Item item) {
        String type = itemService.getMetadataFirstValue(item, "dc", "type", null, Item.ANY);
        return typeConverter != null ? typeConverter.getValue(type) : type;
    }

    private String getItemTypeOrDefault(Item item) {
        String type = getItemType(item);
        return StringUtils.isNotBlank(type) ? type : defaultType;
    }

    public ItemService getItemService() {
        return itemService;
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public String getDefaultType() {
        return defaultType;
    }

    public void setDefaultType(String defaultType) {
        this.defaultType = defaultType;
    }

    public List<String> getFixedTypesOrder() {
        return fixedTypesOrder;
    }

    public void setFixedTypesOrder(List<String> fixedTypesOrder) {
        this.fixedTypesOrder = fixedTypesOrder;
    }

    public SimpleMapConverter getTypeConverter() {
        return typeConverter;
    }

    public void setTypeConverter(SimpleMapConverter typeConverter) {
        this.typeConverter = typeConverter;
    }

    public boolean isTypeHeaderAdditionEnabled() {
        return typeHeaderAdditionEnabled;
    }

    public void setTypeHeaderAdditionEnabled(boolean addTypeHeader) {
        this.typeHeaderAdditionEnabled = addTypeHeader;
    }

}
