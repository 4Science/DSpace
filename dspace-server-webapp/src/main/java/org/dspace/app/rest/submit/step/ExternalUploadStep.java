/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.util.Set;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.ExternalFileUpload;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.factory.PatchOperationFactory;
import org.dspace.app.rest.submit.factory.impl.PatchOperation;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class ExternalUploadStep extends AbstractProcessingStep {

    private static final Pattern PATH_OPERATION_PATTERN = Pattern.compile("(?:/)(?<op>\\w+$)");
    private static final Set<String> OPERATIONS_ALLOWED = Set.of("source");

    /**
     * This step should not provide any data to the client, is only a submission point to start data retrieving of a
     * given url, it should be used with the {@link UploadStep} to get updated bitstreams.
     *
     * @param submissionService
     *            the submission service
     * @param obj
     *            the in progress submission
     * @param config
     *            the submission step configuration
     * @return ExternalFileUpload object
     * @throws Exception
     */
    @Override
    public ExternalFileUpload getData(SubmissionService submissionService, InProgressSubmission obj,
                                      SubmissionStepConfig config) throws Exception {
        return new ExternalFileUpload();
    }

    @Override
    public void doPatchProcessing(Context context, HttpServletRequest currentRequest, InProgressSubmission source,
                                  Operation op, SubmissionStepConfig stepConf) throws Exception {
        if (!canProcess(op)) {
            throw new UnprocessableEntityException(
                "The path " + op.getPath() + " is not supported by the operation " + op.getOp()
            );
        }

        PatchOperation<?> patchOperation =
            new PatchOperationFactory().instanceOf(getPathOperation(op.getPath()), op.getOp());
        patchOperation.perform(context, currentRequest, source, op);
    }

    protected boolean canProcess(Operation op) {
        return "add".equals(op.getOp()) &&
            isOperationAllowed(op.getPath()) &&
            hasValidFilePath(op.getValue());
    }

    private boolean hasValidFilePath(Object value) {
        if (value == null || !(value instanceof String)) {
            return false;
        }
        return true;
    }

    protected boolean isOperationAllowed(String path) {
        return OPERATIONS_ALLOWED.contains(getPathOperation(path));
    }

    protected String getPathOperation(String path) {
        return PATH_OPERATION_PATTERN.matcher(path).results().map(mr -> mr.group(1)).findFirst().orElse(null);
    }
}
