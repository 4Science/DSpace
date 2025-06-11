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
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.model.WorkflowItemRest;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AuthorizationFeatureDocumentation(name = EditItemFeature.NAME,
    description = "It can be used to verify if a user has rights to edit any item.")
public class EditItemFeature implements AuthorizationFeature {
    public static final String NAME = "canEditItem";
    @Autowired
    AuthorizeService authService;
    @Autowired
    ItemService itemService;
    @Autowired
    WorkspaceItemService wis;
    @Autowired
    XmlWorkflowItemService wfis;

    @Autowired
    Utils utils;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException, SearchServiceException {
        if (object instanceof SiteRest) {
            return itemService.countItemsWithEdit(context) > 0;
        } else {
            Item item = null;

            if (object instanceof ItemRest) {
                item = (Item) utils.getDSpaceAPIObjectFromRest(context, object);
            } else if (object instanceof  WorkspaceItemRest) {
                WorkspaceItem wsi = wis.find(context, ((WorkspaceItemRest) object).getId());
                item = wsi != null ? wsi.getItem() : null;
            } else if (object instanceof  WorkflowItemRest) {
                XmlWorkflowItem wfi = wfis.find(context, ((WorkflowItemRest) object).getId());
                item = wfi != null ? wfi.getItem() : null;
            }

            return authService.authorizeActionBoolean(context, item, Constants.WRITE);
        }
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {
            ItemRest.CATEGORY + "." + ItemRest.NAME,
            SiteRest.CATEGORY + "." + SiteRest.NAME,
            WorkspaceItemRest.CATEGORY + "." + WorkspaceItemRest.NAME,
            WorkflowItemRest.CATEGORY + "." + WorkflowItemRest.NAME
        };
    }
}
