/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.configuration;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 * This class is a service contained into the {@link RelationServiceConfiguration}
 * defined in the Spring configuration file:
 * cris-relationpreference.xml
 *
 */
public class RelationService {

    /** the relation configuration */
    private RelationConfiguration relationConfiguration;

    /** the discovery configuration key */
    private String discoveryConfigurationKey;

    /** the add action that will be performed */
    private RelationMetadataAction addAction;

    /** the remove action that will be performed */
    private RelationMetadataAction removeAction;

    /** list of implementations to check if the current user is authorized to perform the defined action */
    private List<SecurityCheck> security;

    public boolean isAuthorized(Context context, DSpaceObject dso) {
        for (SecurityCheck securityCheck : security) {
            if (securityCheck.isAuthorized(context, dso)) {
                return true;
            }
        }

        return false;
    }

    public boolean executeAction(Context context, String action, DSpaceObject target, DSpaceObject selected) throws SQLException, AuthorizeException
    {
        if (action.equals("add")) {
            return addAction.processSelectedItem(context, target, selected);
        }
        else {
            return removeAction.processSelectedItem(context, target, selected);
        }
    }

    public RelationConfiguration getRelationConfiguration() {
        return relationConfiguration;
    }

    public void setRelationConfiguration(RelationConfiguration relationConfiguration) {
        this.relationConfiguration = relationConfiguration;
    }

    public String getDiscoveryConfigurationKey() {
        return discoveryConfigurationKey;
    }

    public void setDiscoveryConfigurationKey(String discoveryConfigurationKey) {
        this.discoveryConfigurationKey = discoveryConfigurationKey;
    }

    public RelationMetadataAction getAddAction() {
        return addAction;
    }

    public void setAddAction(RelationMetadataAction addAction) {
        this.addAction = addAction;
    }

    public RelationMetadataAction getRemoveAction() {
        return removeAction;
    }

    public void setRemoveAction(RelationMetadataAction removeAction) {
        this.removeAction = removeAction;
    }

    public List<SecurityCheck> getSecurity() {
        return security;
    }

    public void setSecurity(List<SecurityCheck> security) {
        this.security = security;
    }

}
