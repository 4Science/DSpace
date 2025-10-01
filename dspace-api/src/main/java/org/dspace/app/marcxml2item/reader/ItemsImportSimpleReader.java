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

import org.apache.commons.lang.StringUtils;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Simple implementation of {@link ItemsImportMetadataFieldReader}
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 **/
public class ItemsImportSimpleReader implements ItemsImportMetadataFieldReader {

    public static final String DEFAULT_METADATAFIELDS_READER = "default";

    @Override
    public List<MetadataValueDTO> readValues(Context context, String metadataField, String type, NodeList nodeList) {
        List<MetadataValueDTO> metadataValues = new ArrayList<MetadataValueDTO>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            String nodeValue = node.getTextContent();
            if (StringUtils.isNotBlank(nodeValue)) {
                String value = convertIfDate(nodeValue);
                metadataValues.add(new MetadataValueDTO(metadataField, value));
            }
        }
        return metadataValues;
    }

    @Override
    public String getReaderName() {
        return DEFAULT_METADATAFIELDS_READER;
    }

}
