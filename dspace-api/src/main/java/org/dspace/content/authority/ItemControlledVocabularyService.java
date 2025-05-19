/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static org.dspace.content.authority.DSpaceControlledVocabulary.ID_SPLITTER;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.factory.ItemAuthorityServiceFactory;
import org.dspace.content.authority.factory.ItemControlledVocabularyFactory;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.dspace.web.ContextUtil;

/*
 * @author Jurgen Mamani
 */
public class ItemControlledVocabularyService extends SelfNamedPlugin
        implements HierarchicalAuthority {

    private static final Logger log = LogManager.getLogger(ItemControlledVocabularyService.class);

    protected static String[] pluginNames = null;

    private static final String CONFIG_PREFIX = "item.controlled.vocabularies";

    private final ItemControlledVocabularyFactory itemControlledVocabularyFactory =
        new DSpace().getServiceManager().getServiceByName(
            "itemControlledVocabularyFactory", ItemControlledVocabularyFactory.class);

    private final SearchService searchService = SearchUtils.getSearchService();

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private static Map<UUID, Boolean> ITEM_CHILDREN_CACHE = new HashMap<>();

    private ItemAuthorityServiceFactory itemAuthorityServiceFactory = new DSpace().getServiceManager()
            .getServiceByName("itemAuthorityServiceFactory", ItemAuthorityServiceFactory.class);

    private static final boolean ENABLED_CACHE = DSpaceServicesFactory.getInstance().getConfigurationService()
        .getBooleanProperty(CONFIG_PREFIX + ".enable.cache");

    public ItemControlledVocabularyService() {
        super();
    }

    public static String[] getPluginNames() {
        if (pluginNames == null) {
            initPluginNames();
        }

        return ArrayUtils.clone(pluginNames);
    }

    private static synchronized void initPluginNames() {
        if (pluginNames == null) {
            pluginNames = DSpaceServicesFactory.getInstance().getConfigurationService()
                    .getArrayProperty(CONFIG_PREFIX);
            log.info("Got plugin names = " + Arrays.deepToString(pluginNames));
        }
    }

    @Override
    public Choices getTopChoices(String authorityName, int start, int limit, String locale) {
        ItemControlledVocabulary controlledVocabulary =
            itemControlledVocabularyFactory.getInstance(authorityName);

        DiscoverQuery discoverQuery = new DiscoverQuery();

        discoverQuery.setStart(start);
        discoverQuery.setMaxResults(limit);
        discoverQuery.setQuery(controlledVocabulary.getParentQuery());

        if (controlledVocabulary.getSortFields() != null && !controlledVocabulary.getSortFields().isEmpty()) {
            for (DiscoverySortFieldConfiguration sortConfig : controlledVocabulary.getSortFields()) {

                discoverQuery.addSortField(
                    searchService.toSortFieldIndex(sortConfig.getMetadataField(), sortConfig.getType()),
                    DiscoverySortFieldConfiguration.SORT_ORDER.asc.equals(sortConfig.getDefaultSortOrder().asc) ?
                        DiscoverQuery.SORT_ORDER.asc : DiscoverQuery.SORT_ORDER.desc
                );
            }
        }

        try {
            DiscoverResult result = searchService.search(ContextUtil.obtainCurrentRequestContext(), discoverQuery);

            if (!result.getIndexableObjects().isEmpty()) {
                int total = (int) result.getTotalSearchResults();

                List<Choice> choices = getChoicesFromResult(authorityName, controlledVocabulary, result);

                return new Choices(choices.toArray(new Choice[choices.size()]), start, total, Choices.CF_AMBIGUOUS,
                                   total > start + limit);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return new Choices(true);
        }

        return new Choices(false);
    }

    @Override
    public Choices getChoicesByParent(String authorityName, String parentId, int start, int limit, String locale) {
        ItemControlledVocabulary controlledVocabulary =
            itemControlledVocabularyFactory.getInstance(authorityName);

        // FIXME: we must be sure that parentId is a pure uuid
        if (StringUtils.startsWith(parentId, authorityName + ID_SPLITTER)) {
            parentId = parentId.replace(authorityName + ID_SPLITTER, "");
        }
        DiscoverQuery discoverQuery = new DiscoverQuery();

        discoverQuery.setStart(start);
        discoverQuery.setMaxResults(limit);

        if (controlledVocabulary.getSortFields() != null && !controlledVocabulary.getSortFields().isEmpty()) {
            for (DiscoverySortFieldConfiguration sortConfig : controlledVocabulary.getSortFields()) {

                discoverQuery.addSortField(searchService.toSortFieldIndex(
                                               sortConfig.getMetadataField(), sortConfig.getType()),
                                           DiscoverySortFieldConfiguration.SORT_ORDER.asc.equals(
                                               sortConfig.getDefaultSortOrder().asc) ?
                                               DiscoverQuery.SORT_ORDER.asc : DiscoverQuery.SORT_ORDER.desc);
            }
        }

        String childrenQuery = MessageFormat.format(controlledVocabulary.getChildrenQuery(), parentId);
        discoverQuery.setQuery(childrenQuery);

        try {
            DiscoverResult result = searchService.search(ContextUtil.obtainCurrentRequestContext(), discoverQuery);

            if (!result.getIndexableObjects().isEmpty()) {
                int total = (int) result.getTotalSearchResults();

                List<Choice> choices = getChoicesFromResult(authorityName, controlledVocabulary, result);

                return new Choices(choices.toArray(new Choice[choices.size()]), start, total, Choices.CF_AMBIGUOUS,
                                   total > start + limit);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return new Choices(true);
        }

        return new Choices(false);
    }

    @Override
    public Choice getParentChoice(String authorityName, String vocabularyId, String locale) {
        ItemControlledVocabulary controlledVocabulary =
            itemControlledVocabularyFactory.getInstance(authorityName);

        // FIXME: hack to prevent vocabularyId name composed with authority name
        if (StringUtils.startsWith(vocabularyId, authorityName + ID_SPLITTER)) {
            vocabularyId = vocabularyId.replace(authorityName + ID_SPLITTER, "");
        }
        try {
            Item self = itemService.find(ContextUtil.obtainCurrentRequestContext(),
                                           UUID.fromString(vocabularyId));
            MetadataValue parentMtd = itemService
                    .getMetadataByMetadataString(self, controlledVocabulary.getParentMetadata())
                    .stream().findFirst().orElse(null);

            if (parentMtd != null) {
                Item parentItem = itemService.find(ContextUtil.obtainCurrentRequestContext(),
                                 UUID.fromString(parentMtd.getAuthority()));
                if (parentItem != null) {
                    return getChoiceFromItem(authorityName, controlledVocabulary, parentItem);
                }
            }
        } catch (SQLException e) {
            log.warn(e.getMessage(), e);
        }

        return null;
    }

    private List<Choice> getChoicesFromResult(ItemControlledVocabulary controlledVocabulary,
                                              DiscoverResult result) {
        return getChoicesFromResult(null, controlledVocabulary, result);
    }

    private List<Choice> getChoicesFromResult(String authorityName, ItemControlledVocabulary controlledVocabulary,
                                              DiscoverResult result) {
        return result.getIndexableObjects().stream()
            .filter(i -> i.getIndexedObject() instanceof Item)
            .map(i -> {
                Item item = (Item) i.getIndexedObject();
                return getChoiceFromItem(authorityName, controlledVocabulary, item);
            }).collect(Collectors.toList());
    }

    private Choice getChoiceFromItem(String authorityName, ItemControlledVocabulary controlledVocabulary,
                                     Item item) {
        Choice choice = new Choice();

        String labelMeta = getValueFromMetadataList(item, controlledVocabulary.getLabelMetadata());
        String storedMeta = labelMeta;
        if (controlledVocabulary.getStoredMetadata() != null) {
            storedMeta = getValueFromMetadataList(item, controlledVocabulary.getStoredMetadata());
        }
        choice.value = storedMeta;
        choice.label = labelMeta;
        choice.extras = controlledVocabulary.getExtraValuesMapper().buildExtraValues(item);

        String selectableValue = getValueFromMetadata(item, controlledVocabulary.getSelectableMetadata());
        choice.selectable = selectableValue.isEmpty() || Boolean.parseBoolean(selectableValue);

        String authority = getValueFromMetadata(item, controlledVocabulary.getAuthorityMetadata());
        choice.authority = authority.isEmpty() ? String.valueOf(item.getID()) : authority;
        if (StringUtils.isNotBlank(authorityName)) {
            choice.authorityName = authorityName;
        }

        try {
            MetadataValue parentMtd = itemService
                    .getMetadataByMetadataString(item, controlledVocabulary.getParentMetadata())
                    .stream().findFirst().orElse(null);
            if (parentMtd != null) {
                Item parentItem = itemService.find(ContextUtil.obtainCurrentRequestContext(),
                                 UUID.fromString(parentMtd.getAuthority()));
                if (parentItem != null) {
                    choice.extras.put("parent", parentItem.getID().toString());
                }
            }
        } catch (SQLException e) {
            log.warn(e.getMessage(), e);
        }

        choice.extras.put("hasChildren", String.valueOf(hasChildren(item)));
        choice.extras.put("id", choice.authority);
        return choice;
    }

    private String getValueFromMetadata(Item item, String metadata) {
        String mtd = itemService.getMetadata(item, metadata);
        return mtd == null ? "" : mtd;
    }

    private String getValueFromMetadataList(Item item, List<String> metadataList) {
        List<String> value = new ArrayList<>();
        for (String mtd: metadataList) {
            String mtdValue = getValueFromMetadata(item, mtd);
            if (!mtdValue.isEmpty()) {
                value.add(mtdValue);
            }
        }
        return value.isEmpty() ? "" : String.join(" - ", value);
    }

    private boolean hasChildren(Item item) {
        // Check cache first
        if (ENABLED_CACHE && ITEM_CHILDREN_CACHE.containsKey(item.getID())) {
            return ITEM_CHILDREN_CACHE.get(item.getID());
        }

        ItemControlledVocabulary controlledVocabulary =
            itemControlledVocabularyFactory.getInstance(this.getPluginInstanceName());

        DiscoverQuery discoverQuery = new DiscoverQuery();

        discoverQuery.setStart(0);
        discoverQuery.setMaxResults(1);

        String childrenQuery = MessageFormat.format(controlledVocabulary.getChildrenQuery(), item.getID());
        discoverQuery.setQuery(childrenQuery);

        try {
            DiscoverResult result = searchService.search(ContextUtil.obtainCurrentRequestContext(), discoverQuery);
            ITEM_CHILDREN_CACHE.put(item.getID(), !result.getIndexableObjects().isEmpty());
            return !result.getIndexableObjects().isEmpty();
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public Integer getPreloadLevel() {
        return 1;
    }

    @Override
    public boolean isHierarchical() {
        return HierarchicalAuthority.super.isHierarchical();
    }

    @Override
    public boolean isPublic() {
        return true;
    }

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        ItemControlledVocabulary itemControlledVocabulary =
                itemControlledVocabularyFactory.getInstance(this.getPluginInstanceName());

        DiscoverQuery discoverQuery = new DiscoverQuery();

        discoverQuery.setStart(start);
        discoverQuery.setMaxResults(limit);
        discoverQuery.setQuery(getSolrQuery(text));

        String entityType = itemControlledVocabulary.getEntityType();
        if (StringUtils.isNotBlank(entityType)) {
            discoverQuery.addFilterQueries("dspace.entity.type:" + entityType);
        }

        try {
            DiscoverResult result = searchService.search(ContextUtil.obtainCurrentRequestContext(), discoverQuery);

            if (!result.getIndexableObjects().isEmpty()) {
                int total = (int) result.getTotalSearchResults();

                List<Choice> choices = getChoicesFromResult(itemControlledVocabulary, result);

                return new Choices(choices.toArray(new Choice[choices.size()]), start, total, Choices.CF_AMBIGUOUS,
                        total > start + limit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new Choices(Choices.CF_UNSET);
        }

        return new Choices(Choices.CF_UNSET);
    }

    @Override
    public Choices getBestMatch(String text, String locale) {
        return getMatches(text, 0, 1, locale);
    }

    @Override
    public String getLabel(String key, String locale) {
        return null;
    }

    @Override
    public String getValue(String key, String locale) {
        return HierarchicalAuthority.super.getValue(key, locale);
    }

    @Override
    public Map<String, String> getExtra(String key, String locale) {
        return HierarchicalAuthority.super.getExtra(key, locale);
    }

    @Override
    public boolean isScrollable() {
        return HierarchicalAuthority.super.isScrollable();
    }

    @Override
    public Choice getChoice(String authKey, String locale) {
        ItemControlledVocabulary itemControlledVocabulary =
            itemControlledVocabularyFactory.getInstance(this.getPluginInstanceName());
        try {
            Item item = itemService
                .find(ContextUtil.obtainCurrentRequestContext(), UUID.fromString(authKey));
            return getChoiceFromItem(
                    super.getPluginInstanceName(),
                    itemControlledVocabulary,
                    item
            );
        } catch (Exception e) {
            log.warn(e);
        }

        return null;
    }

    @Override
    public boolean storeAuthorityInMetadata() {
        return DSpaceServicesFactory.getInstance().getConfigurationService()
                .getBooleanProperty("item.controlled.vocabularies."
                        + this.getPluginInstanceName()
                        + ".store-authority-in-metadata", true);
    }

    public String getSolrQuery(String searchTerm) {
        ItemControlledVocabulary itemControlledVocabulary =
                itemControlledVocabularyFactory.getInstance(this.getPluginInstanceName());
        String entityType = itemControlledVocabulary.getEntityType();
        return itemAuthorityServiceFactory.getInstance(entityType).getSolrQuery(searchTerm);
    }

}
