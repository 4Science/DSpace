/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.iterator;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.identifier.service.DOIService;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class DOIQueueByStatusIterator extends DOIQueueIterator {

    private static final Logger log = LogManager.getLogger(DOIQueueByStatusIterator.class);
    private final Context context;
    private final DOIService doiService;
    private final List<Integer> status;

    DOIQueueByStatusIterator(
        Context context, DOIService doiService, List<Integer> status,
        int offset, int limit
    ) {
        super(context, offset, limit);
        this.context = context;
        this.doiService = doiService;
        this.status = status;
    }

    @Override
    public void refreshIterator() {
        int queryOffset = -1;
        if (offset > 0) {
            offset += limit;
            queryOffset = offset + failed;
        } else if (failed > 0) {
            queryOffset = failed;
        }
        try {
            this.iterator =
                this.doiService.getDOIsByStatus(context, status, queryOffset, limit).iterator();
        } catch (SQLException e) {
            log.error("Cannot find DOI with status {}!", status,  e);
            this.iterator = Collections.emptyIterator();
        }
    }

}
