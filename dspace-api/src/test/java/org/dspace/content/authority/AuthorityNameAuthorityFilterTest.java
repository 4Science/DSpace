/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link CustomAuthorityFilter} flow
 *
 * @author Stefano Maffei
 */
public class AuthorityNameAuthorityFilterTest {

    @Test
    public void filterForAuthoritiesOK() {
        // Test set up AuthorityNameAuthorityFilter setting up custom filters
        // Organization 1 and Organization 2
        AuthorityNameAuthorityFilter entityTypeAuthorityFilter = new AuthorityNameAuthorityFilter(
            Arrays.asList("query 1", "query 2"));
        entityTypeAuthorityFilter.setSupportedAuthorities(Arrays.asList("Organization 1", "Organization 2"));
        ItemAuthority authority = Mockito.mock(ItemAuthority.class);

        // Test set up ItemAuthority is EntityOrg - Organization 1
        Mockito.when(authority.getPluginInstanceName()).thenReturn("Organization 1");
        Mockito.when(authority.getLinkedEntityType()).thenReturn("EntityOrg");

        List<String> queries = entityTypeAuthorityFilter.getFilterQueries(authority);

        assertThat(queries, is(Arrays.asList("query 1", "query 2")));
    }

    @Test
    public void noSupportedAuthorities() {
        // Test set up AuthorityNameAuthorityFilter setting up no custom filters
        AuthorityNameAuthorityFilter entityTypeAuthorityFilter = new AuthorityNameAuthorityFilter(
            Arrays.asList("query 1", "query 2"));
        entityTypeAuthorityFilter.setSupportedAuthorities(Arrays.asList());

        ItemAuthority authority = Mockito.mock(ItemAuthority.class);

        // Test set up ItemAuthority is EntityAnother - Organization 1
        Mockito.when(authority.getPluginInstanceName()).thenReturn("Organization 2");
        Mockito.when(authority.getLinkedEntityType()).thenReturn("EntityAnother");

        List<String> queries = entityTypeAuthorityFilter.getFilterQueries(authority);

        assertThat(queries, is(Arrays.asList("query 1", "query 2")));
    }

    @Test
    public void noQueryData() {

        // Test set up AuthorityNameAuthorityFilter setting up no custom filters
        AuthorityNameAuthorityFilter entityTypeAuthorityFilter = new AuthorityNameAuthorityFilter(Arrays.asList());
        entityTypeAuthorityFilter.setSupportedAuthorities(Arrays.asList("Organization 1", "Organization 2"));

        ItemAuthority authority = Mockito.mock(ItemAuthority.class);

        // Test set up ItemAuthority is EntityAnother - Organization 1
        Mockito.when(authority.getPluginInstanceName()).thenReturn("Organization 1");
        Mockito.when(authority.getLinkedEntityType()).thenReturn("EntityOrg");

        List<String> queries = entityTypeAuthorityFilter.getFilterQueries(authority);

        assertThat(queries, is(Arrays.asList()));
    }

    @Test
    public void entityNoMatchFilters() {

        // Test set up AuthorityNameAuthorityFilter setting up custom fields
        // Organization 1 and Organization 2
        AuthorityNameAuthorityFilter entityTypeAuthorityFilter = new AuthorityNameAuthorityFilter(
            Arrays.asList("query 1", "query 2"));
        entityTypeAuthorityFilter.setSupportedAuthorities(Arrays.asList("Organization 1", "Organization 2"));

        ItemAuthority authority = Mockito.mock(ItemAuthority.class);

        // Test set up ItemAuthority is EntityAnother - Organization 1
        Mockito.when(authority.getPluginInstanceName()).thenReturn("Organization 3");
        Mockito.when(authority.getLinkedEntityType()).thenReturn("EntityAnother");

        List<String> queries = entityTypeAuthorityFilter.getFilterQueries(authority);

        assertThat(queries, is(Arrays.asList()));
    }
}