/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher.metadata;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that encapsulates the extraction of a group from a pattern.
 * The first group of the pattern is extracted from the value.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class MetadataPatternGroupExtractor {
    final String patternWithGroup;

    public MetadataPatternGroupExtractor(String patternWithGroup) {
        this.patternWithGroup = patternWithGroup;
    }

    public String extract(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = Pattern.compile(patternWithGroup).matcher(value);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Pattern " + patternWithGroup + " not found in value " + value);
        }
        return matcher.group(1);
    }
}