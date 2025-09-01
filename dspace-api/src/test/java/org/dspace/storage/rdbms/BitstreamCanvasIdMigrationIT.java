/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;


import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.MetadataFieldBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.rdbms.migration.MigrationUtils;
import org.junit.Test;

/**
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class BitstreamCanvasIdMigrationIT extends AbstractIntegrationTestWithDatabase {

    private static final DataSource dataSource = DSpaceServicesFactory.getInstance()
                                                                      .getServiceManager()
                                                                      .getServiceByName("dataSource", DataSource.class);
    private final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

    @Test
    public void testBitstreamCanvasIdMigration() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("Com").build();
        Collection collection = CollectionBuilder.createCollection(context, community).withName("Col").build();
        MetadataFieldBuilder
            .createMetadataField(context, "bitstream", "iiif", "canvasid")
            .build();
        // Create test content: Item, Bundle, Bitstreams, Metadata...
        Item item = ItemBuilder.createItem(context, collection)
                               .withMetadata("dspace", "iiif", "enabled", "true")
                               .withTitle("Item with IIIF")
                               .build();

        Bundle originalBundle = BundleBuilder.createBundle(context, item)
                                             .withName("ORIGINAL")
                                             .build();
        Bitstream originalPdfBitstream =
            BitstreamBuilder.createBitstream(context, originalBundle, InputStream.nullInputStream())
                            .withName("dummy.pdf")
                            .build();

        Bitstream originalRawBitstream =
            BitstreamBuilder.createBitstream(context, originalBundle, InputStream.nullInputStream())
                            .withName("image.cr2")
                            .build();

        Bundle pdfBundle = BundleBuilder.createBundle(context, item)
                                        .withName("IIIF-PDF-" + originalPdfBitstream.getID())
                                        .build();



        Bitstream firstPDFBitstream =
            BitstreamBuilder.createBitstream(context, pdfBundle, InputStream.nullInputStream())
                            .withName("first")
                            .build();

        Bitstream secondPDFBitstream =
            BitstreamBuilder.createBitstream(context, pdfBundle, InputStream.nullInputStream())
                            .withName("second")
                            .build();


        Bundle rawBundle = BundleBuilder.createBundle(context, item)
                                        .withName("IIIF-RAW-ACCESS")
                                        .build();
        Bitstream masterRawBitstream =
            BitstreamBuilder.createBitstream(context, rawBundle, InputStream.nullInputStream())
                            .withName("master")
                .withMetadata("bitstream", "master", null, originalRawBitstream.getID().toString())
                            .build();

        Bitstream rawBitstream =
            BitstreamBuilder.createBitstream(context, rawBundle, InputStream.nullInputStream())
                            .withName("raw")
                            .build();

        context.commit();
        context.restoreAuthSystemState();


        try (Connection connection = dataSource.getConnection()) {
            String dataMigrateSQL = MigrationUtils.getResourceAsString(

                "org/dspace/storage/rdbms/sqlmigration/h2/V8.0_2025.07.01__Add_canvasid_metadata.sql");

            DatabaseUtils.executeSql(connection, dataMigrateSQL);

            connection.commit();
        }


        Bitstream reloadedPdfBitstream = bitstreamService.find(context, originalPdfBitstream.getID());

        List<MetadataValue> canvasIdMetadata =
            bitstreamService.getMetadata(reloadedPdfBitstream, "bitstream", "iiif", "canvasid",
                                         Item.ANY);

        assertEquals("Should have 1 canvasid metadata", 1, canvasIdMetadata.size());

        String expectedCanvasId = firstPDFBitstream.getID().toString();
        assertEquals("Canvas ID should match bitstream ID", expectedCanvasId, canvasIdMetadata.get(0).getValue());


        Bitstream reloadedRawBitstream = bitstreamService.find(context, originalRawBitstream.getID());
        List<MetadataValue> canvasIdMetadata2 =
            bitstreamService.getMetadata(reloadedRawBitstream, "bitstream", "iiif", "canvasid",
                                         Item.ANY);

        assertEquals("Should have 1 canvasid metadata", 1, canvasIdMetadata2.size());

        String expectedCanvasId2 = masterRawBitstream.getID().toString();
        assertEquals("Canvas ID should match bitstream ID", expectedCanvasId2, canvasIdMetadata2.get(0).getValue());
    }

}