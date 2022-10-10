/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.listener;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.external.provider.ExternalDataProvider;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class DefaultGeneratorExternalId implements ExternalIdGenerator {

    private Map<String, String> metadata2regex;

    @Autowired
    private ItemService itemService;

    @Override
    public String generateExternalId(Context context, ExternalDataProvider provider, Item item, String metadata) {
        List<MetadataValue> metadataByMetadataString = itemService.getMetadataByMetadataString(item, metadata);
        if (Objects.nonNull(metadataByMetadataString) && metadataByMetadataString.size() == 1) {
            return checkValue(metadataByMetadataString.get(0).getValue(), metadata2regex.get(metadata));
        }
        return StringUtils.EMPTY;
    }

    private String checkValue(String value, String regex) {
        if (StringUtils.isNotBlank(value) && StringUtils.isNotBlank(regex)) {
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(value);
            return matcher.find() ? value : StringUtils.EMPTY;
        }
        return StringUtils.EMPTY;
    }

    public Map<String, String> getMetadata2regex() {
        return metadata2regex;
    }

    public void setMetadata2regex(Map<String, String> metadata2regex) {
        this.metadata2regex = metadata2regex;
    }

}