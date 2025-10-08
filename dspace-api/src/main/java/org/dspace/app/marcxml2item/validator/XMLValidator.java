/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.marcxml2item.validator;

import org.dspace.scripts.handler.DSpaceRunnableHandler;

/**
 * Interface for XML validation.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 **/
public interface XMLValidator {

    boolean validate(byte[] xmlContent, DSpaceRunnableHandler handler);

}
