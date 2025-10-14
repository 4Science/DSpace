/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.marcxml2item.validator;

import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.marcxml2item.exception.XmlValidationException;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * A generic XML validator that checks for the presence of specific XML elements or attributes
 * using XPath expressions. This validator ensures that all records in an XML document contain
 * the required elements specified by the XPath query.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class XmlFieldValidation implements XMLValidator {

    private static final Logger log = LogManager.getLogger(XmlFieldValidation.class);

    /** XPath expression to select record elements in the document */
    private static final String RECORD_XPATH = "//record";

    /** XPath query to validate against each record */
    private String query;

    /**
     * Validates the XML content by checking if each element matching the record path
     * contains the elements or attributes specified by the XPath query.
     *
     * @param xmlContent the XML content to validate as a byte array
     * @param handler the handler for logging validation errors and information
     */
    @Override
    public void validate(byte[] xmlContent, DSpaceRunnableHandler handler) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = documentBuilder.parse(new ByteArrayInputStream(xmlContent));

            NodeList nodeList = getNodeList(xPath, document, RECORD_XPATH);
            if (nodeList.getLength() == 0) {
                var errorMessage = "No elements found matching path: " + RECORD_XPATH;
                log.error(errorMessage);
                if (handler != null) {
                    handler.logError(errorMessage);
                }
                throw new XmlValidationException(errorMessage);
            }

            for (int i = 0; i < nodeList.getLength(); i++) {
                NodeList result = getNodeList(xPath, nodeList.item(i), query);
                if (result.getLength() == 0) {
                    var errorMessage = "Validation failed: Required field: " + query + " not found in record";
                    log.error(errorMessage);
                    if (handler != null) {
                        handler.logError(errorMessage);
                    }
                    throw new XmlValidationException(errorMessage);
                }
            }
        } catch (Exception e) {
            var errorMessage = "Error validating XML content: " + e.getMessage();
            log.error(errorMessage);
            if (handler != null) {
                handler.logError(errorMessage);
            }
            throw new XmlValidationException(errorMessage);
        }
    }

    private NodeList getNodeList(XPath xPath, Object item, String expression) {
        try {
            return (NodeList) xPath.compile(expression).evaluate(item, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new XmlValidationException("An error occurs evaluating path: " + expression, e);
        }
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

}
