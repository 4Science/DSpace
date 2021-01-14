/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.cris.json;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.discovery.ConfiguratorResource;
import org.dspace.app.webui.json.JSONRequest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * @author Luigi Andrea Pascarelli
 */
public class TreeHierarchy extends JSONRequest
{

    private static Logger log = Logger
            .getLogger(TreeHierarchy.class);

    private static final String DEFAULT_ENTITY = "item";

    private SearchService searchService;
    
    private ConfiguratorResource configurator;
    
    @Override
    public void doJSONRequest(Context context, HttpServletRequest req,
            HttpServletResponse resp) throws AuthorizeException, IOException
    {
        Gson json = new Gson();
        String selectedNode = req.getParameter("id");

        List<JSNodeDTO> dto = new ArrayList<JSNodeDTO>();
        
        if (StringUtils.isNotBlank(selectedNode) && !selectedNode.equals("#"))
        {
            // only the first level
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery("treeparent_s:\"" + selectedNode + "\"");
            solrQuery.setRows(Integer.MAX_VALUE);
            solrQuery.setFields("crisauthoritylookup","handle", "cris-id", "treeleaf_b");
            try
            {
                QueryResponse response = searchService.search(solrQuery);
                SolrDocumentList docList = response.getResults();
                for(SolrDocument doc : docList) {
                    JSNodeDTO node = new JSNodeDTO();
                    String value = "";
                    if (doc.getFieldValue("crisauthoritylookup") instanceof String)
                    {
                        value = (String) doc.getFieldValue("v");
                    }
                    else
                    {

                        for (String ss : (List<String>) doc.getFieldValue("crisauthoritylookup"))
                        {
                            value += ss;
                        }
                    }                    
                    String authority = (String)(doc.getFieldValue("cris-id"));
                    if(StringUtils.isBlank(authority)) {
                        authority = (String)(doc.getFieldValue("handle"));
                    }
                    node.setId(authority);
                    node.setText(value);
                    node.setParent(selectedNode);
                    dto.add(node);
                }
            }
            catch (SearchServiceException e)
            {
                log.error(e.getMessage(), e);
            }
        }
        else
        {
            // all tree hierarchy with depth equals the requested node
            String pluginName = req.getPathInfo();
            
            if (pluginName.startsWith("/"))
            {
                pluginName = pluginName.substring(1);
                pluginName = pluginName.split("/")[1];
            }
            //get the node related to this cris-id
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery("cris-id:\""+ pluginName + "\" OR handle:\""+ pluginName + "\"");
            solrQuery.setRows(Integer.MAX_VALUE);
            solrQuery.setFields("treecontext_s", "treeroot_s");
            try
            {
                QueryResponse response = searchService.search(solrQuery);
                SolrDocumentList docList = response.getResults();
                for(SolrDocument doc : docList) {
                    String contextTree = (String)(doc.getFieldValue("treecontext_s"));
                    String valueRoot = (String)(doc.getFieldValue("treeroot_s"));
                    if(StringUtils.isBlank(valueRoot)) {
                        valueRoot = pluginName;
                    }
                    //get all node leads by this root node
                    SolrQuery solrQueryInternal = new SolrQuery();
                    solrQueryInternal.setQuery("treeroot_s:\""+ valueRoot + "\"");
                    solrQueryInternal.setFields("crisauthoritylookup","handle", "cris-id", "treeparent_s", "treeleaf_b");
                    solrQueryInternal.setRows(Integer.MAX_VALUE);
                    addSortFields(solrQueryInternal, contextTree);
                    QueryResponse responseInternal = searchService.search(solrQueryInternal);
                    SolrDocumentList docListInternal = responseInternal.getResults();
                    for(SolrDocument docInternal : docListInternal) {                  
                        JSNodeDTO node = new JSNodeDTO();
                        String value = "";
                        if (docInternal.getFieldValue("crisauthoritylookup") instanceof String)
                        {
                            value = (String) docInternal.getFieldValue("crisauthoritylookup");
                        }
                        else
                        {

                            for (String ss : (List<String>) docInternal.getFieldValue("crisauthoritylookup"))
                            {
                                value = ss;
                            }
                        }
                        String authority = (String)(docInternal.getFieldValue("cris-id"));
                        if(StringUtils.isBlank(authority)) {
                            authority = (String)(docInternal.getFieldValue("handle"));
                        }
                        
                        String parent = (String)(docInternal.getFieldValue("treeparent_s"));
                        if(StringUtils.isBlank(parent)) {
                            parent = "#";
                        }
                        if(authority.equals(pluginName)) {
                            JSNodeStateDTO jsNodeStateDTO = new JSNodeStateDTO();
                            jsNodeStateDTO.setSelected(true);
                            node.setState(jsNodeStateDTO);
                        }
                        node.setId(authority);
                        node.setText(value);
                        node.setParent(parent);
                        dto.add(node);
                        
                        Boolean leaf = (Boolean)(docInternal.getFieldValue("treeleaf_b"));
                        if(leaf) {
                            //retrieve all items
                            //get all node leads by this root node
                            SolrQuery solrQueryItem = new SolrQuery();
                            solrQueryItem.setQuery(MessageFormat.format(configurator.getRelation().get(contextTree).getQuery(),authority));
                            solrQueryItem.setFilterQueries("-withdrawn:true");
                            solrQueryItem.setRows(Integer.MAX_VALUE);
                            solrQueryItem.setFields("search.resourcetype", "search.resourceid", "handle", "dc.title");
                            addSortFields(solrQueryItem);
                            QueryResponse responseItem = searchService.search(solrQueryItem);
                            SolrDocumentList docListItem = responseItem.getResults();
                            for(SolrDocument docItem : docListItem) {
                                JSNodeDTO nodeItem = new JSNodeDTO();
                                String handle = (String)(docItem.getFieldValue("handle"));
                                nodeItem.setId(handle.replace("/", "_"));
                                nodeItem.setParent(authority);
                                String valueItem = "";
                                if (docItem.getFieldValue("dc.title") instanceof String)
                                {
                                    valueItem = (String) docItem.getFieldValue("dc.title");
                                }
                                else
                                {

                                    for (String ss : (List<String>) docItem.getFieldValue("dc.title"))
                                    {
                                        valueItem = ss;
                                    }
                                }
                                nodeItem.setText(valueItem);
                                nodeItem.setIcon("fa fa-bars");
                                dto.add(nodeItem);
                            }
                        }
                        
                    }
                }
            }
            catch (SearchServiceException e)
            {
                log.error(e.getMessage(), e);
            }
        }

        JsonElement tree = json.toJsonTree(dto);
        resp.getWriter().write(tree.toString());
    }

    public void addSortFields(SolrQuery solrQuery) {
        addSortFields(solrQuery, DEFAULT_ENTITY);
    }

    public void addSortFields(SolrQuery solrQuery, String contextTree) {
        // retrieve all configured sort fields and sort order
        List<DiscoverySortFieldConfiguration> sortFields = configurator.getSort().get(contextTree);
        String sortOrder = configurator.getSortOrder().get(contextTree);
        if (sortFields != null && !sortFields.isEmpty()) {
            for (DiscoverySortFieldConfiguration sortField : sortFields) {
                if(sortField != null) {
                    // add sort clause to Solr query
                    solrQuery.addSort(
                            SearchUtils.getSearchService().toSortFieldIndex(
                                    sortField.getMetadataField(), sortField.getType()),
                            StringUtils.equalsIgnoreCase(sortOrder, "asc") ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
                }
            }
        }
    }

    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }

    public void setConfigurator(ConfiguratorResource configurator)
    {
        this.configurator = configurator;
    }

}
