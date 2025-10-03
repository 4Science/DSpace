/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.marcxml2item.reader;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Implementation of {@link ItemsImportMetadataFieldReader}
 * for reading and combining multiple fields into one.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 **/
public class ItemsImportCombinFieldsReader implements ItemsImportMetadataFieldReader {

    private static final String DEFAULT_SEPARATOR = " ";

    private String readerName;
    private List<String> fieldsToCombine;
    private String separator = DEFAULT_SEPARATOR;
    private XPath xPath = XPathFactory.newInstance().newXPath();

    @Override
    public List<MetadataValueDTO> readValues(Context context, String metadataField, String type, NodeList nodeList) {
        List<MetadataValueDTO> metadataValues = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            List<String> result = new ArrayList<>();
            for (String field : fieldsToCombine) {
                result.addAll(getMultipleValue(node, xPath, field));
            }

            String metadataValue = result.stream()
                                         .filter(StringUtils::isNotBlank)
                                         .collect(Collectors.joining(separator));
            if (StringUtils.isNotBlank(metadataValue)) {
                metadataValues.add(new MetadataValueDTO(metadataField, metadataValue));
            }
        }
        return metadataValues;
    }

    private List<String> getMultipleValue(Node node, XPath xPath, String path) {
        List<String> values = new ArrayList<>();
        try {
            NodeList nodeList = (NodeList) xPath.compile(path).evaluate(node, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node item = nodeList.item(i);
                values.add(item.getTextContent().trim());
            }
        } catch (XPathExpressionException e) {
            throw new RuntimeException("An error occurs evaluating path " + path, e);
        }
        return values;
    }

    @Override
    public String getReaderName() {
        return this.readerName;
    }

    public void setReaderName(String readerName) {
        this.readerName = readerName;
    }

    public List<String> getFieldsToCombine() {
        return fieldsToCombine;
    }

    public void setFieldsToCombine(List<String> fieldsToCombine) {
        this.fieldsToCombine = fieldsToCombine;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

}
