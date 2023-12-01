/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metadata.export;

import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.text.MessageFormat;

import org.apache.commons.cli.ParseException;
import org.dspace.app.metadata.export.service.MetadataExportServiceFactory;
import org.dspace.app.metadata.export.service.MetadataSchemaExportService;
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * This script can be use to export a given {@code MetadataSchema} into its
 * registry file, that respects the standard DTD / XSD DSpace xml registry.
 * <p/>
 *  This script is supposed to work with the webapp, it accepts only one
 *  parameter {@code -i <schema-id>} representing the id of the schema that
 *  will be exported.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MetadataSchemaExportScript
    extends DSpaceRunnable<MetadataSchemaExportScriptConfiguration<MetadataSchemaExportScript>> {

    protected static String REGISTRY_FILENAME_TEMPLATE = "{0}-types.xml";

    protected MetadataSchemaService metadataSchemaService =
        ContentServiceFactory.getInstance().getMetadataSchemaService();

    protected MetadataSchemaExportService metadataSchemaExportService =
        MetadataExportServiceFactory.getInstance().getMetadataSchemaExportService();

    protected boolean help;
    protected int id;

    protected MetadataSchema metadataSchema;

    @Override
    public MetadataSchemaExportScriptConfiguration getScriptConfiguration() {
        return DSpaceServicesFactory
            .getInstance().getServiceManager()
            .getServiceByName("export-schema", MetadataSchemaExportScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        help = commandLine.hasOption('h');
        try {
            id = Integer.parseInt(commandLine.getOptionValue('i'));
        } catch (Exception e) {
            handler.logError("Cannot parse the id argument ( " + id + " )! You should provide an integer!");
            throw new ParseException("Cannot parse the id argument ( " + id + " )! You should provide an integer!");
        }
    }

    @Override
    public void internalRun() throws Exception {
        if (help) {
            printHelp();
            return;
        }

        Context context = new Context();
        try {
            validate(context);
            exportMetadataSchema(context);
        } catch (Exception e) {
            context.abort();
            throw e;
        }
    }

    private void validate(Context context) throws SQLException, ParseException {
        metadataSchema = this.metadataSchemaService.find(context, id);
        if (metadataSchema == null) {
            handler.logError("Cannot find the metadata-schema with id: " + id);
            throw new ParseException("Cannot find the metadata-schema with id: " + id);
        }
    }

    private void exportMetadataSchema(Context context) throws Exception {
        handler.logInfo(
            "Exporting the metadata-schema file for the schema " + metadataSchema.getName()
        );
        try {
            File tempFile = getExportedFile(context);

            handler.logInfo(
                "Exported to file: " + tempFile.getAbsolutePath()
            );

            try (FileInputStream fis = new FileInputStream(tempFile)) {
                handler.logInfo("Summarizing export ...");
                context.turnOffAuthorisationSystem();
                handler.writeFilestream(
                    context, getFilename(metadataSchema), fis, "application/xml", false
                );
                context.restoreAuthSystemState();
            }
        } catch (Exception e) {
            handler.logError("Problem occured while exporting the schema!", e);
            throw e;
        }
    }

    protected String getFilename(MetadataSchema ms) {
        return MessageFormat.format(REGISTRY_FILENAME_TEMPLATE, ms.getName());
    }

    protected File getExportedFile(Context context) throws DspaceExportMetadataSchemaException {
        return this.metadataSchemaExportService.exportMetadataSchemaToFile(context, metadataSchema);
    }
}
