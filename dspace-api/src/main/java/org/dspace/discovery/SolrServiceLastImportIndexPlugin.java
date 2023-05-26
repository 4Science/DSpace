/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.dspace.content.Item.ANY;

import java.sql.SQLException;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link SolrServiceIndexPlugin} to index the
 * cris.lastimport.{provider} metadata fields for sorting.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class SolrServiceLastImportIndexPlugin implements SolrServiceIndexPlugin {

    @Autowired
    private ItemService itemService;

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Override
    @SuppressWarnings("rawtypes")
    public void additionalIndex(Context context, IndexableObject dso, SolrInputDocument document) {
        if (!(dso instanceof IndexableItem)) {
            return;
        }

        Item item = ((IndexableItem) dso).getIndexedObject();
        for (MetadataFieldName lastImportMetadataField : getLastImportMetadataFields(context)) {
            String lastImportValue = getMetadataFirstValue(item, lastImportMetadataField);
            if (lastImportValue != null) {
                addLastImportSortIndex(document, lastImportMetadataField, lastImportValue);
            }
        }

    }

    private List<MetadataFieldName> getLastImportMetadataFields(Context context) {
        try {
            return metadataFieldService.findMetadataFieldNamesBySchemaAndElement(context, "cris", "lastimport");
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private void addLastImportSortIndex(SolrInputDocument document, MetadataFieldName lastImportField, String value) {
        document.addField(lastImportField.toString() + "_dt", value);
    }

    private String getMetadataFirstValue(Item item, MetadataFieldName metadataField) {
        return itemService.getMetadataFirstValue(item, metadataField, ANY);

    }

}
