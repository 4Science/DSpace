/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;

import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * The curate item feature. It can be used to verify if items can be curated.
 *
 * Authorization is granted if the current user is the item's admin.
 */
@Component
@AuthorizationFeatureDocumentation(name = CanCurateItemFeature.NAME,
    description = "It can be used to verify if curation tasks can be queued by user")
public class CanCurateItemFeature implements AuthorizationFeature {

    public final static String NAME = "canCurateItem";

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private Utils utils;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof ItemRest) {

            if (configurationService.getBooleanProperty("curate-item.authorization.admin.curate", true)) {
                return authorizeService.isAdmin(context,
                    (DSpaceObject)utils.getDSpaceAPIObjectFromRest(context, object));
            }
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            ItemRest.CATEGORY + "." + ItemRest.NAME
        };
    }
}
