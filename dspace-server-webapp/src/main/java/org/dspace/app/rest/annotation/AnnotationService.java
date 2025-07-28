/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import static org.dspace.app.rest.annotation.ItemEnricherFactory.glamContributorEnricher;
import static org.dspace.core.Constants.ADMIN;
import static org.dspace.core.Constants.DELETE;
import static org.dspace.core.Constants.READ;
import static org.dspace.core.Constants.REMOVE;
import static org.dspace.core.Constants.WRITE;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.NotAuthorizedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.annotation.enricher.ItemEnricher;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This service contains the business-logic for {@link AnnotationRest} operations.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class AnnotationService {

    static final String ITEM_PATTERN = "/iiif/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})";
    static final String BITSTREAM_PATTERN = "/canvas/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})";
    static final String ANNOTATION_ID_PATTERN =
        "/annotation/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})";
    static final String ANNOTATION_ENTITY_TYPE = "annotation.default.entity-type";
    static final String DEFAULT_ENTITY_TYPE = "WebAnnotation";
    static final String ANNOTATION_COLLECTION = "annotation.default.collection";
    static final String PERSONAL_ANNOTATION_ENTITY_TYPE = "personal-annotation.default.entity-type";
    static final String PERSONAL_ANNOTATION_GROUP = "personal-annotation.default.group";
    static final String PERSONAL_ANNOTATION_COLLECTION = "personal-annotation.default.collection";
    private static final Logger log = LogManager.getLogger(AnnotationService.class);
    final WorkspaceItemService workspaceItemService;
    final ConfigurationService configurationService;
    final CollectionService collectionService;
    final IdentifierService identifierService;
    final AuthorizeService authorizeService;
    final ItemService itemService;
    final EPersonService ePersonService;
    final InstallItemService installItemService;
    final SearchService searchService;
    final GroupService groupService;
    final ItemEnricher itemEnricher;
    final AnnotationRestMapper annotationRestMapper;

    AnnotationService(
        @Autowired WorkspaceItemService workspaceItemService,
        @Autowired ConfigurationService configurationService,
        @Autowired CollectionService collectionService,
        @Autowired IdentifierService identifierService,
        @Autowired AuthorizeService authorizeService,
        @Autowired ItemService itemService,
        @Autowired InstallItemService installItemService,
        @Autowired EPersonService ePersonService,
        @Autowired SearchService searchService,
        @Autowired GroupService groupService
    ) {
        this.workspaceItemService = workspaceItemService;
        this.configurationService = configurationService;
        this.collectionService = collectionService;
        this.identifierService = identifierService;
        this.authorizeService = authorizeService;
        this.itemService = itemService;
        this.installItemService = installItemService;
        this.ePersonService = ePersonService;
        this.searchService = searchService;
        this.groupService = groupService;
        this.itemEnricher = ItemEnricherFactory.annotationItemEnricher(configurationService);
        this.annotationRestMapper =
            AnnotationRestMapperFactory.annotationRestMapper(configurationService);
    }

    public List<AnnotationRest> search(Context context, String uri) {
        Pattern pattern = Pattern.compile(ITEM_PATTERN);
        Matcher matcher = pattern.matcher(uri);
        List<AnnotationRest> annotations = List.of();
        if (!matcher.find()) {
            return annotations;
        }
        String itemId = matcher.group(1);

        pattern = Pattern.compile(BITSTREAM_PATTERN);
        matcher = pattern.matcher(uri);
        if (!matcher.find()) {
            return annotations;
        }
        String bitstreamId = matcher.group(1);


        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setQuery(
            String.format(
                "search.entitytype:(%s)",
                Stream.of(
                          configurationService.getProperty(ANNOTATION_ENTITY_TYPE, DEFAULT_ENTITY_TYPE),
                          configurationService.getProperty(PERSONAL_ANNOTATION_ENTITY_TYPE)
                      )
                      .filter(StringUtils::isNotBlank)
                      .collect(Collectors.joining(" OR "))
            )
        );
        discoverQuery.addFilterQueries(
            "glam.item_authority:" + itemId,
            "glam.bitstream_authority:" + bitstreamId
        );
        DiscoverResult results = null;
        try {
            results = searchService.search(context, discoverQuery);
        } catch (SearchServiceException e) {
            throw new RuntimeException(e);
        }

        annotations =
            results.getIndexableObjects().stream()
                   .filter(i -> i.getIndexedObject() instanceof Item)
                   .map(i -> annotationRestMapper.map(context, (Item) i.getIndexedObject()))
                   .collect(Collectors.toList());

        return annotations;
    }

    public synchronized void delete(Context context, Item item) {
        EPerson currentUser = context.getCurrentUser();
        if (currentUser == null) {
            throw new NotAuthorizedException("User not logged in");
        }
        if (!canEditAnnotation(context, item)) {
            throw new NotAuthorizedException("User not authorized to delete this annotation");
        }
        try {
            WorkspaceItem wsItem = workspaceItemService.findByItem(context, item);
            if (wsItem != null) {
                try {
                    context.turnOffAuthorisationSystem();
                    workspaceItemService.deleteAll(context, wsItem);
                } catch (IOException e) {
                    throw new RuntimeException(
                        "Error while deleting the workspaceitem associated with id " + item.getID(),
                        e
                    );
                } finally {
                    context.restoreAuthSystemState();
                }
            } else {
                try {
                    context.turnOffAuthorisationSystem();
                    itemService.delete(context, item);
                } catch (IOException e) {
                    throw new RuntimeException(
                        "Error while deleting the item associated with id " + item.getID(),
                        e
                    );
                } finally {
                    context.restoreAuthSystemState();
                }
            }
        } catch (SQLException | AuthorizeException e) {
            log.error("Error while deleting annotation with id: {}", item.getID(), e);
            throw new RuntimeException("Error while deleting annotation with id: " + item.getID(), e);
        }
    }

    public AnnotationRest convert(Context context, Item item) {
        return annotationRestMapper.map(context, item);
    }

    public Item findById(Context context, String annotationId) {

        if (StringUtils.isEmpty(annotationId)) {
            throw new IllegalArgumentException("Empty annotation id: " + annotationId);
        }

        UUID itemUUID =
            Optional.ofNullable(parseAnnotationID(annotationId))
                    .map(this::parseAsUUID)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid annotation id: " + annotationId));

        return findByItemId(context, itemUUID);
    }

    public Item findByItemId(Context context, UUID itemUUID) {
        if (itemUUID == null) {
            log.warn("Cannot find any annotation related to a null item UUID!");
            return null;
        }

        Item annotation = null;
        try {
            annotation = itemService.find(context, itemUUID);
        } catch (SQLException e) {
            log.error("Error while finding item with uuid: {}", itemUUID, e);
        }
        return annotation;
    }

    public Item update(Context context, AnnotationRest annotation) {
        return update(context, annotation, itemEnricher);
    }

    public WorkspaceItem create(Context context, AnnotationRest annotation) {
        return create(context, annotation, itemEnricher);
    }

    private boolean canEditAnnotation(Context context, Item item) {
        try {
            if (authorizeService.isAdmin(context)) {
                return true;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        List<MetadataValue> metadata =
            item.getItemService().getMetadata(item, "glam", "contributor", "annotation", Item.ANY);
        if (metadata.isEmpty()) {
            return false;
        }
        EPerson currentUser = context.getCurrentUser();
        return metadata.stream().anyMatch(m -> currentUser.getID().equals(UUID.fromString(m.getAuthority())));
    }

    private String parseAnnotationID(String annotationId) {
        Matcher matcher = Pattern.compile(ANNOTATION_ID_PATTERN).matcher(annotationId);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid annotation id: " + annotationId);
        }
        return matcher.group(1);
    }

    private UUID parseAsUUID(String annotationId) {
        UUID itemUUID = null;
        try {
            itemUUID = UUID.fromString(annotationId);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot parse annotation id: {} ", annotationId, e);
            }
        }
        return itemUUID;
    }

    protected Item update(Context context, AnnotationRest annotation, ItemEnricher enricher) {
        Item annotationItem = findById(context, annotation.getId());
        if (annotationItem == null) {
            throw new IllegalArgumentException("Annotation not found: " + annotation.getId());
        }
        EPerson currentUser = context.getCurrentUser();
        if (currentUser == null) {
            throw new NotAuthorizedException("User not logged in");
        }
        if (!canEditAnnotation(context, annotationItem)) {
            throw new NotAuthorizedException("User not authorized to update this annotation");
        }
        enrichItem(context, annotationItem, enricher.apply(annotation));
        return annotationItem;
    }

    protected WorkspaceItem create(Context context, AnnotationRest annotation, ItemEnricher enricher) {
        WorkspaceItem workspaceItem;
        try {
            context.turnOffAuthorisationSystem();

            workspaceItem = this.workspaceItemService.create(context, getCollection(context), false);


            Item item = workspaceItem.getItem();
            enrichItem(
                context,
                item,
                enricher.apply(annotation)
                        .andThen(glamContributorEnricher().apply(annotation))
            );


            if (this.isPersonalAnnotationGroup(context)) {
                itemService.addResourcePolicy(context, item, READ, context.getCurrentUser());
                itemService.addResourcePolicy(context, item, WRITE, context.getCurrentUser());
                itemService.addResourcePolicy(context, item, REMOVE, context.getCurrentUser());
                itemService.addResourcePolicy(context, item, DELETE, context.getCurrentUser());
                itemService.addResourcePolicy(context, item, ADMIN, context.getCurrentUser());
            }

            installItemService.installItem(context, workspaceItem);

            workspaceItemService.update(context, workspaceItem);
        } catch (AuthorizeException | SQLException e) {
            log.error("Cannot save the annotation as workspace item", e);
            throw new RuntimeException("Cannot save the annotation as workspace item", e);
        } finally {
            context.restoreAuthSystemState();
        }
        return workspaceItem;
    }

    protected void enrichItem(Context context, Item item, BiConsumer<Context, Item> enrichers) {
        // enrich item with configured enrichers
        enrichers.accept(context, item);
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
                    return null;
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

    protected Group getGroup(Context context) {
        if (context.getCurrentUser() == null) {
            return null;
        }
        Group group = null;
        String groupName = this.configurationService.getProperty(PERSONAL_ANNOTATION_GROUP);
        if (StringUtils.isNotEmpty(groupName)) {
            try {
                boolean member = this.groupService.isMember(context, groupName);
                if (member) {
                    group = this.groupService.findByName(context, groupName);
                }
            } catch (SQLException e) {
                log.error("Error while retrieving the configured group: {}", groupName, e);
            } catch (Exception e) {
                log.debug(
                    "Cannot retrieve the annotation group for the configured identifier {}",
                    groupName,
                    e
                );
            }
        }
        return group;
    }

    protected Collection getCollectionByIdentifier(Context context) {
        Collection collection = null;
        boolean member = false;
        String collectionIdentifier = this.configurationService.getProperty(ANNOTATION_COLLECTION);
        if (context.getCurrentUser() != null) {
            try {
                member = isPersonalAnnotationGroup(context);
                if (member) {
                    collectionIdentifier = this.configurationService.getProperty(PERSONAL_ANNOTATION_COLLECTION);
                }
            } catch (SQLException e) {
                log.error("Error while retrieving the configured group: {}", PERSONAL_ANNOTATION_GROUP, e);
                throw new RuntimeException(e);
            }
        }
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

    private boolean isPersonalAnnotationGroup(Context context) throws SQLException {
        boolean member;
        member = this.groupService.isMember(
            context,
            this.configurationService.getProperty(PERSONAL_ANNOTATION_GROUP)
        );
        return member;
    }

}
