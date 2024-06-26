/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ObjectNameIndexPlugin} which indexes the objectname
 * to retrieve alternatives
 *
 * @author Stefano Maffei at 4science.com
 *
 */
public class ObjectNameIndexPlugin implements SolrServiceIndexPlugin {

    @Autowired
    private ItemService itemService;

    public static String OBJECT_NAME_INDEX = "objectname";

    public static List<String> fields;

    @SuppressWarnings("rawtypes")
    @Override
    public void additionalIndex(Context context, IndexableObject dso, SolrInputDocument document) {
        if (!(dso.getIndexedObject() instanceof Item)) {
            return;
        }

        Item item = (Item) dso.getIndexedObject();
        List<String> metadataValues = getMetadataValues(item, fields);

        metadataValues.forEach(textValue -> document.addField(OBJECT_NAME_INDEX, textValue));
    }

    private List<String> getMetadataValues(Item item, List<String> metadataFields) {
        return metadataFields.stream()
            .flatMap(metadataField -> getMetadataValues(item, metadataField).stream())
            .collect(Collectors.toList());
    }

    private List<String> getMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField).stream()
            .map(MetadataValue::getValue)
            .collect(Collectors.toList());
    }

    public static List<String> getFields() {
        return fields;
    }

    public static void setFields(List<String> fields) {
        ObjectNameIndexPlugin.fields = fields;
    }

}
