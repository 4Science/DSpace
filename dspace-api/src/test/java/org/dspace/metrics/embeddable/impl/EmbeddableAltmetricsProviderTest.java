/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.embeddable.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for AbstractEmbeddableMetricProvider.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */

@RunWith(MockitoJUnitRunner.class)
public class EmbeddableAltmetricsProviderTest {

    @InjectMocks
    private EmbeddableAltmetricsProvider provider;

    @Mock
    private ItemService itemService;

    @Mock
    private Item item;

    @Mock
    private Context context;

    @Before
    public void setUp() throws Exception {
        provider.setDoiField("dc.identifier.doi");
        provider.setPmidField("dc.identifier.pmid");
        provider.setPopover("popover");
        provider.setBadgeType("badgeType");
        provider.setListBadgeType("listBadgeType");
        provider.setListPopOver("listPopOver");
        provider.setMinScore(100);
        provider.setLinkTarget("_blank");
        provider.setListLinkTarget("_blank");
    }

    @Test
    public void testInnerHtml() {

        when(itemService.getMetadataFirstValue(item, new MetadataFieldName("dc.identifier.doi"), Item.ANY))
            .thenReturn("calculatedDoi");

        when(itemService.getMetadataFirstValue(item, new MetadataFieldName("dc.identifier.pmid"), Item.ANY))
            .thenReturn("calculatedPmid");

        String innerHtml = provider.innerHtml(context, item);

        assertEquals("{"
            + "\"data-badge-enabled\":false,"
            + "\"list-data-badge-enabled\":false,"
            + "\"popover\":\"popover\","
            + "\"badgeType\":\"badgeType\","
            + "\"data-badge-details\":null,"
            + "\"doiAttr\":\"calculatedDoi\","
            + "\"pmidAttr\":\"calculatedPmid\","
            + "\"data-hide-less-than\":100,"
            + "\"data-hide-no-mentions\":null,"
            + "\"data-link-target\":\"_blank\","
            + "\"list-popover\":\"listPopOver\","
            + "\"list-badgeType\":\"listBadgeType\","
            + "\"list-data-badge-details\":null,"
            + "\"list-doiAttr\":\"calculatedDoi\","
            + "\"list-pmidAttr\":\"calculatedPmid\","
            + "\"list-data-hide-less-than\":100,"
            + "\"list-data-hide-no-mentions\":null,"
            + "\"list-data-link-target\":\"_blank\""
            + "}", innerHtml);
    }

}
