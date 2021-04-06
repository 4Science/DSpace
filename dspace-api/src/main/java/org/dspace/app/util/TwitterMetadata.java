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
 * Configuration and mapping for Twitter output metadata.
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.it)
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class TwitterMetadata extends SocialNetworkMetadata {

    private final static Logger log = Logger.getLogger(TwitterMetadata.class);

    private static final String TWITTER_PREFIX = "twitter.";

    public static final String TWITTER_CARD = "twitter:card";

    public static final String TWITTER_TITLE = "twitter:title";

    public static final String TWITTER_IMAGE = "twitter:image";

    public static final String TWITTER_SUMMARY_LARGE_IMAGE = "summary_large_image";

    /**
     * Wrap the item, parse all configured fields and generate metadata field values.
     * @param context
     * @param item
     * @throws SQLException
     */
    public TwitterMetadata(Context context, Item item)
            throws SQLException {
        super(context, item);
        parseItem();
    }

    /**
     * Using metadata field mappings contained in the loaded configuration,
     * parse through configured metadata fields,
     * building valid Twitter metadata value strings.
     * Field names & values contained in metadataMappings.
     * @throws SQLException
     */
    private void parseItem()
            throws SQLException {
        metadataMappings.put(TWITTER_CARD, TWITTER_SUMMARY_LARGE_IMAGE);
        metadataMappings.put(TWITTER_TITLE, itemName);
        metadataMappings.put(TWITTER_IMAGE, imageURL);
    }

    /**
     * @return the Twitter card
     */
    public List<String> getTwitterCard() {
        return metadataMappings.get(TWITTER_CARD);
    }

    /**
     * @return the Twitter title
     */
    public List<String> getTwitterTitle() {
        return metadataMappings.get(TWITTER_TITLE);
    }

    /**
     * @return the Twitter image URL
     */
    public List<String> getTwitterImageUrl() {
        return metadataMappings.get(TWITTER_IMAGE);
    }

    @Override
    protected String getPrefix() {
        return TWITTER_PREFIX;
    }
}
