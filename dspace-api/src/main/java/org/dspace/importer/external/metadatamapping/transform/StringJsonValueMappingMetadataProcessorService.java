/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.dspace.importer.external.metadatamapping.contributor.JsonPathMetadataProcessor;
import org.dspace.util.SimpleMapConverter;

/**
 * This class is a Metadata processor from a structured JSON Metadata result
 * and uses a SimpleMapConverter, with a mapping properties file
 * to map to a single string value based on mapped keys.<br/>
 * Like:<br/>
 * <code>journal-article = Article<code/>
 *
 * @author paulo-graca
 *
 */
public class StringJsonValueMappingMetadataProcessorService extends JsonPathMetadataProcessor {

    /**
     * The value map converter.
     * a list of values to map from
     */
    private SimpleMapConverter valueMapConverter;
    private String path;

    @Override
    public Collection<String> processMetadata(String json) {
        JsonNode rootNode = convertStringJsonToJsonNode(json);
        Optional<JsonNode> abstractNode = Optional.ofNullable(rootNode.at(path));
        Collection<String> values = new ArrayList<>(1);

        if (abstractNode.isPresent() && abstractNode.get().getNodeType().equals(JsonNodeType.STRING)) {

            String stringValue = abstractNode.get().asText();
            values.add(Optional.ofNullable(stringValue)
                         .map(value -> valueMapConverter != null ? valueMapConverter.getValue(value) : value)
                         .orElse(valueMapConverter.getValue(null)));
        }
        return values;
    }

    /* Getters and Setters */

    public String convertType(String type) {
        return valueMapConverter != null ? valueMapConverter.getValue(type) : type;
    }

    public void setValueMapConverter(SimpleMapConverter valueMapConverter) {
        this.valueMapConverter = valueMapConverter;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
