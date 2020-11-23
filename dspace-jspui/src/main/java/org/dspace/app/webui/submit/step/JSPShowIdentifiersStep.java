/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.submit.step;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.webui.submit.JSPStep;
import org.dspace.app.webui.submit.JSPStepManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.DOI;
import org.dspace.identifier.Handle;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierService;
import org.dspace.submit.step.SampleStep;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * This step shows previously minted persistend identifiers enduring the
 * submission of a new item.
 *
 * @See org.dspace.submit.step.ShowIdentifiersStep
 * @see org.dspace.submit.step.MintIdentifiersStep
 *
 * @author Pascal-Nicolas Becker (pascal at the dash library dash code dot de)
 */
public class JSPShowIdentifiersStep extends JSPStep
{
    /** JSP which displays the step to the user * */
    private static final String LIST_IDENTIFIER_JSP = "/submit/list-identifiers.jsp";
    /** JSP that lists the persistend identifier as part of the very step. */
    private static final String REVIEW_JSP = "/submit/review-identifiers.jsp";

    private static Logger log = LoggerFactory.getLogger(JSPShowIdentifiersStep.class);

    /**
     * Load and present previously minted identifiers.
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     */
    public void doPreProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // get the item
        Item item = subInfo.getSubmissionItem().getItem();
        if (item == null)
        {
            // this should never happen. Catch and log it to prevent possible NullPointerException.
            log.warn("JSPShowIdentifiersStep called, but no item supplied.");
            throw new IllegalStateException("JSPShowIdentifiersStep called, but no item supplied.");
        }

        // We'd like to configure if DOIs, Handles, other identifiers or any combination of these should be shown.
        // Therefore we'll to load dois, handles and other identifiers separately.
        String doi = null;
        String handle = null;
        List<String> otherIdentifiers = new LinkedList<String>();

        // retrieve the identifierService to load available identifierss
        IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);
        if (identifierService == null)
        {
            // the identifierService seems not to be initialized correctly. Log a warning and set errormessage
            log.warn("We were unable to load the identifier service. Please check your configuration.");
            request.setAttribute("errormessage", "jsp.submit.list-identifiers.no_identifier-service");
            return;
        }

        // load handle
        try
        {
            handle = identifierService.lookup(context, item, Handle.class);
        }
        catch (Exception ex)
        {
            // Some exception accured while fetching the handle. We cannot do anything but log it
            log.error("The following exception accured while we tried to fetch the handle:", ex);
        }

        // load DOI
        try
        {
            doi = identifierService.lookup(context, item, DOI.class);
        }
        catch (Exception ex)
        {
            // Some exception accured while fetching the handle. We cannot do anything but log it
            log.error("The following exception accured while we tried to fetch the handle:", ex);
        }

        // load everything we haven't loaded yet
        for (String identifier : identifierService.lookup(context, item))
        {
            if (! StringUtils.equals(handle, identifier) && ! StringUtils.equals(doi, identifier))
            {
                otherIdentifiers.add(identifier);
            }
        }

        // format doi and handle, if we got any. Don't do this before we compared them to possible other identifiers.
        if (!StringUtils.isEmpty(doi))
        {
            // remove the 'doi:' prefix
            if (StringUtils.startsWithIgnoreCase(doi, DOI.SCHEME))
            {
                doi = doi.substring(DOI.SCHEME.length());
            }
            // add the doi resolver
            doi = "https://doi.org/" + doi;
        }
        if (!StringUtils.isEmpty(handle))
        {
            handle = HandleManager.getCanonicalForm(handle);
        }


        // store loaded identifiers in the request
        request.setAttribute("doi", doi);
        request.setAttribute("handle", handle);
        request.setAttribute("other_identifiers", otherIdentifiers);

        if (doi == null && handle == null && otherIdentifiers.isEmpty())
        {
            // no identifiers found. log an error and set an errormessage to present on the jsp.
            log.error("Unable to find any identifiers assigned to item {}. Did you implemented the MintIdentifierStep " +
                              "into the submission process?", item.getID());
            request.setAttribute("errormessage", "jsp.submit.list-identifiers.no_identifiers_found");
        }

        // load the JSP to present the identifiers.
        JSPStepManager.showJSP(request, response, subInfo, LIST_IDENTIFIER_JSP);
    }

    /**
     * This step only present the identifiers. Therefore we do not have any processiong nor any post-processing.
     *
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * @param status
     *            any status/errors reported by doProcessing() method
     */
    public void doPostProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo, int status)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // nothing to do.
    }

    /**
     * We show one page listing all identifiers assigned to the item.
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
    
    /**
     * Return the URL path (e.g. /submit/review-metadata.jsp) of the JSP
     * which will review the information that was gathered in this Step.
     * <P>
     * This Review JSP is loaded by the 'Verify' Step, in order to dynamically
     * generate a submission verification page consisting of the information
     * gathered in all the enabled submission steps.
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     */
    public String getReviewJSP(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
    {
        return REVIEW_JSP;
    }
    
}
