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
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
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
                archive.getFile().getPath());

        Iterator<Item> itemsIterator = itemService.findAllByCollection(context, magCollection);
        List<Item> items = IteratorUtils.toList(itemsIterator);
        assertEquals(1, items.size());

        List<Bundle> tiffBundles = itemService.getBundles(items.get(0), "CUSTOMER-TIFF");
        assertEquals(1, tiffBundles.size());

        List<Bitstream> tiffBitstreams = tiffBundles.get(0).getBitstreams();
        assertEquals(1, tiffBitstreams.size());

        // "Index|||<stru.nomenclature>|||<img.nomenclature>" from MAG manifest.
        String expectedIfffToc = "Index|||Coperta anteriore e dorso|||Dorso";
        String iiifTocValue = bitstreamService
                .getMetadataFirstValue(tiffBitstreams.get(0), "iiif", "toc", null, Item.ANY);
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
                archive.getFile().getPath());

        Iterator<Item> itemsIterator = itemService.findAllByCollection(context, magCollection);
        List<Item> items = IteratorUtils.toList(itemsIterator);
        assertEquals(1, items.size());

        List<Bundle> tiffBundles = itemService.getBundles(items.get(0), "CUSTOMER-TIFF");
        assertEquals(1, tiffBundles.size());

        List<Bitstream> tiffBitstreams = tiffBundles.get(0).getBitstreams();
        assertEquals(1, tiffBitstreams.size());

        String iiifTocValue = bitstreamService
                .getMetadataFirstValue(tiffBitstreams.get(0), "iiif", "toc", null, Item.ANY);
        assertNull(iiifTocValue);
    }

    @Test
    public void testShouldAddMetadataWhenTagExistsInManifest() throws Exception {
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
                archive.getFile().getPath());

        Iterator<Item> itemsIterator = itemService.findAllByCollection(context, magCollection);
        List<Item> items = IteratorUtils.toList(itemsIterator);
        assertEquals(1, items.size());

        List<MetadataValue> identifiers = itemService.getMetadata(items.get(0), "dc", "identifier", "other", null);
        assertEquals(1, identifiers.size());
        assertEquals("UNIBA_BFB_2069_2_", identifiers.get(0).getValue());

        List<MetadataValue> languages = itemService.getMetadata(items.get(0), "dc", "language", "iso", null);
        assertEquals(1, languages.size());
        assertEquals("fr", languages.get(0).getValue());

        List<MetadataValue> dates = itemService.getMetadata(items.get(0), "dc", "date", "issued", null);
        assertEquals(1, dates.size());
        assertEquals("1829", dates.get(0).getValue());

        List<MetadataValue> titles = itemService.getMetadata(items.get(0), "dc", "title", null, null);
        assertEquals(1, titles.size());
        assertEquals("Histoire naturelle de Pline traduction nouvelle par M. Ajasson de Grandsagne annotée" +
                " par MM. Beudant, Brongniart, G. Cuvier, et al. Tome quatrième", titles.get(0).getValue());
    }

    @Test
    public void testShouldNotAddMetadataWhenTagNotExistsInManifest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection magCollection = CollectionBuilder
                .createCollection(context, parentCommunity, "123456789/31")
                .withName("MagCollection2")
                .withEntityType("Publication").build();

        String archiveClassPath = "classpath:org/dspace/app/itemimport/UNIBA_MAG_archive_with_stru.zip";
        Resource archive = new DSpace().getServiceManager().getApplicationContext().getResource(archiveClassPath);

        runDSpaceScript("packager",
                "-e", admin.getEmail(),
                "-p", magCollection.getHandle(),
                "-t", "MAG",
                archive.getFile().getPath());

        Iterator<Item> itemsIterator = itemService.findAllByCollection(context, magCollection);
        List<Item> items = IteratorUtils.toList(itemsIterator);
        assertEquals(1, items.size());

        List<MetadataValue> identifiers = itemService.getMetadata(items.get(0), "dc", "identifier", "other", null);
        assertEquals(1, identifiers.size());

        List<MetadataValue> languages = itemService.getMetadata(items.get(0), "dc", "language", "iso", null);
        assertEquals(0, languages.size());

        List<MetadataValue> dates = itemService.getMetadata(items.get(0), "dc", "date", "issued", null);
        assertEquals(0, dates.size());

        List<MetadataValue> titles = itemService.getMetadata(items.get(0), "dc", "title", null, null);
        assertEquals(0, titles.size());
    }

}
