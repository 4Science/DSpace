/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.viaf.contributors;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.contributor.JsonPathMetadataProcessor;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class ViafSimplePrefixProcessor implements JsonPathMetadataProcessor  {

    private final static Logger log = LogManager.getLogger(ViafSimplePrefixProcessor.class);

    private String query;
    private String prefix;

    @Override
    public Collection<String> processMetadata(String json) {
        JsonNode jsonNode = convertStringJsonToJsonNode(json);
        String value = jsonNode.at(this.query).asText();
        if (StringUtils.isBlank(value)) {
            return List.of();
        }
        if (StringUtils.isNotBlank(this.prefix)) {
            return List.of(this.prefix + value);
        }
        return List.of(value);
    }

    private JsonNode convertStringJsonToJsonNode(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode body = null;
        try {
            body = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Unable to process json response.", e);
        }
        return body;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setQuery(String query) {
        this.query = query;
    }

}
