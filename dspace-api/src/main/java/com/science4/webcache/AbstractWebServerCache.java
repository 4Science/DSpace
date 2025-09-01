/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package com.science4.webcache;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dspace.app.customurl.CustomUrlService;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.factory.CrisLayoutServiceFactory;
import org.dspace.layout.service.CrisLayoutTabService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

public abstract class AbstractWebServerCache implements WebServerCache {
    private String baseURL;

    private ConfigurationService configurationService;
    private CustomUrlService customUrlService;
    private ItemService itemService;
    private CrisLayoutTabService crisLayoutTabService;

    public void initialize() {
        itemService = ContentServiceFactory.getInstance().getItemService();
        customUrlService = new DSpace().getSingletonService(CustomUrlService.class);
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        crisLayoutTabService = CrisLayoutServiceFactory.getInstance().getTabService();
        baseURL = configurationService.getProperty("dspace.ui.url");
    }

    @Override
    public Collection<? extends String> getURLsToCache(Context ctx, DSpaceObject subject) {
        List<String> allUrls = getAllURLs(ctx, subject);
        return !allUrls.isEmpty() ? allUrls.subList(0, 1) : allUrls;
    }

    @Override
    public Collection<? extends String> getURLsToDontCache(Context ctx, DSpaceObject subject) {
        List<String> allUrls = getAllURLs(ctx, subject);
        return allUrls.size() > 1 ? allUrls.subList(1, allUrls.size()) : Collections.emptyList();
    }

    @Override
    public List<String> getAllURLs(Context context, DSpaceObject subject) {
        List<String> urls = new ArrayList<>();
        if (subject instanceof Community) {
            urls.addAll(getCommunityUrls(subject.getID(), subject.getHandle()));
        } else if (subject instanceof org.dspace.content.Collection) {
            urls.addAll(getCollectionUrls(subject.getID(), subject.getHandle()));
        } else if (subject instanceof Item) {
            Item item = (Item) subject;
            if (item.isArchived() || item.isWithdrawn()) {
                urls.addAll(getItemUrls(item, context));
            }
        }
        return urls;
    }

    @Override
    public Collection<? extends String> getURLsEventuallyInCacheForDeletedObject(
            Context ctx,
            int subjectType,
            UUID subjectID,
            String handle,
            List<String> identifiers
    ) {
        switch (subjectType) {
            case Constants.COMMUNITY:
                return getCommunityUrls(subjectID, handle);
            case Constants.COLLECTION:
                return getCollectionUrls(subjectID, handle);
            case Constants.ITEM:
                return getItemUrls(subjectID, ctx, handle, identifiers);
            default:
                return Collections.emptyList();
        }
    }

    private List<String> getCollectionUrls(UUID collectionId, String handle) {
        ArrayList<String> urls = new ArrayList<>();
        if (isNotBlank(handle)) {
            urls.add(handleUrl(handle));
        }
        urls.add(baseURL + "/collections/" + collectionId.toString());
        return urls;
    }

    private List<String> getCommunityUrls(UUID communityId, String handle) {
        ArrayList<String> urls = new ArrayList<>();
        if (isNotBlank(handle)) {
            urls.add(handleUrl(handle));
        }
        urls.add(baseURL + "/communities/" + communityId.toString());
        return urls;
    }

    private List<String> getItemUrls(UUID itemId, Context ctx, String handle, List<String> identifiers) {
        List<String> urls = new ArrayList<>();

        String[] entityTypes = configurationService.getArrayProperty("cris.entity-type");
        if (nonNull(entityTypes)) {
            for (String entityType : entityTypes) {
                urls.add(baseURL + "/entities/" + entityType.toLowerCase() + "/" + itemId.toString());
                addFirstTabUrl(urls, ctx, itemId.toString(), entityType);
            }
        }

        if (isNotBlank(handle)) {
            urls.add(handleUrl(handle));
        }
        urls.add(baseURL + "/items/" + itemId.toString());

        if (nonNull(identifiers)) {
            List<String> urlsFromIdentifiers = identifiers.stream()
                    .filter(i -> i.startsWith("customurl:"))
                    .map(url -> url.substring("customurl:".length()))
                    .collect(Collectors.toList());
            urls.addAll(urlsFromIdentifiers);
        }
        return urls;
    }

    private List<String> getItemUrls(Item item, Context ctx) {
        List<String> urls = new ArrayList<>();

        String entityType = itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY);
        if (isNotBlank(entityType)) {
            String itemId = item.getID().toString();
            urls.add(baseURL + "/entities/" + entityType.toLowerCase() + "/" + itemId);
            addFirstTabUrl(urls, ctx, itemId, entityType);
        }

        if (isNotBlank(item.getHandle())) {
            urls.add(handleUrl(item.getHandle()));
        }
        urls.add(baseURL + "/items/" + item.getID().toString());
        customUrlService.getCustomUrl(item).ifPresent(urls::add);
        urls.addAll(customUrlService.getOldCustomUrls(item));
        return urls;
    }

    private void addFirstTabUrl(List<String> urls, Context ctx, String itemId, String entityType) {
        try {
            CrisLayoutTab firstTab = crisLayoutTabService.findByItem(ctx, itemId).get(0);
            urls.add(baseURL + "/entities/" + entityType.toLowerCase() + "/" + itemId + "/" + firstTab.getID());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String handleUrl(String handle) {
        return new StringBuilder(baseURL).append("/handle/").append(handle).toString();
    }

}
