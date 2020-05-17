/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.cris.json;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.discovery.ConfiguratorProfile;
import org.dspace.app.cris.discovery.ConfiguratorResource;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.json.JSONRequest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.handle.HandleManager;
import org.dspace.utils.DSpace;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * @author Luigi Andrea Pascarelli
 */
public class Resource extends JSONRequest
{

    private static Logger log = Logger.getLogger(Resource.class);

    private ApplicationService applicationService;
    private ConfiguratorResource configurator;
    
    @Override
    public void doJSONRequest(Context context, HttpServletRequest req,
            HttpServletResponse resp) throws AuthorizeException, IOException
    {

        Gson json = new Gson();

        List<JSDetailsDTO> dto = new ArrayList<JSDetailsDTO>();

        String profile = req.getParameter("profile");

        String pluginName = req.getPathInfo();

        if (pluginName.startsWith("/"))
        {
            pluginName = pluginName.substring(1);
            pluginName = pluginName.split("/")[1];
        }

        DSpaceObject object = applicationService.getEntityByCrisId(pluginName);
        if (object == null)
        {
            try
            {
                pluginName = pluginName.replace("_", "/");
                object = HandleManager.resolveToObject(context, pluginName);
                profile = "item-" + profile;
            }
            catch (IllegalStateException | SQLException e)
            {
                log.error(e.getMessage(), e);
            }
        }

        if (configurator.getProfile().containsKey(profile))
        {
            for (ConfiguratorProfile pp : configurator.getProfile()
                    .get(profile))
            {
                boolean addNode = false;
                JSDetailsDTO node = new JSDetailsDTO();
                String label = "";
                if (StringUtils.isNotBlank(pp.getLabelKey())) {
                    label = I18nUtil.getMessage(pp.getLabelKey(), context.getCurrentLocale(), false);
                }
                if (StringUtils.isBlank(label) || label.equals(pp.getLabelKey())) {
                    label = pp.getLabel();
                }
                node.setLabel(label);
                if (pp.isUrl())
                {
                    List<String> urls = new ArrayList<String>();
                    if (object.getType() >= CrisConstants.CRIS_TYPE_ID_START)
                    {
                        urls.add(ConfigurationManager.getProperty("dspace.url")+"/cris/uuid/"+object.getHandle());
                    }
                    else
                    {
                        try
                        {
                            urls.add(HandleManager.resolveToURL(context,
                                    object.getHandle()));
                        }
                        catch (SQLException e)
                        {
                            log.error(e.getMessage(), e);
                        }
                    }
                    node.setValue(urls);
                    addNode = true;
                }
                else
                {
                    List<String> metadataValue = object.getMetadataValue(pp.getMetadata());
                    if(metadataValue!=null && metadataValue.size()>0) {
                        addNode = true;
                    }
                    node.setValue(metadataValue);
                }
                node.setUrl(pp.isUrl());
                if(addNode) {
                    dto.add(node);
                }
            }
        }

        JsonElement tree = json.toJsonTree(dto);
        resp.getWriter().write(tree.toString());
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    public void setConfigurator(ConfiguratorResource configurator)
    {
        this.configurator = configurator;
    }

}
