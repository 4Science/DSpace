/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static org.dspace.app.rest.security.WebSecurityConfiguration.ADMIN_GRANT;
import static org.dspace.app.rest.security.WebSecurityConfiguration.AUTHENTICATED_GRANT;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.login.PostLoggedInAction;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.EPerson;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * This class is responsible for authenticating a user via REST.
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
@Component
public class EPersonRestAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LogManager.getLogger();

    public static final String MANAGE_ACCESS_GROUP = "MANAGE_ACCESS_GROUP";

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private HttpServletRequest request;

    @Autowired(required = false)
    private List<PostLoggedInAction> postLoggedInActions;

    @PostConstruct
    public void postConstruct() {
        if (postLoggedInActions == null) {
            postLoggedInActions = Collections.emptyList();
        }
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Context context = ContextUtil.obtainContext(request);
        // If a user already exists in the context, then no authentication is necessary. User is already logged in
        if (context != null && context.getCurrentUser() != null) {
            // Simply refresh/reload the auth token. If token has expired, the token will change.
            log.debug("Request to refresh auth token");
            return authenticateRefreshTokenRequest(context);
        } else {
            // Otherwise, this is a new login & we need to attempt authentication
            log.debug("Request to authenticate new login");
            return authenticateNewLogin(context, authentication);
        }
    }

    /**
     * Trigger a JWT token refresh by updating the currently logged-in user's "lastActive" date to *now*.
     * Since the logged-in user's "lastActive" date is used to determine whether the token has expired, this *may*
     * cause the token to change (if expiration time has passed). If expiration has not passed, this request will
     * return the same token as before.
     * @param context current DSpace context (for currently logged in user information)
     * @return DSpaceAuthentication object representing authenticated user
     */
    private Authentication authenticateRefreshTokenRequest(Context context) {
        authenticationService.updateLastActiveDate(context);
        return createAuthentication(context);
    }

    /**
     * Attempt a new login to DSpace based on the information provided in the Authentication class.
     * If login is successful, returns a NEW Authentication class containing the logged in EPerson and their list of
     * GrantedAuthority objects.  If login fails, a BadCredentialsException is thrown. If no valid login found implicit
     * or explicit, then null is returned.
     *
     * @param context The current DSpace context
     * @param authentication Authentication class to attempt authentication.
     * @return new Authentication class containing logged-in user information or null
     */
    private Authentication authenticateNewLogin(Context context, Authentication authentication) {
        Authentication output = null;

        if (authentication != null) {
            String name = authentication.getName();
            String password = Objects.toString(authentication.getCredentials(), null);

            int implicitStatus = authenticationService.authenticateImplicit(context, null, null, null, request);

            if (implicitStatus == AuthenticationMethod.SUCCESS) {
                log.info(LogHelper.getHeader(context, "login", "type=implicit"));
                output = createAuthentication(context);
            } else {
                int authenticateResult = authenticationService.authenticate(context, name, password, null, request);
                if (AuthenticationMethod.SUCCESS == authenticateResult) {

                    log.info(LogHelper.getHeader(context, "login", "type=explicit"));

                    output = createAuthentication(context);

                    for (PostLoggedInAction action : postLoggedInActions) {
                        try {
                            action.loggedIn(context);
                        } catch (Exception ex) {
                            log.error("An error occurs performing post logged in action", ex);
                        }
                    }

                } else {
                    log.info(LogHelper.getHeader(context, "failed_login",
                                                 "email={}, result={}"), name, authenticateResult);
                    throw new BadCredentialsException("Login failed");
                }
            }
        }

        return output;
    }

    /**
     * Create a valid Spring Authentication object for the user currently authenticated in the Context.
     * If no current user is found in the Context, then the login must have failed and a BadCredentialsException is
     * thrown.
     * @param context current DSpace context
     * @return DSpaceAuthentication object for currently authenticated user
     * @throws BadCredentialsException if no current user found
     */
    private Authentication createAuthentication(final Context context) {
        EPerson ePerson = context.getCurrentUser();

        if (ePerson != null && StringUtils.isNotBlank(ePerson.getEmail())) {
            //Pass the eperson ID to the request service
            requestService.setCurrentUserId(ePerson.getID());

            return new DSpaceAuthentication(ePerson, getGrantedAuthorities(context));

        } else {
            log.info(LogHelper.getHeader(context, "failed_login", "No eperson with a non-blank e-mail address found"));
            throw new BadCredentialsException("Login failed");
        }
    }

    /**
     * Return list of GrantedAuthority objects for the user currently authenticated in the Context
     * @param context current DSpace context
     * @return List of GrantedAuthority.  Empty list is returned if no current user exists.
     */
    public List<GrantedAuthority> getGrantedAuthorities(Context context) {
        List<GrantedAuthority> authorities = new LinkedList<>();
        EPerson eperson = context.getCurrentUser();
        if (eperson != null) {
            boolean isAdmin = false;
            try {
                isAdmin = authorizeService.isAdmin(context, eperson);
            } catch (SQLException e) {
                log.error("SQL error while checking for admin rights", e);
            }

            if (isAdmin) {
                authorities.add(new SimpleGrantedAuthority(ADMIN_GRANT));
            } else if (authorizeService.isAccountManager(context)) {
                authorities.add(new SimpleGrantedAuthority(MANAGE_ACCESS_GROUP));
            }

            authorities.add(new SimpleGrantedAuthority(AUTHENTICATED_GRANT));
        }

        return authorities;
    }

    /**
     * Return whether this provider supports this Authentication type.  Only returns true if the Authentication type
     * is a valid DSpaceAuthentication class.
     * @param authentication
     * @return true if valid DSpaceAuthentication class
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return DSpaceAuthentication.class.isAssignableFrom(authentication);
    }
}
