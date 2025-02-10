package org.dspace.content.enhancer.impl;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.content.authority.Choices;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class RootItemEnhancer extends RelatedEntityItemEnhancer {
    private static final Logger log = LoggerFactory.getLogger(RootItemEnhancer.class);

    @Autowired
    private ItemService itemService;

    private String rootMetadataField;
    private String sourceItemMetadataField;

    public void setRootMetadataField(String rootMetadataField) {
        this.rootMetadataField = rootMetadataField;
    }

    public void setSourceItemMetadataField(String sourceItemMetadataField) {
        this.sourceItemMetadataField = sourceItemMetadataField;
    }

    @Override
    public boolean enhance(Context context, Item item, boolean deepMode) {
        try {
            if (isRootItem(item)) {
                String source = itemService.getMetadata(item, sourceItemMetadataField);
                addVirtualField(context, item, source, item.getID().toString(), null, Choices.CF_ACCEPTED);
            }
        } catch (SQLException e) {
            log.error("Error enhancing item {}: {}", item.getID(), e.getMessage(), e);
        }
        return false;
    }

    private boolean isRootItem(Item item) {
        String rootMetadataFieldValue = itemService.getMetadata(item, rootMetadataField);
        return rootMetadataFieldValue == null || rootMetadataFieldValue.isEmpty();
    }


}
