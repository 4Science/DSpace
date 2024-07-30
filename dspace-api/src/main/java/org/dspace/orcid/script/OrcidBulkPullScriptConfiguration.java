/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.script;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * Script configuration for {@link OrcidBulkPull}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 * @param  <T> the type of OrcidBulkPull
 */
public class OrcidBulkPullScriptConfiguration<T extends OrcidBulkPull> extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("l", "linked", false, "to simulates the callback only for linked profiles");
            options.getOption("l").setType(boolean.class);
            options.getOption("l").setRequired(false);

            super.options = options;
        }
        return options;
    }

}
