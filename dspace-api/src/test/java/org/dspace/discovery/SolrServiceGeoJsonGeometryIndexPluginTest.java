/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.mapper.geomap.geojson.GeoJSONMapper;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.validation.model.ValidationError;
import org.dspace.validation.service.GeoJsonValidationService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link SolrServiceGeoJsonGeometryIndexPlugin} class
 */
public class SolrServiceGeoJsonGeometryIndexPluginTest {

    private static final String INDEX_FIELD_NAME = "geo_p";
    private static final String SCHEMA_URL = "schema/geojson/Geometry.json";
    private static final String POINT_COORDINATES = "[16.743178409469415,40.85864359881754]";
    private static final String MULTIPOINT_COORDINATES_1 = "[16.743178409469415,40.85864359881754]";
    private static final String MULTIPOINT_COORDINATES_2 = "[17.389286104550443,40.792034735376525]";
    private static final String CUSTOM_PATTERN = "\\[(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)\\]";

    @Mock
    private ItemService itemService;

    @Mock
    private IndexPluginMapper<Item, List<String>> metadataMapper;

    private GeoJsonValidationService validationService;
    private SolrServiceGeoJsonGeometryIndexPlugin plugin;
    private GeoJSONMapper jsonMapper;

    private Context context;
    private IndexableItem indexableItem;
    private Item item;
    private SolrInputDocument document;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // Create mocks
        validationService = mock(GeoJsonValidationService.class);
        jsonMapper = new GeoJSONMapper();

        // Create plugin with constructor parameters and manually set validation service
        plugin = new SolrServiceGeoJsonGeometryIndexPlugin(
            metadataMapper, INDEX_FIELD_NAME, SCHEMA_URL, GeoJSONMapper.POINT_FORMAT_PATTERN
        ) {
            @Override
            protected GeoJsonValidationService getGeoJsonValidationService() {
                return validationService;
            }
        };

        // Setup common test objects
        context = mock(Context.class);
        indexableItem = mock(IndexableItem.class);
        item = mock(Item.class);
        document = new SolrInputDocument();

        when(indexableItem.getIndexedObject()).thenReturn(item);
    }

    /**
     * Test with valid point coordinates.
     */
    @Test
    public void testWithValidPointCoordinates() {
        // Setup test data
        List<String> coordinates = Collections.singletonList(POINT_COORDINATES);
        String expectedGeoJSON = "{\"type\":\"Point\",\"coordinates\":[16.743178409469415,40.85864359881754]}";

        // Setup mocks
        when(metadataMapper.map(item)).thenReturn(coordinates);
        when(validationService.validateJson(eq(SCHEMA_URL), anyString())).thenReturn(Collections.emptyList());

        // Execute method under test
        plugin.additionalIndex(context, indexableItem, document);

        // Verify results
        assertEquals(expectedGeoJSON, document.getFieldValue(INDEX_FIELD_NAME));
        verify(validationService, times(1)).validateJson(eq(SCHEMA_URL), anyString());
    }

    /**
     * Test with valid multipoint coordinates.
     */
    @Test
    public void testWithValidMultipointCoordinates() {
        // Setup test data
        List<String> coordinates = List.of(MULTIPOINT_COORDINATES_1, MULTIPOINT_COORDINATES_2);
        String expectedGeoJSON = "{\"type\":\"MultiPoint\",\"coordinates\":[[16.743178409469415,40.85864359881754]," +
            "[17.389286104550443,40.792034735376525]]}";

        // Setup mocks
        when(metadataMapper.map(item)).thenReturn(coordinates);
        when(validationService.validateJson(eq(SCHEMA_URL), anyString())).thenReturn(Collections.emptyList());

        // Execute method under test
        plugin.additionalIndex(context, indexableItem, document);

        // Verify results
        assertEquals(expectedGeoJSON, document.getFieldValue(INDEX_FIELD_NAME));
        verify(validationService, times(1)).validateJson(eq(SCHEMA_URL), anyString());
    }

    /**
     * Test with non-IndexableItem object.
     */
    @Test
    public void testWithNonIndexableItem() {
        // Setup test data
        IndexableCollection indexableCollection = mock(IndexableCollection.class);

        // Execute method under test
        plugin.additionalIndex(context, indexableCollection, document);

        // Verify results
        assertEquals(0, document.getFieldNames().size());
        verify(metadataMapper, never()).map(any());
        verify(validationService, never()).validateJson(anyString(), anyString());
    }

    /**
     * Test with empty coordinates list.
     */
    @Test
    public void testWithEmptyCoordinatesList() {
        // Setup mocks
        when(metadataMapper.map(item)).thenReturn(Collections.emptyList());

        // Execute method under test
        plugin.additionalIndex(context, indexableItem, document);

        // Verify results
        assertEquals(0, document.getFieldNames().size());
        verify(validationService, never()).validateJson(anyString(), anyString());
    }

    /**
     * Test with invalid GeoJSON format.
     */
    @Test
    public void testWithInvalidGeoJSONFormat() {
        // Setup test data
        List<String> invalidCoordinates = Collections.singletonList("invalid-format");

        // Setup mocks
        when(metadataMapper.map(item)).thenReturn(invalidCoordinates);

        // Execute method under test
        plugin.additionalIndex(context, indexableItem, document);

        // Verify results
        assertEquals(0, document.getFieldNames().size());
        verify(validationService, never()).validateJson(anyString(), anyString());
    }

    /**
     * Test with validation errors.
     */
    @Test
    public void testWithValidationErrors() {
        // Setup test data
        List<String> coordinates = Collections.singletonList(POINT_COORDINATES);
        List<ValidationError> validationErrors = new ArrayList<>();
        validationErrors.add(mock(ValidationError.class));

        // Setup mocks
        when(metadataMapper.map(item)).thenReturn(coordinates);
        when(validationService.validateJson(eq(SCHEMA_URL), anyString())).thenReturn(validationErrors);

        // Execute method under test
        plugin.additionalIndex(context, indexableItem, document);

        // Verify results
        assertEquals(0, document.getFieldNames().size());
       verify(validationService, times(1)).validateJson(eq(SCHEMA_URL), anyString());
    }

    /**
     * Test with custom coordinates pattern.
     */
    @Test
    public void testWithCustomCoordinatesPattern() {
        // Setup test data
        List<String> coordinates = Collections.singletonList(POINT_COORDINATES);
        String expectedGeoJSON = "{\"type\":\"Point\",\"coordinates\":[16.743178409469415,40.85864359881754]}";

        // Create plugin with custom pattern
        SolrServiceGeoJsonGeometryIndexPlugin customPlugin =
            new SolrServiceGeoJsonGeometryIndexPlugin(metadataMapper, INDEX_FIELD_NAME, SCHEMA_URL, CUSTOM_PATTERN) {
                @Override
                protected GeoJsonValidationService getGeoJsonValidationService() {
                    return validationService;
                }
            };

        // Setup mocks
        when(metadataMapper.map(item)).thenReturn(coordinates);
        when(validationService.validateJson(eq(SCHEMA_URL), anyString())).thenReturn(Collections.emptyList());

        // Execute method under test
        customPlugin.additionalIndex(context, indexableItem, document);

        // Verify results
        assertEquals(expectedGeoJSON, document.getFieldValue(INDEX_FIELD_NAME));
        verify(validationService, times(1)).validateJson(eq(SCHEMA_URL), anyString());
    }
}