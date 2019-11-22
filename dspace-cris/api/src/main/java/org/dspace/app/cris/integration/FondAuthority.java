package org.dspace.app.cris.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.Metadatum;
import org.dspace.utils.DSpace;

public class FondAuthority extends DOAuthority
{
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
        List<String> fondsresults = new ArrayList<>();
        DSpace dspace = new DSpace();
        ApplicationService appService = dspace.getServiceManager()
                .getServiceByName("applicationService",
                        ApplicationService.class);
        String fondshierarchy = cris.getName();
        
        //Get first parent
        Metadatum[] fondsparent = cris.getMetadata(cris.getTypeText(),
                "fondsparent", null, null);

        //Get all parent hierarchy
        fondsresults = getParents(appService, fondsparent);

        //Reverse the list for match "root-to-leaf" mode
        Collections.reverse(fondsresults);

        //Iterate all parent and add each on string
        for (String fondsresult : fondsresults)
        {
            fondshierarchy = fondsresult + " > " + fondshierarchy;
        }

        return fondshierarchy;
    }

    private List<String> getParents(ApplicationService appService,
            Metadatum[] fondsparent)
    {
        ResearchObject ro = null;
        List<String> fondsresults = new ArrayList<>();

        //Iterate on Metatada "fondsparent". For each parent will iterate again on it
        for (Metadatum parent : fondsparent)
        {
            ro = appService.getEntityByCrisId(parent.authority,
                    ResearchObject.class);
            if (ro.getMetadata("fondsparent") != null)
            {
                fondsresults.addAll(getParents(appService, ro.getMetadata(ro.getTypeText(),
                        "fondsparent", null, null)));
            }
            fondsresults.add(ro.getName());
        }
        return fondsresults;
    }
}