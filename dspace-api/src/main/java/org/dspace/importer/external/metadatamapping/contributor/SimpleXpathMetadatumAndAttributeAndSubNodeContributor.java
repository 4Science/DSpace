/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * Metadata contributor that takes multiple value of the some nome.
 * Can fileter also nedes by attribute element value.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class SimpleXpathMetadatumAndAttributeAndSubNodeContributor extends SimpleXpathMetadatumAndAttributeContributor {

    private String attributeValue;
    private String queryToSubNode;

    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element t) {
        List<MetadatumDTO> values = new LinkedList<>();
        List<Namespace> namespaces = new ArrayList<Namespace>();
        for (String ns : prefixToNamespaceMapping.keySet()) {
            namespaces.add(Namespace.getNamespace(prefixToNamespaceMapping.get(ns), ns));
        }

        List<Object> nodes = getNodes(t, query, namespaces);
        List<Object> subNodes = getSubNodes(namespaces, nodes);
        for (Object el : subNodes) {
            if (el instanceof Element) {
                values.add(metadataFieldMapping.toDCValue(this.field, extractValue(el)));
            }
        }
        return values;
    }

    private List<Object> getSubNodes(List<Namespace> namespaces, List<Object> nodes) {
        List<Object> allNodes = new ArrayList<Object>();
        for (Object el : nodes) {
            if (el instanceof Element) {
                List<Element> elements = ((Element) el).getChildren();
                for (Element element : elements) {
                    String attributeValue = element.getAttributeValue(this.attribute);
                    if (StringUtils.equals(attributeValue, this.attributeValue)) {
                        List<Object> subNodes = getNodes(element, queryToSubNode, namespaces);
                        allNodes.addAll(subNodes);
                    }
                }
            }
        }
        return allNodes;
    }

    private List<Object> getNodes(Element t, String query, List<Namespace> namespaces) {
        XPathExpression<Object> xpath = XPathFactory.instance().compile(query, Filters.fpassthrough(),null, namespaces);
        return xpath.evaluate(t);
    }

    private String extractValue(Object el) {
        String value = ((Element) el).getText();
        return StringUtils.isNotBlank(value) ? value : ((Element) el).getValue().trim();
    }

    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    public void setQueryToSubNode(String queryToSubNode) {
        this.queryToSubNode = queryToSubNode;
    }

}
