/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics.plugin;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.metrics.common.model.ConstantMetrics;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.statistics.CrisSolrLogger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

public class StatsViewIndicatorsPlugin extends AStatsIndicatorsPlugin
{

    private static Logger log = Logger
            .getLogger(StatsViewIndicatorsPlugin.class);

    // set up to CRIS_DYNAMIC_TYPE_ID_START (the initial value of dynamic type
    // to simplify the xml configuration)
    private Integer resourceTypeId = CrisConstants.CRIS_DYNAMIC_TYPE_ID_START;

    private String resourceTypeString;

    @Override
    public void buildIndicator(Context context,
            ApplicationService applicationService, CrisSolrLogger statsService,
            CrisSearchService searchService, String filter)
    {
    	MetricsPersistenceService pService = new DSpace().getSingletonService(MetricsPersistenceService.class); 
        SolrQuery query = new SolrQuery(getQueryDefault());
        if (StringUtils.isNotBlank(getResourceTypeString()))
        {
            query.addFilterQuery("{!field f=resourcetype_authority}"
                    + getResourceTypeString());
        }
        else
        {
            query.addFilterQuery(
                    "{!field f=search.resourcetype}" + getResourceTypeId());
        }
        if(StringUtils.isNotBlank(filter)) {
            query.addFilterQuery(filter);
        }
        else if(StringUtils.isNotBlank(getFilterDefault())) {
            query.addFilterQuery(getFilterDefault());    
        }
        query.setFields("search.resourceid", "search.resourcetype",
                resourceTypeId == Constants.ITEM ? "handle" : "cris-uuid",resourceTypeId >= CrisConstants.CRIS_DYNAMIC_TYPE_ID_START ? "crisdo.name": "objectname","crisdo.type");
        query.setRows(Integer.MAX_VALUE);

        try
        {
            QueryResponse response = searchService.search(query);
            SolrDocumentList docList = response.getResults();
            Iterator<SolrDocument> solrDoc = docList.iterator();
            int count = 0;
            
            String dspaceURL = ConfigurationManager.getProperty("dspace.url");
            String searchCore = ConfigurationManager.getProperty("solr-statistics",
    				"solr.join.core");
            String joinQuery = "{!join from=ORIGINAL_mvuntokenized to=search.uniqueid fromIndex=" +searchCore+"}";
            String baseItemURL = dspaceURL  + "/cris/stats/item.html?handle=";
            String baseCRISURL = dspaceURL+ "/cris/stats/";
            while (solrDoc.hasNext())
            {
            	count++;
            	Date start = new Date();
            	SolrDocument doc = solrDoc.next();
                String uuid = (String) doc
                        .getFirstValue(resourceTypeId == Constants.ITEM
                                ? "handle" : "cris-uuid");
                
                Integer resourceType = (Integer) doc
                        .getFirstValue("search.resourcetype");
                Integer resourceId = (Integer) doc
                        .getFirstValue("search.resourceid");
                try
                {

                    Date acquisitionDate = new Date();
                    String url = "";
                    Map<String, String> remark = new HashMap<String, String>();
                    if(resourceType == Constants.ITEM) {
	                    QueryResponse qr = statsService.query("search.uniqueid:"+ resourceTypeId+"-"+resourceId,  Integer.MAX_VALUE);
	                    url =  baseItemURL+ uuid;
	                    remark.put("link", url);
	                    buildIndicator(pService, applicationService,
	                            uuid, resourceType, resourceId,
	                            qr.getResults().getNumFound(),
	                            ConstantMetrics.STATS_INDICATOR_TYPE_VIEW,
	                            null, acquisitionDate, remark);
	
	                    qr = statsService.query(joinQuery+ "search.resourceid:"+resourceId+" AND -withdrawn:true",  Integer.MAX_VALUE);
	                    remark.clear();
	                    remark.put("link", url +"&amp;type=bitstream" );
	                    buildIndicator(pService, applicationService,
	                            uuid, resourceType, resourceId,
	                            qr.getResults().getNumFound(),
	                            ConstantMetrics.STATS_INDICATOR_TYPE_DOWNLOAD,
	                            null, acquisitionDate, remark);
                    }else {
                    	String publicPath ="";
                    	switch (resourceType) {
                    		case(CrisConstants.RP_TYPE_ID):
                    			publicPath ="rp";
                    			break;
                    		case(CrisConstants.PROJECT_TYPE_ID):
                    			publicPath ="pj";
                    			break;
                    		case(CrisConstants.OU_TYPE_ID):
                    			publicPath ="ou";
                    			break;
                    		default:
                    			publicPath= (String) doc.get("crisdo.type");
                    		}
                    	url = baseCRISURL + publicPath  + ".html?id="+ resourceId;
	                    remark.put("link", url);
	                    QueryResponse qr = statsService.query(resourceTypeId+"-"+resourceId,  Integer.MAX_VALUE);
	                    buildIndicator(pService, applicationService,
	                            uuid, resourceType, resourceId,
	                            qr.getResults().getNumFound(),
	                            ConstantMetrics.STATS_INDICATOR_TYPE_VIEW,
	                            null, acquisitionDate, remark);
	                    remark.clear();
                        remark.put("link", url+"&amp;type=bitstream");
                        
                        qr = statsService.query(resourceTypeId+"-"+resourceId, "sectionid:*", null,0, Integer.MAX_VALUE, null, null, null, null, null, false);
                        buildIndicator(pService, applicationService,
                                uuid, resourceType, resourceId,
                                qr.getResults().getNumFound(),
                                ConstantMetrics.STATS_INDICATOR_TYPE_DOWNLOAD,
                                null, acquisitionDate, remark);
                    }
					try {
						DSpaceObject dspaceObject = DSpaceObject.find(context, resourceType, resourceId);
						context.removeCached(dspaceObject, resourceId);
					} catch (SQLException e) {
						log.warn(e);
					}
                    if (count % 100 == 0) {
                    	applicationService.clearCache();
                    }
                    Date end = new Date();
                    long diff = end.getTime() - start.getTime();
                    System.out.println("VIEW and DOWNLOAD METRICS done for :"+count +" in "+ diff);
                }
                catch (SolrServerException e)
                {
                    log.error("Error retrieving stats", e);
                }
            }
            if(isRenewMetricsCache()) {
                searchService.renewMetricsCache();
            }   
        }
        catch (SearchServiceException e)
        {
            log.error("Error retrieving documents", e);
        }
    }

    public Integer getResourceTypeId()
    {
        return resourceTypeId;
    }

    public void setResourceTypeId(Integer resourceTypeId)
    {
        this.resourceTypeId = resourceTypeId;
    }

    public String getResourceTypeString()
    {
        return resourceTypeString;
    }

    public void setResourceTypeString(String resourceTypeString)
    {
        this.resourceTypeString = resourceTypeString;
    }

}
