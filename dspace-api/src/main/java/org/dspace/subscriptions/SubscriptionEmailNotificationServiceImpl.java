/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions;

import static org.dspace.content.Item.ANY;
import static org.dspace.core.Constants.COLLECTION;
import static org.dspace.core.Constants.COMMUNITY;
import static org.dspace.core.Constants.ITEM;
import static org.dspace.core.Constants.READ;
import static org.dspace.subscriptions.SubscriptionItem.fromItem;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.service.SubscribeService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.subscriptions.service.DSpaceObjectUpdates;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link DSpaceRunnable} to find subscribed objects and send notification mails about them
 *
 * @author alba aliu
 */
public class SubscriptionEmailNotificationServiceImpl implements SubscriptionEmailNotificationService {

    private static final Logger log = LogManager.getLogger(SubscriptionEmailNotificationServiceImpl.class);

    private final Map<String, DSpaceObjectUpdates> contentUpdates;
    private final ContentGenerator contentGenerator;
    private final StatisticsGenerator statisticsGenerator;
    private final List<String> supportedSubscriptionTypes;

    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private SubscribeService subscribeService;
    @Autowired
    private CrisMetricsService crisMetricsService;

    public SubscriptionEmailNotificationServiceImpl(Map<String, DSpaceObjectUpdates> contentUpdates,
                                                    ContentGenerator contentGenerator,
                                                    StatisticsGenerator statisticsGenerator,
                                                    List<String> supportedSubscriptionTypes) {
        this.contentUpdates = contentUpdates;
        this.contentGenerator = contentGenerator;
        this.statisticsGenerator = statisticsGenerator;
        this.supportedSubscriptionTypes = supportedSubscriptionTypes;
    }

    public void perform(Context context, DSpaceRunnableHandler handler, String subscriptionType, String frequency) {
        // Verify if subscriptionType is "content" or "subscription"
        if (supportedSubscriptionTypes.get(0).equals(subscriptionType)) {
            performForContent(context, handler, subscriptionType, frequency);
        } else if (supportedSubscriptionTypes.get(1).equals(subscriptionType)) {
            performForStatistics(context, subscriptionType, frequency);
        } else {
            throw new IllegalArgumentException(
                "Currently this SubscriptionType:" + subscriptionType + " is not supported!");
        }
    }

    @SuppressWarnings({ "rawtypes" })
    private void performForContent(Context context, DSpaceRunnableHandler handler,
                                   String subscriptionType, String frequency) {
        try {
            List<Subscription> subscriptions =
                findAllSubscriptionsBySubscriptionTypeAndFrequency(context, subscriptionType, frequency);
            List<SubscriptionItem> communityItems = new ArrayList<>();
            List<SubscriptionItem> collectionsItems = new ArrayList<>();
            Map<String, List<SubscriptionItem>> entityItemsByEntityType = new HashMap<>();
            int iterator = 0;

            for (Subscription subscription : subscriptions) {
                DSpaceObject dSpaceObject = subscription.getDSpaceObject();
                EPerson ePerson = subscription.getEPerson();

                if (!authorizeService.authorizeActionBoolean(context, ePerson, dSpaceObject, READ, true)) {
                    iterator++;
                    continue;
                }

                switch (dSpaceObject.getType()) {
                    case COMMUNITY:
                        List<IndexableObject> indexableCommunityItems = getItems(
                            context, ePerson,
                            contentUpdates.get(Community.class.getSimpleName().toLowerCase())
                                          .findUpdates(context, dSpaceObject, frequency)
                        );
                        communityItems.add(fromItem(dSpaceObject, indexableCommunityItems));
                        break;
                    case COLLECTION:
                        List<IndexableObject> indexableCollectionItems = getItems(
                            context, ePerson,
                            contentUpdates.get(Collection.class.getSimpleName().toLowerCase())
                                          .findUpdates(context, dSpaceObject, frequency)
                        );
                        collectionsItems.add(fromItem(dSpaceObject, indexableCollectionItems));
                        break;
                    case ITEM:
                        List<IndexableObject> indexableEntityItems = getItems(
                            context, ePerson, contentUpdates.get(Item.class.getSimpleName().toLowerCase())
                                                            .findUpdates(context, dSpaceObject, frequency)
                        );
                        String dspaceType = ContentServiceFactory
                            .getInstance().getDSpaceObjectService(dSpaceObject)
                            .getMetadataFirstValue(dSpaceObject, "dspace", "entity", "type", ANY);

                        entityItemsByEntityType.computeIfAbsent(dspaceType, k -> new ArrayList<>())
                                               .add(fromItem(dSpaceObject, indexableEntityItems));
                        break;
                    default:
                        log.warn("found an invalid DSpace Object type ({}) among subscriptions to send",
                                 dSpaceObject.getType());
                        continue;
                }

                if (iterator < subscriptions.size() - 1) {
                    // as the subscriptions are ordered by eperson id, so we send them by ePerson
                    if (ePerson.equals(subscriptions.get(iterator + 1).getEPerson())) {
                        iterator++;
                        continue;
                    } else {
                        contentGenerator.notifyForSubscriptions(
                            ePerson, communityItems, collectionsItems, entityItemsByEntityType
                        );
                        communityItems.clear();
                        collectionsItems.clear();
                        entityItemsByEntityType.clear();
                    }
                } else {
                    //in the end of the iteration
                    contentGenerator.notifyForSubscriptions(
                        ePerson, communityItems, collectionsItems, entityItemsByEntityType
                    );
                }
                iterator++;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handler.handleException(e);
            context.abort();
        }
    }

    private void performForStatistics(Context context, String subscriptionType, String frequency) {
        List<Subscription> subscriptions =
            findAllSubscriptionsBySubscriptionTypeAndFrequency(context, subscriptionType, frequency);
        List<CrisMetrics> crisMetricsList = new ArrayList<>();
        int iterator = 0;

        for (Subscription subscription : subscriptions) {
            EPerson ePerson = subscription.getEPerson();
            DSpaceObject dSpaceObject = subscription.getDSpaceObject();
            try {
                crisMetricsList.addAll(crisMetricsService.findAllByDSO(context, dSpaceObject));
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            if (iterator < subscriptions.size() - 1) {
                if (ePerson.equals(subscriptions.get(iterator + 1).getEPerson())) {
                    iterator++;
                    continue;
                } else {
                    statisticsGenerator.notifyForSubscriptions(context, ePerson, crisMetricsList);
                }
            } else {
                //in the end of the iteration
                statisticsGenerator.notifyForSubscriptions(context, ePerson, crisMetricsList);
            }
            iterator++;
        }
    }

    @SuppressWarnings("rawtypes")
    private List<IndexableObject> getItems(Context context, EPerson ePerson, List<IndexableObject> indexableItems)
            throws SQLException {
        List<IndexableObject> items = new ArrayList<IndexableObject>();
        for (IndexableObject indexableItem : indexableItems) {
            Item item = (Item) indexableItem.getIndexedObject();
            if (authorizeService.authorizeActionBoolean(context, ePerson, item, READ, true)) {
                items.add(indexableItem);
            }
        }
        return items;
    }

    /**
     * Return all Subscriptions by subscriptionType and frequency ordered by ePerson ID
     * if there are none it returns an empty list
     * 
     * @param context            DSpace context
     * @param subscriptionType   Could be "content" or "statistics". NOTE: in DSpace we have only "content"
     * @param frequency          Could be "D" stand for Day, "W" stand for Week, and "M" stand for Month
     * @return                   List of subscriptions
     */
    private List<Subscription> findAllSubscriptionsBySubscriptionTypeAndFrequency(Context context,
             String subscriptionType, String frequency) {
        try {
            return subscribeService.findAllSubscriptionsBySubscriptionTypeAndFrequency(context, subscriptionType,
                                                                                       frequency)
                                   .stream()
                                   .sorted(Comparator.comparing(s -> s.getEPerson().getID()))
                                   .collect(Collectors.toList());
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    @Override
    public List<String> getSupportedSubscriptionTypes() {
        return supportedSubscriptionTypes;
    }

}
