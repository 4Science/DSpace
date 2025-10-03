/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.marcxml2item.model;

import static org.dspace.app.marcxml2item.reader.ItemsImportSimpleReader.DEFAULT_METADATA_FIELDS_READER;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 **/
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "mapping")
public class ItemsImportMapping {

    @XmlElement(name = "item-xpath")
    private String itemXPath;

    @XmlElement(name = "submitter-xpath")
    private String submitterXPath;

    @XmlElement(name = "metadata-fields")
    private MetadataFields metadataFields;

    public MetadataFields getMetadataFields() {
        return metadataFields == null ? new MetadataFields() : metadataFields;
    }

    public void setMetadataFields(MetadataFields metadataFields) {
        this.metadataFields = metadataFields;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MetadataFields {

        @XmlElement(name = "metadata-field")
        private List<MetadataField> metadataFields;

        public List<MetadataField> getMetadataFields() {
            return metadataFields == null ? new ArrayList<>() : metadataFields;
        }

        public void setMetadataFields(List<MetadataField> metadataFields) {
            this.metadataFields = metadataFields;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MetadataField {

        private String field;

        private String reader;

        private String xpath;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getXPath() {
            return xpath;
        }

        public void setXPath(String xpath) {
            this.xpath = xpath;
        }

        public String getReader() {
            return StringUtils.isNotBlank(reader) ? reader : DEFAULT_METADATA_FIELDS_READER;
        }

        public void setReader(String reader) {
            this.reader = reader;
        }

    }

}
