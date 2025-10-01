/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

/**
 * This class is used to extract additional metadata from the Solr document of an authority item.
 * It extracts a single field value or multiple values (if the field is multivalued) from the Solr document
 * and adds it to the extras map with a specified key.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class SimpleAuthorityOtherInformationGenerator implements ItemAuthorityExtraMetadataGenerator {

    private static final Logger log = LogManager.getLogger(SimpleAuthorityOtherInformationGenerator.class);

    private String keyId;
    private String solrField;
    private String separator;
    private Set<String> authorityNames;

    @Override
    public Map<String, String> build(String authorityName, SolrDocument document) {
        Map<String, String> extras = new HashMap<>();
        if (authorityNames != null && authorityNames.contains(authorityName) && StringUtils.isNotBlank(solrField)) {
            buildSingleExtra(document, extras);
        }
        return extras;
    }

    /**
     * Extracts a single field value from the Solr document and adds it to the extras map.
     * 
     * @param document the Solr document to extract from
     * @param extras the map to populate with extracted metadata
     */
    private void buildSingleExtra(SolrDocument document, Map<String, String> extras) {
        String fieldValue = parseValue(document);
        if (StringUtils.isNotBlank(fieldValue)) {
            extras.put(this.keyId, fieldValue);
        }
    }

    private String parseValue(SolrDocument document) {
        Object obj = document.get(solrField);
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj instanceof List<?>) {
            List<?> list = (List<?>) obj;
            if (!list.isEmpty()) {
                return list.stream()
                           .map(String.class::cast)
                           .collect(Collectors.joining(this.separator));
            }
        }
        log.error("Object type:{} isn't supported", obj.getClass());
        return null;
    }

    @Override
    public List<Choice> buildAggregate(String authorityName, SolrDocument solrDocument) {
        return List.of();
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    /**
     * Sets the Solr field name to extract values from.
     * 
     * @param solrField the name of the Solr field
     */
    public void setSolrField(String solrField) {
        this.solrField = solrField;
    }

    /**
     * Sets the list of authority names that this generator should process.
     * 
     * @param authorityNames the list of authority names to match against
     */
    public void setAuthorityNames(Set<String> authorityNames) {
        this.authorityNames = authorityNames;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }
}
