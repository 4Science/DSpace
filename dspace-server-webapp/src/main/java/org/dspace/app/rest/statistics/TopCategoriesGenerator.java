/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.statistics;

import static org.dspace.core.Constants.ITEM;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.rest.model.UsageReportPointCategoryRest;
import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.app.rest.utils.UsageReportUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.services.ConfigurationService;
import org.dspace.statistics.content.StatisticsDatasetDisplay;
import org.dspace.statistics.service.SolrLoggerService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This report generator provides the TopCategories related to the site.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 */
public class TopCategoriesGenerator extends AbstractUsageReportGenerator {

    private static final String OTHER_CATEGORY = "OTHER";

    @Autowired
    private SolrLoggerService solrLoggerService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private DiscoveryConfigurationService discoveryConfigurationService;

    private Map<String, String> categoryQueries;

    private Map<String, Map<String, String>> entityCategoryQueries;

    public UsageReportRest createUsageReport(Context context, DSpaceObject dso, String startDate, String endDate) {

        setCategoryQueries(getEntityType(dso));
        Map<String, Integer> categoriesCount = getCategoriesCount(dso, startDate, endDate);

        UsageReportRest usageReportRest = new UsageReportRest();

        for (String category : categoriesCount.keySet()) {
            UsageReportPointCategoryRest categoryPoint = new UsageReportPointCategoryRest();
            categoryPoint.setId(category);
            categoryPoint.addValue("views", categoriesCount.get(category));
            usageReportRest.addPoint(categoryPoint);
        }

        return usageReportRest;
    }

    private Map<String, Integer> getCategoriesCount(DSpaceObject dso, String startDate, String endDate) {

        DiscoveryConfiguration discoveryConfiguration = getDiscoveryConfiguration();

        Map<String, Integer> categoriesCount = new HashMap<String, Integer>();

        for (String category : categoryQueries.keySet()) {
            String categoryQuery = categoryQueries.get(category);
            Integer categoryCount = getCategoryCount(dso, discoveryConfiguration, categoryQuery, startDate, endDate);
            categoriesCount.put(category, categoryCount);
        }

        return categoriesCount;

    }

    private int getCategoryCount(DSpaceObject dso, DiscoveryConfiguration discoveryConfiguration,
        String categoryQuery, String startDate, String endDate) {

        String query = composeCategoryQuery(dso, discoveryConfiguration, categoryQuery);
        String filterQuery = new StatisticsDatasetDisplay().composeFilterQuery(startDate, endDate, true, ITEM);

        try {
            return (int) solrLoggerService.queryTotal(query, filterQuery, 0).getCount();
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String composeCategoryQuery(DSpaceObject dso, DiscoveryConfiguration configuration, String categoryQuery) {

        List<String> defaultFilterQueries = configuration.getDefaultFilterQueries();
        String query = new StatisticsDatasetDisplay().composeQueryWithInverseRelation(dso,
            defaultFilterQueries, dso.getType());

        if (categoryQuery.equals(OTHER_CATEGORY)) {
            return query + " AND " + getAllCategoryQueriesReverted();
        }

        return query + " AND " + formatCategoryQuery(categoryQuery);

    }

    private String getAllCategoryQueriesReverted() {
        return categoryQueries.values().stream()
            .filter(categoryQuery -> !OTHER_CATEGORY.equals(categoryQuery))
            .map(categoryQuery -> "-" + formatCategoryQuery(categoryQuery))
            .collect(Collectors.joining(" AND "));
    }

    private String formatCategoryQuery(String categoryQuery) {
        return "(" + categoryQuery.replaceAll("'", "\"") + ")";
    }

    private DiscoveryConfiguration getDiscoveryConfiguration() {
        String name = StringUtils.isBlank(getRelation()) ? "default" : getRelation();
        DiscoveryConfiguration configuration = discoveryConfigurationService.getDiscoveryConfigurationByName(name);
        if (configuration == null) {
            throw new IllegalStateException("No configuration found with name " + name);
        }
        return configuration;
    }

    @Override
    public String getReportType() {
        return UsageReportUtils.TOP_CATEGORIES_REPORT_ID;
    }

    private String getEntityType(DSpaceObject dso) {
        return dso.getMetadata()
                  .stream()
                  .filter(metadataValue ->
                      "dspace.entity.type".equals(metadataValue.getMetadataField().toString('.')))
                  .map(MetadataValue::getValue)
                  .findFirst()
                  .orElse("");
    }

    private void setCategoryQueries(String entityType) {
        if (entityCategoryQueries != null &&
            entityCategoryQueries.containsKey(entityType)) {
            categoryQueries = entityCategoryQueries.get(entityType);
        } else {
            categoryQueries = getDefaultCategoryQueries();
        }
    }

    private Map<String, String> getDefaultCategoryQueries() {
        return Arrays.stream(getDefaultEntityTypes())
                     .collect(Collectors.toMap(
                         type -> type.toLowerCase(),
                         type -> "entityType_keyword: '" + type + "'"
                     ));
    }

    private String[] getDefaultEntityTypes() {
        return configurationService.getArrayProperty("cris.entity-type");
    }

    public Map<String, Map<String, String>> getEntityCategoryQueries() {
        return entityCategoryQueries;
    }

    public void setEntityCategoryQueries(
        Map<String, Map<String,String>> entityCategoryQueries) {
        this.entityCategoryQueries = entityCategoryQueries;
    }

}
