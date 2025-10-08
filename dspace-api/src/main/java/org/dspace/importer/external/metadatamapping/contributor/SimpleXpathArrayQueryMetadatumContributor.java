/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * Contributor that extracts metadata using multiple XPath queries from an XML element.
 * Each query result is mapped to a MetadatumDTO.
 * Extends SimpleXpathMetadatumContributor.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class SimpleXpathArrayQueryMetadatumContributor extends SimpleXpathMetadatumContributor {

    private static final Logger log = LogManager.getLogger(SimpleXpathArrayQueryMetadatumContributor.class);

    private List<String> queries;

    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element t) {
        List<MetadatumDTO> values = new LinkedList<>();
        for (String query : queries) {
            XPathExpression<Object> xpath = XPathFactory.instance()
                                                        .compile(query, Filters.fpassthrough(), null,getNamespaces());
            List<Object> nodes = xpath.evaluate(t);
            values.addAll(getValues(nodes));
        }
        return values;
    }

    private List<MetadatumDTO> getValues(List<Object> nodes) {
        List<MetadatumDTO> values = new LinkedList<>();
        for (Object el : nodes) {
            if (el instanceof Element) {
                values.add(metadataFieldMapping.toDCValue(field, extractValue(el)));
            } else if (el instanceof Attribute) {
                values.add(metadataFieldMapping.toDCValue(field, ((Attribute) el).getValue()));
            } else if (el instanceof String) {
                values.add(metadataFieldMapping.toDCValue(field, (String) el));
            } else if (el instanceof Text) {
                values.add(metadataFieldMapping.toDCValue(field, ((Text) el).getText()));
            } else {
                log.error("Encountered unsupported XML node of type: {}. Skipped that node.", el.getClass());
            }
        }
        return values;
    }

    public List<String> getQueries() {
        return queries;
    }

    public void setQueries(List<String> queries) {
        this.queries = queries;
    }

}
