/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;

/**
 * The AssociateItemMode REST Resource
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 */
public class AssociateItemModeRest extends BaseObjectRest<String> {

    private static final long serialVersionUID = -3615146164199721822L;
    public static final String NAME = "associateitemmode";
    public static final String NAMESHORT = "associateitem";
    public static final String CATEGORY = RestAddressableModel.CORE;

    private String name;
    private String label;
    private Integer security;
    private String discovery;
    private String metadatafield;

    /* (non-Javadoc)
     * @see org.dspace.app.rest.model.RestModel#getType()
     */
    @Override
    public String getType() {
        return NAME;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getSecurity() {
        return security;
    }

    public void setSecurity(Integer security) {
        this.security = security;
    }

    public String getMetadatafield() {
        return metadatafield;
    }

    public void setMetadatafield(String metadatafield) {
        this.metadatafield = metadatafield;
    }

    public String getDiscovery() {
        return discovery;
    }

    public void setDiscovery(String discovery) {
        this.discovery = discovery;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.model.RestAddressableModel#getCategory()
     */
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.model.RestAddressableModel#getController()
     */
    @Override
    public Class<RestResourceController> getController() {
        return RestResourceController.class;
    }

}
