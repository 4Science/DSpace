/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.packager;

import java.io.InputStream;
import java.sql.SQLException;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The {@link ScriptConfiguration} for the {@link Packager} script
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class PackagerScriptConfiguration<T extends Packager> extends ScriptConfiguration<T> {

    @Autowired
    private AuthorizeService authorizeService;

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
    public boolean isAllowedToExecute(final Context context) {
        try {
            return authorizeService.isAdmin(context);
        } catch (SQLException e) {
            throw new RuntimeException("SQLException occurred when checking if the current user is an admin", e);
        }
    }

    @Override
    public Options getOptions() {
        Options options = new Options();
        options.addOption("p", "parent", true,
                          "Handle(s) of parent Community or Collection into which to ingest object (repeatable)");
        options
            .addOption(
                "w",
                "install",
                false,
                "disable workflow; install immediately without going through collection's workflow");
        options.addOption("r", "restore", false,
                          "ingest in \"restore\" mode.  Restores a missing object based on the contents in a package.");
        options.addOption("k", "keep-existing", false,
                          "if an object is found to already exist during a restore (-r), then keep the existing " +
                              "object and continue processing.  Can only be used with '-r'.  This avoids " +
                              "object-exists errors which are thrown by -r by default.");
        options.addOption("f", "force-replace", false,
                          "if an object is found to already exist during a restore (-r), then remove it and replace " +
                              "it with the contents of the package.  Can only be used with '-r'.  This REPLACES the " +
                              "object(s) in the repository with the contents from the package(s).");
        options.addOption("t", "type", true, "package type or MIMEtype");
        options
            .addOption("o", "option", true,
                       "Packager option to pass to plugin, \"name=value\" (repeatable)");
        options.addOption("d", "disseminate", false,
                          "Disseminate package (output); default is to submit.");
        options.addOption("s", "submit", false,
                          "Submission package (Input); this is the default. ");
        options.addOption("i", "identifier", true, "Handle of object to disseminate.");
        options.addOption("a", "all", false,
                          "also recursively ingest/disseminate any child packages, e.g. all Items within a Collection" +
                              " (not all packagers may support this option!)");
        options.addOption("h", "help", false,
                          "help (you may also specify '-h -t [type]' for additional help with a specific type of " +
                              "packager)");
        options.addOption(Option.builder("z").longOpt("file-path")
                .desc("The path to the file")
                .type(InputStream.class)
                .hasArg().build());

        return options;
    }
}
