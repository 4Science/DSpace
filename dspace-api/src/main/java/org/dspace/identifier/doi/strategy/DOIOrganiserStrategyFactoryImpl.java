/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.strategy;

import org.dspace.content.logic.Filter;
import org.dspace.core.Context;
import org.dspace.identifier.DOI;
import org.dspace.identifier.doi.action.DOIActionFactory;
import org.dspace.identifier.doi.iterator.DOIIteratorFactory;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class DOIOrganiserStrategyFactoryImpl extends DOIOrganiserStrategyFactory {

    public DOIOrganiserStrategy deleteStrategy(Context context, int offset, int limit) {
        return new DeleteDOIOrganiserStrategy(
            DOIActionFactory.instance().createDeleteAction(), context, offset, limit
        );
    }

    public DOIOrganiserStrategy deleteStrategy(DOI doi) {
        return new DOIOrganiserStrategyImpl(
            DOIActionFactory.instance().createDeleteAction(),
            DOIIteratorFactory.instance().wrapper(doi)
        );
    }

    public DOIOrganiserStrategy registerStrategy(Context context, Filter filter, int offset, int limit) {
        return new RegisterDOIOrganiserStrategy(
            DOIActionFactory.instance().createRegisterAction(filter),
            context, offset, limit
        );
    }

    public DOIOrganiserStrategy registerStrategy(DOI doi) {
        return new DOIOrganiserStrategyImpl(
            DOIActionFactory.instance().createRegisterAction(null),
            DOIIteratorFactory.instance().wrapper(doi)
        );
    }


    public DOIOrganiserStrategy reserveStrategy(Context context, Filter filter, int offset, int limit) {
        return new ReserveDOIOrganiserStrategy(
            DOIActionFactory.instance().createReserveAction(filter), context, offset, limit
        );
    }

    public DOIOrganiserStrategy reserveStrategy(DOI doi) {
        return new DOIOrganiserStrategyImpl(
            DOIActionFactory.instance().createRegisterAction(null),
            DOIIteratorFactory.instance().wrapper(doi)
        );
    }

    public DOIOrganiserStrategy updateStrategy(Context context, Filter filter, int offset, int limit) {
        return new UpdateDOIOrganiserStrategy(
            DOIActionFactory.instance().createUpdateAction(), context, offset, limit
        );
    }

    public DOIOrganiserStrategy updateStrategy(DOI doi) {
        return new DOIOrganiserStrategyImpl(
            DOIActionFactory.instance().createUpdateAction(),
            DOIIteratorFactory.instance().wrapper(doi)
        );
    }

}
