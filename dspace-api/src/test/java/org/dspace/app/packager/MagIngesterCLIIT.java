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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.IteratorUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
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
 * Basic integration testing for the MAG Ingester via CLI {@link Packager}.
 * https://wiki.lyrasis.org/display/DSDOC7x/Importing+and+Exporting+Content+via+Packages
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 * @author Nikita Krivonosov (nikita.krivonosov at 4science.com)
 */
public class MagIngesterCLIIT extends AbstractIntegrationTestWithDatabase {

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
                .createCollection(context, parentCommunity)
                .withName("MagCollection1")
                .withEntityType("Publication").build();

        String archiveClassPath = "classpath:org/dspace/app/packager/UNIBA_MAG_archive_with_stru.zip";
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
        compareBitstreamMetadata(tiffBitstreams.get(0), "iiif", "toc", null, expectedIfffToc);
    }

    @Test
    public void testIiifTocMetadataIsNotPresented() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection magCollection = CollectionBuilder
                .createCollection(context, parentCommunity)
                .withName("MagCollection2")
                .withEntityType("Publication").build();

        String archiveClassPath = "classpath:org/dspace/app/packager/UNIBA_MAG_archive_without_stru.zip";
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

        compareToNullBitstreamMetadata(tiffBitstreams.get(0), "iiif", "toc", null);
    }

    @Test
    public void testShouldAddMetadataWhenTagExistsInManifest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection magCollection = CollectionBuilder
                .createCollection(context, parentCommunity)
                .withName("MagCollection3")
                .withEntityType("Publication").build();

        String archiveClassPath = "classpath:org/dspace/app/packager/UNIBA_MAG_archive_without_stru.zip";
        Resource archive = new DSpace().getServiceManager().getApplicationContext().getResource(archiveClassPath);

        runDSpaceScript("packager",
                "-e", admin.getEmail(),
                "-p", magCollection.getHandle(),
                "-t", "MAG",
                archive.getFile().getPath());

        Iterator<Item> itemsIterator = itemService.findAllByCollection(context, magCollection);
        List<Item> items = IteratorUtils.toList(itemsIterator);
        assertEquals(1, items.size());

        compareItemMetadata(items.get(0), "dc", "identifier", "other", "UNIBA_BFB_2069_2_");
        compareItemMetadata(items.get(0), "dc", "language", "iso", "fr");
        compareItemMetadata(items.get(0), "dc", "date", "issued", "1829");

        String expectedTitle = "Histoire naturelle de Pline traduction nouvelle par M. Ajasson de Grandsagne annotée" +
                " par MM. Beudant, Brongniart, G. Cuvier, et al. Tome quatrième";
        compareItemMetadata(items.get(0), "dc", "title", null, expectedTitle);
    }

    @Test
    public void testShouldNotAddMetadataWhenTagNotExistsInManifest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection magCollection = CollectionBuilder
                .createCollection(context, parentCommunity)
                .withName("MagCollection4")
                .withEntityType("Publication").build();

        String archiveClassPath = "classpath:org/dspace/app/packager/UNIBA_MAG_archive_with_stru.zip";
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

    @Test
    public void testThrowPackageValidationExceptionWhenTagIdentifierNotFoundInManifest() {
        context.turnOffAuthorisationSystem();

        Collection magCollection = CollectionBuilder
                .createCollection(context, parentCommunity)
                .withName("MagCollection5")
                .withEntityType("Publication").build();

        String archiveClassPath = "classpath:org/dspace/app/packager/UNIBA_MAG_archive_without_identifier.zip";
        Resource archive = new DSpace().getServiceManager().getApplicationContext().getResource(archiveClassPath);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> runDSpaceScript("packager",
                        "-e", admin.getEmail(),
                        "-p", magCollection.getHandle(),
                        "-t", "MAG",
                        archive.getFile().getPath()));

        assertTrue(exception.getMessage().contains("Manifest is missing the required identifier."));
    }

    @Test
    public void testShouldAddMetadataToExistingItem() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection magCollection = CollectionBuilder
                .createCollection(context, parentCommunity)
                .withName("MagCollection6")
                .withEntityType("Publication").build();

        ItemBuilder.createItem(context, magCollection)
                .withOtherIdentifier("UNIBA_BFB_2069_2_")
                .withAuthor("Donald, Smith")
                .build();

        String archiveClassPath = "classpath:org/dspace/app/packager/UNIBA_MAG_archive_without_stru.zip";
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
        assertEquals(2, identifiers.size());
        assertEquals("UNIBA_BFB_2069_2_", identifiers.get(0).getValue());
        assertEquals("UNIBA_BFB_2069_2_", identifiers.get(1).getValue());

        List<MetadataValue> authors = itemService.getMetadata(items.get(0), "dc", "contributor", "author", null);
        assertEquals(2, authors.size());
        assertEquals("Donald, Smith", authors.get(0).getValue());
        assertEquals("PLINIUS, Gaius Secundus", authors.get(1).getValue());

        compareItemMetadata(items.get(0), "dc", "type", null, "Testo a stampa");

        String expectedRights = "Università degli Studi di Bari Aldo Moro";
        compareItemMetadata(items.get(0), "dc", "rights", null, expectedRights);
    }

    @Test
    public void testShouldAddMetadataToJpeg300Bitstream() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection magCollection = CollectionBuilder
                .createCollection(context, parentCommunity)
                .withName("MagCollection7")
                .withEntityType("Publication").build();

        String archiveClassPath = "classpath:org/dspace/app/packager/UNIBA_MAG_archive_without_stru.zip";
        Resource archive = new DSpace().getServiceManager().getApplicationContext().getResource(archiveClassPath);

        runDSpaceScript("packager",
                "-e", admin.getEmail(),
                "-p", magCollection.getHandle(),
                "-t", "MAG",
                archive.getFile().getPath());

        Iterator<Item> itemsIterator = itemService.findAllByCollection(context, magCollection);
        List<Item> items = IteratorUtils.toList(itemsIterator);
        assertEquals(1, items.size());

        List<Bundle> jpeg300Bundles = itemService.getBundles(items.get(0), "CUSTOMER-JPEG300");
        assertEquals(1, jpeg300Bundles.size());

        List<Bitstream> jpeg300Bitstreams = jpeg300Bundles.get(0).getBitstreams();
        assertEquals(1, jpeg300Bitstreams.size());

        compareBitstreamMetadata(jpeg300Bitstreams.get(0), "mix", "xsamplingfrequency", null, "300");
        compareBitstreamMetadata(jpeg300Bitstreams.get(0), "mix", "ysamplingfrequency", null, "300");
    }

    @Test
    public void testShouldAddMetadataToThumbnailBitstream() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection magCollection = CollectionBuilder
                .createCollection(context, parentCommunity)
                .withName("MagCollection8")
                .withEntityType("Publication").build();

        String archiveClassPath = "classpath:org/dspace/app/packager/UNIBA_MAG_archive_without_stru.zip";
        Resource archive = new DSpace().getServiceManager().getApplicationContext().getResource(archiveClassPath);

        runDSpaceScript("packager",
                "-e", admin.getEmail(),
                "-p", magCollection.getHandle(),
                "-t", "MAG",
                archive.getFile().getPath());

        Iterator<Item> itemsIterator = itemService.findAllByCollection(context, magCollection);
        List<Item> items = IteratorUtils.toList(itemsIterator);
        assertEquals(1, items.size());

        List<Bundle> thumbnailBundles = itemService.getBundles(items.get(0), "THUMBNAIL");
        assertEquals(1, thumbnailBundles.size());

        List<Bitstream> thumbnailBitstreams = thumbnailBundles.get(0).getBitstreams();
        assertEquals(1, thumbnailBitstreams.size());

        compareBitstreamMetadata(thumbnailBitstreams.get(0), "mix", "xsamplingfrequency", null, "600");
        compareBitstreamMetadata(thumbnailBitstreams.get(0), "mix", "ysamplingfrequency", null, "600");
    }

    @Test
    public void testShouldAddMetadataWithGlamSchemaToItem() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection magCollection = CollectionBuilder
                .createCollection(context, parentCommunity)
                .withName("MagCollection9")
                .withEntityType("Publication").build();

        String archiveClassPath = "classpath:org/dspace/app/packager/UNIBA_MAG_archive_without_stru.zip";
        Resource archive = new DSpace().getServiceManager().getApplicationContext().getResource(archiveClassPath);

        runDSpaceScript("packager",
                "-e", admin.getEmail(),
                "-p", magCollection.getHandle(),
                "-t", "MAG",
                archive.getFile().getPath());

        Iterator<Item> itemsIterator = itemService.findAllByCollection(context, magCollection);
        List<Item> items = IteratorUtils.toList(itemsIterator);
        assertEquals(1, items.size());

        String expectedHolder = "Polo bibliotecario Scientifico - Agrario - Biblioteca di Biologia Vegetale";
        compareItemMetadata(items.get(0), "glam", "rights", "holder", expectedHolder);

        List<MetadataValue> partOfRelations = itemService.getMetadata(items.get(0), "glam", "relation",
                "ispartof", null);
        assertEquals(3, partOfRelations.size());
        assertEquals("Fa parte di: Histoire naturelle de Pline...Panckoucke 1829-1833",
                partOfRelations.get(0).getValue());

        List<MetadataValue> referencesRelations = itemService.getMetadata(items.get(0), "glam", "relation",
                "references", null);
        assertEquals(3, referencesRelations.size());
        assertEquals("Legato con:Histoire naturelle de Pline...Tome troisième.", referencesRelations.get(1).getValue());

        List<MetadataValue> holders = itemService.getMetadata(items.get(0), "dc", "rights", "holder", null);
        assertEquals(0, holders.size());

        partOfRelations = itemService.getMetadata(items.get(0), "dc", "relation", "ispartof", null);
        assertEquals(0, partOfRelations.size());

        referencesRelations = itemService.getMetadata(items.get(0), "dc", "relation", "references", null);
        assertEquals(0, referencesRelations.size());
    }

    @Test
    public void testShouldAddOriginalBitstreamOnlyWithTitleMetadata() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection magCollection = CollectionBuilder
                .createCollection(context, parentCommunity)
                .withName("MagCollection10")
                .withEntityType("Publication").build();

        String archiveClassPath = "classpath:org/dspace/app/packager/UNIBA_MAG_archive_without_stru.zip";
        Resource archive = new DSpace().getServiceManager().getApplicationContext().getResource(archiveClassPath);

        runDSpaceScript("packager",
                "-e", admin.getEmail(),
                "-p", magCollection.getHandle(),
                "-t", "MAG",
                archive.getFile().getPath());

        Iterator<Item> itemsIterator = itemService.findAllByCollection(context, magCollection);
        List<Item> items = IteratorUtils.toList(itemsIterator);
        assertEquals(1, items.size());

        List<Bundle> originalBundles = itemService.getBundles(items.get(0), "ORIGINAL");
        assertEquals(1, originalBundles.size());

        List<Bitstream> originalBitstreams = originalBundles.get(0).getBitstreams();
        assertEquals(1, originalBitstreams.size());
        assertEquals(1, originalBitstreams.get(0).getMetadata().size());

        compareBitstreamMetadata(originalBitstreams.get(0), "dc", "title", null, "UNIBA_BFB_2069_2.pdf");
    }

    @Test
    public void testShouldAddTXTBitstreamOnlyWithTitleMetadata() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection magCollection = CollectionBuilder
                .createCollection(context, parentCommunity)
                .withName("MagCollection10")
                .withEntityType("Publication").build();

        String archiveClassPath = "classpath:org/dspace/app/packager/UNIBA_MAG_archive_without_stru.zip";
        Resource archive = new DSpace().getServiceManager().getApplicationContext().getResource(archiveClassPath);

        runDSpaceScript("packager",
                "-e", admin.getEmail(),
                "-p", magCollection.getHandle(),
                "-t", "MAG",
                archive.getFile().getPath());

        Iterator<Item> itemsIterator = itemService.findAllByCollection(context, magCollection);
        List<Item> items = IteratorUtils.toList(itemsIterator);
        assertEquals(1, items.size());

        List<Bundle> txtBundles = itemService.getBundles(items.get(0), "CUSTOMER-TEXT");
        assertEquals(1, txtBundles.size());

        List<Bitstream> txtBitstreams = txtBundles.get(0).getBitstreams();
        assertEquals(1, txtBitstreams.size());
        assertEquals(1, txtBitstreams.get(0).getMetadata().size());
        compareBitstreamMetadata(txtBitstreams.get(0), "dc", "title", null, "UNIBA_BFB_2069_2_0001.txt");

        List<Bundle> hocrBundles = itemService.getBundles(items.get(0), "CUSTOMER-HOCR");
        assertEquals(1, hocrBundles.size());

        List<Bitstream> hocrBitstreams = hocrBundles.get(0).getBitstreams();
        assertEquals(1, hocrBitstreams.size());
        assertEquals(1, hocrBitstreams.get(0).getMetadata().size());
        compareBitstreamMetadata(hocrBitstreams.get(0), "dc", "title", null, "UNIBA_BFB_2069_2_0001.hocr");
    }

    @Test
    public void testShouldAddThumbnailMetadataToBitstream() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection magCollection = CollectionBuilder
                .createCollection(context, parentCommunity)
                .withName("MagCollection11")
                .withEntityType("Publication").build();

        String archiveClassPath = "classpath:org/dspace/app/packager/UNIBA_MAG_archive_without_stru.zip";
        Resource archive = new DSpace().getServiceManager().getApplicationContext().getResource(archiveClassPath);

        runDSpaceScript("packager",
                "-e", admin.getEmail(),
                "-p", magCollection.getHandle(),
                "-t", "MAG",
                archive.getFile().getPath());

        Iterator<Item> itemsIterator = itemService.findAllByCollection(context, magCollection);
        List<Item> items = IteratorUtils.toList(itemsIterator);
        assertEquals(1, items.size());

        List<Bundle> jpeg100Bundles = itemService.getBundles(items.get(0), "BRANDED_PREVIEW");
        assertEquals(1, jpeg100Bundles.size());

        List<Bitstream> jpeg100Bitstreams = jpeg100Bundles.get(0).getBitstreams();
        assertEquals(1, jpeg100Bitstreams.size());

        compareBitstreamMetadata(jpeg100Bitstreams.get(0), "mix", "samplingfrequencyunit", null, "inch");
        compareToNullBitstreamMetadata(jpeg100Bitstreams.get(0), "mix", "samplingfrequencyplane", null);
        compareBitstreamMetadata(jpeg100Bitstreams.get(0), "mix", "xsamplingfrequency", null, "100");
        compareBitstreamMetadata(jpeg100Bitstreams.get(0), "mix", "ysamplingfrequency", null, "100");
        compareBitstreamMetadata(jpeg100Bitstreams.get(0), "mix", "colorSpace", null, "YCbCr");
        compareBitstreamMetadata(jpeg100Bitstreams.get(0), "mix", "compressionScheme", null, "JPG");
        compareBitstreamMetadata(jpeg100Bitstreams.get(0), "mix", "bitsPerSampleValue", null, "8,8,8");
    }

    @Test
    public void testShouldAddTiffMetadataToBitstream() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection magCollection = CollectionBuilder
                .createCollection(context, parentCommunity)
                .withName("MagCollection12")
                .withEntityType("Publication").build();

        String archiveClassPath = "classpath:org/dspace/app/packager/UNIBA_MAG_archive_without_stru.zip";
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

        compareBitstreamMetadata(tiffBitstreams.get(0), "mix", "samplingfrequencyunit", null, "inch");
        compareToNullBitstreamMetadata(tiffBitstreams.get(0), "mix", "samplingfrequencyplane", null);
        compareBitstreamMetadata(tiffBitstreams.get(0), "mix", "xsamplingfrequency", null, "600");
        compareBitstreamMetadata(tiffBitstreams.get(0), "mix", "ysamplingfrequency", null, "600");
        compareBitstreamMetadata(tiffBitstreams.get(0), "mix", "colorSpace", null, "RGB");
        compareBitstreamMetadata(tiffBitstreams.get(0), "mix", "compressionScheme", null, "Uncompressed");
        compareBitstreamMetadata(tiffBitstreams.get(0), "mix", "bitsPerSampleValue", null, "8,8,8");
        compareBitstreamMetadata(tiffBitstreams.get(0), "mix", "captureDevice", null, "");
        compareBitstreamMetadata(tiffBitstreams.get(0), "mix", "scannerManufacturer", null, "i2s");
        compareBitstreamMetadata(tiffBitstreams.get(0), "mix", "scannerModelName", null, "i2s scanner");
        compareBitstreamMetadata(tiffBitstreams.get(0), "mix", "scanningSoftwareName", null, "i2s aquire");
    }

    @Test
    public void testShouldNotCreateCustomerTextBundleWhenTxtFilesNotExistsInDirectory() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection magCollection = CollectionBuilder
                .createCollection(context, parentCommunity)
                .withName("MagCollection13")
                .withEntityType("Publication").build();

        String archiveClassPath = "classpath:org/dspace/app/packager/" +
                "UNIBA_MAG_archive_without_txt_files_and_jpeg100_altimg.zip";
        Resource archive = new DSpace().getServiceManager().getApplicationContext().getResource(archiveClassPath);

        runDSpaceScript("packager",
                "-e", admin.getEmail(),
                "-p", magCollection.getHandle(),
                "-t", "MAG",
                archive.getFile().getPath());

        Iterator<Item> itemsIterator = itemService.findAllByCollection(context, magCollection);
        List<Item> items = IteratorUtils.toList(itemsIterator);
        assertEquals(1, items.size());

        List<Bundle> txtBundles = itemService.getBundles(items.get(0), "CUSTOMER-TEXT");
        assertEquals(0, txtBundles.size());

        List<Bundle> hocrBundles = itemService.getBundles(items.get(0), "CUSTOMER-HOCR");
        assertEquals(1, hocrBundles.size());
    }

    @Test
    public void testShouldNotCreateJpeg100BundleWhenJpeg100AltImgNotExistsInManifest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection magCollection = CollectionBuilder
                .createCollection(context, parentCommunity)
                .withName("MagCollection14")
                .withEntityType("Publication").build();

        String archiveClassPath = "classpath:org/dspace/app/packager/" +
                "UNIBA_MAG_archive_without_txt_files_and_jpeg100_altimg.zip";
        Resource archive = new DSpace().getServiceManager().getApplicationContext().getResource(archiveClassPath);

        runDSpaceScript("packager",
                "-e", admin.getEmail(),
                "-p", magCollection.getHandle(),
                "-t", "MAG",
                archive.getFile().getPath());

        Iterator<Item> itemsIterator = itemService.findAllByCollection(context, magCollection);
        List<Item> items = IteratorUtils.toList(itemsIterator);
        assertEquals(1, items.size());

        List<Bundle> txtBundles = itemService.getBundles(items.get(0), "BRANDED_PREVIEW");
        assertEquals(0, txtBundles.size());
    }

    @Test
    public void testShouldNotAddTXTBitstreamWhenTxtDirectoryDoesNotExist() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection magCollection = CollectionBuilder
                .createCollection(context, parentCommunity)
                .withName("MagCollection15")
                .withEntityType("Publication").build();

        String archiveClassPath = "classpath:org/dspace/app/packager/UNIBA_MAG_archive_without_txt-directory.zip";
        Resource archive = new DSpace().getServiceManager().getApplicationContext().getResource(archiveClassPath);

        runDSpaceScript("packager",
                "-e", admin.getEmail(),
                "-p", magCollection.getHandle(),
                "-t", "MAG",
                archive.getFile().getPath());

        Iterator<Item> itemsIterator = itemService.findAllByCollection(context, magCollection);
        List<Item> items = IteratorUtils.toList(itemsIterator);
        assertEquals(1, items.size());

        List<Bundle> txtBundles = itemService.getBundles(items.get(0), "CUSTOMER-TEXT");
        assertEquals(0, txtBundles.size());

        List<Bundle> hocrBundles = itemService.getBundles(items.get(0), "CUSTOMER-HOCR");
        assertEquals(0, hocrBundles.size());
    }

    private void compareBitstreamMetadata(Bitstream bitstream, String schema, String element, String qualifier,
                                          String expectedValue) {
        String metadataValue = bitstreamService
                .getMetadataFirstValue(bitstream, schema, element, qualifier, Item.ANY);
        assertEquals(expectedValue, metadataValue);
    }

    private void compareItemMetadata(Item item, String schema, String element, String qualifier,
                                          String expectedValue) {
        String metadataValue = itemService
                .getMetadataFirstValue(item, schema, element, qualifier, Item.ANY);
        assertEquals(expectedValue, metadataValue);
    }

    private void compareToNullBitstreamMetadata(Bitstream bitstream, String schema, String element, String qualifier) {
        String metadataValue = bitstreamService
                .getMetadataFirstValue(bitstream, schema, element, qualifier, Item.ANY);
        assertNull(metadataValue);
    }
}
