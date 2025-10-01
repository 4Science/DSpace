/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.marcxml2item.reader;

import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.content.authority.Choices;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Implementation of {@link ItemsImportMetadataFieldReader} to read nested metadata fields from XML structure
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 **/
public class ItemsImportNestedMetadataFieldReader implements ItemsImportMetadataFieldReader {

    private XPath xPath = XPathFactory.newInstance().newXPath();

    private String readerName;
    private boolean skipDateConversion = false;
    private Map<String, String> authorityPaths = new HashMap<String, String>();
    private Map<String, String> authorityPrefixes = new HashMap<String, String>();
    private Map<String, String> metadataFieldsPaths = new HashMap<String, String>();

    @Override
    public List<MetadataValueDTO> readValues(Context context, String metadataField, String type, NodeList nodeList) {
        List<MetadataValueDTO> metadataValues = new ArrayList<MetadataValueDTO>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i).cloneNode(true);
            for (String nestedMetadataField : metadataFieldsPaths.keySet()) {
                metadataValues.add(readNestedValue(node, nestedMetadataField));
            }
        }
        return metadataValues;
    }

    private MetadataValueDTO readNestedValue(Node node, String nestedMetadataField) {
        String path = metadataFieldsPaths.get(nestedMetadataField);
        if (StringUtils.isBlank(path)) {
            return new MetadataValueDTO(nestedMetadataField, PLACEHOLDER_PARENT_METADATA_VALUE);
        }

        String singleValue = path.startsWith("static:") ? StringUtils.removeStart(path, "static:")
                                                        : getSingleValue(node, xPath, path);

        String value = skipDateConversion ? singleValue : convertIfDate(singleValue);

        if (StringUtils.isBlank(value)) {
            return new MetadataValueDTO(nestedMetadataField, PLACEHOLDER_PARENT_METADATA_VALUE);
        }

        return readNestedAuthority(node, nestedMetadataField)
                  .map(authority -> new MetadataValueDTO(nestedMetadataField, value, authority, Choices.CF_AMBIGUOUS))
                  .orElseGet(() -> new MetadataValueDTO(nestedMetadataField, value));
    }

    private Optional<String> readNestedAuthority(Node node, String nestedMetadataField) {
        if (!authorityPaths.containsKey(nestedMetadataField)) {
            return Optional.empty();
        }

        String authority = getSingleValue(node, xPath, authorityPaths.get(nestedMetadataField));
        if (StringUtils.isBlank(authority)) {
            return Optional.empty();
        }

        String prefix = authorityPrefixes.getOrDefault(nestedMetadataField, AuthorityValueService.GENERATE);
        if (!StringUtils.endsWith(prefix, AuthorityValueService.SPLIT)) {
            prefix = prefix + AuthorityValueService.SPLIT;
        }
        return Optional.of(prefix + authority);
    }

    @Override
    public String getReaderName() {
        return readerName;
    }

    public void setReaderName(String readerName) {
        this.readerName = readerName;
    }

    public Map<String, String> getMetadataFieldsPaths() {
        return metadataFieldsPaths;
    }

    public void setMetadataFieldsPaths(Map<String, String> metadataFieldsPaths) {
        this.metadataFieldsPaths = metadataFieldsPaths;
    }

    public Map<String, String> getAuthorityPaths() {
        return authorityPaths;
    }

    public void setAuthorityPaths(Map<String, String> authorityPaths) {
        this.authorityPaths = authorityPaths;
    }

    public Map<String, String> getAuthorityPrefixes() {
        return authorityPrefixes;
    }

    public void setAuthorityPrefixes(Map<String, String> authorityPrefixes) {
        this.authorityPrefixes = authorityPrefixes;
    }

    public void setSkipDateConversion(boolean skipDateConversion) {
        this.skipDateConversion = skipDateConversion;
    }

}
