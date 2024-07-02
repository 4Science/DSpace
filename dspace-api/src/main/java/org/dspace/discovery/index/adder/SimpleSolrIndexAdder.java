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
 * Implementation of {@link IndexAdder} defining simple index adder without any postfixes.
 *
 * @author Nikita Krisonov
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class SimpleSolrIndexAdder implements IndexAdder {

    @Override
    public void add(SolrInputDocument document, String solrFieldName, String value) {
        Collection<Object> fieldValues = document.getFieldValues(solrFieldName);
        if (fieldValues == null || !fieldValues.contains(value)) {
            document.addField(solrFieldName, value);
        }
    }
}
