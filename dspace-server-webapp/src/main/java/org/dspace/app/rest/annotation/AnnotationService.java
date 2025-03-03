/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * This service contains the business-logic for {@link AnnotationRest} operations.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class AnnotationService {

    private static final Logger log = LogManager.getLogger(AnnotationService.class);
    static final String ANNOTATION_ENTITY_TYPE = "annotation.default.entity-type";
    static final String ANNOTATION_COLLECTION = "annotation.default.collection";

    final WorkspaceItemService workspaceItemService;
    final ConfigurationService configurationService;
    final CollectionService collectionService;
    final IdentifierService identifierService;
    final AuthorizeService authorizeService;

    AnnotationService(
        @Autowired WorkspaceItemService workspaceItemService,
        @Autowired ConfigurationService configurationService,
        @Autowired CollectionService collectionService,
        @Autowired IdentifierService identifierService,
        @Autowired AuthorizeService authorizeService
    ) {
        this.workspaceItemService = workspaceItemService;
        this.configurationService = configurationService;
        this.collectionService = collectionService;
        this.identifierService = identifierService;
        this.authorizeService = authorizeService;
    }


    public WorkspaceItem create(Context context, AnnotationRest annotation) {
        WorkspaceItem workspaceItem = null;
        try {
            context.turnOffAuthorisationSystem();

            workspaceItem = this.workspaceItemService.create(context, getCollection(context), false);

        } catch (AuthorizeException | SQLException e) {
            throw new RuntimeException(e);
        } finally {
            context.restoreAuthSystemState();
        }
        return workspaceItem;
    }

    protected Collection getCollection(Context context) {
        return Optional.ofNullable(getCollectionByIdentifier(context))
                       .or(() -> Optional.ofNullable(getCollectionByEntityType(context)))
                       .orElseThrow(
                           () -> new IllegalArgumentException("Cannot find any configured Annotation Collection")
                       );
    }

    protected Collection getCollectionByEntityType(Context context) {
        Collection collection = null;
        String entityType = this.configurationService.getProperty(ANNOTATION_ENTITY_TYPE);
        if (StringUtils.isNotEmpty(entityType)) {
            List<Collection> allCollectionsByEntityType;
            try {
                allCollectionsByEntityType = this.collectionService.findAllCollectionsByEntityType(context, entityType);
                if (allCollectionsByEntityType.isEmpty()) {
                    log.error(
                        "No collection found for entity type {}",
                        entityType
                    );
                }
                collection = allCollectionsByEntityType.get(0);
                if (allCollectionsByEntityType.size() > 1) {
                    log.warn(
                        "More than one collection found for entity type {}, using the first one: {}!",
                        entityType,
                        allCollectionsByEntityType.get(0)
                    );
                }
            } catch (SearchServiceException e) {
                log.error("Error while retrieving the configured collection: {}", entityType, e);
            }
        }
        return collection;
    }

    protected Collection getCollectionByIdentifier(Context context) {
        Collection collection = null;
        String collectionIdentifier = this.configurationService.getProperty(ANNOTATION_COLLECTION);
        if (StringUtils.isNotEmpty(collectionIdentifier)) {
            try {
                collection = this.collectionService.find(context, UUID.fromString(collectionIdentifier));
            } catch (SQLException e) {
                log.error("Error while retrieving the configured collection: {}", collectionIdentifier, e);
            } catch (Exception e) {
                log.debug(
                    "Cannot retrieve the annotation collection for the configured identifier {}",
                    collectionIdentifier,
                    e
                );
            }
            if (collection == null) {
                try {
                    DSpaceObject resolve = this.identifierService.resolve(context, collectionIdentifier);
                    if (Constants.COLLECTION != resolve.getType()) {
                        throw new IdentifierNotResolvableException(
                            "The configured identifier '" + collectionIdentifier + "' is not a collection"
                        );
                    }
                    collection = (Collection) resolve;
                } catch (IdentifierNotFoundException | IdentifierNotResolvableException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return collection;
    }

    @FunctionalInterface
    public interface ItemEnricher extends Function<AnnotationRest, BiConsumer<Context, Item>> {
    }

    @FunctionalInterface
    public interface AnnotationEnricher extends Function<Item, BiConsumer<Context, AnnotationRest>> {
    }

    public static abstract class AbstractMetadataSpelMapper {

        String spel;
        MetadataFieldName metadata;
        Class<?> clazz;

        Expression fieldExpression;
        SpelExpressionParser expressionParser;

        public AbstractMetadataSpelMapper(String spel, MetadataFieldName metadata, Class<?> clazz) {
            this.spel = spel;
            this.metadata = metadata;
            this.clazz = clazz;
            expressionParser = new SpelExpressionParser();
            fieldExpression = expressionParser.parseExpression(spel);
        }
    }

    public static class MetadataItemEnricher extends AbstractMetadataSpelMapper implements ItemEnricher {

        public MetadataItemEnricher(String spel, MetadataFieldName metadata, Class<?> clazz) {
            super(spel, metadata, clazz);
        }

        @Override
        public BiConsumer<Context, Item> apply(AnnotationRest annotationRest) {
            Object value = fieldExpression.getValue(annotationRest, clazz);
            if (value == null) {
                return empty();
            }

            if (value instanceof java.util.Collection) {
                return ((java.util.Collection<?>) value).stream()
                                                        .map(element -> addMetadata(element.toString()))
                                                        .reduce(BiConsumer::andThen)
                                                        .orElse(empty());
            }
            return addMetadata(value.toString());
        }

        private BiConsumer<Context, Item> empty() {
            return (context, item) -> { };
        }

        protected BiConsumer<Context, Item> addMetadata(String value) {
            return (context, item) -> {
                try {
                    item.getItemService()
                        .addMetadata(
                            context,
                            item,
                            metadata.schema,
                            metadata.element,
                            metadata.qualifier,
                            null,
                            value
                        );
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }

    public static class GenericItemMetadataEnricher<T> extends AbstractMetadataSpelMapper
        implements Function<Item, BiConsumer<Context, T>> {

        public GenericItemMetadataEnricher(String spel, MetadataFieldName metadata, Class<?> clazz) {
            super(spel, metadata, clazz);
        }

        @Override
        public BiConsumer<Context, T> apply(Item item) {
            return (context, bodyRest) ->
                fieldExpression.setValue(bodyRest, item.getItemService().getMetadata(item, metadata.toString()));
        }
    }

    public static class AnnotationBodyRestEnricher extends GenericItemMetadataEnricher<AnnotationBodyRest> {

        public AnnotationBodyRestEnricher(String spel, MetadataFieldName metadata, Class<?> clazz) {
            super(spel, metadata, clazz);
        }
    }

    public static class AnnotationTargetRestEnricher extends GenericItemMetadataEnricher<AnnotationTargetRest> {

        public AnnotationTargetRestEnricher(String spel, MetadataFieldName metadata, Class<?> clazz) {
            super(spel, metadata, clazz);
        }
    }


    public static class AnnotationItemSpelEnricher implements AnnotationEnricher {

        String itemFieldSpel;
        String annotationFieldSpel;
        Expression annotationField;
        Expression itemField;

        public AnnotationItemSpelEnricher(String itemFieldSpel, String annotationFieldSpel) {
            this.itemFieldSpel = itemFieldSpel;
            this.annotationFieldSpel = annotationFieldSpel;
            annotationField = new SpelExpressionParser().parseExpression(annotationFieldSpel);
            itemField = new SpelExpressionParser().parseExpression(itemFieldSpel);
        }

        @Override
        public BiConsumer<Context, AnnotationRest> apply(Item item) {
            return (context, annotation) -> annotationField.setValue(annotation, itemField.getValue(item));
        }
    }

    public static class AnnotationRestMapper {

        List<AnnotationBodyRestEnricher> bodyRestEnricher;
        List<AnnotationTargetRestEnricher> targetEnricher;
        List<AnnotationEnricher> annotationEnrichers;

        public AnnotationRestMapper(
            List<AnnotationBodyRestEnricher> bodyRestEnricher,
            List<AnnotationTargetRestEnricher> targetEnricher,
            List<AnnotationEnricher> annotationEnrichers
        ) {
            this.bodyRestEnricher = bodyRestEnricher;
            this.targetEnricher = targetEnricher;
            this.annotationEnrichers = annotationEnrichers;
        }

        public AnnotationRest map(Context context, Item item) {

            AnnotationRest annotationRest = new AnnotationRest();

            BiConsumer<Context, AnnotationBodyRest> bodyRestConsumer =
                bodyRestEnricher.stream()
                                .map(enricher -> enricher.apply(item))
                                .reduce(BiConsumer::andThen)
                                .orElse(empty());
            BiConsumer<Context, AnnotationTargetRest> targetRestConsumer =
                targetEnricher.stream()
                              .map(enricher -> enricher.apply(item))
                              .reduce(BiConsumer::andThen)
                              .orElse(empty());
            BiConsumer<Context, AnnotationRest> annotationRestConsumer =
                annotationEnrichers.stream()
                                   .map(enricher -> enricher.apply(item))
                                   .reduce(BiConsumer::andThen)
                                   .orElse(empty());

            bodyRestConsumer.accept(context, annotationRest.resource.get(0));
            targetRestConsumer.accept(context, annotationRest.on.get(0));
            annotationRestConsumer.accept(context, annotationRest);

            return annotationRest;
        }

        private <T> BiConsumer<Context, T> empty() {
            return (context, item) -> { };
        }
    }

}
