/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * This factory provides access to the XOAI extensions plugins.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class XOAIExtensionsPluginFactoryImpl extends XOAIExtensionsPluginFactory {

    @Autowired
    protected List<XOAIExtensionBitstreamCompilePlugin> xoaiExtensionBitstreamCompilePlugins;

    @Autowired
    protected List<XOAIExtensionItemCompilePlugin> xoaiExtensionItemCompilePlugins;

    @Override
    public List<XOAIExtensionBitstreamCompilePlugin> getXOAIExtensionBitstreamCompilePlugins() {
        return xoaiExtensionBitstreamCompilePlugins;
    }

    @Override
    public List<XOAIExtensionItemCompilePlugin> getXoaiExtensionItemCompilePlugins() {
        return xoaiExtensionItemCompilePlugins;
    }
}
