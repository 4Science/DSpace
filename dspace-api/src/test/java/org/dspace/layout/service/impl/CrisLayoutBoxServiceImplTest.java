/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service.impl;

import static org.apache.commons.collections4.IteratorUtils.arrayIterator;
import static org.apache.commons.collections4.IteratorUtils.emptyIterator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dspace.app.metrics.CrisMetrics;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoveryConfigurationUtilsService;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBoxTypes;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutFieldBitstream;
import org.dspace.layout.CrisLayoutMetric2Box;
import org.dspace.layout.dao.CrisLayoutBoxDAO;
import org.dspace.metrics.CrisItemMetricsService;
import org.dspace.metrics.embeddable.model.EmbeddableCrisMetrics;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for CrisLayoutBoxServiceImpl, so far only findByItem method is tested.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */

@RunWith(MockitoJUnitRunner.Silent.class)
public class CrisLayoutBoxServiceImplTest {

    @InjectMocks
    private CrisLayoutBoxServiceImpl crisLayoutBoxService;

    @Mock
    private Context context;

    @Mock
    private CrisLayoutBoxDAO dao;

    @Mock
    private AuthorizeService authorizeService;

    @Mock
    private CrisItemMetricsService crisItemMetricsService;

    @Mock
    private DiscoveryConfigurationUtilsService searchConfigurationUtilsService;

    @Mock
    private ItemService itemService;

    @Mock
    private BitstreamService bitstreamService;

    @Test
    public void testHasContentWithMetadataBox() {

        MetadataField titleField = metadataField("dc", "title", null);
        MetadataField authorField = metadataField("dc", "contributor", "author");

        CrisLayoutBox box = crisLayoutMetadataBox("Main Box", authorField, titleField);
        Item item = item(metadataValue(titleField, "John Smith"));

        assertThat(crisLayoutBoxService.hasContent(context, box, item), is(true));
    }

    @Test
    public void testRelationBoxHasContent() {

        CrisLayoutBox box = crisLayoutBox("authors", CrisLayoutBoxTypes.RELATION.name());
        Item item = item();

        Iterator<Item> relatedItems = arrayIterator(item(), item());
        when(searchConfigurationUtilsService.findByRelation(context, item, "authors")).thenReturn(relatedItems);
        assertThat(crisLayoutBoxService.hasContent(context, box, item), is(true));

    }

    @Test
    public void testRelationBoxHasNoContent() {

        CrisLayoutBox box = crisLayoutBox("authors", CrisLayoutBoxTypes.RELATION.name());
        Item item = item();

        when(searchConfigurationUtilsService.findByRelation(context, item, "authors")).thenReturn(emptyIterator());
        assertThat(crisLayoutBoxService.hasContent(context, box, item), is(false));

    }

    @Test
    public void testHasContentWithBoxWithoutType() {

        MetadataField titleField = metadataField("dc", "title", null);

        CrisLayoutBox box = crisLayoutBox("Main Box", null, titleField);
        Item item = item(metadataValue(titleField, "John Smith"));

        assertThat(crisLayoutBoxService.hasContent(context, box, item), is(true));
    }

    @Test
    public void testHasContentWithItemWithoutMetadata() {

        MetadataField titleField = metadataField("dc", "title", null);
        MetadataField authorField = metadataField("dc", "contributor", "author");

        CrisLayoutBox box = crisLayoutMetadataBox("Main Box", titleField, authorField);
        Item item = item();

        assertThat(crisLayoutBoxService.hasContent(context, box, item), is(false));
    }

    @Test
    public void testHasContentWithEmptyMetadataBox() {

        MetadataField titleField = metadataField("dc", "title", null);

        CrisLayoutBox box = crisLayoutMetadataBox("Main Box");
        Item item = item(metadataValue(titleField, "John Smith"));

        assertThat(crisLayoutBoxService.hasContent(context, box, item), is(false));
    }

    @Test
    public void testHasContentWithBoxWithBitstream() throws SQLException {

        MetadataField titleField = metadataField("dc", "title", null);
        MetadataField typeField = metadataField("dc", "type", null);

        Item item = item();

        Bitstream bitstream = mock(Bitstream.class);

        CrisLayoutFieldBitstream fieldBitstream = new CrisLayoutFieldBitstream();
        fieldBitstream.setBundle("ORIGINAL");
        fieldBitstream.setMetadataValue("thumbnail");
        fieldBitstream.setMetadataField(typeField);

        CrisLayoutBox box = new CrisLayoutBox();
        box.addLayoutField(crisLayoutField(titleField));
        box.addLayoutField(fieldBitstream);
        box.setShortname("Main Box");
        box.setType("METADATA");

        when(bitstreamService.findShowableByItem(context, item.getID(), "ORIGINAL",
            Map.of("dc.type", "thumbnail"), false))
            .thenReturn(List.of(bitstream));

        assertThat(crisLayoutBoxService.hasContent(context, box, item), is(true));

        verify(bitstreamService).findShowableByItem(context, item.getID(), "ORIGINAL",
            Map.of("dc.type", "thumbnail"), false);

    }

    @Test
    public void testHasNoContentWithBoxWithBitstream() throws SQLException {

        MetadataField titleField = metadataField("dc", "title", null);
        MetadataField typeField = metadataField("dc", "type", null);

        Item item = item();

        CrisLayoutFieldBitstream fieldBitstream = new CrisLayoutFieldBitstream();
        fieldBitstream.setBundle("ORIGINAL");
        fieldBitstream.setMetadataValue("thumbnail");
        fieldBitstream.setMetadataField(typeField);

        CrisLayoutBox box = new CrisLayoutBox();
        box.addLayoutField(crisLayoutField(titleField));
        box.addLayoutField(fieldBitstream);
        box.setShortname("Main Box");
        box.setType("METADATA");

        when(bitstreamService.findShowableByItem(context, item.getID(), "ORIGINAL",
            Map.of("dc.type", "thumbnal"), false))
            .thenReturn(List.of());

        assertThat(crisLayoutBoxService.hasContent(context, box, item), is(false));

        verify(bitstreamService).findShowableByItem(context, item.getID(), "ORIGINAL",
            Map.of("dc.type", "thumbnail"), false);

    }

    @Test
    public void testHasMetricsBoxContent() throws SQLException {

        when(authorizeService.authorizeActionBoolean(eq(context), any(), eq(Constants.READ))).thenReturn(true);

        // should return false when the box has no metrics associated
        CrisLayoutBox boxWithoutMetrics = crisLayoutMetricBox();
        assertFalse(crisLayoutBoxService.hasContent(context, boxWithoutMetrics, item()));

        // should return true when the box has at least one embeddable associated (stored mocked to empty)
        CrisLayoutBox boxMetric1 = crisLayoutMetricBox("metric1");
        storedCrisMetrics();
        embeddableCrisMetrics("metric1");
        assertTrue(crisLayoutBoxService.hasContent(context, boxMetric1, item()));

        // should return true when the box has at least one stored associated (embeded mocked to empty)
        storedCrisMetrics("metric1");
        embeddableCrisMetrics();
        assertTrue(crisLayoutBoxService.hasContent(context, boxMetric1, item()));

        // shuld return false when the box has embedded but not associated (stored mocked to empty)
        storedCrisMetrics();
        embeddableCrisMetrics("metric2");
        assertFalse(crisLayoutBoxService.hasContent(context, boxMetric1, item()));

        // shuld return false when the box has stored but not associated (embedded mocked to empty)
        storedCrisMetrics("metric2");
        embeddableCrisMetrics();
        assertFalse(crisLayoutBoxService.hasContent(context, boxMetric1, item()));

    }

    @Test
    public void testHasMetricsBoxContentNotAuthorized() throws SQLException {

        // should return false if there is content but context has not an authenticated user
        when(authorizeService.authorizeActionBoolean(eq(context), any(), eq(Constants.READ))).thenReturn(false);
        CrisLayoutBox boxMetric1 = crisLayoutMetricBox("metric1");
        storedCrisMetrics();
        embeddableCrisMetrics("metric1");

        assertFalse(crisLayoutBoxService.hasContent(context, boxMetric1, item()));
    }

    @Test
    public void testIiifBoxHasContentWithMetadataTrue() {
        Item item = item();

        when(itemService.getMetadataFirstValue(item, new MetadataFieldName("dspace", "iiif", "enabled"),
        Item.ANY)).thenReturn("true");

        CrisLayoutBox box = crisLayoutBox("Box", "IIIFVIEWER");

        assertTrue(crisLayoutBoxService.hasContent(context, box, item));
    }

    @Test
    public void testIiifBoxHasNoContentWithMetadataFalse() {
        Item item = item();

        when(itemService.getMetadataFirstValue(item, new MetadataFieldName("dspace", "iiif", "enabled"),
        Item.ANY)).thenReturn("false");

        CrisLayoutBox box = crisLayoutBox("Box", "IIIFVIEWER");

        assertFalse(crisLayoutBoxService.hasContent(context, box, item));
    }

    @Test
    public void testIiifBoxHasNoContentWithMetadataUndefined() {
        Item item = item();

        CrisLayoutBox box = crisLayoutBox("Box", "IIIFVIEWER");

        assertFalse(crisLayoutBoxService.hasContent(context, box, item));
    }

    private CrisLayoutBox crisLayoutMetadataBox(String shortname, MetadataField... metadataFields) {
        return crisLayoutBox(shortname, CrisLayoutBoxTypes.METADATA.name(), metadataFields);
    }

    private CrisLayoutBox crisLayoutBox(String shortname, String boxType, MetadataField... metadataFields) {
        CrisLayoutBox box = new CrisLayoutBox();
        for (MetadataField metadataField : metadataFields) {
            box.addLayoutField(crisLayoutField(metadataField));
        }
        box.setShortname(shortname);
        box.setType(boxType);
        return box;
    }

    private Item item(MetadataValue... metadataValues) {
        Item item = mock(Item.class);
        when(item.getMetadata()).thenReturn(List.of(metadataValues));
        when(item.getID()).thenReturn(UUID.randomUUID());
        return item;
    }

    private MetadataField metadataField(String schema, String element, String qualifier) {
        MetadataField metadataField = mock(MetadataField.class);
        when(metadataField.getElement()).thenReturn(element);
        when(metadataField.getQualifier()).thenReturn(qualifier);

        MetadataSchema metadataSchema = mock(MetadataSchema.class);
        when(metadataSchema.getName()).thenReturn(schema);
        when(metadataField.getMetadataSchema()).thenReturn(metadataSchema);
        if (qualifier == null) {
            when(metadataField.toString('.')).thenReturn(schema + "." + element);
        } else {
            when(metadataField.toString('.')).thenReturn(schema + "." + element + "." + qualifier);
        }

        return metadataField;
    }

    private MetadataValue metadataValue(MetadataField field, String value) {
        return metadataValue(field, value, null);
    }

    private MetadataValue metadataValue(MetadataField field, String value, String authority) {
        MetadataValue metadataValue = mock(MetadataValue.class);
        when(metadataValue.getMetadataField()).thenReturn(field);
        when(metadataValue.getValue()).thenReturn(value);
        when(metadataValue.getAuthority()).thenReturn(authority);
        return metadataValue;
    }

    private CrisLayoutField crisLayoutField(MetadataField metadataField) {
        CrisLayoutField crisLayoutField = new CrisLayoutField();
        crisLayoutField.setMetadataField(metadataField);
        return crisLayoutField;
    }

    private CrisLayoutBox crisLayoutMetricBox(String ...metricTypes) {
        CrisLayoutBox crisLayoutMetricBox = new CrisLayoutBox();
        crisLayoutMetricBox.setType(CrisLayoutBoxTypes.METRICS.name());
        int position = 0;
        for (String metricType : metricTypes) {
            CrisLayoutMetric2Box metric2Box = new CrisLayoutMetric2Box();
            metric2Box.setBox(crisLayoutMetricBox);
            metric2Box.setPosition(position++);
            metric2Box.setType(metricType);
            crisLayoutMetricBox.getMetric2box().add(metric2Box);
        }
        return crisLayoutMetricBox;
    }

    private List<EmbeddableCrisMetrics> embeddableCrisMetrics(String ...metricTypes) {
        List<EmbeddableCrisMetrics> metrics = Arrays.stream(metricTypes)
            .map(this::buildEmbeddableCrisMetrics)
            .collect(Collectors.toList());
        when(crisItemMetricsService.getEmbeddableMetrics(any(), any(), any())).thenReturn(metrics);
        return metrics;
    }

    private List<CrisMetrics> storedCrisMetrics(String ...metricTypes) {
        List<CrisMetrics> metrics = Arrays.stream(metricTypes)
            .map(this::buildCrisMetrics)
            .collect(Collectors.toList());
        when(crisItemMetricsService.getStoredMetrics(any(), any())).thenReturn(metrics);
        return metrics;

    }

    private CrisMetrics buildCrisMetrics(String metricType) {
        CrisMetrics crisMetrics = new CrisMetrics();
        crisMetrics.setMetricType(metricType);
        return crisMetrics;
    }

    private EmbeddableCrisMetrics buildEmbeddableCrisMetrics(String metricType) {
        EmbeddableCrisMetrics crisMetrics = new EmbeddableCrisMetrics();
        crisMetrics.setMetricType(metricType);
        return crisMetrics;
    }

}
