/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.index.adder;

import org.apache.solr.common.SolrInputDocument;

/**
 * Interface for defining strategy of adding an index.
 *
 * @author Nikita Krisonov
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public interface IndexAdder {

    String SOLR_POSTFIX_FILTER = "_filter";

    String SOLR_POSTFIX_KEYWORD = "_keyword";

    /**
     * Adds new index to the document based on passed solr field name and value.
     *
     * @param document  document which will get a new index
     * @param solrField solr field name
     * @param value     solr field value.
     */
    void add(SolrInputDocument document, String solrField, String value);
}
