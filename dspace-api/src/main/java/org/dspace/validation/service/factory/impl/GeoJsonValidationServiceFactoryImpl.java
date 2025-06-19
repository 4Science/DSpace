/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation.service.factory.impl;

import org.dspace.validation.service.GeoJsonValidationService;
import org.dspace.validation.service.factory.GeoJsonValidationServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class GeoJsonValidationServiceFactoryImpl extends GeoJsonValidationServiceFactory {

    @Autowired
    private GeoJsonValidationService validationService;

    @Override
    public GeoJsonValidationService getValidationService() {
        return validationService;
    }
}
