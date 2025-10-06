/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class can be used to combine multiple CustomAuthoritySolrFilter.
 * Each filter will be queried and the results will be combined with an OR operator.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class CompositeAuthorityFilter implements CustomAuthoritySolrFilter {

    protected final List<CustomAuthoritySolrFilter> filters;

    public CompositeAuthorityFilter(List<CustomAuthoritySolrFilter> filters) {
        this.filters = filters;
    }

    @Override
    public String getSolrQuery(String searchTerm) {
        return filters.stream()
               .filter(filter -> filter.isApplicable(searchTerm))
               .map(filter -> filter.getSolrQuery(searchTerm))
               .collect(Collectors.joining(" OR "));
    }

    @Override
    public int getConfidenceForChoices(Choice... choices) {
        return filters.stream()
                      .map(filter -> filter.getConfidenceForChoices(choices))
                      .min(Integer::compareTo)
                      .orElse(Choices.CF_UNSET);
    }
}
