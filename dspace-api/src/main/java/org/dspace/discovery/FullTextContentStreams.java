/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import jakarta.annotation.Nullable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.util.ContentStreamBase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * Construct a <code>ContentStream</code> from a <code>File</code>
 */
public class FullTextContentStreams extends ContentStreamBase {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(FullTextContentStreams.class);

    public static final String FULLTEXT_BUNDLE = "TEXT";

    protected final Context context;
    protected List<FullTextBitstream> fullTextStreams;
    protected List<FullTextBitstream> fullTextMiradorStreams;
    protected List<FullTextBitstream> fullTextVideoStreams;
    protected List<FullTextBitstream> fullTextAllStreams;
    protected BitstreamService bitstreamService;
    protected ItemService itemService;

    private String OCR_FILENAME = "extracted_text.txt";

    public FullTextContentStreams(Context context, Item parentItem) throws SQLException {
        this.context = context;
        init(parentItem);
    }

    protected void init(Item parentItem) {
        fullTextStreams = new ArrayList<>();
        fullTextMiradorStreams = new ArrayList<>();
        fullTextVideoStreams = new ArrayList<>();
        fullTextAllStreams = new ArrayList<>();

        if (parentItem != null) {
            sourceInfo = parentItem.getHandle();

            //extracted full text is always extracted as plain text
            contentType = "text/plain";

            buildFullTextList(parentItem);
        }
    }

    private void buildFullTextList(Item parentItem) {
        // now get full text of any bitstreams in the TEXT bundle
        // trundle through the bundles
        List<Bundle> textBundles = parentItem.getBundles(FULLTEXT_BUNDLE);
        List<Bundle> originalBundles = parentItem.getBundles(Constants.CONTENT_BUNDLE_NAME);

        if (CollectionUtils.isEmpty(textBundles)) {
            return;
        }

        final boolean isOcrProcessed =
            Boolean.valueOf(getItemService().getMetadata(parentItem, "iiif.search.enabled"));

        textBundles.stream()
            .flatMap(bundle -> bundle.getBitstreams().stream())
            .forEach(textBitstream -> {
                FullTextBitstream fullTextBitstream = new FullTextBitstream(sourceInfo, textBitstream);
                Bitstream originalBitstream = getOriginalBitstream(originalBundles, textBitstream);
                String viewer = getViewerProvider(originalBitstream);
                boolean isSubtitleExtracted = isOriginalBitstreamSubtitle(originalBitstream);

                if (isOcrProcessed && (OCR_FILENAME.equals(textBitstream.getName()) ||
                        textBitstream.getSizeBytes() > 0)) {
                    fullTextMiradorStreams.add(fullTextBitstream);
                } else if (StringUtils.equalsAny(viewer, "video-streaming", "audio-streaming")
                    || isSubtitleExtracted) {
                    fullTextVideoStreams.add(fullTextBitstream);
                } else if (!isOcrProcessed ||
                    !StringUtils.equalsAny(viewer, "video-streaming", "audio-streaming", "iiif")) {
                    fullTextStreams.add(fullTextBitstream);
                }

                fullTextAllStreams.add(fullTextBitstream);

                log.debug("Added BitStream: "
                    + textBitstream.getStoreNumber() + " "
                    + textBitstream.getSequenceID() + " "
                    + textBitstream.getName());
            });

    }

    private boolean isOriginalBitstreamSubtitle(Bitstream originalBitstream) {
        if (originalBitstream == null) {
            return false;
        }
        String name = originalBitstream.getName();
        if (name == null) {
            return false;
        }
        return name.endsWith(".vtt");
    }

    private Bitstream getOriginalBitstream(List<Bundle> originalBundles, Bitstream textBitstream) {
        return originalBundles.stream()
                              .flatMap(bundle ->
                                  bundle.getBitstreams().stream())
                              .filter(bitstream ->
                                  isMatchedBitstreams(bitstream, textBitstream))
                              .findFirst()
                              .orElse(null);
    }

    private boolean isMatchedBitstreams(Bitstream originalBitstream, Bitstream textBitstream) {
        return originalBitstream.getName().equals(textBitstream.getName().replace(".txt", ""));
    }

    private String getBitstreamNameWithoutExtension(String bitstreamName) {
        return bitstreamName.substring(0, bitstreamName.lastIndexOf('.'));
    }

    private String getViewerProvider(Bitstream bitstream) {
        if (bitstream == null) {
            return "";
        }
        String value = getBitstreamService().getMetadataFirstValue(bitstream,
            "bitstream", "viewer", "provider", Item.ANY);
        return value == null ? "" : value;
    }

    @Override
    public String getName() {
        return StringUtils.join(Iterables.transform(fullTextAllStreams, new Function<FullTextBitstream, String>() {
            @Nullable
            @Override
            public String apply(@Nullable FullTextBitstream input) {
                return input == null ? "" : input.getFileName();
            }
        }), ";");
    }

    @Override
    public Long getSize() {
        long result = 0;

        if (CollectionUtils.isNotEmpty(fullTextAllStreams)) {
            Iterable<Long> individualSizes = Iterables
                .transform(fullTextAllStreams, new Function<FullTextBitstream, Long>() {
                    @Nullable
                    @Override
                    public Long apply(@Nullable FullTextBitstream input) {
                        return input == null ? 0L : input.getSize();
                    }
                });

            for (Long size : individualSizes) {
                result += size;
            }
        }

        return result;
    }

    @Override
    public Reader getReader() throws IOException {
        return super.getReader();
    }

    @Override
    public InputStream getStream() throws IOException {
        try {
            return new SequenceInputStream(new FullTextEnumeration(fullTextAllStreams.iterator()));
        } catch (Exception e) {
            log.error("Unable to add full text bitstreams to SOLR for item " + sourceInfo + ": " + e.getMessage(), e);
            return new ByteArrayInputStream((e.getClass() + ": " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    public InputStream getFullTextStreamStream() {
        try {
            return new SequenceInputStream(new FullTextEnumeration(fullTextStreams.iterator()));
        } catch (Exception e) {
            log.error("Unable to add full text bitstreams to SOLR for item " +
                sourceInfo + ": " + e.getMessage(), e);
            return new ByteArrayInputStream((e.getClass() + ": " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    public InputStream getMiradorStream() {
        try {
            return new SequenceInputStream(new FullTextEnumeration(fullTextMiradorStreams.iterator()));
        } catch (Exception e) {
            log.error("Unable to add full text mirador bitstreams to SOLR for item " +
                sourceInfo + ": " + e.getMessage(), e);
            return new ByteArrayInputStream((e.getClass() + ": " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    public InputStream getVideoStream() {
        try {
            return new SequenceInputStream(new FullTextEnumeration(fullTextVideoStreams.iterator()));
        } catch (Exception e) {
            log.error("Unable to add full text video bitstreams to SOLR for item " +
                sourceInfo + ": " + e.getMessage(), e);
            return new ByteArrayInputStream((e.getClass() + ": " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    public boolean isEmpty() {
        return CollectionUtils.isEmpty(fullTextAllStreams);
    }

    private BitstreamService getBitstreamService() {
        if (bitstreamService == null) {
            bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
        }
        return bitstreamService;
    }

    private ItemService getItemService() {
        if (itemService == null) {
            itemService = ContentServiceFactory.getInstance().getItemService();
        }
        return itemService;
    }

    private class FullTextBitstream {
        private final String itemHandle;
        private final Bitstream bitstream;

        public FullTextBitstream(final String parentHandle, final Bitstream file) {
            this.itemHandle = parentHandle;
            this.bitstream = file;
        }

        public String getContentType(final Context context) throws SQLException {
            BitstreamFormat format = bitstream.getFormat(context);
            return format == null ? null : StringUtils.trimToEmpty(format.getMIMEType());
        }

        public String getFileName() {
            return StringUtils.trimToEmpty(bitstream.getName());
        }

        public long getSize() {
            return bitstream.getSizeBytes();
        }

        public InputStream getInputStream() throws SQLException, IOException, AuthorizeException {
            return getBitstreamService().retrieve(context, bitstream);
        }

        public String getItemHandle() {
            return itemHandle;
        }
    }

    /**
     * {@link Enumeration} is implemented because instances of this class are
     * passed to a JDK class that requires this obsolete type.
     */
    @SuppressWarnings("JdkObsolete")
    private static class FullTextEnumeration implements Enumeration<InputStream> {

        private final Iterator<FullTextBitstream> fulltextIterator;

        public FullTextEnumeration(final Iterator<FullTextBitstream> fulltextIterator) {
            this.fulltextIterator = fulltextIterator;
        }

        @Override
        public boolean hasMoreElements() {
            return fulltextIterator.hasNext();
        }

        @Override
        public InputStream nextElement() {
            InputStream inputStream = null;
            FullTextBitstream bitstream = null;

            try {
                bitstream = fulltextIterator.next();
                inputStream = bitstream.getInputStream();
            } catch (Exception e) {
                log.warn("Unable to add full text bitstream " + (bitstream == null ? "NULL" :
                    bitstream.getFileName() + " for item " + bitstream.getItemHandle())
                             + " to SOLR:" + e.getMessage(), e);

                inputStream = new ByteArrayInputStream(
                    (e.getClass() + ": " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
            }

            return inputStream == null ? null : new SequenceInputStream(
                new ByteArrayInputStream("\n".getBytes(StandardCharsets.UTF_8)), inputStream);
        }
    }

}

