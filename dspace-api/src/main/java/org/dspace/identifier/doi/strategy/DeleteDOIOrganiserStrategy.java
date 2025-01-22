/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.strategy;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.dspace.core.Context;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.doi.action.DeleteDOIAction;
import org.dspace.identifier.doi.iterator.DOIIteratorFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class DeleteDOIOrganiserStrategy extends DOIOrganiserStrategyImpl {

    DeleteDOIOrganiserStrategy(
        @Autowired DeleteDOIAction deleteAction,
        @Autowired Context context, int offset, int limit
    ) {
        super(
            deleteAction,
            DOIIteratorFactory
                .instance()
                .queueByStatus(context, List.of(DOIIdentifierProvider.TO_BE_DELETED), offset, limit)
        );
        log = LogManager.getLogger(DeleteDOIOrganiserStrategy.class);
    }

}
