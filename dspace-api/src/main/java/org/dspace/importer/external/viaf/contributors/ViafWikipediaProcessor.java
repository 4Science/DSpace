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
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang.StringUtils;
import org.dspace.importer.external.metadatamapping.contributor.AbstractJsonPathMetadataProcessor;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class ViafWikipediaProcessor extends AbstractJsonPathMetadataProcessor {

    private final static String CONTENT_PATH = "/content";
    private final static List<String> ALLOWED_WIKI = List.of("https://en.wikipedia.org", "https://it.wikipedia.org");

    @Override
    public Collection<String> processMetadata(String json) {
        JsonNode jsonNode = convertStringJsonToJsonNode(json);
        JsonNode queryNode = jsonNode.at(this.query);
        if (queryNode.isArray()) {
            List<String> result = new ArrayList<>();
            Iterator<JsonNode> nodes = queryNode.iterator();
            while (nodes.hasNext()) {
                var value = getWikiValue(nodes.next());
                if (StringUtils.isNotBlank(value)) {
                    result.add(value);
                }
            }
            return result;
        }
        var value = getWikiValue(queryNode);
        return StringUtils.isNotBlank(value) ? List.of(value) : List.of();
    }

    private String getWikiValue(JsonNode queryNode) {
        var value = queryNode.at(CONTENT_PATH).asText();
        if (StringUtils.isNotBlank(value) && isAllowedWikiValue(value)) {
            return value;
        }
        return "";
    }

    private boolean isAllowedWikiValue(String value) {
        for (String wiki : ALLOWED_WIKI) {
            if (value.startsWith(wiki)) {
                return true;
            }
        }
        return false;
    }

}
