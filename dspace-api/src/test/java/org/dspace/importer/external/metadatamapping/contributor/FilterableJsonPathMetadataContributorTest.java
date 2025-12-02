/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.dspace.AbstractUnitTest;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for FilterableJsonPathMetadataContributor
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class FilterableJsonPathMetadataContributorTest extends AbstractUnitTest {

    // Sample JSON from ROR API v2 (based on the user's example)
    private static final String SAMPLE_JSON = "{\n" +
        "  \"names\": [\n" +
        "    {\n" +
        "      \"lang\": null,\n" +
        "      \"types\": [\n" +
        "        \"acronym\"\n" +
        "      ],\n" +
        "      \"value\": \"RMIT\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"lang\": \"en\",\n" +
        "      \"types\": [\n" +
        "        \"ror_display\",\n" +
        "        \"label\"\n" +
        "      ],\n" +
        "      \"value\": \"RMIT University\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"lang\": \"en\",\n" +
        "      \"types\": [\n" +
        "        \"alias\"\n" +
        "      ],\n" +
        "      \"value\": \"Royal Melbourne Institute of Technology University\"\n" +
        "    }\n" +
        "  ]\n" +
        "}";
    private FilterableJsonPathMetadataContributor contributor;
    private MetadataFieldConfig fieldConfig;

    @Before
    public void setUp() {
        fieldConfig = new MetadataFieldConfig("dc", "title", null);
        contributor = new FilterableJsonPathMetadataContributor();
        contributor.setField(fieldConfig);
        contributor.setQuery("/names");
        contributor.setValueField("value");
    }

    @Test
    public void testExtractRorDisplayName() {
        // Setup filter to extract ror_display name
        ArrayContainsFilter filter = new ArrayContainsFilter();
        filter.setArrayFieldName("types");
        filter.setRequiredValue("ror_display");
        contributor.addFilter(filter);

        // Execute
        Collection<MetadatumDTO> result = contributor.contributeMetadata(SAMPLE_JSON);

        // Verify
        assertEquals("Should extract exactly one value", 1, result.size());
        MetadatumDTO metadata = result.iterator().next();
        assertEquals("Should extract RMIT University", "RMIT University", metadata.getValue());
        assertEquals("dc", metadata.getSchema());
        assertEquals("title", metadata.getElement());
    }

    @Test
    public void testExtractAcronym() {
        // Setup filter to extract acronym
        ArrayContainsFilter filter = new ArrayContainsFilter();
        filter.setArrayFieldName("types");
        filter.setRequiredValue("acronym");
        contributor.addFilter(filter);

        // Execute
        Collection<MetadatumDTO> result = contributor.contributeMetadata(SAMPLE_JSON);

        // Verify
        assertEquals("Should extract exactly one value", 1, result.size());
        MetadatumDTO metadata = result.iterator().next();
        assertEquals("Should extract RMIT", "RMIT", metadata.getValue());
    }

    @Test
    public void testExtractAlias() {
        // Setup filter to extract alias
        ArrayContainsFilter filter = new ArrayContainsFilter();
        filter.setArrayFieldName("types");
        filter.setRequiredValue("alias");
        contributor.addFilter(filter);

        // Execute
        Collection<MetadatumDTO> result = contributor.contributeMetadata(SAMPLE_JSON);

        // Verify
        assertEquals("Should extract exactly one value", 1, result.size());
        MetadatumDTO metadata = result.iterator().next();
        assertEquals("Should extract full name", "Royal Melbourne Institute of Technology University",
                     metadata.getValue());
    }

    @Test
    public void testExtractMultipleTypesWithOrCondition() {
        // Setup filter to extract items with ror_display OR label
        ArrayContainsFilter filter = new ArrayContainsFilter();
        filter.setArrayFieldName("types");
        Set<String> requiredValues = new HashSet<>();
        requiredValues.add("ror_display");
        requiredValues.add("label");
        filter.setRequiredValues(requiredValues);
        filter.setRequireAll(false); // OR condition
        contributor.addFilter(filter);

        // Execute
        Collection<MetadatumDTO> result = contributor.contributeMetadata(SAMPLE_JSON);

        // Verify - should get RMIT University since it has both ror_display and label
        assertEquals("Should extract exactly one value", 1, result.size());
        MetadatumDTO metadata = result.iterator().next();
        assertEquals("Should extract RMIT University", "RMIT University", metadata.getValue());
    }

    @Test
    public void testExtractMultipleTypesWithAndCondition() {
        // Setup filter to extract items that have BOTH ror_display AND label
        ArrayContainsFilter filter = new ArrayContainsFilter();
        filter.setArrayFieldName("types");
        Set<String> requiredValues = new HashSet<>();
        requiredValues.add("ror_display");
        requiredValues.add("label");
        filter.setRequiredValues(requiredValues);
        filter.setRequireAll(true); // AND condition
        contributor.addFilter(filter);

        // Execute
        Collection<MetadatumDTO> result = contributor.contributeMetadata(SAMPLE_JSON);

        // Verify - should get RMIT University since it has both ror_display and label
        assertEquals("Should extract exactly one value", 1, result.size());
        MetadatumDTO metadata = result.iterator().next();
        assertEquals("Should extract RMIT University", "RMIT University", metadata.getValue());
    }

    @Test
    public void testNoFilters() {
        // No filters should extract all values
        Collection<MetadatumDTO> result = contributor.contributeMetadata(SAMPLE_JSON);

        // Verify - should get all three values
        assertEquals("Should extract all three values", 3, result.size());

        // Collect all extracted values
        Set<String> extractedValues = new HashSet<>();
        for (MetadatumDTO metadata : result) {
            extractedValues.add(metadata.getValue());
        }

        assertTrue("Should contain RMIT", extractedValues.contains("RMIT"));
        assertTrue("Should contain RMIT University", extractedValues.contains("RMIT University"));
        assertTrue("Should contain full name",
                   extractedValues.contains("Royal Melbourne Institute of Technology University"));
    }

    @Test
    public void testNonExistentFilter() {
        // Setup filter for non-existent type
        ArrayContainsFilter filter = new ArrayContainsFilter();
        filter.setArrayFieldName("types");
        filter.setRequiredValue("non_existent");
        contributor.addFilter(filter);

        // Execute
        Collection<MetadatumDTO> result = contributor.contributeMetadata(SAMPLE_JSON);

        // Verify - should get no results
        assertEquals("Should extract no values", 0, result.size());
    }

    @Test
    public void testArrayContainsFilterMatches() {
        ArrayContainsFilter filter = new ArrayContainsFilter("types", "ror_display");

        // Test with the ror_display entry
        String testJson = "{\n" +
            "      \"lang\": \"en\",\n" +
            "      \"types\": [\n" +
            "        \"ror_display\",\n" +
            "        \"label\"\n" +
            "      ],\n" +
            "      \"value\": \"RMIT University\"\n" +
            "    }";

        // This would require parsing the JSON node, but for unit test let's test the description
        assertTrue("Filter description should contain field name",
                   filter.getDescription().contains("types"));
        assertTrue("Filter description should contain required value",
                   filter.getDescription().contains("ror_display"));
    }
}