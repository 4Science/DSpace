/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.ProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.validation.model.ValidationError;
import org.dspace.validation.service.GeoJsonValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class GeoJsonValidationServiceImpl implements GeoJsonValidationService {

    private static final Logger logger = LoggerFactory.getLogger(GeoJsonValidationServiceImpl.class);

    private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonSchema getJsonSchema(String schemaURI) {
        try (FileInputStream fis = new FileInputStream(new File(schemaURI))) {
            return factory.getSchema(fis);
        } catch (ProcessingException | IOException e) {
            logger.error("Error while the file to json schema!", e);
            return null;
        }
    }

    @Override
    public List<ValidationError> validateJson(String schemaURL, String geoJson) {
        return validateSchema(schemaURL, geoJson);
    }

    private List<ValidationError> validateSchema(String schemaURL, String geoJson) {
        JsonNode jsonRoot = null;
        try {
            jsonRoot = mapper.readTree(geoJson);
        } catch (Exception e) {
            logger.error("Cannot convert the invalid json!", e);
            return List.of(new ValidationError(ERROR_GEOJSON_CONVERSION));
        }
        try {
            JsonSchema jsonSchema = this.getJsonSchema(schemaURL);
            if (jsonSchema == null) {
                return List.of(new ValidationError(ERROR_SCHEMA_INVALID));
            }
            jsonSchema.initializeValidators();
            Set<ValidationMessage> validation = jsonSchema.validate(jsonRoot);
            if (validation != null && !validation.isEmpty()) {
                return Stream.concat(
                    Stream.of(new ValidationError(ERROR_GEOJSON_INVALID)),
                    validation.stream().map(pm -> new ValidationError(pm.getMessage()))
                ).collect(Collectors.toList());
            }
        } catch (ProcessingException e) {
            logger.error("Cannot validate the provided json!", e);
            return List.of(new ValidationError(ERROR_GEOJSON_INVALID));
        }
        return this.validateCoordinates(jsonRoot);
    }

    private List<ValidationError> validateCoordinates(JsonNode jsonRoot) {
        boolean isGeometry = false;
        String type = jsonRoot.at(getTypePath(isGeometry)).asText();
        if (StringUtils.isEmpty(type)) {
            isGeometry = true;
            type = jsonRoot.at(getTypePath(isGeometry)).asText();
        }
        if (StringUtils.isEmpty(type)) {
            return List.of(new ValidationError(ERROR_GEOJSON_N0_TYPE));
        }

        JsonNode coordinates = jsonRoot.at(getCoordinatesPath(isGeometry));
        if (coordinates == null) {
            return List.of(new ValidationError(ERROR_GEOJSON_N0_COORDINATES));
        }

        if (StringUtils.equals(type, "Point")) {
            try {
                Object[] coordinatesObj =
                    extractCoordinates(mapper, coordinates, Object[].class);
                if (coordinatesObj != null && coordinatesObj.length > 0) {
                    validatePoint(coordinatesObj);
                }
            } catch (ValidationException e) {
                return List.of(new ValidationError(e.getMessage()));
            }
        } else if (StringUtils.equals(type, "MultiPoint")) {
            try {
                Object[][] coordinatesObj =
                    extractCoordinates(mapper, coordinates, Object[][].class);
                if (coordinatesObj != null && coordinatesObj.length > 1) {
                    validateMultipoint(coordinatesObj);
                }
            } catch (ValidationException e) {
                return List.of(new ValidationError(e.getMessage()));
            }
        } else if (StringUtils.equals(type, "Polygon")) {
            try {
                Object[][][] coordinatesObj =
                    extractCoordinates(mapper, coordinates, Object[][][].class);
                if (coordinatesObj != null && coordinatesObj.length == 1) {
                    validatePolygon(coordinatesObj[0]);
                }
            } catch (ValidationException e) {
                return List.of(new ValidationError(e.getMessage()));
            }
        } else {
            return List.of(new ValidationError(ERROR_GEOJSON_UNKNOWN_COORDINATES));
        }
        return List.of();
    }

    private static String getCoordinatesPath(boolean isGeometry) {
        return isGeometry ? "/coordinates" : "/geometry/coordinates";
    }

    private static String getTypePath(boolean isGeometry) {
        return isGeometry ? "/type" : "/geometry/type";
    }

    private void validatePolygon(Object[][] coordinatesObj) {
        for (Object[] objects : coordinatesObj) {
            validatePoint(objects);
        }
        Object[] firstPoint = coordinatesObj[0];
        Object[] lastPoint = coordinatesObj[coordinatesObj.length - 1];
        if (firstPoint.length != lastPoint.length || firstPoint.length != 2) {
            throw new ValidationException(ERROR_GEOJSON_INVALID_COORDINATES_SIZE);
        }
        boolean invalid = false;
        for (int i = 0; i < firstPoint.length && !invalid; i++) {
            invalid = Math.abs((double) firstPoint[i] - (double) lastPoint[i]) > 1e-16;
        }
        if (invalid) {
            throw new ValidationException(ERROR_GEOJSON_COORDINATES_NOT_POLYGON);
        }
    }

    private void validatePoint(Object[] coordinatesObj) {
        for (int i = 0; i + 1 < coordinatesObj.length; i += 2) {
            checkCoordinates(coordinatesObj, i);
        }
    }

    private void validateMultipoint(Object[][] coordinatesObj) {
        for (Object[] objects : coordinatesObj) {
            validatePoint(objects);
        }
    }

    private <T> T extractCoordinates(
        ObjectMapper objectMapper,
        JsonNode coordinates,
        Class<T> clazz
    ) {
        T coordinatesObj = null;
        try {
            coordinatesObj =
                objectMapper.readValue(coordinates.toString(), clazz);
        } catch (Exception e) {
            logger.error("Cannot parse coordinates!", e);
            throw new ValidationException(ERROR_GEOJSON_INVALID_COORDINATES);
        }
        return coordinatesObj;
    }

    private void checkCoordinates(
        Object[] coordinatesObj,
        int i
    ) {
        double lon = -181.0;
        double lat = -91.0;
        if (coordinatesObj[i] instanceof Integer) {
            lon = (double) (int) coordinatesObj[i];
        } else {
            lon = (double) coordinatesObj[i];
        }
        if (coordinatesObj[i + 1] instanceof Integer) {
            lat = (double) (int) coordinatesObj[i + 1];
        } else {
            lat = (double) coordinatesObj[i + 1];
        }
        if (isInvalidLon(lon) || isInvalidLat(lat)) {
            throw new ValidationException(ERROR_GEOJSON_INVALID_COORDINATES);
        }
    }

    private boolean isInvalidLat(double lat) {
        return lat < -90.0 || lat > 90.0;
    }

    private boolean isInvalidLon(double lon) {
        return lon < -180.0 || lon > 180.0;
    }

}
