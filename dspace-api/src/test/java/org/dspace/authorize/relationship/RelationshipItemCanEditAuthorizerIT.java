/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize.relationship;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.edit.service.EditItemModeService;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link RelationshipItemCanEditAuthorizer}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class RelationshipItemCanEditAuthorizerIT extends AbstractIntegrationTestWithDatabase {

    private Collection collection;

    private RelationshipItemCanEditAuthorizer authorizer;

    @Before
    public void before() {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent community")
            .build();
        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Test collection")
            .withEntityType("Publication")
            .build();
        context.restoreAuthSystemState();

        authorizer = new RelationshipItemCanEditAuthorizer();
        authorizer.setEditItemModeService(getEditItemModeService());
    }

    @Test
    public void testWithNotAllowedUser() {
        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .build();

        context.restoreAuthSystemState();
        context.setCurrentUser(eperson);

        assertThat(authorizer.canHandleRelationshipOnItem(context, item), is(false));
    }

    @Test
    public void testWithoutCurrentUser() {
        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .build();

        context.restoreAuthSystemState();
        context.setCurrentUser(null);

        assertThat(authorizer.canHandleRelationshipOnItem(context, item), is(false));
    }

    @Test
    public void testWithAdminUser() {
        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .build();

        context.restoreAuthSystemState();
        context.setCurrentUser(admin);

        assertThat(authorizer.canHandleRelationshipOnItem(context, item), is(true));
    }

    @Test
    public void testWithOwnerUser() {
        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withDspaceObjectOwner(eperson)
            .build();

        context.restoreAuthSystemState();
        context.setCurrentUser(eperson);

        assertThat(authorizer.canHandleRelationshipOnItem(context, item), is(true));
    }

    @Test
    public void testWithCrisPolicyEPerson() {
        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withCrisPolicyEPerson(eperson.getEmail(), eperson.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.setCurrentUser(eperson);

        assertThat(authorizer.canHandleRelationshipOnItem(context, item), is(true));
    }

    private EditItemModeService getEditItemModeService() {
        return new DSpace().getServiceManager().getServiceByName(
            "org.dspace.content.edit.service.impl.EditItemModeServiceImpl", EditItemModeService.class);
    }

}
