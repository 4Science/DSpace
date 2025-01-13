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
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.services.model.Request;
import org.dspace.validation.model.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator that checks validity of the submitted external file,
 * this could be a file inside the upload folder, or an external file retrievable by link.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class ExternalFileUploadValidator implements SubmissionStepValidator {

    public static final String ERROR_KEY = "external-upload-error";
    private static final Logger log = LoggerFactory.getLogger(ExternalFileUploadValidator.class);

    private String name;

    private HttpServletRequest getCurrentRequest() {
        Request req = DSpaceServicesFactory.getInstance().getRequestService().getCurrentRequest();
        if (req == null) {
            log.warn("No current request found!");
            return null;
        }
        return req.getHttpServletRequest();
    }

    @Override
    public List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionStepConfig config) {
        if (getCurrentRequest() == null) {
            return List.of();
        }
        return Optional.ofNullable((String) getCurrentRequest().getAttribute(ERROR_KEY))
                .map(errorMessage -> addError(errorMessage, config))
                       .map(List::of)
                       .orElseGet(List::of);
    }

    protected ValidationError addError(String message, SubmissionStepConfig config) {
        ValidationError error = new ValidationError(message);
        error.getPaths().add("/" + OPERATION_PATH_SECTIONS + "/" + config.getId());
        return error;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
