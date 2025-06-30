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
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.submit.model.AccessConditionConfiguration;
import org.dspace.submit.model.AccessConditionConfigurationService;
import org.dspace.submit.model.AccessConditionOption;
import org.dspace.validation.model.ValidationError;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class that executes the validation for the item access conditions
 *
 * @author Daniele Ninfo (daniele.ninfo at 4science.com)
 */
public class AccessConditionValidator implements SubmissionStepValidator {

    private static final String ERROR_VALIDATION_ACCESSCONDITIONS_REQUIRED =
            "error.validation.accessconditions.required";

    private AccessConditionConfiguration accessConditionConfiguration;
    private AccessConditionConfigurationService accessConditionConfigurationService;

    @Autowired
    private AuthorizeService authorizeService;

    private String name;

    @Override
    public List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionStepConfig config) {
        try {
            accessConditionConfiguration =
                    accessConditionConfigurationService.getAccessConfigurationById(config.getId());
            return performValidation(context, obj.getItem(), config);
        } catch (SQLException e) {
            throw new SQLRuntimeException("Failed to get access configuration for step " + config.getId(), e);
        }
    }

    private List<ValidationError> performValidation(Context context, Item item,
            SubmissionStepConfig config) throws SQLException {
        List<ValidationError> errors;
        if (accessConditionConfiguration.isRequired() && !isAccessConditionPresent(context, item)) {
            errors = new ArrayList<ValidationError>(1);
            addError(errors, ERROR_VALIDATION_ACCESSCONDITIONS_REQUIRED,
                    "/" + OPERATION_PATH_SECTIONS + "/" + config.getId());
        } else {
            errors = List.of();
        }
        return errors;
    }

    private boolean isAccessConditionPresent(Context context, Item item) throws SQLException {
        List<String> optionsNames = getOptionsNames();
        return authorizeService.getPolicies(context, item)
                               .stream()
                               .anyMatch(policy -> optionsNames.contains(policy.getRpName()));
    }

    private List<String> getOptionsNames() {
        List<AccessConditionOption> options = accessConditionConfiguration.getOptions();
        if (options == null) {
            return new ArrayList<>();
        }
        return options.stream()
            .map(AccessConditionOption::getName)
            .collect(Collectors.toList());
    }

    public void setAccessConditionConfigurationService(
            AccessConditionConfigurationService accessConditionConfigurationService) {
        this.accessConditionConfigurationService = accessConditionConfigurationService;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}