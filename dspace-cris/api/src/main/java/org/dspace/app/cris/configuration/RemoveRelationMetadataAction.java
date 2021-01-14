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

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

import it.cilea.osd.jdyna.model.Property;

public class RemoveRelationMetadataAction extends RelationMetadataAction {

    public boolean processSelectedItem(Context context, DSpaceObject target, DSpaceObject selected) throws SQLException, AuthorizeException {
        if (target instanceof ACrisObject && selected instanceof Item) {
            Item item = (Item) selected;
            String crisID = ((ACrisObject)target).getCrisID();
            ChoiceAuthorityManager cam = ChoiceAuthorityManager.getManager(context);
            String[] metadata = metadataAction.split("\\.");
            Metadatum[] original = item.getMetadataByMetadataString(metadataAction);
            String schema = metadata[0];
            String element = metadata[1];
            String qualifier = metadata.length > 2 ? metadata[2] : null;
            item.clearMetadata(schema, element, qualifier, Item.ANY);
            for (Metadatum md : original) {
                if (!crisID.equals(md.authority)) {
                    item.addMetadata(md.schema, md.element, md.qualifier,
                            md.language, md.value, md.authority, md.confidence);
                }
            }
            item.update();

            return true;
        }
        else if (target instanceof ACrisObject && selected instanceof ACrisObject) {
            ACrisObject crisSelected = (ACrisObject) selected;

            List<Property> properties = (List<Property>)(crisSelected).getAnagrafica4view().get(metadataAction);
            for (Property property : properties) {
                if (((ACrisObject)property.getValue().getObject()).getCrisID().equals(((ACrisObject) target).getCrisID())) {
                    crisSelected.removeProprieta(property);
                }
            }

            new DSpace().getServiceManager()
                    .getServiceByName(
                            "applicationService",
                            ApplicationService.class)
                    .saveOrUpdate(
                            crisSelected.getCRISTargetClass(),
                            crisSelected);

            return true;
        }

        return false;
    }

}
