/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.authorization.impl.LoginOnBehalfOfFeature;
import org.dspace.app.rest.converter.CommunityConverter;
import org.dspace.app.rest.converter.EPersonConverter;
import org.dspace.app.rest.converter.SiteConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class LoginOnBehalfOfFeatureRestIT extends AbstractControllerIntegrationTest {

    @Autowired
    private SiteConverter siteConverter;

    @Autowired
    private EPersonConverter ePersonConverter;

    @Autowired
    private CommunityConverter communityConverter;

    @Autowired
    private SiteService siteService;

    @Autowired
    private Utils utils;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private GroupService groupService;

    private AuthorizationFeature loginOnBehalfOf;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        loginOnBehalfOf = authorizationFeatureService.find(LoginOnBehalfOfFeature.NAME);
    }

    @Test
    public void loginOnBehalfOfTest() throws Exception {
        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, Projection.DEFAULT);
        String siteUri = utils.linkToSingleResource(siteRest, "self").getHref();

        String token = getAuthToken(admin.getEmail(), password);

        configurationService.setProperty("org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", true);
        configurationService.setProperty("webui.user.assumelogin", true);

        Authorization loginOnBehalfOfAuthorization = new Authorization(admin, loginOnBehalfOf, siteRest);
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", siteUri)
                                     .param("eperson", String.valueOf(admin.getID()))
                                     .param("feature", loginOnBehalfOf.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.authorizations", Matchers.hasItem(
                            AuthorizationMatcher.matchAuthorization(loginOnBehalfOfAuthorization))));
    }

    @Test
    public void loginOnBehalfNonSiteObjectOfTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        context.restoreAuthSystemState();

        CommunityRest communityRest = communityConverter.convert(parentCommunity, Projection.DEFAULT);
        String communityUri = utils.linkToSingleResource(communityRest, "self").getHref();

        String token = getAuthToken(admin.getEmail(), password);

        configurationService.setProperty("org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", true);
        configurationService.setProperty("webui.user.assumelogin", true);

        Authorization loginOnBehalfOfAuthorization = new Authorization(admin, loginOnBehalfOf, communityRest);
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", communityUri)
                                     .param("eperson", String.valueOf(admin.getID()))
                                     .param("feature", loginOnBehalfOf.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void loginOnBehalfOfNonAdminUserNotFoundTest() throws Exception {
        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, Projection.DEFAULT);
        String siteUri = utils.linkToSingleResource(siteRest, "self").getHref();

        String token = getAuthToken(admin.getEmail(), password);

        configurationService.setProperty("org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", true);
        configurationService.setProperty("webui.user.assumelogin", true);

        Authorization loginOnBehalfOfAuthorization = new Authorization(eperson, loginOnBehalfOf, siteRest);
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", siteUri)
                                     .param("eperson", String.valueOf(eperson.getID()))
                                     .param("feature", loginOnBehalfOf.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void loginOnBehalfOfNonAdminUserAssumeLoginPropertyFalseNotFoundTest() throws Exception {
        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, Projection.DEFAULT);
        String siteUri = utils.linkToSingleResource(siteRest, "self").getHref();

        String token = getAuthToken(admin.getEmail(), password);

        configurationService.setProperty("org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", true);
        configurationService.setProperty("webui.user.assumelogin", false);

        Authorization loginOnBehalfOfAuthorization = new Authorization(eperson, loginOnBehalfOf, siteRest);
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", siteUri)
                                     .param("eperson", String.valueOf(eperson.getID()))
                                     .param("feature", loginOnBehalfOf.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void loginOnBehalfOfAssumeLoginPropertyFalseNotFoundTest() throws Exception {
        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, Projection.DEFAULT);
        String siteUri = utils.linkToSingleResource(siteRest, "self").getHref();

        String token = getAuthToken(admin.getEmail(), password);

        configurationService.setProperty("org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", true);
        configurationService.setProperty("webui.user.assumelogin", false);

        Authorization loginOnBehalfOfAuthorization = new Authorization(admin, loginOnBehalfOf, siteRest);
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                     .param("uri", siteUri)
                                     .param("eperson", String.valueOf(admin.getID()))
                                     .param("feature", loginOnBehalfOf.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void loginOnBehalfOfAdminUserAssumeLoginAdminPropertyFalseNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson adminUser = EPersonBuilder.createEPerson(context)
                                          .withEmail("loginasuseradmin@test.com")
                                          .build();
        Group adminGroup = groupService.findByName(context, Group.ADMIN);
        groupService.addMember(context, adminGroup, adminUser);
        context.restoreAuthSystemState();

        EPersonRest ePersonRest = ePersonConverter.convert(adminUser, Projection.DEFAULT);
        String epersonUri = utils.linkToSingleResource(ePersonRest, "self").getHref();

        configurationService.setProperty("org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", true);
        configurationService.setProperty("webui.user.assumelogin", true);
        configurationService.setProperty("webui.user.assumelogin.admin", false);

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                            .param("uri", epersonUri)
                            .param("feature", loginOnBehalfOf.getName()))
                        .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void loginOnBehalfOfAdminUserAssumeLoginAdminPropertyNotExistTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson adminUser = EPersonBuilder.createEPerson(context)
                                          .withEmail("loginasuseradmin@test.com")
                                          .build();
        Group adminGroup = groupService.findByName(context, Group.ADMIN);
        groupService.addMember(context, adminGroup, adminUser);
        context.restoreAuthSystemState();

        EPersonRest ePersonRest = ePersonConverter.convert(adminUser, Projection.DEFAULT);
        String epersonUri = utils.linkToSingleResource(ePersonRest, "self").getHref();

        configurationService.setProperty("org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", true);
        configurationService.setProperty("webui.user.assumelogin", true);

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                            .param("uri", epersonUri)
                            .param("feature", loginOnBehalfOf.getName()))
                        .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void loginOnBehalfOfAdminUserTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson adminUser = EPersonBuilder.createEPerson(context)
                                          .withEmail("loginasuseradmin@test.com")
                                          .build();
        Group adminGroup = groupService.findByName(context, Group.ADMIN);
        groupService.addMember(context, adminGroup, adminUser);
        context.restoreAuthSystemState();

        EPersonRest ePersonRest = ePersonConverter.convert(adminUser, Projection.DEFAULT);
        String epersonUri = utils.linkToSingleResource(ePersonRest, "self").getHref();

        configurationService.setProperty("org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", true);
        configurationService.setProperty("webui.user.assumelogin", true);
        configurationService.setProperty("webui.user.assumelogin.admin", true);

        Authorization loginOnBehalfOfAuthorization = new Authorization(admin, loginOnBehalfOf, ePersonRest);

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/authz/authorizations/search/object")
                            .param("uri", epersonUri)
                            .param("feature", loginOnBehalfOf.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.authorizations", Matchers.hasItem(
                            AuthorizationMatcher.matchAuthorization(loginOnBehalfOfAuthorization))));
    }

}
