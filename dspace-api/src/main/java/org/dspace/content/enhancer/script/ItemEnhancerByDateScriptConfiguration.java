/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.script;

import java.sql.SQLException;

import org.apache.commons.cli.Options;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Script configuration of {@link ItemEnhancerEntityTypeScript}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 */
public class ItemEnhancerByDateScriptConfiguration<T extends ItemEnhancerByDateScript> extends ScriptConfiguration<T> {

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

            options.addOption("f", "force", false, "force the usage of the deep mode"
                + " (always compute the enhanced metadata to verify if the item need an update)");
            options.addOption("c", "collection", true,
                "uuid of the collection. If the collection does not exist the script aborts.");
            options.addOption("e", "entity", true, "Entity type of the items");
            options.addOption("d", "dateupper", true,
                "iso date as upper range of  date query for lastModified. e.g. 2022-10-27T12:12:17.369Z ");
            options.addOption("s", "datelower", true, "iso date as lower range of  date query for lastModified ");
            options.addOption("m", "max", true, "--max results/rows from solr");
            options.addOption("l", "limit", true, "commit after --limit entities processed");
            options.addOption("q", "query", true,
                "additional filterquery for the entities. this can f.e. be the exclusion of already enhanced metadata");

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
