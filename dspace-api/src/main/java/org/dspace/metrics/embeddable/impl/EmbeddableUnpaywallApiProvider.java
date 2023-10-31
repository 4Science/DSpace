/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.embeddable.impl;

import java.util.Optional;

import com.google.gson.JsonObject;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.unpaywall.service.UnpaywallService;
import org.dspace.util.FrontendUrlService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class to provide Unpaywal api metrics.
 */
public class EmbeddableUnpaywallApiProvider extends AbstractEmbeddableMetricProvider {

    private final static String UNPAYWALL_VERSIONS_PATH = "unpaywall/versions?autoForward=true";

    @Autowired
    private UnpaywallService unpaywallService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private FrontendUrlService frontendUrlService;

    @Override
    public String getMetricType() {
        return "unpaywall-api";
    }

    @Override
    public String innerHtml(Context context, Item item) {
        String itemUrl = frontendUrlService.generateUrl(context, item);
        JsonObject json = new JsonObject();
        json.addProperty("link", itemUrl + "/" + UNPAYWALL_VERSIONS_PATH);
        return json.toString();
    }

    @Override
    public Optional<Double> getMetricCount(Context context, Item item) {
        int versionsCount = unpaywallService.getItemVersions(context, item).size();
        return Optional.of((double) versionsCount);
    }
}
