/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.health;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.dspace.core.Context;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;


/**
 * Implementation of {@link HealthIndicator} that verifies if the table sites
 * has exactly one row.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class SiteHealthIndicator extends AbstractHealthIndicator {

    @Autowired
    private SiteService siteService;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        Context context = ContextUtil.obtainCurrentRequestContext();
        try {
            List<Site> sites = siteService.findAll(context);
            if (sites != null && sites.size() != 1) {
                builder.down().withDetail("error", "`sites` table must contain exactly one row");
            } else {
                builder.up();
            }
        } catch (SQLException e) {
            builder.down(e);
        }
    }
}
