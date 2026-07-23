/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Plugin that filters out non-administered items from administrative searches for collections and communities admins.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class SolrServiceAdministrativeSearchRestrictionPlugin implements SolrServiceSearchPlugin {

    private static final Logger log = LogManager.getLogger(SolrServiceAdministrativeSearchRestrictionPlugin.class);

    public static final String SEARCH_CONFIGURATION_PREFIX = "administrative";

    @Autowired
    protected AuthorizeService authorizeService;
    @Autowired
    protected GroupService groupService;
    @Autowired
    protected SearchService searchService;

    private static boolean isAdministrativeConfiguration(DiscoverQuery discoveryQuery) {
        return discoveryQuery != null &&
            StringUtils.isNotBlank(discoveryQuery.getDiscoveryConfigurationName()) &&
            discoveryQuery.getDiscoveryConfigurationName().startsWith(SEARCH_CONFIGURATION_PREFIX);
    }

    @Override
    public void additionalSearchParameters(Context context, DiscoverQuery discoveryQuery, SolrQuery solrQuery) {
        try {

            // Only apply this plugin to administrative searches
            if (!isAdministrativeConfiguration(discoveryQuery)) {
                return;
            }

            // Only apply this plugin to non-administrators
            if (isAdmin(context)) {
                return;
            }

            // Only apply this plugin to community / collection administrators
            if (!isCommunityCollAdmin(context)) {
                return;
            }

            // Applies filter query to restrict search results to only those that are administrated by the
            // current user. Direct ADMIN permissions are matched via the "admin" field, while inherited ADMIN
            // permissions are resolved at query-time through the location-ancestor field (same mechanism used
            // by SolrServiceResourceRestrictionPlugin).
            String epersonAndGroupClause =
                Stream.concat(
                          groupService.allMemberGroupsSet(context, context.getCurrentUser())
                                      .stream()
                                      .map(group -> "g" + group.getID()),
                          Stream.of(context.getCurrentUser())
                                .filter(Objects::nonNull)
                                .map(eperson -> "e" + eperson.getID())
                      )
                      .collect(Collectors.joining(" OR "));

            StringBuilder resourceQuery = new StringBuilder("admin:(").append(epersonAndGroupClause).append(")");

            String locations = searchService.createLocationQueryForAdministrableDSOs(epersonAndGroupClause);
            if (StringUtils.isNotBlank(locations)) {
                resourceQuery.append(" OR ").append(locations);
            }
            solrQuery.addFilterQuery(resourceQuery.toString());
        } catch (SQLException e) {
            log.error(LogHelper.getHeader(context, "Error while adding resource policy information to query", ""), e);
        }
    }

    private boolean isCommunityCollAdmin(Context context) throws SQLException {
        return this.authorizeService.isCollectionAdmin(context) || this.authorizeService.isCommunityAdmin(context);
    }

    private boolean isAdmin(Context context) throws SQLException {
        return authorizeService.isAdmin(context);
    }

}
