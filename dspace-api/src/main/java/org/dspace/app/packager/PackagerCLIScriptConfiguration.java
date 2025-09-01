/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.packager;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link PackagerCLI} script
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class PackagerCLIScriptConfiguration extends PackagerScriptConfiguration<PackagerCLI> {

    @Override
    public Options getOptions() {
        Options options = new Options();

        options.addOption("p", "parent", true,
                          "Handle(s) of parent Community or Collection into which to ingest object (repeatable)");
        options.addOption("e", "eperson", true,
                          "email address of eperson doing importing");
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
        options.addOption("u", "no-user-interaction", false,
                          "Skips over all user interaction (i.e. [y/n] question prompts) within this script. This " +
                              "flag can be used if you want to save (pipe) a report of all changes to a file, and " +
                              "therefore need to bypass all user interaction.");
        return options;
    }
}
