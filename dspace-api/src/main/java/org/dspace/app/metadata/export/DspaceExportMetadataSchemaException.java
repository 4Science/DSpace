/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metadata.export;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class DspaceExportMetadataSchemaException extends Exception {

    public DspaceExportMetadataSchemaException(Exception e) {
        super(e);
    }

    public DspaceExportMetadataSchemaException(String message, Exception e) {
        super(message, e);
    }

}
