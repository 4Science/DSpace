/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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
 * It can be used to verify if the user can execute the metadata import script.
 *
 * @author Francesco Molinaro (francesco.molinaro at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = CanImportMetadataFeature.NAME,
        description = "It can be used to verify if the user can execute the metadata import script.")
public class CanImportMetadataFeature implements AuthorizationFeature {

    @Autowired
    private ScriptService scriptService;

    @Lazy
    @Autowired
    protected ConverterService converter;


    @Autowired
    protected Utils utils;

    public static final String NAME = "canImportMetadata";

    private static final String importScript = "metadata-import";

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        ScriptConfiguration scriptConfiguration = scriptService.getScriptConfiguration(importScript);
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
