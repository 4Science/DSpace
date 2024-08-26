/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.List;

import org.dspace.content.authority.mapper.ItemControlledVocabularyMapper;

/*
 * @author Jurgen Mamani
 */
public class  ItemControlledVocabulary {

    private String entityType;

    private String parentQuery;

    private String childrenQuery;

    private String parentMetadata;

    private String authorityMetadata;

    private String selectableMetadata;

    private List<String> labelMetadata;

    private String sortFieldAndOrder;

    private ItemControlledVocabularyMapper extraValuesMapper;

    // Getters and setters

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getParentQuery() {
        return parentQuery;
    }

    public String getChildrenQuery() {
        return childrenQuery;
    }

    public String getParentMetadata() {
        return parentMetadata;
    }

    public void setParentQuery(String parentQuery) {
        this.parentQuery = parentQuery;
    }

    public void setChildrenQuery(String childrenQuery) {
        this.childrenQuery = childrenQuery;
    }

    public void setParentMetadata(String parentMetadata) {
        this.parentMetadata = parentMetadata;
    }

    public String getSelectableMetadata() {
        return selectableMetadata;
    }

    public void setSelectableMetadata(String selectableMetadata) {
        this.selectableMetadata = selectableMetadata;
    }

    public List<String> getLabelMetadata() {
        return labelMetadata;
    }

    public void setLabelMetadata(List<String> labelMetadata) {
        this.labelMetadata = labelMetadata;
    }

    public String getAuthorityMetadata() {
        return authorityMetadata;
    }

    public void setAuthorityMetadata(String authorityMetadata) {
        this.authorityMetadata = authorityMetadata;
    }

    public ItemControlledVocabularyMapper getExtraValuesMapper() {
        return extraValuesMapper;
    }

    public void setExtraValuesMapper(ItemControlledVocabularyMapper extraValuesMapper) {
        this.extraValuesMapper = extraValuesMapper;
    }

    public String getSortFieldAndOrder() {
        return sortFieldAndOrder;
    }

    public void setSortFieldAndOrder(String sortFieldAndOrder) {
        this.sortFieldAndOrder = sortFieldAndOrder;
    }
}
