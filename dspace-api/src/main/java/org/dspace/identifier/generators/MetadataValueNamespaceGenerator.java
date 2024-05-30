/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.generators;

import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Provide custom doi generation based on certain criteria.
 *
 *
 * @author Stefano Maffei (steph-ieffam @ 4Science.com)
 */

public class MetadataValueNamespaceGenerator implements DoiNamespaceGenerator {

    protected final Set<String> metadataFields;
    protected final String postValue;
    protected final String namespaceSeparator;
    @Autowired(required = true)
    private transient ItemService itemService;
    @Autowired(required = true)
    private ConfigurationService configurationService;

    MetadataValueNamespaceGenerator(
        String namespaceSeparator,
        String postValue,
        Set<String> metadataFields
    ) {
        this.namespaceSeparator = namespaceSeparator;
        this.postValue = postValue;
        this.metadataFields = metadataFields;
    }

    @Override
    public String getNamespace(Context context, Item item) {
        for (String metadataField : metadataFields) {
            String metadataValue = itemService.getMetadata(item, metadataField);
            if (StringUtils.isBlank(metadataValue)) {
                continue;
            }
            return getFormattedNamespace(metadataValue);
        }
        return getFormattedNamespace(namespaceSeparator);
    }

    private String getFormattedNamespace(String prefix) {
        return prefix + getPostValue();
    }

    private String getPostValue() {
        return Optional.ofNullable(postValue).orElse("");
    }

}
