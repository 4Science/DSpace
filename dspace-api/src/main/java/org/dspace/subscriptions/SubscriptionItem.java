/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions;

import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.discovery.IndexableObject;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

public class SubscriptionItem {

    private static final ConfigurationService configurationService = DSpaceServicesFactory.getInstance()
                                                                                          .getConfigurationService();

    private String name;
    private String url;
    private Map<String, String> itemUrlsByItemName;

    public SubscriptionItem(String name, String url, Map<String, String> itemUrlsByItemName) {
        this.name = name;
        this.url = url;
        this.itemUrlsByItemName = itemUrlsByItemName;
    }

    @SuppressWarnings({ "rawtypes" })
    static SubscriptionItem fromItem(DSpaceObject dSpaceObject, List<IndexableObject> relatedItems) {
        return new SubscriptionItem(
            dSpaceObject.getName(),
            buildUrlForItem(dSpaceObject.getHandle()),
            relatedItems.stream()
                        .map(obj -> (Item) obj.getIndexedObject())
                        .collect(toMap(Item::getName, item -> buildUrlForItem(item.getHandle())))
        );
    }

    private static String buildUrlForItem(String handle) {
        return configurationService.getProperty("dspace.ui.url") + "/handle/" + handle;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getItemUrlsByItemName() {
        return itemUrlsByItemName;
    }

    public void setItemUrlsByItemName(Map<String, String> itemUrlsByItemName) {
        this.itemUrlsByItemName = itemUrlsByItemName;
    }
}
