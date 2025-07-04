/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import java.util.List;

import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * This factory provides access to the XOAI extensions plugins.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public abstract class XOAIExtensionsPluginFactory {

    public static XOAIExtensionsPluginFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("xoaiExtensionsPluginFactory", XOAIExtensionsPluginFactory.class);
    }

    public abstract List<XOAIExtensionBitstreamCompilePlugin> getXOAIExtensionBitstreamCompilePlugins();

    public abstract List<XOAIExtensionItemCompilePlugin> getXoaiExtensionItemCompilePlugins();

}
