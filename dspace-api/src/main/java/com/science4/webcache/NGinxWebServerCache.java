/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package com.science4.webcache;

import static org.apache.http.HttpHeaders.CACHE_CONTROL;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PreDestroy;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

public class NGinxWebServerCache extends AbstractWebServerCache {
    private static final String DS_LANGUAGE = "dsLanguage";
    private static final String DOMAIN_NAME_REGEX_EXPR = "http(s)?://|www\\.|/.*";
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(NGinxWebServerCache.class);
    private CloseableHttpClient client;
    private CloseableHttpClient clientRenew;
    private BasicCookieStore cookieStore;
    private ExecutorService executor;
    private ExecutorService executorRenew;
    private int timeout = 1000;
    private int timeoutRenew = 5000;
    private int threads = 5;
    private int threadsRenew = 1;

    public void initialize () {
        super.initialize();
        HttpClientBuilder custom = HttpClients.custom();
        cookieStore = new BasicCookieStore();
        client = custom.disableAutomaticRetries().setMaxConnTotal(threads)
                .setDefaultCookieStore(cookieStore)
                .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(timeout).build())
                .build();
        clientRenew = custom.disableAutomaticRetries().setMaxConnTotal(threadsRenew)
                .setDefaultCookieStore(cookieStore)
                .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(timeoutRenew).build())
                .build();
        executor = Executors.newFixedThreadPool(threads);
        executorRenew = Executors.newFixedThreadPool(threadsRenew);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public void invalidateAndRenew(Context ctx, Set<String> urlsToUpdate, Set<String> urlsToRemove) {
        urlsToUpdate.stream().forEach(url -> {
            log.debug("Renewing url {}", url);
            CompletableFuture.runAsync(() -> invalidateUrl(url), executor).thenRun(() -> {
                CompletableFuture.runAsync(() -> generateCache(url), executorRenew).exceptionally(throwable -> {
                    log.error("Failure renewing url in cache " + url, throwable);
                    return null;
                });
            }).exceptionally(throwable -> {
                log.error("Failure removing url in cache (not refreshed)" + url, throwable);
                return null;
            });
        });

        urlsToRemove.stream().forEach(url -> {
            log.debug("Removing from cache url {}", url);
            CompletableFuture.runAsync(() -> invalidateUrl(url), executor).exceptionally(throwable -> {
                log.error("Failure removing url from cache {}", url);
                return null;
            });
        });
    }

    private void generateCache(String url) {
        try {
            ConfigurationService config = DSpaceServicesFactory.getInstance().getConfigurationService();
            String[] languages = config.getArrayProperty("event.consumer.webcache.languages");
            for (String language : languages) {
                addCookie(url, language);

                renewCache(url);
                cookieStore.clear();
            }
            renewCache(url);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Error generating the new cache");
        }
    }

    private void renewCache(String url) throws IOException {
        CloseableHttpResponse response = clientRenew.execute(new HttpGet(url));
        if (log.isDebugEnabled()) {
            log.debug("Generate new cache response code {}", response.getStatusLine().getStatusCode());
        }
        response.close();
    }

    private void invalidateUrl(String url) {
        try {
            ConfigurationService config = DSpaceServicesFactory.getInstance().getConfigurationService();
            String[] languages = config.getArrayProperty("event.consumer.webcache.languages");
            for (String language : languages) {
                addCookie(url, language);

                refreshCache(url);
                cookieStore.clear();
            }
            refreshCache(url);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Error invalidating the cache");
        }
    }

    private void refreshCache(String url) throws IOException {
        HttpUriRequest refreshCacheRequest = RequestBuilder.create("HEAD")
                .setUri(url)
                .setHeader(CACHE_CONTROL, "refresh")
                .build();

        CloseableHttpResponse response = client.execute(refreshCacheRequest);
        if (log.isDebugEnabled()) {
            log.debug("Invalidate cache response code {}", response.getStatusLine().getStatusCode());
        }
        response.close();
    }

    private void addCookie(String url, String language) throws IOException, URISyntaxException {
        URI uri = new URI(url);
        String domainName =  url.replaceAll(DOMAIN_NAME_REGEX_EXPR, "");
        String path = uri.getPath();

        BasicClientCookie cookie = new BasicClientCookie(DS_LANGUAGE, language);
        cookie.setDomain(domainName);
        cookie.setPath(path);

        cookieStore.addCookie(cookie);
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setThreadsRenew(int threadsRenew) {
        this.threadsRenew = threadsRenew;
    }

    public void setTimeoutRenew(int timeoutRenew) {
        this.timeoutRenew = timeoutRenew;
    }
}
