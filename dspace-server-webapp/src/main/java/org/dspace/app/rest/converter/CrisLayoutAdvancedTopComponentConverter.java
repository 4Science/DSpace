/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.Objects;

import org.dspace.app.rest.model.CrisLayoutSectionRest;
import org.dspace.layout.CrisLayoutAdvancedTopComponent;
import org.dspace.layout.CrisLayoutSectionComponent;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link CrisLayoutSectionComponentConverter} for
 * {@link CrisLayoutAdvancedTopComponent}.
 *
 */
@Component
public class CrisLayoutAdvancedTopComponentConverter implements CrisLayoutSectionComponentConverter {

    @Override
    public boolean support(CrisLayoutSectionComponent component) {
        return component instanceof CrisLayoutAdvancedTopComponent;
    }

    @Override
    public CrisLayoutSectionRest.CrisLayoutAdvancedTopComponentRest convert(CrisLayoutSectionComponent component) {
        CrisLayoutAdvancedTopComponent topComponent = (CrisLayoutAdvancedTopComponent) component;
        CrisLayoutSectionRest.CrisLayoutAdvancedTopComponentRest rest = new CrisLayoutSectionRest
                .CrisLayoutAdvancedTopComponentRest();
        boolean showThumbnails =
                Objects.isNull(topComponent.getShowThumbnails()) ?
                        false : topComponent.getShowThumbnails();
        String template = Objects.isNull(topComponent.getTemplate()) ? "default" : topComponent.getTemplate().getName();

        rest.setDiscoveryConfigurationName(topComponent.getDiscoveryConfigurationName());
        rest.setOrder(topComponent.getOrder());
        rest.setSortField(topComponent.getSortField());
        rest.setStyle(component.getStyle());
        rest.setTitleKey(topComponent.getTitleKey());
        rest.setNumberOfItems(topComponent.getNumberOfItems());
        rest.setShowAsCard(topComponent.isShowAsCard());
        rest.setShowLayoutSwitch(topComponent.isShowLayoutSwitch());
        rest.setDefaultLayoutMode(topComponent.getDefaultLayoutMode().toString());
        rest.setCardColumnStyle(topComponent.getCardColumnStyle());
        rest.setCardStyle(topComponent.getCardStyle());
        rest.setItemListStyle(topComponent.getItemListStyle());
        rest.setShowAllResults(topComponent.isShowAllResults());
        rest.setShowThumbnails(showThumbnails);
        rest.setTemplate(template);
        return rest;
    }
}
