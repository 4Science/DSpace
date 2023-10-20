/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.filetype.consumer;

import static org.dspace.util.FunctionalUtils.throwingConsumerWrapper;
import static org.dspace.util.FunctionalUtils.throwingMapperWrapper;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.StringUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTypeMetadataEnhancerConsumer implements Consumer {

    private static final Logger logger = LoggerFactory.getLogger(FileTypeMetadataEnhancerConsumer.class);

    protected static final MetadataFieldName entityTypeMetadata = new MetadataFieldName("dc", "type");
    protected static final MetadataFieldName fileTypeMetadata = new MetadataFieldName("dspace", "file", "type");
    private static final List<MetadataFieldName> itemMetadatas = List.of(fileTypeMetadata);
    private static final List<MetadataFieldName> bitstreamMetadatas = List.of(entityTypeMetadata);
    private static final Map<String, MetadataFieldName> bitstreamToItemMetadatasMap = Map.of(
        entityTypeMetadata.toString(), fileTypeMetadata
    );

    private BitstreamService bitstreamService;
    private ItemService itemService;

    private Set<Bitstream> bitstreamAlreadyProcessed = new HashSet<>();
    private Set<Item> itemsToProcess = new HashSet<>();

    @Override
    public void initialize() throws Exception {
        this.bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
        this.itemService = ContentServiceFactory.getInstance().getItemService();
    }

    @Override
    public void consume(Context ctx, Event event) throws Exception {
        if (Constants.BITSTREAM == event.getSubjectType()) {
            this.handleBitStreamConsumer(
                    ctx,
                    Optional.ofNullable((Bitstream) event.getObject(ctx))
                            .orElse(this.loadBitstream(ctx, event)),
                    event
            );
        } else if (Constants.ITEM == event.getSubjectType() && Event.CREATE == event.getEventType()) {
            this.handleItemConsumer(
                    ctx,
                    Optional.ofNullable((Item) event.getObject(ctx))
                            .orElse(this.loadItem(ctx, event))
            );
        } else {
            logger.warn(
                "Can't consume the DSPaceObject with id {}, only BITSTREAM and ITEMS'CREATION events are consumable!",
                event.getSubjectID()
            );
        }
    }

    @Override
    public void end(Context ctx) throws Exception {
        bitstreamAlreadyProcessed.clear();
        this.itemsToProcess
            .stream()
            .forEach(item -> this.handleItemConsumer(ctx, item));
        itemsToProcess.clear();
    }

    @Override
    public void finish(Context ctx) throws Exception {}

    private Bitstream loadBitstream(Context ctx, Event event) {
        Bitstream found = null;
        try {
            found = this.bitstreamService.find(ctx, event.getSubjectID());
        } catch (SQLException e) {
            logger.error("Error while retrieving the bitstream with ID: " + event.getSubjectID(), e);
            throw new SQLRuntimeException("Error while retrieving the bitstream with ID: " + event.getSubjectID(), e);
        }
        return found;
    }

    private Item loadItem(Context ctx, Event event) {
        Item found = null;
        try {
            found = this.itemService.find(ctx, event.getSubjectID());
        } catch (SQLException e) {
            logger.error("Error while retrieving the bitstream with ID: " + event.getSubjectID(), e);
            throw new SQLRuntimeException("Error while retrieving the bitstream with ID: " + event.getSubjectID(), e);
        }
        return found;
    }

    private void handleBitStreamConsumer(Context ctx, Bitstream bitstream, Event event) {

        if (bitstream == null || this.alreadyProcessed(bitstream)) {
            return;
        }
        List<Item> bitstreamItems = List.of();
        try {
            bitstreamItems = bitstream.getBundles()
                .stream()
                .filter(bundle -> "ORIGINAL".equals(bundle.getName()))
                .map(Bundle::getItems)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            bitstreamAlreadyProcessed.add(bitstream);
            bitstreamItems
                .stream()
                .forEach(item -> this.itemsToProcess.add(item));
        }
    }

    private void handleItemConsumer(Context ctx, Item item) {

        if (item == null) {
            return;
        }

        try {
            Item loadedItem = this.itemService.find(ctx, item.getID());
            Map<MetadataField, List<String>> grouped =
                Optional.ofNullable(loadedItem)
                        .map(i -> i.getBundles("ORIGINAL"))
                        .filter(bundles -> !bundles.isEmpty())
                        .map(bundles -> bundles.get(0))
                        .map(Bundle::getBitstreams)
                        .filter(bitstreams -> !bitstreams.isEmpty())
                        .map(bitstreams -> getMetadatasForItem(ctx, bitstreams).collect(Collectors.toList()))
                        .map(metadatas -> groupByMetadataField(metadatas))
                        .filter(metadatas -> !metadatas.isEmpty())
                        .orElse(Map.of());

            this.itemService.removeMetadataValues(ctx, loadedItem, getRemovableMetadatas(loadedItem));

            grouped
                .entrySet()
                .stream()
                .map(entry ->
                    Map.entry(bitstreamToItemMetadatasMap.get(entry.getKey().toString('.')), entry.getValue())
                )
                .filter(entry -> entry.getKey() != null)
                .forEach(
                    throwingConsumerWrapper(entry ->
                        this.addMetadata(
                            ctx,
                            loadedItem,
                            entry.getKey(),
                            entry.getValue()
                        )
                    )
                );

        } catch (SQLException e) {
            logger.error(MessageFormat.format("Error while processing item {}!", item.getID().toString()), e);
            throw new SQLRuntimeException(e);
        }

    }

    private void addMetadata(Context ctx, Item loadedItem, MetadataFieldName metadata, List<String> value)
            throws SQLException {
        this.itemService.addMetadata(
                ctx,
                loadedItem,
                metadata.schema,
                metadata.element,
                metadata.qualifier,
                null,
                value
        );
    }

    private Stream<MetadataValue> getMetadatasForItem(Context ctx, List<Bitstream> bitstreams) {
        return bitstreams
            .stream()
            .map(
                throwingMapperWrapper(bitstream ->
                    this.bitstreamService.find(ctx, bitstream.getID()),
                    null
                )
            )
            .filter(Objects::nonNull)
            .flatMap(bitstream -> filterBitstreamMetadatasForItem(bitstream));
    }

    private Stream<MetadataValue> filterBitstreamMetadatasForItem(Bitstream bitstream) {
        return bitstream.getMetadata()
            .stream()
            .filter(
                metadataFilter(
                    bitstreamMetadatas
                )
            );
    }

    private Map<MetadataField, List<String>> groupByMetadataField(List<MetadataValue> metadatas) {
        return this.collectByGroupingMetadataFieldMappingValue(metadatas.stream());
    }

    private Map<MetadataField, List<String>> collectByGroupingMetadataFieldMappingValue(Stream<MetadataValue> stream) {
        return stream
                .collect(
                    Collectors.groupingBy(
                        MetadataValue::getMetadataField,
                        Collectors.mapping(MetadataValue::getValue, Collectors.toList())
                    )
                );
    }

    private boolean alreadyProcessed(Bitstream bitstream) {
        return bitstreamAlreadyProcessed.contains(bitstream);
    }

    private List<MetadataValue> getRemovableMetadatas(DSpaceObject dspaceObject) {
        return dspaceObject
            .getMetadata()
            .stream()
            .filter(
                metadataFilter(
                    itemMetadatas
                )
            )
            .collect(Collectors.toList());
    }

    private Predicate<? super MetadataValue> metadataFilter(List<MetadataFieldName> metadataFields) {
        return metadata ->
            metadataFields
                .stream()
                .filter(field ->
                    StringUtils.equals(field.schema, metadata.getSchema()) &&
                    StringUtils.equals(field.element, metadata.getElement()) &&
                    StringUtils.equals(field.qualifier, metadata.getQualifier())
                )
                .findFirst()
                .isPresent();
    }
}
