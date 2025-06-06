/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.DiscoverableEndpointsService;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.EPersonNameNotProvidedException;
import org.dspace.app.rest.exception.PasswordNotValidException;
import org.dspace.app.rest.exception.RESTEmptyWorkflowGroupException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ValidatePasswordService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.EmptyWorkflowGroupException;
import org.dspace.eperson.Group;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.service.AccountService;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.RegistrationDataService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;


/**
 * This is the repository responsible to manage EPerson Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(EPersonRest.CATEGORY + "." + EPersonRest.PLURAL_NAME)
public class EPersonRestRepository extends DSpaceObjectRestRepository<EPerson, EPersonRest>
                                   implements InitializingBean {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ValidatePasswordService validatePasswordService;

    @Autowired
    private RegistrationDataService registrationDataService;

    @Autowired
    private GroupService groupService;

    private final EPersonService es;


    public EPersonRestRepository(EPersonService dsoService) {
        super(dsoService);
        this.es = dsoService;
    }

    @Override
    protected EPersonRest createAndReturn(Context context)
            throws AuthorizeException {
        // this need to be revisited we should receive an EPersonRest as input
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        EPersonRest epersonRest = null;
        try {
            epersonRest = mapper.readValue(req.getInputStream(), EPersonRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("error parsing the body... maybe this is not the right error code");
        }
        String token = req.getParameter("token");
        // If a token is available, we'll swap to the execution that is token based
        if (StringUtils.isNotBlank(token)) {
            try {
                return createAndReturn(context, epersonRest, token);
            } catch (SQLException e) {
                log.error("Something went wrong in the creation of an EPerson with token: " + token, e);
                throw new RuntimeException("Something went wrong in the creation of an EPerson with token: " + token);
            }
        }
        // If no token is present, we simply do the admin execution
        EPerson eperson = createEPersonFromRestObject(context, epersonRest);

        return converter.toRest(eperson, utils.obtainProjection());
    }

    private void addEPersonToGroups(Context context, EPerson eperson, List<Group> groups) {
        if (CollectionUtils.isNotEmpty(groups)) {
            for (Group group : groups) {
                groupService.addMember(context, group, eperson);
            }
        }
    }

    private EPerson createEPersonFromRestObject(Context context, EPersonRest epersonRest) throws AuthorizeException {
        EPerson eperson = null;
        try {
            eperson = es.create(context);

            // this should be probably moved to the converter (a merge method?)
            eperson.setCanLogIn(epersonRest.isCanLogIn());
            eperson.setRequireCertificate(epersonRest.isRequireCertificate());
            eperson.setEmail(epersonRest.getEmail());
            eperson.setNetid(epersonRest.getNetid());
            if (epersonRest.getPassword() != null) {
                if (!validatePasswordService.isPasswordValid(epersonRest.getPassword())) {
                    throw new PasswordNotValidException();
                }
                es.setPassword(eperson, epersonRest.getPassword());
            }
            es.update(context, eperson);
            metadataConverter.setMetadata(context, eperson, epersonRest.getMetadata());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return eperson;
    }

    /**
     * This method will perform checks on whether or not the given Request was valid for the creation of an EPerson
     * with a token or not.
     * It'll check that the token exists, that the token doesn't yet resolve to an actual eperson already,
     * that the email in the given json is equal to the email for the token and that other properties are set to
     * what we expect in this creation.
     * It'll check if all of those constraints hold true and if we're allowed to register new accounts.
     * If this is the case, we'll create an EPerson without any authorization checks and delete the token
     * @param context       The DSpace context
     * @param epersonRest   The EPersonRest given to be created
     * @param token         The token to be used
     * @return              The EPersonRest after the creation of the EPerson object
     * @throws AuthorizeException   If something goes wrong
     * @throws SQLException         If something goes wrong
     */
    private EPersonRest createAndReturn(Context context, EPersonRest epersonRest, String token)
        throws AuthorizeException, SQLException {
        if (!AuthorizeUtil.authorizeNewAccountRegistration(context, requestService
            .getCurrentRequest().getHttpServletRequest())) {
            throw new DSpaceBadRequestException(
                "Registration is disabled, you are not authorized to create a new Authorization");
        }
        RegistrationData registrationData = registrationDataService.findByToken(context, token);
        if (registrationData == null) {
            throw new DSpaceBadRequestException("The token given as parameter: " + token + " does not exist" +
                                                " in the database");
        }
        if (es.findByEmail(context, registrationData.getEmail()) != null) {
            throw new DSpaceBadRequestException("The token given already contains an email address that resolves" +
                                                " to an eperson");
        }
        String emailFromJson = epersonRest.getEmail();
        if (StringUtils.isNotBlank(emailFromJson)) {
            if (!StringUtils.equalsIgnoreCase(registrationData.getEmail(), emailFromJson)) {
                throw new DSpaceBadRequestException("The email resulting from the token does not match the email given"
                                                        + " in the json body. Email from token: " +
                                                    registrationData.getEmail() + " email from the json body: "
                                                    + emailFromJson);
            }
        }
        if (epersonRest.isSelfRegistered() != null && !epersonRest.isSelfRegistered()) {
            throw new DSpaceBadRequestException("The self registered property cannot be set to false using this method"
                                                    + " with a token");
        }
        checkRequiredProperties(registrationData, epersonRest);
        // We'll turn off authorisation system because this call isn't admin based as it's token based
        context.turnOffAuthorisationSystem();
        EPerson ePerson = createEPersonFromRestObject(context, epersonRest);
        List<Group> groups = registrationData.getGroups();
        addEPersonToGroups(context, ePerson, groups);
        context.restoreAuthSystemState();
        // Restoring authorisation state right after the creation call
        accountService.deleteToken(context, token);
        if (context.getCurrentUser() == null) {
            context.setCurrentUser(ePerson);
        }
        return converter.toRest(ePerson, utils.obtainProjection());
    }

    private void checkRequiredProperties(RegistrationData registration, EPersonRest epersonRest) {
        MetadataRest<MetadataValueRest> metadataRest = epersonRest.getMetadata();
        if (metadataRest != null) {
            List<MetadataValueRest> epersonFirstName = metadataRest.getMap().get("eperson.firstname");
            List<MetadataValueRest> epersonLastName = metadataRest.getMap().get("eperson.lastname");
            if (epersonFirstName == null || epersonLastName == null ||
                epersonFirstName.isEmpty() || epersonLastName.isEmpty()) {
                throw new EPersonNameNotProvidedException();
            }
        }

        String password = epersonRest.getPassword();
        String netId = epersonRest.getNetid();
        if (StringUtils.isBlank(password) && StringUtils.isBlank(netId)) {
            throw new DSpaceBadRequestException(
                "You must provide a password or register using an external account"
            );
        }

        if (StringUtils.isBlank(password) && !canRegisterExternalAccount(registration, epersonRest)) {
            throw new DSpaceBadRequestException(
                "Cannot register external account with netId: " + netId
            );
        }
    }

    private boolean canRegisterExternalAccount(RegistrationData registration, EPersonRest epersonRest) {
        return accountService.isTokenValidForCreation(registration) &&
            StringUtils.equals(registration.getNetId(), epersonRest.getNetid());
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'EPERSON', 'READ')")
    public EPersonRest findOne(Context context, UUID id) {
        EPerson eperson = null;
        try {
            eperson = es.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (eperson == null) {
            return null;
        }
        return converter.toRest(eperson, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<EPersonRest> findAll(Context context, Pageable pageable) {
        try {
            long total = es.countTotal(context);
            List<EPerson> epersons = es.findAll(context, EPerson.EMAIL, pageable.getPageSize(),
                    Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(epersons, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Find the eperson with the provided email address if any. The search is delegated to the
     * {@link EPersonService#findByEmail(Context, String)} method
     *
     * @param email
     *            is the *required* email address
     * @return a Page of EPersonRest instances matching the user query
     */
    @SearchRestMethod(name = "byEmail")
    public EPersonRest findByEmail(@Parameter(value = "email", required = true) String email) {
        EPerson eperson = null;
        try {
            Context context = obtainContext();
            eperson = es.findByEmail(context, email);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (eperson == null) {
            return null;
        }
        return converter.toRest(eperson, utils.obtainProjection());
    }

    /**
     * Find the epersons matching the query parameter. The search is delegated to the
     * {@link EPersonService#search(Context, String, int, int)} method
     *
     * @param query
     *            is the *required* query string
     * @param pageable
     *            contains the pagination information
     * @return a Page of EPersonRest instances matching the user query
     */
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MANAGE_ACCESS_GROUP')")
    @SearchRestMethod(name = "byMetadata")
    public Page<EPersonRest> findByMetadata(@Parameter(value = "query", required = true) String query,
            Pageable pageable) {

        try {
            Context context = obtainContext();
            long total = es.searchResultCount(context, query);
            List<EPerson> epersons = es.search(context, query, Math.toIntExact(pageable.getOffset()),
                                                               Math.toIntExact(pageable.getPageSize()));
            return converter.toRestPage(epersons, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Find the EPersons matching the query parameter which are NOT a member of the given Group.
     * The search is delegated to the
     * {@link EPersonService#searchNonMembers(Context, String, Group, int, int)}  method
     *
     * @param groupUUID the *required* group UUID to exclude results from
     * @param query    is the *required* query string
     * @param pageable contains the pagination information
     * @return a Page of EPersonRest instances matching the user query
     */
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MANAGE_ACCESS_GROUP')")
    @SearchRestMethod(name = "isNotMemberOf")
    public Page<EPersonRest> findIsNotMemberOf(@Parameter(value = "group", required = true) UUID groupUUID,
                                             @Parameter(value = "query", required = true) String query,
                                             Pageable pageable) {

        try {
            Context context = obtainContext();
            Group excludeGroup = groupService.find(context, groupUUID);
            long total = es.searchNonMembersCount(context, query, excludeGroup);
            List<EPerson> epersons = es.searchNonMembers(context, query, excludeGroup,
                                                     Math.toIntExact(pageable.getOffset()),
                                                     Math.toIntExact(pageable.getPageSize()));
            return converter.toRestPage(epersons, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasPermission(#uuid, 'EPERSON', #patch)")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model, UUID uuid,
                         Patch patch) throws AuthorizeException, SQLException {
        boolean passwordChangeFound = false;
        for (Operation operation : patch.getOperations()) {
            if (StringUtils.equalsIgnoreCase(operation.getPath(), "/password")) {
                passwordChangeFound = true;
            }
        }
        if (StringUtils.isNotBlank(request.getParameter("token"))) {
            if (!passwordChangeFound) {
                throw new AccessDeniedException("Refused to perform the EPerson patch based on a token without " +
                                                    "changing the password");
            }
        } else {
            if (passwordChangeFound && !StringUtils.equals(context.getAuthenticationMethod(), "password")) {
                throw new AccessDeniedException("Refused to perform the EPerson patch based to change the password " +
                                                        "for non \"password\" authentication");
            }
        }
        patchDSpaceObject(apiCategory, model, uuid, patch);
    }

    @Override
    protected void delete(Context context, UUID id) throws AuthorizeException {
        EPerson eperson = null;
        try {
            eperson = es.find(context, id);
            es.delete(context, eperson);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (EmptyWorkflowGroupException e) {
            throw new RESTEmptyWorkflowGroupException(e);
        } catch (IllegalStateException e) {
            throw new UnprocessableEntityException(e.getMessage(), e);
        }
    }

    @Override
    public Class<EPersonRest> getDomainClass() {
        return EPersonRest.class;
    }
    /**
     * This method will perform an add into the table epersongroup2eperson
     * for the user with id uuid and with the list o groups
     * found on the registration data taken from token offered as query parameter
     * It also deletes the registrationdata found with token after the creation of groups
     * @return              The EPersonRest after the creation of groups
     * @throws AuthorizeException   If user is not allowed
     * @throws SQLException         If something goes wrong
     */
    public EPersonRest joinUserToGroups(UUID uuid, String token) throws AuthorizeException {
        try {
            Context context = obtainContext();
            HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
            RegistrationData registrationData = registrationDataService.findByToken(context, token);
            if (registrationData == null) {
                throw new DSpaceBadRequestException("The token given as parameter: " + token + " does not exist" +
                        " in the database");
            }
            if (uuid == null) {
                throw new DSpaceBadRequestException("The uuid of the person cannot be null");
            }
            EPerson ePerson = es.findByIdOrLegacyId(context, uuid.toString());
            if (ePerson == null) {
                throw new DSpaceBadRequestException("The token given does not contains an email address that resolves" +
                        " to an eperson");
            }
            if (!ePerson.equals(context.getCurrentUser())) {
                throw new AuthorizeException("Only the user with id: " + uuid + " can make this action.");
            }
            // We'll turn off authorisation system because this call isn't admin based as it's token based
            context.turnOffAuthorisationSystem();
            List<Group> groups = registrationData.getGroups();
            addEPersonToGroups(context, ePerson, groups);
            context.restoreAuthSystemState();
            // Restoring authorisation state right after the creation call
            accountService.deleteToken(context, token);
            return converter.toRest(ePerson, utils.obtainProjection());
        } catch (AuthorizeException e) {
            log.error(e);
            throw new AuthorizeException(e.getMessage());
        } catch (SQLException e) {
            log.error(e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public EPersonRest mergeFromRegistrationData(
        Context context, UUID uuid, String token, List<String> override
    ) throws AuthorizeException {
        try {

            if (uuid == null) {
                throw new DSpaceBadRequestException("The uuid of the person cannot be null");
            }

            if (token == null) {
                throw new DSpaceBadRequestException("You must provide a token for the eperson");
            }

            return converter.toRest(
                accountService.mergeRegistration(context, uuid, token, override),
                utils.obtainProjection()
            );
        } catch (SQLException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService.register(this, Arrays.asList(
                Link.of("/api/" + EPersonRest.CATEGORY + "/registrations", EPersonRest.NAME + "-registration")));
    }
}
