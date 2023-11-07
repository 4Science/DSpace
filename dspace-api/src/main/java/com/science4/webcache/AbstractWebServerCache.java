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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dspace.app.customurl.CustomUrlService;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

public abstract class AbstractWebServerCache implements WebServerCache {
    private String baseURL;

    private ConfigurationService configurationService;
    private CustomUrlService customUrlService;
    private ItemService itemService;

    public void initialize() {
        itemService = ContentServiceFactory.getInstance().getItemService();
        customUrlService = new DSpace().getSingletonService(CustomUrlService.class);
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
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
                urls.addAll(getItemUrls(item));
            }
        }
        return urls;
    }

    @Override
    public Collection<? extends String> getURLsEventuallyInCacheForDeletedObject(Context ctx, int subjectType,
                                                                                 UUID subjectID, String handle, List<String> identifiers, List<MetadataValueDTO> metadataValues) {
        switch (subjectType) {
            case Constants.COMMUNITY:
                return getCommunityUrls(subjectID, handle);
            case Constants.COLLECTION:
                return getCollectionUrls(subjectID, handle);
            case Constants.ITEM:
                return getItemUrls(subjectID, handle, identifiers, metadataValues);
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

    private List<String> getItemUrls(UUID itemId, String handle, List<String> identifiers,
            List<MetadataValueDTO> metadataValues) {
        List<String> urls = new ArrayList<>();

        metadataValues.stream()
                .filter(metadataValue -> matchMetadataValue(metadataValue, "dspace", "entity", "type"))
                .map(MetadataValueDTO::getValue)
                .findFirst().ifPresent(entityType -> {
                    String url = baseURL + "/entities/" + entityType.toLowerCase() + "/" + itemId.toString();
                    urls.add(url);
                });

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

    private List<String> getItemUrls(Item item) {
        List<String> urls = new ArrayList<>();

        String entityType = itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY);
        if (isNotBlank(entityType)) {
            String url = baseURL + "/entities/" + entityType.toLowerCase() + "/" + item.getID().toString();
            urls.add(url);
        }

        if (isNotBlank(item.getHandle())) {
            urls.add(handleUrl(item.getHandle()));
        }
        urls.add(baseURL + "/items/" + item.getID().toString());
        customUrlService.getCustomUrl(item).ifPresent(urls::add);
        urls.addAll(customUrlService.getOldCustomUrls(item));
        return urls;
    }

    private String handleUrl(String handle) {
        return new StringBuilder(baseURL).append("/handle/").append(handle).toString();
    }

    private boolean matchMetadataValue(MetadataValueDTO metadataValue, String schema,
                                       String element, String qualifier) {
        return Objects.equals(metadataValue.getSchema(), schema)
                && Objects.equals(metadataValue.getElement(), element)
                && Objects.equals(metadataValue.getQualifier(), qualifier);
    }
}
