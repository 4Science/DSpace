/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.viaf.contributors;

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
public class ViafTitleProcessor extends JsonPathMetadataProcessor {

    private final static Logger log = LogManager.getLogger(ViafTitleProcessor.class);

    private static final String MARC21 = "MARC21";
    private static final String UNIMARC = "UNIMARC";
    private static final List<String> MARC21_CODES = List.of("a", "b");
    private static final List<String> UNIMARC_CODES = List.of("a", "b", "d");

    private static final String UNSUPPORTED_TITLE_TYPE = "Unsupported type of title";
    private static final String DTYPE_PATH = "/dtype";
    private static final String DATAFIELD_PATH = "/ns1:datafield";
    private static final String MAIN_HEADING_EL_PATH = "/ns1:VIAFCluster/ns1:mainHeadings/ns1:mainHeadingEl";

    private String separetor;

    @Override
    public Collection<String> processMetadata(String json) {
        JsonNode jsonNode = convertStringJsonToJsonNode(json);
        Set<String> titleAvaibleSources = getTitleAvaibleSources(jsonNode);
        String preferedSource = getNameOfPreferedSource(titleAvaibleSources);
        return StringUtils.isBlank(preferedSource) ? getFirstAvaibleTitle(jsonNode)
                                                   : getTitleBySource(jsonNode, preferedSource);
    }

    private Collection<String> getTitleBySource(JsonNode jsonNode, String preferedSource) {
        JsonNode mainHeadingElNode = jsonNode.at(MAIN_HEADING_EL_PATH);
        if (!mainHeadingElNode.isArray()) {
            var currentSourceName = mainHeadingElNode.at("/ns1:sources/ns1:s").asText();
            if (StringUtils.equalsIgnoreCase(preferedSource, currentSourceName)) {
                JsonNode datafieldNode = mainHeadingElNode.at("/ns1:datafield");
                return getTitle(datafieldNode);
            }
        }

        Iterator<JsonNode> mainHeadingEl = mainHeadingElNode.iterator();
        while (mainHeadingEl.hasNext()) {
            JsonNode node = mainHeadingEl.next();
            var currentSourceName = node.at("/ns1:sources/ns1:s").asText();
            if (StringUtils.equalsIgnoreCase(preferedSource, currentSourceName)) {
                JsonNode datafieldNode = node.at("/ns1:datafield");
                return getTitle(datafieldNode);
            }
        }
        return List.of();
    }

    private Collection<String> getFirstAvaibleTitle(JsonNode jsonNode) {
        JsonNode datafieldNode = getDatafieldNode(jsonNode);
        return getTitle(datafieldNode);
    }

    private Collection<String> getTitle(JsonNode datafieldNode) {
        String recordType = datafieldNode.at(DTYPE_PATH).asText();
        if (StringUtils.equals(MARC21, recordType)) {
            return getTitleByType(MARC21_CODES, datafieldNode);
        }
        if (StringUtils.equals(UNIMARC, recordType)) {
            return getTitleByType(UNIMARC_CODES, datafieldNode);
        }
        log.error("Current record contains unsupported type: " + recordType);
        return List.of(UNSUPPORTED_TITLE_TYPE);
    }

    private JsonNode getDatafieldNode(JsonNode jsonNode) {
        JsonNode mainHeadingEl = jsonNode.at(MAIN_HEADING_EL_PATH);
        if (mainHeadingEl.isArray()) {
            Iterator<JsonNode> sourceNodes = mainHeadingEl.iterator();
            return sourceNodes.hasNext() ? sourceNodes.next().at(DATAFIELD_PATH) : null;
        } else {
            return mainHeadingEl.at(DATAFIELD_PATH);
        }
    }

    private Collection<String> getTitleByType(List<String> typeCodes, JsonNode datafieldNode) {
        DocumentContext context = JsonPath.parse(datafieldNode.toString());
        StringBuilder title = new StringBuilder();
        for (String code : typeCodes) {
            var value = getSubfieldValueByCode(context, code);
            if (value == null) {
                continue;
            }
            if (title.isEmpty()) {
                title.append(value);
            } else {
                title.append(separetor).append(value);
            }
        }
        return List.of(title.toString());
    }

    private String getSubfieldValueByCode(DocumentContext documentContext, String codeValue) {
        String path = String.format("$.ns1:subfield.[?(@.code == '%s')].content", codeValue);
        List<String> results = documentContext.read(path);
        return results.isEmpty() ? null : results.get(0);
    }

    private String getNameOfPreferedSource(Set<String> titleAvaibleSources) {
        for (String preferedSource : getPreferedSources()) {
            if (titleAvaibleSources.contains(preferedSource)) {
                return preferedSource;
            }
        }
        return "";
    }

    private List<String> getPreferedSources() {
        String [] preferedSources = configurationService.getArrayProperty("viaf.prefer.sources");
        if (preferedSources == null || preferedSources.length == 0) {
            return List.of();
        }
        return List.of(preferedSources);
    }

    private Set<String> getTitleAvaibleSources(JsonNode json) {
        Set<String> sources = new HashSet<>();
        JsonNode sourceNode = json.at("/ns1:VIAFCluster/ns1:sources/ns1:source");
        if (!sourceNode.isArray()) {
            var sourceName = getSourceName(sourceNode);
            return StringUtils.isNotBlank(sourceName) ? Set.of(sourceName) : Set.of();
        }

        Iterator<JsonNode> sourceNodes = sourceNode.iterator();
        while (sourceNodes.hasNext()) {
            var sourceName = getSourceName(sourceNodes.next());
            if (StringUtils.isNotBlank(sourceName)) {
                sources.add(sourceName);
            }
        }
        return sources;
    }

    private String getSourceName(JsonNode sourceNode) {
        String contentValue = sourceNode.at("/content").asText();
        return StringUtils.isNotBlank(contentValue) ? contentValue.substring(0, contentValue.indexOf("|")) : "";
    }

    public void setSeparetor(String separetor) {
        this.separetor = separetor;
    }

}
