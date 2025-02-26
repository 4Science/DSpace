/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class AnnotationMapper {

    private final ItemService itemService;
    private final WorkspaceItemService workspaceItemService;
    private final Collection collection = null;

    AnnotationMapper(
        @Autowired ItemService itemService,
        @Autowired WorkspaceItemService workspaceItemService
    ) {
        this.itemService = itemService;
        this.workspaceItemService = workspaceItemService;
    }

    public Item map(Context context, AnnotationRest annotation) {
        //
        WorkspaceItem workspaceItem = null;
        try {
            workspaceItem =
                this.workspaceItemService.create(context, collection, false);
        } catch (AuthorizeException | SQLException e) {
            throw new RuntimeException(e);
        }
        return workspaceItem.getItem();
    }

}
