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
import org.dspace.identifier.service.DOIService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class DOIIteratorFactoryImpl extends DOIIteratorFactory {

    @Autowired()
    DOIService doiService;

    @Override
    public DOIQueueByStatusIterator queueByStatus(Context context, List<Integer> status) {
        return queueByStatus(context, status, -1, -1);
    }

    @Override
    public DOIQueueByStatusIterator queueByStatus(Context context, List<Integer> status, int offset, int limit) {
        return new DOIQueueByStatusIterator(context, doiService, status, offset, limit);
    }

    @Override
    public DOIWrapperIterator wrapper(DOI doi) {
        return new DOIWrapperIterator(doi);
    }
}
