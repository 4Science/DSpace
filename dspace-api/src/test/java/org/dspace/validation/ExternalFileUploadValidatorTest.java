/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.AbstractDSpaceTest;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.services.model.Request;
import org.dspace.validation.model.ValidationError;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class ExternalFileUploadValidatorTest extends AbstractDSpaceTest {

    private ExternalFileUploadValidator validator;

    @Before
    public void setUp() {
        validator = new ExternalFileUploadValidator();
        validator.setName("external-upload");
    }

    @Test
    public void testValidateNoCurrentRequest() {
        try (MockedStatic<DSpaceServicesFactory> dspaceServicesFactoryMock = Mockito.mockStatic(
            DSpaceServicesFactory.class)) {
            RequestService mockRequestService = mock(RequestService.class);

            // Mock DSpaceServicesFactory and its method chain
            dspaceServicesFactoryMock.when(DSpaceServicesFactory::getInstance)
                                     .thenReturn(mock(DSpaceServicesFactory.class));
            when(DSpaceServicesFactory.getInstance().getRequestService()).thenReturn(mockRequestService);
            when(mockRequestService.getCurrentRequest()).thenReturn(null);

            List<ValidationError> errors = validator.validate(mock(Context.class), mock(InProgressSubmission.class),
                                                              mock(SubmissionStepConfig.class));

            assertEquals(0, errors.size());
        }
    }


    @Test
    public void testValidateWithErrorInRequest() {
        try (MockedStatic<DSpaceServicesFactory> dspaceServicesFactoryMock = Mockito.mockStatic(
            DSpaceServicesFactory.class)) {
            RequestService mockRequestService = mock(RequestService.class);
            Request mockRequest = mock(Request.class);
            HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
            SubmissionStepConfig mockConfig = mock(SubmissionStepConfig.class);

            // Setup mocks
            dspaceServicesFactoryMock.when(DSpaceServicesFactory::getInstance)
                                     .thenReturn(mock(DSpaceServicesFactory.class));
            when(DSpaceServicesFactory.getInstance().getRequestService()).thenReturn(mockRequestService);
            when(mockRequestService.getCurrentRequest()).thenReturn(mockRequest);
            when(mockRequest.getHttpServletRequest()).thenReturn(mockHttpServletRequest);
            when(mockHttpServletRequest.getAttribute("external-upload-error")).thenReturn("File upload failed");
            when(mockConfig.getId()).thenReturn("upload");

            List<ValidationError> errors =
                validator.validate(mock(Context.class), mock(InProgressSubmission.class), mockConfig);

            assertEquals(1, errors.size());
            ValidationError error = errors.get(0);
            assertEquals("File upload failed", error.getMessage());
            assertEquals("/sections/upload", error.getPaths().get(0));
        }
    }

    @Test
    public void testValidateWithoutErrorInRequest() {
        try (MockedStatic<DSpaceServicesFactory> dspaceServicesFactoryMock = Mockito.mockStatic(
            DSpaceServicesFactory.class)) {
            RequestService mockRequestService = mock(RequestService.class);
            Request mockRequest = mock(Request.class);
            HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);

            // Setup mocks
            dspaceServicesFactoryMock.when(DSpaceServicesFactory::getInstance)
                                     .thenReturn(mock(DSpaceServicesFactory.class));
            when(DSpaceServicesFactory.getInstance().getRequestService()).thenReturn(mockRequestService);
            when(mockRequestService.getCurrentRequest()).thenReturn(mockRequest);
            when(mockRequest.getHttpServletRequest()).thenReturn(mockHttpServletRequest);
            when(mockHttpServletRequest.getAttribute("external-upload-error")).thenReturn(null);

            List<ValidationError> errors = validator.validate(mock(Context.class), mock(InProgressSubmission.class),
                                                              mock(SubmissionStepConfig.class));

            assertEquals(0, errors.size());
        }
    }
}
