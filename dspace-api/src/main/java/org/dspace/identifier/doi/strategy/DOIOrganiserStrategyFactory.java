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
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public abstract class DOIOrganiserStrategyFactory {

    public abstract DOIOrganiserStrategy deleteStrategy(Context context, int offset, int limit);
    public abstract DOIOrganiserStrategy deleteStrategy(DOI doi);

    public abstract DOIOrganiserStrategy registerStrategy(Context context, Filter filter, int offset, int limit);
    public abstract DOIOrganiserStrategy registerStrategy(DOI doi);

    public abstract DOIOrganiserStrategy reserveStrategy(Context context, Filter filter, int offset, int limit);
    public abstract DOIOrganiserStrategy reserveStrategy(DOI doi);

    public abstract DOIOrganiserStrategy updateStrategy(Context context, Filter filter, int offset, int limit);
    public abstract DOIOrganiserStrategy updateStrategy(DOI doi);

    public static DOIOrganiserStrategyFactory instance() {
        return DSpaceServicesFactory.getInstance()
                                    .getServiceManager()
                                    .getServicesByType(DOIOrganiserStrategyFactory.class)
                                    .get(0);
    }
}
