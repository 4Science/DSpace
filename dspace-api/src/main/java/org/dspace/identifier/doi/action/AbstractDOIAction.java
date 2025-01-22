/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.action;

import org.apache.logging.log4j.Logger;
import org.dspace.identifier.DOIIdentifierProvider;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
abstract class AbstractDOIAction implements DOIAction {

    final DOIIdentifierProvider provider;
    protected Logger log;

    AbstractDOIAction(DOIIdentifierProvider provider) {
        this.provider = provider;
    }

    protected void logError(String message, Exception ex) {
        if (ex != null) {
            ex.printStackTrace();
        }
        System.err.println(message);
        log.error(message, ex);
    }

    protected void logInfo(String message) {
        System.out.println(message);
    }
}
