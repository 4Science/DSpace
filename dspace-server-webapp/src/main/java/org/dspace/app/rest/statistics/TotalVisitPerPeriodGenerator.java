/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.statistics;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.rest.model.UsageReportPointDateRest;
import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.app.rest.utils.UsageReportUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.content.DatasetTimeGenerator;
import org.dspace.statistics.content.StatisticsDatasetDisplay;
import org.dspace.statistics.content.filter.StatisticsSolrDateFilter;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;
import org.dspace.util.SolrUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;

/**
 * This report generator provides usage data aggregated over a specific period
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class TotalVisitPerPeriodGenerator extends AbstractUsageReportGenerator {
    private static final String POINT_VIEWS_KEY = "views";

    private String periodType = "month";

    private int increment = 1;

    private Integer dsoType;

    protected final SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public void setIncrement(int increment) {
        this.increment = increment;
    }

    @Autowired
    private DiscoveryConfigurationService discoveryConfigurationService;

    /**
     * Create a stat usage report for the amount of TotalVisitPerMonth on a DSO, containing one point for each month
     * with the views on that DSO in that month with the range -6 months to now. If there are no views on the DSO
     * in a month, the point on that month contains views=0.
     *
     * @param context   DSpace context
     * @param dso       DSO we want usage report with TotalVisitsPerMonth to the DSO
     * @param startDate String to filter the start date of statistic
     * @param endDate   String to filter the end date of statistic
     * @return Rest object containing the TotalVisits usage report on the given DSO
     */
    public UsageReportRest createUsageReport(Context context, DSpaceObject dso, String startDate, String endDate)
        throws IOException, SolrServerException {
        boolean showTotal = new DatasetTimeGenerator().isIncludeTotal();
        boolean hasValidRelation = false;
        StatisticsDatasetDisplay statisticsDatasetDisplay = new StatisticsDatasetDisplay();
        StringBuilder query = new StringBuilder();

        if (getRelation() != null) {
            DiscoveryConfiguration discoveryConfiguration = discoveryConfigurationService
                                                                .getDiscoveryConfigurationByName(getRelation());
            if (discoveryConfiguration == null) {
                // not valid because not found bean with this relation configuration name
                hasValidRelation = false;
            } else {
                hasValidRelation = true;
                query.append(
                    statisticsDatasetDisplay
                        .composeQueryWithInverseRelation(
                                dso,
                                discoveryConfiguration.getDefaultFilterQueries()
                        )
                );
            }
        }

        if (!hasValidRelation) {
            if (getDsoType(dso) != -1) {
                query.append("type: ").append(getDsoType(dso));
            }

            if (isNotSiteObject(dso)) {
                query.append((query.length() == 0 ? "" : " AND "))
                     .append("id:").append(dso.getID());
            }
        }

        //add date facets to filter query
        StringBuilder filterQuery = new StringBuilder("");
        String startStr = "-" + (increment * getMaxResults());
        String endStr = "+" + increment;
        StatisticsSolrDateFilter solrDateFilter = new StatisticsSolrDateFilter(startStr, endStr, periodType);
        String dateQueryFilter = solrDateFilter.toQuery();

        filterQuery
            .append("(")
            .append(dateQueryFilter)
            .append(")")
            .append(" AND ")
            .append(
                statisticsDatasetDisplay
                    .composeFilterQuery(startDate, endDate, hasValidRelation, getDsoType(dso))
            );

        Consumer<Calendar> mapper = createConsumerFrom(periodType.toUpperCase());
        String dateformatString = SolrUtils.getDateformatFrom(periodType.toUpperCase());

        Map<String, Pair<Integer, UsageReportPointDateRest>> reportValues =
                generateStatisticsRange(context, solrDateFilter.getStartDate(), mapper, dateformatString);

        // execute query
        ObjectCount[] dateFacetResult = solrLoggerService.queryFacetDateField(context, "id", null, query.toString(),
                filterQuery.toString(), periodType.toUpperCase(), startStr, endStr, showTotal, 1, getMaxResults());

        for (ObjectCount maxDateFacetCount : dateFacetResult) {
            Pair<Integer, UsageReportPointDateRest> pair =
                    Optional.ofNullable(reportValues.get(maxDateFacetCount.getValue()))
                            .orElse(Pair.of(reportValues.size(), new UsageReportPointDateRest()));
            UsageReportPointDateRest monthPoint = pair.getSecond();
            monthPoint.setId(maxDateFacetCount.getValue());
            monthPoint.addValue(POINT_VIEWS_KEY, (int) maxDateFacetCount.getCount());
            reportValues.put(maxDateFacetCount.getValue(), pair);
        }

        UsageReportRest usageReportRest = new UsageReportRest();
        ArrayList<Pair<Integer, UsageReportPointDateRest>> pairs = new ArrayList<>(reportValues.values());
        pairs
            .stream()
            .sorted((a, b) -> Integer.compare(a.getFirst(), b.getFirst()))
            .map(Pair::getSecond)
            .forEach(usageReportRest::addPoint);

        return usageReportRest;
    }

    protected Consumer<Calendar> createConsumerFrom(String periodType) {
        Consumer<Calendar> mapper = null;
        if ("DAY".equalsIgnoreCase(periodType)) {
            mapper = (cal) -> cal.add(Calendar.DAY_OF_YEAR, 1);
        } else if ("MONTH".equalsIgnoreCase(periodType)) {
            mapper = (cal) -> cal.add(Calendar.MONTH, 1);
        } else if ("YEAR".equalsIgnoreCase(periodType)) {
            mapper = (cal) -> cal.add(Calendar.YEAR, 1);
        }
        return mapper;
    }

    protected Map<String, Pair<Integer, UsageReportPointDateRest>> generateStatisticsRange(Context context,
            Date startDate, Consumer<Calendar> mapper, String dateformatString) {
        Calendar calendar = Calendar.getInstance(context.getCurrentLocale());
        calendar.setTime(startDate);
        Map<String, Pair<Integer, UsageReportPointDateRest>> reportValues = new HashMap<>();
        SimpleDateFormat simpleFormat = new SimpleDateFormat(dateformatString, context.getCurrentLocale());
        String id = simpleFormat.format(calendar.getTime());
        UsageReportPointDateRest usageReportPointDateRest = new UsageReportPointDateRest();
        usageReportPointDateRest.setId(id);
        usageReportPointDateRest.setLabel(id);
        usageReportPointDateRest.addValue(POINT_VIEWS_KEY, 0);
        reportValues.put(id, Pair.of(0, usageReportPointDateRest));
        for (int i = 1; i <= increment * getMaxResults(); i++) {
            mapper.accept(calendar);
            usageReportPointDateRest = new UsageReportPointDateRest();
            id = simpleFormat.format(calendar.getTime());
            usageReportPointDateRest.setId(id);
            usageReportPointDateRest.addValue(POINT_VIEWS_KEY, 0);
            reportValues.put(id, Pair.of(i, usageReportPointDateRest));
        }
        return reportValues;
    }

    @Override
    public String getReportType() {
        return UsageReportUtils.TOTAL_VISITS_PER_MONTH_REPORT_ID;
    }

    public UsageReportRest returnEmptyDataReport(Context context) {
        UsageReportRest usageReportRest = new UsageReportRest();
        Calendar cal = Calendar.getInstance(context.getCurrentLocale());
        for (int k = 0; k <= increment * getMaxResults(); k++) {
            UsageReportPointDateRest monthPoint = new UsageReportPointDateRest();
            monthPoint.addValue(POINT_VIEWS_KEY, 0);
            String month = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, context.getCurrentLocale());
            monthPoint.setId(month + " " + cal.get(Calendar.YEAR));
            usageReportRest.addPoint(monthPoint);
            cal.add(Calendar.MONTH, -1);
        }
        return usageReportRest;
    }

    private boolean isNotSiteObject(DSpaceObject dso) {
        return dso.getType() != Constants.SITE;
    }

    private Integer getDsoType(DSpaceObject dso) {
        return dsoType != null ? dsoType : dso.getType();
    }

    public void setDsoType(Integer dsoType) {
        this.dsoType = dsoType;
    }
}
