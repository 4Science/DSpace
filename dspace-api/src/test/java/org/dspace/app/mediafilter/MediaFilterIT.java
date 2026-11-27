/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests of {@link MediaFilterScript}.
 *
 * @author Andrea Bollini <andrea.bollini at 4science.com>
 */
public class MediaFilterIT extends AbstractIntegrationTestWithDatabase {

    private static final long HALF_YEAR_TIME = 180l * 24l * 60l * 60000l;
    private static final long ONE_YEAR_TIME = 360l * 24l * 60l * 60000l;
    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    private IndexingService indexingService = DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName(IndexingService.class.getName(), IndexingService.class);
    protected Community topComm1;
    protected Community topComm2;
    protected Community childComm1_1;
    protected Community childComm1_2;
    protected Collection col1_1;
    protected Collection col1_2;
    protected Collection col1_1_1;
    protected Collection col1_1_2;
    protected Collection col1_2_1;
    protected Collection col1_2_2;
    protected Collection col2_1;
    protected Item item1_1_a;
    protected Item item1_1_b;
    protected Item item1_2_a;
    protected Item item1_2_b;
    protected Item item1_1_1_a;
    protected Item item1_1_1_b;
    protected Item item1_1_2_a;
    protected Item item1_1_2_b;
    protected Item item1_2_1_a;
    protected Item item1_2_1_b;
    protected Item item1_2_2_a;
    protected Item item1_2_2_b;
    protected Item item2_1_a;
    protected Item item2_1_b;

    final long nowTime = new Date().getTime();
    final Date oldestModifiedDate = new Date(nowTime - ONE_YEAR_TIME);
    final Date midModifiedDate = new Date(new Date().getTime() - HALF_YEAR_TIME);

    @Before
    public void setup() throws IOException, SQLException, AuthorizeException, SearchServiceException {
        context.turnOffAuthorisationSystem();
        topComm1 = CommunityBuilder.createCommunity(context).withName("Parent Community1").build();
        topComm2 = CommunityBuilder.createCommunity(context).withName("Parent Community2").build();
        childComm1_1 = CommunityBuilder.createCommunity(context).withName("Child Community1_1")
                .addParentCommunity(context, topComm1).build();
        childComm1_2 = CommunityBuilder.createCommunity(context).withName("Child Community1_2")
                .addParentCommunity(context, topComm1).build();
        col1_1 = CollectionBuilder.createCollection(context, topComm1).withName("Collection 1_1").build();
        col1_2 = CollectionBuilder.createCollection(context, topComm1).withName("Collection 1_2").build();
        col1_1_1 = CollectionBuilder.createCollection(context, childComm1_1).withName("Collection 1_1_1").build();
        col1_1_2 = CollectionBuilder.createCollection(context, childComm1_1).withName("Collection 1_1_2").build();
        col1_2_1 = CollectionBuilder.createCollection(context, childComm1_2).withName("Collection 1_1_1").build();
        col1_2_2 = CollectionBuilder.createCollection(context, childComm1_2).withName("Collection 1_2").build();
        col2_1 = CollectionBuilder.createCollection(context, topComm2).withName("Collection 2_1").build();

        // Create two items in each collection, one with the test.csv file and one with the test.txt file
        item1_1_a = ItemBuilder.createItem(context, col1_1).withTitle("Item 1_1_a").withIssueDate("2017-10-17").build();
        item1_1_b = ItemBuilder.createItem(context, col1_1).withTitle("Item 1_1_b").withIssueDate("2017-10-17").build();
        item1_1_1_a = ItemBuilder.createItem(context, col1_1_1).withTitle("Item 1_1_1_a").withIssueDate("2017-10-17")
                .build();
        item1_1_1_b = ItemBuilder.createItem(context, col1_1_1).withTitle("Item 1_1_1_b").withIssueDate("2017-10-17")
                .build();
        item1_1_2_a = ItemBuilder.createItem(context, col1_1_2).withTitle("Item 1_1_2_a").withIssueDate("2017-10-17")
                .build();
        item1_1_2_b = ItemBuilder.createItem(context, col1_1_2).withTitle("Item 1_1_2_b").withIssueDate("2017-10-17")
                .build();
        item1_2_a = ItemBuilder.createItem(context, col1_2).withTitle("Item 1_2_a").withIssueDate("2017-10-17").build();
        item1_2_b = ItemBuilder.createItem(context, col1_2).withTitle("Item 1_2_b").withIssueDate("2017-10-17").build();
        item1_2_1_a = ItemBuilder.createItem(context, col1_2_1).withTitle("Item 1_2_1_a").withIssueDate("2017-10-17")
                .build();
        item1_2_1_b = ItemBuilder.createItem(context, col1_2_1).withTitle("Item 1_2_1_b").withIssueDate("2017-10-17")
                .build();
        item1_2_2_a = ItemBuilder.createItem(context, col1_2_2).withTitle("Item 1_2_2_a").withIssueDate("2017-10-17")
                .build();
        item1_2_2_b = ItemBuilder.createItem(context, col1_2_2).withTitle("Item 1_2_2_b").withIssueDate("2017-10-17")
                .build();
        item2_1_a = ItemBuilder.createItem(context, col2_1).withTitle("Item 2_1_a").withIssueDate("2017-10-17")
                .build();
        item2_1_b = ItemBuilder.createItem(context, col2_1).withTitle("Item 2_1_b").withIssueDate("2017-10-17")
                .build();
        item2_1_a = Mockito.spy(item2_1_a);
        item2_1_b = Mockito.spy(item2_1_b);
        addBitstream(item1_1_a, "test.csv");
        addBitstream(item1_1_b, "test.txt");
        addBitstream(item1_2_a, "test.csv");
        addBitstream(item1_2_b, "test.txt");
        addBitstream(item1_1_1_a, "test.csv");
        addBitstream(item1_1_1_b, "test.txt");
        addBitstream(item1_1_2_a, "test.csv");
        addBitstream(item1_1_2_b, "test.txt");
        addBitstream(item1_2_1_a, "test.csv");
        addBitstream(item1_2_1_b, "test.txt");
        addBitstream(item1_2_2_a, "test.csv");
        addBitstream(item1_2_2_b, "test.txt");
        addBitstream(item2_1_a, "test.csv");
        addBitstream(item2_1_b, "test.txt");

        // alter the last modified date for two items in the solr index
        Mockito.when(item2_1_a.getLastModified()).thenReturn(midModifiedDate);
        Mockito.when(item2_1_b.getLastModified()).thenReturn(oldestModifiedDate);
        indexingService.indexContent(context, new IndexableItem(item2_1_a), true);
        indexingService.indexContent(context, new IndexableItem(item2_1_b), true);
        indexingService.commit();

        context.restoreAuthSystemState();
    }

    private void addBitstream(Item item, String filename) throws SQLException, AuthorizeException, IOException {
        BitstreamBuilder.createBitstream(context, item, getClass().getResourceAsStream(filename)).withName(filename)
                .guessFormat().build();
    }

    @Test
    public void mediaFilterScriptAllItemsTest() throws Exception {
        performMediaFilterScript((Item) null);
        Iterator<Item> items = itemService.findAll(context);
        while (items.hasNext()) {
            Item item = items.next();
            checkItemHasBeenProcessed(item);
        }
    }

    @Test
    public void mediaFilterScriptAllItemsLastUpdatedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        BitstreamBuilder.createBitstream(context, item1_1_a, IOUtils.toInputStream("placeholder"), "TEXT")
                .withName("placeholder.txt").guessFormat().build();
        BitstreamBuilder.createBitstream(context, item1_1_b, IOUtils.toInputStream("placeholder"), "THUMBNAIL")
                .withName("placeholder.txt").guessFormat().build();
        context.restoreAuthSystemState();
        // we expect the item2_1_b to be skipped
        performMediaFilterScript(String.valueOf(180 + 10));
        checkItemHasBeenNotProcessed(item2_1_b);
        checkItemHasBeenProcessed(item2_1_a);
        // run the script again considering a large time period
        // we expect now also item2_1_b to be processed
        performMediaFilterScript(String.valueOf(360 + 10));
        checkItemHasBeenProcessed(item2_1_b);
    }

    @Test
    public void mediaFilterScriptAllItemsSkipBundleTest() throws Exception {
        context.turnOffAuthorisationSystem();
        BitstreamBuilder.createBitstream(context, item1_1_a, IOUtils.toInputStream("placeholder"), "TEXT")
                .withName("placeholder.txt").guessFormat().build();
        BitstreamBuilder.createBitstream(context, item1_1_b, IOUtils.toInputStream("placeholder"), "THUMBNAIL")
                .withName("placeholder.txt").guessFormat().build();
        context.restoreAuthSystemState();
        performMediaFilterScript("TEXT", "THUMBNAIL");
        Iterator<Item> items = itemService.findAll(context);
        while (items.hasNext()) {
            Item item = items.next();
            String bundlePlaceholder = null;
            if (StringUtils.equals(item.getName(), "Item 1_1_a")) {
                bundlePlaceholder = "TEXT";
            } else if (StringUtils.equals(item.getName(), "Item 1_1_b")) {
                bundlePlaceholder = "THUMBNAIL";
            }
            if (bundlePlaceholder != null) {
                checkItemHasDerivativePlaceholder(item, bundlePlaceholder);
            } else {
                checkItemHasBeenProcessed(item);
            }
        }
    }

    @Test
    public void mediaFilterScriptAllItemsSkipBundleLastModifiedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        BitstreamBuilder.createBitstream(context, item1_1_a, IOUtils.toInputStream("placeholder"), "TEXT")
                .withName("placeholder.txt").guessFormat().build();
        BitstreamBuilder.createBitstream(context, item1_1_b, IOUtils.toInputStream("placeholder"), "THUMBNAIL")
                .withName("placeholder.txt").guessFormat().build();
        context.restoreAuthSystemState();
        // we expect the item2_1_b to be skipped
        performMediaFilterScript(String.valueOf(180 + 10), "TEXT", "THUMBNAIL");
        Iterator<Item> items = itemService.findAll(context);
        while (items.hasNext()) {
            Item item = items.next();
            String bundlePlaceholder = null;
            if (StringUtils.equals(item.getName(), "Item 1_1_a")) {
                bundlePlaceholder = "TEXT";
            } else if (StringUtils.equals(item.getName(), "Item 1_1_b")) {
                bundlePlaceholder = "THUMBNAIL";
            }

            if (StringUtils.equals(item.getName(), "Item 2_1_b")) {
                checkItemHasBeenNotProcessed(item);
            } else if (bundlePlaceholder != null) {
                checkItemHasDerivativePlaceholder(item, bundlePlaceholder);
            } else {
                checkItemHasBeenProcessed(item);
            }
        }
        // run the script again considering a large time period
        // we expect now also item2_1_b to be processed
        performMediaFilterScript(String.valueOf(360 + 10), "TEXT", "THUMBNAIL");
        checkItemHasBeenProcessed(item2_1_b);
    }

    @Test
    public void mediaFilterScriptIdentifiersTest() throws Exception {
        // process the item 1_1_a and verify that no other items has been processed using the "closer" one
        performMediaFilterScript(item1_1_a);
        checkItemHasBeenProcessed(item1_1_a);
        checkItemHasBeenNotProcessed(item1_1_b);
        // process the collection 1_1_1 and verify that items in another collection has not been processed
        performMediaFilterScript(col1_1_1);
        checkItemHasBeenProcessed(item1_1_1_a);
        checkItemHasBeenProcessed(item1_1_1_b);
        checkItemHasBeenNotProcessed(item1_1_2_a);
        checkItemHasBeenNotProcessed(item1_1_2_b);
        // process a top community with only collections
        performMediaFilterScript(topComm2);
        checkItemHasBeenProcessed(item2_1_a);
        checkItemHasBeenProcessed(item2_1_b);
        // verify that the other items have not been processed yet
        checkItemHasBeenNotProcessed(item1_1_b);
        checkItemHasBeenNotProcessed(item1_2_a);
        checkItemHasBeenNotProcessed(item1_2_b);
        checkItemHasBeenNotProcessed(item1_1_2_a);
        checkItemHasBeenNotProcessed(item1_1_2_b);
        checkItemHasBeenNotProcessed(item1_2_1_a);
        checkItemHasBeenNotProcessed(item1_2_1_b);
        checkItemHasBeenNotProcessed(item1_2_2_a);
        checkItemHasBeenNotProcessed(item1_2_2_b);
        // process a more structured community and verify that all the items at all levels are processed
        performMediaFilterScript(topComm1);
        // items that were already processed should stay processed
        checkItemHasBeenProcessed(item1_1_a);
        checkItemHasBeenProcessed(item1_1_1_a);
        checkItemHasBeenProcessed(item1_1_1_b);
        // residual items should have been processed as well now
        checkItemHasBeenProcessed(item1_1_b);
        checkItemHasBeenProcessed(item1_2_a);
        checkItemHasBeenProcessed(item1_2_b);
        checkItemHasBeenProcessed(item1_1_2_a);
        checkItemHasBeenProcessed(item1_1_2_b);
        checkItemHasBeenProcessed(item1_2_1_a);
        checkItemHasBeenProcessed(item1_2_1_b);
        checkItemHasBeenProcessed(item1_2_2_a);
        checkItemHasBeenProcessed(item1_2_2_b);
    }

    private void checkItemHasBeenNotProcessed(Item item) throws IOException, SQLException, AuthorizeException {
        List<Bundle> textBundles = item.getBundles("TEXT");
        assertTrue("The item " + item.getName() + " should NOT have the TEXT bundle", textBundles.size() == 0);
    }

    private void checkItemHasBeenProcessed(Item item) throws IOException, SQLException, AuthorizeException {
        String expectedFileName = StringUtils.endsWith(item.getName(), "_a") ? "test.csv.txt" : "test.txt.txt";
        String expectedContent = StringUtils.endsWith(item.getName(), "_a") ? "data3,3" : "quick brown fox";
        List<Bundle> textBundles = item.getBundles("TEXT");
        assertTrue("The item " + item.getName() + " has the TEXT bundle", textBundles.size() == 1);
        List<Bitstream> bitstreams = textBundles.get(0).getBitstreams();
        assertTrue("The item " + item.getName() + " has exactly 1 bitstream in the TEXT bundle",
                bitstreams.size() == 1);
        assertTrue("The text bistream in the " + item.getName() + " is named properly [" + expectedFileName + "]",
                StringUtils.equals(bitstreams.get(0).getName(), expectedFileName));
        assertTrue("The text bistream in the " + item.getName() + " contains the proper content ["
                + expectedContent + "]", StringUtils.contains(getContent(bitstreams.get(0)), expectedContent));
    }

    private void checkItemHasDerivativePlaceholder(Item item, String placeholder)
            throws IOException, SQLException, AuthorizeException {
        String expectedFileName = "placeholder.txt";
        String expectedContent = "placeholder";
        List<Bundle> textBundles = item.getBundles(placeholder);
        assertTrue("The item " + item.getName() + " has the placeholder bundle", textBundles.size() == 1);
        List<Bitstream> bitstreams = textBundles.get(0).getBitstreams();
        assertTrue("The item " + item.getName() + " has exactly 1 bitstream in the " + placeholder + " bundle",
                bitstreams.size() == 1);
        assertTrue("The text bistream in the " + item.getName() + " is named properly [" + expectedFileName + "]",
                StringUtils.equals(bitstreams.get(0).getName(), expectedFileName));
        assertTrue("The text bistream in the " + item.getName() + " contains the proper content ["
                + expectedContent + "]", StringUtils.contains(getContent(bitstreams.get(0)), expectedContent));
    }

    private CharSequence getContent(Bitstream bitstream) throws IOException, SQLException, AuthorizeException {
        try (InputStream input = bitstreamService.retrieve(context, bitstream)) {
            return IOUtils.toString(input, "UTF-8");
        }
    }

    private void performMediaFilterScript(String updatedSincedays) throws Exception {
        runDSpaceScript("filter-media", "-l", updatedSincedays);
        reloadAllItems();
    }

    private void performMediaFilterScript(String updatedSincedays, String skipBundle1, String skipBundle2)
            throws Exception {
        runDSpaceScript("filter-media", "-l", updatedSincedays, "-b", skipBundle1, "-b", skipBundle2);
        reloadAllItems();
    }

    private void performMediaFilterScript(String skipBundle1, String skipBundle2) throws Exception {
        runDSpaceScript("filter-media", "-b", skipBundle1, "-b", skipBundle2);
        reloadAllItems();

    }

    private void performMediaFilterScript(DSpaceObject dso) throws Exception {
        if (dso != null) {
            runDSpaceScript("filter-media", "-i", dso.getHandle());
        } else {
            runDSpaceScript("filter-media");
        }
        reloadAllItems();

    }

    private void reloadAllItems() throws SQLException {
        // reload our items to see the changes
        item1_1_a = context.reloadEntity(item1_1_a);
        item1_1_b = context.reloadEntity(item1_1_b);
        item1_2_a = context.reloadEntity(item1_2_a);
        item1_2_b = context.reloadEntity(item1_2_b);
        item1_1_1_a = context.reloadEntity(item1_1_1_a);
        item1_1_1_b = context.reloadEntity(item1_1_1_b);
        item1_1_2_a = context.reloadEntity(item1_1_2_a);
        item1_1_2_b = context.reloadEntity(item1_1_2_b);
        item1_2_1_a = context.reloadEntity(item1_2_1_a);
        item1_2_1_b = context.reloadEntity(item1_2_1_b);
        item1_2_2_a = context.reloadEntity(item1_2_2_a);
        item1_2_2_b = context.reloadEntity(item1_2_2_b);
        item2_1_a = context.reloadEntity(item2_1_a);
        item2_1_b = context.reloadEntity(item2_1_b);
    }
}
