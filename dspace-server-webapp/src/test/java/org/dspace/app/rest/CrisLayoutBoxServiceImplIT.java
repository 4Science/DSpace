/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBoxTypes;
import org.dspace.layout.service.impl.CrisLayoutBoxServiceImpl;
import org.dspace.versioning.service.VersioningService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Unit tests for CrisLayoutBoxServiceImpl, so far only findByItem method is
 * tested.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */

public class CrisLayoutBoxServiceImplIT extends AbstractControllerIntegrationTest {

    @Autowired
    private CrisLayoutBoxServiceImpl crisLayoutBoxService;

    @Autowired
    private VersioningService versioningService;

    @Test
    public void testHasContentVersioningBox() {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Test collection")
            .withEntityType("Product")
            .build();

        Item item = ItemBuilder
            .createItem(context, col)
            .withTitle("test")
            .build();

        versioningService.createNewVersion(context, item);

        CrisLayoutBox box = crisLayoutVersioningBox("versioning");

        assertThat(crisLayoutBoxService.hasContent(context, box, item), is(true));
        context.restoreAuthSystemState();
    }

    @Test
    public void testHasNoContentVersioningBox() {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Test collection")
            .build();

        Item item = ItemBuilder
            .createItem(context, col)
            .withTitle("test")
            .build();

        CrisLayoutBox box = crisLayoutVersioningBox("versioning");

        assertThat(crisLayoutBoxService.hasContent(context, box, item), is(false));
        context.restoreAuthSystemState();
    }

    private CrisLayoutBox crisLayoutVersioningBox(String shortname) {
        return crisLayoutBox(shortname, CrisLayoutBoxTypes.VERSIONING.name());
    }

    private CrisLayoutBox crisLayoutBox(String shortname, String boxType) {
        CrisLayoutBox box = new CrisLayoutBox();
        box.setShortname(shortname);
        box.setType(boxType);
        return box;
    }

}
