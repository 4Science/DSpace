/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metadata.export.model;

import java.util.Collection;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class DspaceDcTypesBuilder {

    private DspaceDcTypes dcTypes;

    private final ObjectFactory objectFactory = new ObjectFactory();

    private DspaceDcTypes getDcTypes() {
        if (dcTypes == null) {
            dcTypes = new DspaceDcTypes();
        }
        return dcTypes;
    }

    private DspaceDcTypesBuilder() {
    }

    public static DspaceDcTypesBuilder createBuilder() {
        return new DspaceDcTypesBuilder();
    }

    public DspaceDcTypesBuilder witheader(DspaceHeader header) {
        this.getDcTypes().getDspaceHeaderOrDcSchemaOrDcType().add(header);
        return this;
    }

    public DspaceDcTypesBuilder withSchema(DcSchema schema) {
        this.getDcTypes().getDspaceHeaderOrDcSchemaOrDcType().add(schema);
        return this;
    }

    public DspaceDcTypesBuilder withDcType(DcType dcType) {
        this.getDcTypes().getDspaceHeaderOrDcSchemaOrDcType().add(dcType);
        return this;
    }

    public DspaceDcTypesBuilder withDcTypes(Collection<DcType> dcTypes) {
        this.getDcTypes().getDspaceHeaderOrDcSchemaOrDcType().addAll(dcTypes);
        return this;
    }

    public DspaceDcTypes build() {
        return dcTypes;
    }

}
