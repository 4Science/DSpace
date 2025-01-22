/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.action;

import org.dspace.content.DSpaceObject;
import org.dspace.content.logic.Filter;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public abstract class DOIActionFactory {

    public static DOIActionFactory instance() {
        return DSpaceServicesFactory.getInstance()
                                    .getServiceManager()
                                    .getServicesByType(DOIActionFactory.class).get(0);
    }

    public abstract DeleteDOIAction createDeleteAction();

    public abstract UpdateDOIAction createUpdateAction();

    public abstract RegisterDOIAction createRegisterAction(Filter filter);

    public abstract ReserveDOIAction createReserveAction(Filter filter);

    public abstract <T extends DSpaceObject> EmailAlertDOIAction<T> createAlertEmailAction(T dso);
}
