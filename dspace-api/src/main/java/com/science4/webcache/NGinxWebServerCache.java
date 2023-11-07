/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package com.science4.webcache;

import java.io.IOException;
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
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;

public class NGinxWebServerCache extends AbstractWebServerCache {
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(NGinxWebServerCache.class);
    private CloseableHttpClient client;
    private CloseableHttpClient clientRenew;
    private ExecutorService executor;
    private ExecutorService executorRenew;
    private int timeout = 1000;
    private int timeoutRenew = 5000;
    private int threads = 5;
    private int threadsRenew = 1;

    public void initialize () {
        super.initialize();
        HttpClientBuilder custom = HttpClients.custom();
        client = custom.disableAutomaticRetries().setMaxConnTotal(threads)
                .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(timeout).build())
                .build();
        clientRenew = custom.disableAutomaticRetries().setMaxConnTotal(threadsRenew)
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
            CloseableHttpResponse response = clientRenew.execute(new HttpGet(url));
            if (log.isDebugEnabled()) {
                log.debug("Generate new cache response code {}", response.getStatusLine().getStatusCode());
            }
            response.close();
        } catch (IOException e) {
            throw new RuntimeException("Error generating the new cache");
        }
    }

    private void invalidateUrl(String url) {
        try {
            HttpUriRequest httpPurge = RequestBuilder.create("PURGE").setUri(url).build();
            CloseableHttpResponse response = client.execute(httpPurge);
            if (log.isDebugEnabled()) {
                log.debug("Invalidate cache response code {}", response.getStatusLine().getStatusCode());
            }
            response.close();
        } catch (IOException e) {
            throw new RuntimeException("Error invalidating the cache");
        }
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
