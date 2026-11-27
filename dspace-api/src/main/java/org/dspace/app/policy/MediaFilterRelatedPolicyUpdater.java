/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.policy;


import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.mediafilter.FormatFilter;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Policy updater for media filter related bitstreams.
 *
 * This class updates the policies of derivative bitstreams created by media filters.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MediaFilterRelatedPolicyUpdater extends AbstractPolicyUpdater implements PolicyUpdater {

    public static final String MEDIA_FILTER_PLUGINS_KEY = "filter.plugins";

    private static final Log log = LogFactory.getLog(MediaFilterRelatedPolicyUpdater.class);

    protected final ItemService itemService;

    public MediaFilterRelatedPolicyUpdater(
        ItemService itemService,
        PolicyUpdaterService policyUpdaterService
    ) {
        super(policyUpdaterService);
        this.itemService = itemService;
    }

    protected List<FormatFilter> loadFilterClasses(List<String> filterNames) {
        return filterNames.stream()
                          .map(this::loadFormatFilter)
                          .filter(Objects::nonNull)
                          .collect(Collectors.toList());
    }

    protected FormatFilter loadFormatFilter(String filterName) {
        return (FormatFilter) CoreServiceFactory.getInstance().getPluginService()
                                                .getNamedPlugin(FormatFilter.class,
                                                                filterName);
    }

    @Override
    public void updatePolicies(Context context, Item item, Bitstream source) throws SQLException, AuthorizeException {
        if (item == null || source == null) {
            throw new IllegalArgumentException("Cannot update policies on null item/bitstream");
        }

        ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();
        String[] filterNames = configurationService.getArrayProperty(MEDIA_FILTER_PLUGINS_KEY);

        if (filterNames == null || filterNames.length == 0) {
            log.error("Missing filter names/classes!" +
                          "Cannot update policies, missing property on Bean MediaFilterRelatedPolicyUpdater!");
            return;
        }

        List<FormatFilter> formatFilters = loadFilterClasses(Arrays.asList(filterNames));

        if (formatFilters.isEmpty()) {
            log.warn("Cannot find any of the filterClass configured!");
            return;
        }

        for (FormatFilter formatFilter : formatFilters) {
            for (Bitstream bitstream : findDerivativeBitstreams(item, source, formatFilter)) {
                updatePoliciesOfDerivativeBitstream(
                    context, bitstream, formatFilter, source, getPublicFiltersClasses()
                );
            }
        }
    }

    /**
     * find derivative bitstreams related to source bitstream
     *
     * @param item         item containing bitstreams
     * @param source       source bitstream
     * @param formatFilter formatFilter
     * @return list of derivative bitstreams from source bitstream
     * @throws SQLException If something goes wrong in the database
     */
    private List<Bitstream> findDerivativeBitstreams(Item item, Bitstream source, FormatFilter formatFilter)
        throws SQLException {

        String bitstreamName = formatFilter.getFilteredName(source.getName());
        List<Bundle> bundles = itemService.getBundles(item, formatFilter.getBundleName());

        return bundles.stream()
                      .flatMap(bundle -> bundle.getBitstreams().stream())
                      .filter(bitstream -> StringUtils.equals(
                          bitstream.getName().trim(), bitstreamName.trim()
                      ))
                      .collect(Collectors.toList());
    }

}
