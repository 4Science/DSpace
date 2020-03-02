/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.authorization.AlwaysFalseFeature;
import org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature;
import org.dspace.app.rest.authorization.AlwaysTrueFeature;
import org.dspace.app.rest.authorization.Authorization;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureService;
import org.dspace.app.rest.authorization.TrueForAdminsFeature;
import org.dspace.app.rest.authorization.TrueForLoggedUsersFeature;
import org.dspace.app.rest.authorization.impl.CCLicenseFeature;
import org.dspace.app.rest.authorization.impl.ReinstateFeature;
import org.dspace.app.rest.authorization.impl.WithdrawFeature;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test suite for the Authorization Feature Service
 * 
 * @author fcadili (francesco.cadili at 4science.it)
 *
 */
public class AuthorizationFeatureServiceIT extends AbstractControllerIntegrationTest {
    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private ConverterService converterService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private Utils utils;

    /***
     * Verify find method of AuthorizationFeatureService service.
     */
    @Test
    public void findAutorizationsByNameTest() {
        AuthorizationFeature ccLicenseFeature = authorizationFeatureService.find(CCLicenseFeature.NAME);
        assertThat("cclicense feature", ccLicenseFeature.getName(), equalTo(CCLicenseFeature.NAME));

        AuthorizationFeature withdrawFeature = authorizationFeatureService.find(WithdrawFeature.NAME);
        assertThat("withdraw feature", withdrawFeature.getName(), equalTo(WithdrawFeature.NAME));

        AuthorizationFeature reinstateFeature = authorizationFeatureService.find(ReinstateFeature.NAME);
        assertThat("reinstate feature", reinstateFeature.getName(), equalTo(ReinstateFeature.NAME));

        AuthorizationFeature alwaysFalseFeature = authorizationFeatureService.find(AlwaysFalseFeature.NAME);
        assertThat("always false feature", alwaysFalseFeature.getName(), equalTo(AlwaysFalseFeature.NAME));

        AuthorizationFeature alwaysThrownExceptionFeature = authorizationFeatureService
                .find(AlwaysThrowExceptionFeature.NAME);
        assertThat("always thrown exception feature", alwaysThrownExceptionFeature.getName(),
                equalTo(AlwaysThrowExceptionFeature.NAME));

        AuthorizationFeature alwaysTrueFeature = authorizationFeatureService.find(AlwaysTrueFeature.NAME);
        assertThat("always true feature", alwaysTrueFeature.getName(), equalTo(AlwaysTrueFeature.NAME));

        AuthorizationFeature trueForAminsFeature = authorizationFeatureService.find(TrueForAdminsFeature.NAME);
        assertThat("true for admins feature", trueForAminsFeature.getName(), equalTo(TrueForAdminsFeature.NAME));

        AuthorizationFeature trueForLoggedUserFeature = authorizationFeatureService
                .find(TrueForLoggedUsersFeature.NAME);
        assertThat("true for logged user feature", trueForLoggedUserFeature.getName(),
                equalTo(TrueForLoggedUsersFeature.NAME));
    }

    /***
     * Verify isAuthorized method of AuthorizationFeatureService service using
     * CCLicenseFeature feature.
     * 
     * @throws Exception
     */
    @Test
    public void cclicenzeIsAutorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection colItem = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withSubmitterGroup(eperson).build();

        Item item = ItemBuilder.createItem(context, colItem).withTitle("Sample submission").withIssueDate("2020-01-31")
                .withAuthor("Cadili, Francesco").withAuthor("Perelli, Matteo").withSubject("Sample").build();

        context.restoreAuthSystemState();

        AuthorizationFeature ccLicenseFeature = authorizationFeatureService.find(CCLicenseFeature.NAME);
        assertThat("cclicense feature", ccLicenseFeature.getName(), equalTo(CCLicenseFeature.NAME));

        context.setCurrentUser(eperson);
        ItemRest itemRest = converterService.toRest(item, converterService.getProjection(DefaultProjection.NAME));

        assertThat("is user authorized?", authorizationFeatureService.isAuthorized(context, ccLicenseFeature, itemRest),
                equalTo(false));

        // add policies
        authorizeService.addPolicy(context, item, Constants.ADD, eperson);
        authorizeService.addPolicy(context, item, Constants.REMOVE, eperson);

        assertThat("is user authorized?", authorizationFeatureService.isAuthorized(context, ccLicenseFeature, itemRest),
                equalTo(true));
    }

    /***
     * Verify isAuthorized method of AuthorizationFeatureService service using
     * ReinstateFeature feature
     * 
     * @throws Exception
     */
    @Test
    public void reinstateIsAutorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection colItem = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withSubmitterGroup(eperson).build();

        Item item = ItemBuilder.createItem(context, colItem).withTitle("Sample submission").withIssueDate("2020-01-31")
                .withAuthor("Cadili, Francesco").withAuthor("Perelli, Matteo").withSubject("Sample").build();

        context.restoreAuthSystemState();

        AuthorizationFeature reinstateFeature = authorizationFeatureService.find(ReinstateFeature.NAME);
        assertThat("reinstate feature", reinstateFeature.getName(), equalTo(ReinstateFeature.NAME));

        ItemRest itemRest = converterService.toRest(item, converterService.getProjection(DefaultProjection.NAME));
        context.setCurrentUser(eperson);

        assertThat("is user authorized?", authorizationFeatureService.isAuthorized(context, reinstateFeature, itemRest),
                equalTo(false));

        // set item withdrawn
        authorizeService.addPolicy(context, item.getOwningCollection(), Constants.REMOVE, eperson);
        authorizeService.addPolicy(context, item, Constants.WRITE, eperson);
        itemService.withdraw(context, item);

        assertThat("is user authorized?", authorizationFeatureService.isAuthorized(context, reinstateFeature, itemRest),
                equalTo(true));
    }

    /***
     * Verify isAuthorized method of AuthorizationFeatureService service using
     * CCLicenseFeature feature.
     * 
     * @throws Exception
     */
    @Test
    public void withdrawnIsAutorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection colItem = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withSubmitterGroup(eperson).build();

        Item item = ItemBuilder.createItem(context, colItem).withTitle("Sample submission").withIssueDate("2020-01-31")
                .withAuthor("Cadili, Francesco").withAuthor("Perelli, Matteo").withSubject("Sample").build();
        context.restoreAuthSystemState();

        AuthorizationFeature withdrawnFeature = authorizationFeatureService.find(WithdrawFeature.NAME);
        assertThat("withdrawn feature", withdrawnFeature.getName(), equalTo(WithdrawFeature.NAME));
        context.setCurrentUser(eperson);

        ItemRest itemRest = converterService.toRest(item, converterService.getProjection(DefaultProjection.NAME));
        assertThat("is user authorized?", authorizationFeatureService.isAuthorized(context, withdrawnFeature, itemRest),
                equalTo(false));

        // add policy
        authorizeService.addPolicy(context, item.getOwningCollection(), Constants.REMOVE, eperson);

        assertThat("is user authorized?", authorizationFeatureService.isAuthorized(context, withdrawnFeature, itemRest),
                equalTo(true));
    }

    @Test
    /***
     * Verify that the search by object and feature works using rest call using
     * CCLicenseFeature feature.
     * 
     * @See {@link CCLicenseFeatureRestIT}
     * 
     * @throws Exception
     */
    public void cclicenzeIsAutorizedRestTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection colItem = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withSubmitterGroup(eperson).build();
        Item item = ItemBuilder.createItem(context, colItem).withTitle("Sample submission").withIssueDate("2020-01-31")
                .withAuthor("Cadili, Francesco").withAuthor("Perelli, Matteo").withSubject("Sample").build();
        context.restoreAuthSystemState();

        AuthorizationFeature ccLicenseFeature = authorizationFeatureService.find(CCLicenseFeature.NAME);
        assertThat("cclicenze feature", ccLicenseFeature.getName(), equalTo(CCLicenseFeature.NAME));
        context.setCurrentUser(eperson);

        ItemRest itemRest = converterService.toRest(item, converterService.getProjection(DefaultProjection.NAME));
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();
        Authorization epersonAuth = new Authorization(eperson, ccLicenseFeature, itemRest);

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(get("/api/authz/authorizations/" + epersonAuth.getID()))
                .andExpect(status().isNotFound());

        // add policies
        authorizeService.addPolicy(context, item, Constants.ADD, eperson);
        authorizeService.addPolicy(context, item, Constants.REMOVE, eperson);

        getClient(epersonToken).perform(get("/api/authz/authorizations/" + epersonAuth.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(epersonAuth))));

        getClient(epersonToken)
                .perform(get("/api/authz/authorizations/search/objectAndFeature").param("uri", itemUri)
                        .param("eperson", eperson.getID().toString()).param("feature", ccLicenseFeature.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(epersonAuth))));
    }

    @Test
    /**
     * Verify that the search by object and feature works using rest call using
     * ReinstateFeature feature.
     * 
     * @See {@link ReinstateFeatureRestIT}
     * 
     * @throws Exception
     */
    public void reinstateIsAutorizedRestTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection colItem = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withSubmitterGroup(eperson).build();

        Item item = ItemBuilder.createItem(context, colItem).withTitle("Sample submission").withIssueDate("2020-01-31")
                .withAuthor("Cadili, Francesco").withAuthor("Perelli, Matteo").withSubject("Sample").build();
        itemService.withdraw(context, item);

        context.restoreAuthSystemState();

        AuthorizationFeature reinstateFeature = authorizationFeatureService.find(ReinstateFeature.NAME);
        assertThat("reinstate feature", reinstateFeature.getName(), equalTo(ReinstateFeature.NAME));
        context.setCurrentUser(eperson);

        ItemRest itemRest = converterService.toRest(item, converterService.getProjection(DefaultProjection.NAME));
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();
        Authorization epersonAuth = new Authorization(eperson, reinstateFeature, itemRest);

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(get("/api/authz/authorizations/" + epersonAuth.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(epersonAuth))));

        getClient(epersonToken)
                .perform(get("/api/authz/authorizations/search/objectAndFeature").param("uri", itemUri)
                        .param("eperson", eperson.getID().toString()).param("feature", reinstateFeature.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(epersonAuth))));
    }

    @Test
    /**
     * Verify that the search by object and feature works using rest call using
     * Withdraw feature.
     * 
     * @See {@link WithdrawFeatureRestIT}
     * 
     * @throws Exception
     */
    public void withdrawIsAutorizedRestTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity).withName("Sub Community")
                .build();
        Collection colItem = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withSubmitterGroup(eperson).build();

        Item item = ItemBuilder.createItem(context, colItem).withTitle("Sample submission").withIssueDate("2020-01-31")
                .withAuthor("Cadili, Francesco").withAuthor("Perelli, Matteo").withSubject("Sample").build();
        context.restoreAuthSystemState();

        AuthorizationFeature withdrawnFeature = authorizationFeatureService.find(WithdrawFeature.NAME);
        assertThat("withdrawn feature", withdrawnFeature.getName(), equalTo(WithdrawFeature.NAME));
        context.setCurrentUser(eperson);

        ItemRest itemRest = converterService.toRest(item, converterService.getProjection(DefaultProjection.NAME));
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();
        Authorization epersonAuth = new Authorization(eperson, withdrawnFeature, itemRest);

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(get("/api/authz/authorizations/" + epersonAuth.getID()))
                .andExpect(status().isNotFound());

        // add policy
        authorizeService.addPolicy(context, item.getOwningCollection(), Constants.REMOVE, eperson);

        getClient(epersonToken).perform(get("/api/authz/authorizations/" + epersonAuth.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(epersonAuth))));

        getClient(epersonToken)
                .perform(get("/api/authz/authorizations/search/objectAndFeature").param("uri", itemUri)
                        .param("eperson", eperson.getID().toString()).param("feature", withdrawnFeature.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(epersonAuth))));
    }
}
