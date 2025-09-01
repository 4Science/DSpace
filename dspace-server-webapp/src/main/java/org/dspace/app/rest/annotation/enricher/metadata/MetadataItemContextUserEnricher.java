/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher.metadata;

import java.sql.SQLException;
import java.util.List;
import java.util.function.BiConsumer;

import org.dspace.app.rest.annotation.AnnotationRest;
import org.dspace.app.rest.annotation.enricher.ItemEnricher;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choices;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.eperson.EPerson;

/**
 * Item Enricher that extract the user from the context and then stores it to the item using the metadata configured
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MetadataItemContextUserEnricher implements ItemEnricher {

    private final MetadataFieldName metadata;

    public MetadataItemContextUserEnricher(MetadataFieldName metadata) {
        this.metadata = metadata;
    }

    @Override
    public BiConsumer<Context, Item> apply(AnnotationRest annotationRest) {
        return (context, item) -> {
            EPerson currentUser = context.getCurrentUser();
            if (currentUser == null) {
                return;
            }
            ItemService itemService = item.getItemService();
            List<MetadataValue> values =
                itemService.getMetadata(item, metadata.schema, metadata.element, metadata.qualifier, null);
            try {
                if (values.isEmpty()) {
                    MetadataFieldService metadataFieldService =
                        ContentServiceFactory.getInstance().getMetadataFieldService();
                    MetadataField metadataField =
                        metadataFieldService.findByElement(context, metadata.schema, metadata.element,
                                                           metadata.qualifier);
                    itemService.addMetadata(
                        context,
                        item,
                        metadataField,
                        null,
                        currentUser.getFullName(),
                        currentUser.getID().toString(),
                        Choices.CF_ACCEPTED
                    );
                }
            } catch (SQLException e) {
                throw new SQLRuntimeException(e);
            }
        };
    }
}
