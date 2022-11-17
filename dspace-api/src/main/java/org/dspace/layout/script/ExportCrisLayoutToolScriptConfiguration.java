/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script;

import java.sql.SQLException;

import org.apache.commons.cli.Options;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Configuration for {@link ExportCrisLayoutToolScript}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class ExportCrisLayoutToolScriptConfiguration<T extends ExportCrisLayoutToolScript>
    extends ScriptConfiguration<T> {

    @Autowired
    private AuthorizeService authorizeService;

    private Class<T> dspaceRunnableClass;

    @Override
    public boolean isAllowedToExecute(Context context) {
        try {
            return authorizeService.isAdmin(context);
        } catch (SQLException e) {
            throw new RuntimeException("SQLException occurred when checking if the current user is an admin", e);
        }
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            options = new Options();
        }
        return options;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

}
