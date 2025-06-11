/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher.metadata;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import org.dspace.AbstractDSpaceTest;
import org.dspace.app.rest.annotation.AnnotationRest;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

public class MetadataItemValueEnricherTest extends AbstractDSpaceTest {

    public static final String TEST_VALUE = "Test Value";

    @InjectMocks
    private MetadataItemValueEnricher<String> enricher;

    @Mock
    private Item item;

    @Mock
    private MetadataFieldName metadataField;

    @Mock
    private AnnotationRest annotationRest;

    @Mock
    private ItemService itemService;

    @Mock
    private Context context;

    @Before
    public void setUp() {
        Mockito.when(item.getItemService()).thenReturn(itemService);
        enricher = new MetadataItemValueEnricher<>(metadataField, TEST_VALUE);
    }

    @Test
    public void testEnricherInstantiation() {
        assertNotNull(enricher);
    }

    @Test
    public void testEnricherWithNullAnnotation() {
        enricher = new MetadataItemValueEnricher<>(metadataField, null);
        BiConsumer<Context, Item> apply = enricher.apply(null);
        verifyNoInteractions(item, itemService);
        apply.accept(context, item);
        verifyNoInteractions(item, itemService);
    }

    @Test
    public void testEnricherWithMetadataValue() throws SQLException {
        BiConsumer<Context, Item> apply = enricher.apply(annotationRest);
        assertNotNull(item);
        apply.accept(context, item);
        Mockito.verify(
            itemService
        ).addMetadata(
            eq(context), eq(item),
            eq(metadataField.schema),
            eq(metadataField.element),
            eq(metadataField.qualifier),
            isNull(),
            eq(TEST_VALUE)
        );
    }

}
