/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierService;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * This step mints the persistent identifiers (handle, doi, ...) an item will
 * get assigned when entering the repository.
 * This feature cannot be included in any official release of DSpace as the
 * JSPUI won't be continued in DSpace 7 and all following minor versions of
 * DSpace 6 will contain bugfixes only. Therefore we developped this feature
 * in a way that keeps it as close to the existing code as possible, to keep
 * minor verison updates as simple as possible.
 * In JSPUI's JSPStepManager there is a bug. It detects a step as
 * non-interactive one, by checking if a JSP was called enduring the step's
 * pre-processing. Non-interactive steps are steps working in the background
 * of the submission process, no being listed in the progress bar, not calling
 * any JSPs. Therefore for any step detected as non-inveractive one its
 * post-processing method is never called.
 * To keep updates simple we decided to not fix the bug, but to create two
 * submission steps: one step mints the identifiers without showing any JSP in
 * the pre-processing. The other one shows identifiers in the preprocessing
 * without doing anything in the processing and in the post-processing.
 * This step is the one minting the identifiers.
 *
 * @author Pascal-Nicolas Becker (pascal at the dash library dash code dot de)
 */
public class MintIdentifiersStep extends AbstractProcessingStep
{

    private static Logger log = LoggerFactory.getLogger(MintIdentifiersStep.class);
    /***************************************************************************
     * STATUS / ERROR FLAGS (returned by doProcessing() if an error occurs or
     * additional user interaction may be required)
     * 
     * (Do NOT use status of 0, since it corresponds to STATUS_COMPLETE flag
     * defined in the JSPStepManager class)
     **************************************************************************/
    public static final int STATUS_NO_ITEM = 1;
    public static final int STATUS_UNEXPECTED_ERROR = 2;
    public static final int STATUS_MINTING_ERROR = 3;

    // default constructor, just to add error messages.
    public MintIdentifiersStep()
    {
        super();
        this.addErrorMessage(STATUS_NO_ITEM,
                             "The MintIdentifiersStep was unable to obtain any submission item.");
        this.addErrorMessage(STATUS_UNEXPECTED_ERROR,
                             "An unexpected Error occured. Check the logfiles for further information.");
        this.addErrorMessage(STATUS_MINTING_ERROR,
                             "An error occured while minting the persistent identifiers. Check the log files for further information.");
    }

    /**
     * This method calls IdentifierService.reserver(...) that will call the
     * mint methods of all configured IdentifierServiceProviders, which will
     * then mint the identifiers.
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * @return Status or error flag which will be processed by
     *         doPostProcessing() below! (if STATUS_COMPLETE or 0 is returned,
     *         no errors occurred!)
     */
    public int doProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // currently it is not possible to write a submission step that does not send any JSP at start of the step, but
        // only at its end. Therefore we have to write a non-interactive step that does mint the identifiers only and
        // a second separate stept that does show the previously minted identifiers to the user.

        // get the item
        Item item = subInfo.getSubmissionItem().getItem();
        if (item == null)
        {
            // this shouldn't happen, catch and log it anyway.
            log.warn("MintIdentifiersStep called, but no item supplied.");
            return STATUS_NO_ITEM;
        }

        // try to get the identifier service
        IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);
        try {
            if (identifierService != null)
            {
                // reserving let the identifier service calls mint on all IdentivierServiceProvider.
                identifierService.reserve(context, item);
            }
        } catch (IdentifierException ex) {
            log.error("Cannot mint identifier.", ex);
            return STATUS_MINTING_ERROR;
        }

        // store changes to the item, we do not need to call context.commit here,
        // it is done by the submission controller.
        item.update();

        return STATUS_COMPLETE;
    }

    
    /**
     * As this is a non-interactive step we do not have any pages => 0.
     * 
     * @return the number of pages in this step
     */
    public int getNumberOfPages(HttpServletRequest request,
            SubmissionInfo subInfo) throws ServletException
    {
        /*
         * If you return 0, this Step will not appear in the Progress Bar at
         * ALL! Therefore it is important for non-interactive steps to return 0.
         */

        return 0;
    }
}