/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.service.impl;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.enhancer.ItemEnhancer;
import org.dspace.content.enhancer.service.ItemEnhancerService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
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
    public void enhance(Context context, Item item, boolean deepMode) {
        boolean isUpdateNeeded = false;

        for (ItemEnhancer itemEnhancer : itemEnhancers) {
            if (itemEnhancer.canEnhance(context, item)) {
                isUpdateNeeded = itemEnhancer.enhance(context, item, deepMode) || isUpdateNeeded;
            }
        }

        if (isUpdateNeeded) {
            updateItem(context, item);
        }
    }

    private void updateItem(Context context, Item item) {
        try {
            itemService.update(context, item);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
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
