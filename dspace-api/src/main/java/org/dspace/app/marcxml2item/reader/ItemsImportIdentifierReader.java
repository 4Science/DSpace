/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.marcxml2item.reader;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.lowerCase;

import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.springframework.util.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Implementation of {@link ItemsImportMetadataFieldReader} for reading identifiers
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 **/
public class ItemsImportIdentifierReader implements ItemsImportMetadataFieldReader {

    private static final String READER_NAME = "identifier";

    private String valueXPath;
    private String qualifierXPath;
    private XPath xPath = XPathFactory.newInstance().newXPath();
    private ImportIdentifierValueProcessor importIdentifierValueProcessor;

    @Override
    public List<MetadataValueDTO> readValues(Context context, String metadataField, String type, NodeList nodeList) {
        List<MetadataValueDTO> metadataValues = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            String value = getSingleValue(node, xPath, valueXPath);
            String qualifier = getQualifier(node);
            String identifierField = isNotBlank(qualifier) ? metadataField + "." + qualifier : metadataField;
            value = importIdentifierValueProcessor.processValue(identifierField, value);
            metadataValues.add(new MetadataValueDTO(identifierField, value));
        }
        return metadataValues;
    }

    private String getQualifier(Node node) {
        String qualifier = getSingleValue(node, xPath, qualifierXPath);
        qualifier = lowerCase(qualifier);
        qualifier = StringUtils.replace(qualifier, " ", "-");

        if ("scopusid".equals(qualifier)) {
            return "scopus";
        }

        if (qualifier != null && qualifier.startsWith("epo")) {
            return "epo";
        }

        return qualifier;
    }

    @Override
    public String getReaderName() {
        return READER_NAME;
    }

    public String getQualifierXPath() {
        return qualifierXPath;
    }

    public void setQualifierXPath(String qualifierXPath) {
        this.qualifierXPath = qualifierXPath;
    }

    public String getValueXPath() {
        return valueXPath;
    }

    public void setValueXPath(String valueXPath) {
        this.valueXPath = valueXPath;
    }

    public ImportIdentifierValueProcessor getImportIdentifierValueProcessor() {
        return importIdentifierValueProcessor;
    }

    public void setImportIdentifierValueProcessor(ImportIdentifierValueProcessor importIdentifierValueProcessor) {
        this.importIdentifierValueProcessor = importIdentifierValueProcessor;
    }

}
