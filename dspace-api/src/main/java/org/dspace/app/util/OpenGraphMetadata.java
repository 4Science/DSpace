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
import org.apache.log4j.Logger;

import java.util.List;

import org.dspace.core.Context;

/**
 * Configuration and mapping for Open Graph output metadata.
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.it)
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class OpenGraphMetadata extends SocialNetworkMetadata {

    private final static Logger log = Logger.getLogger(OpenGraphMetadata.class);

    private static final String OPEN_GRAPH_PREFIX = "open-graph.";

    public static final String OPEN_GRAPH_URL = "og:url";

    public static final String OPEN_GRAPH_TITLE = "og:title";

    public static final String OPEN_GRAPH_DESCRIPTION = "og:description";

    public static final String OPEN_GRAPH_IMAGE = "og:image";

    /**
     * Wrap the item, parse all configured fields and generate metadata field values.
     * @param context
     * @param item
     * @throws SQLException
     */
    public OpenGraphMetadata(Context context, Item item)
            throws SQLException {
        super(context, item);
        parseItem();
    }

    /**
     * Using metadata field mappings contained in the loaded configuration,
     * parse through configured metadata fields,
     * building valid Open Graph metadata value strings.
     * Field names & values contained in metadataMappings.
     * @throws SQLException
     */
    private void parseItem()
            throws SQLException {
        metadataMappings.put(OPEN_GRAPH_URL, itemURL);
        metadataMappings.put(OPEN_GRAPH_TITLE, itemName);
        metadataMappings.put(OPEN_GRAPH_DESCRIPTION, itemDescription);
        metadataMappings.put(OPEN_GRAPH_IMAGE, imageURL);
    }

    /**
     * @return the Open Graph URL
     */
    public List<String> getOpenGraphUrl() {
        return metadataMappings.get(OPEN_GRAPH_URL);
    }

    /**
     * @return the Open Graph title
     */
    public List<String> getOpenGraphTitle() {
        return metadataMappings.get(OPEN_GRAPH_TITLE);
    }

    /**
     * @return the Open Graph description
     */
    public List<String> getOpenGraphDescription() {
        return metadataMappings.get(OPEN_GRAPH_DESCRIPTION);
    }

    /**
     * @return the Open Graph image URL
     */
    public List<String> getOpenGraphImageUrl() {
        return metadataMappings.get(OPEN_GRAPH_IMAGE);
    }

    @Override
    protected String getPrefix() {
        return OPEN_GRAPH_PREFIX;
    }
}
