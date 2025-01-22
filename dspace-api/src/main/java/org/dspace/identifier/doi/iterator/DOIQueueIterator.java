/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.iterator;

import java.util.Iterator;
import javax.annotation.CheckForNull;

import com.google.common.collect.AbstractIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.identifier.DOI;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public abstract class DOIQueueIterator extends AbstractIterator<DOI> implements DOIIterator {

    private static final Logger log = LogManager.getLogger(DOIQueueIterator.class);

    protected Iterator<DOI> iterator;
    protected final Context context;
    protected int limit;
    protected int offset;
    protected int failed = 0;

    DOIQueueIterator(Context context,int offset, int limit) {
        this.context = context;
        this.offset = offset;
        this.limit = limit;
    }

    public abstract void refreshIterator();

    @CheckForNull
    @Override
    protected DOI computeNext() {
        try {
            if (iterator == null) {
                this.refreshIterator();
            }
            // consume next page of the queue
            if (!iterator.hasNext()) {
                context.commit();
                refreshIterator();
            }
            // consume doi in the current slice of the queue
            if (iterator.hasNext()) {
                try {
                    DOI doi = iterator.next();
                    if (doi != null) {
                        return doi;
                    } else {
                        failed();
                        log.warn("Found null DOI, trying to fetch the next one!");
                        return computeNext();
                    }
                } catch (Exception e) {
                    failed();
                    log.error("Cannot fetch the current DOI!", e);
                    return computeNext();
                }
            }
        } catch (Exception e) {
            failed();
            log.error("Cannot get the next DOI!", e);
        }
        return endOfData();
    }

    @Override
    public void failed() {
        this.failed++;
    }
}
