package org.dspace.app.cris.integration;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.Metadatum;
import org.dspace.utils.DSpace;

public class FondAuthority extends DOAuthority
{
	ApplicationService applicationService = new DSpace().getServiceManager().getServiceByName(
            "applicationService", ApplicationService.class);

    /** The logger */
    private static Logger log = Logger.getLogger(FondAuthority.class);

    @Override
    public int getCRISTargetTypeID()
    {
        return -1;
    }

    @Override
    public Class<ResearchObject> getCRISTargetClass()
    {
        return ResearchObject.class;
    }

    @Override
    public String getPublicPath()
    {
        return null;
    }

    @Override
    public ResearchObject getNewCrisObject()
    {
        return new ResearchObject();
    }

    @Override
    protected String getDisplayEntry(ACrisObject cris)
    {
        if (cris instanceof ResearchObject)
        {
            if (cris instanceof ResearchObject
                    && cris.getTypeText().equals("crisfonds"))
            {
                List<String> results = new ArrayList<>();

                generateTree(cris, results);

                return StringUtils.join(results, " > ");
            }
        }

        return cris.getName();
    }

    private void generateTree(ACrisObject cris, List<String> results)
    {
        Metadatum[] fondsparents = cris.getMetadata(
                cris.getTypeText(), "fondsparent", null, null);

        if (fondsparents != null && fondsparents.length > 0) {
            for (Metadatum fondsparent : fondsparents) {
                ResearchObject parent = applicationService.getEntityByCrisId(fondsparent.authority,
                        ResearchObject.class);

                generateTree(parent, results);
            }
        }

        results.add(cris.getName());
    }
}