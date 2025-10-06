/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;

/**
 * This filter is used to search for Handles in Solr Search core.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class HandleAuthoritySolrFilter implements CustomAuthoritySolrFilter {

    private static final String HANDLE_REGEX = "^[0-9]+/.+$";
    private final static String HANDLE = "handle";
    private final static String QUERY = "'{'!lucene q.op=AND df={0}'}'\n({0}:{1})^100";

    @Override
    public boolean isApplicable(String query) {
        return isHandle(query);
    }

    @Override
    public String getSolrQuery(String query) {
        String searchTerm = ClientUtils.escapeQueryChars(query) + "*";
        return MessageFormat.format(QUERY, HANDLE, searchTerm);
    }

    protected boolean isHandle(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        return value.matches(HANDLE_REGEX);
    }

}
