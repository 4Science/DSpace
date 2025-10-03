/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.marcxml2item.reader;

import org.apache.commons.codec.binary.StringUtils;

/**
 * Processes identifier values by adding a "WOS:" prefix to values associated with a specified metadata field.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class IsiValueProcessor implements ImportIdentifierValueProcessor {

    private static final String WOS_PREFIX = "WOS:";

    private String metadataField;

    /**
     * Processes the given value associated with the specified identifier field.
     * If the identifier field matches the specified metadata field, a "WOS:" prefix
     * is added to the original value; otherwise, the original value is returned.
     *
     * @param identifierField the identifier field for which the value is processed
     * @param value           the original value to be processed
     * @return the processed value
     */
    @Override
    public String processValue(String identifierField, String value) {
        return StringUtils.equals(this.metadataField, identifierField) ? WOS_PREFIX + value : value;
    }

    public String getMetadataField() {
        return metadataField;
    }

    public void setMetadataField(String metadataField) {
        this.metadataField = metadataField;
    }

}
