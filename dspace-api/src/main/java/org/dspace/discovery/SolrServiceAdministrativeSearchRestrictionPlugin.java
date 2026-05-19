/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
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

    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected GroupService groupService;
    @Autowired(required = true)
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

            EPerson currentUser = context.getCurrentUser();
            StringBuilder epersonAndGroupClause = new StringBuilder();
            if (currentUser != null) {
                epersonAndGroupClause.append("e").append(currentUser.getID());
            }
            Set<Group> groups = groupService.allMemberGroupsSet(context, currentUser);
            for (Group group : groups) {
                if (!epersonAndGroupClause.isEmpty()) {
                    epersonAndGroupClause.append(" OR g").append(group.getID());
                } else {
                    epersonAndGroupClause.append("g").append(group.getID());
                }
            }

            // Use location-based filtering to restrict results to only items within containers
            // (communities/collections) where the user has administrative rights.
            // This matches the pattern used in SolrServiceResourceRestrictionPlugin when
            // inheritAuthorizations is enabled.
            String locationQuery = searchService
                .createLocationQueryForAdministrableDSOs(epersonAndGroupClause.toString());
            if (StringUtils.isNotBlank(locationQuery)) {
                solrQuery.addFilterQuery(locationQuery);
            }
        } catch (SQLException e) {
            log.error(
                LogHelper.getHeader(context, "Error while adding administrative location filter to query", ""), e);
        }
    }

    private boolean isCommunityCollAdmin(Context context) throws SQLException {
        return this.authorizeService.isCollectionAdmin(context) || this.authorizeService.isCommunityAdmin(context);
    }

    private boolean isAdmin(Context context) throws SQLException {
        return authorizeService.isAdmin(context);
    }

}
