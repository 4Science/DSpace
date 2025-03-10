/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class FacetValueMatcher {

    private FacetValueMatcher() { }

    public static Matcher<? super Object> entryAuthor(String label) {
        return allOf(
            hasJsonPath("$.label", is(label)),
            hasJsonPath("$.type", is("discover")),
            hasJsonPath("$.uniqueType", is("discover.discover")),
            hasJsonPath("$._links.search.href", containsString("api/discover/search/objects")),
            hasJsonPath("$._links.search.href", containsString(
                    "f.author=" + urlPathSegmentEscaper().escape(label) + ",equals"
            ))
        );
    }

    public static Matcher<? super Object> entryAuthorWithAuthority(String label, String authority, int count) {
        return allOf(
            hasJsonPath("$.authorityKey", is(authority)),
            hasJsonPath("$.count", is(count)),
            hasJsonPath("$.label", is(label)),
            hasJsonPath("$.type", is("discover")),
            hasJsonPath("$.uniqueType", is("discover.discover")),
            hasJsonPath("$._links.search.href", containsString("api/discover/search/objects")),
            hasJsonPath("$._links.search.href", containsString("f.author=" + authority + ",authority"))
        );
    }

    public static Matcher<? super Object> matchEntry(String facet, String label, int count) {
        return allOf(
                hasJsonPath("$.label", is(label)),
                hasJsonPath("$.type", is("discover")),
                hasJsonPath("$.uniqueType", is("discover.discover")),
                hasJsonPath("$.count", is(count)),
                hasJsonPath("$._links.search.href", containsString("api/discover/search/objects")),
                hasJsonPath("$._links.search.href", containsString("f." + facet + "=" + label + ",equals"))
        );
    }

    public static Matcher<? super Object> entryDateIssued() {
        return allOf(
            hasJsonPath("$.label", Matchers.notNullValue()),
            hasJsonPath("$.type", is("discover")),
            hasJsonPath("$.uniqueType", is("discover.discover")),
            hasJsonPath("$._links.search.href", containsString("api/discover/search/objects")),
            hasJsonPath("$._links.search.href", containsString("f.dateIssued=")),
            hasJsonPath("$._links.search.href", containsString(",equals"))
        );
    }

    public static Matcher<? super Object> entryDateIssuedWithCountOne() {
        return allOf(
            hasJsonPath("$.label", Matchers.notNullValue()),
            hasJsonPath("$.type", is("discover")),
            hasJsonPath("$.uniqueType", is("discover.discover")),
            hasJsonPath("$.count", is(1)),
            hasJsonPath("$._links.search.href", containsString("api/discover/search/objects")),
            hasJsonPath("$._links.search.href", containsString("f.dateIssued=")),
            hasJsonPath("$._links.search.href", containsString(",equals"))
        );
    }

    public static Matcher<? super Object> entryDateIssuedWithLabel(String label) {
        return allOf(
            hasJsonPath("$.label", is(label)),
            hasJsonPath("$.type", is("discover")),
            hasJsonPath("$.uniqueType", is("discover.discover")),
            hasJsonPath("$._links.search.href", containsString("api/discover/search/objects")),
            hasJsonPath("$._links.search.href", containsString("f.dateIssued=")),
            hasJsonPath("$._links.search.href", containsString(",equals"))
        );
    }

    public static Matcher<? super Object> entryDateIssuedWithLabelAndCount(String label, int count) {
        return allOf(
            hasJsonPath("$.label", is(label)),
            hasJsonPath("$.count", is(count)),
            hasJsonPath("$.type", is("discover")),
            hasJsonPath("$.uniqueType", is("discover.discover")),
            hasJsonPath("$._links.search.href", containsString(",equals"))
        );
    }

    public static Matcher<? super Object> entryText(String facetName, String label, int count) {
        return allOf(
            hasJsonPath("$.label", is(label)),
            hasJsonPath("$.count", is(count)),
            hasJsonPath("$.type", is("discover")),
            hasJsonPath("$.uniqueType", is("discover.discover")),
            hasJsonPath("$._links.search.href", containsString("api/discover/search/objects")),
            hasJsonPath("$._links.search.href", containsString("f." + facetName + "=" + label + ",equals"))
        );
    }

    public static Matcher<? super Object> entryLanguage(String label) {
        return allOf(
            hasJsonPath("$.label", is(label)),
            hasJsonPath("$.type", is("discover")),
            hasJsonPath("$.uniqueType", is("discover.discover")),
            hasJsonPath("$._links.search.href", containsString("api/discover/search/objects")),
            hasJsonPath("$._links.search.href", containsString("f.language="))
        );
    }

    public static Matcher<? super Object> entrySupervisedBy(String label, String authority, int count) {
        return allOf(
            hasJsonPath("$.authorityKey", is(authority)),
            hasJsonPath("$.count", is(count)),
            hasJsonPath("$.label", is(label)),
            hasJsonPath("$.type", is("discover")),
            hasJsonPath("$.uniqueType", is("discover.discover")),
            hasJsonPath("$._links.search.href", containsString("api/discover/search/objects")),
            hasJsonPath("$._links.search.href", containsString("f.supervisedBy=" + authority + ",authority"))
        );
    }

}
