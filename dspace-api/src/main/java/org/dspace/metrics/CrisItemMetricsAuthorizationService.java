/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics;

import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service to evaluate authorization regarding Metrics visibility for a given Item.
 */
public interface CrisItemMetricsAuthorizationService {

    /**
     * Return whether the item's metrics are visible within the context.
     * @param context the context
     * @param itemUuid the item UUID
     * @return
     */
    boolean isAuthorized(Context context, UUID itemUuid);

    /**
     * Return whether the item's metrics are visible within the context.
     * @param context the context
     * @param item the item object
     * @return
     */
    boolean isAuthorized(Context context, Item item);

}
