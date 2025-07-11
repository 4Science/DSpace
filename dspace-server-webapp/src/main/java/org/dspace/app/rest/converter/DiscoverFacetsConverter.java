/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import static org.dspace.discovery.configuration.DiscoveryConfigurationParameters.TYPE_DATE;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchFacetValueRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.util.MultiFormatDateParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class DiscoverFacetsConverter {

    private static final Logger log = LogManager.getLogger(DiscoverFacetsConverter.class);

    @Autowired
    private DiscoverFacetValueConverter facetValueConverter;

    @Autowired
    private SearchService searchService;

    public SearchResultsRest convert(Context context, String query, List<String> dsoTypes, String configurationName,
                                     String dsoScope, List<SearchFilter> searchFilters, final Pageable page,
                                     DiscoveryConfiguration configuration, DiscoverResult searchResult,
                                     Projection projection) {

        SearchResultsRest searchResultsRest = new SearchResultsRest();
        searchResultsRest.setProjection(projection);

        setRequestInformation(context, query, dsoTypes, configurationName, dsoScope, searchFilters, page,
                              searchResultsRest);
        addFacetValues(context, searchResult, searchResultsRest, configuration, projection);

        return searchResultsRest;
    }

    /**
     * Fill the facet values information in the SearchResultsRest using the information in the api DiscoverResult object
     * according to the configuration applied to the discovery query
     *
     * @param context
     *            The relevant DSpace context
     * @param searchResult
     *            The DiscoverResult containing the discovery result
     * @param resultsRest
     *            The SearchResultsRest that need to be filled in
     * @param configuration
     *            The DiscoveryConfiguration applied to the query
     */
    public void addFacetValues(Context context, final DiscoverResult searchResult, final SearchResultsRest resultsRest,
            final DiscoveryConfiguration configuration, final Projection projection) {

        List<DiscoverySearchFilterFacet> facets = configuration.getSidebarFacets();
        for (DiscoverySearchFilterFacet field : CollectionUtils.emptyIfNull(facets)) {
            List<DiscoverResult.FacetResult> facetValues = searchResult.getFacetResult(field);

            SearchFacetEntryRest facetEntry = new SearchFacetEntryRest(field.getIndexFieldName());
            facetEntry.setProjection(projection);
            int valueCount = 0;
            facetEntry.setHasMore(false);
            facetEntry.setFacetLimit(field.getFacetLimit());
            if (field.exposeMinAndMaxValue()) {
                handleExposeMinMaxValues(context, field, facetEntry);
            }
            facetEntry.setOpenByDefault(field.isOpenByDefault());
            facetEntry.setExposeMinMax(field.exposeMinAndMaxValue());
            facetEntry.setFacetType(field.getType());
            for (DiscoverResult.FacetResult value : CollectionUtils.emptyIfNull(facetValues)) {
                // The discover results contains max facetLimit + 1 values. If we reach the "+1", indicate that there
                // are
                // more results available.
                if (valueCount < field.getFacetLimit()) {
                    SearchFacetValueRest valueRest = facetValueConverter.convert(value, projection);

                    facetEntry.addValue(valueRest);
                } else {
                    facetEntry.setHasMore(true);
                }

                valueCount++;
            }

            resultsRest.addFacetEntry(facetEntry);
        }
    }

    private static void minMaxDateHandler(SearchFacetEntryRest facetEntry, String minValue, String maxValue) {
        ZonedDateTime minDate = MultiFormatDateParser.parse(minValue);
        ZonedDateTime maxDate = MultiFormatDateParser.parse(maxValue);
        facetEntry.setMinValue(getYearOr(minDate, () -> "0"));
        facetEntry.setMaxValue(getYearOr(maxDate, DiscoverFacetsConverter::getCurrentYear));
    }

    private static String getCurrentYear() {
        return String.valueOf(Instant.now().atZone(ZoneId.of("UTC")).getYear());
    }

    private static void minMaxHandler(SearchFacetEntryRest facetEntry, String minValue, String maxValue) {
        facetEntry.setMinValue(minValue);
        facetEntry.setMaxValue(maxValue);
    }

    private static String getYearOr(ZonedDateTime date, Supplier<String> defaultSupplier) {
        if (date == null) {
            return defaultSupplier.get();
        }
        return String.valueOf(date.getYear());
    }

    /**
     * This method will fill the facetEntry with the appropriate min and max values if they're not empty
     *
     * @param context
     *            The relevant DSpace context
     * @param field
     *            The DiscoverySearchFilterFacet field to search for this value in solr
     * @param facetEntry
     *            The SearchFacetEntryRest facetEntry for which this needs to be filled in
     */
    private void handleExposeMinMaxValues(Context context, DiscoverySearchFilterFacet field,
            SearchFacetEntryRest facetEntry) {
        try {

            String minValueField = field.getIndexFieldName() + "_min";
            String minSortField = field.getIndexFieldName() + "_min_sort";
            String maxValueField = field.getIndexFieldName() + "_max";
            String maxSortField = field.getIndexFieldName() + "_max_sort";
            TriConsumer<SearchFacetEntryRest, String, String> minMaxHandler = DiscoverFacetsConverter::minMaxHandler;

            if (TYPE_DATE.equals(field.getType())) {
                minValueField = field.getIndexFieldName() + ".year_sort";
                minSortField = field.getIndexFieldName() + ".year_sort";
                maxValueField = field.getIndexFieldName() + ".year_sort";
                maxSortField = field.getIndexFieldName() + ".year_sort";
                minMaxHandler = DiscoverFacetsConverter::minMaxDateHandler;
            }

            String minValue =
                searchService.calculateExtremeValue(
                    context, minValueField,
                    minSortField, DiscoverQuery.SORT_ORDER.asc
                );
            String maxValue =
                searchService.calculateExtremeValue(
                    context, maxValueField,
                    maxSortField, DiscoverQuery.SORT_ORDER.desc
                );

            if (StringUtils.isNotBlank(minValue) && StringUtils.isNotBlank(maxValue)) {
                minMaxHandler.accept(facetEntry, minValue, maxValue);
            }
        } catch (SearchServiceException e) {
            log.error(
                "Cannot retrieve min and max values with for the filter {}: {}",
                field.getIndexFieldName(),
                e.getMessage(),
                e
            );
        }
    }

    private void setRequestInformation(final Context context, final String query, final List<String> dsoTypes,
                                       final String configurationName, final String scope,
                                       final List<SearchFilter> searchFilters, final Pageable page,
                                       final SearchResultsRest resultsRest) {
        resultsRest.setQuery(query);
        resultsRest.setConfiguration(configurationName);
        resultsRest.setDsoTypes(dsoTypes);
        resultsRest.setSort(SearchResultsRest.Sorting.fromPage(page));

        resultsRest.setScope(scope);

        SearchFilterToAppliedFilterConverter searchFilterToAppliedFilterConverter = new
            SearchFilterToAppliedFilterConverter();
        for (SearchFilter searchFilter : CollectionUtils.emptyIfNull(searchFilters)) {

            resultsRest
                .addAppliedFilter(searchFilterToAppliedFilterConverter.convertSearchFilter(context, searchFilter));
        }
    }
}
