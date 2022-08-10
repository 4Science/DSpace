/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jdom2.Element;
import org.jdom2.Namespace;


public class SimpleXpathDateFormatMetadataContributor extends SimpleXpathMetadatumContributor {


    private DateFormat dateFormatFrom;
    private DateFormat dateFormatTo;

    public void setDateFormatFrom(String dateFormatFrom) {
        this.dateFormatFrom = new SimpleDateFormat(dateFormatFrom);
    }

    public void setDateFormatTo(String dateFormatTo) {
        this.dateFormatTo = new SimpleDateFormat(dateFormatTo);
    }

    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element el) {
        List<MetadatumDTO> values = new LinkedList<>();
        for (String ns : prefixToNamespaceMapping.keySet()) {
            List<Element> nodes = el.getChildren(query, Namespace.getNamespace(ns));
            for (Element element : nodes) {
                values.add(getMetadatum(field, element.getValue()));
            }
        }
        return values;
    }

    private MetadatumDTO getMetadatum(MetadataFieldConfig field, String value) {
        MetadatumDTO dcValue = new MetadatumDTO();
        if (field == null) {
            return null;
        }
        try {
            dcValue.setValue(dateFormatTo.format(dateFormatFrom.parse(value)));
        } catch (ParseException e) {
            dcValue.setValue(value);
        }
        dcValue.setElement(field.getElement());
        dcValue.setQualifier(field.getQualifier());
        dcValue.setSchema(field.getSchema());
        return dcValue;
    }
}
