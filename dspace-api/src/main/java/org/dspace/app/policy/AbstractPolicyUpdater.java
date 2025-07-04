/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.policy;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.mediafilter.FormatFilter;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Interface for updating resource policies on bitstreams.
 * Implementations handle the propagation of access policies from source bitstreams
 * to their derivatives.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public abstract class AbstractPolicyUpdater implements PolicyUpdater {

    private static final Log log = LogFactory.getLog(AbstractPolicyUpdater.class);

    @Autowired
    protected GroupService groupService;

    @Autowired
    protected AuthorizeService authorizeService;

    @Autowired
    protected PolicyUpdaterService policyUpdaterService;

    protected List<String> getPublicFiltersClasses() {
        String[] publicPermissionFilters =
            DSpaceServicesFactory.getInstance()
                                 .getConfigurationService()
                                 .getArrayProperty("filter.org.dspace.app.mediafilter.publicPermission");
        List<String> publicFiltersClasses = new ArrayList<>();
        if (publicPermissionFilters != null) {
            for (String filter : publicPermissionFilters) {
                publicFiltersClasses.add(filter.trim());
            }
        }
        return publicFiltersClasses;
    }

    protected void updatePoliciesOfDerivativeBitstream(Context context, Bitstream bitstream, FormatFilter formatFilter,
                                                       Bitstream source, List<String> publicFiltersClasses)
        throws SQLException, AuthorizeException {
        this.policyUpdaterService.updatePoliciesOfDerivativeBitstream(context, bitstream, formatFilter, source,
                                                                      publicFiltersClasses);
    }

    protected void updatePoliciesOfDerivativeBitstream(Context context, Bitstream bitstream, Bitstream source)
        throws SQLException, AuthorizeException {
        this.updatePoliciesOfDerivativeBitstream(
            context, bitstream, null, source, getPublicFiltersClasses()
        );
    }


}
