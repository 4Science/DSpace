/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit.service;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.exception.ResourceAlreadyExistsException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;

/**
 * Service interface class for the AssociateItem object.
 * The implementation of this class is responsible for all
 * business logic calls for the AssociateItem object and is autowired
 * by spring
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 *
 *
 */
public interface AssociateItemService {

    ItemService getItemService();

    /**
     * Create some metadata
     * @param context
     * @param sourceID the item where the metadata is created
     * @param targetID the item where the metadata points to
     * @param mode specified mode with the security config and the metadatafield
     * @return
     * @throws SQLException
     * @throws AuthorizeException
     * @throws ResourceAlreadyExistsException when some metadata already exists
     * @throws IllegalArgumentException
     */
    boolean create(Context context, UUID sourceID, UUID targetID, String mode) throws SQLException,
        AuthorizeException, ResourceAlreadyExistsException, IllegalArgumentException;

    /**
     * Delete any metadata fields with authority between the item with sourceID and targetID
     * @param context
     * @param sourceID item containing the metadata
     * @param targetID item where the metadata points to
     * @param mode specified mode with the security config and the metadatafield
     * @return
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IllegalArgumentException
     */
    boolean delete(Context context, UUID sourceID, UUID targetID, String mode) throws SQLException,
        AuthorizeException, IllegalArgumentException;
}
