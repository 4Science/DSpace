/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.metrics.embeddable.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/*
*  @author Jurgen Mamani
*
*/
@RunWith(MockitoJUnitRunner.class)
public class EmbeddableDimensionsMetricProviderTest {

    @Mock
    private EmbeddableDimensionsMetricProvider provider;

    @Mock
    Context context;

    @Mock
    Item item;

    @Before
    public void setUp() {
        when(provider.innerHtml(any(), any())).thenCallRealMethod();
        when(provider.getValueFromMetadataField(eq(item), eq("dc.identifier.doi"))).thenReturn("doi-test");
        when(provider.getValueFromMetadataField(eq(item), eq("dc.identifier.pmid"))).thenReturn("pmid-test");

        provider.dataStyle = "small_circle";
        provider.dataLegend = "hover-right";
        provider.listDataStyle = "list-small_circle";
        provider.listDataLegend = "list-hover-right";
        provider.doiField = "dc.identifier.doi";
        provider.pmidField = "dc.identifier.pmid";
        provider.badgeInstalled = true;
    }

    @Test
    public void innerHtml() {
        String innerHtml = provider.innerHtml(context, item);

        assertEquals("{\"data-legend\":\"hover-right\",\"data-style\":\"small_circle\"," +
                         "\"data-dimensions-badge-installed\":true,\"data-doi\":\"doi-test\"," +
                         "\"data-pmid\":\"pmid-test\",\"list-data-legend\":\"list-hover-right\"," +
                         "\"list-data-style\":\"list-small_circle\",\"list-data-dimensions-badge-installed\":false," +
                         "\"list-data-doi\":\"doi-test\",\"list-data-pmid\":\"pmid-test\"}", innerHtml);
    }

}
