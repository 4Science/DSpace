/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dspace.app.rest.model.wrapper.AuthenticationToken;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.stereotype.Service;

/**
 * Interface for a service that can provide authentication for the REST API
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
@Service
public interface RestAuthenticationService {

    /**
     * This method should be called after a successful authentication occurs. It gathers the authentication data for
     * the currently logged in user, adds it into the auth token & saves that token to the response (optionally
     * in a cookie).
     * @param request current request
     * @param response current response
     * @param authentication Authentication data generated by the authentication plugin
     * @param addCookie boolean, whether to save the generated auth token to a Cookie or not. Default is false. However,
     *                  some authentication methods may require this information be saved to a cookie (even temporarily)
     *                  in order to complete the login process (e.g. Shibboleth requires this)
     * @throws IOException
     */
    void addAuthenticationDataForUser(HttpServletRequest request, HttpServletResponse response,
                                      DSpaceAuthentication authentication, boolean addCookie) throws IOException;

    /**
     * Retrieve a short lived authentication token, this can be used (among other things) for file downloads
     * @param context the DSpace context
     * @param request The current client request
     * @return An AuthenticationToken that contains a string with the token
     */
    AuthenticationToken getShortLivedAuthenticationToken(Context context, HttpServletRequest request);

    /**
     * Retrieve a machine to machine authentication token.
     * @param context the DSpace context
     * @param request The current client request
     * @return An AuthenticationToken that contains a string with the token
     */
    AuthenticationToken getMachineAuthenticationToken(Context context, HttpServletRequest request);

    /**
     * Checks the current request for a valid authentication token. If found, extracts that token and obtains the
     * currently logged in EPerson.
     * @param request current request
     * @param response current response
     * @param context current DSpace Context
     * @return EPerson of the logged in user (if auth token found), or null if no auth token is found
     */
    EPerson getAuthenticatedEPerson(HttpServletRequest request, HttpServletResponse response, Context context);

    /**
     * Checks the current request for a valid authentication token. If found, returns true. If not found, returns false
     * @param request current request
     * @return true if this request includes a valid authentication token. False otherwise.
     */
    boolean hasAuthenticationData(HttpServletRequest request);

    /**
     * Invalidate the current authentication token/data in the request. This is used during logout to ensure any
     * existing authentication data/token is destroyed/invalidated and cannot be reused in later requests.
     * <P>
     * In other words, this method invalidates the authentication data created by addAuthenticationDataForUser().
     *
     * @param request current request
     * @param response current response
     * @param context current DSpace Context.
     * @throws Exception
     */
    void invalidateAuthenticationData(HttpServletRequest request, HttpServletResponse response, Context context)
            throws Exception;

    /**
     * Get access to the current AuthenticationService
     * @return current AuthenticationService
     */
    AuthenticationService getAuthenticationService();

    /**
     * Return the value that should be passed in the WWWW-Authenticate header for 4xx responses to the client
     * @param request The current client request
     * @param response The response being build for the client
     * @return A string value that should be set in the WWWW-Authenticate header
     */
    String getWwwAuthenticateHeaderValue(HttpServletRequest request, HttpServletResponse response);

    /**
     * Invalidate just the authentication Cookie (optionally created by addAuthenticationDataForUser()), while
     * keeping the authentication token valid.
     * <P>
     * This method may be used by authentication services which require a Cookie (i.e. addCookie=true in
     * addAuthenticationDataForUser()). It's useful for those services to immediately *remove/discard* the Cookie after
     * it has been used. This ensures the auth Cookie is temporary in nature, and is destroyed as soon as it is no
     * longer needed.
     * @param request current request
     * @param res current response (where Cookie should be destroyed)
     */
    void invalidateAuthenticationCookie(HttpServletRequest request, HttpServletResponse res);

    /**
     * Invalidate the machine token related to the current user.
     *
     * @param  context   the DSpace context
     * @param  request   The current client request
     * @throws Exception if an error occurs
     */
    void invalidateMachineAuthenticationToken(Context context, HttpServletRequest request) throws Exception;

}
