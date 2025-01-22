/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.strategy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.identifier.DOI;
import org.dspace.identifier.doi.action.DOIAction;
import org.dspace.identifier.doi.iterator.DOIIterator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
class DOIOrganiserStrategyImpl implements DOIOrganiserStrategy {

    protected Logger log = LogManager.getLogger(DOIOrganiserStrategyImpl.class);

    final DOIAction action;
    final DOIIterator iterator;

    DOIOrganiserStrategyImpl(
        @Autowired DOIAction action,
        @Autowired DOIIterator iterator
    ) {
        this.action = action;
        this.iterator = iterator;
    }

    @Override
    public void apply(Context context) {
        if (!iterator.hasNext()) {
            logEmptyDOI();
            return;
        }
        do {
            DOI doi = iterator.next();
            logProcessing(doi);
            boolean processed = applyAction(context, doi);
            if (!processed) {
                iterator.failed();
            }
        } while (iterator.hasNext());
    }

    boolean applyAction(Context context, DOI doi) {
        try {
            action.apply(context, doi);
            context.flush();
            return true;
        } catch (Exception e) {
            logError(
                "Error applying action " + action.getClass() + " for DOI: " + doi.getDoi(), e);
            return false;
        }
    }

    protected void logProcessing(DOI doi) {
        System.out.println("Processing DOI: " + doi.getDoi() + " with identifier: " + doi.getID());
        log.info("Processing DOI: {} with identifier {}", doi.getDoi(), doi.getID());
    }

    protected void logEmptyDOI() {
        System.err.println("There are no valid DOI objects in the database!");
        log.error("There are no valid DOI objects in the database!");
    }

    protected void logError(String s, Exception e) {
        System.err.println(s);
        e.printStackTrace();
        log.error(s, e);
    }

}
