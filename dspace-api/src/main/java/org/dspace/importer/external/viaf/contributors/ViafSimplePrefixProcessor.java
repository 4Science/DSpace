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

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang.StringUtils;
import org.dspace.importer.external.metadatamapping.contributor.AbstractJsonPathMetadataProcessor;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class ViafSimplePrefixProcessor extends AbstractJsonPathMetadataProcessor {

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

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

}
