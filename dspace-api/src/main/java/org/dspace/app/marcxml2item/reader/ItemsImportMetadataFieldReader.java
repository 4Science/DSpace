/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.marcxml2item.reader;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.dspace.util.MultiFormatDateParser;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Interface for reading metadata values from XML nodes
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 **/
public interface ItemsImportMetadataFieldReader {

    DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    DateFormat YYYY_DATE_FORMAT = new SimpleDateFormat("yyyy");
    DateFormat YYYY_MM_DATE_FORMAT = new SimpleDateFormat("yyyy-MM");

    String getReaderName();

    List<MetadataValueDTO> readValues(Context context, String metadataField, String type, NodeList nodeList);

    default String getSingleValue(Node node, XPath xPath, String path) {
        try {
            return (String) xPath.compile(path).evaluate(node, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("An error occurs evaluating path " + path, e);
        }
    }

    default String convertIfDate(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        Date date = MultiFormatDateParser.parse(value);
        if (date != null) {
            // Operation to tackle cases having value expressed in yyyy, yyyyMM, yyyy-MM formats
            if (value.length() == 4) {
                return YYYY_DATE_FORMAT.format(date);
            }
            if (value.length() == 6 || value.length() == 7) {
                return YYYY_MM_DATE_FORMAT.format(date);
            }
            return DATE_FORMAT.format(date);
        }
        return value;
    }

}
