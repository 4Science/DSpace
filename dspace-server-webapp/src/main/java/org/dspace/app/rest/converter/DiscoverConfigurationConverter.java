/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.model.SearchConfigurationRest;
import org.dspace.app.rest.model.SearchConfigurationRest.Filter.Operator;
import org.dspace.app.rest.projection.Projection;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.springframework.stereotype.Component;

/**
 * This class' purpose is to create a SearchConfigurationRest object from the DiscoveryConfiguration to be given
 * to the convert method.
 */
@Component
public class DiscoverConfigurationConverter
        implements DSpaceConverter<DiscoveryConfiguration, SearchConfigurationRest> {

    @Override
    public SearchConfigurationRest convert(DiscoveryConfiguration configuration, Projection projection) {
        SearchConfigurationRest searchConfigurationRest = new SearchConfigurationRest();
        searchConfigurationRest.setProjection(projection);
        if (configuration != null) {
            addSearchFilters(searchConfigurationRest,
                             configuration.getSearchFilters(), configuration.getSidebarFacets());
            addSortOptions(searchConfigurationRest, configuration.getSearchSortConfiguration());
        }
        return searchConfigurationRest;
    }

    @Override
    public Class<DiscoveryConfiguration> getModelClass() {
        return DiscoveryConfiguration.class;
    }

    public void addSearchFilters(
        SearchConfigurationRest searchConfigurationRest,
        List<DiscoverySearchFilter> searchFilterList,
        List<DiscoverySearchFilterFacet> facetList
    ) {
        final Map<String, List<DiscoverySearchFilterFacet>> facetFieldMap =
            facetList.stream()
                .collect(
                    Collectors.groupingBy(DiscoverySearchFilterFacet::getIndexFieldName)
                );
        CollectionUtils.emptyIfNull(searchFilterList)
            .stream()
            .map(discoverySearchFilter -> createFilter(facetFieldMap, discoverySearchFilter))
            .forEach(searchConfigurationRest::addFilter);
    }

    protected SearchConfigurationRest.Filter createFilter(
        final Map<String, List<DiscoverySearchFilterFacet>> facetFieldMap, DiscoverySearchFilter discoverySearchFilter
    ) {
        SearchConfigurationRest.Filter filter = new SearchConfigurationRest.Filter();
        filter.setFilter(discoverySearchFilter.getIndexFieldName());
        filter.setHasFacets(facetFieldMap.containsKey(discoverySearchFilter.getIndexFieldName()));
        filter.setType(discoverySearchFilter.getType());
        filter.setOpenByDefault(discoverySearchFilter.isOpenByDefault());
        if (DiscoveryConfigurationParameters.TYPE_GEOMAP.equals(discoverySearchFilter.getType())) {
            filter.addOperator(new Operator(SearchConfigurationRest.Filter.OPERATOR_POINT));
            filter.addOperator(new Operator(SearchConfigurationRest.Filter.OPERATOR_POLYGON));
        } else {
            filter.addDefaultOperatorsToList();
        }
        filter.setPageSize(discoverySearchFilter.getPageSize());
        return filter;
    }

    private void addSortOptions(SearchConfigurationRest searchConfigurationRest,
                                DiscoverySortConfiguration searchSortConfiguration) {
        if (searchSortConfiguration != null) {
            for (DiscoverySortFieldConfiguration discoverySearchSortConfiguration : CollectionUtils
                .emptyIfNull(searchSortConfiguration.getSortFields())) {
                SearchConfigurationRest.SortOption sortOption = new SearchConfigurationRest.SortOption();
                if (StringUtils.isBlank(discoverySearchSortConfiguration.getMetadataField())) {
                    sortOption.setName(DiscoverySortConfiguration.SCORE);
                } else {
                    sortOption.setName(discoverySearchSortConfiguration.getMetadataField());
                }
                sortOption.setActualName(discoverySearchSortConfiguration.getType());
                sortOption.setSortOrder(discoverySearchSortConfiguration.getDefaultSortOrder().name());
                searchConfigurationRest.addSortOption(sortOption);
            }

            DiscoverySortFieldConfiguration defaultSortField = searchSortConfiguration.getDefaultSortField();
            if (defaultSortField != null) {
                SearchConfigurationRest.SortOption sortOption = new SearchConfigurationRest.SortOption();
                sortOption.setName(defaultSortField.getMetadataField());
                sortOption.setActualName(defaultSortField.getType());
                sortOption.setSortOrder(defaultSortField.getDefaultSortOrder().name());
                searchConfigurationRest.setDefaultSortOption(sortOption);
            }
        }

    }

}
