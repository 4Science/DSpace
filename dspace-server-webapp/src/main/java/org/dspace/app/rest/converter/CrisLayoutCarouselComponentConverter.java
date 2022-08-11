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
import org.dspace.layout.CrisLayoutCarouselComponent;
import org.dspace.layout.CrisLayoutSectionComponent;
import org.springframework.stereotype.Component;

/**
 * @author Stefano Maffei (steph-ieffam @ 4Science)
 *
 */
@Component
public class CrisLayoutCarouselComponentConverter implements CrisLayoutSectionComponentConverter {

    @Override
    public boolean support(CrisLayoutSectionComponent component) {
        return component instanceof CrisLayoutCarouselComponent;
    }

    @Override
    public CrisLayoutSectionComponentRest convert(CrisLayoutSectionComponent component) {
        return new CrisLayoutSectionRest.CrisLayoutCarouselComponentRest(
            (CrisLayoutCarouselComponent)component);
    }

}
