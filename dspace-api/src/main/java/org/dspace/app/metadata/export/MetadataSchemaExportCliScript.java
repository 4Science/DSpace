/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metadata.export;

import java.io.File;

import org.apache.commons.cli.ParseException;
import org.dspace.core.Context;

/**
 * This script can be use to export a given {@code MetadataSchema} into its
 * registry file, that respects the standard DTD / XSD DSpace xml registry.
 * <p/>
 *  This script is supposed to work with the CLI (command-line-interface),
 *  it accepts only two parameters {@code -i <schema-id> -f <file-path>}
 *  respectively representing:
 *  <ul>
 *      <li><b>{@code schema-id}</b>: id of the schema to export</li>
 *      <li><b>{@code file-path}</b>:full file path of the file that will contain the export</li>
 *  <ul>
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 *
 */
public class MetadataSchemaExportCliScript extends MetadataSchemaExportScript {

    protected String filename;

    @Override
    public void setup() throws ParseException {
        super.setup();
        filename = commandLine.getOptionValue('f');
    }

    @Override
    protected File getExportedFile(Context context) throws DspaceExportMetadataSchemaException {
        try {
            File file = new File(filename);
            return metadataSchemaExportService.exportMetadataSchemaToFile(context, metadataSchema, file);
        } catch (DspaceExportMetadataSchemaException e) {
            handler.logError("Problem occured while exporting the schema to file: " + filename, e);
            throw e;
        }
    }

}
