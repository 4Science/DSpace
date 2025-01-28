/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.script.migrateEntity;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.Options;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class MigrateEntityScriptConfiguration<T extends MigrateEntity> extends ScriptConfiguration<T> {

    private static final Logger log = LoggerFactory.getLogger(MigrateEntityScriptConfiguration.class);

    private Class<T> dspaceRunnableClass;

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public boolean isAllowedToExecute(Context context, List<DSpaceCommandLineParameter> commandLineParameters) {
        try {
            return authorizeService.isAdmin(context);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();
            options.addOption("u", "uuid", true, "Collection or Community uuid");
            options.getOption("u").setType(String.class);
            options.getOption("u").setRequired(false);

            options.addOption("i", "handle", true, "Collection or Community handle");
            options.getOption("i").setType(String.class);
            options.getOption("i").setRequired(false);

            options.addOption("n", "entitytype", true, "new EntityType");
            options.getOption("n").setType(String.class);
            options.getOption("n").setRequired(false);

            options.addOption("f", "formname", true, "new Submission form name");
            options.getOption("f").setType(String.class);
            options.getOption("f").setRequired(false);
            super.options = options;
        }
        return options;
    }

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

}
