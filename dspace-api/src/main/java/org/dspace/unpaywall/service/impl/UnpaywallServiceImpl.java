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
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.io.IOUtils.copy;
import static org.dspace.unpaywall.model.UnpaywallStatus.IMPORTED;
import static org.dspace.unpaywall.model.UnpaywallStatus.NOT_FOUND;
import static org.dspace.unpaywall.model.UnpaywallStatus.NO_FILE;
import static org.dspace.unpaywall.model.UnpaywallStatus.PENDING;
import static org.dspace.unpaywall.model.UnpaywallStatus.SUCCESSFUL;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
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
import org.dspace.unpaywall.dao.UnpaywallDAO;
import org.dspace.unpaywall.dto.UnpaywallApiResponse;
import org.dspace.unpaywall.dto.UnpaywallItemVersionDto;
import org.dspace.unpaywall.model.Unpaywall;
import org.dspace.unpaywall.model.UnpaywallStatus;
import org.dspace.unpaywall.service.UnpaywallService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class UnpaywallServiceImpl implements UnpaywallService {

    private static final String LOCATION_HEADER = "Location";
    private static final String REFERER_HEADER = "Referer";
    public static final String BEST_OA_LOCATION = "best_oa_location";
    public static final String URL_FOR_PDF = "url_for_pdf";
    public static final String URL = "url";
    private final Logger logger = LoggerFactory.getLogger(UnpaywallServiceImpl.class);
    private final CloseableHttpClient client;

    private final ObjectMapper objectMapper = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    private ConfigurationService configurationService;

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

    private int timeout;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<String, CompletableFuture<Void>> requestMap = new ConcurrentHashMap<>();

    public UnpaywallServiceImpl() {
        HttpClientBuilder custom = HttpClients.custom();
        client = custom.disableAutomaticRetries().setMaxConnTotal(5)
                .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(timeout).build())
                .build();
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public void initUnpaywallCallIfNeeded(Context context, String doi, UUID itemId) {
        Optional<Unpaywall> unpaywall = findUnpaywall(context, doi, itemId);
        if (unpaywall.isEmpty()) {
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
    public void downloadResource(Context context, Unpaywall unpaywall, Item item) {
        CompletableFuture
            .runAsync(() -> resolveResourceForItem(unpaywall, item), executor)
            .orTimeout(30, TimeUnit.SECONDS)
            .exceptionally(throwable -> {
                logger.error("Cannot resolve the linked unpaywall resource", throwable);
                unpaywall.setStatus(UnpaywallStatus.NOT_FOUND);
                unpaywall.setPdfUrl(null);
                try {
                    unpaywallDAO.save(context, unpaywall);
                    context.commit();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
        updateStatus(context, unpaywall, PENDING);
    }

    protected void resolveResourceForItem(Unpaywall unpaywall, Item item) {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            Context context = new Context(Context.Mode.READ_WRITE);
            unpaywall = context.reloadEntity(unpaywall);
            try (InputStream inputstream = downloadResource(client, unpaywall.getPdfUrl())) {
                context.turnOffAuthorisationSystem();
                item = context.reloadEntity(item);
                List<Bundle> bundles = item.getBundles(Constants.DEFAULT_BUNDLE_NAME);

                if (bundles.isEmpty()) {
                    bundles.add(this.bundleService.create(context, item, Constants.DEFAULT_BUNDLE_NAME));
                }

                Bundle defaultBundle = bundles.get(0);
                Bitstream unpaywallResource = this.bitstreamService.create(context, defaultBundle, inputstream);
                unpaywallResource.setName(context, getPdfName(unpaywall));
                unpaywallResource.setSource(context, unpaywall.getPdfUrl());
                unpaywallResource.setFormat(context, bitstreamFormatService.guessFormat(context, unpaywallResource));
                bitstreamService.update(context, unpaywallResource);

                context.restoreAuthSystemState();

                updateStatus(context, unpaywall, IMPORTED);

                context.commit();
            } catch (IOException e) {
                updateStatus(context, unpaywall, NOT_FOUND);
                throw new RuntimeException("Cannot retrieve the linked unpaywall resource", e);
            } catch (SQLException | AuthorizeException e) {
                updateStatus(context, unpaywall, NOT_FOUND);
                throw new RuntimeException("Cannot store the linked unpaywall resource", e);
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Cannot connect to the linked unpaywall resource", e);
        }
    }

    private void updateStatus(Context context, Unpaywall unpaywall, UnpaywallStatus successful) {
        unpaywall.setStatus(successful);
        try {
            unpaywallDAO.save(context, unpaywall);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot update the status of the unpaywall: "  + unpaywall.getID(), e);
        }
    }

    private static String getPdfName(Unpaywall unpaywall) {
        return Optional.ofNullable(unpaywall.getPdfUrl())
            .map(s -> s.substring(s.lastIndexOf('/') + 1))
            .orElse(null);
    }

    protected InputStream downloadResource(HttpClient client, String pdfUrl) throws IOException {
        HttpGet httpGet = new HttpGet(pdfUrl);
        httpGet.addHeader("Accept", "audio/*, video/*, image/*, text/*");
        HttpResponse response = client.execute(httpGet);

        // If request returns 301, then get new url from headers and repeat
        while (response.getStatusLine().getStatusCode() == 301) {
            httpGet = new HttpGet(response.getFirstHeader(LOCATION_HEADER).getValue());
            httpGet.addHeader("Accept", "audio/*, video/*, image/*, text/*");
            response = client.execute(httpGet);
        }

        // If request returns 301, then get new url from headers and repeat
        if (response.getStatusLine().getStatusCode() == 400) {
            httpGet.addHeader("Referer", pdfUrl);
            response = client.execute(httpGet);
        }

        if (response.getStatusLine().getStatusCode() == 403) {
            throw new RuntimeException("Unable to download file, forbidden access");
        }

        return new BufferedInputStream(response.getEntity().getContent());
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
            throw new RuntimeException(e);
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
            // Request already in progress.
            return;
        }
        CompletableFuture<Void> newRequest = CompletableFuture
                .runAsync(() -> callApiAndUpdateUnpaywallRecord(doi, itemId), executor)
                .thenRun(() -> requestMap.remove(doi))
                .orTimeout(30, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    requestMap.remove(doi);
                    return null;
                });
        requestMap.put(doi, newRequest);
    }

    private void callApiAndUpdateUnpaywallRecord(String doi, UUID itemId) {
        try {
            Context context = new Context(Context.Mode.READ_WRITE);
            Unpaywall unpaywall = getUnpaywall(context, doi, itemId);

            callUnpaywallApi(doi).ifPresentOrElse(
                jsonResponse -> mapSuccessful(jsonResponse, unpaywall),
                () -> mapNotFound(unpaywall)
            );
            unpaywallDAO.save(context, unpaywall);
            context.commit();
        } catch (SQLException | RuntimeException e) {
            logger.error("Cannot retrieve unpaywall details for doi: " + doi, e);
            throw new RuntimeException(e);
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

    private Optional<String> callUnpaywallApi(String doi) {
        String endpoint = configurationService.getProperty("unpaywall.url");
        String email = getEmail();
        HttpGet method = null;

        try {
            URIBuilder uriBuilder = new URIBuilder(endpoint + doi);
            uriBuilder.addParameter("email", email);
            method = new HttpGet(uriBuilder.build());

            HttpResponse response = client.execute(method);
            StatusLine statusLine = response.getStatusLine();

            int statusCode = response.getStatusLine().getStatusCode();
            switch (statusCode) {
                case SC_OK:
                    InputStream responseStream = response.getEntity().getContent();
                    StringWriter writer = new StringWriter();
                    copy(responseStream, writer, StandardCharsets.UTF_8);
                    return of(writer.toString());
                case SC_NOT_FOUND:
                    return empty();
                default:
                    throw new RuntimeException("Http call failed: " + statusLine);
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    private String getEmail() {
        String email = configurationService.getProperty("unpaywall.email");
        if (StringUtils.isBlank(email)) {
            throw new RuntimeException("\"unpaywall.email\" property cannot be empty.");
        }
        return email;
    }

    private UnpaywallApiResponse mapJsonResponse(String unpaywallApiJson) {
        try {
            return objectMapper.readValue(unpaywallApiJson, UnpaywallApiResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Item getItem(Context context, UUID itemId) {
        try {
            return itemService.find(context, itemId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
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
}
