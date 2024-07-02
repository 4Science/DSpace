/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit.service.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dspace.content.Item;
import org.dspace.content.edit.AssociateItemMode;
import org.dspace.content.edit.service.AssociateItemModeService;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.content.security.service.CrisSecurityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the AssociateItemMode object.
 * This class is responsible for all business logic calls
 * for the Item object and is autowired by spring.
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 * @author Florian gantner (florian.gantner@uni-bamberg.de)
 */
public class AssociateItemModeServiceImpl implements AssociateItemModeService {

    @Autowired
    private ItemService itemService;
    @Autowired
    private CrisSecurityService crisSecurityService;

    private Map<String, List<AssociateItemMode>> associateModesMap;

    @Override
    public List<AssociateItemMode> findModes(Context context, Item item) throws SQLException {
        return findModes(context, item, true);
    }

    public List<AssociateItemMode> findModes(Context context, Item item, boolean checkSecurity) throws SQLException {

        if (context.getCurrentUser() == null) {
            return List.of();
        }

        List<AssociateItemMode> configuredModes = findAssociateItemModesByItem(item);

        if (!checkSecurity) {
            return configuredModes;
        }

        return configuredModes.stream()
            .filter(associateMode -> hasAccess(context, item, associateMode))
            .collect(Collectors.toList());
    }

    @Override
    public List<AssociateItemMode> findModes(Context context, UUID itemId) throws SQLException {
        return findModes(context, itemService.find(context, itemId));
    }

    @Override
    public AssociateItemMode findMode(Context context, Item item, String name) throws SQLException {
        List<AssociateItemMode> modes = findModes(context, item, false);
        return modes.stream()
                .filter(mode -> mode.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean canEdit(Context context, Item item) {

        if (context.getCurrentUser() == null) {
            return false;
        }

        return findAssociateItemModesByItem(item).stream()
            .anyMatch(associateMode -> hasAccess(context, item, associateMode));

    }

    private boolean hasAccess(Context context, Item item, AssociateItemMode accessitemmode) {
        try {
            // Check access condition on target if some condition on the item is fulfilled.
            if (Objects.nonNull(accessitemmode.getConditionTarget())) {
                try {
                    return crisSecurityService.hasAccess(context, item, context.getCurrentUser(), accessitemmode) &&
                        accessitemmode.getConditionTarget().getResult(context, item);
                } catch (LogicalStatementException s) {
                    // do nothing
                    throw new SQLException();
                }
            } else {
                return crisSecurityService.hasAccess(context, item, context.getCurrentUser(), accessitemmode);
            }
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private List<AssociateItemMode> findAssociateItemModesByItem(Item item) {

        List<AssociateItemMode> defaultModes = List.of();

        if (item == null) {
            return defaultModes;
        }

        String entityType = itemService.getEntityTypeLabel(item);
        if (isBlank(entityType)) {
            return defaultModes;
        }

        return getAssociateModesMap().getOrDefault(entityType.toLowerCase(), defaultModes);
    }

    public Map<String, List<AssociateItemMode>> getAssociateModesMap() {
        return associateModesMap;
    }

    public void setAssociateModesMap(Map<String, List<AssociateItemMode>> associateModesMap) {
        this.associateModesMap = associateModesMap;
    }
}
