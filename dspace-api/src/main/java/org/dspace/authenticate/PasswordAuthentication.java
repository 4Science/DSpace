/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * A stackable authentication method
 * based on the DSpace internal "EPerson" database.
 * See the <code>AuthenticationMethod</code> interface for more details.
 * <p>
 * The <em>username</em> is the E-Person's email address,
 * and and the <em>password</em> (given to the <code>authenticate()</code>
 * method) must match the EPerson password.
 * <p>
 * This is the default method for a new DSpace configuration.
 * If you are implementing a new "explicit" authentication method,
 * use this class as a model.
 * <p>
 * You can use this (or another explicit) method in the stack to
 * implement HTTP Basic Authentication for servlets, by passing the
 * Basic Auth username and password to the <code>AuthenticationManager</code>.
 *
 * @author Larry Stone
 */
public class PasswordAuthentication
    implements AuthenticationMethod {

    /**
     * log4j category
     */
    private static final Logger log = LogManager.getLogger();

    private static final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    private static final String PASSWORD_AUTHENTICATED = "password.authenticated";

    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();


    /**
     * Look to see if this email address is allowed to register.
     * <p>
     * The configuration key domain.valid is examined
     * in authentication-password.cfg to see what domains are valid.
     * <p>
     * Example - aber.ac.uk domain : @aber.ac.uk
     * Example - MIT domain and all .ac.uk domains: @mit.edu, .ac.uk
     *
     * @param email email
     * @throws SQLException if database error
     */
    @Override
    public boolean canSelfRegister(Context context,
                                   HttpServletRequest request,
                                   String email)
        throws SQLException {
        // Is there anything set in domain.valid?
        String[] domains = configurationService.getArrayProperty("authentication-password.domain.valid");
        if ((domains == null) || (domains.length == 0)) {
            // No conditions set, so must be able to self register
            return true;
        } else {
            // Itterate through all domains
            String check;
            email = email.trim().toLowerCase();
            for (int i = 0; i < domains.length; i++) {
                check = domains[i].trim().toLowerCase();
                if (email.endsWith(check)) {
                    // A match, so we can register this user
                    return true;
                }
            }

            // No match
            return false;
        }
    }

    /**
     * Nothing extra to initialize.
     *
     * @throws SQLException if database error
     */
    @Override
    public void initEPerson(Context context, HttpServletRequest request,
                            EPerson eperson)
        throws SQLException {
    }

    /**
     * We always allow the user to change their password.
     *
     * @throws SQLException if database error
     */
    @Override
    public boolean allowSetPassword(Context context,
                                    HttpServletRequest request,
                                    String username)
        throws SQLException {
        return true;
    }

    /**
     * This is an explicit method, since it needs username and password
     * from some source.
     *
     * @return false
     */
    @Override
    public boolean isImplicit() {
        return false;
    }

    /**
     * Add authenticated users to the group defined in authentication-password.cfg by
     * the login.specialgroup key.
     */
    @Override
    public List<Group> getSpecialGroups(Context context, HttpServletRequest request) {
        // Prevents anonymous users from being added to this group, and the second check
        // ensures they are password users
        try {
            if (context.getCurrentUser() != null
                && StringUtils.isNotBlank(
                EPersonServiceFactory.getInstance().getEPersonService().getPasswordHash(context.getCurrentUser())
                                     .toString())) {
                String groupName = configurationService.getProperty("authentication-password.login.specialgroup");
                if ((groupName != null) && !groupName.trim().isEmpty()) {
                    Group specialGroup = EPersonServiceFactory.getInstance().getGroupService()
                                                              .findByName(context, groupName);
                    if (specialGroup == null) {
                        // Oops - the group isn't there.
                        log.warn(LogHelper.getHeader(context,
                                                      "password_specialgroup",
                                                      "Group defined in modules/authentication-password.cfg login" +
                                                          ".specialgroup does not exist"));
                        return Collections.EMPTY_LIST;
                    } else {
                        return Arrays.asList(specialGroup);
                    }
                }
            }
        } catch (Exception e) {
            log.error(LogHelper.getHeader(context, "getSpecialGroups", ""), e);
        }
        return Collections.EMPTY_LIST;
    }


    /**
     * Check credentials: username must match the email address of an
     * EPerson record, and that EPerson must be allowed to login.
     * Password must match its password.  Also checks for EPerson that
     * is only allowed to login via an implicit method
     * and returns <code>CERT_REQUIRED</code> if that is the case.
     *
     * @param context  DSpace context, will be modified (EPerson set) upon success.
     * @param username Username (or email address) when method is explicit. Use null for
     *                 implicit method.
     * @param password Password for explicit auth, or null for implicit method.
     * @param realm    Realm is an extra parameter used by some authentication methods, leave null if
     *                 not applicable.
     * @param request  The HTTP request that started this operation, or null if not applicable.
     * @return One of:
     * SUCCESS, BAD_CREDENTIALS, CERT_REQUIRED, NO_SUCH_USER, BAD_ARGS
     * <p>Meaning:
     * <br>SUCCESS         - authenticated OK.
     * <br>BAD_CREDENTIALS - user exists, but password doesn't match
     * <br>CERT_REQUIRED   - not allowed to login this way without X.509 cert.
     * <br>NO_SUCH_USER    - no EPerson with matching email address.
     * <br>BAD_ARGS        - missing username, or user matched but cannot login.
     * @throws SQLException if database error
     */
    @Override
    public int authenticate(Context context,
                            String username,
                            String password,
                            String realm,
                            HttpServletRequest request)
        throws SQLException {
        if (username != null && password != null) {
            EPerson eperson = null;
            log.info(LogHelper.getHeader(context, "authenticate", "attempting password auth of user=" + username));
            eperson = EPersonServiceFactory.getInstance().getEPersonService()
                                           .findByEmail(context, username.toLowerCase());

            if (eperson == null) {
                // lookup failed.
                return NO_SUCH_USER;
            } else if (!eperson.canLogIn()) {
                // cannot login this way
                return BAD_ARGS;
            } else if (eperson.getRequireCertificate()) {
                // this user can only login with x.509 certificate
                log.warn(LogHelper.getHeader(context, "authenticate",
                                              "rejecting PasswordAuthentication because " + username + " requires " +
                                                  "certificate."));
                return CERT_REQUIRED;
            } else if (EPersonServiceFactory.getInstance().getEPersonService()
                                            .checkPassword(context, eperson, password)) {
                // login is ok if password matches:
                context.setCurrentUser(eperson);
                if (request != null) {
                    request.setAttribute(PASSWORD_AUTHENTICATED, true);
                }
                log.info(LogHelper.getHeader(context, "authenticate", "type=PasswordAuthentication"));
                return SUCCESS;
            } else {
                return BAD_CREDENTIALS;
            }
        } else {
            // BAD_ARGS always defers to the next authentication method.
            // It means this method cannot use the given credentials.
            return BAD_ARGS;
        }
    }

    /**
     * Returns URL of password-login servlet.
     *
     * @param context  DSpace context, will be modified (EPerson set) upon success.
     * @param request  The HTTP request that started this operation, or null if not applicable.
     * @param response The HTTP response from the servlet method.
     * @return fully-qualified URL
     */
    @Override
    public String loginPageURL(Context context,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        return null;
    }

    @Override
    public String getName() {
        return "password";
    }


    @Override
    public boolean isUsed(final Context context, final HttpServletRequest request) {
        if (request != null &&
                context.getCurrentUser() != null &&
                request.getAttribute(PASSWORD_AUTHENTICATED) != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canChangePassword(Context context, EPerson ePerson, String currentPassword) {
        if (context == null || ePerson == null) {
            return false;
        }
        return ePersonService.checkPassword(context, ePerson, currentPassword);
    }

    @Override
    public boolean areSpecialGroupsApplicable(Context context, HttpServletRequest request) {
        return isPasswordAuthenticationMethodInContext(context, request) ||
            isPasswordAuthenticatedInRequest(context, request);
    }

    private boolean isPasswordAuthenticatedInRequest(Context context, HttpServletRequest request) {
        return (context == null || StringUtils.isBlank(context.getAuthenticationMethod())) &&
            request != null && Optional.ofNullable(request.getAttribute(PASSWORD_AUTHENTICATED))
                                       .map(Boolean.class::cast)
                                       .orElse(false);
    }

    private boolean isPasswordAuthenticationMethodInContext(Context context, HttpServletRequest request) {
        return AuthenticationMethod.super.areSpecialGroupsApplicable(context, request);
    }
}
