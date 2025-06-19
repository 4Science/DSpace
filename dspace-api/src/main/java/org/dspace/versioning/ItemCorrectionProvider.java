/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;


import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.RelationshipMetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Giuseppe Digilio (giuseppe.digilio at 4science.it)
 */
//FIXME be sure AbstractVersionProvider is not implementing ItemVersionProvider
public class ItemCorrectionProvider extends AbstractVersionProvider {

    Logger log = org.apache.logging.log4j.LogManager.getLogger(ItemCorrectionProvider.class);

    private Set<String> ignoredMetadataFieldsOfCreation;

    private Set<String> ignoredMetadataFieldsOfMerging;

    private Set<String> bundles;

    @Autowired(required = true)
    protected WorkspaceItemService workspaceItemService;

    @Autowired(required = true)
    protected XmlWorkflowItemService workflowItemService;

    public WorkspaceItem createNewItemAndAddItInWorkspace(Context context, Collection collection, Item nativeItem)
            throws AuthorizeException, IOException, SQLException {

        Set<String> ignoredMetadataFieldsOfCreation =
            Sets.union(getIgnoredMetadataFields(), getIgnoredMetadataFieldsOfCreation());

        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item itemNew = workspaceItem.getItem();
        // copy metadata from native item to corrected item
        copyMetadata(context, itemNew, nativeItem, ignoredMetadataFieldsOfCreation);
        context.turnOffAuthorisationSystem();
        // copy bundles and bitstreams from native item
        createBundlesAndAddBitstreams(context, itemNew, nativeItem);
        context.restoreAuthSystemState();
        workspaceItem.setItem(itemNew);
        workspaceItemService.update(context, workspaceItem);

        log.info(
            "Created new correction item {} from item {}",
             workspaceItem.getItem().getID().toString(),
             nativeItem.getID().toString()
        );

        return workspaceItem;

    }

    public XmlWorkflowItem updateNativeItemWithCorrection(Context context, XmlWorkflowItem workflowItem,
            Item correctionItem, Item nativeItem) throws AuthorizeException, IOException, SQLException {

        Set<String> ignoredMetadataFieldsOfCreation =
                Sets.union(getIgnoredMetadataFields(), getIgnoredMetadataFieldsOfCreation());
        Set<String> ignoredMetadataFieldsOfMerging =
            Sets.union(getIgnoredMetadataFields(), getIgnoredMetadataFieldsOfMerging());

        // clear all metadata that are not ignored inside the nativeItem
        clearMetadataNotInSet(context, nativeItem, ignoredMetadataFieldsOfCreation);
        // copy metadata from corrected item to native item
        copyMetadata(context, nativeItem, correctionItem, ignoredMetadataFieldsOfMerging);

        context.turnOffAuthorisationSystem();
        // copy bundles and bitstreams of native item
        updateBundlesAndBitstreams(context, correctionItem, nativeItem);
        log.info("Updated new item " + nativeItem.getID().toString()
                + " with correction item " + correctionItem.getID().toString());

        workflowItem.setItem(nativeItem);
        workflowItemService.update(context, workflowItem);

        WorkspaceItem workspaceItem = workspaceItemService.findByItem(context, correctionItem);
        if (workspaceItem != null) {
            workspaceItemService.deleteWrapper(context, workspaceItem);
        }

        itemService.delete(context, correctionItem);
        log.info("Deleted correction item " + correctionItem.getID().toString());

        context.restoreAuthSystemState();

        return workflowItem;
    }

    private void clearMetadataNotInSet(Context context, Item nativeItem, Set<String> ignoredMetadata) {
        Set<MetadataField> metadatasToClear = new HashSet<>();
        Iterator<MetadataValue> metadata = nativeItem.getMetadata().iterator();
        MetadataValue metadataValue = null;
        while (!ignoredMetadata.isEmpty() && metadata.hasNext() && (metadataValue = metadata.next()) != null) {
            if (!isIgnored(ignoredMetadata, metadataValue)) {
                metadatasToClear.add(metadataValue.getMetadataField());
            }
        }
        if (!metadatasToClear.isEmpty()) {
            for (MetadataField metadataField : metadatasToClear) {
                try {
                    this.itemService.clearMetadata(
                        context, nativeItem,
                        metadataField.getMetadataSchema().getName(),
                        metadataField.getElement(),
                        metadataField.getQualifier(),
                        Item.ANY
                    );
                } catch (SQLException e) {
                    throw new RuntimeException(
                        "Cannot clear not ignored Metadata: " + metadataField.toString(), e
                    );
                }
            }
        }
    }

    private void copyMetadata(Context context, Item itemNew, Item nativeItem, Set<String> ignoredMetadataFields)
        throws SQLException {
        List<MetadataValue> metadataList = itemService.getMetadata(nativeItem, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (MetadataValue metadataValue : metadataList) {
            if (isRelationshipOrIgnored(ignoredMetadataFields, metadataValue)) {
                //Skip this metadata field (ignored and/or virtual)
                continue;
            }
            MetadataField metadataField = metadataValue.getMetadataField();
            itemService.addMetadata(
                context,
                itemNew,
                metadataField.getMetadataSchema().getName(),
                metadataField.getElement(),
                metadataField.getQualifier(),
                metadataValue.getLanguage(),
                metadataValue.getValue(),
                metadataValue.getAuthority(),
                metadataValue.getConfidence(),
                metadataValue.getPlace()
            );
        }
    }

    protected boolean isRelationshipOrIgnored(Set<String> ignoredMetadataFields, MetadataValue metadataValue) {
        return metadataValue instanceof RelationshipMetadataValue || isIgnored(ignoredMetadataFields, metadataValue);
    }

    protected boolean isIgnored(Set<String> ignoredMetadataFields, MetadataValue metadataValue) {
        return isIgnoredWithQualifier(ignoredMetadataFields, metadataValue.getMetadataField()) ||
            isIgnoredAnyQualifier(ignoredMetadataFields, metadataValue.getMetadataField());
    }

    private boolean isIgnoredAnyQualifier(Set<String> ignoredMetadataFields, MetadataField metadataField) {
        return ignoredMetadataFields.contains(getUnqualifiedMetadata(metadataField));
    }

    private String getUnqualifiedMetadata(MetadataField metadataField) {
        return metadataField.getMetadataSchema().getName() + "." + metadataField.getElement() + "." + Item.ANY;
    }

    private boolean isIgnoredWithQualifier(Set<String> ignoredMetadataFields, MetadataField metadataField) {
        return ignoredMetadataFields.contains(metadataField.toString('.'));
    }

    protected void updateBundlesAndBitstreams(Context c, Item itemNew, Item nativeItem)
            throws SQLException, AuthorizeException, IOException {

        for (String bundleName : bundles) {
            List<Bundle> nativeBundles = nativeItem.getBundles(bundleName);
            List<Bundle> correctedBundles = itemNew.getBundles(bundleName);

            if (CollectionUtils.isEmpty(nativeBundles) && CollectionUtils.isEmpty(correctedBundles)) {
                continue;
            }

            Bundle nativeBundle;
            if (CollectionUtils.isEmpty(nativeBundles)) {
                nativeBundle = bundleService.create(c, nativeItem, bundleName);
            } else {
                nativeBundle = nativeBundles.get(0);
            }

            Bundle correctedBundle;
            if (CollectionUtils.isEmpty(correctedBundles)) {
                correctedBundle = bundleService.create(c, nativeItem, bundleName);
            } else {
                correctedBundle = correctedBundles.get(0);
            }

            updateBundleAndBitstreams(c, nativeBundle, correctedBundle);
        }
    }

    protected void updateBundleAndBitstreams(Context c, Bundle nativeBundle, Bundle correctedBundle)
            throws SQLException, AuthorizeException, IOException {

        for (Bitstream bitstreamCorrected : correctedBundle.getBitstreams()) {
            // check if new bitstream exists in native bundle
            Bitstream nativeBitstream = findBitstreamByChecksum(nativeBundle, bitstreamCorrected.getChecksum());
            if (nativeBitstream != null) {
                // update native bitstream metadata
                bitstreamService.clearMetadata(c, nativeBitstream, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
                List<MetadataValue> metadataValues = bitstreamService
                        .getMetadata(bitstreamCorrected, Item.ANY, Item.ANY, Item.ANY, Item.ANY);

                for (MetadataValue metadataValue : metadataValues) {
                    bitstreamService.addMetadata(c, nativeBitstream, metadataValue.getMetadataField(),
                        metadataValue.getLanguage(), metadataValue.getValue(), metadataValue.getAuthority(),
                        metadataValue.getConfidence());
                }
                bitstreamService.update(c, nativeBitstream);
            } else {
                // Add new bitstram to native bundle
                // Metadata and additional information like internal identifier,
                // file size, checksum, and checksum algorithm are set by the bitstreamStorageService.clone(...)
                // and respectively bitstreamService.clone(...) method.
                Bitstream bitstreamNew =  bitstreamStorageService.clone(c, bitstreamCorrected);

                bundleService.addBitstream(c, nativeBundle, bitstreamNew);

                // NOTE: bundle.addBitstream() causes Bundle policies to be inherited by default.
                // So, we need to REMOVE any inherited TYPE_CUSTOM policies before copying over the correct ones.
                authorizeService.removeAllPoliciesByDSOAndType(c, bitstreamNew, ResourcePolicy.TYPE_CUSTOM);

                // Now, we need to copy the TYPE_CUSTOM resource policies from old bitstream
                // to the new bitstream, like we did above for bundles
                List<ResourcePolicy> bitstreamPolicies =
                    authorizeService.findPoliciesByDSOAndType(c, nativeBitstream, ResourcePolicy.TYPE_CUSTOM);
                authorizeService.addPolicies(c, bitstreamPolicies, bitstreamNew);

                if (correctedBundle.getPrimaryBitstream() != null && correctedBundle.getPrimaryBitstream()
                                                                              .equals(nativeBitstream)) {
                    nativeBundle.setPrimaryBitstreamID(bitstreamNew);
                }

                bitstreamService.update(c, bitstreamNew);
            }

        }
        deleteBitstreams(nativeBundle, correctedBundle);
        bundleService.update(c, nativeBundle);
        if (nativeBundle.getItems().isEmpty()) {
            bundleService.delete(c, nativeBundle);
        }
    }

    private void deleteBitstreams(Bundle nativeBundle, Bundle correctedBundle) {
        for (Bitstream bitstream : nativeBundle.getBitstreams()) {
            if (contains(correctedBundle, bitstream)) {
                continue;
            }
            nativeBundle.removeBitstream(bitstream);
        }
    }

    private boolean contains(Bundle bundle, Bitstream bitstream) {
        return bundle.getBitstreams().stream()
                     .map(Bitstream::getChecksum)
                     .anyMatch(cs -> bitstream.getChecksum().equals(cs));
    }

    protected Bitstream findBitstreamByChecksum(Bundle bundle, String bitstreamChecksum) {
        List<Bitstream> bitstreams = bundle.getBitstreams();
        for (Bitstream bitstream : bitstreams) {
            if (bitstream.getChecksum().equals(bitstreamChecksum)) {
                return bitstream;
            }
        }

        return null;
    }

    public Set<String> getIgnoredMetadataFieldsOfCreation() {
        if (ignoredMetadataFieldsOfCreation == null) {
            return new HashSet<>();
        }
        return ignoredMetadataFieldsOfCreation;
    }

    public void setIgnoredMetadataFieldsOfCreation(Set<String> ignoredMetadataFieldsOfCreation) {
        this.ignoredMetadataFieldsOfCreation = ignoredMetadataFieldsOfCreation;
    }

    public Set<String> getIgnoredMetadataFieldsOfMerging() {
        if (ignoredMetadataFieldsOfMerging == null) {
            return new HashSet<>();
        }
        return ignoredMetadataFieldsOfMerging;
    }

    public void setIgnoredMetadataFieldsOfMerging(Set<String> ignoredMetadataFieldsOfMerging) {
        this.ignoredMetadataFieldsOfMerging = ignoredMetadataFieldsOfMerging;
    }

    public Set<String> getBundles() {
        return bundles;
    }

    public void setBundles(Set<String> bundles) {
        this.bundles = bundles;
    }
}
