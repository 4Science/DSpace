/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.service;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service related to item enhancement.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface ItemEnhancerService {

    /**
     * Enhances the given item with all the item enhancers defined adding virtual
     * metadata fields on it.
     *
     * @param context the DSpace Context
     * @param item    the item to enhance
     * @param deepMode <code>false</code>, if the implementation can assume that only the target
     *        item as been updated since the eventual previous computation of enhanced metadata
     */
    void enhance(Context context, Item item, boolean deepMode);

}
