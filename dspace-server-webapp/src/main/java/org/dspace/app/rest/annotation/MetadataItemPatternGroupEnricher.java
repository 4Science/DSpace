/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.core.Context;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MetadataItemPatternGroupEnricher extends MetadataItemEnricher {

    final String patternWithGroup;

    @Override
    protected BiConsumer<Context, Item> addMetadata(String value) {
        if (value == null) {
            return empty();
        }
        Matcher matcher = Pattern.compile(patternWithGroup).matcher(value);
        if (!matcher.find()) {
            return super.addMetadata(value);
        }
        return super.addMetadata(matcher.group(1));
    }

    public MetadataItemPatternGroupEnricher(
        String spel, MetadataFieldName metadata, Class<?> clazz, String patternWithGroup
    ) {
        super(spel, metadata, clazz);
        this.patternWithGroup = patternWithGroup;
    }
}
