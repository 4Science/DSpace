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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.validation.model.ValidationError;

/**
 * Validator that checks correctness of a metadata {@link MetadataField} containing
 * a geomap field value.
 * This validator validate that field against a common regex specified inside the bean declaration.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class GeoMapValidator implements GlobalSubmissionValidator {

    public static final String DEFAULT_REGEX = "((-?\\d+.\\d+),(-?\\d+.\\d+);?)+";

    private static final String GEOMAP_TYPE = "geomap";

    private static final String ERROR_INVALID_FORMAT = "error.validation.regex.geomap";


    private final Pattern pattern;
    private DCInputsReader inputReader;
    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    public GeoMapValidator() {
        this(DEFAULT_REGEX);
    }

    public GeoMapValidator(String regex) {
        super();
        pattern = Pattern.compile(regex);
    }


    @Override
    public List<ValidationError> validate(
        Context context, InProgressSubmission<?> obj, SubmissionConfig config
    ) {
        return StreamSupport.stream(config.spliterator(), false)
                            .filter(step -> SubmissionStepConfig.INPUT_FORM_STEP_NAME.equals(step.getType()))
                            .flatMap(step -> validate(context, obj, step))
                            .collect(Collectors.toList());
    }

    public Stream<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionStepConfig config) {
        DCInputSet inputConfig = getDCInputSet(config);

        Item item = obj.getItem();
        return Stream.of(inputConfig.getFields())
                     .flatMap(row ->
                                  Stream.of(row)
                                        .filter(GeoMapValidator::isGeomapType)
                                        .map(input -> getMetadata(input, item))
                                        .filter(metadataValues -> !metadataValues.isEmpty())
                                        .flatMap(metadataValues -> validateMetadata(metadataValues, config))
                     );
    }

    private Stream<ValidationError> validateMetadata(List<MetadataValue> metadataValues, SubmissionStepConfig config) {
        return metadataValues
            .stream()
            .map(metadataValue -> validateMetadata(metadataValue, config))
            .filter(Objects::nonNull);
    }

    private ValidationError validateMetadata(MetadataValue metadataValue, SubmissionStepConfig config) {
        if (metadataValue == null) {
            return null;
        }

        String value = metadataValue.getValue();
        if (StringUtils.isBlank(value)) {
            return null;
        }

        Matcher matcher = pattern.matcher(value);
        if (!matcher.matches()) {
            ValidationError error = new ValidationError();
            error.setMessage(ERROR_INVALID_FORMAT);
            error.getPaths().add(
                "/" + OPERATION_PATH_SECTIONS + "/" + config.getId() +
                    "/" + metadataValue.getMetadataField().toString('.') + "/" + metadataValue.getPlace()
            );
            return error;
        }
        return null;
    }

    private List<MetadataValue> getMetadata(DCInput input, Item item) {
        return itemService.getMetadata(item, input.getSchema(), input.getElement(), input.getQualifier(), Item.ANY);
    }

    private static boolean isGeomapType(DCInput input) {
        return GEOMAP_TYPE.equals(input.getInputType());
    }

    protected DCInputSet getDCInputSet(SubmissionStepConfig config) {
        try {
            return getInputReader().getInputsByFormName(config.getId());
        } catch (DCInputsReaderException e) {
            throw new RuntimeException(e);
        }
    }

    public DCInputsReader getInputReader() {
        if (inputReader == null) {
            try {
                inputReader = new DCInputsReader();
            } catch (DCInputsReaderException e) {
                throw new RuntimeException(e);
            }
        }
        return inputReader;
    }
}
