/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

/**
 *
 * @author Stefano Maffei 4Science.com
 */
public interface CustomAuthoritySolrFilter {

    /**
     * Checks if the searchTerm is valid for this kind of filter
     * By default it returns true, meaning that the filter is always applicable
     * You can override it to implement custom logic.
     *
     * @param searchTerm
     * @return
     */
    default boolean isApplicable(String searchTerm) {
        return true;
    }

    /**
     * Returns the solr query to be used for a specified authority
     * 
     * @return String the solr query
     */
    String getSolrQuery(String searchTerm);

    /**
     * Get the confidence value for the generated choices
     * @return            solr query
     */
    default int getConfidenceForChoices(Choice... choices) {
        if (choices.length == 0) {
            return Choices.CF_UNSET;
        }
        if (choices.length == 1) {
            return Choices.CF_ACCEPTED;
        }
        return Choices.CF_UNCERTAIN;
    }

}
