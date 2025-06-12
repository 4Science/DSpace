/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.authorization.impl.CanCreateVersionFeature;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Test for the CanViewInWorkflowSince authorization feature.
 */
public class CanViewInWorkflowSinceFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private Utils utils;

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    private Item itemA;
    private Item itemB;
    private EPerson user;
    private ItemRest itemARest;
    private Community communityA;
    private Collection collectionA;
    private AuthorizationFeature canCreateVersionFeature;

    final String feature = "canViewInWorkflowSinceStatistics";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        canCreateVersionFeature = authorizationFeatureService.find(CanCreateVersionFeature.NAME);

        user = EPersonBuilder.createEPerson(context)
                             .withEmail("userEmail@test.com")
                             .withPassword(password).build();

        communityA = CommunityBuilder.createCommunity(context)
                                     .withName("communityA").build();

        collectionA = CollectionBuilder.createCollection(context, communityA)
                                       .withName("collectionA").build();

        itemA = ItemBuilder.createItem(context, collectionA)
                           .withTitle("Item A").build();

        itemB = ItemBuilder.createItem(context, collectionA)
                           .withTitle("Item B").build();

        context.restoreAuthSystemState();

        itemARest = itemConverter.convert(itemA, Projection.DEFAULT);
    }

    @Test
    public void anonymousHasNotAccessTest() throws Exception {
        getClient().perform(get("/api/authz/authorizations/search/object")
                   .param("embed", "feature")
                   .param("feature", feature)
                   .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(0)))
                .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void adminItemSuccessTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/authz/authorizations/search/object")
                             .param("embed", "feature")
                             .param("feature", feature)
                             .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
                          .andExpect(status().isOk())
                          .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                          .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void submitterItemSuccessTest() throws Exception {
        context.turnOffAuthorisationSystem();

        itemA.setSubmitter(user);

        context.restoreAuthSystemState();

        String userToken = getAuthToken(user.getEmail(), password);
        getClient(userToken).perform(get("/api/authz/authorizations/search/object")
                            .param("embed", "feature")
                            .param("feature", feature)
                            .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
                         .andExpect(status().isOk())
                         .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                         .andExpect(jsonPath("$._embedded").exists());
    }
}
