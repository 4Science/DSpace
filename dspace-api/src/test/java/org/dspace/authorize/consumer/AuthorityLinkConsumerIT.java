/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize.consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authority.AuthorityLinkConsumer;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link AuthorityLinkConsumer}.
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 *
 */
public class AuthorityLinkConsumerIT extends AbstractIntegrationTestWithDatabase {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();

    private Collection collection;

    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection 1")
            .withEntityType("Person")
            .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void testPersonLinkInTextValue() throws Exception {

        context.turnOffAuthorisationSystem();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle("test authority link")
            .withCustomIdentifierUrl("https://www.example.com", "")
            .build();

        installItemService.installItem(context, workspaceItem);
        context.dispatchEvents();
        context.restoreAuthSystemState();

        itemService.getMetadata(workspaceItem.getItem(), "oairecerif", "identifier", "url", Item.ANY)
            .forEach(metadataVal -> {
                assertThat(metadataVal.getAuthority(), is(metadataVal.getValue()));
            });

    }

    @Test
    public void testPersonLinkLabelAndValueDiffer() throws Exception {

        context.turnOffAuthorisationSystem();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle("test authority link")
            .withCustomIdentifierUrl("example", "https://www.example.com")
            .build();

        installItemService.installItem(context, workspaceItem);
        context.dispatchEvents();
        context.restoreAuthSystemState();

        itemService.getMetadata(workspaceItem.getItem(), "oairecerif", "identifier", "url", Item.ANY)
            .forEach(metadataVal -> {
                assertThat(metadataVal.getAuthority(), is("https://www.example.com"));
                assertThat(metadataVal.getValue(), is("example"));
            });

    }

    @Test
    public void testPersonLinkLabelEmpty() throws Exception {

        context.turnOffAuthorisationSystem();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle("test authority link")
            .withCustomIdentifierUrl("", "https://www.example.com")
            .build();

        installItemService.installItem(context, workspaceItem);
        context.dispatchEvents();
        context.restoreAuthSystemState();

        itemService.getMetadata(workspaceItem.getItem(), "oairecerif", "identifier", "url", Item.ANY)
            .forEach(metadataVal -> {
                assertThat(metadataVal.getAuthority(), is(metadataVal.getValue()));
            });

    }

    @Test
    public void testPersonLinkMixedTests() throws Exception {

        context.turnOffAuthorisationSystem();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle("test authority link")
            .withCustomIdentifierUrl("", "https://www.example.com")
            .withCustomIdentifierUrl("https://www.example.com", "https://www.example.com")
            .withCustomIdentifierUrl("https://www.example.com", "")
            .build();

        installItemService.installItem(context, workspaceItem);
        context.dispatchEvents();
        context.restoreAuthSystemState();

        itemService.getMetadata(workspaceItem.getItem(), "oairecerif", "identifier", "url", Item.ANY)
            .forEach(metadataVal -> {
                assertThat(metadataVal.getAuthority(), is(metadataVal.getValue()));
            });

    }

}
