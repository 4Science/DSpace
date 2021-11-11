/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics;

import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Supplier;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class CrisItemMetricsAuthorizationServiceImpl implements CrisItemMetricsAuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(CrisItemMetricsAuthorizationServiceImpl.class);

    @Autowired
    protected AuthorizeService authorizeService;

    @Autowired
    protected ItemService itemService;

    @Override
    public boolean isAuthorized(Context context, UUID itemUuid) {
        return isAuthorized(context, new Supplier<Item>() {
            @Override
            public Item get() {
                Item item = null;
                try {
                    item = itemService.find(context, itemUuid);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
                if (item == null) {
                    throw new IllegalArgumentException();
                }
                return item;
            }
        });
    }

    @Override
    public boolean isAuthorized(Context context, Item item) {
        return isAuthorized(context, new Supplier<Item>() {
            @Override
            public Item get() {
                return item;
            }
        });
    }

    private boolean isAuthorized(Context context, Supplier<Item> itemSupplier) {

        // anonymous user
        if (context.getCurrentUser() == null) {
            return false;
        }

        try {

            return authorizeService.authorizeActionBoolean(context, itemSupplier.get(), Constants.READ);

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }


}
