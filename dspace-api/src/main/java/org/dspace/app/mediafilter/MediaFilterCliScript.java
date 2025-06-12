/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import org.dspace.core.Context;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MediaFilterCliScript extends MediaFilterScript {

    @Override
    protected void handleAuthorizationSystem(Context context) {
        if (context.ignoreAuthorization()) {
            context.restoreAuthSystemState();
        } else {
            context.turnOffAuthorisationSystem();
        }
    }
}
