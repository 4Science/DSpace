/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.apache.commons.lang.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterators;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.annotation.Nullable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.BitstreamDAO;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the Bitstream object.
 * This class is responsible for all business logic calls for the Bitstream object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class BitstreamServiceImpl extends DSpaceObjectServiceImpl<Bitstream> implements BitstreamService {

    /**
     * log4j logger
     */
    private static final Logger log
            = org.apache.logging.log4j.LogManager.getLogger();


    @Autowired(required = true)
    protected BitstreamDAO bitstreamDAO;
    @Autowired(required = true)
    protected ItemService itemService;


    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected BitstreamFormatService bitstreamFormatService;
    @Autowired(required = true)
    protected BundleService bundleService;
    @Autowired(required = true)
    protected BitstreamStorageService bitstreamStorageService;

    @Autowired
    private ConfigurationService configurationService;

    protected BitstreamServiceImpl() {
        super();
    }

    @Override
    public Bitstream find(Context context, UUID id) throws SQLException {
        Bitstream bitstream = bitstreamDAO.findByID(context, Bitstream.class, id);

        if (bitstream == null) {
            if (log.isDebugEnabled()) {
                log.debug(LogHelper.getHeader(context, "find_bitstream",
                                               "not_found,bitstream_id=" + id));
            }

            return null;
        }

        // not null, return Bitstream
        if (log.isDebugEnabled()) {
            log.debug(LogHelper.getHeader(context, "find_bitstream",
                                           "bitstream_id=" + id));
        }

        return bitstream;
    }

    @Override
    public List<Bitstream> findAll(Context context) throws SQLException {
        return bitstreamDAO.findAll(context, Bitstream.class);
    }

    @Override
    public Bitstream clone(Context context, Bitstream bitstream)
            throws SQLException, AuthorizeException {
        // Create a new bitstream with a new ID.
        Bitstream clonedBitstream = bitstreamDAO.create(context, new Bitstream());
        // Set the internal identifier, file size, checksum, and
        // checksum algorithm as same as the given bitstream.
        clonedBitstream.setInternalId(bitstream.getInternalId());
        clonedBitstream.setSizeBytes(bitstream.getSizeBytes());
        clonedBitstream.setChecksum(bitstream.getChecksum());
        clonedBitstream.setChecksumAlgorithm(bitstream.getChecksumAlgorithm());
        clonedBitstream.setFormat(bitstream.getBitstreamFormat());
        update(context, clonedBitstream);
        return clonedBitstream;
    }

    @Override
    public Iterator<Bitstream> findAll(Context context, int limit, int offset) throws SQLException {
        return bitstreamDAO.findAll(context, limit, offset);
    }

    @Override
    public Bitstream create(Context context, InputStream is) throws IOException, SQLException {
        // Store the bits
        UUID bitstreamID = bitstreamStorageService.store(context, bitstreamDAO.create(context, new Bitstream()), is);

        log.info(LogHelper.getHeader(context, "create_bitstream",
                                      "bitstream_id=" + bitstreamID));

        // Set the format to "unknown"
        Bitstream bitstream = find(context, bitstreamID);
        setFormat(context, bitstream, null);

        context.addEvent(
            new Event(Event.CREATE, Constants.BITSTREAM, bitstreamID, null, getIdentifiers(context, bitstream)));

        return bitstream;
    }

    @Override
    public Bitstream create(Context context, Bundle bundle, InputStream is)
        throws IOException, SQLException, AuthorizeException {
        // Check authorisation
        authorizeService.authorizeAction(context, bundle, Constants.ADD);

        Bitstream b = create(context, is);
        bundleService.addBitstream(context, bundle, b);
        return b;
    }

    @Override
    public Bitstream register(Context context, Bundle bundle, int assetstore, String bitstreamPath)
        throws IOException, SQLException, AuthorizeException {
        // check authorisation
        authorizeService.authorizeAction(context, bundle, Constants.ADD);

        Bitstream bitstream = register(context, assetstore, bitstreamPath);

        bundleService.addBitstream(context, bundle, bitstream);
        return bitstream;
    }

    /**
     * Register a new bitstream, with a new ID.  The checksum and file size
     * are calculated.  This method is not public, and does not check
     * authorisation; other methods such as Bundle.createBitstream() will
     * check authorisation.  The newly created bitstream has the "unknown"
     * format.
     *
     * @param context       DSpace context object
     * @param assetstore    corresponds to an assetstore in dspace.cfg
     * @param bitstreamPath the path and filename relative to the assetstore
     * @return the newly registered bitstream
     * @throws IOException        if IO error
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    @Override
    public Bitstream register(Context context,
                              int assetstore, String bitstreamPath)
        throws IOException, SQLException, AuthorizeException {
        // Store the bits
        Bitstream bitstream = bitstreamDAO.create(context, new Bitstream());
        bitstreamStorageService.register(
            context, bitstream, assetstore, bitstreamPath);

        log.info(LogHelper.getHeader(context,
                                      "create_bitstream",
                                      "bitstream_id=" + bitstream.getID()));

        // Set the format to "unknown"
        setFormat(context, bitstream, null);

        context.addEvent(new Event(Event.CREATE, Constants.BITSTREAM,
                                   bitstream.getID(), "REGISTER", getIdentifiers(context, bitstream)));

        return bitstream;
    }

    @Override
    public void setUserFormatDescription(Context context, Bitstream bitstream, String desc) throws SQLException {
        setFormat(context, bitstream, null);
        setMetadataSingleValue(context, bitstream, MetadataSchemaEnum.DC.getName(), "format", null, null, desc);
    }

    @Override
    public String getFormatDescription(Context context, Bitstream bitstream) throws SQLException {
        if (bitstream.getFormat(context).getShortDescription().equals("Unknown")) {
            // Get user description if there is one
            String desc = bitstream.getUserFormatDescription();

            if (desc == null) {
                return "Unknown";
            }

            return desc;
        }

        // not null or Unknown
        return bitstream.getFormat(context).getShortDescription();
    }

    @Override
    public void setFormat(Context context, Bitstream bitstream, BitstreamFormat bitstreamFormat) throws SQLException {
        // FIXME: Would be better if this didn't throw an SQLException,
        // but we need to find the unknown format!
        if (bitstreamFormat == null) {
            // Use "Unknown" format
            bitstreamFormat = bitstreamFormatService.findUnknown(context);
        }

        // Remove user type description
        clearMetadata(context, bitstream, MetadataSchemaEnum.DC.getName(), "format", null, Item.ANY);

        // Update the ID in the table row
        bitstream.setFormat(bitstreamFormat);
    }

    @Override
    public void update(Context context, Bitstream bitstream) throws SQLException, AuthorizeException {
        // Check authorisation
        authorizeService.authorizeAction(context, bitstream, Constants.WRITE);

        log.info(LogHelper.getHeader(context, "update_bitstream",
                                      "bitstream_id=" + bitstream.getID()));
        super.update(context, bitstream);
        if (bitstream.isModified()) {
            context.addEvent(new Event(Event.MODIFY, Constants.BITSTREAM, bitstream.getID(), null,
                                       getIdentifiers(context, bitstream)));
            bitstream.setModified();
        }
        if (bitstream.isMetadataModified()) {
            context.addEvent(
                new Event(Event.MODIFY_METADATA, Constants.BITSTREAM, bitstream.getID(), bitstream.getDetails(),
                          getIdentifiers(context, bitstream)));
            bitstream.clearModified();
            bitstream.clearDetails();
        }

        bitstreamDAO.save(context, bitstream);
    }

    @Override
    public void delete(Context context, Bitstream bitstream) throws SQLException, AuthorizeException {

        // changed to a check on delete
        // Check authorisation
        authorizeService.authorizeAction(context, bitstream, Constants.DELETE);
        log.info(LogHelper.getHeader(context, "delete_bitstream",
                                      "bitstream_id=" + bitstream.getID()));

        context.addEvent(new Event(Event.DELETE, Constants.BITSTREAM, bitstream.getID(),
                                   String.valueOf(bitstream.getSequenceID()), getIdentifiers(context, bitstream)));

        // Remove bitstream itself
        bitstream.setDeleted(true);
        update(context, bitstream);

        //Remove our bitstream from all our bundles
        final List<Bundle> bundles = bitstream.getBundles();
        for (Bundle bundle : bundles) {
            authorizeService.authorizeAction(context, bundle, Constants.REMOVE);
            //We also need to remove the bitstream id when it's set as bundle's primary bitstream
            if (bitstream.equals(bundle.getPrimaryBitstream())) {
                bundle.unsetPrimaryBitstreamID();
            }
            bundle.removeBitstream(bitstream);
        }

        //Remove all bundles from the bitstream object, clearing the connection in 2 ways
        bundles.clear();

        // Remove policies only after the bitstream has been updated (otherwise the current user has not WRITE rights)
        authorizeService.removeAllPolicies(context, bitstream);
    }

    @Override
    public int getSupportsTypeConstant() {
        return Constants.BITSTREAM;
    }

    @Override
    public InputStream retrieve(Context context, Bitstream bitstream)
        throws IOException, SQLException, AuthorizeException {
        // Maybe should return AuthorizeException??
        authorizeService.authorizeAction(context, bitstream, Constants.READ);

        return bitstreamStorageService.retrieve(context, bitstream);
    }

    @Override
    public boolean isRegisteredBitstream(Bitstream bitstream) {
        return bitstreamStorageService.isRegisteredBitstream(bitstream.getInternalId());
    }

    @Override
    public DSpaceObject getParentObject(Context context, Bitstream bitstream) throws SQLException {
        List<Bundle> bundles = bitstream.getBundles();
        if (CollectionUtils.isNotEmpty(bundles)) {
            // the ADMIN action is not allowed on Bundle object so skip to the item
            Item item = (Item) bundleService.getParentObject(context, bundles.iterator().next());
            if (item != null) {
                return item;
            } else {
                return null;
            }
        } else if (bitstream.getCommunity() != null) {
            return bitstream.getCommunity();
        } else if (bitstream.getCollection() != null) {
            return bitstream.getCollection();
        }
        return null;
    }

    @Override
    public void updateLastModified(Context context, Bitstream bitstream) {
        //Also fire a modified event since the bitstream HAS been modified
        context.addEvent(
            new Event(Event.MODIFY, Constants.BITSTREAM, bitstream.getID(), null, getIdentifiers(context, bitstream)));
    }

    @Override
    public List<Bitstream> findDeletedBitstreams(Context context, int limit, int offset) throws SQLException {
        return bitstreamDAO.findDeletedBitstreams(context, limit, offset);
    }

    @Override
    public void expunge(Context context, Bitstream bitstream) throws SQLException, AuthorizeException {
        authorizeService.authorizeAction(context, bitstream, Constants.DELETE);
        if (!bitstream.isDeleted()) {
            throw new IllegalStateException("Bitstream " + bitstream.getID().toString()
                    + " must be deleted before it can be removed from the database.");
        }
        bitstreamDAO.delete(context, bitstream);
    }

    @Override
    public List<Bitstream> findDuplicateInternalIdentifier(Context context, Bitstream bitstream) throws SQLException {
        return bitstreamDAO.findDuplicateInternalIdentifier(context, bitstream);
    }

    @Override
    public Iterator<Bitstream> getItemBitstreams(Context context, Item item) throws SQLException {
        return bitstreamDAO.findByItem(context, item);
    }


    @Override
    public Iterator<Bitstream> getCollectionBitstreams(Context context, Collection collection) throws SQLException {
        return bitstreamDAO.findByCollection(context, collection);

    }

    @Override
    public Iterator<Bitstream> getCommunityBitstreams(Context context, Community community) throws SQLException {
        return bitstreamDAO.findByCommunity(context, community);
    }

    @Override
    public List<Bitstream> findBitstreamsWithNoRecentChecksum(Context context) throws SQLException {
        return bitstreamDAO.findBitstreamsWithNoRecentChecksum(context);
    }

    @Override
    public Bitstream getBitstreamByName(Item item, String bundleName, String bitstreamName) throws SQLException {
        List<Bundle> bundles = itemService.getBundles(item, bundleName);
        for (int i = 0; i < bundles.size(); i++) {
            Bundle bundle = bundles.get(i);
            List<Bitstream> bitstreams = bundle.getBitstreams();
            for (int j = 0; j < bitstreams.size(); j++) {
                Bitstream bitstream = bitstreams.get(j);
                if (StringUtils.equals(bitstream.getName(), bitstreamName)) {
                    return bitstream;
                }
            }
        }
        return null;
    }

    @Override
    public List<Bitstream> getBitstreamByBundleName(Item item, String bundleName) throws SQLException {
        return itemService.getBundles(item, bundleName).stream()
            .flatMap(bundle -> bundle.getBitstreams().stream())
            .collect(Collectors.toList());
    }

    @Override
    public Bitstream getFirstBitstream(Item item, String bundleName) throws SQLException {
        List<Bundle> bundles = itemService.getBundles(item, bundleName);
        if (CollectionUtils.isNotEmpty(bundles)) {
            List<Bitstream> bitstreams = bundles.get(0).getBitstreams();
            if (CollectionUtils.isNotEmpty(bitstreams)) {
                return bitstreams.get(0);
            }
        }
        return null;
    }

    @Override
    public Bitstream getThumbnail(Context context, Bitstream bitstream) throws SQLException {
        Pattern pattern = getBitstreamNamePattern(bitstream);

        for (Bundle bundle : bitstream.getBundles()) {
            for (Item item : bundle.getItems()) {
                for (Bundle thumbnails : itemService.getBundles(item, "THUMBNAIL")) {
                    for (Bitstream thumbnail : thumbnails.getBitstreams()) {
                        if (pattern.matcher(thumbnail.getName()).matches() &&
                            isValidThumbnail(context, thumbnail)) {
                            return thumbnail;
                        }
                    }
                }

                for (Bundle thumbnails : itemService.getBundles(item, "PREVIEW")) {
                    for (Bitstream thumbnail : thumbnails.getBitstreams()) {
                        if (pattern.matcher(thumbnail.getName()).matches() &&
                            isValidThumbnail(context, thumbnail)) {
                            return thumbnail;
                        }
                    }
                }

                if (isValidThumbnail(context, bitstream)) {
                    return bitstream;
                }
            }
        }

        return null;
    }

    protected Pattern getBitstreamNamePattern(Bitstream bitstream) {
        if (bitstream.getName() != null) {
            return Pattern.compile("^" + Pattern.quote(bitstream.getName()) + ".([^.]+)$");
        }
        return Pattern.compile("^" + bitstream.getName() + ".([^.]+)$");
    }

    @Override
    public boolean isValidThumbnail(Context context, Bitstream thumbnail) throws SQLException {
        return thumbnail != null &&
            configurationService.getIntProperty("cris.layout.thumbnail.maxsize", 250000) >= thumbnail.getSizeBytes() &&
            containsIgnoreCase(thumbnail.getFormat(context).getMIMEType(), "image/");
    }

    @Override
    public BitstreamFormat getFormat(Context context, Bitstream bitstream) throws SQLException {
        if (bitstream.getBitstreamFormat() == null) {
            return bitstreamFormatService.findUnknown(context);
        } else {
            return bitstream.getBitstreamFormat();
        }
    }

    @Override
    public Iterator<Bitstream> findByStoreNumber(Context context, Integer storeNumber) throws SQLException {
        return bitstreamDAO.findByStoreNumber(context, storeNumber);
    }

    @Override
    public Long countByStoreNumber(Context context, Integer storeNumber) throws SQLException {
        return bitstreamDAO.countByStoreNumber(context, storeNumber);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return bitstreamDAO.countRows(context);
    }

    @Override
    public Bitstream findByIdOrLegacyId(Context context, String id) throws SQLException {
        try {
            if (StringUtils.isNumeric(id)) {
                return findByLegacyId(context, Integer.parseInt(id));
            } else {
                return find(context, UUID.fromString(id));
            }
        } catch (IllegalArgumentException e) {
            // Not a valid legacy ID or valid UUID
            return null;
        }
    }

    @Override
    public Bitstream findByLegacyId(Context context, int id) throws SQLException {
        return bitstreamDAO.findByLegacyId(context, id, Bitstream.class);

    }

    @Override
    public int countDeletedBitstreams(Context context) throws SQLException {
        return bitstreamDAO.countDeleted(context);
    }

    @Override
    public int countBitstreamsWithoutPolicy(Context context) throws SQLException {
        return bitstreamDAO.countWithNoPolicy(context);
    }

    @Override
    public List<Bitstream> getNotReferencedBitstreams(Context context) throws SQLException {
        return bitstreamDAO.getNotReferencedBitstreams(context);
    }

    @Nullable
    @Override
    public Long getLastModified(Bitstream bitstream) throws IOException {
        return bitstreamStorageService.getLastModified(bitstream);
    }

    @Override
    public List<Bitstream> findShowableByItem(Context context, UUID itemId, String bundleName,
            Map<String, String> filterMetadata) throws SQLException {

        return streamOf(bitstreamDAO.findShowableByItem(context, itemId, bundleName))
            .filter(bitstream -> hasAllMetadataValues(bitstream, filterMetadata))
            .collect(Collectors.toList());

    }

    @Override
    public List<Bitstream> findByItemAndBundleAndMetadata(Context context, Item item, String bundleName,
        Map<String, String> filterMetadata) {

        try {

            return streamOf(getItemBitstreams(context, item))
                .filter(bitstream -> isContainedInBundleNamed(bitstream, bundleName))
                .filter(bitstream -> hasAllMetadataValues(bitstream, filterMetadata))
                .collect(Collectors.toList());

        } catch (SQLException ex) {
            throw new SQLRuntimeException(ex);
        }

    }

    public boolean exists(Context context, UUID id) throws SQLException {
        return this.bitstreamDAO.exists(context, Bitstream.class, id);
    }

    private boolean isContainedInBundleNamed(Bitstream bitstream, String name) {

        if (StringUtils.isEmpty(name)) {
            return true;
        }

        try {
            return bitstream.getBundles().stream()
                .anyMatch(bundle -> name.equals(bundle.getName()));
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }

    }

    private boolean hasAllMetadataValues(Bitstream bitstream, Map<String, String> filterMetadata) {
        return filterMetadata.keySet().stream()
            .allMatch(metadataField -> hasMetadataValue(bitstream, metadataField, filterMetadata.get(metadataField)));
    }

    private boolean hasMetadataValue(Bitstream bitstream, String metadataField, String value) {
        if (StringUtils.isEmpty(metadataField) || StringUtils.isEmpty(value)) {
            return true;
        }
        List<MetadataValue> metadata = bitstream.getMetadata().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().toString('.').equals(metadataField))
                .collect(Collectors.toList());
        if (isNegativeMatch(value) && metadata.size() == 0) {
            return true;
        }
        return metadata.stream().anyMatch(metadataValue -> matchesMetadataValue(metadataValue, value));
    }

    private boolean isNegativeMatch(String value) {
        return StringUtils.startsWith(value, "!");
    }

    private boolean isRegexMatch(String value) {
        String tmpValue = value;
        if (isNegativeMatch(value)) {
            tmpValue = value.substring(1);
        }
        return StringUtils.startsWith(tmpValue, "(") && StringUtils.endsWith(tmpValue, ")");
    }

    private String getMatchValue(String value) {
        String tmpValue = value;
        if (isNegativeMatch(value)) {
            tmpValue = value.substring(1);
        }
        if (isRegexMatch(tmpValue)) {
            return tmpValue.substring(1, tmpValue.length() - 1);
        }
        return tmpValue;
    }


    private boolean matchesMetadataValue(MetadataValue metadataValue, String value) {
        boolean isNegative = isNegativeMatch(value);
        boolean matchResult = false;
        if (isRegexMatch(value)) {
            matchResult = metadataValue.getValue().matches(getMatchValue(value));
        } else {
            matchResult = StringUtils.equals(metadataValue.getValue(), getMatchValue(value));
        }
        if (isNegative) {
            return !matchResult;
        } else {
            return matchResult;
        }
    }

    private Stream<Bitstream> streamOf(Iterator<Bitstream> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }

    @Override
    public boolean isOriginalBitstream(DSpaceObject dso) throws SQLException {

        if (dso.getType() != Constants.BITSTREAM) {
            return false;
        }

        Bitstream bitstream = (Bitstream) dso;

        return bitstream.getBundles().stream()
            .anyMatch(bundle -> "ORIGINAL".equals(bundle.getName()));

    }

    @Override
    public void updateThumbnailResourcePolicies(Context context, Bitstream bitstream) throws SQLException {
        getThumbnail(bitstream)
            .ifPresent(thumbnail -> replacePolicies(context, bitstream, thumbnail));
    }

    private void replacePolicies(Context context, Bitstream bitstream, Bitstream thumbnail) {
        try {
            authorizeService.replaceAllPolicies(context, bitstream, thumbnail);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<Bitstream> getThumbnail(Bitstream bitstream) throws SQLException {
        return getItem(bitstream)
            .flatMap(item -> getThumbnail(item, bitstream.getName()));
    }

    private Optional<Item> getItem(Bitstream bitstream) throws SQLException {
        return bitstream.getBundles().stream()
            .flatMap(bundle -> bundle.getItems().stream())
            .findFirst();
    }

    private Optional<Bitstream> getThumbnail(Item item, String name) {
        List<Bundle> bundles = getThumbnailBundles(item);
        if (CollectionUtils.isEmpty(bundles)) {
            return Optional.empty();
        }

        return bundles.stream()
            .flatMap(bundle -> bundle.getBitstreams().stream())
            .filter(bitstream -> startsWith(bitstream.getName(), name))
            .findFirst();
    }

    private List<Bundle> getThumbnailBundles(Item item) {
        try {
            return itemService.getBundles(item, "THUMBNAIL");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
