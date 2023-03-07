/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields.postprocessors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.integration.crosswalks.csl.CSLResult;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.util.SimpleMapConverter;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link GroupByTypeCitationsPostProcessor}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class GroupByTypeCitationsPostProcessorTest {

    private Context context = mock(Context.class);

    private Item item = mock(Item.class);

    private ItemService itemService = mock(ItemService.class);

    private GroupByTypeCitationsPostProcessor processor;

    @Before
    public void setup() {
        processor = new GroupByTypeCitationsPostProcessor();
        processor.setItemService(itemService);
    }

    @Test
    public void testWithoutFixedOrder() throws SQLException {

        UUID[] itemIds = new UUID[] { UUID.fromString("1cec205c-a965-4199-8ff0-dd1263eb275d"),
            UUID.fromString("2c7cda5e-04e1-49ec-b6e6-871efa158de0"),
            UUID.fromString("34b5e840-e5ff-42c0-963a-7f4571218a38"),
            UUID.fromString("47827560-6300-49ce-873d-fdb55f6d184d"),
            UUID.fromString("5dc27d99-5e92-4fc6-ba6b-ffa37923b5ce"),
            UUID.fromString("6c9c29d0-95e8-493c-ab51-d13e55c1de7e"),
            UUID.fromString("7510a7cb-f02b-4352-b5f9-e04be7d4c351"),
            UUID.fromString("872ce199-a66f-4321-86f1-61892abcb632") };

        String[] citations = new String[] {
            "<fo:block>Publication 1 title<fo:block/>",
            "<fo:block>Publication 2 title<fo:block/>",
            "<fo:block>Publication 3 title<fo:block/>",
            "<fo:block>Publication 4 title<fo:block/>",
            "<fo:block>Publication 5 title<fo:block/>",
            "<fo:block>Publication 6 title<fo:block/>",
            "<fo:block>Publication 7 title<fo:block/>",
            "<fo:block>Publication 8 title<fo:block/>"
        };

        createItemMockWithType(itemIds[0], "book");
        createItemMockWithType(itemIds[1], "speech");
        createItemMockWithType(itemIds[2], "conference");
        createItemMockWithType(itemIds[3], "article");
        createItemMockWithType(itemIds[4], null);
        createItemMockWithType(itemIds[5], "article");
        createItemMockWithType(itemIds[6], "book");
        createItemMockWithType(itemIds[7], null);

        CSLResult result = processor.process(context, item, new CSLResult("fo", itemIds, citations));
        assertThat(result, notNullValue());

        assertThat(result.getFormat(), is("fo"));
        assertThat(result.getItemIds(), arrayContaining(itemIds[3], itemIds[5], itemIds[0],
            itemIds[6], itemIds[2], itemIds[1], itemIds[4], itemIds[7]));

        assertThat(result.getCitationEntries(), arrayContaining(
            "<fo:block font-size=\"12pt\" font-weight=\"bold\" margin-top=\"4mm\" >Article</fo:block>"
                + "<fo:block>Publication 4 title<fo:block/>",
            "<fo:block>Publication 6 title<fo:block/>",
            "<fo:block font-size=\"12pt\" font-weight=\"bold\" margin-top=\"4mm\" >Book</fo:block>"
                + "<fo:block>Publication 1 title<fo:block/>",
            "<fo:block>Publication 7 title<fo:block/>",
            "<fo:block font-size=\"12pt\" font-weight=\"bold\" margin-top=\"4mm\" >Conference</fo:block>"
                + "<fo:block>Publication 3 title<fo:block/>",
            "<fo:block font-size=\"12pt\" font-weight=\"bold\" margin-top=\"4mm\" >Speech</fo:block>"
                + "<fo:block>Publication 2 title<fo:block/>",
            "<fo:block font-size=\"12pt\" font-weight=\"bold\" margin-top=\"4mm\" >Other</fo:block>"
                + "<fo:block>Publication 5 title<fo:block/>",
            "<fo:block>Publication 8 title<fo:block/>"));

    }

    @Test
    public void testWithFixedOrder() throws SQLException {

        UUID[] itemIds = new UUID[] { UUID.fromString("1cec205c-a965-4199-8ff0-dd1263eb275d"),
            UUID.fromString("2c7cda5e-04e1-49ec-b6e6-871efa158de0"),
            UUID.fromString("34b5e840-e5ff-42c0-963a-7f4571218a38"),
            UUID.fromString("47827560-6300-49ce-873d-fdb55f6d184d"),
            UUID.fromString("5dc27d99-5e92-4fc6-ba6b-ffa37923b5ce"),
            UUID.fromString("6c9c29d0-95e8-493c-ab51-d13e55c1de7e"),
            UUID.fromString("7510a7cb-f02b-4352-b5f9-e04be7d4c351"),
            UUID.fromString("872ce199-a66f-4321-86f1-61892abcb632") };

        String[] citations = new String[] {
            "<fo:block>Publication 1 title<fo:block/>",
            "<fo:block>Publication 2 title<fo:block/>",
            "<fo:block>Publication 3 title<fo:block/>",
            "<fo:block>Publication 4 title<fo:block/>",
            "<fo:block>Publication 5 title<fo:block/>",
            "<fo:block>Publication 6 title<fo:block/>",
            "<fo:block>Publication 7 title<fo:block/>",
            "<fo:block>Publication 8 title<fo:block/>"
        };

        createItemMockWithType(itemIds[0], "book");
        createItemMockWithType(itemIds[1], "speech");
        createItemMockWithType(itemIds[2], "conference");
        createItemMockWithType(itemIds[3], "article");
        createItemMockWithType(itemIds[4], null);
        createItemMockWithType(itemIds[5], "article");
        createItemMockWithType(itemIds[6], "book");
        createItemMockWithType(itemIds[7], null);

        processor.setFixedTypesOrder(List.of("speech", "conference", "book review"));

        CSLResult result = processor.process(context, item, new CSLResult("fo", itemIds, citations));
        assertThat(result, notNullValue());

        assertThat(result.getFormat(), is("fo"));
        assertThat(result.getItemIds(), arrayContaining(itemIds[1], itemIds[2], itemIds[3], itemIds[5], itemIds[0],
            itemIds[6], itemIds[4], itemIds[7]));

        assertThat(result.getCitationEntries(), arrayContaining(
            "<fo:block font-size=\"12pt\" font-weight=\"bold\" margin-top=\"4mm\" >Speech</fo:block>"
                + "<fo:block>Publication 2 title<fo:block/>",
            "<fo:block font-size=\"12pt\" font-weight=\"bold\" margin-top=\"4mm\" >Conference</fo:block>"
                + "<fo:block>Publication 3 title<fo:block/>",
            "<fo:block font-size=\"12pt\" font-weight=\"bold\" margin-top=\"4mm\" >Article</fo:block>"
                + "<fo:block>Publication 4 title<fo:block/>",
            "<fo:block>Publication 6 title<fo:block/>",
            "<fo:block font-size=\"12pt\" font-weight=\"bold\" margin-top=\"4mm\" >Book</fo:block>"
                + "<fo:block>Publication 1 title<fo:block/>",
            "<fo:block>Publication 7 title<fo:block/>",
            "<fo:block font-size=\"12pt\" font-weight=\"bold\" margin-top=\"4mm\" >Other</fo:block>"
                + "<fo:block>Publication 5 title<fo:block/>",
            "<fo:block>Publication 8 title<fo:block/>"));

    }

    @Test
    public void testWithNoFoFormatAndTypeHeaderAdditionEnabled() throws SQLException {

        UUID[] itemIds = new UUID[] { UUID.fromString("1cec205c-a965-4199-8ff0-dd1263eb275d"),
            UUID.fromString("2c7cda5e-04e1-49ec-b6e6-871efa158de0"),
            UUID.fromString("34b5e840-e5ff-42c0-963a-7f4571218a38") };

        String[] citations = new String[] {
            "<fo:block>Publication 1 title<fo:block/>",
            "<fo:block>Publication 2 title<fo:block/>",
            "<fo:block>Publication 3 title<fo:block/>"
        };

        createItemMockWithType(itemIds[0], "book");
        createItemMockWithType(itemIds[1], "speech");
        createItemMockWithType(itemIds[2], "conference");

        processor.setTypeHeaderAdditionEnabled(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> processor.process(context, item, new CSLResult("text", itemIds, citations)));

        assertThat(exception.getMessage(), is("Only CSLResult related to fo format is supports type header addition"));

    }

    @Test
    public void testWithNoFoFormatAndTypeHeaderAdditionDisabled() throws SQLException {

        UUID[] itemIds = new UUID[] { UUID.fromString("1cec205c-a965-4199-8ff0-dd1263eb275d"),
            UUID.fromString("2c7cda5e-04e1-49ec-b6e6-871efa158de0"),
            UUID.fromString("34b5e840-e5ff-42c0-963a-7f4571218a38") };

        String[] citations = new String[] { "Publication 1 title", "Publication 2 title", "Publication 3 title" };

        createItemMockWithType(itemIds[0], "book");
        createItemMockWithType(itemIds[1], "speech");
        createItemMockWithType(itemIds[2], "book");

        processor.setTypeHeaderAdditionEnabled(false);

        CSLResult result = processor.process(context, item, new CSLResult("text", itemIds, citations));
        assertThat(result, notNullValue());

        assertThat(result.getFormat(), is("text"));
        assertThat(result.getItemIds(), arrayContaining(itemIds[0], itemIds[2], itemIds[1]));

        assertThat(result.getCitationEntries(), arrayContaining(
            "Publication 1 title", "Publication 3 title", "Publication 2 title"));

    }

    @Test
    public void testWithoutItems() throws SQLException {

        UUID[] itemIds = new UUID[] {};

        String[] citations = new String[] {};

        CSLResult result = processor.process(context, item, new CSLResult("fo", itemIds, citations));
        assertThat(result, notNullValue());

        assertThat(result.getFormat(), is("fo"));
        assertThat(result.getItemIds(), emptyArray());
        assertThat(result.getCitationEntries(), emptyArray());
    }

    @Test
    public void testWithMapConverter() throws SQLException {

        UUID[] itemIds = new UUID[] { UUID.fromString("1cec205c-a965-4199-8ff0-dd1263eb275d"),
            UUID.fromString("2c7cda5e-04e1-49ec-b6e6-871efa158de0"),
            UUID.fromString("34b5e840-e5ff-42c0-963a-7f4571218a38"),
            UUID.fromString("47827560-6300-49ce-873d-fdb55f6d184d"),
            UUID.fromString("5dc27d99-5e92-4fc6-ba6b-ffa37923b5ce"),
            UUID.fromString("6c9c29d0-95e8-493c-ab51-d13e55c1de7e"),
            UUID.fromString("7510a7cb-f02b-4352-b5f9-e04be7d4c351"),
            UUID.fromString("872ce199-a66f-4321-86f1-61892abcb632") };

        String[] citations = new String[] {
            "<fo:block>Publication 1 title<fo:block/>",
            "<fo:block>Publication 2 title<fo:block/>",
            "<fo:block>Publication 3 title<fo:block/>",
            "<fo:block>Publication 4 title<fo:block/>",
            "<fo:block>Publication 5 title<fo:block/>",
            "<fo:block>Publication 6 title<fo:block/>",
            "<fo:block>Publication 7 title<fo:block/>",
            "<fo:block>Publication 8 title<fo:block/>"
        };

        createItemMockWithType(itemIds[0], "book");
        createItemMockWithType(itemIds[1], "speech");
        createItemMockWithType(itemIds[2], "conference");
        createItemMockWithType(itemIds[3], "article");
        createItemMockWithType(itemIds[4], null);
        createItemMockWithType(itemIds[5], "article");
        createItemMockWithType(itemIds[6], "book");
        createItemMockWithType(itemIds[7], null);

        SimpleMapConverter typeConverter = new SimpleMapConverter();
        typeConverter.setMapping(Map.of("book", "1 book converted", "conference", "conference converted"));

        processor.setTypeConverter(typeConverter);
        processor.setFixedTypesOrder(List.of("speech", "conference converted", "book review"));

        CSLResult result = processor.process(context, item, new CSLResult("fo", itemIds, citations));
        assertThat(result, notNullValue());

        assertThat(result.getFormat(), is("fo"));
        assertThat(result.getItemIds(), arrayContaining(itemIds[1], itemIds[2], itemIds[0],
            itemIds[6], itemIds[3], itemIds[5], itemIds[4], itemIds[7]));

        assertThat(result.getCitationEntries(), arrayContaining(
            "<fo:block font-size=\"12pt\" font-weight=\"bold\" margin-top=\"4mm\" >Speech</fo:block>"
                + "<fo:block>Publication 2 title<fo:block/>",
            "<fo:block font-size=\"12pt\" font-weight=\"bold\" margin-top=\"4mm\" >Conference converted</fo:block>"
                + "<fo:block>Publication 3 title<fo:block/>",
            "<fo:block font-size=\"12pt\" font-weight=\"bold\" margin-top=\"4mm\" >1 book converted</fo:block>"
                + "<fo:block>Publication 1 title<fo:block/>",
            "<fo:block>Publication 7 title<fo:block/>",
            "<fo:block font-size=\"12pt\" font-weight=\"bold\" margin-top=\"4mm\" >Article</fo:block>"
                + "<fo:block>Publication 4 title<fo:block/>",
            "<fo:block>Publication 6 title<fo:block/>",
            "<fo:block font-size=\"12pt\" font-weight=\"bold\" margin-top=\"4mm\" >Other</fo:block>"
                + "<fo:block>Publication 5 title<fo:block/>",
            "<fo:block>Publication 8 title<fo:block/>"));

    }

    @Test
    public void testWithEmptyCitationArray() throws SQLException {

        UUID[] itemIds = new UUID[0];
        String[] citations = new String[0];

        CSLResult result = processor.process(context, item, new CSLResult("fo", itemIds, citations));
        assertThat(result, notNullValue());

        assertThat(result.getFormat(), is("fo"));
        assertThat(result.getItemIds(), emptyArray());
        assertThat(result.getCitationEntries(), emptyArray());

    }

    @Test
    public void testWithNullCitationArray() throws SQLException {

        UUID[] itemIds = null;
        String[] citations = null;

        CSLResult result = processor.process(context, item, new CSLResult("fo", itemIds, citations));
        assertThat(result, notNullValue());

        assertThat(result.getFormat(), is("fo"));
        assertThat(result.getItemIds(), emptyArray());
        assertThat(result.getCitationEntries(), emptyArray());

    }

    private Item createItemMockWithType(UUID uuid, String type) throws SQLException {
        Item mock = mock(Item.class);
        when(mock.getID()).thenReturn(uuid);
        when(itemService.find(context, uuid)).thenReturn(mock);
        when(itemService.getMetadataFirstValue(mock, "dc", "type", null, Item.ANY)).thenReturn(type);
        return mock;
    }
}
