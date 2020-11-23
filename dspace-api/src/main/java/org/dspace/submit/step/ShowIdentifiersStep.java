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
 * This step shows previously minted persistent identifiers (handle, doi,
 * ...) so that an author can include them into his/her files.
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
 * This step is the one shpwing the previously minted identifiers.
 *
 * @author Pascal-Nicolas Becker (pascal at the dash library dash code dot de)
 */
public class ShowIdentifiersStep extends AbstractProcessingStep
{

    private static Logger log = LoggerFactory.getLogger(ShowIdentifiersStep.class);

    /**
     * This method will run after the pre-processing already displayed the
     * identifiers. Therefore all we need to do is to mark this step as
     * completed.
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
        // a second separate step that does show the previously minted identifiers to the user. Following that this step
        // seems to do nothing. Its JSPUI part does show the previously minted identifiers.

        return STATUS_COMPLETE;
    }

    
    /**
     * We show the identifiers on one page => return 1
     * 
     * 
     * @param request
     *            The HTTP Request
     * @param subInfo
     *            The current submission information object
     * 
     * @return the number of pages in this step
     */
    public int getNumberOfPages(HttpServletRequest request,
            SubmissionInfo subInfo) throws ServletException
    {
        return 1;
    }
}