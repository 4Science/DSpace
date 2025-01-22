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
public class UpdateDOIAction extends AbstractDOIAction {

    UpdateDOIAction(
        @Autowired DOIIdentifierProvider provider
    ) {
        super(provider);
        this.log = LogManager.getLogger(UpdateDOIAction.class);
    }

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

            provider.updateMetadataOnline(context, dso,
                DOI.SCHEME + doi.getDoi()
            );

            logInfo("Successfully updated metadata of DOI " + DOI.SCHEME
                    + doi.getDoi() + ".");
        } catch (IdentifierException ex) {
            if (!(ex instanceof DOIIdentifierException)) {
                logError("It wasn't possible to register the identifier online. ", ex);
                throw ex;
            }

            DOIIdentifierException doiIdentifierException = (DOIIdentifierException) ex;

            DOIActionFactory.instance()
                .createAlertEmailAction(dso)
                .sendAlertMail(
                    "Update",
                    DOI.SCHEME + doi.getDoi(),
                    DOIIdentifierException.codeToString(doiIdentifierException.getCode())
                );

            logError("It wasn't possible to update this identifier:  "
                      + DOI.SCHEME + doi.getDoi()
                      + " Exceptions code:  "
                      + DOIIdentifierException.codeToString(doiIdentifierException.getCode()), ex);

            throw ex;
        } catch (IllegalArgumentException ex) {
            logError("Database table DOI contains a DOI that is not valid: "
                      + DOI.SCHEME + doi.getDoi() + "!", ex);
            throw new IllegalStateException("Database table DOI contains a DOI "
                                            + " that is not valid: "
                                            + DOI.SCHEME + doi.getDoi() + "!", ex);
        } catch (SQLException ex) {
            logError("It wasn't possible to connect to the Database!", ex);
            throw new RuntimeException("It wasn't possible to connect to the Database!", ex);
        } catch (Exception ex) {
            logError("We hit an unexpected exception with the doi " + doi.getDoi(), ex);
            throw ex;
        }
    }
}
