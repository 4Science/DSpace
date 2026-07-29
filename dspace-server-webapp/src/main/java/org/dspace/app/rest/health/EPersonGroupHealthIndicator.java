/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.health;

import java.sql.SQLException;
import java.util.Map;

import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Implementation of {@link HealthIndicator} to verify the presence of essential
 * groups in the system, specifically "Anonymous" and "Administrators" in the
 * `epersongroups` table.
 * Marks the status as "UP" if both groups are present, otherwise "DOWN" with
 * details about the missing group(s).
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class EPersonGroupHealthIndicator extends AbstractHealthIndicator {

    private static final Map<Map<Boolean, Boolean>, String> ERROR_MESSAGES = Map.of(
        Map.of(false, false), "Both 'Anonymous' and 'Administrators' groups are missing",
        Map.of(true, false), "The 'Administrators' group is missing",
        Map.of(false, true), "The 'Anonymous' group is missing"
    );
    @Autowired
    private GroupService groupService;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        Context context = ContextUtil.obtainCurrentRequestContext();
        try {
            boolean hasAnonymous = groupService.findByName(context, Group.ANONYMOUS) != null;
            boolean hasAdministrators = groupService.findByName(context, Group.ADMIN) != null;

            if (hasAnonymous && hasAdministrators) {
                builder.up();
            } else {
                builder.down()
                       .withDetail("error", ERROR_MESSAGES.get(Map.of(hasAnonymous, hasAdministrators)));
            }
        } catch (SQLException e) {
            builder.down(e);
        }
    }
}
