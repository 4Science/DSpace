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
 *         &lt;element ref="{}title"/&gt;
 *         &lt;element ref="{}contributor.author"/&gt;
 *         &lt;element ref="{}contributor.editor"/&gt;
 *         &lt;element ref="{}date.created"/&gt;
 *         &lt;element ref="{}description"/&gt;
 *         &lt;element ref="{}description.version"/&gt;
 *       &lt;/choice&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "titleOrContributorAuthorOrContributorEditor"
})
@XmlRootElement(name = "dspace-header")
public class DspaceHeader {

    @XmlElementRefs({
        @XmlElementRef(name = "title", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "contributor.author", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "contributor.editor", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "date.created", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "description", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "description.version", type = JAXBElement.class, required = false)
    })
    protected List<JAXBElement<String>> titleOrContributorAuthorOrContributorEditor;

    /**
     * Gets the value of the titleOrContributorAuthorOrContributorEditor property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the titleOrContributorAuthorOrContributorEditor property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTitleOrContributorAuthorOrContributorEditor().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    public List<JAXBElement<String>> getTitleOrContributorAuthorOrContributorEditor() {
        if (titleOrContributorAuthorOrContributorEditor == null) {
            titleOrContributorAuthorOrContributorEditor = new ArrayList<JAXBElement<String>>();
        }
        return this.titleOrContributorAuthorOrContributorEditor;
    }

}
