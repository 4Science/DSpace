/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.beacon;

import java.sql.SQLException;

import org.apache.commons.cli.Options;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Script configuration for {@link BeaconFileScript}.
 *
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 */
public class BeaconFileScriptConfiguration<T extends BeaconFileScript> extends ScriptConfiguration<T> {

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
            Options options = new Options();

            options.addOption("v", "verbose", false, "print out result on handler log");
            options.getOption("v").setType(boolean.class);
            options.getOption("v").setRequired(false);

            options.addOption("f", "file", true, "print to file and specify filename, e.g. beacon.txt");
            options.getOption("f").setType(String.class);
            options.getOption("f").setRequired(false);

            super.options = options;
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
