/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.AssociateItemModeRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.edit.AssociateItemMode;
import org.springframework.stereotype.Component;

/**
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 *
 */
@Component
public class AssociateItemModeConverter implements DSpaceConverter<AssociateItemMode, AssociateItemModeRest> {

    /* (non-Javadoc)
     * @see org.dspace.app.rest.converter.DSpaceConverter#
     * convert(java.lang.Object, org.dspace.app.rest.projection.Projection)
     */
    @Override
    public AssociateItemModeRest convert(AssociateItemMode model, Projection projection) {
        AssociateItemModeRest rest = new AssociateItemModeRest();
        rest.setId(model.getName());
        rest.setName(model.getName());
        rest.setLabel(model.getLabel());
        rest.setMetadatafield(model.getMetadatafield());
        rest.setDiscovery(model.getDiscovery());
        return rest;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.converter.DSpaceConverter#getModelClass()
     */
    @Override
    public Class<AssociateItemMode> getModelClass() {
        return AssociateItemMode.class;
    }

}
