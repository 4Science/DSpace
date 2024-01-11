/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
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

    /**
     * Class used to map a target metadata into a solr index using {@code SolrInputDocument}
     *
     * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
     *
     * @param <T>
     */
    private static class SolrFieldMetadataMapper<T> {
        private final String solrField;
        private final BiFunction<SolrInputDocument, String, Consumer<T>> fieldAdder;

        public SolrFieldMetadataMapper(
                String metadata,
                BiFunction<SolrInputDocument, String, Consumer<T>> fieldAdder
        ) {
            super();
            this.solrField = metadata;
            this.fieldAdder = fieldAdder;
        }

        public void map(SolrInputDocument document, T value) {
            this.fieldAdder.apply(document, this.solrField).accept(value);
        }

    }

    private static final Logger logger = LoggerFactory.getLogger(SolrServiceFileInfoPlugin.class);

    private static final DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd");
    private static final String BUNDLE_NAME = "ORIGINAL";
    private static final String SOLR_FIELD_NAME_FOR_FILENAMES = "original_bundle_filenames";
    private static final String SOLR_FIELD_NAME_FOR_DESCRIPTIONS = "original_bundle_descriptions";
    private static final String SOLR_FIELD_NAME_FOR_OAIRE_LICENSE_CONDITION = "original_bundle_oaire_licenseCondition";
    private static final String SOLR_FIELD_NAME_FOR_DATACITE_RIGHTS = "original_bundle_datacite_rights";
    private static final String SOLR_FIELD_NAME_FOR_DATACITE_AVAILABLE = "original_bundle_datacite_available";
    private static final String SOLR_FIELD_NAME_FOR_MIMETYPE = "original_bundle_mime_type";
    private static final String SOLR_FIELD_NAME_FOR_CHECKSUM = "original_bundle_checksum";
    private static final String SOLR_FIELD_NAME_FOR_SIZEBYTES = "original_bundle_sizebytes";
    private static final String SOLR_FIELD_NAME_FOR_SHORT_DESCRIPTION = "original_bundle_short_description";
    private static final String SOLR_POSTFIX_FILTER = "_filter";
    private static final String SOLR_POSTFIX_KEYWORD = "_keyword";
    private static final String BITSTREAM_METADATA_SOLR_PREFIX_KEYWORD = "bitstreams.";
    // used for facets and filters of type Date to correctly search them and visualize in facets.
    private static final String SOLR_POSTFIX_YEAR = ".year";
    private static final MetadataFieldName METADATA_DATACITE_RIGHTS = new MetadataFieldName("datacite", "rights");
    private static final MetadataFieldName METADATA_DATACITE_AVAILABLE = new MetadataFieldName("datacite", "available");
    private static final MetadataFieldName METADATA_LICENSE_CONDITION =
        new MetadataFieldName("oaire", "licenseCondition");

    private static final BiFunction<SolrInputDocument, String, Consumer<String>> defaultSolrIndexAdder =
        (document, fieldName) -> value -> {
            Collection<Object> fieldValues = document.getFieldValues(fieldName);
            if (fieldValues == null || !fieldValues.contains(value)) {
                addField(document, fieldName, value);
                addField(document, fieldName.concat(SOLR_POSTFIX_KEYWORD), value);
                addField(document, fieldName.concat(SOLR_POSTFIX_FILTER), value);
            }
        };

    private static final BiFunction<SolrInputDocument, String, Consumer<String>> simpleSolrIndexAdder =
        (document, fieldName) -> value -> {
            Collection<Object> fieldValues = document.getFieldValues(fieldName);
            if (fieldValues == null || !fieldValues.contains(value)) {
                addField(document, fieldName, value);
            }
        };

    private static final BiFunction<SolrInputDocument, String, Consumer<String>> bitstreamMetadataSolrIndexAdder =
        (document, fieldName) -> value -> {
            String baseIndex = BITSTREAM_METADATA_SOLR_PREFIX_KEYWORD.concat(fieldName);
            Collection<Object> fieldValues = document.getFieldValues(baseIndex);
            if (fieldValues == null || !fieldValues.contains(value)) {
                addField(document, baseIndex, value);
                addField(document, baseIndex.concat(SOLR_POSTFIX_KEYWORD), value);
                addField(document, baseIndex.concat(SOLR_POSTFIX_FILTER), value);
            }
        };

    private static final BiFunction<SolrInputDocument, String, Consumer<String>> yearSolrIndexAdder =
        (document, fieldName) -> value -> {
            Collection<Object> fieldValues = document.getFieldValues(fieldName);
            if (fieldValues == null || !fieldValues.contains(value)) {
                addField(document, fieldName, value);
                addField(document, fieldName.concat(SOLR_POSTFIX_KEYWORD), value);
                addField(document, fieldName.concat(SOLR_POSTFIX_FILTER), value);
                addField(document, fieldName.concat(SOLR_POSTFIX_YEAR), dtf.parseLocalDate(value).getYear());
            }
        };

    private static final SolrFieldMetadataMapper<String> getFieldMapper(
        String solrField,
        BiFunction<SolrInputDocument, String, Consumer<String>> adder
    ) {
        return new SolrFieldMetadataMapper<String>(solrField, adder);
    }

    private static final SolrFieldMetadataMapper<String> OAIRE_LICENSE_MAPPER =
        new SolrFieldMetadataMapper<String>(
            SOLR_FIELD_NAME_FOR_OAIRE_LICENSE_CONDITION,
            defaultSolrIndexAdder
        );

    private static final SolrFieldMetadataMapper<String> DATACITE_RIGHTS_MAPPER =
        new SolrFieldMetadataMapper<String>(
            SOLR_FIELD_NAME_FOR_DATACITE_RIGHTS,
            defaultSolrIndexAdder
        );

    private static final SolrFieldMetadataMapper<String> DATACITE_AVAILABLE_MAPPER =
        new SolrFieldMetadataMapper<String>(
            SOLR_FIELD_NAME_FOR_DATACITE_AVAILABLE,
            yearSolrIndexAdder
        );

    private static final Map<String, SolrFieldMetadataMapper<String>> mappableMetadatas =
        Stream.of(
            Map.entry(METADATA_LICENSE_CONDITION.toString(), OAIRE_LICENSE_MAPPER),
            Map.entry(METADATA_DATACITE_RIGHTS.toString(), DATACITE_RIGHTS_MAPPER),
            Map.entry(METADATA_DATACITE_AVAILABLE.toString(), DATACITE_AVAILABLE_MAPPER)
        )
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


    private static void addField(SolrInputDocument document, String name, Object value) {
        document.addField(name, value);
    }

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
            simpleSolrIndexAdder, document, bitstream, SOLR_FIELD_NAME_FOR_FILENAMES, bitstream.getName()
        );

        Optional.ofNullable(bitstream.getDescription())
            .filter(StringUtils::isNotEmpty)
            .ifPresent(
                (description) ->
                    addAndHandleException(
                        simpleSolrIndexAdder, document, bitstream, SOLR_FIELD_NAME_FOR_DESCRIPTIONS, description
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
                        defaultSolrIndexAdder, document, bitstream, SOLR_FIELD_NAME_FOR_MIMETYPE, format
                    )
                );

            formatOptional
                .map(BitstreamFormat::getShortDescription)
                .ifPresent(format ->
                    addAndHandleException(
                        simpleSolrIndexAdder, document, bitstream, SOLR_FIELD_NAME_FOR_SHORT_DESCRIPTION, format
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
                    defaultSolrIndexAdder, document, bitstream, SOLR_FIELD_NAME_FOR_CHECKSUM, checksum
                )
            );

        Optional.ofNullable(bitstream.getSizeBytes())
            .filter(l -> l > 0)
            .map(String::valueOf)
            .ifPresent(size ->
                addAndHandleException(
                    simpleSolrIndexAdder, document, bitstream, SOLR_FIELD_NAME_FOR_SIZEBYTES, size
                )
            );
    }

    protected void addAndHandleException(
        BiFunction<SolrInputDocument, String, Consumer<String>> solrIndexAdder,
        SolrInputDocument document, Bitstream bitstream,
        String field, String value
    ) {
        try {
            solrIndexAdder.apply(document, field).accept(value);
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
                Optional.ofNullable(mappableMetadatas.get(bitstreamMetadata))
                    .filter(Objects::nonNull)
                    .orElse(
                        getFieldMapper(
                            metadataField.toString(),
                            bitstreamMetadataSolrIndexAdder
                        )
                    )
                    .map(document, metadata.getValue());
            });
    }

    private <T> boolean areEquals(MetadataFieldName metadataFieldName, MetadataValue metadata) {
        return StringUtils.equals(metadataFieldName.schema, metadata.getSchema()) &&
        StringUtils.equals(metadataFieldName.element, metadata.getElement()) &&
        StringUtils.equals(metadataFieldName.qualifier, metadata.getQualifier());
    }
}
