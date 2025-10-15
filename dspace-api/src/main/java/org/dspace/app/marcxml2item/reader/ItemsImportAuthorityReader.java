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
 * Reader to extract metadata values with authority from an XML structure.
 * The reader uses XPath expressions to locate the value and authority within the XML nodes.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ItemsImportAuthorityReader implements ItemsImportMetadataFieldReader {

    private String readerName;
    private String valueXPath;
    private String authorityXPath;
    private String authorityPrefix;
    private XPath xPath = XPathFactory.newInstance().newXPath();

    @Override
    public List<MetadataValueDTO> readValues(Context context, String metadataField, String type, NodeList nodeList) {
        List<MetadataValueDTO> metadataValues = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            NodeList subNodes = getSubnodes(node, xPath, valueXPath);
            for (int j = 0; j < subNodes.getLength(); j++) {
                Node subNode = subNodes.item(j);
                String value = getSingleValue(subNode, xPath, ".");
                if (StringUtils.isNotBlank(value)) {
                    String authority = getSingleValue(subNode, xPath, ".");
                    metadataValues.add(buildMetadata(metadataField, value, authority));
                }
            }
        }
        return metadataValues;
    }

    private NodeList getSubnodes(Node node, XPath xPath, String path) {
        try {
            return (NodeList) xPath.compile(path).evaluate(node, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("An error occurs evaluating path " + path, e);
        }
    }

    private MetadataValueDTO buildMetadata(String metadataField, String value, String authority) {
        MetadataValueDTO metadataValue = new MetadataValueDTO(metadataField, value);
        if (StringUtils.isNotBlank(authority)) {
            metadataValue.setAuthority(authorityPrefix + "::" + authority);
            metadataValue.setConfidence(600);
        }
        return metadataValue;
    }

    @Override
    public String getReaderName() {
        return readerName;
    }

    public void setReaderName(String readerName) {
        this.readerName = readerName;
    }

    public String getAuthorityPrefix() {
        return authorityPrefix;
    }

    public void setAuthorityPrefix(String authorityPrefix) {
        this.authorityPrefix = authorityPrefix;
    }

    public String getValueXPath() {
        return valueXPath;
    }

    public String getAuthorityXPath() {
        return authorityXPath;
    }

    public void setAuthorityXPath(String authorityXPath) {
        this.authorityXPath = authorityXPath;
    }

    public void setValueXPath(String valueXPath) {
        this.valueXPath = valueXPath;
    }

}
