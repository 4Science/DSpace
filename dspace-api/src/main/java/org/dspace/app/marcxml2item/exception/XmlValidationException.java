/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.marcxml2item.exception;

/**
 * Exception for errors that occurs during the XML validation.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class XmlValidationException extends RuntimeException {

    private static final long serialVersionUID = 3377335341871311369L;

    /**
     * Constructor with error message and cause.
     *
     * @param message the error message
     * @param cause   the error cause
     */
    public XmlValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with error message.
     *
     * @param message the error message
     */
    public XmlValidationException(String message) {
        super(message);
    }

    /**
     * Constructor with error cause.
     *
     * @param cause the error cause
     */
    public XmlValidationException(Throwable cause) {
        super(cause);
    }

}
