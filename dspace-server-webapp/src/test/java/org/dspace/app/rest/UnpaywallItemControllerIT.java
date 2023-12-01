/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.unpaywall.dto.UnpaywallApiResponse;
import org.dspace.unpaywall.model.Unpaywall;
import org.dspace.unpaywall.model.UnpaywallStatus;
import org.dspace.unpaywall.service.UnpaywallService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test for the {@link UnpaywallItemController}
 */
public class UnpaywallItemControllerIT extends AbstractControllerIntegrationTest {

    private final static String DOI = "doi-value";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UnpaywallService unpaywallService;

    @Test
    public void testSiteUUID() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity).build();
        Item item = ItemBuilder.createItem(context, collection)
                .withDoiIdentifier(DOI)
                .build();

        String version = "publishedVersion";
        String licence = "cc-by";
        String landingPageUrl = "url-to-landing-page";
        String pdfUrl = "url-to-pdf";
        String hostType = "repository";
        UnpaywallApiResponse.OaLocation oaLocation = new UnpaywallApiResponse.OaLocation();
        oaLocation.setHostType(hostType);
        oaLocation.setLicense(licence);
        oaLocation.setVersion(version);
        oaLocation.setUrlForLandingPage(landingPageUrl);
        oaLocation.setUrlToPdf(pdfUrl);
        UnpaywallApiResponse unpaywallApiResponse = new UnpaywallApiResponse();
        unpaywallApiResponse.setOaLocations(List.of(oaLocation));

        Unpaywall unpaywall = new Unpaywall();
        unpaywall.setItemId(item.getID());
        unpaywall.setStatus(UnpaywallStatus.SUCCESSFUL);
        unpaywall.setDoi(DOI);
        unpaywall.setJsonRecord(objectMapper.writeValueAsString(unpaywallApiResponse));
        unpaywallService.create(context, unpaywall);

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/items/{uuid}/unpaywall/versions", item.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versions").isArray())
                .andExpect(jsonPath("$.versions[0].version", is(version)))
                .andExpect(jsonPath("$.versions[0].license", is(licence)))
                .andExpect(jsonPath("$.versions[0].landingPageUrl", is(landingPageUrl)))
                .andExpect(jsonPath("$.versions[0].pdfUrl", is(pdfUrl)))
                .andExpect(jsonPath("$.versions[0].hostType", is(hostType)));
    }

}
