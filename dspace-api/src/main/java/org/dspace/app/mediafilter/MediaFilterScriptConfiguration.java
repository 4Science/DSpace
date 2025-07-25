/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import static org.dspace.app.mediafilter.MediaFilterServiceImpl.MEDIA_FILTER_PLUGINS_KEY;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.configuration.ScriptConfiguration;

public class MediaFilterScriptConfiguration<T extends MediaFilterScript> extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
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

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    @Override
    public Options getOptions() {
        Options options = new Options();
        options.addOption("v", "verbose", false, "print all extracted text and other details to STDOUT");
        options.addOption("q", "quiet", false, "do not print anything except in the event of errors.");
        options.addOption("f", "force", false, "force all bitstreams to be processed");
        options.addOption("i", "identifier", true,
            "ONLY process bitstreams belonging to the provided handle identifier");
        options.addOption("l", "last", true,
            "ONLY process bitstreams belonging to items modified since the number of days specified"
            + " (as integer number i.e. 1, 2, 3, etc). CANNOT BE combined with an identifier, use only"
            + " when executed over the whole repository");
        options.addOption("b", "bundle", true,
            "ONLY process bistreams that have no bundles with the specified names");

        options.addOption("m", "maximum", true, "process no more than maximum items");
        options.addOption("h", "help", false, "help");

        Option pluginOption = Option.builder("p")
                                    .longOpt("plugins")
                                    .hasArg()
                                    .hasArgs()
                                    .valueSeparator(',')
                                    .desc(
                                            "ONLY run the specified Media Filter plugin(s)\n" +
                                                    "listed from '" + MEDIA_FILTER_PLUGINS_KEY + "' in dspace.cfg.\n" +
                                                    "Separate multiple with a comma (,)\n" +
                                                    "(e.g. MediaFilterManager -p \n\"Word Text Extractor\",\"PDF Text" +
                                                    " Extractor\")")
                                    .build();
        options.addOption(pluginOption);

        options.addOption("d", "fromdate", true, "Process only item from specified last modified date");

        Option skipOption = Option.builder("s")
                                  .longOpt("skip")
                                  .hasArg()
                                  .hasArgs()
                                  .valueSeparator(',')
                                  .desc(
                                          "SKIP the bitstreams belonging to identifier\n" +
                                                  "Separate multiple identifiers with a comma (,)\n" +
                                                  "(e.g. MediaFilterManager -s \n 123456789/34,123456789/323)")
                                  .build();
        options.addOption(skipOption);

        return options;
    }
}
