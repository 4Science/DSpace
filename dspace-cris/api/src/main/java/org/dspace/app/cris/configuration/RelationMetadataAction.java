/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.configuration;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

public abstract class RelationMetadataAction {

    protected String metadataAction;

    public abstract boolean processSelectedItem(Context context, DSpaceObject target, DSpaceObject selected) throws SQLException, AuthorizeException;

    public String getMetadataAction() {
        return metadataAction;
    }

    public void setMetadataAction(String metadataAction) {
        this.metadataAction = metadataAction;
    }

}
