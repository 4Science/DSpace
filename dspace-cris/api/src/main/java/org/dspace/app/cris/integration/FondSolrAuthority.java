package org.dspace.app.cris.integration;

import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

public class FondSolrAuthority extends DOAuthority
{
    
    /** The logger */
    private static Logger log = Logger.getLogger(FondSolrAuthority.class);
    
    @Override
    public int getCRISTargetTypeID()
    {
        return -1;
    }

    @Override
    public Class<ResearchObject> getCRISTargetClass()
    {
        return ResearchObject.class;
    }

    @Override
    public String getPublicPath()
    {
        return null;
    }

    @Override
    public ResearchObject getNewCrisObject()
    {
        return new ResearchObject();
    }

    @Override
    protected String getDisplayEntry(ACrisObject cris)
    {
        SolrQuery solrQuery = new SolrQuery("cris-id:" + cris.getCrisID());
        solrQuery.addFilterQuery("search.resourcetype:" + getCRISTargetTypeID());
        solrQuery.addField("crisfonds.fondshierarchy");
        try
        {
            QueryResponse response = searchService.search(solrQuery);
            SolrDocumentList docList = response.getResults();
            Iterator<SolrDocument> solrDoc = docList.iterator();
            while (solrDoc.hasNext())
            {
                SolrDocument doc = solrDoc.next();
                String fondshierarchy = (String) doc
                        .getFirstValue("crisfonds.fondshierarchy");
                if(StringUtils.isNotBlank(fondshierarchy)) {
                    return fondshierarchy;
                }
            }
        }
        catch (SearchServiceException e)
        {
            log.error(e.getMessage() , e);
        }
        return cris.getName();
    }
}