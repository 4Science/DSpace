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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
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
 *         &lt;element ref="{}dspace-header"/&gt;
 *         &lt;element ref="{}dc-schema"/&gt;
 *         &lt;element ref="{}dc-type"/&gt;
 *       &lt;/choice&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "dspaceHeaderOrDcSchemaOrDcType"
})
@XmlRootElement(name = "dspace-dc-types")
public class DspaceDcTypes {

    @XmlElements({
        @XmlElement(name = "dspace-header", type = DspaceHeader.class),
        @XmlElement(name = "dc-schema", type = DcSchema.class),
        @XmlElement(name = "dc-type", type = DcType.class)
    })
    protected List<Object> dspaceHeaderOrDcSchemaOrDcType;

    /**
     * Gets the value of the dspaceHeaderOrDcSchemaOrDcType property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dspaceHeaderOrDcSchemaOrDcType property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDspaceHeaderOrDcSchemaOrDcType().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DspaceHeader }
     * {@link DcSchema }
     * {@link DcType }
     */
    public List<Object> getDspaceHeaderOrDcSchemaOrDcType() {
        if (dspaceHeaderOrDcSchemaOrDcType == null) {
            dspaceHeaderOrDcSchemaOrDcType = new ArrayList<Object>();
        }
        return this.dspaceHeaderOrDcSchemaOrDcType;
    }

}
