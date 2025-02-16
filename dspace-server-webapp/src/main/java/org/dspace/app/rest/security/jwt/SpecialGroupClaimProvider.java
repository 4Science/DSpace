/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security.jwt;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.eperson.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JWT claim provider to read and set the special groups of an eperson on a JWT token
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
@Component
public class SpecialGroupClaimProvider implements JWTClaimProvider {

    private static final Logger log = LogManager.getLogger();

    public static final String SPECIAL_GROUPS = "sg";

    @Autowired
    private AuthenticationService authenticationService;

    @Override
    public String getKey() {
        return SPECIAL_GROUPS;
    }

    @Override
    public Object getValue(Context context, HttpServletRequest request) {

        List<String> specialGroups = getSpecialGroupsFromContext(context);
        if (CollectionUtils.isNotEmpty(specialGroups)) {
            return specialGroups;
        }

        List<Group> groups = new ArrayList<>();
        try {
            groups = authenticationService.getSpecialGroups(context, request);
        } catch (SQLException e) {
            log.error("SQLException while retrieving special groups", e);
            return null;
        }

        return getGroupsIds(groups);
    }

    @Override
    public void parseClaim(Context context, HttpServletRequest request, JWTClaimsSet jwtClaimsSet) {
        try {
            List<String> groupIds = jwtClaimsSet.getStringListClaim(SPECIAL_GROUPS);

            for (String groupId : CollectionUtils.emptyIfNull(groupIds)) {
                context.setSpecialGroup(UUID.fromString(groupId));
            }
        } catch (ParseException e) {
            log.error("Error while trying to access specialgroups from ClaimSet", e);
        }
    }

    private List<String> getSpecialGroupsFromContext(Context context) {
        try {
            return getGroupsIds(context.getSpecialGroups());
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private List<String> getGroupsIds(List<Group> groups) {
        return groups.stream()
            .map(group -> group.getID().toString())
            .collect(Collectors.toList());
    }

}
