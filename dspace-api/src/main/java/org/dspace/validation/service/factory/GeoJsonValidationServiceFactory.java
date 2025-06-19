/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation.service.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.validation.service.GeoJsonValidationService;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public abstract class GeoJsonValidationServiceFactory {

    public abstract GeoJsonValidationService getValidationService();

    public static GeoJsonValidationServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("geoJsonValidationServiceFactory",
                                                      GeoJsonValidationServiceFactory.class);
    }
}
