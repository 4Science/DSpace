/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.action;

import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.logic.Filter;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
class DOIActionFactoryImpl extends DOIActionFactory {


    @Autowired
    ConfigurationService configurationService;

    @Autowired(required = false)
    DOIIdentifierProvider provider;

    @Override
    public DeleteDOIAction createDeleteAction() {
        return new DeleteDOIAction(provider);
    }

    @Override
    public UpdateDOIAction createUpdateAction() {
        return new UpdateDOIAction(provider);
    }

    @Override
    public RegisterDOIAction createRegisterAction(Filter filter) {
        return new RegisterDOIAction(provider, filter);
    }

    @Override
    public ReserveDOIAction createReserveAction(Filter filter) {
        return new ReserveDOIAction(provider, filter);
    }

    @Override
    public <T extends DSpaceObject> EmailAlertDOIAction<T> createAlertEmailAction(T dso) {
        return new EmailAlertDOIAction<>(
            configurationService,
            ContentServiceFactory.getInstance().getDSpaceObjectService(dso),
            dso
        );
    }

}
