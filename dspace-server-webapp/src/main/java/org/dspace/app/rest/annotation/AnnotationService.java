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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
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

/**
 * This service contains the business-logic for {@link AnnotationRest} operations.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class AnnotationService {

    private static final Logger log = LogManager.getLogger(AnnotationService.class);
    final WorkspaceItemService workspaceItemService;
    final ConfigurationService configurationService;
    final CollectionService collectionService;
    final IdentifierService identifierService;
    public String ANNOTATION_ENTITY_TYPE = "annotation.default.entity-type";
    public String ANNOTATION_COLLECTION = "annotation.default.collection";

    AnnotationService(
        @Autowired WorkspaceItemService workspaceItemService,
        @Autowired ConfigurationService configurationService,
        @Autowired CollectionService collectionService,
        @Autowired IdentifierService identifierService
    ) {
        this.workspaceItemService = workspaceItemService;
        this.configurationService = configurationService;
        this.collectionService = collectionService;
        this.identifierService = identifierService;
    }


    public WorkspaceItem create(Context context, AnnotationRest annotation) {
        try {
            return this.workspaceItemService.create(context, getCollection(context), false);
        } catch (AuthorizeException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected Collection getCollection(Context context) {
        return Optional.ofNullable(getCollectionByIdentifier(context))
                       .orElseGet(() -> getCollectionByEntityType(context));
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

}
