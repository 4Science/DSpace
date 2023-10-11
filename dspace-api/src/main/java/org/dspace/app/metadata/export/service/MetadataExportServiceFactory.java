/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metadata.export.service;

import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Factory for the export services related to metadata-schema and metadata-fields.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public abstract class MetadataExportServiceFactory {

    public static MetadataExportServiceFactory getInstance() {
        return DSpaceServicesFactory
            .getInstance().getServiceManager()
            .getServiceByName("metadataExportServiceFactory", MetadataExportServiceFactory.class);
    }

    public abstract MetadataSchemaExportService getMetadataSchemaExportService();
    public abstract MetadataFieldExportService getMetadataFieldExportService();

}
