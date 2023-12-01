/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.filter;

import org.dspace.content.Item;
import org.dspace.content.logic.DefaultFilter;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.unpaywall.service.UnpaywallService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Filter to pass only items with Unpaywall api connection.
 */
public class UnpaywallApiFilter extends DefaultFilter {

    @Autowired
    private UnpaywallService unpaywallService;

    @Autowired
    private ItemService itemService;

    @Override
    public Boolean getResult(Context context, Item item) throws LogicalStatementException {
        String doi = itemService.getMetadataFirstValue(item, "dc", "identifier", "doi", Item.ANY);
        return unpaywallService.findUnpaywall(context, doi, item.getID()).isPresent();
    }
}
