/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script.service.impl;

import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class CrisLayoutToolRenderSearchValidatorImpl extends CrisLayoutToolRenderValidatorImpl {

    @Autowired
    private DiscoveryConfigurationService searchConfigurationService;

    @Override
    protected boolean isSubTypeNotSupported(String renderType) {
        String[] subType = renderType.split("\\.");
        if (!isSubTypeFormatted(subType)) {
            return true;
        }

        if ("default".equals(subType[1]) && ("default".equals(subType[2]) || "auto".equals(subType[2]))) {
            return false;
        }

        DiscoveryConfiguration configuration = searchConfigurationService.getDiscoveryConfigurationByName(subType[1]);
        if (configuration == null) {
            return true;
        }

        return configuration.getSearchFilters()
                            .stream()
                            .noneMatch(d -> subType[2].startsWith(d.getIndexFieldName()));
    }

    private boolean isSubTypeFormatted(String[] subType) {
        return subType.length == 3;
    }
}
