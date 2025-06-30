/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.dspace.AbstractUnitTest;
import org.dspace.content.Item;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Unit tests for SolrGeoMetadataMapper class
 */
public class SolrGeoMetadataMapperTest extends AbstractUnitTest {

    private SolrGeoMetadataMapper<Item> solrGeoMetadataMapper;

    @Mock
    private IndexPluginMapper<Item, List<String>> defaultMappers;

    @Mock
    private IndexPluginMapper<Item, List<String>> relationMappers;

    @Mock
    private Item item;

    @Before
    public void setUp() {
        solrGeoMetadataMapper = new SolrGeoMetadataMapper<>();
        solrGeoMetadataMapper.setDefaultMappers(defaultMappers);
        solrGeoMetadataMapper.setRelationMappers(relationMappers);
    }

    /**
     * Test mapping with default mappers returning a valid result
     */
    @Test
    public void testMapWithDefaultMappers() {
        // Setup test data
        List<String> expectedResult = List.of("[16.743178409469415,40.85864359881754]");

        // Setup mock behavior
        when(defaultMappers.map(item)).thenReturn(expectedResult);

        // Execute the method under test
        List<String> result = solrGeoMetadataMapper.map(item);

        // Verify results
        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(defaultMappers).map(item);
        verify(relationMappers, never()).map(item); // Relation mappers should not be called
    }

    /**
     * Test mapping with relation mappers when default mappers return null
     */
    @Test
    public void testMapWithRelationMappers() {
        // Setup test data
        List<String> expectedResult = List.of("[17.389286104550443,40.792034735376525]");

        // Setup mock behavior
        when(defaultMappers.map(item)).thenReturn(null);
        when(relationMappers.map(item)).thenReturn(expectedResult);

        // Execute the method under test
        List<String> result = solrGeoMetadataMapper.map(item);

        // Verify results
        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(defaultMappers).map(item);
        verify(relationMappers).map(item); // Relation mappers should be called
    }

    /**
     * Test mapping with null default mappers
     */
    @Test
    public void testMapWithNullDefaultMappers() {
        // Setup test data
        List<String> expectedResult = List.of("[17.389286104550443,40.792034735376525]");

        // Setup mock behavior - set default mappers to null
        solrGeoMetadataMapper.setDefaultMappers(null);
        when(relationMappers.map(item)).thenReturn(expectedResult);

        // Execute the method under test
        List<String> result = solrGeoMetadataMapper.map(item);

        // Verify results
        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(relationMappers).map(item); // Relation mappers should be called
    }


    /**
     * Test mapping with both mappers null
     */
    @Test
    public void testMapWithBothMappersNull() {
        // Setup mock behavior - set both mappers to null
        solrGeoMetadataMapper.setDefaultMappers(null);
        solrGeoMetadataMapper.setRelationMappers(null);

        // Execute the method under test - should handle null mappers gracefully
        List<String> result = solrGeoMetadataMapper.map(item);

        // Verify results - should return empty list
        assertThat(result, empty());
    }
}