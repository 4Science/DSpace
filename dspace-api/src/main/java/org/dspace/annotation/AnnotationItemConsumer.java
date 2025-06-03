/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.annotation;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchUtils;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Consumer that will be evaluated when a metadata of an annotation is modified
 * It will update the glam.annotation.fulltext with the dc.description.abstract just modified.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class AnnotationItemConsumer implements Consumer {

    ItemService itemService;
    SearchService searchService;
    Map<Integer, Set<Action>> itemsProcessed = new HashMap<>();

    Set<Integer> events = Set.of(Event.MODIFY_METADATA);

    @Override
    public void initialize() throws Exception {
        itemService = ContentServiceFactory.getInstance().getItemService();
        searchService = SearchUtils.getSearchService();
    }

    public static UnaryOperator<String> getAnnotationTitleReducer(ConfigurationService configurationService) {
        int length = configurationService.getIntProperty("annotation.dc.title.length", 20);
        return (s) -> s.length() > length ? s.substring(0, length) + "..." : s;
    }

    public static String getAnnotationTitle(ConfigurationService configurationService, String metadataValue) {
        return getAnnotationTitleReducer(configurationService).apply(metadataValue);
    }

    @Override
    public void consume(Context ctx, Event event) throws Exception {

        if (!events.contains(event.getEventType())) {
            return;
        }

        DSpaceObject subject = event.getSubject(ctx);
        if (subject == null) {
            return;
        }

        if (!(subject instanceof Item)) {
            return;
        }

        Item item = (Item) subject;
        if (!"WebAnnotation".equals(itemService.getMetadata(item, "dspace.entity.type"))) {
            return;
        }
        if (event.getEventType() == Event.MODIFY_METADATA) {

            String detail = event.getDetail();
            if (detail == null) {
                return;
            }

            if (!detail.contains("dc_description_abstract")) {
                return;
            }

            String description = itemService.getMetadata(item, "dc.description.abstract");
            if (description == null || description.isEmpty()) {
                return;
            }
            description = description.replaceAll("<[ /]*[a-zA-Z0-9 ]*[ /]*>", "");
            String fullText = itemService.getMetadata(item, "glam.annotation.fulltext");

            if (StringUtils.equals(description, fullText)) {
                return;
            }
            Set<Action> modifiedItems = itemsProcessed.get(Event.MODIFY_METADATA);
            if (modifiedItems == null) {
                modifiedItems = new HashSet<>();
                itemsProcessed.put(Event.MODIFY_METADATA, modifiedItems);
            }
            ModifiedAction modifiedAction = new ModifiedAction(item.getID(), description);
            if (modifiedItems.contains(modifiedAction)) {
                return;
            }
            modifiedAction.accept(ctx);
            modifiedItems.add(modifiedAction);
        }
    }

    @Override
    public void end(Context ctx) throws Exception {
        itemsProcessed.clear();
    }

    @Override
    public void finish(Context ctx) throws Exception {

    }

    static abstract class Action implements java.util.function.Consumer<Context> {
        final int eventType;

        Action(int eventType) {
            this.eventType = eventType;
        }

    }

    static class ModifiedAction extends Action {

        private final String metadatavalue;
        private final UUID itemId;

        ModifiedAction(UUID itemId, String metadatavalue) {
            super(Event.MODIFY_METADATA);
            this.itemId = itemId;
            this.metadatavalue = metadatavalue;
        }

        @Override
        public void accept(Context context) {
            ItemService itemService = ContentServiceFactory.getInstance().getItemService();
            try {
                Item item = itemService.find(context, itemId);
                if (item == null) {
                    return;
                }
                itemService.setMetadataSingleValue(
                    context,
                    item,
                    "glam",
                    "annotation",
                    "fulltext",
                    null,
                    metadatavalue
                );
                itemService.setMetadataSingleValue(
                    context,
                    item,
                    "dc",
                    "title",
                    null,
                    null,
                    getAnnotationTitle(
                        DSpaceServicesFactory.getInstance().getConfigurationService(),
                        metadatavalue
                    )
                );
                itemService.update(context, item);
            } catch (SQLException | AuthorizeException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ModifiedAction that = (ModifiedAction) o;
            return Objects.equals(itemId, that.itemId);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(itemId);
        }
    }

}
