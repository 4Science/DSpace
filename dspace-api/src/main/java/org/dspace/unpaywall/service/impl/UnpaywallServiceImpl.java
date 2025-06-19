/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.unpaywall.service.impl;


import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.rometools.utils.Strings.isBlank;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.dspace.unpaywall.model.UnpaywallStatus.IMPORTED;
import static org.dspace.unpaywall.model.UnpaywallStatus.NOT_FOUND;
import static org.dspace.unpaywall.model.UnpaywallStatus.NO_FILE;
import static org.dspace.unpaywall.model.UnpaywallStatus.PENDING;
import static org.dspace.unpaywall.model.UnpaywallStatus.SUCCESSFUL;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.unpaywall.dao.UnpaywallDAO;
import org.dspace.unpaywall.dto.UnpaywallApiResponse;
import org.dspace.unpaywall.dto.UnpaywallItemVersionDto;
import org.dspace.unpaywall.model.Unpaywall;
import org.dspace.unpaywall.model.UnpaywallStatus;
import org.dspace.unpaywall.service.UnpaywallClientAPI;
import org.dspace.unpaywall.service.UnpaywallService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class UnpaywallServiceImpl implements UnpaywallService {

    public static final String BEST_OA_LOCATION = "best_oa_location";
    public static final String URL_FOR_PDF = "url_for_pdf";
    public static final String URL = "url";
    private final Logger logger = LoggerFactory.getLogger(UnpaywallServiceImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance()
                             .getConfigurationService();

    @Autowired
    private UnpaywallClientAPI unpaywallClientAPI;

    @Autowired
    private UnpaywallDAO unpaywallDAO;

    @Autowired
    private ItemService itemService;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private BundleService bundleService;

    @Autowired
    private BitstreamFormatService bitstreamFormatService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<String, CompletableFuture<Void>> requestMap = new ConcurrentHashMap<>();

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public void initUnpaywallCallIfNeeded(Context context, String doi, UUID itemId) {
        if (findUnpaywall(context, doi, itemId).isEmpty()) {
            initUnpaywallCall(context, doi, itemId);
        }
    }

    @Override
    public void initUnpaywallCall(Context context, String doi, UUID itemId) {
        if (isBlank(doi) || isNull(itemId)) {
            throw new IllegalArgumentException();
        }
        initApiCall(doi, itemId);
    }

    @Override
    public Optional<Unpaywall> findUnpaywall(Context context, String doi, UUID itemId) {
        return unpaywallDAO.findByDOIAndItemID(context, doi, itemId);
    }

    @Override
    public Unpaywall create(Context context, Unpaywall unpaywall) {
        try {
            return unpaywallDAO.create(context, unpaywall);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Unpaywall> findAll(Context context) {
        try {
            return unpaywallDAO.findAll(context, Unpaywall.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<UnpaywallItemVersionDto> getItemVersions(Context context, UUID itemId) {
        Item item = getItem(context, itemId);
        return getItemVersions(context, item);
    }

    @Override
    public Unpaywall downloadResource(Context context, Unpaywall unpaywall, Item item) {
        updateStatus(context, unpaywall, PENDING);
        return resolveResourceForItem(unpaywall, item);
    }

    protected Unpaywall resolveResourceForItem(Unpaywall unpaywall, Item item) {
        Context context = new Context(Context.Mode.READ_WRITE);
        try (InputStream inputStream =
                 new FileInputStream(unpaywallClientAPI.downloadResource(unpaywall.getPdfUrl()))) {
            createUnpaywallBitstream(
                context, unpaywall,
                getOrCreateBundle(item, item.getBundles(Constants.DEFAULT_BUNDLE_NAME), context),
                inputStream
            );
            updateStatus(context, unpaywall, IMPORTED);
        } catch (IOException e) {
            unpaywall.setPdfUrl(null);
            updateStatus(context, unpaywall, NOT_FOUND);
            logger.error("Cannot retrieve the linked unpaywall resource", e);
            throw new RuntimeException("Cannot retrieve the linked unpaywall resource", e);
        } catch (SQLException | AuthorizeException e) {
            unpaywall.setPdfUrl(null);
            updateStatus(context, unpaywall, NOT_FOUND);
            logger.error("Cannot store the linked unpaywall resource", e);
            throw new RuntimeException("Cannot store the linked unpaywall resource", e);
        }
        return unpaywall;
    }

    private Bitstream createUnpaywallBitstream(Context context, Unpaywall unpaywall, Bundle defaultBundle,
                                               InputStream inputstream)
        throws IOException, SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();

        Bitstream unpaywallResource = this.bitstreamService.create(context, defaultBundle, inputstream);
        unpaywallResource.setName(context, getPdfName(unpaywall));
        unpaywallResource.setSource(context, unpaywall.getPdfUrl());
        unpaywallResource.setFormat(context, bitstreamFormatService.guessFormat(context, unpaywallResource));
        bitstreamService.update(context, unpaywallResource);

        context.restoreAuthSystemState();
        return unpaywallResource;
    }

    private Bundle getOrCreateBundle(Item item, List<Bundle> bundles, Context context) throws SQLException,
        AuthorizeException {
        if (bundles.isEmpty()) {
            context.turnOffAuthorisationSystem();
            bundles.add(this.bundleService.create(context, item, Constants.DEFAULT_BUNDLE_NAME));
            context.restoreAuthSystemState();
        }
        return bundles.get(0);
    }

    private void updateStatus(Context context, Unpaywall unpaywall, UnpaywallStatus successful) {
        unpaywall.setStatus(successful);
        try {
            unpaywallDAO.save(context, unpaywall);
        } catch (SQLException e) {
            logger.error("Cannot update the status of the unpaywall: "  + unpaywall.getID(), e);
            throw new RuntimeException("Cannot update the status of the unpaywall: "  + unpaywall.getID(), e);
        }
    }

    private static String getPdfName(Unpaywall unpaywall) {
        return Optional.ofNullable(unpaywall.getPdfUrl())
                       .map(s -> s.substring(s.lastIndexOf('/') + 1))
                       .orElse(null);
    }

    @Override
    public List<UnpaywallItemVersionDto> getItemVersions(Context context, Item item) {
        String doi = itemService.getMetadataFirstValue(item, "dc", "identifier", "doi", Item.ANY);
        return findUnpaywall(context, doi, item.getID())
            .filter(unpaywall -> SUCCESSFUL.equals(unpaywall.getStatus()))
            .map(unpaywall -> {
                String unpaywallApiJson = unpaywall.getJsonRecord();
                UnpaywallApiResponse unpaywallApiResponse = mapJsonResponse(unpaywallApiJson);
                return unpaywallApiResponse.getOaLocations();
            })
            .stream().flatMap(List::stream)
            .map(UnpaywallServiceImpl::mapUnpaywallItemVersionDto)
            .collect(Collectors.toList());
    }

    @Override
    public void delete(Context context, Unpaywall unpaywall) {
        try {
            unpaywallDAO.delete(context, unpaywall);
        } catch (SQLException e) {
            logger.error("Cannot delete the unpaywall: "  + unpaywall.getID(), e);
            throw new RuntimeException("Cannot delete the unpaywall: "  + unpaywall.getID(), e);
        }
    }

    private Unpaywall createUnpaywall(String doi, UUID itemId) {
        Unpaywall unpaywall = new Unpaywall();
        unpaywall.setDoi(doi);
        unpaywall.setItemId(itemId);
        unpaywall.setStatus(PENDING);
        unpaywall.setTimestampCreated(new Date());
        return unpaywall;
    }

    private void initApiCall(String doi, UUID itemId) {
        CompletableFuture<Void> currentRequest = requestMap.get(doi);
        if (nonNull(currentRequest) && !currentRequest.isDone()) {
            return;
        }
        CompletableFuture<Void> newRequest = CompletableFuture
            .runAsync(() -> callApiAndUpdateUnpaywallRecord(doi, itemId), executor)
            .thenRun(() -> requestMap.remove(doi))
            .exceptionally(throwable -> {
                logger.error("Cannot find the unpaywall for doi: " + doi, throwable);
                requestMap.remove(doi);
                return null;
            });
        requestMap.put(doi, newRequest);
    }

    private void callApiAndUpdateUnpaywallRecord(String doi, UUID itemId) {
        try (Context context = new Context(Context.Mode.READ_WRITE);) {
            Unpaywall unpaywall = getUnpaywall(context, doi, itemId);
            unpaywall.setStatus(PENDING);
            unpaywallDAO.save(context, unpaywall);
            context.commit();
            unpaywall = context.reloadEntity(unpaywall);
            Optional<String> jsonResponse = unpaywallClientAPI.callUnpaywallApi(doi);
            if (jsonResponse.isPresent()) {
                mapSuccessful(jsonResponse.get(), unpaywall);
                unpaywallDAO.save(context, unpaywall);
                context.complete();
                return;
            } else {
                mapNotFound(unpaywall);
                unpaywallDAO.save(context, unpaywall);
                context.complete();
            }
        } catch (Exception e) {
            logger.error("Cannot retrieve unpaywall details for doi: " + doi, e);
            try (Context context = new Context(Context.Mode.READ_WRITE);) {
                Unpaywall unpaywall = getUnpaywall(context, doi, itemId);
                mapNotFound(unpaywall);
                unpaywallDAO.save(context, unpaywall);
                context.complete();
            } catch (SQLException e1) {
                logger.error("Error saving unpaywall details for doi: " + doi, e1);
                throw new RuntimeException(e1);
            }
        }
    }


    private void mapSuccessful(String jsonResponse, Unpaywall unpaywall) {
        unpaywall.setJsonRecord(jsonResponse);
        JSONObject jsonRecord = new JSONObject(jsonResponse);
        if (jsonRecord.has(BEST_OA_LOCATION)) {
            JSONObject jsonLocation = jsonRecord.getJSONObject(BEST_OA_LOCATION);
            if (jsonLocation.has(URL_FOR_PDF) && !jsonLocation.isNull(URL_FOR_PDF)) {
                unpaywall.setPdfUrl(jsonLocation.getString(URL_FOR_PDF));
                unpaywall.setStatus(SUCCESSFUL);
            } else {
                unpaywall.setStatus(NO_FILE);
            }
        } else {
            unpaywall.setStatus(NO_FILE);
        }
    }

    private void mapNotFound(Unpaywall unpaywall) {
        unpaywall.setJsonRecord(null);
        unpaywall.setStatus(UnpaywallStatus.NOT_FOUND);
    }

    private Unpaywall getUnpaywall(Context context, String doi, UUID itemId) throws SQLException {
        Optional<Unpaywall> unpaywall = unpaywallDAO.findByItemId(context, itemId);
        if (unpaywall.isPresent()) {
            unpaywall.get().setDoi(doi);
            return unpaywall.get();
        }
        return unpaywallDAO.create(context, createUnpaywall(doi, itemId));
    }

    private UnpaywallApiResponse mapJsonResponse(String unpaywallApiJson) {
        try {
            return objectMapper.readValue(unpaywallApiJson, UnpaywallApiResponse.class);
        } catch (JsonProcessingException e) {
            logger.error("Cannot parse the json response: " + unpaywallApiJson, e);
            throw new RuntimeException("Cannot parse the json response: " + unpaywallApiJson, e);
        }
    }

    private Item getItem(Context context, UUID itemId) {
        try {
            return itemService.find(context, itemId);
        } catch (SQLException e) {
            logger.error("Cannot find the item wiht uuid: " + itemId, e);
            throw new RuntimeException("Cannot find the item wiht uuid: " + itemId, e);
        }
    }

    private static UnpaywallItemVersionDto mapUnpaywallItemVersionDto(UnpaywallApiResponse.OaLocation itemVersion) {
        return new UnpaywallItemVersionDto(
            itemVersion.getVersion(),
            itemVersion.getLicense(),
            itemVersion.getUrlForLandingPage(),
            itemVersion.getUrlToPdf(),
            itemVersion.getHostType()
        );
    }

    public UnpaywallClientAPI getUnpaywallClientAPI() {
        return unpaywallClientAPI;
    }

    public void setUnpaywallClientAPI(UnpaywallClientAPI spyClientAPI) {
        this.unpaywallClientAPI = spyClientAPI;
    }
}
