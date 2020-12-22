package org.dspace.app.cris.ctask.general;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.discovery.IndexClient;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

/**
 * Curation task to reindex all children of given tree node.
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.it)
 *
 */
public class TreeReindexCurationTask extends AbstractCurationTask {

    private Logger log = Logger.getLogger(TreeReindexCurationTask.class);

    private SearchService searchService = new DSpace().getServiceManager().getServiceByName(
            SearchService.class.getName(), SearchService.class);

    @Override
    public int perform(DSpaceObject dso)
            throws IOException {
        distribute(dso);
        return Curator.CURATE_SUCCESS;
    }

    @Override
    protected void performItem(Item item)
            throws SQLException, IOException {
        indexObjects(item.getHandle());
    }

    @Override
    public int perform(Context ctx, String id)
            throws IOException {
        indexObjects(id);
        return Curator.CURATE_SUCCESS;
    }

    private void indexObjects(String id) {
        List<String> objectsToReindex = new ArrayList<>();

        try {
            // retrieve children recursively by parent node
            objectsToReindex = findByNode(id);

            // reindex objects
            IndexClient.main(new String[] {"-f", "-u", StringUtils.join(objectsToReindex, ',')});
        }
        catch (SearchServiceException | SQLException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private List<String> findByNode(String id)
            throws SearchServiceException {
        List<String> objectsToReindex = new ArrayList<>();

        // retrieve children recursively by parent node
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("treeparent_s:\""+ id + "\"");
        solrQuery.setFields("handle", "cris-id");
        solrQuery.setRows(Integer.MAX_VALUE);

        QueryResponse response = searchService.search(solrQuery);
        SolrDocumentList docList = response.getResults();
        for (SolrDocument doc : docList) {
            String authority = (String)(doc.getFieldValue("cris-id"));
            if (StringUtils.isBlank(authority)) {
                authority = (String)(doc.getFieldValue("handle"));
            }
            objectsToReindex.add(authority);
            objectsToReindex.addAll(findByNode(authority));
        }

        return objectsToReindex;
    }
}
