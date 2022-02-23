/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.service.impl;

import static org.dspace.content.Item.ANY;
import static org.dspace.content.enhancer.ItemEnhancer.VIRTUAL_METADATA_ELEMENT;
import static org.dspace.content.enhancer.ItemEnhancer.VIRTUAL_METADATA_SCHEMA;
import static org.dspace.content.enhancer.ItemEnhancer.VIRTUAL_SOURCE_METADATA_ELEMENT;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.collections4.ListUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.enhancer.ItemEnhancer;
import org.dspace.content.enhancer.service.ItemEnhancerService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ItemEnhancerService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ItemEnhancerServiceImpl implements ItemEnhancerService {

    @Autowired
    private List<ItemEnhancer> itemEnhancers;

    @Autowired
    private ItemService itemService;

    @Override
    public void enhance(Context context, Item item) {

        itemEnhancers.stream()
            .filter(itemEnhancer -> itemEnhancer.canEnhance(context, item))
            .forEach(itemEnhancer -> itemEnhancer.enhance(context, item));

        updateItem(context, item);

    }

    @Override
    public void forceEnhancement(Context context, Item item) {
        cleanUpVirtualFields(context, item);
        enhance(context, item);
    }

    private void cleanUpVirtualFields(Context context, Item item) {

        List<MetadataValue> virtualFields = getVirtualFields(item);
        List<MetadataValue> virtualSourceFields = getVirtualSourceFields(item);
        List<MetadataValue> metadataValuesToRemove = ListUtils.union(virtualFields, virtualSourceFields);

        if (metadataValuesToRemove.isEmpty()) {
            return;
        }

        try {
            itemService.removeMetadataValues(context, item, ListUtils.union(virtualFields, virtualSourceFields));
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }

    }

    private void updateItem(Context context, Item item) {
        try {
            itemService.update(context, item);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

    private List<MetadataValue> getVirtualFields(Item item) {
        return itemService.getMetadata(item, VIRTUAL_METADATA_SCHEMA, VIRTUAL_METADATA_ELEMENT, ANY, ANY);
    }

    private List<MetadataValue> getVirtualSourceFields(Item item) {
        return itemService.getMetadata(item, VIRTUAL_METADATA_SCHEMA, VIRTUAL_SOURCE_METADATA_ELEMENT, ANY, ANY);
    }

    public List<ItemEnhancer> getItemEnhancers() {
        return itemEnhancers;
    }

    public void setItemEnhancers(List<ItemEnhancer> itemEnhancers) {
        this.itemEnhancers = itemEnhancers;
    }

    public ItemService getItemService() {
        return itemService;
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

}
