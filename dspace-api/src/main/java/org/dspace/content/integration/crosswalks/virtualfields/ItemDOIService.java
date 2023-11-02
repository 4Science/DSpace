/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.Comparator;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;


public class ItemDOIService {
    static final String CFG_PREFIX = "identifier.doi.prefix";

    static final String DOI_METADATA = "dc.identifier.doi";

    @Autowired
    protected ItemService itemService;
    @Autowired
    private ConfigurationService configurationService;

    public String[] getAlternativeDOIFromItem(Item item) {
        List<MetadataValue> metadataValueList = itemService.getMetadataByMetadataString(item, DOI_METADATA);
        return getAlternativeDOI(metadataValueList, getPrimaryDOI(metadataValueList));
    }
    private String[] getAlternativeDOI(List<MetadataValue> metadataValueList, String primaryValue) {
        return metadataValueList.stream().map(MetadataValue::getValue)
                .filter(value -> !value.equals(primaryValue)).toArray(String[]::new);
    }

    public String getPrimaryDOIFromItem(Item item) {
        return getPrimaryDOI(itemService.getMetadataByMetadataString(item, DOI_METADATA));
    }

    private String getPrimaryDOI(List<MetadataValue> metadataValueList) {
        return metadataValueList.stream().filter(metadata -> metadata.getValue().contains(getPrefix()))
                .min(Comparator.comparingInt(MetadataValue::getPlace)).map(MetadataValue::getValue)
                .orElse(!metadataValueList.isEmpty() ? metadataValueList.get(0).getValue() : null);
    }

    protected String getPrefix() {
        String prefix;
        prefix = this.configurationService.getProperty(CFG_PREFIX);
        if (null == prefix) {
            throw new RuntimeException("Unable to load DOI prefix from "
                    + "configuration. Cannot find property " +
                    CFG_PREFIX + ".");
        }
        return prefix;
    }
}
