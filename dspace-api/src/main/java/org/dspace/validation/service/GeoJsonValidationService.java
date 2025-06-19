/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation.service;

import java.util.List;

import org.dspace.validation.model.ValidationError;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public interface GeoJsonValidationService {

    String ERROR_SCHEMA_INVALID = "error.validation.geojson.schema.invalid";
    String ERROR_GEOJSON_INVALID = "error.validation.geojson.invalid";
    String ERROR_GEOJSON_CONVERSION = "error.validation.geojson.conversion";
    String ERROR_GEOJSON_N0_TYPE = "error.validation.geojson.type.notfound";
    String ERROR_GEOJSON_UNKNOWN_COORDINATES = "error.validation.geojson.type.unknown";
    String ERROR_GEOJSON_N0_COORDINATES = "error.validation.geojson.coordinates.notfound";
    String ERROR_GEOJSON_INVALID_COORDINATES = "error.validation.geojson.coordinates.invalid";
    String ERROR_GEOJSON_INVALID_COORDINATES_SIZE = "error.validation.geojson.coordinates.invalidsize";
    String ERROR_GEOJSON_COORDINATES_NOT_POLYGON = "error.validation.geojson.coordinates.notpolygon";

    List<ValidationError> validateJson(String schemaURL, String geoJson);

}
