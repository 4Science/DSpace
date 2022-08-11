/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.factory;

import java.util.Map;

import org.apache.solr.common.StringUtils;
import org.dspace.content.authority.ItemControlledVocabulary;

/*
 * @author Jurgen Mamani
 */
public class ItemControlledVocabularyFactory {

    private Map<String, ItemControlledVocabulary> entityMapping;

    public ItemControlledVocabulary getInstance(String entityType) {
        return (!StringUtils.isEmpty(entityType))
            && entityMapping.containsKey(entityType) ? entityMapping.get(entityType) : null;
    }

    public void setEntityMapping(Map<String, ItemControlledVocabulary> entityMapping) {
        this.entityMapping = entityMapping;
    }
}
