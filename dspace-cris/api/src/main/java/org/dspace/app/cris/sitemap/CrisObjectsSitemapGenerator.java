/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.sitemap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.sitemap.AbstractGenerator;
import org.dspace.app.sitemap.ISitemapGeneratorPlugin;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

public class CrisObjectsSitemapGenerator implements ISitemapGeneratorPlugin
{
    private ApplicationService applicationService;

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    @Override
    public String addUrls(Context c, boolean makeHTMLMap,
            boolean makeSitemapOrg, List<String> includes,
            AbstractGenerator html, AbstractGenerator sitemapsOrg)
            throws IOException
    {
        StringBuffer objectDetails = new StringBuffer();
        String crisURLStem = ConfigurationManager.getProperty("dspace.url")
                + "/cris/";

        if (includes.contains("crispj"))
        {

            List<Project> crispjs = applicationService.getCrisObjectPaginate(Project.class,
                    CrisConstants.PROJECT_TYPE_ID);

            addUrlsInternal(makeHTMLMap, makeSitemapOrg, html, sitemapsOrg,
                    crisURLStem, crispjs);
            objectDetails.append(",crispj=").append(crispjs.size());
        }

        if (includes.contains("crisou"))
        {
            List<OrganizationUnit> orgUnits = applicationService.getCrisObjectPaginate(OrganizationUnit.class,
                    CrisConstants.OU_TYPE_ID);

            addUrlsInternal(makeHTMLMap, makeSitemapOrg, html, sitemapsOrg,
                    crisURLStem, orgUnits);
            objectDetails.append(",crisou=").append(orgUnits.size());
        }

        if (includes.contains("crisrp"))
        {

            List<ResearcherPage> crisrps = applicationService.getCrisObjectPaginate(ResearcherPage.class,
                    CrisConstants.RP_TYPE_ID);

            addUrlsInternal(makeHTMLMap, makeSitemapOrg, html, sitemapsOrg,
                    crisURLStem, crisrps);
            objectDetails.append(",crisrp=").append(crisrps.size());
        }

        List<DynamicObjectType> dynTypes = applicationService
                .getList(DynamicObjectType.class);
        for (DynamicObjectType dynType : dynTypes)
        {
            String dynShortName = dynType.getShortName();
            if (includes.contains("cris" + dynShortName))
            {
                long count = applicationService
                        .countResearchObjectByType(dynType);
                // num of full pages plus 1 if there are residual items
                int numPages = (int) (count / 100) + (count % 100 > 0 ? 1 : 0);
                for (int page = 0; page < numPages; page++)
                {
                    List<ResearchObject> researchers = applicationService
                            .getResearchObjectPaginateListByType(dynType, "id",
                                    false, page, 100);

                    addUrlsInternal(makeHTMLMap, makeSitemapOrg, html, sitemapsOrg,
                            crisURLStem, researchers);
                }
                objectDetails.append(",cris").append(dynShortName).append("=")
                    .append(count);
            }
        }
        return objectDetails.toString();
    }

    private void addUrlsInternal(boolean makeHTMLMap, boolean makeSitemapOrg,
            AbstractGenerator html, AbstractGenerator sitemapsOrg,
            String crisURLStem, List<? extends ACrisObject> crisObjects) throws IOException
    {
        for (ACrisObject crisObj : crisObjects)
        {
            String url = crisURLStem + crisObj.getAuthorityPrefix() + "/"
                    + crisObj.getCrisID();
            if (BooleanUtils.isNotTrue(crisObj.getStatus())) {
                continue;
            }
            if (makeHTMLMap)
            {
                html.addURL(url, null);
            }
            if (makeSitemapOrg)
            {
                sitemapsOrg.addURL(url, null);
            }
            applicationService.clearCache();
        }
    }

    @Override
    public List<String> getListTypes()
    {
        List<DynamicObjectType> dynTypes = applicationService
                .getList(DynamicObjectType.class);
        List<String> types = new ArrayList<String>();
        types.addAll(Arrays.asList("crisrp", "crisou", "crispj"));
        for (DynamicObjectType dynType : dynTypes)
        {
            types.add("cris" + dynType.getShortName());
        }
        return types;
    }

}
