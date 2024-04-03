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
 * Implementation of {@link IndexAdder} defining default index adder.
 */
public class DefaultSolrIndexAdder implements IndexAdder {

    @Override
    public void add(SolrInputDocument document, String solrFieldName, String value) {
        Collection<Object> fieldValues = document.getFieldValues(solrFieldName);
        if (fieldValues == null || !fieldValues.contains(value)) {
            document.addField(solrFieldName, value);
            document.addField(solrFieldName.concat(SOLR_POSTFIX_KEYWORD), value);
            document.addField(solrFieldName.concat(SOLR_POSTFIX_FILTER), value);
        }
    }
}
