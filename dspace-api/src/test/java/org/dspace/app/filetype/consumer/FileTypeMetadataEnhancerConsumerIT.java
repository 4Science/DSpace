/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.filetype.consumer;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.function.Predicate;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.tools.ant.filters.StringInputStream;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class FileTypeMetadataEnhancerConsumerIT extends AbstractIntegrationTestWithDatabase {

    private Collection collection;

    private final BitstreamService bitstreamService = ContentServiceFactory.getInstance()
            .getBitstreamService();
    private final ItemService itemService = ContentServiceFactory.getInstance()
            .getItemService();

    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();

        collection = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        context.restoreAuthSystemState();
    }

    @Test
    public void testWithoutBitstreams()
            throws FileNotFoundException, SQLException, AuthorizeException, IOException, ParseException {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).build();
        context.restoreAuthSystemState();
        context.commit();

        item = context.reloadEntity(item);

        assertThat(item.getMetadata(), not(hasItem(with("dc.type", null))));
        assertThat(item.getMetadata(), not(hasItem(with("dspace.file_type", null))));

        context.turnOffAuthorisationSystem();
        this.itemService.update(context, item);
        context.restoreAuthSystemState();

        item = context.reloadEntity(item);

        assertThat(item.getMetadata(), not(hasItem(with("dc.type", null))));
        assertThat(item.getMetadata(), not(hasItem(with("dspace.file.type", null))));
    }

    @Test
    public void testWithoutEntityType()
            throws FileNotFoundException, SQLException, AuthorizeException, IOException, ParseException {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).build();
        Bitstream bitstream = BitstreamBuilder
                    .createBitstream(context, item, new StringInputStream("test"))
                    .build();

        context.restoreAuthSystemState();
        context.commit();

        bitstream = context.reloadEntity(bitstream);
        item = context.reloadEntity(item);

        assertThat(bitstream.getMetadata(), not(hasItem(with("dc.type", null))));
        assertThat(item.getMetadata(), not(hasItem(with("dspace.file.type", null))));
    }

    @Test
    public void testWithEntityTypeDelete()
            throws FileNotFoundException, SQLException, AuthorizeException, IOException, ParseException {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).build();
        Bitstream bitstream =
            BitstreamBuilder
                .createBitstream(context, item, new StringInputStream("test"))
                .build();

        ResourcePolicyBuilder
            .createResourcePolicy(context)
            .withDspaceObject(bitstream)
            .withAction(Constants.READ)
            .withUser(admin)
            .build();

        context.restoreAuthSystemState();
        context.commit();

        context.turnOffAuthorisationSystem();

        this.bitstreamService.delete(context, bitstream);

        context.restoreAuthSystemState();
        context.commit();

        bitstream = context.reloadEntity(bitstream);
        item = context.reloadEntity(item);

        assertThat(bitstream.getMetadata(), not(hasItem(with("dc.type", null))));
        assertThat(item.getMetadata(), not(hasItem(with("dspace.file.type", null))));
    }

    @Test
    public void testWithEntityType()
            throws FileNotFoundException, SQLException, AuthorizeException, IOException, ParseException {
        final String type = "Publication";
        context.turnOffAuthorisationSystem();
        final Item item =
            ItemBuilder
                .createItem(context, collection)
                .build();
        Bitstream bitstream =
            BitstreamBuilder
                .createBitstream(context, item, new StringInputStream("test"))
                .withType(type)
                .build();

        context.restoreAuthSystemState();
        context.commit();

        bitstream = context.reloadEntity(bitstream);

        assertThat(bitstream.getMetadata(), hasItem(with("dc.type", type)));
        assertThat(bitstream.getMetadata(), not(hasItem(with("dspace.file.type", type))));
        assertThat(item.getMetadata(), not(hasItem(with("dc.type", type))));
        assertThat(item.getMetadata(), hasItem(with("dspace.file.type", type)));
    }

    @Test
    public void testWithTypeEdited()
            throws FileNotFoundException, SQLException, AuthorizeException, IOException, ParseException {
        String type = "Publication";
        context.turnOffAuthorisationSystem();
        Item item =
            ItemBuilder
                .createItem(context, collection)
                .build();
        Bitstream bitstream =
            BitstreamBuilder
                .createBitstream(context, item, new StringInputStream("test"))
                .withType(type)
                .build();

        context.restoreAuthSystemState();
        context.commit();

        bitstream = context.reloadEntity(bitstream);
        item = context.reloadEntity(item);

        assertThat(bitstream.getMetadata(), hasItem(with("dc.type", type)));
        assertThat(bitstream.getMetadata(), not(hasItem(with("dspace.file.type", type))));
        assertThat(item.getMetadata(), hasItem(with("dspace.file.type", type)));
        assertThat(item.getMetadata(), not(hasItem(with("dc.type", type))));

        context.turnOffAuthorisationSystem();

        type = "Thesis";
        this.bitstreamService.setMetadataSingleValue(context, bitstream,
                FileTypeMetadataEnhancerConsumer.entityTypeMetadata, null, type);
        this.bitstreamService.update(context, bitstream);

        context.restoreAuthSystemState();
        context.commit();

        bitstream = context.reloadEntity(bitstream);
        item = context.reloadEntity(item);

        assertThat(bitstream.getMetadata(), hasItem(with("dc.type", type)));
        assertThat(bitstream.getMetadata(), not(hasItem(with("dspace.file.type", type))));
        assertThat(item.getMetadata(), hasItem(with("dspace.file.type", type)));
        assertThat(item.getMetadata(), not(hasItem(with("dc.type", type))));
    }

    @Test
    public void testWithTypeDeleted()
            throws FileNotFoundException, SQLException, AuthorizeException, IOException, ParseException {
        final String type = "Publication";
        context.turnOffAuthorisationSystem();
        Item item =
            ItemBuilder
                .createItem(context, collection)
                .build();
        Bitstream bitstream =
            BitstreamBuilder
                .createBitstream(context, item, new StringInputStream("test"))
                .withType(type)
                .build();

        context.restoreAuthSystemState();
        context.commit();

        bitstream = context.reloadEntity(bitstream);
        final MetadataValue entityType = bitstream.getMetadata()
                .stream()
                .filter(metadataFilter(FileTypeMetadataEnhancerConsumer.entityTypeMetadata))
                .findFirst()
                .orElseThrow();
        bitstream.getMetadata().remove(entityType);
        context.turnOffAuthorisationSystem();

        this.bitstreamService.update(context, bitstream);

        context.restoreAuthSystemState();
        context.commit();

        bitstream = context.reloadEntity(bitstream);
        item = context.reloadEntity(item);

        assertThat(bitstream.getMetadata(), not(hasItem(with("dc.type", Mockito.any()))));
        assertThat(item.getMetadata(), not(hasItem(with("dspace.file.type", Mockito.any()))));
    }

    @Test
    public void testWithMultipleEntityType()
            throws FileNotFoundException, SQLException, AuthorizeException, IOException, ParseException {
        final String type = "Publication";
        final String type1 = "Thesis";
        context.turnOffAuthorisationSystem();
        final Item item =
            ItemBuilder
                .createItem(context, collection)
                .build();
        Bitstream bitstream =
            BitstreamBuilder
                .createBitstream(context, item, new StringInputStream("test"))
                .withType(type)
                .build();
        final Bitstream bitstream1 =
            BitstreamBuilder
                .createBitstream(context, item, new StringInputStream("test"))
                .withType(type1)
                .build();

        context.restoreAuthSystemState();
        context.commit();

        bitstream = context.reloadEntity(bitstream);

        assertThat(bitstream.getMetadata(), hasItem(with("dc.type", type)));
        assertThat(bitstream.getMetadata(), not(hasItem(with("dspace.file.type", type))));
        assertThat(bitstream1.getMetadata(), hasItem(with("dc.type", type1)));
        assertThat(bitstream1.getMetadata(), not(hasItem(with("dspace.file.type", type1))));
        assertThat(item.getMetadata(), not(hasItem(with("dc.type", type))));
        assertThat(item.getMetadata(), not(hasItem(with("dc.type", type1))));
        assertThat(item.getMetadata(), hasItem(with("dspace.file.type", type, null, 0, -1)));
        assertThat(item.getMetadata(), hasItem(with("dspace.file.type", type1, null, 1, -1)));
    }

    @Test
    public void testWithMultipleEntityTypeEdited()
            throws FileNotFoundException, SQLException, AuthorizeException, IOException, ParseException {
        String type = "Publication";
        String type1 = "Thesis";
        context.turnOffAuthorisationSystem();
        Item item =
            ItemBuilder
                .createItem(context, collection)
                .build();
        Bitstream bitstream =
            BitstreamBuilder
                .createBitstream(context, item, new StringInputStream("test"))
                .withType(type)
                .build();
        Bitstream bitstream1 =
            BitstreamBuilder
                .createBitstream(context, item, new StringInputStream("test"))
                .withType(type1)
                .build();

        context.restoreAuthSystemState();
        context.commit();

        bitstream = context.reloadEntity(bitstream);
        bitstream1 = context.reloadEntity(bitstream1);

        assertThat(bitstream.getMetadata(), hasItem(with("dc.type", type)));
        assertThat(bitstream.getMetadata(), not(hasItem(with("dspace.file.type", type))));
        assertThat(bitstream1.getMetadata(), hasItem(with("dc.type", type1)));
        assertThat(bitstream1.getMetadata(), not(hasItem(with("dspace.file.type", type1))));
        assertThat(item.getMetadata(), not(hasItem(with("dc.type", type))));
        assertThat(item.getMetadata(), not(hasItem(with("dc.type", type1))));
        assertThat(item.getMetadata(), hasItem(with("dspace.file.type", type, null, 0, -1)));
        assertThat(item.getMetadata(), hasItem(with("dspace.file.type", type1, null, 1, -1)));

        context.turnOffAuthorisationSystem();

        type = "Journal";
        this.bitstreamService.setMetadataSingleValue(
            context,
            bitstream,
            FileTypeMetadataEnhancerConsumer.entityTypeMetadata,
            null,
            type
        );
        this.bitstreamService.update(context, bitstream);

        type1 = "Journal Article";
        this.bitstreamService.setMetadataSingleValue(
            context,
            bitstream1,
            FileTypeMetadataEnhancerConsumer.entityTypeMetadata,
            null,
            type1
        );
        this.bitstreamService.update(context, bitstream1);

        context.restoreAuthSystemState();
        context.commit();

        bitstream = context.reloadEntity(bitstream);
        bitstream1 = context.reloadEntity(bitstream1);
        item = context.reloadEntity(item);

        assertThat(bitstream.getMetadata(), hasItem(with("dc.type", type)));
        assertThat(bitstream.getMetadata(), not(hasItem(with("dspace.file.type", type))));
        assertThat(bitstream1.getMetadata(), hasItem(with("dc.type", type1)));
        assertThat(bitstream1.getMetadata(), not(hasItem(with("dspace.file.type", type1))));
        assertThat(item.getMetadata(), not(hasItem(with("dc.type", type))));
        assertThat(item.getMetadata(), not(hasItem(with("dc.type", type1))));
        assertThat(item.getMetadata(), hasItem(with("dspace.file.type", type, null, 0, -1)));
        assertThat(item.getMetadata(), hasItem(with("dspace.file.type", type1, null, 1, -1)));
    }

    @Test
    public void testWithMultipleEntityTypeDelete()
            throws FileNotFoundException, SQLException, AuthorizeException, IOException, ParseException {
        final String type = "Publication";
        final String type1 = "Thesis";
        context.turnOffAuthorisationSystem();
        Item item =
            ItemBuilder
                .createItem(context, collection)
                .build();
        Bitstream bitstream =
            BitstreamBuilder
                .createBitstream(context, item, new StringInputStream("test"))
                .withType(type)
                .build();
        Bitstream bitstream1 =
            BitstreamBuilder
                .createBitstream(context, item, new StringInputStream("test"))
                .withType(type1)
                .build();

        context.restoreAuthSystemState();
        context.commit();

        bitstream = context.reloadEntity(bitstream);
        bitstream1 = context.reloadEntity(bitstream1);

        assertThat(bitstream.getMetadata(), hasItem(with("dc.type", type)));
        assertThat(bitstream.getMetadata(), not(hasItem(with("dspace.file.type", type))));
        assertThat(bitstream1.getMetadata(), hasItem(with("dc.type", type1)));
        assertThat(bitstream1.getMetadata(), not(hasItem(with("dspace.file.type", type1))));
        assertThat(item.getMetadata(), not(hasItem(with("dc.type", type))));
        assertThat(item.getMetadata(), not(hasItem(with("dc.type", type1))));
        assertThat(item.getMetadata(), hasItem(with("dspace.file.type", type, null, 0, -1)));
        assertThat(item.getMetadata(), hasItem(with("dspace.file.type", type1, null, 1, -1)));

        context.turnOffAuthorisationSystem();

        this.bitstreamService.clearMetadata(
            context,
            bitstream,
            FileTypeMetadataEnhancerConsumer.entityTypeMetadata.schema,
            FileTypeMetadataEnhancerConsumer.entityTypeMetadata.element,
            FileTypeMetadataEnhancerConsumer.entityTypeMetadata.qualifier,
            null
        );
        this.bitstreamService.update(context, bitstream);

        context.restoreAuthSystemState();
        context.commit();

        bitstream = context.reloadEntity(bitstream);
        bitstream1 = context.reloadEntity(bitstream1);
        item = context.reloadEntity(item);

        assertThat(bitstream.getMetadata(), not(hasItem(with("dc.type", type))));
        assertThat(bitstream.getMetadata(), not(hasItem(with("dspace.file.type", type))));
        assertThat(bitstream1.getMetadata(), hasItem(with("dc.type", type1)));
        assertThat(bitstream1.getMetadata(), not(hasItem(with("dspace.file.type", type1))));
        assertThat(item.getMetadata(), not(hasItem(with("dc.type", type))));
        assertThat(item.getMetadata(), not(hasItem(with("dc.type", type1))));
        assertThat(item.getMetadata(), not(hasItem(with("dspace.file.type", type, null, 0, -1))));
        assertThat(item.getMetadata(), not(hasItem(with("dspace.file.type", type1, null, 1, -1))));
        assertThat(item.getMetadata(), hasItem(with("dspace.file.type", type1, null, 0, -1)));
    }

    private Predicate<? super MetadataValue> metadataFilter(MetadataFieldName metadataField) {
        return metadata ->
                StringUtils.equals(metadataField.schema, metadata.getSchema()) &&
                StringUtils.equals(metadataField.element, metadata.getElement()) &&
                StringUtils.equals(metadataField.qualifier, metadata.getQualifier());
    }
}
