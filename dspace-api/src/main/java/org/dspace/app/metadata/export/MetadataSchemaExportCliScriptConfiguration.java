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

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 *
 */
public class MetadataSchemaExportCliScriptConfiguration
    extends MetadataSchemaExportScriptConfiguration<MetadataSchemaExportCliScript> {

    @Override
    public Options getOptions() {
        Options options = super.getOptions();

        options.addOption(
            Option.builder("f").longOpt("file")
                .desc("The temporary file-name to use")
                .hasArg()
                .build()
        );

        return options;
    }

}
