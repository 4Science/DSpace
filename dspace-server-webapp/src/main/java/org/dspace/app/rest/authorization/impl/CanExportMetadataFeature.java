package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;

import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.service.ScriptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;


/**
 * It can be used to verify if the user can execute the metadata export script.
 *
 * @author Francesco Molinaro (francesco.molinaro at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = CanExportMetadataFeature.NAME,
        description = "It can be used to verify if the user can execute the metadata export script.")
public class CanExportMetadataFeature implements AuthorizationFeature {

    @Autowired
    private ScriptService scriptService;

    @Lazy
    @Autowired
    protected ConverterService converter;


    @Autowired
    protected Utils utils;

    public static final String NAME = "canExportMetadata";

    private static final String exportScript = "metadata-export";

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        ScriptConfiguration scriptConfiguration = scriptService.getScriptConfiguration(exportScript);
        if (scriptConfiguration != null) {
            return scriptConfiguration.isAllowedToExecute(context, null);
        }

        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            SiteRest.CATEGORY + "." + SiteRest.NAME,
        };
    }

}
