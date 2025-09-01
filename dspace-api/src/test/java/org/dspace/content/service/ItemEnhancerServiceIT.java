/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.apache.logging.log4j.Logger;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.enhancer.service.ItemEnhancerService;
import org.dspace.content.enhancer.service.impl.ItemEnhancerServiceImpl;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ItemEnhancerServiceIT extends AbstractIntegrationTestWithDatabase {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ItemEnhancerServiceIT.class);

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private ItemService spyItemService = spy(itemService);
    private ItemEnhancerServiceImpl itemEnhancerService = (ItemEnhancerServiceImpl) new DSpace()
            .getSingletonService(ItemEnhancerService.class);

    Community community;
    Collection collPub;
    Collection collPerson;
    Item person;
    Item publication;

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     */
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        community = CommunityBuilder.createCommunity(context)
            .build();
        collPerson = CollectionBuilder.createCollection(context, community)
                .withEntityType("Person")
                .build();
        collPub = CollectionBuilder.createCollection(context, community)
            .withEntityType("Publication")
            .build();
        person = ItemBuilder.createItem(context, collPerson)
                .withTitle("Famous Researcher")
                .withAffiliation("Some department", null)
                .build();

        publication = ItemBuilder.createItem(context, collPub)
                .withTitle("Item to enhance")
                .withAuthor(person.getName(), person.getID().toString())
                .build();
        assertMetadataValue(itemService.getMetadataByMetadataString(publication, "cris.virtual.department").get(0),
                "cris", "virtual", "department", "Some department", null, 0);
        context.restoreAuthSystemState();
        itemEnhancerService.setItemService(spyItemService);
    }

    @After
    public void after() {
        itemEnhancerService.setItemService(itemService);
    }

    @Test
    public void noUpdateRequiredTest() throws Exception {
        context.turnOffAuthorisationSystem();
        itemEnhancerService.enhance(context, publication, false);
        verify(spyItemService, never()).update(any(), any());
        itemEnhancerService.enhance(context, publication, true);
        verify(spyItemService, never()).update(any(), any());
        context.restoreAuthSystemState();
    }


    private void assertMetadataValue(MetadataValue metadataValue, String schema, String element, String qualifier,
            String value, String authority, int place) {
        assertThat(metadataValue.getValue(), equalTo(value));
        assertThat(metadataValue.getMetadataField().getMetadataSchema().getName(), equalTo(schema));
        assertThat(metadataValue.getMetadataField().getElement(), equalTo(element));
        assertThat(metadataValue.getMetadataField().getQualifier(), equalTo(qualifier));
        assertThat(metadataValue.getAuthority(), equalTo(authority));
        assertThat(metadataValue.getPlace(), equalTo(place));
    }
}
