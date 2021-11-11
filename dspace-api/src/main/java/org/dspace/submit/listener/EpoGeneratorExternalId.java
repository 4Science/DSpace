/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.listener;

import java.util.List;
import java.util.Objects;

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
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class EpoGeneratorExternalId implements ExternalIdGenerator {

    @Autowired
    private ItemService itemService;

    @Override
    public String generateExternalId(Context context, ExternalDataProvider provider, Item item, String metadata) {
        EpoImportMetadataSourceServiceImpl epoProv = (EpoImportMetadataSourceServiceImpl)
                ((LiveImportDataProvider) provider).getQuerySource();
        String dateFiledMd = epoProv.getDateFiled().getField();
        String applicationNumberMd = epoProv.getApplicationNumber().getField();
        if (StringUtils.equals(metadata, dateFiledMd)
                || StringUtils.equals(metadata, applicationNumberMd)) {
            List<MetadataValue> dateFiled = itemService.getMetadataByMetadataString(item, dateFiledMd);
            List<MetadataValue> applicNo = itemService.getMetadataByMetadataString(item, applicationNumberMd);
            if (Objects.nonNull(dateFiled) && dateFiled.size() == 1 &&
                Objects.nonNull(applicNo) && applicNo.size() == 1) {
                return applicNo.get(0).getValue() + EpoImportMetadataSourceServiceImpl.APP_NO_DATE_SEPARATOR
                        + dateFiled.get(0).getValue();
            } else {
                return StringUtils.EMPTY;
            }
        }
        return StringUtils.EMPTY;
    }

    @Override
    public boolean support(ExternalDataProvider provider) {
        return provider instanceof LiveImportDataProvider &&
               ((LiveImportDataProvider) provider).getQuerySource() instanceof EpoImportMetadataSourceServiceImpl;
    }

}