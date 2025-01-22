/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.strategy;

import java.util.List;

import org.dspace.core.Context;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.doi.action.UpdateDOIAction;
import org.dspace.identifier.doi.iterator.DOIIteratorFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
class UpdateDOIOrganiserStrategy extends DOIOrganiserStrategyImpl {

    public static final List<Integer> UPDATE_STATUS = List.of(
        DOIIdentifierProvider.UPDATE_BEFORE_REGISTRATION,
        DOIIdentifierProvider.UPDATE_RESERVED,
        DOIIdentifierProvider.UPDATE_REGISTERED
    );

    UpdateDOIOrganiserStrategy(
        @Autowired UpdateDOIAction action,
        @Autowired Context context, int offset, int limit
    ) {
        super(
            action,
            DOIIteratorFactory
                .instance()
                .queueByStatus(
                    context, UPDATE_STATUS, offset, limit
                )
        );
    }

}
