/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class AnnotationBodyRestDeserializer extends StdDeserializer<AnnotationBodyRest> {

    private String TAG_SANITIZER = "<[ /]*[a-zA-Z0-9 ]*[ /]*>";

    public AnnotationBodyRestDeserializer() {
        this(AnnotationBodyRest.class);
    }

    protected AnnotationBodyRestDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public AnnotationBodyRest deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException, JacksonException {
        ObjectCodec codec = jsonParser.getCodec();
        JsonNode json = codec.readTree(jsonParser);

        String chars = json.get("chars").asText();
        String type = json.get("@type").asText();
        String fullText = null;
        if (chars != null && !json.has(AnnotationBodyRest.FULL_TEXT)) {
            fullText = chars.replaceAll(TAG_SANITIZER, "");
        } else {
            fullText = json.get(AnnotationBodyRest.FULL_TEXT).asText();
        }

        return new AnnotationBodyRest().setType(type)
                                       .setFullText(fullText)
                                       .setChars(chars);
    }
}
