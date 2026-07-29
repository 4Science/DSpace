/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.authorization.impl.CanExportMetadataFeature;
import org.dspace.app.rest.converter.SiteConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Site;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.SiteService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;



/**
 * Test suite for the export metadata feature
 *
 * @author Francesco Molinaro (francesco.molinaro at 4science.it)
 *
 */
public class CanExportMetadataFeatureIT  extends AbstractControllerIntegrationTest {

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private SiteConverter siteConverter;

    private SiteService siteService;

    /**
     * this hold a reference to the test feature {@link org.dspace.app.rest.authorization.impl.CanExportMetadataFeature}
     */
    private AuthorizationFeature canExportMetadataFeature;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        siteService = ContentServiceFactory.getInstance().getSiteService();
        canExportMetadataFeature = authorizationFeatureService.find(CanExportMetadataFeature.NAME);
    }


    @Test
    public void adminCanExportMetadataIfScriptExistTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, DefaultProjection.DEFAULT);

        Authorization adminAuth = new Authorization(admin, canExportMetadataFeature, siteRest);
        Authorization epersonAuth = new Authorization(eperson, canExportMetadataFeature, siteRest);


        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + adminAuth.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(adminAuth))));
        getClient(epersonToken).perform(get("/api/authz/authorizations/" + epersonAuth.getID()))
                .andExpect(status().isNotFound());

    }

    @Test
    public void adminCantExportMetadataIfScriptDoesNotExistTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/missingScript" ))
                .andExpect(status().isNotFound());
        getClient(epersonToken).perform(get("/api/authz/authorizations/missingScript" ))
                .andExpect(status().isBadRequest());
    }


}
