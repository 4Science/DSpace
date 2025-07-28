/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.index.mapper;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.discovery.index.adder.IndexAdder;

/**
 * Class used to map a target metadata into a solr index using {@code SolrInputDocument}
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class SolrFieldMetadataMapper {

    private final String solrField;

    private final IndexAdder fieldAdder;

    public SolrFieldMetadataMapper(
            String metadata,
            IndexAdder indexAdder
    ) {
        this.solrField = metadata;
        this.fieldAdder = indexAdder;
    }

    /**
     * Adds new index into the {@code document} based on passed {@code value}.
     *
     * @param document document which will get a new index
     * @param value    value of new index.
     */
    public void map(SolrInputDocument document, String value) {
        fieldAdder.add(document, solrField, value);
    }
}
