/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher.metadata;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.List;
import java.util.function.BiConsumer;

import org.dspace.AbstractUnitTest;
import org.dspace.app.rest.annotation.AnnotationRest;
import org.dspace.app.rest.annotation.AnnotationTargetRest;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class MetadataItemPatternGroupEnricherTest extends AbstractUnitTest {

    static final String ITEM_PATTERN = "/iiif/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})";

    private MetadataItemPatternGroupEnricher enricher;

    @Mock
    private ItemService itemService;

    @Mock
    private AnnotationRest annotationRest;

    @Mock
    private Context context;


    MetadataFieldName glamItem = new MetadataFieldName("glam", "item");
    String fullIdentifierSelector = "on.![full]";

    @Before
    public void setUp() {
        enricher = new MetadataItemPatternGroupEnricher(
            fullIdentifierSelector, glamItem,
            String.class,
            ITEM_PATTERN
        );
        when(annotationRest.getOn())
            .thenReturn(List.of(
                new AnnotationTargetRest().setFull("http://example.com/iiif/12345678-1234-1234-1234-123456789012")));
    }

    @Test
    public void testEnricherInitialization() {
        assertNotNull(enricher);
    }

    @Test
    public void testEnrichWithNullReference() {
        Item item = mock(Item.class);
        when(annotationRest.getOn()).thenReturn(List.of(new AnnotationTargetRest()));

        assertThrows(IllegalArgumentException.class, () -> enricher.apply(annotationRest));
        verifyNoInteractions(item, itemService);
    }

    @Test
    public void testEnrichWithEmptyReference() {
        Item item = mock(Item.class);
        when(annotationRest.getOn()).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> enricher.apply(annotationRest));
        verifyNoInteractions(item, itemService);
    }

    @Test
    public void testEnrichWithValidReference() throws SQLException {
        Item item = mock(Item.class);
        when(item.getItemService()).thenReturn(itemService);

        BiConsumer<Context, Item> apply = enricher.apply(annotationRest);
        apply.accept(context, item);
        verify(itemService).setMetadataSingleValue(
            eq(context), eq(item), eq(glamItem.schema), eq(glamItem.element), isNull(), isNull(),
            eq("12345678-1234-1234-1234-123456789012")
        );
    }

}
