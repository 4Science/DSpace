/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.dspace.app.exception.ResourceAlreadyExistsException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.edit.AssociateItem;
import org.dspace.content.edit.AssociateItemMode;
import org.dspace.content.edit.service.AssociateItemModeService;
import org.dspace.content.edit.service.AssociateItemService;
import org.dspace.content.security.service.CrisSecurityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the AssociateItem object.
 * This class is responsible for all business logic calls
 * for the Item object and is autowired by spring.
 * Mainly creating and removing connections (metadata with authority) between some item sourceID
 * and some target targetID as the authority value. The metadata field is being readed from the {@AssociateItemMode}
 * configuration.
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 *
 */
public class AssociateItemServiceImpl implements AssociateItemService {

    @Autowired(required = true)
    private ItemService itemService;

    @Autowired(required = true)
    private ItemDAO itemDAO;

    @Autowired(required = true)
    private AssociateItemModeService modeService;

    @Autowired(required = true)
    private MetadataFieldService metadatafieldService;

    @Autowired(required = true)
    private CrisSecurityService crisSecurityService;

    /* (non-Javadoc)
     * @see org.dspace.content.edit.service.EditItemService#getItemService()
     */
    @Override
    public ItemService getItemService() {
        return itemService;
    }

    private AssociateItem find(Context context, UUID sourceID, UUID targetID, String mode)
        throws SQLException, AuthorizeException {
        boolean hasAccess = false;
        Item targetItem = itemService.find(context, targetID);
        Item sourceItem = itemService.find(context, sourceID);

        AssociateItemMode associateItemMode = null;
        EPerson currentUser = context.getCurrentUser();
        //TODO:assume nonNull for accessmode and items
        if (currentUser == null) {
            throw new AuthorizeException();
        } else {

            associateItemMode = modeService.findMode(context, targetItem, mode);
            if (associateItemMode == null) {
                return null;
            }
            /*

            if (AssociateItemMode.NONE.equals(mode)) {
                return AssociateItemMode.none(context, target);
            }*/
        }
        return new AssociateItem(context, sourceItem, targetItem, associateItemMode);
    }

    private boolean checkConditions(Context context, AssociateItem associateItem)
        throws SQLException {

        if (associateItem.getMode().isdisableAuthSource()) {
            context.turnOffAuthorisationSystem();
        }
        //Check conditions on source and target items
        if (Objects.nonNull(associateItem.getMode().getConditionSource())) {
            if (associateItem.getMode().getConditionSource().getResult(context, associateItem.getSourceitem())
                == false) {
                return false;
            }
        }
        if (Objects.nonNull(associateItem.getMode().getItemTypeSource())) {
            if (!itemService.getEntityType(associateItem.getSourceitem())
                .contentEquals(associateItem.getMode().getItemTypeSource())) {
                return false;
            }
        }
        if (Objects.nonNull(associateItem.getMode().getItemTypeTarget())) {
            if (!itemService.getEntityType(associateItem.getTargetitem())
                .contentEquals(associateItem.getMode().getItemTypeTarget())) {
                return false;
            }
        }
        if (Objects.nonNull(associateItem.getMode().getConditionTarget())) {
            if (associateItem.getMode().getConditionTarget().getResult(context, associateItem.getTargetitem())) {
                return false;
            }
        }
        return crisSecurityService
            .hasAccess(context, associateItem.getTargetitem(), context.getCurrentUser(), associateItem.getMode());
    }

    @Override
    public boolean create(Context context, UUID sourceID, UUID targetID, String mode)
        throws SQLException, AuthorizeException, ResourceAlreadyExistsException, IllegalArgumentException {
        AssociateItem associateItem = this.find(context, sourceID, targetID, mode);
        if (associateItem == null) {
            throw new IllegalArgumentException("associateitem does not exist");
        }
        boolean hasAccess = checkConditions(context, associateItem);
        if (!hasAccess) {
            throw new AuthorizeException();
        }
        //cases: metadata already exist
        List<MetadataValue> mdvs = itemService.getMetadataByMetadataString(associateItem.getSourceitem(),
            associateItem.getMode().getMetadatafield());
        // check existing value, if some pointer to target exist
        if (Objects.nonNull(mdvs) && !mdvs.isEmpty()
            && mdvs.stream().anyMatch(mdv -> Objects.nonNull(mdv.getAuthority())
            && mdv.getAuthority().contentEquals(associateItem.getTargetitem().getID().toString()))) {
           // Value exists
            throw new ResourceAlreadyExistsException("metadata association already exist");
        }

        MetadataField mf =
            this.metadatafieldService.findByString(context, associateItem.getMode().getMetadatafield(), '.');
        if (mf == null) {
            throw new IllegalArgumentException("unknown Metadatafield");
        }

        itemService.addMetadata(context, associateItem.getSourceitem(), mf, null,
            associateItem.getTargetitem().getName(), associateItem.getTargetitem().getID().toString(), 600);
        itemService.update(context, associateItem.getSourceitem());
        if (context.ignoreAuthorization()) {
            context.restoreAuthSystemState();
        }
        return true;
    }

    @Override
    public boolean delete(Context context, UUID sourceID, UUID targetID, String mode)
        throws SQLException, AuthorizeException, IllegalArgumentException {
        AssociateItem associateItem = this.find(context, sourceID, targetID, mode);
        if (associateItem == null) {
            throw new IllegalArgumentException("specified AssociateItem does not exist");
        }
        boolean hasAccess = checkConditions(context, associateItem);
        if (!hasAccess) {
            throw new AuthorizeException();
        }
        List<MetadataValue> mdvs = itemService.getMetadataByMetadataString(associateItem.getSourceitem(),
            associateItem.getMode().getMetadatafield());
        if (Objects.isNull(mdvs) || mdvs.isEmpty()) {
            throw new IllegalArgumentException("metadatavalue not existed or already deleted");
        }
        List<MetadataValue> toremove = new ArrayList<>();
        for (MetadataValue mdv : mdvs) {
            if (mdv.getAuthority().contentEquals(associateItem.getTargetitem().getID().toString())) {
                toremove.add(mdv);
            }
        }
        if (toremove.isEmpty()) {
            throw new IllegalArgumentException(
                "no metadatavalue to targetitem found. perhaps metadatavalue not existed or deleted", null);
        }
        itemService.removeMetadataValues(context, associateItem.getSourceitem(), toremove);
        itemService.update(context, associateItem.getSourceitem());
        if (context.ignoreAuthorization()) {
            context.restoreAuthSystemState();
        }
        return true;
    }

}
