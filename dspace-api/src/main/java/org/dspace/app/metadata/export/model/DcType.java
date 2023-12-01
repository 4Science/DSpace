/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metadata.export.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per anonymous complex type.
 *
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 *
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;choice maxOccurs="unbounded" minOccurs="0"&gt;
 *         &lt;element ref="{}schema"/&gt;
 *         &lt;element ref="{}element"/&gt;
 *         &lt;element ref="{}qualifier"/&gt;
 *         &lt;element ref="{}scope_note"/&gt;
 *       &lt;/choice&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "schemaOrElementOrQualifier"
})
@XmlRootElement(name = "dc-type")
public class DcType {

    @XmlElementRefs({
        @XmlElementRef(name = "schema", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "element", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "qualifier", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "scope_note", type = JAXBElement.class, required = false)
    })
    protected List<JAXBElement<String>> schemaOrElementOrQualifier;

    /**
     * Gets the value of the schemaOrElementOrQualifier property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the schemaOrElementOrQualifier property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSchemaOrElementOrQualifier().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public List<JAXBElement<String>> getSchemaOrElementOrQualifier() {
        if (schemaOrElementOrQualifier == null) {
            schemaOrElementOrQualifier = new ArrayList<JAXBElement<String>>();
        }
        return this.schemaOrElementOrQualifier;
    }

}
