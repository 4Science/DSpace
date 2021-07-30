package org.dspace.app.cris.integration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.integration.defaultvalues.DefaultValuesBean;
import org.dspace.content.integration.defaultvalues.EnhancedValuesGenerator;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;

public class TreeRootGenerator implements EnhancedValuesGenerator
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(TreeRootGenerator.class);
    
    private SearchService searchService;

    private String fieldName;

    @Override
    public DefaultValuesBean generateValues(Item item, String schema,
            String element, String qualifier, String value)
    {
        DefaultValuesBean result = new DefaultValuesBean();
        if (item != null)
        {
            result.setLanguage("en");
            result.setMetadataSchema(schema);
            result.setMetadataElement(element);
            result.setMetadataQualifier(qualifier);
            
            Metadatum[] metadatums = item.getMetadata(schema, element, qualifier,
                    Item.ANY);
            SolrQuery solrQuery = new SolrQuery();
            String query = "";
            int i = 0;
            for (Metadatum metadatum : metadatums)
            {
                if(i!=0) {
                    query += " OR ";    
                }
                query += "cris-id:\"" + metadatum.authority + "\"";
                i++;
            }
            solrQuery.setQuery(query);
            solrQuery.setRows(Integer.MAX_VALUE);
            solrQuery.setFields("treeroot_s", "treerootname_s");
            try
            {
                QueryResponse response = searchService.search(solrQuery);
                if (response != null)
                {
                    SolrDocumentList docList = response.getResults();
                    if (docList != null && docList.getNumFound() > 0)
                    {
                        List<String> values = new ArrayList<String>();
                        List<String> authorities = new ArrayList<String>();
                        for (SolrDocument doc : docList)
                        {
                            String rootCrisId = (String) (doc
                                    .getFieldValue("treeroot_s"));
                            String rootName = (String) (doc
                                    .getFieldValue("treerootname_s"));

                            authorities.add(rootCrisId);
                            values.add(rootName);
                        }
                        
                        Object[] arrayA = authorities.toArray();
                        String[] arrayAuthorities = Arrays.copyOf(arrayA, arrayA.length, String[].class);
                        
                        Object[] arrayB = values.toArray();
                        String[] arrayValues = Arrays.copyOf(arrayB, arrayB.length, String[].class);
                        
                        result.setAuthorities(arrayAuthorities);
                        result.setValues(arrayValues);
                    }
                }
            }
            catch (SearchServiceException e)
            {
                log.error(e.getMessage(), e);
            }
        }
        return result;
    }

    public SearchService getSearchService()
    {
        return searchService;
    }

    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
}
