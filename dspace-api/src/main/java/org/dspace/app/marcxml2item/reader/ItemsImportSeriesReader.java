/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.marcxml2item.reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A metadata reader to read series information from an XML document.
 * The series information is composed of a title and a number.
 * The title and number are concatenated with a "; " separator.
 * If the item type is configured as "is part of series", the metadata is
 * written to a different metadata field.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class ItemsImportSeriesReader implements ItemsImportMetadataFieldReader {

    private static final String READER_NAME = "serie";

    private String titleXPath;
    private String numberXPath;
    private String isPartOfSeriesMetadataField;
    private XPath xPath = XPathFactory.newInstance().newXPath();

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public List<MetadataValueDTO> readValues(Context context, String metadataField, String type, NodeList nodeList) {
        List<MetadataValueDTO> metadataValues = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            String title = getSingleValue(node, xPath, titleXPath);
            String number = getSingleValue(node, xPath, numberXPath);

            String value = Stream.of(title, number)
                                 .filter(StringUtils::isNotBlank)
                                 .collect(Collectors.joining("; "));

            if (StringUtils.isNotBlank(value)) {
                String field = getIsPartOfSeriesTypes().contains(type) ? isPartOfSeriesMetadataField : metadataField;
                metadataValues.add(new MetadataValueDTO(field, value));
            }
        }
        return metadataValues;
    }

    private List<String> getIsPartOfSeriesTypes() {
        return Arrays.asList(configurationService.getArrayProperty("items-import.is-part-of.series.types"));
    }

    @Override
    public String getReaderName() {
        return READER_NAME;
    }

    public String getTitleXPath() {
        return titleXPath;
    }

    public void setTitleXPath(String titleXPath) {
        this.titleXPath = titleXPath;
    }

    public String getNumberXPath() {
        return numberXPath;
    }

    public void setNumberXPath(String numberXPath) {
        this.numberXPath = numberXPath;
    }

    public String getIsPartOfSeriesMetadataField() {
        return isPartOfSeriesMetadataField;
    }

    public void setIsPartOfSeriesMetadataField(String isPartOfSeriesMetadataField) {
        this.isPartOfSeriesMetadataField = isPartOfSeriesMetadataField;
    }

}
