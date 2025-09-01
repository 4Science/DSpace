/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.dspace.discovery.SearchUtils.AUTHORITY_SEPARATOR;
import static org.dspace.discovery.SearchUtils.FILTER_SEPARATOR;
import static org.dspace.discovery.SolrServiceImpl.SOLR_FIELD_SUFFIX_FACET_PREFIXES;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.DCInputAuthority;
import org.dspace.content.authority.DSpaceControlledVocabulary;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.MultiLanguageDiscoverSearchFilterFacet;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.services.ConfigurationService;
import org.dspace.web.ContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link SolrServiceIndexPlugin} to add indexes for value
 * pairs.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class SolrServiceValuePairsIndexPlugin implements SolrServiceIndexPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrServiceValuePairsIndexPlugin.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private ChoiceAuthorityService cas;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    @SuppressWarnings("rawtypes")
    public void additionalIndex(Context context, IndexableObject object, SolrInputDocument document) {
        if (isNotIndexableItem(object)) {
            return;
        }

        Item item = ((IndexableItem)object).getIndexedObject();
        try {
            Collection collection = (Collection) itemService.getParentObject(context, item);
            for (MetadataValue metadata : item.getItemService().getMetadata(item, Item.ANY, Item.ANY, Item.ANY,
                    Item.ANY)) {
                for (Locale locale : I18nUtil.getSupportedLocales()) {
                    String language = locale.getLanguage();
                    if (cas.isChoicesConfigured(metadata.getMetadataField().toString(), item.getType(), collection)) {
                        additionalIndex(collection, item, metadata, language, document);
                    }
                }
            }

        } catch (Exception ex) {
            LOGGER.error("An error occurs indexing value pairs for item " + item.getID(), ex);
        }
    }

    private void additionalIndex(Collection collection, Item item, MetadataValue metadataValue, String language,
            SolrInputDocument document) {
        String metadataField = metadataValue.getMetadataField().toString('.');
        List<DiscoverySearchFilter> searchFilters = findSearchFiltersByMetadataField(item, metadataField);
        String authority = metadataValue.getAuthority();
        String value = getMetadataValue(collection, metadataValue, language);
        if (StringUtils.isNotBlank(value)) {
            for (DiscoverySearchFilter searchFilter : searchFilters) {
                addDiscoveryFieldFields(language, document, value, authority, searchFilter);
            }
        }
    }

    private String getMetadataValue(Collection collection, MetadataValue metadataValue, String language) {
        String fieldKey = metadataValue.getMetadataField().toString();
        ChoiceAuthority choiceAuthority = cas.getAuthorityByFieldKeyCollection(fieldKey, Constants.ITEM, collection);
        String authority = metadataValue.getAuthority();
        if (choiceAuthority instanceof DSpaceControlledVocabulary) {
            String label = StringUtils.isNotBlank(authority) ? choiceAuthority.getLabel(authority, language)
                    : metadataValue.getValue();
            if (StringUtils.isBlank(label)) {
                label = metadataValue.getValue();
            }
            return label;
        } else if (choiceAuthority instanceof DCInputAuthority) {
            String label = choiceAuthority.getLabel(metadataValue.getValue(), language);
            if (StringUtils.isBlank(label)) {
                label = metadataValue.getValue();
            }
            return label;
        }
        return null;
    }

    private void addDiscoveryFieldFields(String language, SolrInputDocument document, String value, String authority,
        DiscoverySearchFilter searchFilter) {
        String separator = configurationService.getProperty("discovery.solr.facets.split.char", FILTER_SEPARATOR);
        String fieldNameWithLanguage = language + "_" + searchFilter.getIndexFieldName();
        String valueLowerCase = value.toLowerCase();

        String keywordField = appendAuthorityIfNotBlank(value, authority);
        String acidField = appendAuthorityIfNotBlank(valueLowerCase + separator + value, authority);
        String filterField = appendAuthorityIfNotBlank(valueLowerCase + separator + value, authority);
        String prefixField = appendAuthorityIfNotBlank(valueLowerCase + separator + value, authority);

        document.addField(fieldNameWithLanguage + "_keyword", keywordField);
        document.addField(fieldNameWithLanguage + "_acid", acidField);
        document.addField(fieldNameWithLanguage + "_filter", filterField);
        document.addField(fieldNameWithLanguage + SOLR_FIELD_SUFFIX_FACET_PREFIXES, prefixField);
        document.addField(fieldNameWithLanguage + "_ac", valueLowerCase + separator + value);
        if (document.containsKey(searchFilter.getIndexFieldName() + "_authority")) {
            document.addField(fieldNameWithLanguage + "_authority", authority);
        }
    }

    private String appendAuthorityIfNotBlank(String fieldValue, String authority) {
        return isNotBlank(authority) ? fieldValue + AUTHORITY_SEPARATOR + authority : fieldValue;
    }

    /**
     * Returns all the search fields configured for the given metadataField. Filters
     * returned are not filtered by instance type equal to
     * {@link MultiLanguageDiscoverSearchFilterFacet} to allow for language-based
     * searches
     */
    private List<DiscoverySearchFilter> findSearchFiltersByMetadataField(Item item, String metadataField) {
        return getAllDiscoveryConfiguration(item).stream()
            .flatMap(discoveryConfiguration -> discoveryConfiguration.getSearchFilters().stream())
            .filter(searchFilter -> searchFilter.getMetadataFields().contains(metadataField))
            .distinct()
            .collect(Collectors.toList());
    }

    private List<DiscoveryConfiguration> getAllDiscoveryConfiguration(Item item) {
        try {
            return SearchUtils.getAllDiscoveryConfigurations(ContextUtil.obtainCurrentRequestContext(), item);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    private boolean isNotIndexableItem(IndexableObject object) {
        return !(object instanceof IndexableItem);
    }

}
