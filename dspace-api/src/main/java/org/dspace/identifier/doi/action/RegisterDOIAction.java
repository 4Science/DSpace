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
import org.dspace.content.DSpaceObject;
import org.dspace.content.logic.Filter;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.doi.DOIIdentifierException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class RegisterDOIAction extends AbstractDOIAction {

    final Filter filter;

    public RegisterDOIAction(
        @Autowired DOIIdentifierProvider provider,
        @Autowired Filter filter
    ) {
        super(provider);
        this.filter = filter;
        log = LogManager.getLogger(RegisterDOIAction.class);
    }

    /**
     * Register DOI with the provider
     *
     * @param doi - doi to register
     * @throws SQLException
     * @throws DOIIdentifierException
     */
    @Override
    public void apply(Context context, DOI doi) throws Exception {
        DSpaceObject dso = doi.getDSpaceObject();
        if (dso == null) {
            throw new IllegalArgumentException("Linked DSpaceObject not found for doi: " + doi.getDoi());
        }
        if (Constants.ITEM != dso.getType()) {
            throw new IllegalArgumentException("Currently DSpace supports DOIs for Items only.");
        }

        try {

            if (provider == null) {
                logInfo("Cannot find any provider, skipping doi: " + doi.getDoi());
                return;
            }

            provider.registerOnline(context, dso, DOI.SCHEME + doi.getDoi(), filter);

            //if (!quiet) {
            logInfo("This identifier: "
                    + DOI.SCHEME + doi.getDoi()
                    + " is successfully registered.");
            //}
        } catch (IdentifierException ex) {
            if (!(ex instanceof DOIIdentifierException)) {
                logError("It wasn't possible to register this identifier: "
                          + DOI.SCHEME + doi.getDoi()
                          + " online. ", ex);
                throw ex;
            }

            DOIIdentifierException doiIdentifierException = (DOIIdentifierException) ex;

            DOIActionFactory.instance()
                .createAlertEmailAction(dso)
                .sendAlertMail(
                    "Register",
                    DOI.SCHEME + doi.getDoi(),
                    DOIIdentifierException.codeToString(doiIdentifierException.getCode())
                );

            logError("It wasn't possible to register this identifier : "
                      + DOI.SCHEME + doi.getDoi()
                      + " online. Exceptions code: "
                      + DOIIdentifierException.codeToString(doiIdentifierException.getCode()), ex);

            throw ex;
        } catch (IllegalArgumentException ex) {
            logError("Database table DOI contains a DOI that is not valid: "
                      + DOI.SCHEME + doi.getDoi() + "!", ex);

            throw new IllegalStateException("Database table DOI contains a DOI "
                                            + " that is not valid: "
                                            + DOI.SCHEME + doi.getDoi() + "!", ex);
        } catch (SQLException ex) {
            logError("Error while trying to get data from database", ex);
            throw new RuntimeException("Error while trying to get data from database", ex);
        }
    }
}
