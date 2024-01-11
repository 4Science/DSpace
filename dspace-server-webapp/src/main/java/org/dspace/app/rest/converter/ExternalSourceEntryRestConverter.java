/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dspace.app.rest.model.ExternalSourceEntryRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This converter deals with the conversion between ExternalDataObjects and ExternalSourceEntryRest objects
 */
@Component
public class ExternalSourceEntryRestConverter implements DSpaceConverter<ExternalDataObject, ExternalSourceEntryRest> {

    @Autowired
    private MetadataValueDTOListConverter metadataConverter;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemConverter itemConverter;

    public ExternalSourceEntryRest convert(ExternalDataObject modelObject, Projection projection) {
        ExternalSourceEntryRest externalSourceEntryRest = new ExternalSourceEntryRest();
        externalSourceEntryRest.setId(modelObject.getId());
        externalSourceEntryRest.setExternalSource(modelObject.getSource());
        externalSourceEntryRest.setDisplay(modelObject.getDisplayValue());
        externalSourceEntryRest.setValue(modelObject.getValue());
        externalSourceEntryRest.setExternalSource(modelObject.getSource());
        externalSourceEntryRest.setMetadata(metadataConverter.convert(modelObject.getMetadata()));
        externalSourceEntryRest.setMatchObjects(convertToItemRests(modelObject.getMatchUUIDs(), projection));
        return externalSourceEntryRest;
    }

    private List<ItemRest> convertToItemRests(List<UUID> uuids, Projection projection) {

        if (uuids == null) {
            return List.of();
        }

        Context context = ContextUtil.obtainCurrentRequestContext();
        return uuids.stream()
                    .map(uuid -> {
                        try {
                            return itemService.find(context, uuid);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .filter(item -> Objects.nonNull(item))
                    .map(item -> itemConverter.convert(item, projection))
                    .collect(Collectors.toList());
    }

    public Class<ExternalDataObject> getModelClass() {
        return ExternalDataObject.class;
    }
}
