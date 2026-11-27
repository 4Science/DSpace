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
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * The view in workflow since feature. It can be used to verify if in workflow since can be viewed.
 */
@Component
@AuthorizationFeatureDocumentation(name = CanViewInWorkflowSinceFeature.NAME,
    description = "It can be used to verify if in workflow since can be viewed")
public class CanViewInWorkflowSinceFeature implements AuthorizationFeature {

    public final static String NAME = "canViewInWorkflowSinceStatistics";

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private Utils utils;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof ItemRest) {
            if (authorizeService.isAdmin(context)) {
                return true;
            }
            Item item = (Item) utils.getDSpaceAPIObjectFromRest(context, object);
            EPerson eperson = context.getCurrentUser();
            if (eperson != null) {
                return eperson.equals(item.getSubmitter());
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
