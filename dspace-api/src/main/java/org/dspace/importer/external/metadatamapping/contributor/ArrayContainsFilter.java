package org.dspace.importer.external.metadatamapping.contributor;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

/**
 * A {@link JsonPathFilter} implementation that checks if a specified array field
 * within a JSON object contains one or more required string values.
 *
 * <p>
 * This filter is highly configurable and supports both "ANY" (at least one
 * value must match) and "ALL" (all specified values must be present) modes.
 * It is particularly useful for filtering JSON structures where an array
 * property is used to classify or tag an object, such as in the ROR API v2.
 * </p>
 *
 * <p><b>Example Usage (Spring XML):</b></p>
 * <pre>{@code
 * <bean class="org.dspace.importer.external.metadatamapping.contributor.ArrayContainsFilter">
 *     <property name="arrayFieldName" value="types"/>
 *     <property name="requiredValue" value="ror_display"/>
 * </bean>
 * }</pre>
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class ArrayContainsFilter implements JsonPathFilter {

    private String arrayFieldName;
    private Set<String> requiredValues;
    private boolean requireAll;

    /**
     * Default constructor, typically used for Spring bean instantiation.
     * Initializes with an empty set of required values and sets `requireAll` to `false`.
     */
    public ArrayContainsFilter() {
        this.requiredValues = new HashSet<>();
        this.requireAll = false;
    }

    /**
     * Constructs a filter with a specified array field name and a single required value.
     * The `requireAll` flag is set to `false` by default.
     *
     * @param arrayFieldName The name of the array field to inspect within the JSON node.
     * @param requiredValue  The string value that must be present in the array for the filter to match.
     */
    public ArrayContainsFilter(String arrayFieldName, String requiredValue) {
        this.arrayFieldName = arrayFieldName;
        this.requiredValues = new HashSet<>();
        this.requiredValues.add(requiredValue);
        this.requireAll = false;
    }

    /**
     * Constructs a filter with a specified array field name, a set of required values,
     * and a flag to control matching logic.
     *
     * @param arrayFieldName The name of the array field to inspect.
     * @param requiredValues A {@link Set} of string values to check for in the array.
     * @param requireAll     If `true`, the array must contain all the `requiredValues`.
     *                       If `false`, the array must contain at least one of the `requiredValues`.
     */
    public ArrayContainsFilter(String arrayFieldName, Set<String> requiredValues, boolean requireAll) {
        this.arrayFieldName = arrayFieldName;
        this.requiredValues = new HashSet<>(requiredValues);
        this.requireAll = requireAll;
    }

    /**
     * Tests whether the given JSON node matches the filter's criteria.
     *
     * <p>
     * The method checks if the node contains an array with the configured {@link #arrayFieldName}.
     * It then verifies if that array contains the {@link #requiredValues} based on the
     * {@link #requireAll} logic (either ANY or ALL).
     * </p>
     *
     * @param node The {@link JsonNode} to be tested.
     * @return {@code true} if the node's array field contains the required values according to the
     *         `requireAll` policy; {@code false} otherwise, or if the node or array is missing.
     */
    @Override
    public boolean matches(JsonNode node) {
        if (node == null || StringUtils.isBlank(arrayFieldName)) {
            return false;
        }

        JsonNode arrayNode = node.get(arrayFieldName);
        if (arrayNode == null || !arrayNode.isArray()) {
            return false;
        }

        Set<String> foundValues = new HashSet<>();
        for (JsonNode element : arrayNode) {
            if (element.isTextual()) {
                foundValues.add(element.textValue());
            }
        }

        if (requireAll) {
            return foundValues.containsAll(requiredValues);
        } else {
            for (String requiredValue : requiredValues) {
                if (foundValues.contains(requiredValue)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Returns a string description of the filter's configuration.
     * Useful for logging and debugging.
     *
     * @return A descriptive string including the array field name, required values, and matching policy.
     */
    @Override
    public String getDescription() {
        String operator = requireAll ? "ALL" : "ANY";
        return String.format("ArrayContainsFilter[field=%s, values=%s, require=%s]",
                             arrayFieldName, requiredValues, operator);
    }

    // Getters and setters for Spring configuration

    public String getArrayFieldName() {
        return arrayFieldName;
    }

    /**
     * Sets the name of the array field to be inspected in the JSON node.
     *
     * @param arrayFieldName The name of the array property (e.g., "types").
     */
    public void setArrayFieldName(String arrayFieldName) {
        this.arrayFieldName = arrayFieldName;
    }

    public Set<String> getRequiredValues() {
        return requiredValues;
    }

    /**
     * Sets the collection of required values to be checked in the array.
     *
     * @param requiredValues A {@link Set} of string values.
     */
    public void setRequiredValues(Set<String> requiredValues) {
        this.requiredValues = requiredValues != null ? new HashSet<>(requiredValues) : new HashSet<>();
    }

    /**
     * A convenience setter for configuring a single required value.
     * This is commonly used in Spring XML for simpler filter definitions.
     *
     * @param requiredValue The single string value to require.
     */
    public void setRequiredValue(String requiredValue) {
        this.requiredValues = new HashSet<>();
        if (StringUtils.isNotBlank(requiredValue)) {
            this.requiredValues.add(requiredValue);
        }
    }

    /**
     * A convenience setter for providing required values as a single comma-separated string.
     * This is useful for concise Spring XML configuration.
     *
     * @param requiredValuesString A string containing comma-separated values (e.g., "ror_display,label").
     */
    public void setRequiredValuesString(String requiredValuesString) {
        this.requiredValues = new HashSet<>();
        if (StringUtils.isNotBlank(requiredValuesString)) {
            String[] values = requiredValuesString.split(",");
            for (String value : values) {
                this.requiredValues.add(value.trim());
            }
        }
    }

    public boolean isRequireAll() {
        return requireAll;
    }

    /**
     * Sets the matching policy for the required values.
     *
     * @param requireAll If `true`, all configured values must be present in the array (AND logic).
     *                   If `false`, at least one value must be present (OR logic). Defaults to `false`.
     */
    public void setRequireAll(boolean requireAll) {
        this.requireAll = requireAll;
    }
}