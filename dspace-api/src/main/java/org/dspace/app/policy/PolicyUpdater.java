/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.policy;

import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Interface for updating resource policies on bitstreams.
 *
 *  <p>Implementations handle the propagation of access policies from source bitstreams
 * to their derivatives.</p>
 *
 * <p>This interface is used to ensure that derivative bitstreams inherit the correct
 * access policies based on their source bitstream.</p>
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public interface PolicyUpdater {

    /**
     * Update resource policies of derivative bitstreams.
     *
     * <p>Removes all resource policies and sets derivative bitstreams to be publicly accessible or
     * replaces derivative bitstreams policies using the same ones in the source bitstream.</p>
     *
     * @param context Context
     * @param item Item to which the bitstream belongs
     * @param source the source bitstream from which policies are derived
     * @throws Exception
     */
    void updatePolicies(Context context, Item item, Bitstream source) throws Exception;

}
