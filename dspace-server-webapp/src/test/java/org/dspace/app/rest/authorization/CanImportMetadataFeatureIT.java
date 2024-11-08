package org.dspace.app.rest.authorization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.authorization.impl.CanImportMetadataFeature;
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
 * Test suite for the import metadata feature
 *
 * @author Francesco Molinaro (francesco.molinaro at 4science.it)
 *
 */
public class CanImportMetadataFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private SiteConverter siteConverter;

    private SiteService siteService;

    /**
     * this hold a reference to the test feature {@link CanImportMetadataFeature}
     */
    private AuthorizationFeature canImportMetadataFeature;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        siteService = ContentServiceFactory.getInstance().getSiteService();
        canImportMetadataFeature = authorizationFeatureService.find(CanImportMetadataFeature.NAME);
    }


    @Test
    public void adminCanImportMetadataIfScriptExistTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        Site site = siteService.findSite(context);
        SiteRest siteRest = siteConverter.convert(site, DefaultProjection.DEFAULT);

        Authorization adminAuth = new Authorization(admin, canImportMetadataFeature, siteRest);
        Authorization epersonAuth = new Authorization(eperson, canImportMetadataFeature, siteRest);


        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + adminAuth.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(adminAuth))));
        getClient(epersonToken).perform(get("/api/authz/authorizations/" + epersonAuth.getID()))
                .andExpect(status().isNotFound());

    }

    @Test
    public void adminCantImportMetadataIfScriptDoesNotExistTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/missingScript" ))
                .andExpect(status().isNotFound());
        getClient(epersonToken).perform(get("/api/authz/authorizations/missingScript" ))
                .andExpect(status().isBadRequest());
    }


}
