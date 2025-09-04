/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract base class for JSON path metadata processors.
 * Provides common functionality for processing JSON metadata using JSONPath queries.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public abstract class AbstractJsonPathMetadataProcessor implements JsonPathMetadataProcessor {

    private final static Logger log = LogManager.getLogger(AbstractJsonPathMetadataProcessor.class);

    protected String query;

    @Autowired
    protected ConfigurationService configurationService;

    public JsonNode convertStringJsonToJsonNode(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode body = null;
        try {
            body = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Unable to process json response. JSON: " + json, e);
        }
        return body;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

}
