/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.wrapper;

/**
 * This class represents an authentication token. It acts as a wrapper for a String object to differentiate between
 * actual Strings and AuthenticationToken
 */
public class AuthenticationToken {

    public static final String MACHINETOKEN_TYPE = "machinetoken";

    public static final String SHORTLIVEDTOKEN_TYPE = "shortlivedtoken";

    private String token;

    private String type;

    public static AuthenticationToken shortLivedToken(String token) {
        return new AuthenticationToken(token, SHORTLIVEDTOKEN_TYPE);
    }

    public static AuthenticationToken machineToken(String token) {
        return new AuthenticationToken(token, MACHINETOKEN_TYPE);
    }

    private AuthenticationToken(String token, String type) {
        this.token = token;
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
