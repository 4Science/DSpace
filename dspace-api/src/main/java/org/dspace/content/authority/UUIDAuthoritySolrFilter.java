/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.text.MessageFormat;

import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.util.UUIDUtils;

/**
 * This filter is used to search for UUIDs in Solr Search core.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class UUIDAuthoritySolrFilter implements CustomAuthoritySolrFilter {

    private final static String SEARCH_RESOURCEID = "search.resourceid";
    private final static String QUERY = "'{'!lucene q.op=AND df={0}'}'\n({0}:{1})^100";

    @Override
    public boolean isApplicable(String query) {
        return UUIDUtils.isUUID(query);
    }

    @Override
    public String getSolrQuery(String query) {
        String searchTerm = ClientUtils.escapeQueryChars(query);
        return MessageFormat.format(QUERY, SEARCH_RESOURCEID, searchTerm);
    }

}
