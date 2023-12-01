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
public class DcTypeBuilder extends AbstractJaxbBuilder<DcType, String> {

    protected DcTypeBuilder() {
        super(DcType.class);
    }

    public static DcTypeBuilder createBuilder() {
        return new DcTypeBuilder();
    }

    public DcTypeBuilder withSchema(String schema) {
        addChildElement(schema, objectFactory::createSchema);
        return this;
    }

    public DcTypeBuilder withElement(String element) {
        addChildElement(element, objectFactory::createElement);
        return this;
    }

    public DcTypeBuilder withQualifier(String qualifier) {
        addChildElement(qualifier, objectFactory::createQualifier);
        return this;
    }

    public DcTypeBuilder withScopeNote(String scopeNote) {
        addChildElement(scopeNote, objectFactory::createScopeNote);
        return this;
    }

    @Override
    protected void addChildElement(JAXBElement<String> v) {
        getObejct().getSchemaOrElementOrQualifier().add(v);
    }
}
