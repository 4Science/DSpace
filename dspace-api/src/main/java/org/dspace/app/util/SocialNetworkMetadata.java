/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.sql.SQLException;

import org.dspace.content.*;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.jdom.Element;

/**
 * Configuration and mapping for generic output metadata.
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.it)
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public abstract class SocialNetworkMetadata extends MappingMetadata {

    protected Context context;

    protected String itemName = "";

    protected String itemDescription = "";

    protected String imageURL = "";

    /**
     * Wrap the item, parse all configured fields and generate metadata field values.
     * @param context
     * @param item
     * @throws SQLException
     */
    public SocialNetworkMetadata(Context context, Item item)
            throws SQLException {
        this.context = context;
        this.item = item;
        itemURL = HandleManager.resolveToURL(context, item.getHandle());
        itemName = item.getName();
        if (StringUtils.isBlank(itemName)) {
            itemName = "";
        }
        itemDescription = item.getMetadata("dc.type");
        if (StringUtils.isBlank(itemDescription)) {
            itemDescription = "";
        }
        imageURL = buildImageURL();
    }

    /**
     * Build the URL of the primary bitstream image
     * @return the URL of the primary bitstream image
     * @throws SQLException
     */
    private String buildImageURL()
            throws SQLException {
        String imageURL = "";
        if (item == null || !(item instanceof Item)) {
            // retrieve default image URL
            imageURL = ConfigurationManager.getProperty("socialnetworks.image.default");
        }

        Item i = (Item)item;
        String imageID = "";
        Bitstream bitstream = retrievePrimaryBitstream(context, i, Constants.CONTENT_BUNDLE_NAME);
        if (bitstream != null) {
            imageID = bitstream.getMetadata("bitstream.iiif.imageid");
            if (StringUtils.isBlank(imageID)) {
                bitstream = retrievePrimaryBitstream(context, i, "IIIF-PDF-" + bitstream.getID());
                if (bitstream != null) {
                    imageID = bitstream.getMetadata("bitstream.iiif.imageid");
                }
            }
        }
        if (StringUtils.isNotBlank(imageID)) {
            String sWidth = bitstream.getMetadata("bitstream.image.width");
            int width = Integer.parseInt(sWidth);
            String sHeight = bitstream.getMetadata("bitstream.image.height");
            int height = Integer.parseInt(sHeight);

            // manage horizontal image when the width is at least twice the height
            // e.g. w = 20 h = 9 -> w = 9*2 = 18
            if (height < width/2) {
                width = height*2;
            }

            // build IIIF image URL
            imageURL = imageID + "/0,0," + width + "," + (width/2) + "/" + Math.min(width, 1200) + ",/0/default.jpg";
        }
        return imageURL;
    }

    /**
     * Retrieve the primary bitstream of the item
     * @param context
     * @param item
     * @param bundleName
     * @return
     * @throws SQLException
     */
    private Bitstream retrievePrimaryBitstream(Context context, Item item, String bundleName)
            throws SQLException {
        Bitstream bitstream = null;
        Bundle[] bundles = item.getBundles(bundleName);
        if (bundles != null && bundles.length > 0) {
            for (Bundle bundle : bundles) {
                bitstream = Bitstream.find(context, bundle.getPrimaryBitstreamID());
                if (bitstream == null) {
                    Bitstream[] bitstreams = bundle.getBitstreams();
                    if (bitstreams != null && bitstreams.length > 0) {
                        bitstream = bitstreams[0];
                    }
                }
            }
        }
        return bitstream;
    }

    /**
     * Fetch retaining the order of the values for any given key in which they where added.
     *
     * @return Iterable of metadata fields mapped to Open Graph and Twitter formatted values
     */
    public Collection<Entry<String, String>> getMappings()
    {
        return metadataMappings.entries();
    }

    /**
     * Produce meta elements that can easily be put into the head.
     */
    public List<Element> disseminateList() {
        List<Element> metas = new ArrayList<Element>();

        for (Entry<String, String> m : getMappings()) {
            Element e = new Element("meta");
            e.setNamespace(null);
            e.setAttribute("name", m.getKey());
            e.setAttribute("property", m.getKey());
            e.setAttribute("content", m.getValue());
            metas.add(e);
        }

        return metas;
    }
}
