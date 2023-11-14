/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metadata.export.service;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.dspace.app.metadata.export.DspaceExportMetadataSchemaException;
import org.dspace.app.metadata.export.model.DcSchema;
import org.dspace.app.metadata.export.model.DcSchemaBuilder;
import org.dspace.app.metadata.export.model.DspaceDcTypes;
import org.dspace.app.metadata.export.model.DspaceDcTypesBuilder;
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;

/**
 * This service can be used to export a target schema into a registry-file
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MetadataSchemaExportServiceImpl implements MetadataSchemaExportService {

    private MetadataSchemaService metadataSchemaService =
        ContentServiceFactory.getInstance().getMetadataSchemaService();

    @Override
    public DspaceDcTypes exportMetadataSchema(Context context, int schemaId) throws SQLException {
        return this.exportMetadataSchema(context, metadataSchemaService.find(context, schemaId));
    }

    @Override
    public DspaceDcTypes exportMetadataSchema(Context context, MetadataSchema metadataSchema) throws SQLException {
        return DspaceDcTypesBuilder
            .createBuilder()
            .withSchema(this.mapToDcSchema(metadataSchema))
            .withDcTypes(
                MetadataExportServiceFactory.getInstance()
                    .getMetadataFieldExportService()
                    .exportMetadataFieldsBy(context, metadataSchema)
            )
            .build();
    }

    @Override
    public File exportMetadataSchemaToFile(Context context, MetadataSchema metadataSchema)
        throws DspaceExportMetadataSchemaException {
        File tempFile;
        try {
            tempFile =
                File.createTempFile(
                    metadataSchema.getName() + "-" + metadataSchema.getID(),
                    ".xml"
                );
            tempFile.deleteOnExit();
            return this.exportMetadataSchemaToFile(context, metadataSchema, tempFile);
        } catch (IOException e) {
            throw new DspaceExportMetadataSchemaException(
                "Probelm occured during while exporting to temporary file!",
                e
            );
        }
    }

    @Override
    public File exportMetadataSchemaToFile(Context context, MetadataSchema metadataSchema, File file)
        throws DspaceExportMetadataSchemaException {
        try {
            DspaceDcTypes dspaceDcTypes = this.exportMetadataSchema(context, metadataSchema);

            JAXBContext jaxb = JAXBContext.newInstance(DspaceDcTypes.class);
            Marshaller jaxbMarshaller = jaxb.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            jaxbMarshaller.marshal(dspaceDcTypes, file);
        } catch (SQLException e) {
            throw new DspaceExportMetadataSchemaException(
                "Problem occured while retrieving data from DB!",
                e
            );
        } catch (JAXBException e) {
            throw new DspaceExportMetadataSchemaException(
                "Problem occured during the export to XML file!",
                e
            );
        }
        return file;
    }

    private DcSchema mapToDcSchema(MetadataSchema metadataSchema) {
        return DcSchemaBuilder
            .createBuilder()
            .withName(metadataSchema.getName())
            .withNamespace(metadataSchema.getNamespace())
            .build();
    }

}
