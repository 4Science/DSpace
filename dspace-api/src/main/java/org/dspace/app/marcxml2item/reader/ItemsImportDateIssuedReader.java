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

import org.apache.commons.lang.StringUtils;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Implementation of {@link ItemsImportMetadataFieldReader} that reads the most precise date
 * (i.e. the date with the most hyphens, e.g. 2020-01-01 is more precise than 2020-01 or 2020)
 * from the provided NodeList and returns it as a MetadataValueDTO.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class ItemsImportDateIssuedReader implements ItemsImportMetadataFieldReader {

    private static final String READER_NAME = "dateIssued";

    @Override
    public List<MetadataValueDTO> readValues(Context context, String metadataField, String type, NodeList nodeList) {
        return getAllValues(nodeList).stream()
                                     .sorted(this::compareByPrecision)
                                     .limit(1L)
                                     .map(value -> new MetadataValueDTO(metadataField, convertIfDate(value)))
                                     .collect(Collectors.toList());
    }

    private int compareByPrecision(String firstDate, String secondDate) {
        Integer firstPrecisionCount = Integer.valueOf(StringUtils.countMatches(firstDate, "-"));
        Integer secondPrecisionCount = Integer.valueOf(StringUtils.countMatches(secondDate, "-"));
        return secondPrecisionCount.compareTo(firstPrecisionCount);
    }

    private List<String> getAllValues(NodeList nodeList) {
        List<String> metadataValues = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            String value = node.getTextContent();
            if (StringUtils.isNotBlank(value)) {
                metadataValues.add(value);
            }
        }
        return metadataValues;
    }

    @Override
    public String getReaderName() {
        return READER_NAME;
    }

}
