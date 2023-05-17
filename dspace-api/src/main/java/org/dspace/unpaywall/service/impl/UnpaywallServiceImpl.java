/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.unpaywall.service.impl;


import static com.rometools.utils.Strings.isBlank;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.io.IOUtils.copy;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PreDestroy;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.unpaywall.dao.UnpaywallDAO;
import org.dspace.unpaywall.model.Unpaywall;
import org.dspace.unpaywall.model.UnpaywallStatus;
import org.dspace.unpaywall.service.UnpaywallService;
import org.springframework.beans.factory.annotation.Autowired;

public class UnpaywallServiceImpl implements UnpaywallService {

    private CloseableHttpClient client;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private UnpaywallDAO unpaywallDAO;

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
    public Unpaywall getUnpaywallCall(Context context, String doi, UUID itemId) throws SQLException {
        if (isBlank(doi) || isNull(itemId)) {
            throw new IllegalArgumentException();
        }
        Unpaywall unpaywall = unpaywallDAO.uniqueByDOIAndItemID(context, doi, itemId);
        if (isNull(unpaywall)) {
            initApiCall(doi, itemId);
            unpaywall = createUnpaywall(doi, itemId);
        }
        return unpaywall;
    }

    private Unpaywall createUnpaywall(String doi, UUID itemId) {
        Unpaywall unpaywall = new Unpaywall();
        unpaywall.setDoi(doi);
        unpaywall.setItemId(itemId);
        unpaywall.setStatus(UnpaywallStatus.PENDING);
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

            unpaywallCall(doi).ifPresentOrElse(value -> {
                unpaywall.setJsonRecord(value);
                unpaywall.setStatus(UnpaywallStatus.SUCCESSFUL);
            }, () -> {
                unpaywall.setJsonRecord(null);
                unpaywall.setStatus(UnpaywallStatus.NOT_FOUND);
            });
            unpaywallDAO.save(context, unpaywall);
            context.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Unpaywall getUnpaywall(Context context, String doi, UUID itemId) throws SQLException {
        Unpaywall unpaywall = unpaywallDAO.uniqueByItemId(context, itemId);
        if (nonNull(unpaywall)) {
            unpaywall.setDoi(doi);
            return unpaywall;
        }
        return unpaywallDAO.create(context, createUnpaywall(doi, itemId));
    }

    private Optional<String> unpaywallCall(String doi) {
        String endpoint = configurationService.getProperty("unpaywall.url");
        String email = configurationService.getProperty("unpaywall.email");
        HttpGet method = null;

        try {
            endpoint = endpoint + doi;
            URIBuilder uriBuilder = new URIBuilder(endpoint);
            uriBuilder.addParameter("email", email);
            method = new HttpGet(uriBuilder.build());

            HttpResponse response = client.execute(method);
            StatusLine statusLine = response.getStatusLine();

            int statusCode = response.getStatusLine().getStatusCode();
            switch (statusCode) {
                case 200:
                    InputStream is = response.getEntity().getContent();
                    StringWriter writer = new StringWriter();
                    copy(is, writer, "UTF-8");
                    return of(writer.toString());
                case 404:
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
}
