/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.listener;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.external.provider.ExternalDataProvider;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public interface ExternalIdGenerator {

    public String generateExternalId(Context context, ExternalDataProvider provider, Item item, String metadata);

    default boolean support(ExternalDataProvider provider) {
        return true;
    }

}