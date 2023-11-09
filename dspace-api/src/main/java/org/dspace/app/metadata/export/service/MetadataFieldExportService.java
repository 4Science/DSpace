/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metadata.export.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.metadata.export.model.DcType;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;

/**
 * Exports {@code MetadataField} into {@code DcType}
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public interface MetadataFieldExportService {

    /**
     * Creates a one {@link DCType} for each {@link MetadataField}
     * in the given {@link MetadataSchema}, and returns them in a list
     *
     * @param context
     * @param metadataSchema
     * @return
     * @throws SQLException
     */
    List<DcType> exportMetadataFieldsBy(Context context, MetadataSchema metadataSchema) throws SQLException;
}
