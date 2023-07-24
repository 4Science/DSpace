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
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.io.IOUtils.copy;

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

    private final CloseableHttpClient client;

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

            callUnpaywallApi(doi).ifPresentOrElse(value -> {
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
        Optional<Unpaywall> unpaywall = unpaywallDAO.findByItemId(context, itemId);
        if (unpaywall.isPresent()) {
            unpaywall.get().setDoi(doi);
            return unpaywall.get();
        }
        return unpaywallDAO.create(context, createUnpaywall(doi, itemId));
    }

    private Optional<String> callUnpaywallApi(String doi) {
        String endpoint = configurationService.getProperty("unpaywall.url");
        String email = configurationService.getProperty("unpaywall.email");
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
}
