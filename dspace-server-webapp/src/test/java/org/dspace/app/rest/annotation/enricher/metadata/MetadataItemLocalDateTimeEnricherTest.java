/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher.metadata;

import static org.dspace.app.rest.annotation.AnnotationRestDeserializer.DATETIME_FORMATTER;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.function.BiConsumer;

import org.dspace.AbstractUnitTest;
import org.dspace.app.rest.annotation.AnnotationRest;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class MetadataItemLocalDateTimeEnricherTest extends AbstractUnitTest {

    private MetadataItemLocalDateTimeEnricher enricher;

    @Mock
    private MetadataValue metadataValue;
    @Mock
    private AnnotationRest annotationRest;
    @Mock
    private Item item;
    @Mock
    private Context context;
    @Mock
    private ItemService itemService;

    private final MetadataFieldName dateIssued = new MetadataFieldName("dc", "date", "issued");

    @Before
    public void setUp() {
        enricher = new MetadataItemLocalDateTimeEnricher(
            "created", dateIssued, DATETIME_FORMATTER
        );
    }

    @Test
    public void testEnrichWithValidLocalDateTime() throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        when(annotationRest.getCreated()).thenReturn(now);
        String dateStr = now.format(DATETIME_FORMATTER);

        when(item.getItemService()).thenReturn(itemService);

        BiConsumer<Context, Item> enriched = enricher.apply(annotationRest);

        assertNotNull(enriched);
        enriched.accept(context, item);
        verify(itemService).setMetadataSingleValue(
            eq(context), eq(item), eq(dateIssued.schema), eq(dateIssued.element), eq(dateIssued.qualifier),
            isNull(), eq(dateStr)
        );
    }

    @Test
    public void testEnrichWithNullValue() {
        when(annotationRest.getCreated()).thenReturn(null);

        BiConsumer<Context, Item> enriched = enricher.apply(annotationRest);

        assertNotNull(enriched);
        enriched.accept(context, item);
        verifyNoInteractions(item);
    }

}
