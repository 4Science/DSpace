/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.AssociateItemModeRest;
import org.dspace.content.Item;
import org.dspace.content.edit.AssociateItemMode;
import org.dspace.content.edit.service.AssociateItemModeService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 *
 */
@Component(AssociateItemModeRest.CATEGORY + "." + AssociateItemModeRest.NAME)
public class AssociateItemModeRestRepository
    extends DSpaceRestRepository<AssociateItemModeRest, String> {

    @Autowired
    private AssociateItemModeService aimService;

    @Autowired
    ItemService itemService;

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#findOne(org.dspace.core.Context, java.io.Serializable)
     */
    @Override
    @PreAuthorize("permitAll")
    public AssociateItemModeRest findOne(Context context, String data) {
        AssociateItemMode mode = null;
        String uuid = null;
        String modeName = null;
        String[] values = data.split(":");
        if (values != null && values.length == 2) {
            uuid = values[0];
            modeName = values[1];
        } else {
            throw new DSpaceBadRequestException(
                    "Given parameters are incomplete. Expected <UUID-ITEM>:<MODE>, Received: " + data);
        }
        try {
            UUID itemUuid = UUID.fromString(uuid);
            Item item = itemService.find(context, itemUuid);
            if (item == null) {
                throw new ResourceNotFoundException("No such item with uuid : " + itemUuid);
            }
            mode = aimService.findMode(context, item, modeName);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (mode == null) {
            return null;
        }
        return converter.toRest(mode, utils.obtainProjection());
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#
     * findAll(org.dspace.core.Context, org.springframework.data.domain.Pageable)
     */
    @Override
    public Page<AssociateItemModeRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not Implemented!", "");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#getDomainClass()
     */
    @Override
    public Class<AssociateItemModeRest> getDomainClass() {
        return AssociateItemModeRest.class;
    }

    @PreAuthorize("permitAll")
    @SearchRestMethod(name = "findModesById")
    public Page<AssociateItemMode> findModesById(@Parameter(value = "uuid", required = true) UUID id,
                                                 Pageable pageable) {
        Context context = obtainContext();
        List<AssociateItemMode> modes = null;
        try {
            modes = aimService.findModes(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (modes == null) {
            return null;
        }
        return converter.toRestPage(modes, pageable, modes.size(), utils.obtainProjection());
    }

}
