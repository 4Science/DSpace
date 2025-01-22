/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.iterator;

import javax.annotation.CheckForNull;

import com.google.common.collect.AbstractIterator;
import org.dspace.identifier.DOI;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class DOIWrapperIterator extends AbstractIterator<DOI> implements DOIIterator {

    private DOI doi;

    DOIWrapperIterator(DOI doi) {
        this.doi = doi;
    }

    @CheckForNull
    @Override
    protected DOI computeNext() {
        DOI that =  this.doi;
        this.doi = null;
        return that;
    }

    @Override
    public void failed() {
        this.doi = null;
    }
}
