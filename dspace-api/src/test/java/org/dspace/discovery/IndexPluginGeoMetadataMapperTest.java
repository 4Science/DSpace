/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;

import org.dspace.AbstractUnitTest;
import org.dspace.content.Item;
import org.dspace.content.dto.MetadataValueDTO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class IndexPluginGeoMetadataMapperTest extends AbstractUnitTest {
    private IndexPluginGeoMetadataMapper<Item> geoMetadataMapper;

    @Mock
    private IndexPluginMetadataMapper<Item> latitudeMapper;

    @Mock
    private IndexPluginMetadataMapper<Item> longitudeMapper;

    @Mock
    private Item item;

    @Before
    public void setUp() {
        geoMetadataMapper = new IndexPluginGeoMetadataMapper<>();
        geoMetadataMapper.latitudeMapper = latitudeMapper;
        geoMetadataMapper.longitudeMapper = longitudeMapper;
    }

    /**
     * Test mapping with valid latitude and longitude values.
     */
    @Test
    public void testMapWithValidCoordinates() {
        // Create test metadata values
        MetadataValueDTO latitude1 = createMetadataValueDTO("40.85864359881754");
        MetadataValueDTO latitude2 = createMetadataValueDTO("40.792034735376525");
        MetadataValueDTO longitude1 = createMetadataValueDTO("16.743178409469415");
        MetadataValueDTO longitude2 = createMetadataValueDTO("17.389286104550443");

        // Setup mock behavior
        when(latitudeMapper.map(item)).thenReturn(List.of(latitude1, latitude2));
        when(longitudeMapper.map(item)).thenReturn(List.of(longitude1, longitude2));

        // Execute the method under test
        List<String> result = geoMetadataMapper.map(item);

        // Verify results
        assertEquals(2, result.size());
        assertEquals("[16.743178409469415,40.85864359881754]", result.get(0));
        assertEquals("[17.389286104550443,40.792034735376525]", result.get(1));
    }

    /**
     * Test mapping with empty coordinate lists.
     */
    @Test
    public void testMapWithEmptyCoordinates() {
        // Setup mock behavior for empty lists
        when(latitudeMapper.map(item)).thenReturn(List.of());
        when(longitudeMapper.map(item)).thenReturn(List.of());

        // Execute the method under test
        List<String> result = geoMetadataMapper.map(item);

        // Verify results
        assertNull(result);
    }

    /**
     * Test mapping with null latitude.
     */
    @Test
    public void testMapWithNullLatitude() {
        // Setup mock behavior
        when(latitudeMapper.map(item)).thenReturn(null);
        when(longitudeMapper.map(item)).thenReturn(
            List.of(createMetadataValueDTO("16.743178409469415")));

        // Execute the method under test
        List<String> result = geoMetadataMapper.map(item);

        // Verify results
        assertNull(result);
    }

    /**
     * Test mapping with null longitude.
     */
    @Test
    public void testMapWithNullLongitude() {
        // Setup mock behavior
        when(latitudeMapper.map(item)).thenReturn(
            List.of(createMetadataValueDTO("40.85864359881754")));
        when(longitudeMapper.map(item)).thenReturn(null);

        // Execute the method under test
        List<String> result = geoMetadataMapper.map(item);

        // Verify results
        assertNull(result);
    }

    /**
     * Test mapping with different size arrays for latitude and longitude.
     */
    @Test
    public void testMapWithDifferentSizeArrays() {
        // Create test metadata values
        MetadataValueDTO latitude1 = createMetadataValueDTO("40.85864359881754");
        MetadataValueDTO latitude2 = createMetadataValueDTO("40.792034735376525");
        MetadataValueDTO longitude1 = createMetadataValueDTO("16.743178409469415");

        // Setup mock behavior with different size arrays
        when(latitudeMapper.map(item)).thenReturn(List.of(latitude1, latitude2));
        when(longitudeMapper.map(item)).thenReturn(List.of(longitude1));

        // Execute the method under test
        List<String> result = geoMetadataMapper.map(item);

        // Verify results
        assertNull(result);
    }

    /**
     * Test mapping with a custom longLatPattern format.
     */
    @Test
    public void testCustomLongLatPattern() {
        // Create test metadata values
        MetadataValueDTO latitude = createMetadataValueDTO("40.85864359881754");
        MetadataValueDTO longitude = createMetadataValueDTO("16.743178409469415");

        // Setup mock behavior
        when(latitudeMapper.map(item)).thenReturn(List.of(latitude));
        when(longitudeMapper.map(item)).thenReturn(List.of(longitude));

        // Set custom pattern
        geoMetadataMapper.longLatPattern = "POINT({0} {1})";

        // Execute the method under test
        List<String> result = geoMetadataMapper.map(item);

        // Verify results
        assertEquals(1, result.size());
        assertEquals("POINT(16.743178409469415 40.85864359881754)", result.get(0));
    }

    /**
     * Helper method to create a MetadataValueDTO with a given value.
     */
    private MetadataValueDTO createMetadataValueDTO(String value) {
        MetadataValueDTO dto = new MetadataValueDTO();
        dto.setValue(value);
        return dto;
    }
}