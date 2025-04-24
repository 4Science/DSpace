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
import java.util.Collections;
import java.util.Map;

import org.dspace.AbstractUnitTest;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

/**
 * Unit tests for {@link EPersonGroupHealthIndicator}.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class EPersonGroupHealthIndicatorTest extends AbstractUnitTest {

    @Mock
    private GroupService groupService;

    @Mock
    private Group anonymousGroup;

    @Mock
    private Group administratorsGroup;

    @InjectMocks
    private EPersonGroupHealthIndicator ePersonGroupHealthIndicator;


    @Test
    public void testRequiredGroupsExist() throws SQLException {
        when(groupService.findByName(null, "Anonymous"))
            .thenReturn(anonymousGroup);
        when(groupService.findByName(null, "Administrator"))
            .thenReturn(administratorsGroup);

        Health health = ePersonGroupHealthIndicator.health();

        assertThat(health.getStatus(), is(Status.UP));
        assertThat(health.getDetails(), is(Collections.emptyMap()));
    }

    @Test
    public void testAnonymousGroupMissing() throws SQLException {
        when(groupService.findByName(null, "Anonymous"))
            .thenReturn(null);
        when(groupService.findByName(null, "Administrator"))
            .thenReturn(administratorsGroup);

        Health health = ePersonGroupHealthIndicator.health();

        assertThat(health.getStatus(), is(Status.DOWN));
        assertThat(health.getDetails(), is(Map.of("error", "The 'Anonymous' group is missing")));
    }

    @Test
    public void testAdministratorsGroupMissing() throws SQLException {
        when(groupService.findByName(null, "Anonymous"))
            .thenReturn(anonymousGroup);
        when(groupService.findByName(null, "Administrator"))
            .thenReturn(null);

        Health health = ePersonGroupHealthIndicator.health();

        assertThat(health.getStatus(), is(Status.DOWN));
        assertThat(health.getDetails(), is(Map.of("error", "The 'Administrators' group is missing")));
    }

    @Test
    public void testBothGroupsMissing() throws SQLException {
        when(groupService.findByName(null, "Anonymous"))
            .thenReturn(null);
        when(groupService.findByName(null, "Administrator"))
            .thenReturn(null);
        Health health = ePersonGroupHealthIndicator.health();

        assertThat(health.getStatus(), is(Status.DOWN));
        assertThat(health.getDetails(),
                   is(Map.of("error", "Both 'Anonymous' and 'Administrators' groups are missing")));
    }

    @Test
    public void testUnexpectedError() throws SQLException {
        when(groupService.findByName(null, "Anonymous"))
            .thenThrow(new SQLException("Unexpected database error"));

        Health health = ePersonGroupHealthIndicator.health();

        assertThat(health.getStatus(), is(Status.DOWN));
        assertThat(health.getDetails(), is(Map.of("error", "java.sql.SQLException: Unexpected database error")));
    }
}
