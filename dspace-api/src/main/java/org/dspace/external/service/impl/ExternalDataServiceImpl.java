/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.service.impl;

import static org.dspace.app.deduplication.service.impl.SolrDedupServiceImpl.RESOURCE_FLAG_FIELD;
import static org.dspace.app.deduplication.service.impl.SolrDedupServiceImpl.RESOURCE_IDS_FIELD;
import static org.dspace.app.deduplication.service.impl.SolrDedupServiceImpl.RESOURCE_SIGNATURE_FIELD;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.deduplication.service.DedupService;
import org.dspace.app.deduplication.service.impl.SolrDedupServiceImpl;
import org.dspace.app.deduplication.utils.Signature;
import org.dspace.app.suggestion.SuggestionProvider;
import org.dspace.app.suggestion.SuggestionService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.discovery.SearchServiceException;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.external.service.ExternalDataService;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ExternalDataService}
 */
public class ExternalDataServiceImpl implements ExternalDataService {

    private static final Logger log
            = org.apache.logging.log4j.LogManager.getLogger();

    @Autowired
    private List<ExternalDataProvider> externalDataProviders;

    @Autowired
    private ItemService itemService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private SuggestionService suggestionService;

    @Autowired
    private DedupService dedupService;

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String source, String id) {
        ExternalDataProvider provider = getExternalDataProvider(source);
        if (provider == null) {
            throw new IllegalArgumentException("Provider for: " + source + " couldn't be found");
        }
        return provider.getExternalDataObject(id);
    }

    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String source, String query, int start, int limit) {
        ExternalDataProvider provider = getExternalDataProvider(source);
        if (provider == null) {
            throw new IllegalArgumentException("Provider for: " + source + " couldn't be found");
        }

        List<ExternalDataObject> externalDataObjects = provider.searchExternalDataObjects(query, start, limit);
        appendMatchedUUIDs(externalDataObjects);

        return externalDataObjects;
    }

    private void appendMatchedUUIDs(List<ExternalDataObject> externalDataObjects) {
        for (ExternalDataObject externalDataObject : externalDataObjects) {
            List<UUID> uuids = new ArrayList<>();
            try {
                QueryResponse response = dedupService.find("*:*", buildFilters(externalDataObject));
                for (SolrDocument resultDoc : response.getResults()) {
                    uuids.addAll(resultDoc.getFieldValues(RESOURCE_IDS_FIELD)
                                          .stream()
                                          .map(id ->
                                              UUID.fromString(String.valueOf(id)))
                                          .collect(Collectors.toList()));
                }
                externalDataObject.setMatchUUIDs(uuids);
            } catch (SearchServiceException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String[] buildFilters(ExternalDataObject object) {
        List<String> filters = new ArrayList<>();
        List<String> allSignatures = getAllSignatures(object);

        if (!allSignatures.isEmpty()) {
            filters.add(RESOURCE_FLAG_FIELD + ":" + SolrDedupServiceImpl.DeduplicationFlag.FAKE.getDescription());
            filters.add(RESOURCE_SIGNATURE_FIELD + ":(" +
                StringUtils.joinWith(" OR ", allSignatures.stream().toArray(String[]::new)) + ")");
        }

        return filters.toArray(new String[filters.size()]);
    }

    private List<String> getAllSignatures(ExternalDataObject iu) {
        List<Signature> signAlgo = new DSpace().getServiceManager().getServicesByType(Signature.class);
        return signAlgo.stream()
                       .filter(algo -> Constants.ITEM == algo.getResourceTypeID())
                       .flatMap(algo -> algo.getSignature(iu).stream())
                       .filter(signature -> StringUtils.isNotEmpty(signature))
                       .collect(Collectors.toList());
    }

    @Override
    public List<ExternalDataProvider> getExternalDataProviders() {
        return externalDataProviders;
    }

    @Override
    public List<ExternalDataProvider> getExternalDataProvidersForEntityType(String entityType) {
        return externalDataProviders.stream().filter(edp -> edp.supportsEntityType(entityType))
                .collect(Collectors.toList());
    }

    @Override
    public ExternalDataProvider getExternalDataProvider(String sourceIdentifier) {
        for (ExternalDataProvider externalDataProvider : externalDataProviders) {
            if (externalDataProvider.supports(sourceIdentifier)) {
                return externalDataProvider;
            }
        }
        return null;
    }

    @Override
    public int getNumberOfResults(String source, String query) {
        ExternalDataProvider provider = getExternalDataProvider(source);
        if (provider == null) {
            throw new IllegalArgumentException("Provider for: " + source + " couldn't be found");
        }
        return provider.getNumberOfResults(query);
    }


    @Override
    public WorkspaceItem createWorkspaceItemFromExternalDataObject(Context context,
                                                                    ExternalDataObject externalDataObject,
                                                                    Collection collection)
        throws AuthorizeException, SQLException {
        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, true);
        Item item = workspaceItem.getItem();
        for (MetadataValueDTO metadataValueDTO : externalDataObject.getMetadata()) {
            if (metadataValueDTO.getValue() == null) {
                // skip invalid metadata
                continue;
            }
            if (metadataValueDTO.getAuthority() == null) {
                itemService.addMetadata(context, item, metadataValueDTO.getSchema(), metadataValueDTO.getElement(),
                    metadataValueDTO.getQualifier(), metadataValueDTO.getLanguage(),
                    metadataValueDTO.getValue());
            } else {
                itemService.addMetadata(context, item, metadataValueDTO.getSchema(), metadataValueDTO.getElement(),
                    metadataValueDTO.getQualifier(), metadataValueDTO.getLanguage(),
                    metadataValueDTO.getValue(), metadataValueDTO.getAuthority(),
                    metadataValueDTO.getConfidence());
            }
        }

        log.info(LogHelper.getHeader(context, "create_item_from_externalDataObject", "Created item" +
            "with id: " + item.getID() + " from source: " + externalDataObject.getSource() + " with identifier: " +
            externalDataObject.getId()));
        try {
            List<SuggestionProvider> providers = suggestionService.getSuggestionProviders();
            if (providers != null) {
                for (SuggestionProvider p : providers) {
                    p.flagRelatedSuggestionsAsProcessed(context, externalDataObject);
                }
            }
        } catch (Exception e) {
            log.error("Got problems with the solr suggestion storage service: " + e.getMessage(), e);
        }
        return workspaceItem;
    }

}
