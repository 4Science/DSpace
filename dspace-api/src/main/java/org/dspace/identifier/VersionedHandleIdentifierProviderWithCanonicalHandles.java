/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class VersionedHandleIdentifierProviderWithCanonicalHandles extends IdentifierProvider
    implements InitializingBean {
    /**
     * log4j category
     */
    private static final Logger log =
            org.apache.logging.log4j.LogManager.getLogger(VersionedHandleIdentifierProviderWithCanonicalHandles.class);

    /**
     * Prefix registered to no one
     */
    static final String EXAMPLE_PREFIX = "123456789";

    private static final char DOT = '.';

    @Autowired(required = true)
    private VersioningService versionService;

    @Autowired(required = true)
    private VersionHistoryService versionHistoryService;

    @Autowired(required = true)
    private HandleService handleService;

    /**
     * After all the properties are set check that the versioning is enabled
     *
     * @throws Exception throws an exception if this isn't the case
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (!configurationService.getBooleanProperty("versioning.enabled", true)) {
            throw new RuntimeException("the " + VersionedHandleIdentifierProviderWithCanonicalHandles.class.getName() +
                    " is enabled, but the versioning is disabled.");
        }
    }

    @Override
    public boolean supports(Class<? extends Identifier> identifier) {
        return Handle.class.isAssignableFrom(identifier);
    }

    @Override
    public boolean supports(String identifier) {
        return handleService.parseHandle(identifier) != null;
    }

    @Override
    public String register(Context context, DSpaceObject dso) {
        String id = mint(context, dso);

        // move canonical to point the latest version
        if (dso.getType() == Constants.ITEM && dso instanceof Item) {
            Item item = (Item) dso;
            VersionHistory history;
            try {
                history = versionHistoryService.findByItem(context, item);
            } catch (SQLException ex) {
                throw new RuntimeException("A problem with the database connection occured.", ex);
            }
            if (history != null) {
                String canonical = getCanonical(context, item);

                // Modify Canonical: 12345/100 will point to the new item
                try {
                    handleService.modifyHandleDSpaceObject(context, canonical, item);
                } catch (SQLException ex) {
                    throw new RuntimeException("A problem with the database connection occured.", ex);
                }

                Version version;
                Version previous;
                boolean previousIsFirstVersion = false;
                String previousItemHandle = null;
                try {
                    version = versionService.getVersion(context, item);
                    previous = versionHistoryService.getPrevious(context, history, version);
                    if (previous != null) {
                        previousIsFirstVersion = versionHistoryService.isFirstVersion(context, history, previous);
                        previousItemHandle = handleService.findHandle(context, previous.getItem());
                    }
                } catch (SQLException ex) {
                    throw new RuntimeException("A problem with the database connection occured.", ex);
                }

                // we have to ensure the previous item still has a handle
                // check if we have a previous item
                if (previous != null) {
                    try {
                        // If we have a reviewer they might not have the
                        // rights to edit the metadata of thes previous item.
                        // Temporarly grant them:
                        context.turnOffAuthorisationSystem();

                        // Check if our previous item hasn't got a handle anymore.
                        // This only occurs when a switch has been made from
                        // the default handle identifier provider to the
                        // versioned one. In this case no "versioned handle" is
                        // reserved so we need to create one.
                        if (previousItemHandle == null) {
                            if (previousIsFirstVersion) {
                                previousItemHandle = getCanonical(id) + DOT + previous.getVersionNumber();
                                handleService.createHandle(context, previous.getItem(), previousItemHandle);
                            } else {
                                previousItemHandle = makeIdentifierBasedOnHistory(context, previous.getItem(), history);
                            }
                        }
                        // remove the canonical handle from the previous item's metadata
                        modifyHandleMetadata(context, previous.getItem(), previousItemHandle);
                    } catch (SQLException ex) {
                        throw new RuntimeException("A problem with the database connection occured.", ex);
                    } catch (AuthorizeException ex) {
                        // cannot occure, as the authorization system is turned of
                        throw new IllegalStateException("Caught an "
                                                            + "AuthorizeException while the "
                                                            + "authorization system was turned off!", ex);
                    } finally {
                        context.restoreAuthSystemState();
                    }
                }
            }
            try {
                // remove all handles from metadata and add the canonical one.
                modifyHandleMetadata(context, item, getCanonical(id));
            } catch (SQLException ex) {
                throw new RuntimeException("A problem with the database connection occured.", ex);
            } catch (AuthorizeException ex) {
                throw new RuntimeException("The current user is not authorized to change this item.", ex);
            }
        }
        if (dso instanceof Collection || dso instanceof Community) {
            try {
                // Update the metadata with the handle for collections and communities.
                modifyHandleMetadata(context, dso, getCanonical(id));
            } catch (SQLException ex) {
                throw new RuntimeException("A problem with the database connection occured.", ex);
            } catch (AuthorizeException ex) {
                throw new RuntimeException("The current user is not authorized to change this item.", ex);
            }
        }

        return id;
    }

    @Override
    public void register(Context context, DSpaceObject dso, String identifier) {
        try {
            if (dso instanceof Item) {
                Item item = (Item) dso;
                // if this identifier is already present in the Handle table and the corresponding item
                // has a history, then someone is trying to restore the latest version for the item. When
                // trying to restore the latest version, the identifier in input doesn't have the
                // 1234/123.latestVersion. Instead, it is the canonical 1234/123
                VersionHistory itemHistory = getHistory(context, identifier);
                if (!identifier.matches(".*/.*\\.\\d+") && itemHistory != null) {

                    int newVersionNumber = versionHistoryService.getLatestVersion(context, itemHistory)
                                                                .getVersionNumber() + 1;
                    String canonical = identifier;
                    identifier = identifier.concat(".").concat("" + newVersionNumber);
                    restoreItAsVersion(context, dso, identifier, item, canonical, itemHistory);
                } else if (identifier.matches(".*/.*\\.\\d+")) {
                    // if identifier == 1234.5/100.4 reinstate the version 4 in the version table if absent

                    // if it is a version of an item is needed to put back the record
                    // in the versionitem table
                    String canonical = getCanonical(identifier);
                    DSpaceObject canonicalItem = this.resolve(context, canonical);
                    if (canonicalItem == null) {
                        restoreItAsCanonical(context, dso, identifier, item, canonical);
                    } else {
                        VersionHistory history = versionHistoryService.findByItem(context, (Item) canonicalItem);
                        if (history == null) {
                            restoreItAsCanonical(context, dso, identifier, item, canonical);
                        } else {
                            restoreItAsVersion(context, dso, identifier, item, canonical, history);

                        }
                    }
                } else {
                    // A regular handle to create for an Item
                    createNewIdentifier(context, dso, identifier);
                    modifyHandleMetadata(context, item, getCanonical(identifier));
                }
            } else {
                // Handle being registered for a different type of object (e.g. Collection or Community)
                createNewIdentifier(context, dso, identifier);
            }
        } catch (IOException | SQLException | AuthorizeException e) {
            log.error(LogHelper.getHeader(context,
                    "Error while attempting to create handle",
                    "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID(), e);
        }
    }

    protected VersionHistory getHistory(Context context, String identifier) throws SQLException {
        DSpaceObject item = this.resolve(context, identifier);
        if (item != null) {
            VersionHistory history = versionHistoryService.findByItem(context, (Item) item);
            return history;
        }
        return null;
    }

    protected void restoreItAsVersion(Context context, DSpaceObject dso, String identifier, Item item, String canonical,
                                      VersionHistory history)
        throws SQLException, IOException, AuthorizeException {
        createNewIdentifier(context, dso, identifier);
        modifyHandleMetadata(context, item, getCanonical(identifier));

        int versionNumber = Integer.parseInt(identifier.substring(identifier.lastIndexOf(".") + 1));
        versionService
            .createNewVersion(context, history, item, "Restoring from AIP Service", new Date(), versionNumber);
        Version latest = versionHistoryService.getLatestVersion(context, history);


        // if restoring the lastest version: needed to move the canonical
        if (latest.getVersionNumber() < versionNumber) {
            handleService.modifyHandleDSpaceObject(context, canonical, dso);
        }
    }

    protected void restoreItAsCanonical(Context context, DSpaceObject dso, String identifier, Item item,
                                        String canonical) throws SQLException, IOException, AuthorizeException {
        createNewIdentifier(context, dso, identifier);
        modifyHandleMetadata(context, item, getCanonical(identifier));

        int versionNumber = Integer.parseInt(identifier.substring(identifier.lastIndexOf(".") + 1));
        VersionHistory history = versionHistoryService.create(context);
        versionService
            .createNewVersion(context, history, item, "Restoring from AIP Service", new Date(), versionNumber);

        handleService.modifyHandleDSpaceObject(context, canonical, dso);

    }


    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier) {
        try {
            handleService.createHandle(context, dso, identifier);
        } catch (IllegalStateException | SQLException e) {
            log.error(LogHelper.getHeader(context,
                    "Error while attempting to create handle",
                    "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID());
        }
    }


    /**
     * Creates a new handle in the database.
     *
     * @param context DSpace context
     * @param dso     The DSpaceObject to create a handle for
     * @return The newly created handle
     */
    @Override
    public String mint(Context context, DSpaceObject dso) {
        if (dso.getHandle() != null) {
            return dso.getHandle();
        }

        try {
            String handleId = null;
            VersionHistory history = null;
            if (dso instanceof Item) {
                history = versionHistoryService.findByItem(context, (Item) dso);
            }

            if (history != null) {
                handleId = makeIdentifierBasedOnHistory(context, dso, history);
            } else {
                handleId = createNewIdentifier(context, dso, null);
            }
            return handleId;
        } catch (SQLException | AuthorizeException e) {
            log.error(LogHelper.getHeader(context,
                    "Error while attempting to create handle",
                    "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID());
        }
    }

    @Override
    public DSpaceObject resolve(Context context, String identifier, String... attributes) {
        // We can do nothing with this, return null
        try {
            identifier = handleService.parseHandle(identifier);
            return handleService.resolveToObject(context, identifier);
        } catch (IllegalStateException | SQLException e) {
            log.error(LogHelper.getHeader(context, "Error while resolving handle to item", "handle: " + identifier),
                      e);
        }
        return null;
    }

    @Override
    public String lookup(Context context, DSpaceObject dso)
        throws IdentifierNotFoundException, IdentifierNotResolvableException {

        try {
            return handleService.findHandle(context, dso);
        } catch (SQLException sqe) {
            throw new IdentifierNotResolvableException(sqe.getMessage(), sqe);
        }
    }

    @Override
    public void delete(Context context, DSpaceObject dso, String identifier) throws IdentifierException {
        delete(context, dso);
    }

    @Override
    public void delete(Context context, DSpaceObject dso) throws IdentifierException {

        try {
            if (dso instanceof Item) {
                Item item = (Item) dso;

                // If it is the most current version occurs to move the canonical to the previous version
                VersionHistory history = versionHistoryService.findByItem(context, item);
                if (history != null && versionHistoryService.getLatestVersion(context, history).getItem().equals(item)
                    && versionService.getVersionsByHistory(context, history).size() > 1) {
                    Item previous;
                    try {
                        previous = versionHistoryService
                            .getPrevious(context, history, versionHistoryService.getLatestVersion(context, history))
                            .getItem();
                    } catch (SQLException ex) {
                        throw new RuntimeException("A problem with our database connection occured.", ex);
                    }

                    // Modify Canonical: 12345/100 will point to the new item
                    String canonical = getCanonical(context, previous);
                    handleService.modifyHandleDSpaceObject(context, canonical, previous);
                }
            }
        } catch (RuntimeException | SQLException e) {
            log.error(LogHelper.getHeader(context,
                    "Error while attempting to register doi",
                    "Item id: " + dso.getID()), e);
            throw new IdentifierException("Error while moving doi identifier", e);
        }


    }

    public static String retrieveHandleOutOfUrl(String url) {
        // We can do nothing with this, return null
        if (!url.contains("/")) {
            return null;
        }

        String[] splitUrl = url.split("/");

        return splitUrl[splitUrl.length - 2] + "/" + splitUrl[splitUrl.length - 1];
    }

    /**
     * Get the configured Handle prefix string, or a default
     *
     * @return configured prefix or "123456789"
     */
    public static String getPrefix() {
        ConfigurationService configurationService
                = DSpaceServicesFactory.getInstance().getConfigurationService();
        String prefix = configurationService.getProperty("handle.prefix");
        if (null == prefix) {
            prefix = EXAMPLE_PREFIX; // XXX no good way to exit cleanly
            log.error("handle.prefix is not configured; using " + prefix);
        }
        return prefix;
    }

    protected String createNewIdentifier(Context context, DSpaceObject dso, String handleId) throws SQLException {
        if (handleId == null) {
            return handleService.createHandle(context, dso);
        } else {
            return handleService.createHandle(context, dso, handleId);
        }
    }

    protected String makeIdentifierBasedOnHistory(Context context, DSpaceObject dso, VersionHistory history)
        throws AuthorizeException, SQLException {
        Item item = (Item) dso;

        // FIRST time a VERSION is created 2 identifiers will be minted  and the canonical will be updated to point
        // to the newer URL:
        //  - id.1-->old URL
        //  - id.2-->new URL
        Version version = versionService.getVersion(context, item);
        Version previous;
        try {
            previous = versionHistoryService.getPrevious(context, history, version);
        } catch (SQLException ex) {
            throw new RuntimeException("A problem with our database connection occured.");
        }
        String canonical = getCanonical(context, previous.getItem());

        if (versionHistoryService.isFirstVersion(context, history, previous)) {
            // add a new Identifier for previous item: 12345/100.1
            String identifierPreviousItem = canonical + DOT + previous.getVersionNumber();
            //Make sure that this hasn't happened already
            if (handleService.resolveToObject(context, identifierPreviousItem) == null) {
                handleService.createHandle(context, previous.getItem(), identifierPreviousItem, true);
            }
        }

        DSpaceObject itemWithCanonicalHandle = handleService.resolveToObject(context, canonical);
        if (itemWithCanonicalHandle != null) {
            if (itemWithCanonicalHandle.getID() != previous.getItem().getID()) {
                log.warn("The previous version's item (" + previous.getItem().getID() +
                        ") does not match with the item containing handle " + canonical +
                        " (" + itemWithCanonicalHandle.getID() + ")");
            }
            // Move the original handle from whatever item it's on to the newest version
            handleService.modifyHandleDSpaceObject(context, canonical, dso);
        } else {
            handleService.createHandle(context, dso, canonical);
        }

        // add a new Identifier for this item: 12345/100.x
        String idNew = canonical + DOT + version.getVersionNumber();
        //Make sure we don't have an old handle hanging around (if our previous version was deleted in the workspace)
        if (handleService.resolveToObject(context, idNew) == null) {
            handleService.createHandle(context, dso, idNew);
        } else {
            handleService.modifyHandleDSpaceObject(context, idNew, dso);
        }

        return idNew;
    }


    protected String getCanonical(Context context, Item item) {
        String canonical = item.getHandle();
        if (canonical.matches(".*/.*\\.\\d+") && canonical.lastIndexOf(DOT) != -1) {
            canonical = canonical.substring(0, canonical.lastIndexOf(DOT));
        }

        return canonical;
    }

    protected String getCanonical(String identifier) {
        String canonical = identifier;
        if (canonical.matches(".*/.*\\.\\d+") && canonical.lastIndexOf(DOT) != -1) {
            canonical = canonical.substring(0, canonical.lastIndexOf(DOT));
        }

        return canonical;
    }

    /**
     * Remove all handles from an item's metadata and add the supplied handle instead.
     *
     * @param context The relevant DSpace Context.
     * @param dso    which dso to modify
     * @param handle  which handle to add
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    protected void modifyHandleMetadata(Context context, DSpaceObject dso, String handle)
        throws SQLException, AuthorizeException {
        // we want to exchange the old handle against the new one. To do so, we
        // load all identifiers, clear the metadata field, re add all
        // identifiers which are not from type handle and add the new handle.
        String handleref = handleService.getCanonicalForm(handle);
        DSpaceObjectService<DSpaceObject> dSpaceObjectService =
            ContentServiceFactory.getInstance().getDSpaceObjectService(dso);
        List<MetadataValue> identifiers = dSpaceObjectService
            .getMetadata(dso, MetadataSchemaEnum.DC.getName(), "identifier", "uri", Item.ANY);
        dSpaceObjectService.clearMetadata(context, dso, MetadataSchemaEnum.DC.getName(), "identifier", "uri", Item.ANY);
        for (MetadataValue identifier : identifiers) {
            if (this.supports(identifier.getValue())) {
                // ignore handles
                continue;
            }
            dSpaceObjectService.addMetadata(context,
                                    dso,
                                    identifier.getMetadataField(),
                                    identifier.getLanguage(),
                                    identifier.getValue(),
                                    identifier.getAuthority(),
                                    identifier.getConfidence());
        }
        if (!StringUtils.isEmpty(handleref)) {
            dSpaceObjectService.addMetadata(context, dso, MetadataSchemaEnum.DC.getName(),
                                    "identifier", "uri", null, handleref);
        }
        dSpaceObjectService.update(context, dso);
    }
}
