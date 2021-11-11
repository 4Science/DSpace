/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.embeddable.impl;

import java.util.List;

import com.google.gson.JsonObject;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Provider class for plumX metric widget
 *
 * @author Alba Aliu (alba.aliu at atis.al)
 */
public class EmbeddablePlumXMetricProvider extends AbstractEmbeddableMetricProvider {
    /**
     * Script to render widget for persons
     */
    private final String personPlumXScript = "//cdn.plu.mx/widget-person.js";
    /**
     * Script to render widget for publication
     */
    private final String publicationPlumXScript = "//cdn.plu.mx/widget-popup.js";
    /**
     * Href link for publication researches
     */
    private static final String publicationHref = "https://plu.mx/plum/a/";
    /**
     * Href link for persons
     */
    private static final String personHref = "https://plu.mx/plum/u/";

    @Autowired
    private ItemService itemService;
    String doiIdentifier;
    String orcid;

    @Override
    public boolean hasMetric(Context context, Item item, List<CrisMetrics> retrivedStoredMetrics) {
        String entityType = getEntityType(item);
        if (entityType != null) {
            if (entityType.equals("Person")) {
                // if it is of type person use orcid
                orcid = getItemService()
                        .getMetadataFirstValue(item, "person", "identifier", "orcid", Item.ANY);
                if (orcid != null) {
                    return true;
                } else {
                    return false;
                }
            } else {
                // if it is of type publication use doi
                if (entityType.equals("Publication")) {
                    doiIdentifier = getItemService()
                            .getMetadataFirstValue(item, "dc", "identifier", "doi", Item.ANY);
                    if (doiIdentifier != null) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        } else {
            return false;
        }
        return false;
    }

    @Override
    public String innerHtml(Context context, Item item) {
        JsonObject innerHtml = new JsonObject();
        String entityType = getEntityType(item);
        innerHtml.addProperty("type", entityType);
        if (entityType.equals("Person")) {
            innerHtml.addProperty("src", personPlumXScript);
            innerHtml.addProperty("href", personHref + "?orcid=" + orcid);
        } else {
            innerHtml.addProperty("src", publicationPlumXScript);
            innerHtml.addProperty("href", publicationHref + "?doi=" + doiIdentifier);
        }
        return innerHtml.toString();
    }

    @Override
    public String getMetricType() {
        return "plumX";
    }

    protected String getEntityType(Item item) {
        return getItemService().getMetadataFirstValue(item,
                "dspace", "entity", "type", Item.ANY);
    }

    protected ItemService getItemService() {
        return itemService;
    }
}
