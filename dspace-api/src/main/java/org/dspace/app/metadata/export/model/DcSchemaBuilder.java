/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metadata.export.model;

import javax.xml.bind.JAXBElement;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class DcSchemaBuilder extends AbstractJaxbBuilder<DcSchema, String> {

    protected DcSchemaBuilder() {
        super(DcSchema.class);
    }

    public static DcSchemaBuilder createBuilder() {
        return new DcSchemaBuilder();
    }

    public DcSchemaBuilder withName(String name) {
        this.addChildElement(name, objectFactory::createName);
        return this;
    }

    public DcSchemaBuilder withNamespace(String namespace) {
        this.addChildElement(namespace, objectFactory::createNamespace);
        return this;
    }

    @Override
    protected void addChildElement(JAXBElement<String> v) {
        getObejct().getNameOrNamespace().add(v);
    }
}
