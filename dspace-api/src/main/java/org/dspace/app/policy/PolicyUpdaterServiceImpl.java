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

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class PolicyUpdaterServiceImpl implements PolicyUpdaterService {

    private static final Log log = LogFactory.getLog(PolicyUpdaterServiceImpl.class);
    private final AuthorizeService authorizeService;
    private final GroupService groupService;

    PolicyUpdaterServiceImpl(AuthorizeService authorizeService, GroupService groupService) {
        this.authorizeService = authorizeService;
        this.groupService = groupService;
    }

    protected boolean hasPolicy(Context context, DSpaceObject dso, ResourcePolicy rp) {
        try {
            ResourcePolicy found =
                authorizeService.findByTypeGroupAction(context, dso, rp.getGroup(), rp.getAction());
            return found != null &&
                Objects.equals(rp.getEPerson(), found.getEPerson()) &&
                Objects.equals(rp.getStartDate(), found.getStartDate()) &&
                Objects.equals(rp.getEndDate(), found.getEndDate());
        } catch (SQLException e) {
            log.error("Cannot find any related policy to dso " + dso.getID() + " for policy " + rp, e);
        }
        return false;
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
    public void updatePoliciesOfDerivativeBitstream(Context context, Bitstream bitstream, FormatFilter formatFilter,
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

    protected void clonePoliciesFromOriginalBundle(
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

    protected void addToBundlePolicies(
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
