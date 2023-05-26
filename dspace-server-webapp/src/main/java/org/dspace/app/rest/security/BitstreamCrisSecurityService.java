/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.edit.CorrectItemMode;
import org.dspace.content.security.AccessItemMode;
import org.dspace.content.security.service.CrisSecurityService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 */
@Component
public class BitstreamCrisSecurityService {
    @Autowired
    private CrisSecurityService crisSecurityService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    @Qualifier("bitstreamAccessModesMap")
    private Map<String, List<CorrectItemMode>> bitstreamAccessModesMap;

    @Autowired
    @Qualifier("allowedBundlesForBitstreamAccess")
    private Map<CorrectItemMode, List<String>> allowedBundles;

    public boolean isBitstreamAccessAllowedByCrisSecurity(Context context, EPerson ePerson, Bitstream bit)
            throws SQLException {
        DSpaceObject pdso = bitstreamService.getParentObject(context, bit);
        if (pdso != null && pdso instanceof Item) {
            Item item = (Item) pdso;
            String entityType = itemService.getEntityType(item);
            if (StringUtils.isNotEmpty(entityType)) {
                for (AccessItemMode accessMode : bitstreamAccessModesMap.getOrDefault(entityType,
                        Collections.emptyList())) {
                    if (accessMode != null &&
                        crisSecurityService.hasAccess(context, item, ePerson, accessMode) &&
                        isBundleNotRestricted(accessMode, bit)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isBundleNotRestricted(AccessItemMode accessItemMode, Bitstream bit) throws SQLException {
        if (allowedBundles.get(accessItemMode) == null) {
            // no restriction was specified
            return true;
        }
        List<Bundle> bitstreamBundles = bit.getBundles();
        return bitstreamBundles.stream().anyMatch(bundle -> {
            return allowedBundles.get(accessItemMode).contains(bundle.getName());
        });
    }
}
