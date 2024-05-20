/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.index.adder;

import java.util.Collection;

import org.apache.solr.common.SolrInputDocument;

/**
 * Implementation of {@link IndexAdder} defining default bitstream metadata index adder.
 *
 * @author Nikita Krisonov
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class BitstreamMetadataSolrIndexAdder implements IndexAdder {

    private static final String BITSTREAM_METADATA_SOLR_PREFIX_KEYWORD = "bitstreams.";

    @Override
    public void add(SolrInputDocument document, String solrFieldName, String value) {
        String baseIndex = BITSTREAM_METADATA_SOLR_PREFIX_KEYWORD.concat(solrFieldName);
        Collection<Object> fieldValues = document.getFieldValues(baseIndex);
        if (fieldValues == null || !fieldValues.contains(value)) {
            document.addField(solrFieldName, value);
            document.addField(solrFieldName.concat(SOLR_POSTFIX_KEYWORD), value);
            document.addField(solrFieldName.concat(SOLR_POSTFIX_FILTER), value);
        }
    }
}
