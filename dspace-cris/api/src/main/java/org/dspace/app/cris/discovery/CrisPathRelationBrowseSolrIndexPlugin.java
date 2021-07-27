/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.discovery;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.model.RelationPreference;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.sort.OrderFormat;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;

public class CrisPathRelationBrowseSolrIndexPlugin implements SolrServiceIndexPlugin {

    private static final Logger log = Logger
            .getLogger(CrisPathRelationBrowseSolrIndexPlugin.class);

    private ApplicationService applicationService;

    @Override
    public void additionalIndex(Context context, DSpaceObject dso, SolrInputDocument doc,
            Map<String, List<DiscoverySearchFilter>> searchFilters) {
        if (!(dso instanceof Item)) {
            return;
        }
        Item item = (Item) dso;

        // add sorting option for cris path relation
        try {
            for (SortOption so : SortOption.getSortOptions()) {
                if (so.getName().equals("relationcrispath")) {
                    String value = "";
                    // retrieve relations
                    List<RelationPreference> preferences = applicationService
                            .findRelationsPreferencesForItemID(item.getID());
                    if (preferences != null && !preferences.isEmpty()) {
                        value = "0";
                        for (RelationPreference rp : preferences) {
                            if (rp.getRelationType().equals("crispath.publications")) {
                                // retrieve relation priority
                                value = String.valueOf(rp.getPriority());
                                break;
                            }
                        }
                    }
                    if (StringUtils.isNotBlank(value)) {
                        // add sorting option
                        String nValue = OrderFormat
                                .makeSortString(value, "en", so.getType());
                        // NOTE: use integer field to use numerical order instead of lexicographic order
                        doc.addField("bi_sort_" + so.getNumber() + "_sint", nValue);
                    }
                }
            }
        } catch (SortException e) {
            // we can't solve it so rethrow as runtime exception
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }
}
