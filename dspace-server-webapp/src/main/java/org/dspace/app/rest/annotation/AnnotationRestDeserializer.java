/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DCDate;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class AnnotationRestDeserializer extends StdDeserializer<AnnotationRest> {

    public static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static DateTimeFormatter DATETIME_FORMATTER =  DateTimeFormatter.ofPattern(DATETIME_FORMAT);

    private static final Logger log = LogManager.getLogger(AnnotationRestDeserializer.class);

    public AnnotationRestDeserializer() {
        this(AnnotationRest.class);
    }

    protected AnnotationRestDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    @SuppressWarnings("unchecked")
    public AnnotationRest deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException, JacksonException {
        ObjectCodec codec = jsonParser.getCodec();
        JsonNode json = codec.readTree(jsonParser);

        String id = null;
        JsonNode obj = json.get("@id");
        if (obj != null) {
            id = obj.asText();
        }
        // type must be `oa:Annotation`
        String type = json.get("@type").asText();
        // context should be `http://iiif.io/api/presentation/2/context.json`
        String context = json.get("@context").asText();

        String created = null;
        if (json.has("http://purl.org/dc/terms/created")) {
            created = json.get("http://purl.org/dc/terms/created").asText();
        } else if (json.has("dcterms:created")) {
            created = json.get("dcterms:created").asText();
        }
        String modified = null;
        if (created == null) {
            created = LocalDateTime.now(ZoneId.of("UTC")).format(DATETIME_FORMATTER);
            modified = LocalDateTime.now(ZoneId.of("UTC")).format(DATETIME_FORMATTER);
        } else if (json.has("http://purl.org/dc/terms/modified")) {
            modified = json.get("http://purl.org/dc/terms/modified").asText();
        } else if (json.has("dcterms:modified")) {
            modified = json.get("dcterms:modified").asText();
        } else {
            modified = LocalDateTime.now(ZoneId.of("UTC")).format(DATETIME_FORMATTER);
        }
        LocalDateTime createdDate = LocalDateTime.ofInstant(
            new DCDate(created).toDate().toInstant(), ZoneId.of("UTC")
        );
        LocalDateTime modifiedDate = LocalDateTime.ofInstant(
            new DCDate(modified).toDate().toInstant(), ZoneId.of("UTC")
        );

        List<String> motivation = List.of();
        JsonNode motivationNode = json.get("motivation");
        if (motivationNode.isTextual()) {
            motivation = List.of(motivationNode.asText());
        } else if (motivationNode.isArray()) {
            try (JsonParser motivationParser = motivationNode.traverse()) {
                motivationParser.setCodec(codec);
                motivation = motivationParser.readValueAs(new TypeReference<List<String>>() { });
            }
        }

        List<AnnotationBodyRest> resource = null;
        JsonNode resourceNode = json.get("resource");
        if (resourceNode == null || !(resourceNode.isArray() || resourceNode.isObject())) {
            log.error("Cannot parse the field resource! Its value is incompatible {}", resourceNode);
            throw new IllegalArgumentException("Cannot parse the field resource! Its value is incompatible");
        }
        try (JsonParser resourceParser = resourceNode.traverse()) {
            resourceParser.setCodec(codec);
            if (resourceNode.isArray()) {
                resource = resourceParser.readValueAs(new TypeReference<List<AnnotationBodyRest>>() { });
            } else if (resourceNode.isObject()) {
                resource = List.of(resourceParser.readValueAs(AnnotationBodyRest.class));
            }
        } catch (Exception e) {
            log.error("Cannot deserialize annotation", e);
        }
        JsonNode onNode = json.get("on");
        if (onNode == null || !(onNode.isArray() || onNode.isObject())) {
            log.error("Cannot parse the field on! Its value is incompatible {}", resourceNode);
            throw new IllegalArgumentException("Cannot parse the field on! Its value is incompatible");
        }
        List<AnnotationTargetRest> on = null;
        try (JsonParser resourceParser = onNode.traverse()) {
            resourceParser.setCodec(codec);
            if (resourceNode.isArray()) {
                on = resourceParser.readValueAs(new TypeReference<List<AnnotationTargetRest>>() { });
            } else if (resourceNode.isObject()) {
                on = List.of(resourceParser.readValueAs(AnnotationTargetRest.class));
            }
        } catch (Exception e) {
            log.error("Cannot deserialize annotation", e);
        }
        return new AnnotationRest().setType(type)
                                   .setContext(context)
                                   .setCreated(createdDate)
                                   .setModified(modifiedDate)
                                   .setMotivation(motivation)
                                   .setId(id)
                                   .setOn(on)
                                   .setResource(resource);
    }

}
