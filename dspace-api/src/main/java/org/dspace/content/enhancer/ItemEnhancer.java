/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * The enhancers allow to add metadata to the item so that additional
 * information can be associated with it.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface ItemEnhancer {

    public static final String VIRTUAL_METADATA_SCHEMA = "dspace";
    public static final String VIRTUAL_METADATA_ELEMENT = "virtual";

    boolean canEnhance(Context context, Item item);

    boolean enhance(Context context, Item item, boolean deepMode);

}
