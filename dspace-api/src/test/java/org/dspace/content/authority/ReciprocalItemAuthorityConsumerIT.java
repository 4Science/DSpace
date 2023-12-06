/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.List;
import java.util.UUID;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.MockSolrSearchCore;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ReciprocalItemAuthorityConsumerIT extends AbstractIntegrationTestWithDatabase {

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private MockSolrSearchCore searchService;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        ServiceManager serviceManager = DSpaceServicesFactory.getInstance().getServiceManager();
        searchService = serviceManager.getServiceByName(null, MockSolrSearchCore.class);

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
    }

    @Test
    public void testShouldCreatePublicationMetadataForProductItem() throws Exception {
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

        SolrDocumentList solrDocumentList = getSolrDocumentList(productItem);
        Assert.assertEquals(1, solrDocumentList.size());
        SolrDocument solrDoc = solrDocumentList.get(0);

        List<String> publicationTitles = (List<String>) solrDoc.get("dc.relation.publication");
        Assert.assertEquals(1, publicationTitles.size());
        Assert.assertEquals(publicationItem.getName(), publicationTitles.get(0));

        List<String> publicationAuthorities = (List<String>) solrDoc.get("dc.relation.publication_authority");
        Assert.assertEquals(1, publicationAuthorities.size());
        Assert.assertEquals(publicationItem.getID().toString(), publicationAuthorities.get(0));
    }

    @Test
    public void testShouldCreateProductMetadataForPublicationItem() throws Exception {
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

        SolrDocumentList solrDocumentList = getSolrDocumentList(publicationItem);
        Assert.assertEquals(1, solrDocumentList.size());
        SolrDocument solrDoc = solrDocumentList.get(0);

        List<String> productTitles = (List<String>) solrDoc.get("dc.relation.product");
        Assert.assertEquals(1, productTitles.size());
        Assert.assertEquals(productItem.getName(), productTitles.get(0));

        List<String> productAuthorities = (List<String>) solrDoc.get("dc.relation.product_authority");
        Assert.assertEquals(1, productAuthorities.size());
        Assert.assertEquals(productItem.getID().toString(), productAuthorities.get(0));
    }

    @Test
    public void testItemMentioningNotExistingAuthorityIsCreated() throws Exception {
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

        SolrDocumentList solrDocumentList = getSolrDocumentList(publicationItem);
        Assert.assertEquals(1, solrDocumentList.size());
        SolrDocument solrDoc = solrDocumentList.get(0);

        List<String> productTitles = (List<String>) solrDoc.get("dc.relation.product");
        Assert.assertNull(productTitles);

        List<String> productAuthorities = (List<String>) solrDoc.get("dc.relation.product_authority");
        Assert.assertNull(productAuthorities);

        Item foundProductItem = itemService.findByIdOrLegacyId(new Context(), productItem.getID().toString());
        Assert.assertEquals(productItem.getID(), foundProductItem.getID());
    }

    @Test
    public void testItemMentioningInvalidAuthorityIsCreated() throws Exception {
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

        SolrDocumentList solrDocumentList = getSolrDocumentList(productItem);
        Assert.assertEquals(1, solrDocumentList.size());
        SolrDocument solrDoc = solrDocumentList.get(0);

        List<String> publicationTitles = (List<String>) solrDoc.get("dc.relation.publication");
        Assert.assertNull(publicationTitles);

        List<String> publicationAuthorities = (List<String>) solrDoc.get("dc.relation.publication_authority");
        Assert.assertNull(publicationAuthorities);

        Item foundProductItem = itemService.findByIdOrLegacyId(new Context(), productItem.getID().toString());
        Assert.assertEquals(productItem.getID(), foundProductItem.getID());
    }

    @Test
    public void testItemWithoutAuthorityIsCreated() throws Exception {
        String publicationTitle = "publicationTitle";
        Collection publicatoinItemCollection = CollectionBuilder.createCollection(context, parentCommunity)
                .withEntityType("publication")
                .withName("test_collection").build();
        Item publicationItem = ItemBuilder.createItem(context, publicatoinItemCollection)
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
                publicationItem, "dc.relation.product");
        Assert.assertEquals(0, metadataValues.size());

        SolrDocumentList solrDocumentList = getSolrDocumentList(publicationItem);
        Assert.assertEquals(1, solrDocumentList.size());
        SolrDocument solrDoc = solrDocumentList.get(0);

        List<String> productTitles = (List<String>) solrDoc.get("dc.relation.product");
        Assert.assertNull(productTitles);

        List<String> productAuthorities = (List<String>) solrDoc.get("dc.relation.product_authority");
        Assert.assertNull(productAuthorities);

        Item foundProductItem = itemService.findByIdOrLegacyId(new Context(), productItem.getID().toString());
        Assert.assertEquals(productItem.getID(), foundProductItem.getID());
    }

    @Test
    public void testItemWithoutPublicationMetadataIsCreated() throws Exception {
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

        SolrDocumentList solrDocumentList = getSolrDocumentList(productItem);
        Assert.assertEquals(1, solrDocumentList.size());
        SolrDocument solrDoc = solrDocumentList.get(0);

        List<String> publicationTitles = (List<String>) solrDoc.get("dc.relation.publication");
        Assert.assertNull(publicationTitles);

        List<String> publicationAuthorities = (List<String>) solrDoc.get("dc.relation.publication_authority");
        Assert.assertNull(publicationAuthorities);

        Item foundProductItem = itemService.findByIdOrLegacyId(new Context(), productItem.getID().toString());
        Assert.assertEquals(productItem.getID(), foundProductItem.getID());
    }

    public SolrDocumentList getSolrDocumentList(Item item) throws Exception {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("search.resourceid:" + item.getID());
        QueryResponse queryResponse = searchService.getSolr().query(solrQuery);
        return queryResponse.getResults();
    }

}