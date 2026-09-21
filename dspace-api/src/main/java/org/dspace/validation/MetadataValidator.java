/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation;

import static org.dspace.validation.service.ValidationService.OPERATION_PATH_SECTIONS;
import static org.dspace.validation.util.ValidationUtils.addError;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.app.util.TypeBindUtils;
import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.dspace.core.Utils;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.services.ConfigurationService;
import org.dspace.validation.model.ValidationError;
import org.dspace.workflow.WorkflowItem;

/**
 * Execute three validation check on fields validation: - mandatory metadata
 * missing - regex missing match - authority required metadata missing
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4sciente.it)
 */
public class MetadataValidator implements SubmissionStepValidator {

    private static final String ERROR_VALIDATION_REQUIRED = "error.validation.required";

    private static final String ERROR_VALIDATION_AUTHORITY_REQUIRED = "error.validation.authority.required";

    private static final String ERROR_VALIDATION_REGEX = "error.validation.regex";

    private static final String ERROR_VALIDATION_NOT_REPEATABLE = "error.validation.notRepeatable";

    private static final Logger log = LogManager.getLogger(MetadataValidator.class);

    private DCInputsReader inputReader;

    private ItemService itemService;

    private ConfigurationService configurationService;

    private MetadataAuthorityService metadataAuthorityService;

    private String name;

    @Override
    public List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionStepConfig config) {

        List<ValidationError> errors = new ArrayList<>();

        DCInputSet inputConfig = getDCInputSet(config);
        String documentType = TypeBindUtils.getTypeBindValue(obj);

        // Get list of all field names (including qualdrop names) allowed for this dc.type
        List<String> allowedFieldNames = inputConfig.populateAllowedFieldNames(documentType);

        for (DCInput[] row : inputConfig.getFields()) {
            for (DCInput input : row) {
                if (isGroupInput(input)) {
                    validateNestedGroup(obj, config, input, documentType, errors, new HashSet<>());
                }
                String fieldKey = metadataAuthorityService.makeFieldKey(input.getSchema(), input.getElement(),
                        input.getQualifier());
                boolean isAuthorityControlled = metadataAuthorityService.isAuthorityAllowed(fieldKey, Constants.ITEM,
                        obj.getCollection());

                List<String> fieldsName = new ArrayList<String>();
                if (input.isQualdropValue()) {
                    boolean foundResult = false;
                    List<Object> inputPairs = input.getPairs();
                    //starting from the second element of the list and skipping one every time because the display
                    // values are also in the list and before the stored values.
                    for (int i = 1; i < inputPairs.size(); i += 2) {
                        String fullFieldname = input.getFieldName() + "." + (String) inputPairs.get(i);
                        List<MetadataValue> mdv = itemService.getMetadataByMetadataString(obj.getItem(), fullFieldname);

                        // Check the lookup list. If no other inputs of the same field name allow this type,
                        // then remove. This includes field name without qualifier.
                        if (!input.isAllowedFor(documentType) &&  (!allowedFieldNames.contains(fullFieldname)
                                && !allowedFieldNames.contains(input.getFieldName()))) {
                            removeMetadataValues(context, obj.getItem(), mdv);
                        } else {
                            validateMetadataValues(obj.getCollection(), mdv, input, config, isAuthorityControlled,
                                fieldKey, errors);
                            if (mdv.size() > 0 && (input.isVisible(DCInput.SUBMISSION_SCOPE) ||
                                    input.isVisible(DCInput.WORKFLOW_SCOPE))) {
                                foundResult = true;
                            }
                        }
                    }
                    if (input.isRequired() && !foundResult) {
                        // for this required qualdrop no value was found, add to the list of error fields
                        addError(errors, ERROR_VALIDATION_REQUIRED,
                            "/" + OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" +
                                input.getFieldName());
                    }

                } else {
                    String fieldName = input.getFieldName();
                    if (fieldName != null) {
                        fieldsName.add(fieldName);
                    }
                }

                for (String fieldName : fieldsName) {
                    boolean valuesRemoved = false;
                    List<MetadataValue> mdv = itemService.getMetadataByMetadataString(obj.getItem(), fieldName);
                    if (!input.isAllowedFor(documentType)) {
                        // Check the lookup list. If no other inputs of the same field name allow this type,
                        // then remove. Otherwise, do not
                        if (!(allowedFieldNames.contains(fieldName))) {
                            removeMetadataValues(context, obj.getItem(), mdv);
                            valuesRemoved = true;
                            log.debug("Stripping metadata values for " + input.getFieldName() + " on type "
                                    + documentType + " as it is allowed by another input of the same field " +
                                    "name");
                        } else {
                            log.debug("Not removing unallowed metadata values for " + input.getFieldName() + " on type "
                                    + documentType + " as it is allowed by another input of the same field " +
                                    "name");
                        }
                    }
                    validateMetadataValues(obj.getCollection(), mdv, input, config,
                        isAuthorityControlled, fieldKey, errors);
                    if ((input.isRequired() && mdv.size() == 0) && !isGroupInput(input)
                            && (input.isVisible(DCInput.SUBMISSION_SCOPE)
                            || (obj instanceof WorkflowItem && input.isVisible(DCInput.WORKFLOW_SCOPE)))
                            && !valuesRemoved) {
                        // Is the input required for *this* type? In other words, are we looking at a required
                        // input that is also allowed for this document type
                        if (input.isAllowedFor(documentType)) {
                            // since this field is missing add to list of error
                            // fields
                            addError(errors, ERROR_VALIDATION_REQUIRED,
                                "/" + OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" +
                                    input.getFieldName());
                        }
                    }
                }
            }
        }
        return errors;
    }

    /**
     * Validate the required sub-fields of a nested group ({@code group} / {@code inline-group}) input.
     *
     * The sub-fields of a group are described in a dedicated child form named
     * {@code <stepId>-<schema>-<element>-<qualifier>} which the top level validation loop does not traverse.
     * This method loads that child form and validates its required fields <b>per row</b>
     * (i.e. per metadata {@code place}), because nested groups are place-aligned and repeatable.
     *
     * The alignment placeholder {@link CrisConstants#PLACEHOLDER_PARENT_METADATA_VALUE} is treated as
     * an empty value, so a row that only carries placeholders on a required field is reported as missing.
     *
     * @param context       the DSpace context
     * @param obj           the in progress submission
     * @param config        the submission step configuration (its id is the top level form name)
     * @param groupInput    the group / inline-group input to validate
     * @param documentType  the type-bind value of the submission
     * @param errors        the list of validation errors to append to
     * @param visitedForms  the set of already visited child form names (cycle guard)
     */
    private void validateNestedGroup(InProgressSubmission<?> obj, SubmissionStepConfig config, DCInput groupInput,
                                     String documentType, List<ValidationError> errors, Set<String> visitedForms) {

        String childFormName = config.getId() + "-"
                + Utils.standardize(groupInput.getSchema(), groupInput.getElement(), groupInput.getQualifier(), "-");

        // guard against cyclic / self references between forms
        if (!visitedForms.add(childFormName)) {
            return;
        }

        DCInputSet childConfig;
        try {
            childConfig = getInputReader().getInputsByFormName(childFormName);
        } catch (DCInputsReaderException e) {
            log.warn("Unable to read nested submission form '{}' while validating group {}",
                     childFormName, groupInput.getFieldName(), e);
            return;
        }

        Item item = obj.getItem();
        // Collect the leaf sub-fields, recursing into nested-of-nested groups
        List<DCInput> leafInputs = new ArrayList<>();
        for (DCInput[] row : childConfig.getFields()) {
            for (DCInput childInput : row) {
                if (isGroupInput(childInput)) {
                    validateNestedGroup(obj, config, childInput, documentType, errors, visitedForms);
                } else {
                    leafInputs.add(childInput);
                }
            }
        }

        // Determine which rows (places) of the group are actually populated with a real value
        Set<Integer> populatedPlaces = new TreeSet<>();
        for (DCInput childInput : leafInputs) {
            String fieldName = childInput.getFieldName();
            if (fieldName == null) {
                continue;
            }
            for (MetadataValue value : itemService.getMetadataByMetadataString(item, fieldName)) {
                if (isRealValue(value.getValue())) {
                    populatedPlaces.add(value.getPlace());
                }
            }
        }

        // Group level requirement: when the group itself is required at least one populated row is expected
        if (groupInput.isRequired() && populatedPlaces.isEmpty()
                                    && isVisibleInScope(groupInput, obj)
                                    && groupInput.isAllowedFor(documentType)) {
            addError(errors, ERROR_VALIDATION_REQUIRED,
                    "/" + OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" + groupInput.getFieldName());
        }

        // Nothing to validate row by row when there are no populated rows
        if (populatedPlaces.isEmpty()) {
            return;
        }

        // Per-row requirement: a required sub-field must carry a real value on every populated row.
        // The error path includes the row index (place) so that clients can flag the specific
        // invalid row, consistently with the per-value errors already produced for repeatable fields.
        for (DCInput childInput : leafInputs) {
            if (!childInput.isRequired() || !childInput.isAllowedFor(documentType)
                                         || !isVisibleInScope(childInput, obj)) {
                continue;
            }
            String fieldName = childInput.getFieldName();
            if (fieldName == null) {
                continue;
            }
            Set<Integer> placesWithRealValue = new HashSet<>();
            for (MetadataValue value : itemService.getMetadataByMetadataString(item, fieldName)) {
                if (isRealValue(value.getValue())) {
                    placesWithRealValue.add(value.getPlace());
                }
            }
            for (Integer place : populatedPlaces) {
                if (!placesWithRealValue.contains(place)) {
                    addError(errors, ERROR_VALIDATION_REQUIRED,
                            "/" + OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" + fieldName + "/" + place);
                }
            }
        }
    }

    private boolean isGroupInput(DCInput input) {
        return StringUtils.equalsAny(input.getInputType(), "group", "inline-group");
    }

    private boolean isRealValue(String value) {
        return StringUtils.isNotBlank(value)
                && !StringUtils.equals(value, CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE);
    }

    private boolean isVisibleInScope(DCInput input, InProgressSubmission<?> obj) {
        return input.isVisible(DCInput.SUBMISSION_SCOPE)
                || (obj instanceof WorkflowItem && input.isVisible(DCInput.WORKFLOW_SCOPE));
    }

    private DCInputSet getDCInputSet(SubmissionStepConfig config) {
        try {
            return getInputReader().getInputsByFormName(config.getId());
        } catch (DCInputsReaderException e) {
            throw new RuntimeException(e);
        }
    }

    private void validateMetadataValues(Collection collection, List<MetadataValue> metadataValues, DCInput input,
        SubmissionStepConfig config, boolean isAuthorityControlled, String fieldKey, List<ValidationError> errors) {
        // if the filed is not repeatable, it should have only one value
        if (!input.isRepeatable() && metadataValues.size() > 1) {
            for (int i = 0; i < metadataValues.size(); i++) {
                addError(errors, ERROR_VALIDATION_NOT_REPEATABLE,
                        "/" + OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" + input.getFieldName() + "/" + i);
            }
        }

        for (MetadataValue md : metadataValues) {
            if (! (input.validate(md.getValue()))) {
                addError(errors, ERROR_VALIDATION_REGEX,
                    "/" + OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" +
                        input.getFieldName() + "/" + md.getPlace());
            }
            if (isAuthorityControlled) {
                String authKey = md.getAuthority();
                if (metadataAuthorityService.isAuthorityRequired(md.getMetadataField(), Constants.ITEM, collection) &&
                    StringUtils.isBlank(authKey)) {
                    addError(errors, ERROR_VALIDATION_AUTHORITY_REQUIRED,
                        "/" + OPERATION_PATH_SECTIONS + "/" + config.getId() +
                            "/" + input.getFieldName() + "/" + md.getPlace());
                }
            }
        }
    }

    private void removeMetadataValues(Context context, Item item, List<MetadataValue> metadataValues) {
        try {
            itemService.removeMetadataValues(context, item, metadataValues);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public void setMetadataAuthorityService(MetadataAuthorityService metadataAuthorityService) {
        this.metadataAuthorityService = metadataAuthorityService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public DCInputsReader getInputReader() {
        if (inputReader == null) {
            try {
                inputReader = new DCInputsReader();
            } catch (DCInputsReaderException e) {
                log.error(e.getMessage(), e);
            }
        }
        return inputReader;
    }

    public void setInputReader(DCInputsReader inputReader) {
        this.inputReader = inputReader;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
