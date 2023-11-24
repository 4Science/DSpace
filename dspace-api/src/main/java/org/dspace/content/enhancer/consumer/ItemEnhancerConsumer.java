/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.consumer;

import java.util.HashSet;
import java.util.Set;

import org.dspace.content.Item;
import org.dspace.content.enhancer.service.ItemEnhancerService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link Consumer} that force the item enhancement on the
 * item subject of the event, if any.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ItemEnhancerConsumer implements Consumer {

    public static final String ITEMENHANCER_ENABLED = "itemenhancer.enabled";
    private Set<Item> itemsAlreadyProcessed = new HashSet<Item>();

    private ItemEnhancerService itemEnhancerService;

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Override
    public void finish(Context ctx) throws Exception {

    }

    @Override
    public void initialize() throws Exception {
        itemEnhancerService = new DSpace().getSingletonService(ItemEnhancerService.class);
    }

    @Override
    public void consume(Context context, Event event) throws Exception {

        if (!isConsumerEnabled()) {
            return;
        }

        Item item = (Item) event.getSubject(context);
        if (item == null || itemsAlreadyProcessed.contains(item) || !item.isArchived()) {
            return;
        }

        itemsAlreadyProcessed.add(item);

        context.turnOffAuthorisationSystem();
        try {
            itemEnhancerService.enhance(context, item);
        } finally {
            context.restoreAuthSystemState();
        }

    }

    protected boolean isConsumerEnabled() {
        return configurationService.getBooleanProperty(ITEMENHANCER_ENABLED, true);
    }

    @Override
    public void end(Context ctx) throws Exception {
        itemsAlreadyProcessed.clear();
    }

}
