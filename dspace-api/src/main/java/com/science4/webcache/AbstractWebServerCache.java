/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package com.science4.webcache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.dspace.app.customurl.CustomUrlService;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;

public abstract class AbstractWebServerCache implements WebServerCache {
    private String baseURL;

    private ConfigurationService configurationService;
    private CustomUrlService customUrlService;

    public void initialize() {
        baseURL = configurationService.getProperty("dspace.ui.url");
    }

    public Collection<? extends String> getURLsToCache(Context ctx, DSpaceObject subject) {
        List<String> urls = new ArrayList<String>();
        if (subject instanceof Community || subject instanceof Collection) {
            urls.add(handleUrl(subject.getHandle()));
        } else if (subject instanceof Item) {
            customUrlService.getCustomUrl((Item) subject).ifPresentOrElse(url -> urls.add(url),
                    () -> urls.add(itemUuid(subject.getID())));
        }
        return urls;
    }

    private String itemUuid(UUID id) {
        return new StringBuilder(baseURL).append("/items/").append(id.toString()).toString();
    }

    private String handleUrl(String handle) {
        return new StringBuilder(baseURL).append("/handle/").append(handle).toString();
    }

    public Collection<? extends String> getURLsToDontCache(Context ctx, DSpaceObject subject) {
        List<String> urls = new ArrayList<String>();
        if (subject instanceof Item) {
            Item item = (Item) subject;
            Optional<String> customUrl = customUrlService.getCustomUrl(item);
            customUrl.ifPresent(url -> {
                urls.add(itemUuid(subject.getID()));
                urls.addAll(customUrlService.getOldCustomUrls(item));
            });
        }
        return urls;
    }

    public Collection<? extends String> getURLsEventuallyInCacheForDeletedObject(Context ctx, int subjectType,
            UUID subjectID, String handle, List<String> identifiers) {
        List<String> urls = new ArrayList<String>();
        switch (subjectType) {
            case Constants.COMMUNITY:
            case Constants.COLLECTION:
                urls.add(handleUrl(handle));
                break;
            case Constants.ITEM:
                identifiers.stream().filter(i -> i.startsWith("customurl:")).findFirst().ifPresentOrElse(
                        url -> urls.add(url.substring("customurl:".length())), () -> urls.add(itemUuid(subjectID)));
                break;
            default:
                break;
        }
        return urls;
    }

}
