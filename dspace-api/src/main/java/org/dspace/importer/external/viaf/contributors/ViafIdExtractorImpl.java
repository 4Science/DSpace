/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.viaf.contributors;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implementation of the VIAF ID extractor.
 * 
 * This class provides functionality to extract VIAF (Virtual International Authority File)
 * identifiers from JSON records returned by VIAF services. It specifically filters
 * results to only return VIAF IDs for personal names, excluding other entity types.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class ViafIdExtractorImpl implements ViafIdExtractor {

    private final static Logger log = LogManager.getLogger(ViafIdExtractorImpl.class);

    /** The VIAF name type value that identifies personal record */
    private static final String VIAF_PERSON_TYPE = "Personal";

    /** JsonPath expression template for extracting VIAF ID from the record */
    private static final String VIAF_ID_PATH = "$.recordData['ns%d:VIAFCluster']['ns%d:viafID']";

    /** JsonPath expression template for extracting name type from the record */
    private static final String NAME_TYPE_PATH = "$.recordData['ns%d:VIAFCluster']['ns%d:nameType']";

    /**
     * Extracts the VIAF ID from a VIAF record if it represents a Person.
     * 
     * This method processes the provided JSON node to extract both the VIAF ID and
     * the name type using JsonPath expressions. It only returns the VIAF ID if the
     * name type is "Personal", filtering out other entity types.
     * 
     * @param jsonNode The JSON node containing the VIAF record data
     * @param recordNumberForNameSpace The namespace number used in the JsonPath expressions
     * @return The VIAF ID if the record represents a Person
     */
    @Override
    public String getViafId(JsonNode jsonNode, Integer recordNumberForNameSpace) {
        String id = extractValue(jsonNode, VIAF_ID_PATH, recordNumberForNameSpace);
        String nameType = extractValue(jsonNode, NAME_TYPE_PATH, recordNumberForNameSpace);
        log.debug("Extracted viaf-id:%s and nameType: %s", id, nameType);
        return StringUtils.equals(VIAF_PERSON_TYPE, nameType) ? id : null;
    }

    /**
     * Extracts a value from the JSON node using a JsonPath expression.
     * 
     * This helper method processes the JsonPath expression by replacing the namespace
     * placeholder with the actual namespace number, then executes the path to extract
     * the desired value from the JSON structure. The method converts the JsonNode to
     * a string representation for JsonPath processing.
     * 
     * @param jsonNode The JSON node to extract data from
     * @param path The JsonPath expression template containing namespace placeholders
     * @param recordNumberForNameSpace The namespace number to substitute in the path
     * @return The extracted string value, or null if not found
     */
    private String extractValue(JsonNode jsonNode, String path, Integer recordNumberForNameSpace) {
        DocumentContext context = JsonPath.parse(jsonNode.toString());
        String currentPath = path;
        currentPath = currentPath.replaceAll("%d", recordNumberForNameSpace.toString());
        return context.read(currentPath, String.class);
    }

}
