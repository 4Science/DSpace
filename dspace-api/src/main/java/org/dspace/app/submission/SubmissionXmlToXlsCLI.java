/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.submission;

import org.dspace.core.Context;
import org.dspace.scripts.DSpaceRunnable;

/**
 * Implementation of {@link DSpaceRunnable} to perform a submission export via XLS file.
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 *
 */
public class SubmissionXmlToXlsCLI extends SubmissionXmlToXls {

    @Override
    protected boolean isAuthorized(Context context) {
        return true;
    }

}
