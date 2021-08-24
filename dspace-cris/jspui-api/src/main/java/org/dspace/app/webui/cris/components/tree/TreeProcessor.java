/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components.tree;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.cris.components.ExploreProcessor;
import org.dspace.core.ConfigurationManager;
import org.dspace.discovery.configuration.DiscoveryConfiguration;

public class TreeProcessor implements ExploreProcessor
{
    
    private Boolean lazyload;
    private Boolean showprofile;
    private Boolean showall;
    private String tree;
    private String rootID;
    private String contextTree;
    
    @Override
    public Map<String, Object> process(String configurationName,
            DiscoveryConfiguration discoveryConfiguration,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception
    {
        // NOOP
        return null;
    }

    @Override
    public void process(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        request.setAttribute("lazyload", lazyload);
        request.setAttribute("showprofile", showprofile);
        request.setAttribute("showall", showall);
        request.setAttribute("tree", tree);
        request.setAttribute("rootID", rootID);
        request.setAttribute("contextTree", contextTree);
    }

    public void setLazyload(Boolean lazyload)
    {
        this.lazyload = lazyload;
    }

    public void setShowprofile(Boolean showprofile)
    {
        this.showprofile = showprofile;
    }

    public void setShowall(Boolean showall)
    {
        this.showall = showall;
    }

    public void setTree(String tree)
    {
        this.tree = tree;
    }

    public void setRootID(String rootID)
    {
        this.rootID = rootID;
    }

    public void setContextTree(String contextTree)
    {
        this.contextTree = contextTree;
    }

}
