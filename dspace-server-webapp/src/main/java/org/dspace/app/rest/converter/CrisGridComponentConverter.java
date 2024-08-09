/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CrisLayoutSectionRest;
import org.dspace.layout.CrisGridComponent;
import org.dspace.layout.CrisLayoutSectionComponent;
import org.springframework.stereotype.Component;

/**
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
@Component
public class CrisGridComponentConverter implements CrisLayoutSectionComponentConverter {
    @Override
    public boolean support(CrisLayoutSectionComponent component) {
        return component instanceof CrisGridComponent;
    }

    @Override
    public CrisLayoutSectionRest.CrisLayoutSectionComponentRest convert(CrisLayoutSectionComponent component) {
        CrisGridComponent gridComponent = (CrisGridComponent) component;

        return new CrisLayoutSectionRest.CrisGridComponentRest(gridComponent.getDiscoveryConfigurationName(),
            gridComponent.getStyle(),
            gridComponent.getMainContentLink(),
            gridComponent.getSortField(),
            gridComponent.getOrder());
    }
}