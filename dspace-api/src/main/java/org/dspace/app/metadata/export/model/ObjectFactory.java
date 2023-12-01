/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metadata.export.model;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the org.dspace.app.metadata.export.model package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Title_QNAME = new QName("", "title");
    private final static QName _ContributorAuthor_QNAME = new QName("", "contributor.author");
    private final static QName _ContributorEditor_QNAME = new QName("", "contributor.editor");
    private final static QName _DateCreated_QNAME = new QName("", "date.created");
    private final static QName _Description_QNAME = new QName("", "description");
    private final static QName _DescriptionVersion_QNAME = new QName("", "description.version");
    private final static QName _Name_QNAME = new QName("", "name");
    private final static QName _Namespace_QNAME = new QName("", "namespace");
    private final static QName _Schema_QNAME = new QName("", "schema");
    private final static QName _Element_QNAME = new QName("", "element");
    private final static QName _Qualifier_QNAME = new QName("", "qualifier");
    private final static QName _ScopeNote_QNAME = new QName("", "scope_note");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org
     * .dspace.app.metadata.export.model
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link DspaceDcTypes }
     */
    public DspaceDcTypes createDspaceDcTypes() {
        return new DspaceDcTypes();
    }

    /**
     * Create an instance of {@link DspaceHeader }
     */
    public DspaceHeader createDspaceHeader() {
        return new DspaceHeader();
    }

    /**
     * Create an instance of {@link DcSchema }
     */
    public DcSchema createDcSchema() {
        return new DcSchema();
    }

    /**
     * Create an instance of {@link DcType }
     */
    public DcType createDcType() {
        return new DcType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "title")
    public JAXBElement<String> createTitle(String value) {
        return new JAXBElement<String>(_Title_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "contributor.author")
    public JAXBElement<String> createContributorAuthor(String value) {
        return new JAXBElement<String>(_ContributorAuthor_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "contributor.editor")
    public JAXBElement<String> createContributorEditor(String value) {
        return new JAXBElement<String>(_ContributorEditor_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "date.created")
    public JAXBElement<String> createDateCreated(String value) {
        return new JAXBElement<String>(_DateCreated_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "description")
    public JAXBElement<String> createDescription(String value) {
        return new JAXBElement<String>(_Description_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "description.version")
    public JAXBElement<String> createDescriptionVersion(String value) {
        return new JAXBElement<String>(_DescriptionVersion_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "name")
    public JAXBElement<String> createName(String value) {
        return new JAXBElement<String>(_Name_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "namespace")
    public JAXBElement<String> createNamespace(String value) {
        return new JAXBElement<String>(_Namespace_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "schema")
    public JAXBElement<String> createSchema(String value) {
        return new JAXBElement<String>(_Schema_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "element")
    public JAXBElement<String> createElement(String value) {
        return new JAXBElement<String>(_Element_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "qualifier")
    public JAXBElement<String> createQualifier(String value) {
        return new JAXBElement<String>(_Qualifier_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value Java instance representing xml element's value.
     * @return the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "scope_note")
    public JAXBElement<String> createScopeNote(String value) {
        return new JAXBElement<String>(_ScopeNote_QNAME, String.class, null, value);
    }

}
