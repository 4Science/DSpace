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
 * Processor for extracting and processing date information from VIAF JSON responses.
 * This processor can extract full dates or only years from date strings in various formats.
 * When extracting years, it validates that the year is exactly 4 characters long.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class ViafDateProcessor implements JsonPathMetadataProcessor  {

    private final static Logger log = LogManager.getLogger(ViafDateProcessor.class);

    private String query;
    private boolean takeOnlyYear;

    @Override
    public Collection<String> processMetadata(String json) {
        JsonNode jsonNode = convertStringJsonToJsonNode(json);
        String date = jsonNode.at(this.query).asText();
        if (StringUtils.isBlank(date) || StringUtils.equals(date, "0")) {
            return List.of();
        }
        if (this.takeOnlyYear) {
            return List.of(extractYear(date));
        }
        return List.of(date);
    }

    private String extractYear(String date) {
        String year;
        if (date.contains("-")) {
            year = date.substring(0, date.indexOf("-"));
        } else if (date.contains("/")) {
            year = date.substring(0, date.indexOf("/"));
        } else {
            year = date;
        }
        
        if (year.length() != 4) {
            return date;
        }
        return year;
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

    public void setQuery(String query) {
        this.query = query;
    }

    public void setTakeOnlyYear(boolean takeOnlyYear) {
        this.takeOnlyYear = takeOnlyYear;
    }

}
