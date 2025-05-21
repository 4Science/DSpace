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
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.mediafilter.FormatFilter;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public abstract class AbstractPolicyUpdater implements PolicyUpdater {

    private static final Log log = LogFactory.getLog(AbstractPolicyUpdater.class);
    final GroupService groupService;
    final AuthorizeService authorizeService;

    protected AbstractPolicyUpdater(GroupService groupService, AuthorizeService authorizeService) {
        this.groupService = groupService;
        this.authorizeService = authorizeService;
    }

    protected boolean hasPolicy(Context context, DSpaceObject dso, ResourcePolicy rp) {
        try {
            ResourcePolicy found =
                authorizeService.findByTypeGroupAction(context, dso, rp.getGroup(), rp.getAction());
            return found != null &&
                Objects.equals(rp.getEPerson(), found.getEPerson()) &&
                Objects.equals(rp.getEndDate(), found.getStartDate()) &&
                Objects.equals(rp.getEndDate(), found.getEndDate());
        } catch (SQLException e) {
            log.error("Cannot find any related policy to dso " + dso.getID() + " for policy " + rp, e);
        }
        return false;
    }

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

    /**
     * update resource polices of derivative bitstreams.
     * by remove all resource policies and
     * set derivative bitstreams to be publicly accessible or
     * replace derivative bitstreams policies using
     * the same in the source bitstream.
     *
     * @param context      the context
     * @param bitstream    derivative bitstream
     * @param formatFilter formatFilter
     * @param source       the source bitstream
     * @throws SQLException       If something goes wrong in the database
     * @throws AuthorizeException if authorization error
     */
    protected void updatePoliciesOfDerivativeBitstream(Context context, Bitstream bitstream, FormatFilter formatFilter,
                                                       Bitstream source, List<String> publicFiltersClasses)
        throws SQLException, AuthorizeException {

        authorizeService.removeAllPolicies(context, bitstream);

        if (formatFilter != null && publicFiltersClasses.contains(formatFilter.getClass().getSimpleName())) {
            Group anonymous = groupService.findByName(context, Group.ANONYMOUS);
            authorizeService.addPolicy(context, bitstream, Constants.READ, anonymous);
            List<Bundle> bundles = bitstream.getBundles();
            for (Bundle bundle : bundles) {
                ResourcePolicy anonymousPolicy =
                    authorizeService.findByTypeGroupAction(context, bundle, anonymous, Constants.READ);
                if (anonymousPolicy == null) {
                    authorizeService.addPolicy(context, bundle, Constants.READ, anonymous);
                }
            }
        } else {
            authorizeService.replaceAllPolicies(context, source, bitstream);
            clonePoliciesFromOriginalBundle(
                context, bitstream, source.getBundles().get(0), source.getResourcePolicies()
            );
        }
    }

    private void clonePoliciesFromOriginalBundle(
        Context context, Bitstream bitstream, Bundle original, List<ResourcePolicy> addedPolicies
    ) throws SQLException, AuthorizeException {

        if (original == null || addedPolicies.isEmpty()) {
            return;
        }

        if (!Objects.equals(Constants.CONTENT_BUNDLE_NAME, original.getName())) {
            return;
        }

        List<Bundle> bundles = bitstream.getBundles();
        for (Bundle bundle : bundles) {
            // cleanup policies of the derived bundle
            authorizeService.removeAllPolicies(context, bundle);
            // copies policies from the original bundle
            addToBundlePolicies(context, bundle, original.getResourcePolicies());
        }
    }

    private void addToBundlePolicies(
        Context context,
        Bundle bundle,
        List<ResourcePolicy> resourcePolicies
    ) throws SQLException, AuthorizeException {
        authorizeService.addPolicies(
            context,
            resourcePolicies
                .stream()
                .filter(resourcePolicy -> !hasPolicy(context, bundle, resourcePolicy))
                .collect(Collectors.toList()),
            bundle
        );
    }
}
