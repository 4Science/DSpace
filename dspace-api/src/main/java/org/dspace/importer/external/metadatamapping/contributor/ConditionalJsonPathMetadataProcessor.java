/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A JsonPathMetadataProcessor implementation that applies conditional filtering
 * to extract metadata values. This processor supports complex extraction scenarios
 * where values should only be extracted when certain conditions are met.
 * 
 * This processor works in conjunction with FilterableJsonPathMetadataContributor
 * but can also be used independently with SimpleJsonPathMetadataContributor.
 * 
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class ConditionalJsonPathMetadataProcessor implements JsonPathMetadataProcessor {

    private final static Logger log = LogManager.getLogger();

    private String query;
    private String valueField;
    private List<JsonPathFilter> filters;

    /**
     * Default constructor for Spring configuration
     */
    public ConditionalJsonPathMetadataProcessor() {
        this.filters = new ArrayList<>();
    }

    /**
     * Constructor with query and valueField
     *
     * @param query The JsonPath query to select the array or object to process
     * @param valueField The field name from which to extract the final value
     */
    public ConditionalJsonPathMetadataProcessor(String query, String valueField) {
        this.query = query;
        this.valueField = valueField;
        this.filters = new ArrayList<>();
    }

    @Override
    public Collection<String> processMetadata(String json) {
        Collection<String> metadataValues = new ArrayList<>();

        JsonNode jsonNode = convertStringJsonToJsonNode(json);
        if (jsonNode == null) {
            return metadataValues;
        }

        JsonNode targetNode = jsonNode.at(query);
        if (targetNode.isArray()) {
            processArrayNode(targetNode, metadataValues);
        } else if (!targetNode.isNull()) {
            processSingleNode(targetNode, metadataValues);
        }

        return metadataValues;
    }

    /**
     * Process an array node, applying filters to each element
     */
    private void processArrayNode(JsonNode arrayNode, Collection<String> metadataValues) {
        Iterator<JsonNode> nodes = arrayNode.iterator();
        while (nodes.hasNext()) {
            JsonNode node = nodes.next();
            if (passesAllFilters(node)) {
                String extractedValue = extractValueFromNode(node);
                if (StringUtils.isNotBlank(extractedValue)) {
                    metadataValues.add(extractedValue);
                }
            }
        }
    }

    /**
     * Process a single node, applying filters
     */
    private void processSingleNode(JsonNode node, Collection<String> metadataValues) {
        if (passesAllFilters(node)) {
            String extractedValue = extractValueFromNode(node);
            if (StringUtils.isNotBlank(extractedValue)) {
                metadataValues.add(extractedValue);
            }
        }
    }

    /**
     * Check if a node passes all configured filters
     */
    private boolean passesAllFilters(JsonNode node) {
        if (filters.isEmpty()) {
            return true; // No filters means accept all
        }

        for (JsonPathFilter filter : filters) {
            if (!filter.matches(node)) {
                if (log.isDebugEnabled()) {
                    log.debug("Node failed filter: {} - Node: {}", filter.getDescription(), node.toString());
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Extract the value from a node using the configured valueField
     */
    private String extractValueFromNode(JsonNode node) {
        if (StringUtils.isBlank(valueField)) {
            return getStringValue(node);
        }

        JsonNode valueNode = node.get(valueField);
        if (valueNode != null && !valueNode.isNull()) {
            return getStringValue(valueNode);
        }

        return StringUtils.EMPTY;
    }

    /**
     * Convert a JsonNode to its string representation
     */
    private String getStringValue(JsonNode node) {
        if (node.isTextual()) {
            return node.textValue();
        }
        if (node.isNumber()) {
            return node.numberValue().toString();
        }
        if (node.isBoolean()) {
            return String.valueOf(node.booleanValue());
        }
        return node.asText();
    }

    /**
     * Convert JSON string to JsonNode
     */
    private JsonNode convertStringJsonToJsonNode(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Unable to process json response.", e);
            return null;
        }
    }

    // Getters and setters for Spring configuration

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getValueField() {
        return valueField;
    }

    public void setValueField(String valueField) {
        this.valueField = valueField;
    }

    public List<JsonPathFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<JsonPathFilter> filters) {
        this.filters = filters != null ? filters : new ArrayList<>();
    }

    /**
     * Add a single filter to the list of filters
     */
    public void addFilter(JsonPathFilter filter) {
        if (filter != null) {
            this.filters.add(filter);
        }
    }
}