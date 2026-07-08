/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.index.adder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

import org.apache.solr.common.SolrInputDocument;

/**
 * Implementation of {@link IndexAdder} defining index adder for handling year of a date.
 *
 * @author Nikita Krisonov
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class YearSolrIndexAdder implements IndexAdder {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String SOLR_POSTFIX_YEAR = ".year";

    @Override
    public void add(SolrInputDocument document, String solrFieldName, String value) {
        Collection<Object> fieldValues = document.getFieldValues(solrFieldName);
        if (fieldValues == null || !fieldValues.contains(value)) {
            document.addField(solrFieldName, value);
            document.addField(solrFieldName.concat(SOLR_POSTFIX_KEYWORD), value);
            document.addField(solrFieldName.concat(SOLR_POSTFIX_FILTER), value);

            int year = LocalDate.parse(value, DTF).getYear();
            document.addField(solrFieldName.concat(SOLR_POSTFIX_YEAR), year);
        }
    }
}

