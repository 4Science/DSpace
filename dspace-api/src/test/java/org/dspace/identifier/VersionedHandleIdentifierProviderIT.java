/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.VersionBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.junit.Before;
import org.junit.Test;

public class VersionedHandleIdentifierProviderIT extends AbstractIdentifierProviderIT  {

    private String firstHandle;

    private Collection collection;
    private Item itemV1;
    private Item itemV2;
    private Item itemV3;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection")
                .withEntityType("Publication")
                .build();
    }

    private void createVersions() throws SQLException, AuthorizeException {
        itemV1 = ItemBuilder.createItem(context, collection)
                .withTitle("First version")
                .build();
        firstHandle = itemV1.getHandle();
        itemV2 = VersionBuilder.createVersion(context, itemV1, "Second version").build().getItem();
        itemV3 = VersionBuilder.createVersion(context, itemV1, "Third version").build().getItem();
    }

    @Test
    public void testDefaultVersionedHandleProvider() throws Exception {
        createVersions();

        // Confirm the original item only has its original handle
        assertEquals(firstHandle, itemV1.getHandle());
        assertEquals(1, itemV1.getHandles().size());
        // Confirm the second item has the correct version handle
        assertEquals(firstHandle + ".2", itemV2.getHandle());
        assertEquals(1, itemV2.getHandles().size());
        // Confirm the last item has the correct version handle
        assertEquals(firstHandle + ".3", itemV3.getHandle());
        assertEquals(1, itemV3.getHandles().size());
    }

    @Test
    public void testCanonicalVersionedHandleProvider() throws Exception {
        registerProvider(VersionedHandleIdentifierProviderWithCanonicalHandles.class);
        createVersions();

        // Confirm the original item only has a version handle
        assertEquals(firstHandle + ".1", itemV1.getHandle());
        assertEquals(1, itemV1.getHandles().size());
        // Confirm the second item has the correct version handle
        assertEquals(firstHandle + ".2", itemV2.getHandle());
        assertEquals(1, itemV2.getHandles().size());
        // Confirm the last item has both the correct version handle and the original handle
        assertEquals(firstHandle, itemV3.getHandle());
        assertEquals(2, itemV3.getHandles().size());
        containsHandle(itemV3, firstHandle + ".3");

        // Unregister this non-default provider
        unregisterProvider(VersionedHandleIdentifierProviderWithCanonicalHandles.class);
        // Re-register the default provider (for later tests)
        registerProvider(VersionedHandleIdentifierProvider.class);
    }

    private void containsHandle(Item item, String handle) {
        assertTrue(item.getHandles().stream().anyMatch(h -> handle.equals(h.getHandle())));
    }
}
