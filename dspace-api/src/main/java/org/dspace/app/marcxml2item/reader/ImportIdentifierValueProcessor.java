/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.marcxml2item.reader;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public interface ImportIdentifierValueProcessor {

    /**
     * Processes the given value associated with the specified identifier field.
     *
     * @param identifierField the identifier field for which the value is processed
     * @param value           the original value to be processed
     * @return the processed value
     */
    String processValue(String identifierField, String value);

}
