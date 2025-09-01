/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.policy;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.mediafilter.FormatFilter;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public interface PolicyUpdaterService {

    /**
     * update resource polices of derivative bitstreams.
     * Removes all resource policies and set derivative bitstreams to be publicly accessible or
     * replaces derivative bitstreams policies using the same ones in the source bitstream.
     *
     * @param context      the context
     * @param bitstream    derivative bitstream
     * @param formatFilter formatFilter
     * @param source       the source bitstream
     * @throws SQLException       If something goes wrong in the database
     * @throws AuthorizeException if authorization error
     */
    void updatePoliciesOfDerivativeBitstream(
        Context context, Bitstream bitstream, FormatFilter formatFilter,
        Bitstream source, List<String> publicFiltersClasses
    ) throws SQLException, AuthorizeException;
}
