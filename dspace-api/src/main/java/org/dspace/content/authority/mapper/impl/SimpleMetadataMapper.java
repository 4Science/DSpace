/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.mapper.impl;

import java.util.HashMap;
import java.util.Map;

import org.dspace.content.Item;
import org.dspace.content.authority.mapper.ItemControlledVocabularyMapper;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;


/*
 * @author Jurgen Mamani
 */
public class SimpleMetadataMapper implements ItemControlledVocabularyMapper {

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private Map<String, String> extraValues;

    @Override
    public Map<String, String> buildExtraValues(Item item) {
        Map<String, String> extras = new HashMap<>();
        extraValues.forEach(
            (k, v) -> extras.put(k, getValueFromMetadata(item, v)));
        return extras;
    }

    public void setExtraValues(Map<String, String> extraValues) {
        this.extraValues = extraValues;
    }

    private String getValueFromMetadata(Item item, String metadata) {
        String mtd = itemService.getMetadata(item, metadata);
        return mtd == null ? "" : mtd;
    }

}
