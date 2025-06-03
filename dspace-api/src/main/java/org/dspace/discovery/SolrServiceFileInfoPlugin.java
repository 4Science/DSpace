/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.discovery.index.adder.IndexAdder;
import org.dspace.discovery.index.mapper.SolrFieldMetadataMapper;
import org.dspace.discovery.indexobject.IndexableItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Adds filenames and file descriptions of all files in the ORIGINAL bundle
 * to the Solr search index.
 *
 * <p>
 * To activate the plugin, add the following line to discovery.xml
 * <pre>
 * {@code <bean id="solrServiceFileInfoPlugin" class="org.dspace.discovery.SolrServiceFileInfoPlugin"/>}
 * </pre>
 *
 * <p>
 * After activating the plugin, rebuild the discovery index by executing:
 * <pre>
 * [dspace]/bin/dspace index-discovery -b
 * </pre>
 *
 * @author Martin Walk
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 *
 */
public class SolrServiceFileInfoPlugin implements SolrServiceIndexPlugin {

    private static final Logger logger = LoggerFactory.getLogger(SolrServiceFileInfoPlugin.class);

    private static final String BUNDLE_NAME = "ORIGINAL";
    private static final String SOLR_FIELD_NAME_FOR_FILENAMES = "original_bundle_filenames";
    private static final String SOLR_FIELD_NAME_FOR_DESCRIPTIONS = "original_bundle_descriptions";
    private static final String SOLR_FIELD_NAME_FOR_MIMETYPE = "original_bundle_mime_type";
    private static final String SOLR_FIELD_NAME_FOR_CHECKSUM = "original_bundle_checksum";
    private static final String SOLR_FIELD_NAME_FOR_SIZEBYTES = "original_bundle_sizebytes";
    private static final String SOLR_FIELD_NAME_FOR_SHORT_DESCRIPTION = "original_bundle_short_description";

    private Map<String, SolrFieldMetadataMapper> mappableMetadata;

    private IndexAdder simpleIndexAdder;

    private IndexAdder defaultIndexAdder;

    private IndexAdder bitstreamMetadataIndexAdder;

    @Override
    public void additionalIndex(Context context, IndexableObject indexableObject, SolrInputDocument document) {
        if (indexableObject instanceof IndexableItem) {
            generateBundleIndex(context, document, ((IndexableItem) indexableObject).getIndexedObject().getBundles());
        }
    }

    private void generateBundleIndex(Context context, SolrInputDocument document, List<Bundle> bundles) {
        if (bundles != null) {
            for (Bundle bundle : bundles) {
                String bundleName = bundle.getName();
                if (bundleName != null && bundleName.equals(BUNDLE_NAME)) {
                    generateBitstreamIndex(context, document, bundle.getBitstreams());
                }
            }
        }
    }

    /**
     * Method that adds index to {@link SolrInputDocument}, iterates between {@code bitstreams} and {@code mappableMetadatas}
     * then applies the corresponding mapping function to the bitstream
     *
     * @param document solr document
     * @param bitstreams list of bitstreams to analyze
     */
    private void generateBitstreamIndex(Context context, SolrInputDocument document, List<Bitstream> bitstreams) {
        if (document != null && bitstreams != null) {
            for (Bitstream bitstream : bitstreams) {

                indexBitstreamFields(context, document, bitstream);

                indexBitstreamsMetadatadas(document, bitstream);
            }
        }
    }

    private void indexBitstreamFields(Context context, SolrInputDocument document, Bitstream bitstream) {
        addAndHandleException(
            simpleIndexAdder, document, bitstream, SOLR_FIELD_NAME_FOR_FILENAMES, bitstream.getName()
        );

        Optional.ofNullable(bitstream.getDescription())
            .filter(StringUtils::isNotEmpty)
            .ifPresent(
                (description) ->
                    addAndHandleException(
                        simpleIndexAdder, document, bitstream, SOLR_FIELD_NAME_FOR_DESCRIPTIONS, description
                    )
            );

        try {
            Optional<BitstreamFormat> formatOptional =
                Optional.ofNullable(bitstream.getFormat(context))
                    .filter(Objects::nonNull);

            formatOptional
                .map(BitstreamFormat::getMIMEType)
                .filter(StringUtils::isNotBlank)
                .ifPresent(format ->
                    addAndHandleException(
                        defaultIndexAdder, document, bitstream, SOLR_FIELD_NAME_FOR_MIMETYPE, format
                    )
                );

            formatOptional
                .map(BitstreamFormat::getShortDescription)
                .ifPresent(format ->
                    addAndHandleException(
                        defaultIndexAdder, document, bitstream, SOLR_FIELD_NAME_FOR_SHORT_DESCRIPTION, format
                    )
                );
        } catch (SQLException e) {
            logger.error("Error while retrievig bitstream format", e);
            throw new RuntimeException("Error while retrievig bitstream format", e);
        }

        Optional.ofNullable(bitstream.getChecksum())
            .filter(StringUtils::isNotBlank)
            .map(checksum -> bitstream.getChecksumAlgorithm() + ":" + bitstream.getChecksum())
            .ifPresent(checksum ->
                addAndHandleException(
                    defaultIndexAdder, document, bitstream, SOLR_FIELD_NAME_FOR_CHECKSUM, checksum
                )
            );

        Optional.of(bitstream.getSizeBytes())
            .filter(l -> l > 0)
            .map(String::valueOf)
            .ifPresent(size ->
                addAndHandleException(
                    simpleIndexAdder, document, bitstream, SOLR_FIELD_NAME_FOR_SIZEBYTES, size
                )
            );
    }

    protected void addAndHandleException(
        IndexAdder indexAdder,
        SolrInputDocument document, Bitstream bitstream,
        String field, String value
    ) {
        try {
            indexAdder.add(document, field, value);
        } catch (Exception e) {
            logger.warn(
                "Error occurred during the update of index field {} for bitstream {}",
                field,
                bitstream.getID()
            );
        }
    }

    private void indexBitstreamsMetadatadas(SolrInputDocument document, Bitstream bitstream) {
        bitstream
            .getMetadata()
            .stream()
            .filter(metadata -> metadata != null && StringUtils.isNotBlank(metadata.getValue()))
            .forEach(metadata -> {
                MetadataField metadataField = metadata.getMetadataField();
                String bitstreamMetadata = metadataField.toString('.');
                Optional.ofNullable(mappableMetadata.get(bitstreamMetadata))
                    .orElseGet(() -> new SolrFieldMetadataMapper(metadataField.toString(), bitstreamMetadataIndexAdder))
                    .map(document, metadata.getValue());
            });
    }

    public void setMappableMetadata(Map<String, SolrFieldMetadataMapper> mappableMetadata) {
        this.mappableMetadata = mappableMetadata;
    }

    public void setSimpleIndexAdder(IndexAdder simpleIndexAdder) {
        this.simpleIndexAdder = simpleIndexAdder;
    }

    public void setDefaultIndexAdder(IndexAdder defaultIndexAdder) {
        this.defaultIndexAdder = defaultIndexAdder;
    }

    public void setBitstreamMetadataIndexAdder(IndexAdder bitstreamMetadataIndexAdder) {
        this.bitstreamMetadataIndexAdder = bitstreamMetadataIndexAdder;
    }
}
