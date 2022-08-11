/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.CrisLayoutBoxConfigurationRest;
import org.dspace.app.rest.model.CrisLayoutHierarchicalConfigurationRest;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBoxTypes;
import org.dspace.layout.CrisLayoutHierarchicalVocabulary2Box;
import org.springframework.stereotype.Component;

/**
 * @author Jurgen Mamani
 */
@Component
public class CrisLayoutHierarchicalBoxConfigurator implements CrisLayoutBoxConfigurator {

    @Override
    public boolean support(CrisLayoutBox box) {
        return StringUtils.equals(box.getType(), CrisLayoutBoxTypes.HIERARCHY.name());
    }

    @Override
    public CrisLayoutBoxConfigurationRest getConfiguration(CrisLayoutBox box) {
        // Rest representation of the configuration
        CrisLayoutHierarchicalConfigurationRest rest = new CrisLayoutHierarchicalConfigurationRest();

        CrisLayoutHierarchicalVocabulary2Box hierarchicalVocabulary2Box =
            box.getHierarchicalVocabulary2Box();

        rest.setMaxColumns(box.getMaxColumns());
        rest.setMetadata(hierarchicalVocabulary2Box.getMetadataField().toString('.'));
        rest.setVocabulary(hierarchicalVocabulary2Box.getVocabulary());
        return rest;
    }

    @Override
    public void configure(Context context, CrisLayoutBox box, CrisLayoutBoxConfigurationRest rest) {
        // Nothing to do
    }
}
