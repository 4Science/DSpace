/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.health;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.dspace.AbstractUnitTest;
import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

/**
 * Unit tests for {@link SiteHealthIndicator}.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class SiteHealthIndicatorTest extends AbstractUnitTest {

    @Mock
    private SiteService siteService;

    @Mock
    private Site mockSite;

    @InjectMocks
    private SiteHealthIndicator siteHealthIndicator;


    @Test
    public void testSiteExists() throws SQLException {
        context.turnOffAuthorisationSystem();
        context.restoreAuthSystemState();
        when(siteService.findAll(null)).thenReturn(Collections.singletonList(mockSite));

        Health health = siteHealthIndicator.health();

        assertThat(health.getStatus(), is(Status.UP));
    }

    @Test
    public void testSiteMissing() throws SQLException {
        when(siteService.findAll(null)).thenReturn(Collections.emptyList());

        Health health = siteHealthIndicator.health();

        assertThat(health.getStatus(), is(Status.DOWN));
        assertThat(health.getDetails(), is(Map.of("error", "`sites` table must contain exactly one row")));
    }

    @Test
    public void testMultipleSitesExist() throws SQLException {
        // Mock a situation where findSite returns multiple rows instead of one
        context.turnOffAuthorisationSystem();
        context.restoreAuthSystemState();
        when(siteService.findAll(null)).thenReturn(Arrays.asList(mockSite, mockSite));

        Health health = siteHealthIndicator.health();

        assertThat(health.getStatus(), is(Status.DOWN));
        assertThat(health.getDetails(), is(Map.of("error", "`sites` table must contain exactly one row")));
    }

    @Test
    public void testUnexpectedError() throws SQLException {
        when(siteService.findAll(null)).thenThrow(new SQLException("Unexpected database error"));

        Health health = siteHealthIndicator.health();

        assertThat(health.getStatus(), is(Status.DOWN));
        assertThat(health.getDetails(), is(Map.of("error", "java.sql.SQLException: Unexpected database error")));
    }
}

