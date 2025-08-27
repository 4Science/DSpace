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
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.contributor.JsonPathMetadataProcessor;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class ViafSubjectIcshProcessor extends JsonPathMetadataProcessor {

    private final static Logger log = LogManager.getLogger(ViafNationalityProcessor.class);

    private static final String TEXT_PATH = "/ns1:text";
    private static final String LC_SOURCE = "LC";

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
            return getValuesBySource(dataNode, LC_SOURCE);
        } catch (Exception e) {
            log.warn("Error processing VIAF data", e);
            return List.of();
        }
    }

    private Collection<String> getValuesBySource(JsonNode dataNode, String source2get) {
        if (dataNode.isArray()) {
            Iterator<JsonNode> sourceNodes = dataNode.iterator();
            List<String> subjects = new ArrayList<>();
            while (sourceNodes.hasNext()) {
                JsonNode sourceNode = sourceNodes.next();
                Set<String> sourceNames = getSourceNames(sourceNode);
                if (sourceNames.contains(source2get)) {
                    subjects.add(sourceNode.at(TEXT_PATH).asText());
                }
            }
            return subjects;
        }
        Set<String> sourceNames = getSourceNames(dataNode);
        if (sourceNames.contains(source2get)) {
            return List.of(dataNode.at(TEXT_PATH).asText());
        }
        return List.of();
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

}
