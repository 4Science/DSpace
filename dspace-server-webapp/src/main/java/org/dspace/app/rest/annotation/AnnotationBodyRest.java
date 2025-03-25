/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 *
 * <code>
 * "resource": {
 *     // needs to be stored! - resource.chars & resource.fulltext
 *     "chars": "<p>Test</p>",
 *     // static
 *     "@type": "dctypes:Text"
 * }
 * </code>
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(using = AnnotationBodyRestDeserializer.class)
public class AnnotationBodyRest {

    static final String FULL_TEXT = "http://dev.llgc.org.uk/sas/full_text";
    static final String TYPE = "dctypes:Text";

    @JsonProperty(value = "@type", defaultValue = TYPE, required = true)
    String type = TYPE;
    @JsonProperty("chars")
    String chars;
    @JsonProperty("language")
    String language;
    @JsonProperty("format")
    String format;
    @JsonProperty(FULL_TEXT)
    String fullText;

    public AnnotationBodyRest setType(String type) {
        this.type = type;
        return this;
    }

    public AnnotationBodyRest setChars(String chars) {
        this.chars = chars;
        return this;
    }

    public AnnotationBodyRest setFullText(String fullText) {
        this.fullText = fullText;
        return this;
    }

    public AnnotationBodyRest setLanguage(String language) {
        this.language = language;
        return this;
    }

    public AnnotationBodyRest setFormat(String format) {
        this.format = format;
        return this;
    }

    public String getType() {
        return type;
    }

    public String getChars() {
        return chars;
    }

    public String getLanguage() {
        return language;
    }

    public String getFormat() {
        return format;
    }

    public String getFullText() {
        return fullText;
    }
}
