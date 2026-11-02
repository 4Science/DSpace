/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.dspace.content.integration.crosswalks.virtualfields.ItemDOIService.formatDOI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class ItemDOIServiceIT extends AbstractIntegrationTestWithDatabase {

    private ItemDOIService itemDOIService;

    private Collection collection;

    private ConfigurationService configurationService;

    @Before
    public void setup() {
        itemDOIService = new DSpace().getServiceManager().getServicesByType(ItemDOIService.class).get(0);
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();


        context.setCurrentUser(admin);
        parentCommunity = createCommunity(context).build();
        collection = createCollection(context, parentCommunity).build();

        // Set test DOI prefix in configuration
        configurationService.setProperty("identifier.doi.prefix", "10.1234");
    }

    @Test
    public void testPrimaryAndAlternativeDOIExtraction() {
        Item item = ItemBuilder.createItem(context, collection)
                               .withDoiIdentifier("10.1234/primary-doi")
                               .withDoiIdentifier("10.5678/other-doi")
                               .withDoiIdentifier("https://doi.org/10.5678/resolved-doi")
                               .build();

        String primary = itemDOIService.getPrimaryDOIFromItem(item);
        assertThat(primary, is("10.1234/primary-doi"));

        String[] alternatives = itemDOIService.getAlternativeDOIFromItem(item);
        assertThat(alternatives.length, is(2));
        assertThat(alternatives, arrayContainingInAnyOrder(
            "10.5678/other-doi", "10.5678/resolved-doi"));
    }

    @Test
    public void testFallbackToFirstDOIWhenNoMatchWithPrefix() {
        configurationService.setProperty("identifier.doi.prefix", "10.9999");

        Item item = ItemBuilder.createItem(context, collection)
                               .withDoiIdentifier("10.5678/first")
                               .withDoiIdentifier("10.5678/second")
                               .build();

        String primary = itemDOIService.getPrimaryDOIFromItem(item);
        assertThat(primary, is("10.5678/first"));

        String[] alternatives = itemDOIService.getAlternativeDOIFromItem(item);
        assertThat(alternatives.length, is(1));
        assertThat(alternatives[0], is("10.5678/second"));
    }


    @Test
    public void testFormatDOI_Normalization() {
        assertEquals("10.25560/119170", formatDOI("https://doi.org/10.25560/119170"));
        assertEquals("10.25561/95151", formatDOI("https://www.dx.doi.org/10.25561/95151"));
        assertEquals("10.25560/31589", formatDOI("https://doi.org/10.25560/31589"));
        assertEquals("10.25561/93710", formatDOI("https://www.dx.doi.org/10.25561/93710"));
        assertEquals("10.1214/14-AAP1038", formatDOI("https://www.dx.doi.org/10.1214/14-AAP1038"));
        assertEquals("10.25561/109825", formatDOI("https://doi.org/10.25561/109825"));
        assertEquals("10.1016/j.econlet.2013.02.011", formatDOI("https://www.dx.doi.org/10.1016/j.econlet.2013.02.011"));
        assertEquals("10.1101/024349", formatDOI("https://www.dx.doi.org/10.1101/024349"));
        assertEquals("10.1038/s41586-020-2405-7", formatDOI("https://www.dx.doi.org/10.1038/s41586-020-2405-7"));
        assertEquals("10.1093/comnet/cnu039", formatDOI("https://www.dx.doi.org/10.1093/comnet/cnu039"));
        assertEquals("10.3310/hsdr04290", formatDOI("https://www.dx.doi.org/10.3310/hsdr04290"));
        assertEquals("10.2140/ant.2016.10.843", formatDOI("https://www.dx.doi.org/10.2140/ant.2016.10.843"));
        assertEquals("10.1215/00127094-2885764", formatDOI("https://www.dx.doi.org/10.1215/00127094-2885764"));
        assertEquals("10.1016/S2214-109X(20)30288-6", formatDOI("https://www.dx.doi.org/10.1016/S2214-109X(20)30288-6"));
    }

    @Test
    public void testFormatDOI_PlainText() {
        assertEquals("text_value", formatDOI("text_value"));
        assertEquals("10.25561/54217", formatDOI("10.25561/54217"));
    }

}
