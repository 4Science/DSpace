/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CrisLayoutSectionRest;
import org.dspace.app.rest.model.CrisLayoutSectionRest.CrisLayoutSectionComponentRest;
import org.dspace.layout.CrisLayoutSectionComponent;
import org.dspace.layout.CrisLayoutSliderComponent;
import org.springframework.stereotype.Component;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@Component
public class CrisLayoutSliderComponentConverter implements CrisLayoutSectionComponentConverter {

    @Override
    public boolean support(CrisLayoutSectionComponent component) {
        return component instanceof CrisLayoutSliderComponent;
    }

    @Override
    public CrisLayoutSectionComponentRest convert(CrisLayoutSectionComponent component) {
        return new CrisLayoutSectionRest.CrisLayoutSliderComponentRest(
            (CrisLayoutSliderComponent)component);
    }

}
