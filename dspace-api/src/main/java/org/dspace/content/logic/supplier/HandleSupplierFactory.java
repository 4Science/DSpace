/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.supplier;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.factory.WorkflowServiceFactory;

/**
 * This Factory stores all the {@link HandleSupplier} implemented
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class HandleSupplierFactory {

    private static final HandleSupplierFactory instance = new HandleSupplierFactory();

    private HandleSupplierFactory() {
    }

    public static HandleSupplierFactory getInstance() {
        return instance;
    }

    public HandleSupplier collectionHandleSupplier() {
        return (context, item) ->
            Optional.of(getCollections(item)
                            .map(Collection::getHandle)
                            .collect(Collectors.toSet())
            );
    }

    public HandleSupplier collectionWorkspaceHandleSupplier() {
        return (context, item) ->
                getWorkspaceItem(context, item)
                    .map(WorkspaceItem::getCollection)
                    .map(Collection::getHandle)
                    .map(handle -> Set.of(handle));
    }

    public HandleSupplier collectionWorkflowHandleSupplier() {
        return (context, item) ->
                    getWorkflowItem(context, item)
                        .map(WorkflowItem::getCollection)
                        .map(Collection::getHandle)
                        .map(handle -> Set.of(handle));
    }

    public HandleSupplier relatedCollectionHandleSupplier() {
        return (context, item) ->
            Optional.of(
                getRelatedCollections(context, item)
                    .map(Collection::getHandle)
                    .collect(Collectors.toSet())
            );
    }

    public HandleSupplier communityHandleSupplier() {
        return (context, item) ->
            Optional.of(
                getCollections(item)
                    .flatMap(HandleSupplierFactory::getCommunities)
                    .map(Community::getHandle)
                    .collect(Collectors.toSet())
            );
    }

    public HandleSupplier relatedCommunityHandleSupplier() {
        return (context, item) ->
            Optional.of(
                getRelatedCollections(context, item)
                    .flatMap(HandleSupplierFactory::getCommunities)
                    .map(Community::getHandle)
                    .collect(Collectors.toSet())
            );
    }

    private Stream<Collection> getRelatedCollections(Context context, Item item) {
        return Stream.concat(
            item.getCollections().stream(),
            Stream.of(
                getWorkflowItem(context, item)
                        .map(WorkflowItem::getCollection)
                        .orElse(null),
                getWorkspaceItem(context, item)
                        .map(WorkspaceItem::getCollection)
                        .orElse(null)
            ).filter(Objects::nonNull)
        );
    }

    private Optional<WorkspaceItem> getWorkspaceItem(Context context, Item item) {
        try {
            return Optional.ofNullable(
                ContentServiceFactory.getInstance().getWorkspaceItemService().findByItem(context, item));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<WorkflowItem> getWorkflowItem(Context context, Item item) {
        try {
            return Optional.ofNullable(
                WorkflowServiceFactory.getInstance().getWorkflowItemService().findByItem(context, item));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Community> getCommunities(Collection collection) {
        try {
            return collection.getCommunities().stream();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Collection> getCollections(Item item) {
        return item.getCollections().stream();
    }

}
