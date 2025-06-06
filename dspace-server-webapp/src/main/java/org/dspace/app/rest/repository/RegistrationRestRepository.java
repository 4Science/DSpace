/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.dspace.eperson.service.CaptchaService.REGISTER_ACTION;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.RegistrationRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.ResourcePatch;
import org.dspace.app.rest.utils.Utils;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.InvalidReCaptchaException;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.RegistrationTypeEnum;
import org.dspace.eperson.service.AccountService;
import org.dspace.eperson.service.CaptchaService;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.RegistrationDataService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository that is responsible for managing Registration Rest objects
 */
@Component(RegistrationRest.CATEGORY + "." + RegistrationRest.PLURAL_NAME)
public class RegistrationRestRepository extends DSpaceRestRepository<RegistrationRest, Integer> {

    private static Logger log = LogManager.getLogger(RegistrationRestRepository.class);

    public static final String TOKEN_QUERY_PARAM = "token";
    public static final String TYPE_QUERY_PARAM = "accountRequestType";
    public static final String TYPE_REGISTER = RegistrationTypeEnum.REGISTER.toString().toLowerCase();
    public static final String TYPE_FORGOT = RegistrationTypeEnum.FORGOT.toString().toLowerCase();

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private RegistrationDataService registrationDataService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private Utils utils;

    @Autowired
    private ResourcePatch<RegistrationData> resourcePatch;

    @Override
    @PreAuthorize("permitAll()")
    public RegistrationRest findOne(Context context, Integer integer) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    public Page<RegistrationRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    public RegistrationRest createAndReturn(Context context) {
        HttpServletRequest request = requestService.getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest;
        String captchaToken = request.getHeader("X-Recaptcha-Token");
        boolean verificationEnabled = configurationService.getBooleanProperty("registration.verification.enabled");

        if (verificationEnabled) {
            try {
                captchaService.processResponse(captchaToken, REGISTER_ACTION);
            } catch (InvalidReCaptchaException e) {
                throw new InvalidReCaptchaException(e.getMessage(), e);
            }
        }

        try {
            ServletInputStream input = request.getInputStream();
            registrationRest = mapper.readValue(input, RegistrationRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body.", e1);
        }
        if (StringUtils.isBlank(registrationRest.getEmail())) {
            throw new UnprocessableEntityException("The email cannot be omitted from the Registration endpoint");
        }
        if (Objects.nonNull(registrationRest.getGroups()) && registrationRest.getGroups().size() > 0) {
            try {
                if (Objects.isNull(context.getCurrentUser())
                    || (!authorizeService.isAdmin(context)
                    && !hasPermission(context, registrationRest.getGroups()))) {
                    throw new AccessDeniedException("Only admin users can invite new users to join groups");
                }
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }
        String accountType = request.getParameter(TYPE_QUERY_PARAM);
        if (StringUtils.isBlank(accountType) ||
            (!accountType.equalsIgnoreCase(TYPE_FORGOT) && !accountType.equalsIgnoreCase(TYPE_REGISTER))) {
            throw new IllegalArgumentException(String.format("Needs query param '%s' with value %s or %s indicating " +
                                                                 "what kind of registration request it is",
                                                             TYPE_QUERY_PARAM, TYPE_FORGOT, TYPE_REGISTER));
        }
        EPerson eperson = null;
        try {
            eperson = ePersonService.findByEmail(context, registrationRest.getEmail());
        } catch (SQLException e) {
            log.error("Something went wrong retrieving EPerson for email: " + registrationRest.getEmail(), e);
        }
        if (eperson != null && accountType.equalsIgnoreCase(TYPE_FORGOT)) {
            try {
                if (!AuthorizeUtil.authorizeForgotPassword()) {
                    throw new AccessDeniedException("Password reset is not allowed!");
                }
                if (!AuthorizeUtil.authorizeUpdatePassword(context, eperson.getEmail())) {
                    throw new DSpaceBadRequestException("Password cannot be updated for the given EPerson with email: "
                                                            + eperson.getEmail());
                }
                accountService.sendForgotPasswordInfo(context, registrationRest.getEmail(),
                                                      registrationRest.getGroups());
            } catch (SQLException | IOException | MessagingException | AuthorizeException e) {
                log.error("Something went wrong with sending forgot password info email: "
                              + registrationRest.getEmail(), e);
            }
        } else if (accountType.equalsIgnoreCase(TYPE_REGISTER)) {
            try {
                String email = registrationRest.getEmail();
                if (!AuthorizeUtil.authorizeNewAccountRegistration(context, request)) {
                    throw new AccessDeniedException(
                        "Registration is disabled, you are not authorized to create a new Authorization");
                }

                if (!authenticationService.canSelfRegister(context, request, registrationRest.getEmail())) {
                    throw new UnprocessableEntityException(
                        String.format("Registration is not allowed with email address" +
                                          " %s", email));
                }

                accountService.sendRegistrationInfo(context, registrationRest.getEmail(), registrationRest.getGroups());
            } catch (SQLException | IOException | MessagingException | AuthorizeException e) {
                log.error("Something went wrong with sending registration info email: "
                              + registrationRest.getEmail(), e);
            }
        }
        return null;
    }

    private boolean hasPermission(Context context, List<UUID> groups) throws SQLException {
        for (UUID groupUuid : groups) {
            Group group = groupService.find(context, groupUuid);
            if (Objects.nonNull(group)) {
                DSpaceObject obj = groupService.getParentObject(context, group);
                if (!authorizeService.isAdmin(context, obj)) {
                    return false;
                }
            } else {
                throw new UnprocessableEntityException("Group uuid " + groupUuid.toString() + " not valid!");
            }
        }
        return true;
    }

    /**
     * This method will find the RegistrationRest object that is associated with the token given
     *
     * @param token The token to be found and for which a RegistrationRest object will be found
     * @return A RegistrationRest object for the given token
     * @throws SQLException       If something goes wrong
     * @throws AuthorizeException If something goes wrong
     */
    @SearchRestMethod(name = "findByToken")
    public RegistrationRest findByToken(@Parameter(value = "token", required = true) String token)
        throws SQLException, AuthorizeException {
        Context context = obtainContext();
        RegistrationData registrationData = registrationDataService.findByToken(context, token);
        if (registrationData == null) {
            throw new ResourceNotFoundException("The token: " + token + " couldn't be found");
        }
        return converter.toRest(registrationData, utils.obtainProjection());
    }

    @Override
    public RegistrationRest patch(
        HttpServletRequest request, String apiCategory, String model, Integer id, Patch patch
    ) throws UnprocessableEntityException, DSpaceBadRequestException {
        if (id == null || id <= 0) {
            throw new BadRequestException("The id of the registration cannot be null or negative");
        }
        if (patch == null || patch.getOperations() == null || patch.getOperations().isEmpty()) {
            throw new BadRequestException("Patch request is incomplete: cannot find operations");
        }
        String token = request.getParameter("token");
        if (token == null || token.trim().isBlank()) {
            throw new AccessDeniedException("The token is required");
        }
        Context context = obtainContext();

        validateToken(context, token);

        try {
            resourcePatch.patch(context, registrationDataService.find(context, id), patch.getOperations());
            context.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return null;
    }

    private void validateToken(Context context, String token) {
        try {
            RegistrationData registrationData =
                registrationDataService.findByToken(context, token);
            if (registrationData == null || !registrationDataService.isValid(registrationData)) {
                throw new AccessDeniedException("The token is invalid");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setCaptchaService(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @Override
    public Class<RegistrationRest> getDomainClass() {
        return RegistrationRest.class;
    }

}
