/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static org.apache.solr.client.solrj.util.ClientUtils.escapeQueryChars;
import static org.dspace.discovery.SolrServiceStrictBestMatchIndexingPlugin.cleanNameWithStrictPolicies;

import java.util.Optional;

import org.dspace.discovery.SolrServiceStrictBestMatchIndexingPlugin;

/**
 *
 * @author Stefano Maffei 4Science.com
 */
public class PersonStrictCustomSolrFilterImpl implements CustomAuthoritySolrFilter {

    @Override
    public String getSolrQuery(String searchTerm) {
        return generateSearchQueryStrictBestMatch(searchTerm);
    }

    /**
     * Get solr query for best match
     * @param  searchTerm The search term string
     * @return            solr query
     */
    public String generateSearchQueryStrictBestMatch(String searchTerm) {
        return Optional.ofNullable(cleanNameWithStrictPolicies(searchTerm))
            .map(query -> SolrServiceStrictBestMatchIndexingPlugin.BEST_MATCH_INDEX + ":" + escapeQueryChars(query))
            .orElse(null);
    }
}
