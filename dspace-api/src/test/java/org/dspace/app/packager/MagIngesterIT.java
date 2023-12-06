/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.packager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.IteratorUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.Resource;

/**
 * Basic integration testing for the MAG Ingester.
 */
public class MagIngesterIT extends AbstractIntegrationTestWithDatabase {

    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected static final InstallItemService installItemService = ContentServiceFactory.getInstance()
            .getInstallItemService();
    protected ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Root community").build();
        context.restoreAuthSystemState();
    }

    @Test
    public void testIiifTocMetadataIsPresented() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection magCollection = CollectionBuilder
                .createCollection(context, parentCommunity, "123456789/34")
                .withName("MagCollection1")
                .withEntityType("Publication").build();

        String archiveClassPath = "classpath:org/dspace/app/itemimport/UNIBA_MAG_archive_with_stru.zip";
        Resource archive = new DSpace().getServiceManager().getApplicationContext().getResource(archiveClassPath);

        runDSpaceScript("packager",
                "-e", admin.getEmail(),
                "-p", magCollection.getHandle(),
                "-t", "MAG",
                "-z", archive.getFile().getPath());

        Iterator<Item> itemsIterator = itemService.findAllByCollection(context, magCollection);
        List<Item> items = IteratorUtils.toList(itemsIterator);
        assertEquals(1, items.size());

        List<Bundle> originalBundles = itemService.getBundles(items.get(0), Constants.CONTENT_BUNDLE_NAME);
        assertEquals(1, originalBundles.size());

        List<Bitstream> originalBitstreams = originalBundles.get(0).getBitstreams();
        assertEquals(1, originalBitstreams.size());

        // "Index|||<stru.nomenclature>|||<img.nomenclature>" from MAG manifest.
        String expectedIfffToc = "Index|||Coperta anteriore e dorso|||Dorso";
        String iiifTocValue = bitstreamService
                .getMetadataFirstValue(originalBitstreams.get(0), "iiif", "toc", null, Item.ANY);
        assertEquals(expectedIfffToc, iiifTocValue);
    }

    @Test
    public void testIiifTocMetadataIsNotPresented() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection magCollection = CollectionBuilder
                .createCollection(context, parentCommunity, "123456789/31")
                .withName("MagCollection2")
                .withEntityType("Publication").build();

        String archiveClassPath = "classpath:org/dspace/app/itemimport/UNIBA_MAG_archive_without_stru.zip";
        Resource archive = new DSpace().getServiceManager().getApplicationContext().getResource(archiveClassPath);

        runDSpaceScript("packager",
                "-e", admin.getEmail(),
                "-p", magCollection.getHandle(),
                "-t", "MAG",
                "-z", archive.getFile().getPath());

        Iterator<Item> itemsIterator = itemService.findAllByCollection(context, magCollection);
        List<Item> items = IteratorUtils.toList(itemsIterator);
        assertEquals(1, items.size());

        List<Bundle> originalBundles = itemService.getBundles(items.get(0), Constants.CONTENT_BUNDLE_NAME);
        assertEquals(1, originalBundles.size());

        List<Bitstream> originalBitstreams = originalBundles.get(0).getBitstreams();
        assertEquals(1, originalBitstreams.size());

        String iiifTocValue = bitstreamService
                .getMetadataFirstValue(originalBitstreams.get(0), "iiif", "toc", null, Item.ANY);
        assertNull(iiifTocValue);
    }

}
