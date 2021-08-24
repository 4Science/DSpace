/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components.tree;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.plugin.PluginException;
import org.dspace.plugin.SiteHomeProcessor;

public class TreeHomeProcessor implements SiteHomeProcessor
{

    @Override
    public void process(Context context, HttpServletRequest request,
            HttpServletResponse response)
            throws PluginException, AuthorizeException
    {
        Boolean lazyload = ConfigurationManager.getBooleanProperty("cris", "cris.tree.widget.homeprocessor.lazyload", false);
        Boolean showprofile = ConfigurationManager.getBooleanProperty("cris", "cris.tree.widget.homeprocessor.showprofile", false);
        Boolean showall = ConfigurationManager.getBooleanProperty("cris", "cris.tree.widget.homeprocessor.showall", false);
        String tree = ConfigurationManager.getProperty("cris","cris.tree.widget.homeprocessor.tree");
        String rootID = ConfigurationManager.getProperty("cris","cris.tree.widget.homeprocessor.rootid");
        String contextTree = ConfigurationManager.getProperty("cris","cris.tree.widget.homeprocessor.contexttree");
        
        request.setAttribute("lazyload", lazyload);
        request.setAttribute("showprofile", showprofile);
        request.setAttribute("showall", showall);
        request.setAttribute("tree", tree);
        request.setAttribute("rootID", rootID);
        request.setAttribute("contextTree", contextTree);
    }

}
