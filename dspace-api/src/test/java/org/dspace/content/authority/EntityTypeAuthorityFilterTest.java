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
public class EntityTypeAuthorityFilterTest {

    @Test
    public void filterForEntitiesOK() {
        // Test set up EntityTypeAuthorityFilter setting up custom filters EntityOrg and
        // EntityPerson
        EntityTypeAuthorityFilter entityTypeAuthorityFilter = new EntityTypeAuthorityFilter(
            Arrays.asList("query 1", "query 2"));
        entityTypeAuthorityFilter.setSupportedEntities(Arrays.asList("EntityOrg", "EntityPerson"));
        ItemAuthority authority = Mockito.mock(ItemAuthority.class);

        // Test set up ItemAuthority is EntityOrg - Organization 1
        Mockito.when(authority.getPluginInstanceName()).thenReturn("Organization 1");
        Mockito.when(authority.getLinkedEntityType()).thenReturn("EntityOrg");

        List<String> queries = entityTypeAuthorityFilter.getFilterQueries(authority);

        assertThat(queries, is(Arrays.asList("query 1", "query 2")));
    }

    @Test
    public void noSupportedEntities() {
        // Test set up EntityTypeAuthorityFilter setting up no custom filter
        EntityTypeAuthorityFilter entityTypeAuthorityFilter = new EntityTypeAuthorityFilter(
            Arrays.asList("query 1", "query 2"));
        entityTypeAuthorityFilter.setSupportedEntities(Arrays.asList());

        ItemAuthority authority = Mockito.mock(ItemAuthority.class);

        // Test set up ItemAuthority is EntityAnother - Organization 2
        Mockito.when(authority.getPluginInstanceName()).thenReturn("Organization 2");
        Mockito.when(authority.getLinkedEntityType()).thenReturn("EntityAnother");

        List<String> queries = entityTypeAuthorityFilter.getFilterQueries(authority);

        assertThat(queries, is(Arrays.asList("query 1", "query 2")));
    }

    @Test
    public void noQueryData() {

        // Test set up EntityTypeAuthorityFilter setting up custom filters EntityOrg e
        // EntityPerson
        EntityTypeAuthorityFilter entityTypeAuthorityFilter = new EntityTypeAuthorityFilter(Arrays.asList());
        entityTypeAuthorityFilter.setSupportedEntities(Arrays.asList("EntityOrg", "EntityPerson"));

        ItemAuthority authority = Mockito.mock(ItemAuthority.class);

        // Test set up ItemAuthority is EntityOrg - Organization 1
        Mockito.when(authority.getPluginInstanceName()).thenReturn("Organization 1");
        Mockito.when(authority.getLinkedEntityType()).thenReturn("EntityOrg");

        List<String> queries = entityTypeAuthorityFilter.getFilterQueries(authority);

        assertThat(queries, is(Arrays.asList()));
    }

    @Test
    public void entityNoMatchFilters() {

        // Test set up EntityTypeAuthorityFilter setting up custom filters EntityOrg e
        // EntityPerson
        EntityTypeAuthorityFilter entityTypeAuthorityFilter = new EntityTypeAuthorityFilter(
            Arrays.asList("query 1", "query 2"));
        entityTypeAuthorityFilter.setSupportedEntities(Arrays.asList("EntityOrg", "EntityPerson"));

        ItemAuthority authority = Mockito.mock(ItemAuthority.class);

        // Test set up ItemAuthority is EntityAnother - Organization 1
        Mockito.when(authority.getPluginInstanceName()).thenReturn("Organization 1");
        Mockito.when(authority.getLinkedEntityType()).thenReturn("EntityAnother");

        List<String> queries = entityTypeAuthorityFilter.getFilterQueries(authority);

        assertThat(queries, is(Arrays.asList()));
    }
}