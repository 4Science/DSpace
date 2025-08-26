/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

/**
 * This Processor allows to extract all values of a matrix.
 * Only need to configure the path to the matrix in "pathToMatrix"
 * For exaple to extract all values
 * "matrix": [
 *     [
 *      "first",
 *      "second"
 *     ],
 *     [
 *      "third"
 *     ],
 *     [
 *      "fourth",
 *      "fifth"
 *     ]
 *   ],
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class MatrixElementProcessor extends JsonPathMetadataProcessor {

    private String pathToMatrix;

    @Override
    public Collection<String> processMetadata(String json) {
        JsonNode rootNode = convertStringJsonToJsonNode(json);
        Iterator<JsonNode> array = rootNode.at(pathToMatrix).elements();
        Collection<String> values = new ArrayList<>();
        while (array.hasNext()) {
            JsonNode element = array.next();
            if (element.isArray()) {
                Iterator<JsonNode> nodes = element.iterator();
                while (nodes.hasNext()) {
                    String nodeValue = nodes.next().textValue();
                    if (StringUtils.isNotBlank(nodeValue)) {
                        values.add(nodeValue);
                    }
                }
            } else {
                String nodeValue = element.textValue();
                if (StringUtils.isNotBlank(nodeValue)) {
                    values.add(nodeValue);
                }
            }
        }
        return values;
    }

    public void setPathToMatrix(String pathToMatrix) {
        this.pathToMatrix = pathToMatrix;
    }

}