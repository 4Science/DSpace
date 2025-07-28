/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.dspace.content.DSpaceObject;

/**
 * Interface for mapping a DSpaceObject to a generic value.
 *
 * This interface defines a contract for implementing classes to map a given
 * DSpaceObject into a generic value of type R. It is used in the context of
 * indexing and discovery within the DSpace framework.
 *
 * @param <T> the type of DSpaceObject to be mapped
 * @param <R> the type of the resulting mapped value
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public interface IndexPluginMapper<T extends DSpaceObject, R> {

    /**
     * Maps a given DSpaceObject into a generic value.
     */
    R map(T t);

}
