/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

/**
 * @author Jurgen Mamani
 */
public class CrisLayoutHierarchicalConfigurationRest implements CrisLayoutBoxConfigurationRest {

    public static final String NAME = "boxhierarchyconfiguration";

    private String type = NAME;

    private String vocabulary;

    private String metadata;

    private Integer maxColumns;

    public String getType() {
        return this.type;
    }

    public String getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(String vocabulary) {
        this.vocabulary = vocabulary;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public void setMaxColumns(Integer maxColumns) {
        this.maxColumns = maxColumns;
    }

    public Integer getMaxColumns() {
        return this.maxColumns;
    }
}
