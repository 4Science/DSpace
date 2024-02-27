/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package com.science4.webcache;

import static java.util.Objects.nonNull;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Class for invalidate stale content in the web cache and trigger the creation of an updated version
 *
 * @author Andrea Bollini (andrea.bollini at 4science.com)
 */
public class StaleWebDataConsumer implements Consumer {
    /**
     * log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(StaleWebDataConsumer.class);

    // collect the set of url that need to be regenerated
    private Set<String> urlsToUpdate = new HashSet<>();
    // collect the set of url that need to be invalidated without forcing a new caching
    private Set<String> urlsToRemove = new HashSet<>();

    private final WebServerCache webServerCache = DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName(WebServerCache.class.getName(),
                              WebServerCache.class);

    private final AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    private final GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    @Override
    public void initialize() throws Exception {

    }

    /**
     * Consume a content event -- just build the sets of url to regenerate or invalidate
     * @param ctx   DSpace context
     * @param event Content event
     */
    @Override
    public void consume(Context ctx, Event event) throws Exception {
        if (webServerCache == null) {
            log.warn("No webcache implementation found, consider to remove the webcache consumer if not needed");
        }
        if (urlsToUpdate == null) {
            urlsToUpdate = new HashSet<>();
            urlsToRemove = new HashSet<>();
        }

        int st = event.getSubjectType();
        if (!(st == Constants.ITEM || st == Constants.BUNDLE
            || st == Constants.COLLECTION || st == Constants.COMMUNITY || st == Constants.SITE)) {
            log
                .warn("IndexConsumer should not have been given this kind of Subject in an event, skipping: "
                          + event.toString());
            return;
        }

        DSpaceObject subject = event.getSubject(ctx);

        DSpaceObject object = event.getObject(ctx);


        // If event subject is a Bundle and event was Add or Remove,
        // transform the event to be a Modify on the owning Item.
        // It could be a new bitstream in the TEXT bundle which
        // would change the index.
        int et = event.getEventType();
        if (st == Constants.BUNDLE) {
            if ((et == Event.ADD || et == Event.REMOVE) && subject != null
                && ((Bundle) subject).getName().equals("TEXT")) {
                st = Constants.ITEM;
                et = Event.MODIFY;
                subject = ((Bundle) subject).getItems().get(0);
                if (log.isDebugEnabled()) {
                    log.debug("Transforming Bundle event into MODIFY of Item "
                                  + subject.getHandle());
                }
            } else {
                return;
            }
        }

        switch (et) {
            case Event.CREATE:
            case Event.MODIFY:
            case Event.MODIFY_METADATA:
                if (subject != null) {
                    updateUrlLists(ctx, subject);
                }
                break;

            case Event.REMOVE:
            case Event.ADD:
            case Event.INSTALL:
                if (subject != null && st == Constants.ITEM) {
                    updateUrlLists(ctx, subject);
                }
                break;

            case Event.DELETE:
                if (event.getSubjectType() == -1 || event.getSubjectID() == null) {
                    log.warn("got null subject type and/or ID on DELETE event, skipping it.");
                } else {
                    urlsToRemove.addAll(webServerCache.getURLsEventuallyInCacheForDeletedObject(ctx,
                            event.getSubjectType(), event.getSubjectID(), event.getDetail(), event.getIdentifiers()));
                }
                break;
            default:
                log
                    .warn("StaleWebDataConsumer should not have been given a event of type="
                              + event.getEventTypeAsString()
                              + " on subject="
                              + event.getSubjectTypeAsString());
                break;
        }
    }

    /**
     * Process sets of objects to add, update, and delete in index. Correct for
     * interactions between the sets -- e.g. objects which were deleted do not
     * need to be added or updated, new objects don't also need an update, etc.
     */
    @Override
    public void end(Context ctx) throws Exception {

        try {
            webServerCache.invalidateAndRenew(ctx, urlsToUpdate, urlsToRemove);
        } finally {
            if (!urlsToUpdate.isEmpty() || !urlsToRemove.isEmpty()) {
                // "free" the resources
                urlsToUpdate.clear();
                urlsToRemove.clear();
            }
        }
    }

    @Override
    public void finish(Context ctx) throws Exception {
        // No-op

    }

    private boolean isSubjectPublic(Context ctx, DSpaceObject subject) throws SQLException {
        Group anonymousGroup = groupService.findByName(ctx, Group.ANONYMOUS);
        ResourcePolicy anonymousRead
                = authorizeService.findByTypeGroupAction(ctx, subject, anonymousGroup, Constants.READ);
        return nonNull(anonymousRead);
    }

    private void updateUrlLists(Context ctx, DSpaceObject subject) throws SQLException {
        if (isSubjectPublic(ctx, subject)) {
            urlsToUpdate.addAll(webServerCache.getURLsToCache(ctx, subject));
            urlsToRemove.addAll(webServerCache.getURLsToDontCache(ctx, subject));
        } else {
            urlsToRemove.addAll(webServerCache.getAllURLs(ctx, subject));
        }
    }
}
