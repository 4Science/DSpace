/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher.metadata;

import java.sql.SQLException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.dspace.app.rest.annotation.AnnotationRest;
import org.dspace.app.rest.annotation.enricher.ItemEnricher;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.authority.Choices;
import org.dspace.core.Context;

/**
 * Extracts the authority and value from the AnnotationRest and applies them to the Item.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MetadataAuthorityItemEnricher implements ItemEnricher {

    protected final BiFunction<Context, AnnotationRest, String> authorityExtractor;
    protected final BiFunction<Context, AnnotationRest, String> valueExtractor;
    protected final MetadataFieldName metadata;

    public MetadataAuthorityItemEnricher(
        MetadataFieldName metadata,
        BiFunction<Context, AnnotationRest, String> authorityExtractor,
        BiFunction<Context, AnnotationRest, String> valueExtractor
    ) {
        this.metadata = metadata;
        this.authorityExtractor = authorityExtractor;
        this.valueExtractor = valueExtractor;
    }


    @Override
    public BiConsumer<Context, Item> apply(AnnotationRest annotationRest) {
        return (context, item) -> {
            String authority = authorityExtractor.apply(context, annotationRest);
            String value = valueExtractor.apply(context, annotationRest);
            if (authority != null && value != null) {
                try {
                    item.getItemService().addMetadata(
                        context,
                        item,
                        metadata.schema,
                        metadata.element,
                        metadata.qualifier,
                        null,
                        value,
                        authority,
                        Choices.CF_ACCEPTED
                    );
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
