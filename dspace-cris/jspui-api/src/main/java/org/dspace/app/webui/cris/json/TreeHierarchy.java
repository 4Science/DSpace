/**
 *
 */
package org.dspace.app.webui.cris.json;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.configuration.RelationConfiguration;
import org.dspace.app.cris.discovery.tree.TreeViewConfigurator;
import org.dspace.app.webui.json.JSONRequest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
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
    
    private TreeViewConfigurator configurator;
    
    @Override
    public void doJSONRequest(Context context, HttpServletRequest req,
            HttpServletResponse resp) throws AuthorizeException, IOException
    {
        Gson json = new Gson();
        String selectedNode = req.getParameter("id");
        String lazyLoad = req.getParameter("lazy");

        List<JSNodeDTO> dto = new ArrayList<JSNodeDTO>();

        if (StringUtils.isWhitespace(lazyLoad))
        {
            if((StringUtils.isBlank(selectedNode) || selectedNode.equals("#"))) {
                String pluginName = req.getPathInfo();
                
                if (pluginName.startsWith("/"))
                {
                    pluginName = pluginName.substring(1);
                    selectedNode = pluginName.split("/")[1];
                }
            }
            
            SolrQuery q1 = new SolrQuery();
            q1.setQuery("cris-id:\"" + selectedNode + "\"");
            q1.setFields("crisauthoritylookup","treecontext_s","treeparent_s", "treeparents_mvuntokenized", "treeroot_s");
            q1.setRows(1);
            try
            {
                QueryResponse response1 = searchService.search(q1);
                SolrDocumentList docList1 = response1.getResults();
                for (SolrDocument doc : docList1)
                {
                    
                    JSNodeDTO nodeParent = new JSNodeDTO();
                    nodeParent.setId(selectedNode);
                    
                    String value = "";
                    if (doc.getFieldValue(
                            "crisauthoritylookup") instanceof String)
                    {
                        value = (String) doc
                                .getFieldValue("crisauthoritylookup");
                    }
                    else
                    {
                        for (String ss : (List<String>) doc
                                .getFieldValue("crisauthoritylookup"))
                        {
                            value += ss;
                        }
                    }
                    nodeParent.setText(value);
                    String contextTree = (String)(doc.getFieldValue("treecontext_s"));
                    String cssIcon = configurator.getIcons().get(contextTree);
                    if(StringUtils.isNotBlank(cssIcon)) {
                        nodeParent.setIcon(cssIcon);
                    }
                    
                    // initializing childs
                    nodeParent.setChildren(new ArrayList<JSNodeChildrenDTO>());
                    // only the first level
                    SolrQuery q2 = new SolrQuery();
                    q2.setQuery("treeparent_s:\"" + selectedNode + "\"");
                    q2.setFields("crisauthoritylookup","handle", "cris-id", "treeleaf_b", "treecontext_s");
                    q2.setRows(Integer.MAX_VALUE);
                    try
                    {
                        QueryResponse response2 = searchService.search(q2);
                        SolrDocumentList docList2 = response2.getResults();
                        for(SolrDocument doc2 : docList2) {
                            JSNodeChildrenDTO node = new JSNodeChildrenDTO();
                            String value2 = "";
                            if (doc2.getFieldValue("crisauthoritylookup") instanceof String)
                            {
                                value2 = (String) doc2.getFieldValue("crisauthoritylookup");
                            }
                            else
                            {
                                for (String ss : (List<String>) doc2.getFieldValue("crisauthoritylookup"))
                                {
                                    value2 += ss;
                                }
                            }                    
                            String authority2 = (String)(doc2.getFieldValue("cris-id"));
                            if(StringUtils.isBlank(authority2)) {
                                authority2 = (String)(doc2.getFieldValue("handle"));
                            }
                            node.setId(authority2);
                            node.setText(value2);
                            String contextTree2 = (String)(doc2.getFieldValue("treecontext_s"));
                            String cssIcon2 = configurator.getIcons().get(contextTree2);
                            if(StringUtils.isNotBlank(cssIcon2)) {
                                node.setIcon(cssIcon2);
                            }
                            
                            // check if have childs
                            SolrQuery q3 = new SolrQuery();
                            q3.setQuery("treeparent_s:\"" + authority2 + "\"");
                            q3.setRows(1);
                            QueryResponse response3 = searchService.search(q3);
                            if(response3.getResults()!=null) {
                                if(response3.getResults().size()>0) {
                                    node.setChildren(true);
                                }
                            }
                            
                            nodeParent.getChildren().add(node);
                        }
                    }
                    catch (SearchServiceException e)
                    {
                        log.error(e.getMessage(), e);
                    }
                    
                    dto.add(nodeParent);
                }
            }
            catch (SearchServiceException e)
            {
                log.error(e.getMessage(), e);
            }

        }
        else if ((StringUtils.isNotBlank(selectedNode) && !selectedNode.equals("#")))
        {
            // only the first level
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery("treeparent_s:\"" + selectedNode + "\"");
            solrQuery.setFields("crisauthoritylookup","handle", "cris-id", "treeleaf_b");
            solrQuery.setRows(Integer.MAX_VALUE);
            try
            {
                QueryResponse response = searchService.search(solrQuery);
                SolrDocumentList docList = response.getResults();
                for(SolrDocument doc : docList) {
                    JSNodeDTO node = new JSNodeDTO();
                    String value = "";
                    if (doc.getFieldValue("crisauthoritylookup") instanceof String)
                    {
                        value = (String) doc.getFieldValue("crisauthoritylookup");
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
            solrQuery.setFields("treeroot_s");
            try
            {
                QueryResponse response = searchService.search(solrQuery);
                SolrDocumentList docList = response.getResults();
                for(SolrDocument doc : docList) {
                    String valueRoot = (String)(doc.getFieldValue("treeroot_s"));
                    if(StringUtils.isBlank(valueRoot)) {
                        valueRoot = pluginName;
                    }
                    //get all node leads by this root node
                    SolrQuery solrQueryInternal = new SolrQuery();
                    solrQueryInternal.setQuery("treeroot_s:\""+ valueRoot + "\"");
                    solrQueryInternal.setFields("crisauthoritylookup","handle", "cris-id", "treeparent_s", "treeleaf_b", "treecontext_s", "treenodeclosed_b");
                    solrQueryInternal.setRows(Integer.MAX_VALUE);
                    QueryResponse responseInternal = searchService.search(solrQueryInternal);
                    SolrDocumentList docListInternal = responseInternal.getResults();
                    for(SolrDocument docInternal : docListInternal) {                  
                        JSNodeDTO node = new JSNodeDTO();
                        JSNodeStateDTO jsNodeStateDTO = new JSNodeStateDTO();
                        String value = "";
                        if (docInternal.getFieldValue("crisauthoritylookup") != null)
                        {
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
                            jsNodeStateDTO.setSelected(true);
                        }
                        
                        String contextTree = (String)(docInternal.getFieldValue("treecontext_s"));
                        
                        node.setId(authority);
                        node.setText(value);
                        node.setParent(parent);
                        String cssIconDefault = configurator.getIcons().get(contextTree);
                        String cssIcon = configurator.getIcons().get(contextTree+"-"+authority);
                        if(StringUtils.isNotBlank(cssIcon)) {
                            node.setIcon(cssIcon);
                        } else {
                            node.setIcon(cssIconDefault);
                        }
                        Boolean nodeIsClosed = (Boolean) (docInternal
                                .getFieldValue("treenodeclosed_b"));
                        if (nodeIsClosed != null)
                        {
                            if (nodeIsClosed)
                            {
                                jsNodeStateDTO.setOpened(false);
                            }
                            else
                            {
                                jsNodeStateDTO.setOpened(true);
                            }
                        }
                        node.setState(jsNodeStateDTO);
                        dto.add(node);
                        
                        // to show relations on all node you have to setup "leafs" mapping to empty or leave "treeleaf_b" metadata blank or setup all to true
                        Boolean showRelationOnLeafs = configurator.getShowRelationOnLeaf().get(contextTree);
                        Boolean showRelationCount = configurator.getShowRelationCount().get(contextTree);
                        Boolean leaf = (Boolean)(docInternal.getFieldValue("treeleaf_b"));
                        
                        Map<String,String> mapAttribute = new HashMap<String, String>();
                        node.setA_attr(mapAttribute);
                        if(showRelationCount || showRelationOnLeafs) {

                                // retrieve all items
                                // get all node leads by this root node
                                SolrQuery solrQueryItem = new SolrQuery();
                                List<RelationConfiguration> relations = configurator.getRelations().get(contextTree);
                                for(RelationConfiguration relation : relations) {
                                solrQueryItem
                                .setQuery(MessageFormat.format(
                                        relation
                                                .getQuery(),
                                        authority));
                                
                                String relationName = StringUtils.replace(relation.getRelationName(), ".", "-", -1);
                                if (relation.getRelationClass()
                                        .isAssignableFrom(Item.class))
                                {

                                    solrQueryItem.setFilterQueries(
                                            "-withdrawn:true");
                                    solrQueryItem.setFields(
                                            "search.resourcetype",
                                            "search.resourceid", "handle");
                                    for (String field : configurator.getLeafTitleMetadata()) {
        								solrQueryItem.addField(field);
        							}
                                    addSortFields(solrQueryItem);
                                    solrQueryItem.setRows(
                                            Integer.valueOf(Integer.MAX_VALUE));
                                    QueryResponse responseItem = searchService
                                            .search(solrQueryItem);
                                    SolrDocumentList docListItem = responseItem
                                            .getResults();
                                    // setup "leaf" to null or setup to "true"
                                    // means show relation
                                    if (showRelationOnLeafs && (leaf == null || leaf))
                                    {
                                        for (SolrDocument docItem : docListItem)
                                        {
                                            JSNodeDTO nodeItem = new JSNodeDTO();
                                            String handle = (String) (docItem
                                                    .getFieldValue("handle"));
                                            nodeItem.setId(
                                                    handle.replace("/", "_"));
                                            nodeItem.setParent(authority);
                                            String valueItem = configurator.getLeafLabel(docItem);
                                            nodeItem.setText(valueItem);
                                            nodeItem.setIcon("fa fa-bars");
                                            dto.add(nodeItem);
                                        }
                                    }
                                    if (docListItem != null)
                                    {
                                        mapAttribute.put(
                                                "data-count-" + relationName,
                                                "" + docListItem.getNumFound());
                                    }
                                }
                                else
                                {
                                    solrQueryItem.setFilterQueries(
                                            "-withdrawn:true");
                                    solrQueryItem.setFields(
                                            "search.resourcetype",
                                            "search.resourceid", "cris-id");
                                    for (String field : configurator.getLeafTitleMetadata()) {
        								solrQueryItem.addField(field);
        							}
                                    solrQueryItem.setRows(
                                            Integer.valueOf(Integer.MAX_VALUE));
                                    QueryResponse responseItem = searchService
                                            .search(solrQueryItem);
                                    SolrDocumentList docListItem = responseItem
                                            .getResults();
                                    // setup "leaf" to null or setup to "true"
                                    // means show relation
                                    if (showRelationOnLeafs && (leaf == null || leaf))
                                    {
                                        for (SolrDocument docItem : docListItem)
                                        {
                                            JSNodeDTO nodeItem = new JSNodeDTO();
                                            String handle = (String) (docItem
                                                    .getFieldValue("cris-id"));
                                            nodeItem.setId(handle);
                                            nodeItem.setParent(authority);
                                            String valueItem = configurator.getLeafLabel(docItem);
                                            nodeItem.setText(valueItem);
                                            nodeItem.setIcon("fa fa-bars");
                                            dto.add(nodeItem);
                                        }
                                    }
                                    if (docListItem != null)
                                    {
                                        mapAttribute.put(
                                                "data-count-" + relationName,
                                                "" + docListItem.getNumFound());
                                    }
                                }
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

    public void setConfigurator(TreeViewConfigurator configurator)
    {
        this.configurator = configurator;
    }

}
