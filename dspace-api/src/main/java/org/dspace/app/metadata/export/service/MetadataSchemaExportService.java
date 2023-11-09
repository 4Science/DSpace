/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metadata.export.service;

import java.io.File;
import java.sql.SQLException;

import org.dspace.app.metadata.export.DspaceExportMetadataSchemaException;
import org.dspace.app.metadata.export.model.DspaceDcTypes;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public interface MetadataSchemaExportService {

    /**
     * Exports the given {@code schemaId} into a {@link DspaceDcTypes} entity
     *
     * @param context
     * @param schemaId
     * @return
     * @throws SQLException
     */
    DspaceDcTypes exportMetadataSchema(Context context, int schemaId) throws SQLException;

    /**
     * Exports the given {@code metadataSchema} into a {@link DspaceDcTypes} entity
     *
     * @param context
     * @param metadataSchema
     * @return
     * @throws SQLException
     */
    DspaceDcTypes exportMetadataSchema(Context context, MetadataSchema metadataSchema) throws SQLException;

    /**
     * Exports the given {@code metadataSchema} to a temporary {@code File},
     * that will respect the {@code registry} xml format of dspace
     *
     * @param context
     * @param metadataSchema
     * @return
     * @throws DspaceExportMetadataSchemaException
     */
    File exportMetadataSchemaToFile(Context context, MetadataSchema metadataSchema)
        throws DspaceExportMetadataSchemaException;

    /**
     * Exports the given {@code metadataSchema} to a target {@code File},
     * that will respect the {@code registry} xml format of dspace
     *
     * @param context
     * @param metadataSchema
     * @param file
     * @return
     * @throws DspaceExportMetadataSchemaException
     */
    File exportMetadataSchemaToFile(Context context, MetadataSchema metadataSchema, File file)
        throws DspaceExportMetadataSchemaException;

}
