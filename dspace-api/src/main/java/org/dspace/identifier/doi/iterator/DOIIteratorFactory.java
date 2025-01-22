/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.iterator;

import java.util.List;

import org.dspace.core.Context;
import org.dspace.identifier.DOI;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public abstract class DOIIteratorFactory {

    public static DOIIteratorFactory instance() {
        return DSpaceServicesFactory.getInstance()
                                    .getServiceManager()
                                    .getServicesByType(DOIIteratorFactory.class)
                                    .get(0);
    }


    public abstract DOIQueueByStatusIterator queueByStatus(
        Context context, List<Integer> status
    );
    public abstract DOIQueueByStatusIterator queueByStatus(
        Context context, List<Integer> status, int offset, int limit
    );

    public abstract DOIWrapperIterator wrapper(DOI doi);

}
