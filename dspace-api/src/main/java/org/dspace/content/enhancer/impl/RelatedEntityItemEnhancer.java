/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipConfigurationServiceImpl;
import org.dspace.content.authority.ItemAuthorityExtraMetadataGenerator;
import org.dspace.content.authority.ItemSimpleAuthorityMetadataGenerator;
import org.dspace.content.enhancer.AbstractItemEnhancer;
import org.dspace.content.enhancer.ItemEnhancer;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.RelationshipConfigurationService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ItemEnhancer} that derives additional metadata values from the entities
 * linked to an item through configuration-backed relationships.
 *
 * <p><strong>How it works:</strong></p>
 * <ol>
 *   <li>The enhancer only considers relationship-backed primary values. The relationship of a
 *       primary value identifies both the endpoint of the item and the related entity.</li>
 *   <li>The relationship configuration decides which additional target fields apply to the owner
 *       endpoint. Each target field is mapped, in the configuration, to the
 *       {@link ItemAuthorityExtraMetadataGenerator} that declares the source field on the related
 *       entity; the enhancer copies the related entity metadata of that source field.</li>
 *   <li>Every derived value is stamped with the relationship of the hop that produced it and keeps
 *       the authority of the primary value, i.e. the ultimate ancestor UUID. This makes the derived
 *       value navigable upward without any {@code dspace.virtualsource.*} back-pointer.</li>
 *   <li>Idempotency and cleanup are keyed on the pair (relationship id, target metadata field).
 *       A duplicated reference such as the same person listed as author and editor yields one
 *       derived block per relationship occurrence.</li>
 * </ol>
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 * @see ItemEnhancer
 * @see AbstractItemEnhancer
 * @see org.dspace.content.enhancer.consumer.ItemEnhancerConsumer
 * @see org.dspace.content.dao.ItemForMetadataEnhancementUpdateDAO
 */
public class RelatedEntityItemEnhancer extends AbstractItemEnhancer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedEntityItemEnhancer.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private MetadataValueService metadataValueService;

    @Autowired
    private RelationshipConfigurationService relationshipConfigurationService;

    /**
     * the entities that can be extended by this enhancer, i.e. Publication
     */
    private List<String> sourceEntityTypes;

    @Override
    public boolean canEnhance(Context context, Item item) {
        return sourceEntityTypes == null || sourceEntityTypes.contains(itemService.getEntityTypeLabel(item));
    }

    /**
     * Synchronize the derived metadata values of the given item with the current state of its
     * relationships.
     *
     * <p>In incremental mode only the outdated blocks are rewritten. In deep mode every derived
     * value is rebuilt from scratch, which is what the {@code item-enhancer -f} command and the
     * event cascade use for eventual consistency.</p>
     *
     * @param context  the DSpace context for database operations
     * @param item     the item to enhance
     * @param deepMode if true, rebuilds all derived values from scratch
     * @return true if any derived metadata was added, updated, or removed; false otherwise
     * @throws SQLRuntimeException if database operations fail during enhancement
     */
    @Override
    public boolean enhance(Context context, Item item, boolean deepMode) {
        try {
            List<MetadataValue> primaries = getRelationshipBackedPrimaries(context, item);
            DesiredState desired = getDesiredValues(context, primaries);
            Map<AdditionalKey, List<MetadataValue>> current = getCurrentAdditionals(context, item);
            return reconcile(context, item, current, desired, deepMode);
        } catch (SQLException e) {
            LOGGER.error("An error occurs enhancing item with id {}: {}", item.getID(), e.getMessage(), e);
            throw new SQLRuntimeException(e);
        }
    }

    /**
     * Collect all relationship-backed primary values that are owned by the given item.
     *
     * @param context the DSpace context
     * @param item the item to inspect
     * @return the primary values, in metadata order
     * @throws SQLException if the relationship configuration cannot be resolved
     */
    private List<MetadataValue> getRelationshipBackedPrimaries(Context context, Item item) throws SQLException {
        List<MetadataValue> primaries = new ArrayList<>();
        for (MetadataValue value : item.getMetadata()) {
            Relationship relationship = value.getRelationship();
            if (relationship == null) {
                continue;
            }
            if (isConfigured(relationship)
                && relationshipConfigurationService.isPrimaryMetadata(context, relationship, value)) {
                primaries.add(value);
            }
        }
        return primaries;
    }

    /**
     * Build the desired state of the derived values, keyed by relationship and target field.
     *
     * @param context   the DSpace context
     * @param primaries the relationship-backed primary values of the item
     * @return the desired values per relationship and target field
     * @throws SQLException if the related metadata cannot be read
     */
    private DesiredState getDesiredValues(Context context, List<MetadataValue> primaries)
        throws SQLException {
        Map<AdditionalKey, List<DesiredValue>> desired = new LinkedHashMap<>();
        Map<Integer, Relationship> relationships = new LinkedHashMap<>();
        for (MetadataValue primary : primaries) {
            Relationship relationship = primary.getRelationship();
            if (!isConfigured(relationship)) {
                continue;
            }
            relationships.putIfAbsent(relationship.getID(), relationship);
            Item owner = (Item) primary.getDSpaceObject();
            Set<String> targetFields = new LinkedHashSet<>(
                relationshipConfigurationService.getAdditionalMetadataFields(context, relationship, owner));
            if (targetFields.isEmpty()) {
                continue;
            }
            boolean ownerIsLeft = isLeftEndpoint(relationship, owner);
            Item relatedItem = ownerIsLeft ? relationship.getRightItem() : relationship.getLeftItem();
            for (String targetField : targetFields) {
                List<DesiredValue> values = desired.computeIfAbsent(
                    new AdditionalKey(relationship.getID(), targetField), key -> new ArrayList<>());
                values.addAll(extractValues(context, relationship, owner, relatedItem, targetField,
                    primary.getAuthority()));
            }
        }
        return new DesiredState(desired, relationships);
    }

    /**
     * Extract the values to persist for a single additional target field.
     *
     * <p>The configuration maps the target field to the generator that declares the source field on
     * the related entity. The enhancer copies the related entity metadata of that source field. When
     * no value is available a placeholder is written so that the field stays present and consistent.
     * The authority of the derived value is the authority of the primary value, so it points at the
     * ultimate ancestor of the relationship chain, falling back to the source value authority.</p>
     *
     * @param context       the DSpace context
     * @param relationship  the relationship that produced the derived values
     * @param endpoint      the owner endpoint item of the relationship
     * @param relatedItem   the related entity, possibly null
     * @param targetField   the fully qualified target metadata field
     * @param authority     the authority of the primary value
     * @return the values to persist for the target field
     * @throws SQLException if the generator cannot be resolved
     */
    private List<DesiredValue> extractValues(Context context, Relationship relationship, Item endpoint,
                                             Item relatedItem, String targetField, String authority)
        throws SQLException {
        String sourceField = getSourceField(context, relationship, endpoint, targetField);
        if (relatedItem == null || sourceField == null) {
            return List.of();
        }
        List<MetadataValue> sourceValues = itemService.getMetadataByMetadataString(relatedItem, sourceField);
        if (sourceValues.isEmpty()) {
            return List.of();
        }
        List<DesiredValue> values = new ArrayList<>(sourceValues.size());
        for (MetadataValue sourceValue : sourceValues) {
            String effectiveAuthority = StringUtils.isNotBlank(authority) ? authority : sourceValue.getAuthority();
            values.add(new DesiredValue(sourceValue.getValue(), sourceValue.getLanguage(), effectiveAuthority,
                sourceValue.getConfidence()));
        }
        return values;
    }

    /**
     * Resolve the source metadata field on the related entity for an additional target field.
     *
     * <p>The source field is declared by the {@link ItemAuthorityExtraMetadataGenerator} mapped to
     * the target field in the configuration. Only {@link ItemSimpleAuthorityMetadataGenerator}
     * exposes the source field; other generators cannot be read at runtime and yield no source.</p>
     *
     * @param context       the DSpace context
     * @param relationship  the relationship that produced the derived values
     * @param endpoint      the owner endpoint item of the relationship
     * @param targetField   the fully qualified target metadata field
     * @return the source metadata field, or {@code null} if it cannot be resolved
     * @throws SQLException if the generator cannot be resolved
     */
    private String getSourceField(Context context, Relationship relationship, Item endpoint,
                                  String targetField) throws SQLException {
        Optional<ItemAuthorityExtraMetadataGenerator> generator =
            relationshipConfigurationService.getMetadataGenerator(context, relationship, endpoint, targetField);
        if (generator.isEmpty() || !(generator.get() instanceof ItemSimpleAuthorityMetadataGenerator simple)) {
            return null;
        }
        String qualifier = simple.getQualifier();
        return simple.getSchema() + "." + simple.getElement()
            + (StringUtils.isBlank(qualifier) ? "" : "." + qualifier);
    }

    /**
     * Collect the derived values currently stored on the item, keyed by relationship and target field.
     *
     * <p>A derived value is a relationship-backed value that is not the primary value of its
     * relationship. Values that cannot be classified are ignored: this enhancer only owns values it
     * can attribute to a configured relationship.</p>
     *
     * @param context the DSpace context
     * @param item the enhanced item
     * @return the current derived values
     * @throws SQLException if the relationship configuration cannot be resolved
     */
    private Map<AdditionalKey, List<MetadataValue>> getCurrentAdditionals(Context context, Item item)
        throws SQLException {
        Map<AdditionalKey, List<MetadataValue>> additionals = new LinkedHashMap<>();
        for (MetadataValue value : new ArrayList<>(item.getMetadata())) {
            Relationship relationship = value.getRelationship();
            if (relationship == null || !isConfigured(relationship)
                || relationshipConfigurationService.isPrimaryMetadata(context, relationship, value)) {
                continue;
            }
            Item owner = (Item) value.getDSpaceObject();
            String targetField = fieldName(value);
            if (!relationshipConfigurationService.getAdditionalMetadataFields(context, relationship, owner)
                .contains(targetField)) {
                continue;
            }
            additionals.computeIfAbsent(new AdditionalKey(relationship.getID(), targetField),
                key -> new ArrayList<>()).add(value);
        }
        return additionals;
    }

    /**
     * Reconcile the current derived values with the desired state.
     *
     * @param context   the DSpace context
     * @param item      the enhanced item
     * @param current   the current derived values
     * @param desired   the desired derived values
     * @param deepMode  whether the reconciliation is a full rebuild
     * @return true if anything changed
     * @throws SQLException if a value cannot be added or removed
     */
    private boolean reconcile(Context context, Item item, Map<AdditionalKey, List<MetadataValue>> current,
                              DesiredState desired, boolean deepMode) throws SQLException {
        boolean changed = false;
        if (deepMode) {
            changed = deleteAdditionals(context, current.values().stream().flatMap(List::stream).toList());
            for (Map.Entry<AdditionalKey, List<DesiredValue>> entry : desired.values().entrySet()) {
                Relationship relationship = desired.relationships().get(entry.getKey().relationshipId());
                for (DesiredValue value : entry.getValue()) {
                    addAdditional(context, item, relationship, entry.getKey().targetField(), value);
                    changed = true;
                }
            }
            return changed;
        }
        for (Map.Entry<AdditionalKey, List<DesiredValue>> entry : desired.values().entrySet()) {
            List<MetadataValue> currentValues = current.getOrDefault(entry.getKey(), List.of());
            if (!equivalent(currentValues, entry.getValue())) {
                changed = deleteAdditionals(context, currentValues) || changed;
                Relationship relationship = desired.relationships().get(entry.getKey().relationshipId());
                for (DesiredValue value : entry.getValue()) {
                    addAdditional(context, item, relationship, entry.getKey().targetField(), value);
                }
                changed = true;
            }
        }
        for (Map.Entry<AdditionalKey, List<MetadataValue>> entry : current.entrySet()) {
            if (!desired.values().containsKey(entry.getKey())) {
                changed = deleteAdditionals(context, entry.getValue()) || changed;
            }
        }
        return changed;
    }

    /**
     * Check whether the persisted values already match the desired values.
     *
     * @param current the persisted values
     * @param desired the desired values
     * @return true when no write is needed
     */
    private boolean equivalent(List<MetadataValue> current, List<DesiredValue> desired) {
        if (current.size() != desired.size()) {
            return false;
        }
        for (int i = 0; i < current.size(); i++) {
            MetadataValue value = current.get(i);
            DesiredValue expected = desired.get(i);
            if (!StringUtils.equals(value.getValue(), expected.value())
                || !StringUtils.equals(value.getLanguage(), expected.language())
                || !StringUtils.equals(value.getAuthority(), expected.authority())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Add a single derived value and stamp it with its relationship.
     *
     * <p>The relationship and authority are set through the metadata value service because virtual
     * fields are not authority controlled and would otherwise silently drop the authority.</p>
     *
     * @param context      the DSpace context
     * @param item         the enhanced item
     * @param relationship the relationship that produced the value, may be null
     * @param targetField  the fully qualified target metadata field
     * @param value        the value to persist
     * @throws SQLException if the value cannot be persisted
     */
    private void addAdditional(Context context, Item item, Relationship relationship, String targetField,
                               DesiredValue value) throws SQLException {
        String[] parts = targetField.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid additional metadata field: " + targetField);
        }
        String qualifier = parts.length > 2 ? parts[2] : null;
        MetadataValue added = itemService.addMetadata(context, item, parts[0], parts[1], qualifier, value.language(),
            value.value(), null, value.confidence());
        if (added == null) {
            return;
        }
        if (relationship != null) {
            added.setRelationship(relationship);
        }
        if (StringUtils.isNotBlank(value.authority())) {
            added.setAuthority(value.authority());
        }
        metadataValueService.update(context, added);
    }

    /**
     * Delete derived values without going through the relationship-aware removal logic: these
     * values are not primary values, so removing them must never tear the relationship down.
     *
     * @param context the DSpace context
     * @param values  the values to delete
     * @return true when at least one value was deleted
     * @throws SQLException if a value cannot be deleted
     */
    private boolean deleteAdditionals(Context context, List<MetadataValue> values) throws SQLException {
        boolean changed = false;
        for (MetadataValue value : values) {
            value.getDSpaceObject().getMetadata().remove(value);
            metadataValueService.delete(context, value);
            changed = true;
        }
        return changed;
    }

    /**
     * Whether the relationship is backed by a resolvable configuration. Classification and
     * field-resolution are delegated to the configuration service, which resolves the configuration
     * from the relationship internally; this guard only screens out relationships that carry no
     * configuration key at all.
     *
     * @param relationship the relationship to inspect
     * @return true when the relationship carries a configuration key
     */
    private boolean isConfigured(Relationship relationship) {
        return StringUtils.isNotBlank(relationship.getRelationshipConfigKey());
    }

    private boolean isLeftEndpoint(Relationship relationship, Item item) {
        return relationship.getLeftItem() != null && relationship.getLeftItem().getID().equals(item.getID());
    }

    private String fieldName(MetadataValue value) {
        return RelationshipConfigurationServiceImpl.fieldName(value);
    }

    public void setSourceEntityTypes(List<String> sourceEntityTypes) {
        this.sourceEntityTypes = sourceEntityTypes;
    }

    /**
     * Identity of a derived value: the relationship of the hop plus the target metadata field.
     */
    private record AdditionalKey(Integer relationshipId, String targetField) {
    }

    /**
     * The desired derived values together with the relationships that produce them.
     *
     * @param values        the desired values per relationship and target field
     * @param relationships the relationships indexed by id
     */
    private record DesiredState(Map<AdditionalKey, List<DesiredValue>> values,
                                Map<Integer, Relationship> relationships) {
    }

    /**
     * A value that should be persisted for a derived field.
     */
    private record DesiredValue(String value, String language, String authority, int confidence) {
        private DesiredValue {
            Objects.requireNonNull(value);
        }
    }

}
