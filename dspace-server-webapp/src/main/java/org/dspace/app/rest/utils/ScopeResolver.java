/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.discovery.indexobject.IndexableItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Utility bean that can resolve a scope in the REST API to a DSpace Object
 */
@Component
public class ScopeResolver {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ScopeResolver.class);

    @Autowired
    CollectionService collectionService;

    @Autowired
    CommunityService communityService;

    @Autowired
    ItemService itemService;

    /**
     * Returns an IndexableObject corresponding to the community or collection
     * of the given scope, or null if the scope is not a valid UUID, or is a
     * valid UUID that does not correspond to a community of collection.
     *
     * @param context the DSpace context
     * @param scope a String containing the UUID of the community or collection
     * to return.
     * @return an IndexableObject corresponding to the community or collection
     * of the given scope, or null if the scope is not a valid UUID, or is a
     * valid UUID that does not correspond to a community of collection.
     */
    public IndexableObject resolveScope(Context context, String scope) {
        IndexableObject scopeObj = null;
        Optional<UUID> uuidOptional =
            Optional.ofNullable(scope)
                    .filter(StringUtils::isNotBlank)
                    .map(this::asUUID);
        return uuidOptional
            .flatMap(uuid -> resolveWithIndexedObject(context, Optional.of(uuid), communityService))
            .or(() -> resolveWithIndexedObject(context, uuidOptional, collectionService))
            .orElseGet(() -> {
                log.warn(
                    "The given scope string " +
                        StringUtils.trimToEmpty(scope) +
                        " is not a collection or community UUID."
                );
                return uuidOptional.map(uuid -> resolve(context, uuid, itemService)).orElse(null);
            });
    }

    private UUID asUUID(String scope) {
        try {
            return UUID.fromString(scope);
        } catch (IllegalArgumentException ex) {
            log.warn("The given scope string " + StringUtils.trimToEmpty(scope) + " is not a UUID", ex);
        }
        return null;
    }

    private <T extends DSpaceObject> Optional<IndexableObject> resolveWithIndexedObject(
        Context context, Optional<UUID> uuidOptional, DSpaceObjectService<T> service
    ) {
        return uuidOptional.map(uuid -> resolve(context, uuid, service))
                           .filter(obj -> obj.getIndexedObject() != null);
    }

    public <T extends DSpaceObject> IndexableObject resolve(
        Context context, UUID uuid, DSpaceObjectService<T> service
    ) {
        if (uuid == null) {
            return null;
        }
        T dspaceObject = null;
        try {
            dspaceObject = service.find(context, uuid);
        } catch (IllegalArgumentException ex) {
            log.warn("The given scope string " + StringUtils.trimToEmpty(uuid.toString()) + " is not a UUID", ex);
        } catch (SQLException ex) {
            log.warn(
                "Unable to retrieve DSpace Object with ID " + StringUtils.trimToEmpty(uuid.toString()) +
                    " from the database",
                ex);
        }
        if (dspaceObject == null) {
            return null;
        }
        if (dspaceObject instanceof Community) {
            return new IndexableCommunity((Community) dspaceObject);
        }
        if (dspaceObject instanceof Collection) {
            return new IndexableCollection((Collection) dspaceObject);
        }
        return new IndexableItem((Item) dspaceObject);
    }

}
