/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import org.apache.commons.cli.Options;

/**
 * Extension of {@link CollectionExportScriptConfiguration} for CLI.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CollectionExportCliScriptConfiguration extends CollectionExportScriptConfiguration<CollectionExportCli> {

    @Override
    public Options getOptions() {
        Options options = super.getOptions();
        options.addOption("e", "email", true, "email address of user");
        options.getOption("e").setRequired(true);
        super.options = options;
        return options;
    }

}
