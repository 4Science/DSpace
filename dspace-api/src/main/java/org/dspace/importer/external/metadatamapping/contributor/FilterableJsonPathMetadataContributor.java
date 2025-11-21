/**
 * A {@link MetadataContributor} that supports conditional extraction of metadata
 * from a JSON document using JSONPath expressions and configurable filters.
 *
 * <p>
 * This contributor is designed for complex JSON structures where metadata values
 * need to be extracted only when certain conditions are met. It works by:
 * <ol>
 *   <li>Applying a main JSONPath {@link #query} to select a portion of the JSON tree (an array or object).</li>
 *   <li>Iterating through the selected nodes (if it's an array) or processing the single node.</li>
 *   <li>Applying a list of {@link JsonPathFilter} implementations to each node.</li>
 *   <li>If a node passes all filters, a final value is extracted using the {@link #valueField} property.</li>
 *   <li>The extracted value is then mapped to a DSpace metadata field configured by {@link #field}.</li>
 * </ol>
 * This component is highly configurable through Spring XML and is central to the ROR v2 integration.
 * </p>
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class FilterableJsonPathMetadataContributor implements MetadataContributor<String> {

    private final static Logger log = LogManager.getLogger();

    private String query;
    private String valueField;
    private MetadataFieldConfig field;
    private List<JsonPathFilter> filters;
    private JsonPathMetadataProcessor metadataProcessor;

    /**
     * Default constructor, typically used for Spring bean instantiation.
     * Initializes an empty list of filters.
     */
    public FilterableJsonPathMetadataContributor() {
        this.filters = new ArrayList<>();
    }

    /**
     * Constructs a new FilterableJsonPathMetadataContributor with essential properties.
     *
     * @param query      The JSONPath query to select the array or object to be filtered.
     * @param valueField The name of the field from which to extract the final value after filtering.
     *                   Can be a simple field name or a JSONPath expression starting with '/'.
     * @param field      The {@link MetadataFieldConfig} defining the target DSpace metadata field.
     */
    public FilterableJsonPathMetadataContributor(String query, String valueField, MetadataFieldConfig field) {
        this.query = query;
        this.valueField = valueField;
        this.field = field;
        this.filters = new ArrayList<>();
    }

    /**
     * This method is not used by this implementation.
     *
     * @param rt The metadata field mapping.
     */
    @Override
    public void setMetadataFieldMapping(MetadataFieldMapping<String, MetadataContributor<String>> rt) {
        // This contributor is self-contained and does not require the mapping object.
    }

    /**
     * Performs the metadata extraction from the given JSON string.
     *
     * <p>
     * It processes the JSON, applies the configured query and filters,
     * and returns a collection of {@link MetadatumDTO} objects representing the extracted metadata.
     * If a {@link #metadataProcessor} is set, it delegates the extraction logic to it.
     * </p>
     *
     * @param fullJson The full JSON document as a string.
     * @return A collection of {@link MetadatumDTO} containing the extracted metadata, or an empty collection if no
     *         metadata is found or an error occurs.
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(String fullJson) {
        Collection<MetadatumDTO> metadata = new ArrayList<>();
        Collection<String> metadataValue = new ArrayList<>();

        if (Objects.nonNull(metadataProcessor)) {
            metadataValue = metadataProcessor.processMetadata(fullJson);
        } else {
            JsonNode jsonNode = convertStringJsonToJsonNode(fullJson);
            if (jsonNode == null) {
                return metadata;
            }

            JsonNode targetNode = jsonNode.at(query);
            if (targetNode.isArray()) {
                processArrayNode(targetNode, metadataValue);
            } else if (!targetNode.isNull()) {
                processSingleNode(targetNode, metadataValue);
            }
        }

        // Convert extracted values to MetadatumDTO objects
        for (String value : metadataValue) {
            if (StringUtils.isNotBlank(value)) {
                MetadatumDTO metadatumDto = new MetadatumDTO();
                metadatumDto.setValue(value);
                metadatumDto.setElement(field.getElement());
                metadatumDto.setQualifier(field.getQualifier());
                metadatumDto.setSchema(field.getSchema());
                metadata.add(metadatumDto);
            }
        }

        return metadata;
    }

    /**
     * Processes a JSON array node. It iterates over each element in the array,
     * applies the configured filters, and extracts values from the elements that pass.
     *
     * @param arrayNode     The JSON array node to process.
     * @param metadataValue The collection to which extracted metadata values will be added.
     */
    private void processArrayNode(JsonNode arrayNode, Collection<String> metadataValue) {
        Iterator<JsonNode> nodes = arrayNode.iterator();
        while (nodes.hasNext()) {
            JsonNode node = nodes.next();
            if (passesAllFilters(node)) {
                append(metadataValue, node);
            }
        }
    }

    private void append(Collection<String> metadataValue, JsonNode node) {
        JsonNode resolvedNode = resolveNode(node);
        if (resolvedNode == null) {
            return;
        }

        if (resolvedNode.isArray()) {
            for (JsonNode el : resolvedNode) {
                metadataValue.add(getStringValue(el));
            }
        } else {
            metadataValue.add(getStringValue(resolvedNode));
        }
    }


    /**
     * Processes a single JSON object node. It applies the configured filters to the node
     * and, if it passes, extracts a value from it.
     *
     * @param node          The JSON object node to process.
     * @param metadataValue The collection to which the extracted metadata value will be added.
     */
    private void processSingleNode(JsonNode node, Collection<String> metadataValue) {
        if (passesAllFilters(node)) {
            append(metadataValue, node);
        }
    }

    /**
     * Checks if a given JSON node satisfies all configured filters.
     * If no filters are configured, this method returns {@code true}.
     *
     * @param node The JSON node to test.
     * @return {@code true} if the node passes all filters or if no filters are defined, {@code false} otherwise.
     */
    private boolean passesAllFilters(JsonNode node) {
        if (filters.isEmpty()) {
            return true; // No filters means accept all
        }

        for (JsonPathFilter filter : filters) {
            if (!filter.matches(node)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Resolves the final value node from a given node using the {@link #valueField} property.
     * If {@link #valueField} is blank, it returns the original node.
     *
     * @param node The JSON node from which to resolve the value.
     * @return The resolved JSON node, or {@code null} if the value field is not found.
     */
    private JsonNode resolveNode(JsonNode node) {
        if (StringUtils.isBlank(valueField)) {
            return node;
        }

        if (valueField.startsWith("/")) {
            JsonNode valueNode = node.at(valueField);
            if (valueNode != null && !valueNode.isNull()) {
                return valueNode;
            }
        }

        JsonNode valueNode = node.get(valueField);
        if (valueNode != null && !valueNode.isNull()) {
            return valueNode;
        }

        return null;
    }

    /**
     * Converts a {@link JsonNode} to its string representation.
     * Handles textual, numeric, and boolean types.
     *
     * @param node The JSON node to convert.
     * @return The string representation of the node.
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
     * Converts a JSON string to a {@link JsonNode}.
     *
     * @param json The JSON string to parse.
     * @return The parsed {@link JsonNode}, or {@code null} if parsing fails.
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

    /**
     * Sets the main JSONPath query used to select the part of the JSON to process.
     *
     * @param query A valid JSONPath expression.
     */
    public void setQuery(String query) {
        this.query = query;
    }

    public String getValueField() {
        return valueField;
    }

    /**
     * Sets the field name or JSONPath expression used to extract the final value from a filtered node.
     *
     * @param valueField The name of the field (e.g., "value") or a JSONPath starting with "/"
     */
    public void setValueField(String valueField) {
        this.valueField = valueField;
    }

    public MetadataFieldConfig getField() {
        return field;
    }

    /**
     * Sets the target DSpace metadata field configuration.
     *
     * @param field A {@link MetadataFieldConfig} object.
     */
    public void setField(MetadataFieldConfig field) {
        this.field = field;
    }

    public List<JsonPathFilter> getFilters() {
        return filters;
    }

    /**
     * Sets the list of filters to be applied to the JSON nodes.
     *
     * @param filters A list of {@link JsonPathFilter} implementations.
     */
    public void setFilters(List<JsonPathFilter> filters) {
        this.filters = filters != null ? filters : new ArrayList<>();
    }

    /**
     * Adds a single filter to the list of filters.
     *
     * @param filter A {@link JsonPathFilter} implementation to add.
     */
    public void addFilter(JsonPathFilter filter) {
        if (filter != null) {
            this.filters.add(filter);
        }
    }

    public JsonPathMetadataProcessor getMetadataProcessor() {
        return metadataProcessor;
    }

    /**
     * Sets an alternative {@link JsonPathMetadataProcessor} to handle metadata extraction.
     * If set, this processor will be used instead of the contributor's internal logic.
     *
     * @param metadataProcessor A {@link JsonPathMetadataProcessor} implementation.
     */
    public void setMetadataProcessor(JsonPathMetadataProcessor metadataProcessor) {
        this.metadataProcessor = metadataProcessor;
    }
}