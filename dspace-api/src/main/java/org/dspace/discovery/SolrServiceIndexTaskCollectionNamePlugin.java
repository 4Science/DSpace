/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableClaimedTask;
import org.dspace.discovery.indexobject.IndexablePoolTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link SolrServiceIndexPlugin} that add an index related to
 * the task's collection name.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class SolrServiceIndexTaskCollectionNamePlugin implements SolrServiceIndexPlugin {

    public static final Logger LOGGER = LoggerFactory.getLogger(SolrServiceIndexTaskCollectionNamePlugin.class);

    @Override
    @SuppressWarnings("rawtypes")
    public void additionalIndex(Context context, IndexableObject dso, SolrInputDocument document) {

        try {

            Collection collection = getCollection(dso);
            if (collection == null) {
                return;
            }

            document.addField("collection.name", collection.getName());
            document.addField("collection.name_sort", collection.getName());

        } catch (Exception ex) {
            LOGGER.error("An error occurs trying to index the own collection name of object " + dso.getUniqueIndexID());
        }

    }

    @SuppressWarnings("rawtypes")
    private Collection getCollection(IndexableObject dso) {

        if (dso instanceof IndexableClaimedTask) {
            return ((IndexableClaimedTask) dso).getIndexedObject().getWorkflowItem().getCollection();
        }

        if (dso instanceof IndexablePoolTask) {
            return ((IndexablePoolTask) dso).getIndexedObject().getWorkflowItem().getCollection();
        }

        return null;
    }

}