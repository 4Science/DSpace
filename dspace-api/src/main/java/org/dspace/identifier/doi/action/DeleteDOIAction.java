/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.action;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.dspace.core.Context;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.doi.DOIIdentifierException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class DeleteDOIAction extends AbstractDOIAction {

    public DeleteDOIAction(
        @Autowired DOIIdentifierProvider provider
    ) {
        super(provider);
        log = LogManager.getLogger(DeleteDOIAction.class);
    }

    /**
     * Deletes a DOI
     *
     * @param doi - DOI to delete
     * @throws SQLException
     */
    public void apply(Context context, DOI doi) throws Exception {

        if (null == doi) {
            throw new IllegalArgumentException("You specified a valid DOI, that is not stored in our database.");
        }

        String doiIdentifier = null;

        try {

            if (provider == null) {
                logInfo("Cannot find any provider, skipping doi: " + doi.getDoi());
                return;
            }

            provider.deleteOnline(context, doi.getDoi());

            logInfo("It was possible to delete this identifier: "
                    + DOI.SCHEME + doi.getDoi()
                    + " online.");
        } catch (DOIIdentifierException ex) {
            // Identifier was not recognized as DOI.
            logError("It wasn't possible to detect this identifier: " + doiIdentifier, ex);
            throw ex;
        } catch (IllegalArgumentException ex) {
            logError("It wasn't possible to delete this identifier: "
                               + DOI.SCHEME + doi.getDoi()
                               + " online. Take a look in log file.", ex);
            throw ex;
        }
    }
}
