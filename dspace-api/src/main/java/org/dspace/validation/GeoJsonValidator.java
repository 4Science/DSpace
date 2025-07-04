/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation;

import static org.dspace.validation.service.ValidationService.OPERATION_PATH_SECTIONS;

import java.util.List;

import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.validation.model.ValidationError;
import org.dspace.validation.service.GeoJsonValidationService;
import org.dspace.validation.service.factory.GeoJsonValidationServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator that checks correctness of a metadata {@link MetadataField} containing
 * a geojson value.
 * This validator validate that field against a jsonschema and then checks coordinates values.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class GeoJsonValidator implements SubmissionStepValidator {

    private static final Logger logger = LoggerFactory.getLogger(GeoJsonValidator.class);

    private String schemaURL;
    private String metadataField = "";
    private String name;
    private final GeoJsonValidationService validationService =
        GeoJsonValidationServiceFactory.getInstance().getValidationService();
    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    public GeoJsonValidator(String schemaURL) {
        this.schemaURL = schemaURL;
    }

    @Override
    public List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionStepConfig config) {
        Item item = obj.getItem();
        String geoJson = this.itemService.getMetadata(item, metadataField);

        if (geoJson == null) {
            return List.of();
        }

        String errorPath = "/" + OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" + "geojson";
        List<ValidationError> validationErrors = this.validationService.validateJson(schemaURL, geoJson);
        validationErrors.forEach(error -> error.getPaths().add(errorPath));

        return validationErrors;
    }

    public String getSchemaURL() {
        return schemaURL;
    }

    public void setSchemaURL(String schemaURL) {
        this.schemaURL = schemaURL;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMetadataField() {
        return metadataField;
    }

    public void setMetadataField(String metadataField) {
        this.metadataField = metadataField;
    }

}
