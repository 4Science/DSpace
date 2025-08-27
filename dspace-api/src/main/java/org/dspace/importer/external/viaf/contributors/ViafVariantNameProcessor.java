/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.viaf.contributors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.contributor.JsonPathMetadataProcessor;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class ViafVariantNameProcessor extends JsonPathMetadataProcessor {

    private final static Logger log = LogManager.getLogger(ViafTitleProcessor.class);

    private static final String MARC21 = "MARC21";
    private static final String UNIMARC = "UNIMARC";

    private static final String UNSUPPORTED_TITLE_TYPE = "Unsupported type of title";
    private static final String DTYPE_PATH = "/dtype";
    private static final String DATAFIELD_PATH = "/ns1:datafield";
    private static final String SOURCE_NAME_PATH = "/ns1:sources/ns1:s";

    private static final String X400_PATH = "/ns1:VIAFCluster/ns1:x400s/ns1:x400";
    private static final String MAIN_HEADING_EL_PATH = "/ns1:VIAFCluster/ns1:mainHeadings/ns1:mainHeadingEl";

    private String separetor;
    private List<String> marc21codes;
    private List<String> unimarcCodes;

    @Override
    public Collection<String> processMetadata(String json) {
        JsonNode jsonNode = convertStringJsonToJsonNode(json);
        Set<String> variantNames = new HashSet<>();
        for (String sourceName : getPreferedSources()) {
            List<String> variantNameFromx400s = getVariantNameFromx400s(jsonNode, sourceName);
            List<String> variantNameFromMainHeading = getVariantNameFromMainHeading(jsonNode, sourceName);
            variantNames.addAll(variantNameFromMainHeading);
            variantNames.addAll(variantNameFromx400s);
        }
        return variantNames;
    }

    private List<String> getVariantNameFromx400s(JsonNode jsonNode, String sourceName) {
        JsonNode x400Node = jsonNode.at(X400_PATH);
        return getVarianNames(sourceName, x400Node);
    }

    private List<String> getVariantNameFromMainHeading(JsonNode jsonNode, String sourceName) {
        JsonNode mainHeadingElNode = jsonNode.at(MAIN_HEADING_EL_PATH);
        return getVarianNames(sourceName, mainHeadingElNode);
    }

    private List<String> getVarianNames(String sourceName, JsonNode jsonNode) {
        List<String> variantNames = new ArrayList<>();
        if (jsonNode.isArray()) {
            Iterator<JsonNode> sourceNodes = jsonNode.iterator();
            while (sourceNodes.hasNext()) {
                JsonNode sourceNode = sourceNodes.next();
                var title = extractValue(sourceNode, sourceName);
                if (StringUtils.isNotBlank(title)) {
                    variantNames.add(title.trim());
                }
            }
        } else {
            var title = extractValue(jsonNode, sourceName);
            if (StringUtils.isNotBlank(title)) {
                variantNames.add(title.trim());
            }
        }
        return variantNames;
    }

    private String extractValue(JsonNode jsonNode, String sourceName) {
        var currentSourceName = jsonNode.at(SOURCE_NAME_PATH).asText();
        if (StringUtils.equalsIgnoreCase(sourceName, currentSourceName)) {
            JsonNode datafieldNode = jsonNode.at(DATAFIELD_PATH);
            return getTitle(datafieldNode);
        }
        return "";
    }

    private String getTitle(JsonNode datafieldNode) {
        String recordType = datafieldNode.at(DTYPE_PATH).asText();
        if (StringUtils.equals(MARC21, recordType)) {
            return getTitleByType(marc21codes, datafieldNode);
        }
        if (StringUtils.equals(UNIMARC, recordType)) {
            return getTitleByType(unimarcCodes, datafieldNode);
        }
        log.error("Current record contains unsupported type: " + recordType);
        return UNSUPPORTED_TITLE_TYPE;
    }

    private String getTitleByType(List<String> typeCodes, JsonNode datafieldNode) {
        DocumentContext context = JsonPath.parse(datafieldNode.toString());
        StringBuilder title = new StringBuilder();
        for (String code : typeCodes) {
            var value = getSubfieldValueByCode(context, code);
            if (StringUtils.isBlank(value)) {
                continue;
            }
            if (title.isEmpty()) {
                title.append(value);
            } else {
                title.append(separetor).append(value);
            }
        }
        return title.isEmpty() ? "" : title.toString();
    }

    private String getSubfieldValueByCode(DocumentContext documentContext, String codeValue) {
        String path = String.format("$.ns1:subfield.[?(@.code == '%s')].content", codeValue);
        List<String> results = documentContext.read(path);
        return results.isEmpty() ? null : results.get(0);
    }

    public void setSeparetor(String separetor) {
        this.separetor = separetor;
    }

    public void setMarc21codes(List<String> marc21codes) {
        this.marc21codes = marc21codes;
    }

    public void setUnimarcCodes(List<String> unimarcCodes) {
        this.unimarcCodes = unimarcCodes;
    }

    private List<String> getPreferedSources() {
        String [] preferedSources = configurationService.getArrayProperty("viaf.prefer.variant.name.sources");
        if (preferedSources == null || preferedSources.length == 0) {
            return List.of();
        }
        return List.of(preferedSources);
    }

}
