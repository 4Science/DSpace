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
import java.util.stream.Collectors;

import org.dspace.app.metadata.export.model.DcType;
import org.dspace.app.metadata.export.model.DcTypeBuilder;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MetadataFieldExportServiceImpl implements MetadataFieldExportService {

    private MetadataFieldService metadataFieldService =
        ContentServiceFactory.getInstance().getMetadataFieldService();

    public List<DcType> exportMetadataFieldsBy(Context context, MetadataSchema metadataSchema) throws SQLException {
        return metadataFieldService
            .findAllInSchema(context, metadataSchema)
            .stream()
            .map(this::toDcType)
            .collect(Collectors.toList());
    }

    private DcType toDcType(MetadataField metadataField) {
        return DcTypeBuilder
            .createBuilder()
            .withSchema(metadataField.getMetadataSchema().getName())
            .withElement(metadataField.getElement())
            .withQualifier(metadataField.getQualifier())
            .withScopeNote(metadataField.getScopeNote())
            .build();
    }

}

