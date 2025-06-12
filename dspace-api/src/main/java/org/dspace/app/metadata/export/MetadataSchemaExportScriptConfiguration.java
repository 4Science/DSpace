/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metadata.export;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * Configuration of the Script {@code MetadataSchemaExportScript}
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MetadataSchemaExportScriptConfiguration<T extends MetadataSchemaExportScript>
    extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public Class<T> getDspaceRunnableClass() {
        return this.dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }


    @Override
    public Options getOptions() {
        Options options = new Options();

        options.addOption(
            Option.builder("i").longOpt("id")
                .desc("Metadata schema id")
                .hasArg()
                .required()
                .build()
        );

        options.addOption(
            Option.builder("h").longOpt("help")
                .desc("help")
                .hasArg(false)
                .required(false)
                .build()
        );

        return options;
    }
}
