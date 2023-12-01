/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.event.ConsumerProfile;
import org.dspace.event.Dispatcher;
import org.dspace.event.factory.EventServiceFactory;
import org.dspace.event.service.EventService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ReciprocalItemAuthorityConsumerIT extends AbstractIntegrationTestWithDatabase {

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private final ConfigurationService configurationService = DSpaceServicesFactory.getInstance()
            .getConfigurationService();

    private final MetadataAuthorityService metadataAuthorityService = ContentAuthorityServiceFactory.getInstance()
            .getMetadataAuthorityService();

    private final EventService eventService = EventServiceFactory.getInstance().getEventService();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("ItemAuthority.reciprocalMetadata.Publication.dc.relation.product",
                "dc.relation.publication");
        configurationService.setProperty("ItemAuthority.reciprocalMetadata.Product.dc.relation.publication",
                "dc.relation.product");
        metadataAuthorityService.clearCache();

        initializeReciprocalConfiguration();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
    }

    @Test
    public void testShouldCreatePublicationMetadataForProductItem() {
        try {
            configurationService.setProperty("authority.controlled.dc.relation.product", "true");
            metadataAuthorityService.clearCache();

            String productTitle = "productTitle";
            Collection productItemCollection = CollectionBuilder.createCollection(context, parentCommunity)
                    .withEntityType("product")
                    .withName("test_collection").build();
            Item productItem = ItemBuilder.createItem(context, productItemCollection)
                    .withPersonIdentifierFirstName("test_first_name")
                    .withPersonIdentifierLastName("test_second_name")
                    .withScopusAuthorIdentifier("test_author_identifier")
                    .withMetadata(MetadataSchemaEnum.DC.getName(), "title", null, productTitle)
                    .withType("product")
                    .build();

            Collection publicationItemCollection = CollectionBuilder.createCollection(context, parentCommunity)
                    .withEntityType("publication")
                    .withName("test_collection").build();
            Item publicationItem = ItemBuilder.createItem(context, publicationItemCollection)
                    .withPersonIdentifierFirstName("test_first_name")
                    .withPersonIdentifierLastName("test_second_name")
                    .withScopusAuthorIdentifier("test_author_identifier")
                    .withMetadata(MetadataSchemaEnum.DC.getName(), "title", null, "publicationTitle")
                    .withMetadata(MetadataSchemaEnum.DC.getName(), "relation",
                            "product", null, productTitle, productItem.getID().toString(), Choices.CF_ACCEPTED)
                    .withType("publication")
                    .build();

            List<MetadataValue> metadataValues = itemService.getMetadataByMetadataString(
                    productItem, "dc.relation.publication");

            Assert.assertEquals(1, metadataValues.size());
            Assert.assertNotNull(metadataValues.get(0));
            Assert.assertEquals(publicationItem.getID().toString(), metadataValues.get(0).getAuthority());
            Assert.assertEquals(publicationItem.getName(), metadataValues.get(0).getValue());

        } finally {
            configurationService.setProperty("authority.controlled.dc.relation.product", "false");
            metadataAuthorityService.clearCache();
        }
    }

    @Test
    public void testShouldCreateProductMetadataForPublicationItem() {
        try {
            configurationService.setProperty("authority.controlled.dc.relation.product", "true");
            metadataAuthorityService.clearCache();

            String publicationTitle = "publicationTitle";
            Collection publicationItemCollection = CollectionBuilder.createCollection(context, parentCommunity)
                    .withEntityType("publication")
                    .withName("test_collection").build();
            Item publicationItem = ItemBuilder.createItem(context, publicationItemCollection)
                    .withPersonIdentifierFirstName("test_first_name")
                    .withPersonIdentifierLastName("test_second_name")
                    .withScopusAuthorIdentifier("test_author_identifier")
                    .withMetadata(MetadataSchemaEnum.DC.getName(), "title", null, publicationTitle)
                    .withType("publication")
                    .build();

            Collection productItemCollection = CollectionBuilder.createCollection(context, parentCommunity)
                    .withEntityType("product")
                    .withName("test_collection").build();
            Item productItem = ItemBuilder.createItem(context, productItemCollection)
                    .withPersonIdentifierFirstName("test_first_name")
                    .withPersonIdentifierLastName("test_second_name")
                    .withScopusAuthorIdentifier("test_author_identifier")
                    .withMetadata(MetadataSchemaEnum.DC.getName(), "title", null, "productTitle")
                    .withMetadata(MetadataSchemaEnum.DC.getName(), "relation", "publication",
                            null, publicationTitle, publicationItem.getID().toString(), Choices.CF_ACCEPTED)
                    .withType("product")
                    .build();

            List<MetadataValue> metadataValues = itemService.getMetadataByMetadataString(
                    publicationItem, "dc.relation.product");

            Assert.assertEquals(1, metadataValues.size());
            Assert.assertNotNull(metadataValues.get(0));
            Assert.assertEquals(productItem.getID().toString(), metadataValues.get(0).getAuthority());
            Assert.assertEquals(productItem.getName(), metadataValues.get(0).getValue());

        } finally {
            configurationService.setProperty("authority.controlled.dc.relation.product", "false");
            metadataAuthorityService.clearCache();
        }
    }

    @Test
    public void testItemMentioningNotExistingAuthorityIsCreated() throws Exception {
        try {
            configurationService.setProperty("authority.controlled.dc.relation.product", "true");
            metadataAuthorityService.clearCache();

            UUID notExistingItemId = UUID.fromString("803762b5-6f73-4870-b941-adf3c5626f04");
            Collection publicationItemCollection = CollectionBuilder.createCollection(context, parentCommunity)
                    .withEntityType("publication")
                    .withName("test_collection").build();
            Item publicationItem = ItemBuilder.createItem(context, publicationItemCollection)
                    .withPersonIdentifierFirstName("test_first_name")
                    .withPersonIdentifierLastName("test_second_name")
                    .withScopusAuthorIdentifier("test_author_identifier")
                    .withMetadata(MetadataSchemaEnum.DC.getName(), "title", null, "publicationTitle")
                    .withType("publication")
                    .build();

            Collection productItemCollection = CollectionBuilder.createCollection(context, parentCommunity)
                    .withEntityType("product")
                    .withName("test_collection").build();
            Item productItem = ItemBuilder.createItem(context, productItemCollection)
                    .withPersonIdentifierFirstName("test_first_name")
                    .withPersonIdentifierLastName("test_second_name")
                    .withScopusAuthorIdentifier("test_author_identifier")
                    .withMetadata(MetadataSchemaEnum.DC.getName(), "title", null, "productTitle")
                    .withMetadata(MetadataSchemaEnum.DC.getName(), "relation", "product",
                            null, "notExistingPublicationTitle", notExistingItemId.toString(), Choices.CF_ACCEPTED)
                    .withType("product")
                    .build();

            List<MetadataValue> metadataValues = itemService.getMetadataByMetadataString(
                    publicationItem, "dc.relation.product");
            Assert.assertEquals(0, metadataValues.size());

            Item foundProductItem = itemService.findByIdOrLegacyId(new Context(), productItem.getID().toString());
            Assert.assertEquals(productItem.getID(), foundProductItem.getID());

        } finally {
            configurationService.setProperty("authority.controlled.dc.relation.product", "false");
            metadataAuthorityService.clearCache();
        }
    }

    @Test
    public void testItemMentioningInvalidAuthorityIsCreated() throws Exception {
        try {
            configurationService.setProperty("authority.controlled.dc.relation.product", "true");
            metadataAuthorityService.clearCache();

            Collection productItemCollection = CollectionBuilder.createCollection(context, parentCommunity)
                    .withEntityType("product")
                    .withName("test_collection").build();
            Item productItem = ItemBuilder.createItem(context, productItemCollection)
                    .withPersonIdentifierFirstName("test_first_name")
                    .withPersonIdentifierLastName("test_second_name")
                    .withScopusAuthorIdentifier("test_author_identifier")
                    .withMetadata(MetadataSchemaEnum.DC.getName(), "title", null, "productTitle")
                    .withMetadata(MetadataSchemaEnum.DC.getName(), "relation", "product",
                            null, "notExistingPublicationTitle", "invalidAuthorityUUID", Choices.CF_ACCEPTED)
                    .withType("product")
                    .build();

            Item foundProductItem = itemService.findByIdOrLegacyId(new Context(), productItem.getID().toString());
            Assert.assertEquals(productItem.getID(), foundProductItem.getID());

        } finally {
            configurationService.setProperty("authority.controlled.dc.relation.product", "false");
            metadataAuthorityService.clearCache();
        }
    }

    @Test
    public void testItemWithoutAuthorityIsCreated() throws Exception {
        try {
            configurationService.setProperty("authority.controlled.dc.relation.product", "true");
            metadataAuthorityService.clearCache();

            String publicationTitle = "publicationTitle";
            Collection publicatoinItemCollection = CollectionBuilder.createCollection(context, parentCommunity)
                    .withEntityType("publication")
                    .withName("test_collection").build();
            Item publicatoinItem = ItemBuilder.createItem(context, publicatoinItemCollection)
                    .withPersonIdentifierFirstName("test_first_name")
                    .withPersonIdentifierLastName("test_second_name")
                    .withScopusAuthorIdentifier("test_author_identifier")
                    .withMetadata(MetadataSchemaEnum.DC.getName(), "title", null, publicationTitle)
                    .withType("publication")
                    .build();

            Collection productItemCollection = CollectionBuilder.createCollection(context, parentCommunity)
                    .withEntityType("product")
                    .withName("test_collection").build();
            Item productItem = ItemBuilder.createItem(context, productItemCollection)
                    .withPersonIdentifierFirstName("test_first_name")
                    .withPersonIdentifierLastName("test_second_name")
                    .withScopusAuthorIdentifier("test_author_identifier")
                    .withMetadata(MetadataSchemaEnum.DC.getName(), "title", null, "productTitle")
                    .withMetadata(MetadataSchemaEnum.DC.getName(), "relation", "publication", publicationTitle)
                    .withType("product")
                    .build();

            List<MetadataValue> metadataValues = itemService.getMetadataByMetadataString(
                    publicatoinItem, "dc.relation.product");
            Assert.assertEquals(0, metadataValues.size());

            Item foundProductItem = itemService.findByIdOrLegacyId(new Context(), productItem.getID().toString());
            Assert.assertEquals(productItem.getID(), foundProductItem.getID());

        } finally {
            configurationService.setProperty("authority.controlled.dc.relation.product", "false");
            metadataAuthorityService.clearCache();
        }
    }

    @Test
    public void testItemWithoutPublicationMetadataIsCreated() throws Exception {
        try {
            configurationService.setProperty("authority.controlled.dc.relation.product", "true");
            metadataAuthorityService.clearCache();

            Collection productItemCollection = CollectionBuilder.createCollection(context, parentCommunity)
                    .withEntityType("product")
                    .withName("test_collection").build();
            Item productItem = ItemBuilder.createItem(context, productItemCollection)
                    .withPersonIdentifierFirstName("test_first_name")
                    .withPersonIdentifierLastName("test_second_name")
                    .withScopusAuthorIdentifier("test_author_identifier")
                    .withMetadata(MetadataSchemaEnum.DC.getName(), "title", null, "productTitle")
                    .withType("product")
                    .build();

            List<MetadataValue> productItemMetadataValues = itemService.getMetadataByMetadataString(
                    productItem, "dc.relation.publication");
            Assert.assertEquals(0, productItemMetadataValues.size());

            Item foundProductItem = itemService.findByIdOrLegacyId(new Context(), productItem.getID().toString());
            Assert.assertEquals(productItem.getID(), foundProductItem.getID());

        } finally {
            configurationService.setProperty("authority.controlled.dc.relation.product", "false");
            metadataAuthorityService.clearCache();
        }
    }

    private void initializeReciprocalConfiguration() throws Exception {
        Dispatcher dispatcher = eventService.getDispatcher("default");
        Object object = dispatcher.getConsumers();
        if (object instanceof Map) {
            Map<String, ConsumerProfile> consumers = (LinkedHashMap<String, ConsumerProfile>)dispatcher.getConsumers();

            ConsumerProfile consumerProfile = consumers.get("reciprocal");
            consumerProfile.getConsumer().initialize();
        }
    }

}