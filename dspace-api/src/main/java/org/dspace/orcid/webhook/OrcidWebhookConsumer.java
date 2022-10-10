/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.webhook;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.dspace.orcid.webhook.OrcidWebhookMode.ALL;
import static org.dspace.orcid.webhook.OrcidWebhookMode.DISABLED;

import java.util.HashSet;
import java.util.Set;

import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.orcid.factory.OrcidServiceFactory;
import org.dspace.orcid.service.OrcidTokenService;
import org.dspace.orcid.service.OrcidWebhookService;
import org.dspace.profile.service.ResearcherProfileService;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link Consumer} that perform registrations and
 * unregistration from ORCID webhook.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidWebhookConsumer implements Consumer {

    private Set<Item> itemsAlreadyProcessed = new HashSet<Item>();

    private ItemService itemService;

    private OrcidWebhookService orcidWebhookService;

    private OrcidTokenService orcidTokenService;

    private ResearcherProfileService researcherProfileService;

    @Override
    public void initialize() throws Exception {
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.researcherProfileService = new DSpace().getSingletonService(ResearcherProfileService.class);
        this.orcidWebhookService = OrcidServiceFactory.getInstance().getOrcidWebhookService();
        this.orcidTokenService = OrcidServiceFactory.getInstance().getOrcidTokenService();
    }

    @Override
    public void finish(Context ctx) throws Exception {

    }

    @Override
    public void consume(Context context, Event event) throws Exception {

        OrcidWebhookMode webhookConfiguration = orcidWebhookService.getOrcidWebhookMode();
        if (webhookConfiguration == DISABLED) {
            return;
        }

        Item item = (Item) event.getSubject(context);
        if (item == null || itemsAlreadyProcessed.contains(item) || !item.isArchived()) {
            return;
        }

        itemsAlreadyProcessed.add(item);

        if (isNotProfile(item)) {
            return;
        }

        boolean isWebhookAlreadyRegistered = orcidWebhookService.isProfileRegistered(item);
        boolean hasRequiredOrcidMetadata = hasRequiredOrcidMetadata(context, item, webhookConfiguration);

        if (!isWebhookAlreadyRegistered && hasRequiredOrcidMetadata) {
            orcidWebhookService.register(context, item);
        }

    }

    private boolean isNotProfile(Item item) {
        return !researcherProfileService.getProfileType().equals(itemService.getEntityTypeLabel(item));
    }

    private boolean hasRequiredOrcidMetadata(Context context, Item item, OrcidWebhookMode webhookConfiguration) {
        boolean hasOrcidId = isNotBlank(getMetadataFirstValue(item, "person.identifier.orcid"));
        return webhookConfiguration == ALL ? hasOrcidId : hasOrcidId && hasOrcidAccessToken(context, item);
    }

    private boolean hasOrcidAccessToken(Context context, Item item) {
        return orcidTokenService.findByProfileItem(context, item) != null;
    }

    private String getMetadataFirstValue(Item item, String metadataField) {
        return itemService.getMetadataFirstValue(item, new MetadataFieldName(metadataField), Item.ANY);
    }

    @Override
    public void end(Context ctx) throws Exception {
        itemsAlreadyProcessed.clear();
    }

}
