/**
 * A {@link JsonPathFilter} implementation that checks if a specified field within a JSON object
 * contains one or more specific values. This filter can handle fields that are either a single
 * text value or an array of text values.
 *
 * This filter is useful when a field's value needs to be validated against a predefined set of values.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class ObjectContainsFilter implements JsonPathFilter {

    private String field;
    private Set<String> requiredValues;
    private boolean requireAll;

    /**
     * Constructs an ObjectContainsFilter with the specified field name and a single required value.
     * The `requireAll` flag is implicitly `false` as it only checks for one value.
     *
     * @param field         The name of the field to inspect within the JSON node.
     * @param requiredValue The string value that must be present in the field for the filter to match.
     */
    public ObjectContainsFilter(String field, String requiredValue) {
        this.field = field;
        this.requiredValues = Set.of(requiredValue);
    }

    /**
     * Constructs an ObjectContainsFilter with the specified field name, required values, and matching policy.
     *
     * @param field          The name of the field to inspect within the JSON node.
     * @param requiredValues A {@link Set} of string values to check for in the field.
     * @param requireAll     If `true`, the field (or its array elements) must contain all the `requiredValues`.
     *                       If `false`, the field (or its array elements) must contain at least one of the `requiredValues`.
     */
    public ObjectContainsFilter(String field, Set<String> requiredValues, boolean requireAll) {
        this.field = field;
        this.requiredValues = requiredValues;
        this.requireAll = requireAll;
    }

    /**
     * Tests whether the given JSON node matches the filter's criteria.
     *
     * <p>
     * The method checks if the node contains a field with the configured {@link #field} name.
     * If the field is a single text value, it checks if it's present in `requiredValues`.
     * If the field is an array, it checks if its elements contain the `requiredValues` based on the
     * {@link #requireAll} logic (either ANY or ALL).
     * </p>
     *
     * @param node The {@link JsonNode} to be tested.
     * @return {@code true} if the node's field contains the required values according to the
     *         `requireAll` policy; {@code false} otherwise, or if the field is missing or not a text/array type.
     */
    @Override
    public boolean matches(JsonNode node) {

        if (StringUtils.isEmpty(field)) {
            return true;
        }

        JsonNode jsonNode = node.get(field);
        if (jsonNode == null || (!jsonNode.isTextual() && !jsonNode.isArray())) {
            return false;
        }

        if (jsonNode.isArray()) {
            List<String> foundValues = new ArrayList<>();
            for (JsonNode element : jsonNode) {
                if (element.isTextual()) {
                    foundValues.add(element.textValue());
                }
            }
            if (requireAll) {
                return requiredValues.containsAll(foundValues);
            }
            return foundValues.stream().anyMatch(requiredValues::contains);
        }

        return requiredValues.contains(jsonNode.textValue());
    }

    /**
     * Returns a string description of the filter's configuration.
     * Useful for logging and debugging.
     *
     * @return A descriptive string including the field name, required values, and matching policy.
     */
    @Override
    public String getDescription() {
        String operator = requireAll ? "ALL" : "ANY";
        return String.format("ObjectContainsFilter[field=%s, values=%s, require=%s]",
                             field, requiredValues, operator);
    }
}
