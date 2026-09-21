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
import org.dspace.core.Constants;
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
     * Returns an IndexableObject corresponding to the community, collection, or item
     * of the given scope, or null if the scope is not a valid UUID, or is a
     * valid UUID that does not correspond to a community, collection, or item.
     *
     * @param context the DSpace context
     * @param scope a String containing the UUID of the DSpace object to return
     * @return an IndexableObject corresponding to the community, collection, or item
     * of the given scope, or null if the scope is not a valid UUID, or is a
     * valid UUID that does not correspond to a community, collection, or item.
     */
    public IndexableObject resolveScope(Context context, String scope) {
        Optional<UUID> uuidOptional =
            Optional.ofNullable(scope)
                    .filter(StringUtils::isNotBlank)
                    .map(this::asUUID);
        return resolveScope(context, uuidOptional).orElse(null);
    }

    /**
     * Method that resolves a scope with a given level.
     * It can resolve the scope for the following type of entities (level):
     * <ul>
     *     <li>{@link Constants#ITEM}</li>
     *     <li>{@link Constants#COLLECTION}</li>
     *     <li>{@link Constants#COMMUNITY}</li>
     * </ul>
     * returns an {@link Optional} that contains the resolved {@link IndexableObject}.
     * If not specified resolves the scope by using all the levels, you can refer to the other public method:
     * {@link ScopeResolver#resolveScope(Context, Optional)}.
     *
     * @param context
     * @param scope
     * @param level
     * @return
     */
    public Optional<IndexableObject> resolveScope(Context context, String scope, int level) {
        Optional<UUID> uuidOptional =
            Optional.ofNullable(scope)
                    .filter(StringUtils::isNotBlank)
                    .map(this::asUUID);

        switch (level) {
            case Constants.ITEM:
                return uuidOptional.map(uuid -> resolveWithIndexedObject(context, uuid, itemService));
            case Constants.COLLECTION:
                return uuidOptional.map(uuid -> resolveWithIndexedObject(context, uuid, collectionService));
            case Constants.COMMUNITY:
                return uuidOptional.map(uuid -> resolveWithIndexedObject(context, uuid, communityService));
            default:
                return resolveScope(context, uuidOptional);
        }
    }

    private Optional<IndexableObject> resolveScope(Context context, Optional<UUID> uuidOptional) {
        return uuidOptional
            .map(uuid -> resolveWithIndexedObject(context, uuid, communityService))
            .or(() -> uuidOptional.map(uuid -> resolveWithIndexedObject(context, uuid, collectionService)))
            .or(() -> uuidOptional.map(uuid -> resolveWithIndexedObject(context, uuid, itemService)));
    }

    /**
     * Attempts to convert a string to a UUID
     *
     * @param scope the string to convert
     * @return the UUID or null if conversion fails
     */
    private UUID asUUID(String scope) {
        try {
            return UUID.fromString(scope);
        } catch (IllegalArgumentException ex) {
            log.warn("The given scope string {} is not a valid UUID", StringUtils.trimToEmpty(scope));
            return null;
        }
    }

    private <T extends DSpaceObject> IndexableObject resolveWithIndexedObject(
        Context context, UUID uuid, DSpaceObjectService<T> service
    ) {
        IndexableObject resolved = this.resolve(context, uuid, service);
        if (resolved  == null || resolved.getIndexedObject() == null) {
            return null;
        }
        return resolved;
    }

    /**
     * Resolves a UUID to a DSpaceObject and wraps it in the appropriate IndexableObject
     *
     * @param context the DSpace context
     * @param uuid the UUID to resolve
     * @param service the service to use for resolution
     * @param <T> the type of DSpaceObject
     * @return the IndexableObject or null if not found
     */
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
            if (log.isDebugEnabled()) {
                log.debug("Invalid UUID format: {}", StringUtils.trimToEmpty(uuid.toString()), ex);
            }
        } catch (SQLException ex) {
            if (log.isDebugEnabled()) {
                log.debug("Error retrieving object with ID {}", StringUtils.trimToEmpty(uuid.toString()), ex);
            }
        }

        if (dspaceObject == null) {
            return null;
        }

        // Create the appropriate IndexableObject based on the type of DSpaceObject
        if (dspaceObject instanceof Community) {
            return new IndexableCommunity((Community) dspaceObject);
        } else if (dspaceObject instanceof Collection) {
            return new IndexableCollection((Collection) dspaceObject);
        } else if (dspaceObject instanceof Item) {
            return new IndexableItem((Item) dspaceObject);
        }

        // This should not happen if all DSpaceObject types are handled above.
        log.warn("Unhandled DSpaceObject type: {}", dspaceObject.getClass().getName());
        return null;
    }
}
