/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.Options;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link MetadataExport} script
 */
public class MetadataExportScriptConfiguration<T extends MetadataExport> extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    public boolean isAllowedToExecute(Context context, List<DSpaceCommandLineParameter> commandLineParameters) {
        try {
            return authorizeService.isAdmin(context) || authorizeService.isComColAdmin(context) ||
                authorizeService.isItemAdmin(context);
        } catch (SQLException e) {
            throw new RuntimeException(
                "SQLException occurred when checking if the current user is eligible to run the script", e);
        }
    }

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    /**
     * Generic setter for the dspaceRunnableClass
     * @param dspaceRunnableClass   The dspaceRunnableClass to be set on this MetadataExportScriptConfiguration
     */
    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("i", "id", true, "ID or handle of thing to export (item, collection, or community)");
            options.addOption("a", "all", false,
                              "include all metadata fields that are not normally changed (e.g. provenance)");
            options.addOption("h", "help", false, "help");


            super.options = options;
        }
        return options;
    }

}
