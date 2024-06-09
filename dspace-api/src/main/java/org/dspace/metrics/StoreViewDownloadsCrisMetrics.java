/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.metrics;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.app.metrics.service.CrisMetricsServiceImpl;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Context.Mode;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;
import org.json.JSONObject;

/**
 * Implementation of {@link DSpaceRunnable} to add CrisMetrics for view and downloads events of cris items
 *
 * @author alba aliu
 */
public class StoreViewDownloadsCrisMetrics extends
        DSpaceRunnable<StoreViewDownloadsCrisMetricsScriptConfiguration<StoreViewDownloadsCrisMetrics>> {
    private static final int SOLR_PAGINATION = 1000;
    private CrisMetricsService crisMetricsService;
    private IndexingService indexingService;
    private SearchService searchService;
    private static final Logger log = LogManager.getLogger(StoreViewDownloadsCrisMetrics.class);
    private Context context;

    @Override
    public void setup() throws ParseException {
        crisMetricsService = new DSpace().getServiceManager()
                .getServiceByName(CrisMetricsServiceImpl.class.getName(),
                        CrisMetricsServiceImpl.class);
        indexingService = new DSpace().getSingletonService(IndexingService.class);
        searchService = new DSpace().getSingletonService(SearchService.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public StoreViewDownloadsCrisMetricsScriptConfiguration<StoreViewDownloadsCrisMetrics> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("store-metrics",
                StoreViewDownloadsCrisMetricsScriptConfiguration.class);
    }

    @Override
    public void internalRun() throws Exception {
        assignCurrentUserInContext();
        assignSpecialGroupsInContext();
        try {
            context.turnOffAuthorisationSystem();
            context.setMode(Mode.READ_ONLY);
            performUpdateAndStorage(context);
            context.complete();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handler.handleException(e);
            context.abort();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private void performUpdateAndStorage(Context context) {
        try {
            storeMetricsForDso(context, "items", Constants.ITEM);
            storeMetricsForDso(context, "collections", Constants.COLLECTION);
            storeMetricsForDso(context, "communities", Constants.COMMUNITY);
        } catch (SolrServerException | SQLException | IOException exception) {
            log.error(exception.getMessage());
        }
    }

    private QueryResponse findDSO(int type, int start) throws SolrServerException, IOException {
        SolrQuery discoverQuery = new SolrQuery("*:*");
        switch (type) {
            case Constants.ITEM:
                discoverQuery.addFilterQuery("search.resourcetype:Item");
                discoverQuery.addFilterQuery("withdrawn:false");
                discoverQuery.addFilterQuery("archived:true");
                break;
            case Constants.COMMUNITY:
                discoverQuery.addFilterQuery("search.resourcetype:Community");
                break;
            case Constants.COLLECTION:
                discoverQuery.addFilterQuery("search.resourcetype:Collection");
                break;
            default:
                throw new IllegalArgumentException("Type " + type + " not supported");
        }

        discoverQuery.addSort("search.resourceid", ORDER.asc);
        discoverQuery.setStart(start);
        discoverQuery.setRows(SOLR_PAGINATION);
        discoverQuery.setFields("search.resourceid");
        return searchService.getSolrSearchCore().getSolr().query(discoverQuery);
    }

    private void assignCurrentUserInContext() throws SQLException {
        context = new Context();
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    private void assignSpecialGroupsInContext() {
        for (UUID uuid : handler.getSpecialGroups()) {
            context.setSpecialGroup(uuid);
        }
    }

    // this method creates new metrics objects for views and downloads,
    // also returns true/false if there are/aren't previous metrics related with the item
    private boolean createMetricObject(String metricType, double metricCount, UUID uuid, int resourceType, String type)
            throws SQLException, AuthorizeException {
        boolean existentValue = false;
        Double last_week = null;
        Double last_month = null;
        // if already exists a cris metric set last flag to false
        CrisMetrics existentCrisMetrics = crisMetricsService
                .findLastMetricByResourceIdAndMetricsTypes(
                        context, metricType, uuid);
        if (existentCrisMetrics != null) {
            //set last flag value to false
            existentCrisMetrics.setLast(false);
            existentValue = true;
            //if there are values one week before
            last_week = getDeltaPeriod(uuid, "week", metricType);
            //if there are values one month before
            last_month = getDeltaPeriod(uuid, "month", metricType);
        }
        // create new metrics object
        CrisMetrics newMetrics = crisMetricsService.create(context, resourceType, uuid);
        newMetrics.setMetricType(metricType);
        newMetrics.setMetricCount(metricCount);
        newMetrics.setLast(true);
        //set remark
        JSONObject jsonRemark = new JSONObject();
        jsonRemark.put("detailUrl", "/statistics/" + type + "/" + uuid);
        newMetrics.setRemark(jsonRemark.toString());
        if (last_week != null) {
            newMetrics.setDeltaPeriod1(metricCount - last_week);
        }
        if (last_month != null) {
            newMetrics.setDeltaPeriod2(metricCount - last_month);
        }
        indexingService.updateMetrics(context, newMetrics);
        return existentValue;
    }

    private Double getDeltaPeriod(UUID id, String period, String type) throws SQLException {
        Optional<CrisMetrics> metricLast = crisMetricsService
                .getCrisMetricByPeriod(context, type, id, new Date(), period);
        //if there exist values one period ago return metric count value
        return metricLast.map(CrisMetrics::getMetricCount).orElse(null);
    }

    private void storeMetricsForDso(Context context, String path, int type)
            throws SQLException, SolrServerException, IOException {
        int count = 0;
        int countFoundItems = 0;
        int countAddedItems = 0;
        int countUpdatedItems = 0;
        handler.logInfo("Addition start " + Constants.typeText[type]);
        TotalDownloadsAndVisitsGenerator totalDownloadsAndVisitsGenerator = new TotalDownloadsAndVisitsGenerator();
        QueryResponse response = null;
        int start = 0;
        while (response == null || response.getResults().getNumFound() > start) {
            response = findDSO(type, start);
            Iterator<SolrDocument> iterator = response.getResults().iterator();
            start += SOLR_PAGINATION;
            while (iterator.hasNext()) {
                SolrDocument doc = iterator.next();
                // get views and downloads for current item
                UUID uuid = UUID.fromString((String) doc.getFieldValue("search.resourceid"));
                Map<String, Integer> views_downloads = totalDownloadsAndVisitsGenerator.
                        createUsageReport(uuid, type);
                countFoundItems++;
                // crismetrics savage if there are views
                if (views_downloads.get("views") > 0) {
                    try {
                        //add edit cris metrics for views
                        if (createMetricObject("view", views_downloads.get("views"), uuid, type, path)) {
                            //if the method returns true it means that found previous metrics
                            countUpdatedItems++;
                        }
                        countAddedItems++;
                        // crismetrics savage if there are downloads
                        if (views_downloads.get("downloads") > 0) {
                            //add edit cris metrics for downloads
                            if (createMetricObject("download", views_downloads.get("downloads"), uuid, type, path)) {
                                //if the method returns true it means that found previous metrics
                                countUpdatedItems++;
                            }
                            countAddedItems++;
                        }
                    } catch (SQLException e) {
                        log.error(e.getMessage(), e);
                        throw new RuntimeException(e.getMessage(), e);
                    } catch (AuthorizeException e) {
                        log.error(e.getMessage(), e);
                    }
                }
                count++;
                if (count % 20 == 0) {
                    context.commit();
                    context.clear();
                }
            }
        }
        handler.logInfo("Found " + countFoundItems + " " + Constants.typeText[type]);
        handler.logInfo("Added " + countAddedItems + " metrics");
        handler.logInfo("Updated " + countUpdatedItems + " metrics");
        handler.logInfo("Update end");
        context.commit();
    }
}
