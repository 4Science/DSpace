/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security.jwt;

import java.sql.SQLException;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link JWTClaimProvider} that add the claim machine to the
 * jwt token to specify if that token is a machine to machine token or not.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component
public class MachineClaimProvider implements JWTClaimProvider {

    public static final String MACHINE_TOKEN = "machine";

    @Override
    public String getKey() {
        return MACHINE_TOKEN;
    }

    @Override
    public Object getValue(Context context, HttpServletRequest request) {
        return isMachineTokenRequest(request);
    }

    @Override
    public void parseClaim(Context context, HttpServletRequest request, JWTClaimsSet jwtClaimsSet) throws SQLException {
    }

    public boolean isMachineTokenRequest(HttpServletRequest request) {
        return request.getAttribute(MACHINE_TOKEN) != null;
    }

}
