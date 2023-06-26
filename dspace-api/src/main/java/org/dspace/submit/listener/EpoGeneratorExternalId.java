/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.listener;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.dspace.importer.external.epo.service.EpoImportMetadataSourceServiceImpl.APP_NO_DATE_SEPARATOR;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.external.provider.impl.LiveImportDataProvider;
import org.dspace.importer.external.epo.service.EpoImportMetadataSourceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class used to generate identifire to query EPO provider.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class EpoGeneratorExternalId implements ExternalIdGenerator {

    public static final String PATENT_METADATA = "dc.identifier.patentno";

    @Autowired
    private ItemService itemService;

    /**
     * This method handles two cases to generate an identifier.
     * First one: when Application Number and Filled Date are not empty and generate the following ID:
     *            ApplicationNumber$$$FilledDate (ex:202013003816$$$2013-01-24).
     * Second one: if Patent Numeber is not empty, it returns this as an identifier.
     * Otherwise, it returns the empty string.
     */
    @Override
    public String generateExternalId(Context context, ExternalDataProvider provider, Item item, String metadata) {
        EpoImportMetadataSourceServiceImpl epoProv = (EpoImportMetadataSourceServiceImpl)
                ((LiveImportDataProvider) provider).getQuerySource();
        String dateFilled = epoProv.getDateFilled().getField();
        String applicationNumber = epoProv.getApplicationNumber().getField();
        // first case
        if (StringUtils.equals(metadata, dateFilled) || StringUtils.equals(metadata, applicationNumber)) {
            return generateApplicationNumberAndFilledDateID(item, dateFilled, applicationNumber);
        }
        // second case
        if (StringUtils.equals(metadata, PATENT_METADATA)) {
            List<MetadataValue> patentNumberValue = itemService.getMetadataByMetadataString(item, PATENT_METADATA);
            return CollectionUtils.isNotEmpty(patentNumberValue) ? patentNumberValue.get(0).getValue() : EMPTY;
        }
        return EMPTY;
    }

    private String generateApplicationNumberAndFilledDateID(Item item, String dateFilled, String applicationNumber) {
        List<MetadataValue> dateFilledValue = itemService.getMetadataByMetadataString(item, dateFilled);
        List<MetadataValue> applicationNumberValue = itemService.getMetadataByMetadataString(item, applicationNumber);
        if (CollectionUtils.isNotEmpty(dateFilledValue) && CollectionUtils.isNotEmpty(applicationNumberValue)) {
            return applicationNumberValue.get(0).getValue() + APP_NO_DATE_SEPARATOR + dateFilledValue.get(0).getValue();
        }
        return EMPTY;
    }

    @Override
    public boolean support(ExternalDataProvider provider) {
        return provider instanceof LiveImportDataProvider &&
               ((LiveImportDataProvider) provider).getQuerySource() instanceof EpoImportMetadataSourceServiceImpl;
    }

}