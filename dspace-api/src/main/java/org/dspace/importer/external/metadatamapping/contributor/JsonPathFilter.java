/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Interface for defining filters/matchers that can be applied to JSON nodes
 * to determine whether they match specific criteria before extracting metadata values.
 * 
 * This allows for complex conditional metadata extraction from JSON objects,
 * particularly useful for ROR API v2 migration where data structures have
 * nested arrays with conditional filtering requirements.
 * 
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public interface JsonPathFilter {

    /**
     * Tests whether the given JSON node matches the filter criteria.
     * 
     * @param node The JsonNode to test against the filter criteria
     * @return true if the node matches the filter criteria, false otherwise
     */
    boolean matches(JsonNode node);
    
    /**
     * Gets the description of this filter for debugging and configuration purposes.
     * 
     * @return A string description of what this filter matches
     */
    String getDescription();
}