/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.dspace.discovery.SearchUtils.AUTHORITY_SEPARATOR;
import static org.dspace.discovery.SearchUtils.FILTER_SEPARATOR;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.MultiLanguageDiscoverSearchFilterFacet;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.services.ConfigurationService;
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

    private Map<String, DCInputsReader> dcInputsReaders = new HashMap<>();

    private String separator;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ChoiceAuthorityService cas;

    @Autowired
    private ConfigurationService configurationService;

    @PostConstruct
    public void setup() throws DCInputsReaderException {
        separator = configurationService.getProperty("discovery.solr.facets.split.char", FILTER_SEPARATOR);
        for (Locale locale : I18nUtil.getSupportedLocales()) {
            dcInputsReaders.put(locale.getLanguage(), new DCInputsReader(I18nUtil.getInputFormsFileName(locale)));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void additionalIndex(Context context, IndexableObject object, SolrInputDocument document) {

        if (isNotIndexableItem(object)) {
            return;
        }

        Item item = ((IndexableItem)object).getIndexedObject();

        try {

            for (String language : dcInputsReaders.keySet()) {
                List<DCInput> valueListInputs = getAllValueListInputs(context, language, item);
                for (DCInput valueListInput : valueListInputs) {
                    additionalIndex(valueListInput, item, language, document);
                }
            }

        } catch (Exception ex) {
            LOGGER.error("An error occurs indexing value pairs for item " + item.getID(), ex);
        }

    }

    private void additionalIndex(DCInput valueListInput, Item item, String language, SolrInputDocument document) {

        String metadataField = valueListInput.getFieldName();
        List<DiscoverySearchFilter> searchFilters = findSearchFiltersByMetadataField(item, metadataField);
        List<MetadataValue> metadataValues = itemService.getMetadataByMetadataString(item, metadataField);

        for (MetadataValue metadataValue : metadataValues) {

            String authority = metadataValue.getAuthority();
            String value = getMetadataValue(metadataValue, valueListInput, language);

            for (DiscoverySearchFilter searchFilter : searchFilters) {
                addDiscoveryFieldFields(language, document, value, authority, searchFilter);
            }

        }

    }

    private String getMetadataValue(MetadataValue metadataValue, DCInput valueListInput, String language) {
        if (valueListInput.isControlledVocabulary() && isNotBlank(metadataValue.getAuthority())) {
            return getControlledVocabularyValue(metadataValue, language);
        } else {
            return getDisplayValue(valueListInput, metadataValue);
        }
    }

    private String getControlledVocabularyValue(MetadataValue metadataValue, String language) {
        String value = metadataValue.getValue();
        String authority = metadataValue.getAuthority();

        if (StringUtils.isBlank(authority)) {
            return value;
        }

        String [] authorityValue = metadataValue.getAuthority().split(":");
        if (authorityValue.length != 2) {
            return value;
        }

        try {
            ChoiceAuthority choiceAuthority = cas.getChoiceAuthorityByAuthorityName(authorityValue[0]);
            Choice choice = choiceAuthority.getChoice(authorityValue[1], language);
            return choice != null ? choice.label : value;
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("An error occurs getting controlled vocabulary value: " + getRootCauseMessage(ex));
            return value;
        }
    }

    private void addDiscoveryFieldFields(String language, SolrInputDocument document, String value, String authority,
        DiscoverySearchFilter searchFilter) {

        String fieldNameWithLanguage = language + "_" + searchFilter.getIndexFieldName();
        String valueLowerCase = value.toLowerCase();

        String keywordField = appendAuthorityIfNotBlank(value, authority);
        String acidField = appendAuthorityIfNotBlank(valueLowerCase + separator + value, authority);
        String filterField = appendAuthorityIfNotBlank(valueLowerCase + separator + value, authority);

        document.addField(fieldNameWithLanguage + "_keyword", keywordField);
        document.addField(fieldNameWithLanguage + "_acid", acidField);
        document.addField(fieldNameWithLanguage + "_filter", filterField);
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
            return SearchUtils.getAllDiscoveryConfigurations(item);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private String getDisplayValue(DCInput valueListInput, MetadataValue metadataValue) {
        String displayValue = valueListInput.getDisplayString(metadataValue.getValue());
        if (StringUtils.isEmpty(displayValue)) {
            displayValue = metadataValue.getValue();
        }
        return displayValue;
    }

    private List<DCInput> getAllValueListInputs(Context context, String language, Item item) {
        return getInputs(context, language, item).stream()
            .flatMap(this::getAllDCInput)
            .filter(dcInput -> dcInput.isDropDown()
                            || dcInput.isList()
                            || StringUtils.isNotBlank(dcInput.getVocabulary()))
            .collect(Collectors.toList());
    }

    private List<DCInputSet> getInputs(Context context, String language, Item item) {
        try {
            Collection collection = (Collection) itemService.getParentObject(context, item);
            return dcInputsReaders.get(language).getInputsByCollection(collection);
        } catch (DCInputsReaderException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<DCInput> getAllDCInput(DCInputSet dcInputSet) {
        return Arrays.stream(dcInputSet.getFields())
            .flatMap(dcInputs -> Arrays.stream(dcInputs));
    }

    @SuppressWarnings("rawtypes")
    private boolean isNotIndexableItem(IndexableObject object) {
        return !(object instanceof IndexableItem);
    }

}
