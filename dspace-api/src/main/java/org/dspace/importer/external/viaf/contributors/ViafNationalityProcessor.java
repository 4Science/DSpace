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
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.contributor.JsonPathMetadataProcessor;

/**
 * Processor for extracting nationality information from VIAF JSON responses.
 * Prioritizes sources based on configuration and falls back to first available source.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class ViafNationalityProcessor extends JsonPathMetadataProcessor  {

    private final static Logger log = LogManager.getLogger(ViafNationalityProcessor.class);

    private static final String TEXT_PATH = "/ns1:text";

    @Override
    public Collection<String> processMetadata(String json) {
        try {
            JsonNode rootNode = convertStringJsonToJsonNode(json);
            if (rootNode == null) {
                return List.of();
            }
            JsonNode dataNode = rootNode.at(query);
            if (dataNode == null) {
                return List.of();
            }
            Set<String> nationalityAvaibleSources = getNationalityAvaibleSources(dataNode);
            String preferedSource = getNameOfPreferedSource(nationalityAvaibleSources);
            return StringUtils.isBlank(preferedSource) ? getFirstAvaibleNationality(dataNode)
                                                       : getNationalityBySource(dataNode, preferedSource);
        } catch (Exception e) {
            log.warn("Error processing VIAF nationality data", e);
            return List.of();
        }
    }

    private Collection<String> getFirstAvaibleNationality(JsonNode jsonNode) {
        var nationality = jsonNode.at(TEXT_PATH).asText();
        return StringUtils.isNotBlank(nationality) ? List.of(nationality) : List.of();
    }

    private Collection<String> getNationalityBySource(JsonNode dataNode, String preferedSource) {
        if (dataNode.isArray()) {
            Iterator<JsonNode> sourceNodes = dataNode.iterator();
            while (sourceNodes.hasNext()) {
                JsonNode sourceNode = sourceNodes.next();
                Set<String> sourceNames = getSourceNames(sourceNode);
                if (sourceNames.contains(preferedSource)) {
                    return List.of(sourceNode.at(TEXT_PATH).asText());
                }
            }
        }
        return List.of(dataNode.at(TEXT_PATH).asText());
    }

    private Set<String> getSourceNames(JsonNode sourceNode) {
        JsonNode sNode = sourceNode.at("/ns1:sources/ns1:s");
        if (sNode.isArray()) {
            Set<String> names = new HashSet<>();
            Iterator<JsonNode> sourceNames = sNode.iterator();
            while(sourceNames.hasNext()) {
                var sourceName = sourceNames.next().asText();
                if (StringUtils.isNotBlank(sourceName)) {
                    names.add(sourceName);
                }
            }
            return names;
        }
        return StringUtils.isNotBlank(sNode.asText()) ? Set.of(sNode.asText()) : Set.of();
    }

    private String getNameOfPreferedSource(Set<String> nationalityAvaibleSources) {
        for (String preferedSource : getPreferedSources()) {
            if (nationalityAvaibleSources.contains(preferedSource)) {
                return preferedSource;
            }
        }
        return "";
    }

    private Set<String> getNationalityAvaibleSources(JsonNode dataNode) {
        if (dataNode.isArray()) {
            Iterator<JsonNode> sourceNodes = dataNode.iterator();
            Set<String> sources = new HashSet<>();
            while (sourceNodes.hasNext()) {
                Set<String> sourceNames = getSourceNames(sourceNodes.next());
                sources.addAll(sourceNames);
            }
            return sources;
        }
        return getSourceNames(dataNode);
    }

    private List<String> getPreferedSources() {
        String [] preferedSources = configurationService.getArrayProperty("viaf.prefer.sources");
        if (preferedSources == null || preferedSources.length == 0) {
            return List.of();
        }
        return List.of(preferedSources);
    }

}
