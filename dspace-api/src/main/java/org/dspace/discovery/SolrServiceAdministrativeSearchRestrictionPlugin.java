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

    private static final Logger log =
        org.apache.logging.log4j.LogManager.getLogger(SolrServiceAdministrativeSearchRestrictionPlugin.class);
    public static final String SEARCH_CONFIGURATION_PREFIX = "administrative";

    @Autowired
    protected AuthorizeService authorizeService;
    @Autowired
    protected GroupService groupService;

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

            // Applies filter query to restrict search results to only those that are administrate by the current user
            solrQuery.addFilterQuery(
                Stream.concat(
                          groupService.allMemberGroupsSet(context, context.getCurrentUser())
                                      .stream()
                                      .map(group -> "g" + group.getID()),
                          Stream.of(context.getCurrentUser())
                                .filter(Objects::nonNull)
                                .map(eperson -> String.valueOf(eperson.getID()))
                      )
                      .collect(Collectors.joining(" OR ", "admin:(", ")"))
            );
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
