package org.dspace.app.cris.integration;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.utils.DSpace;

public class FondAuthority extends DOAuthority
{

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
        return ResearcherPageUtils.getDisplayEntry(cris);

    }
}