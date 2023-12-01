/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metadata.export.service;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MetadataExportServiceFactoryImpl extends MetadataExportServiceFactory {

    @Autowired
    private MetadataSchemaExportService metadataSchemaExportService;
    @Autowired
    private MetadataFieldExportService metadataFieldExportService;

    @Override
    public MetadataSchemaExportService getMetadataSchemaExportService() {
        return metadataSchemaExportService;
    }

    @Override
    public MetadataFieldExportService getMetadataFieldExportService() {
        return metadataFieldExportService;
    }
}
