/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.client;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.util.List;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory of {@link HttpClient} with common configurations.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class DSpaceHttpClientFactory {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private DSpaceProxyRoutePlanner proxyRoutePlanner;

    @Autowired(required = false)
    private List<HttpRequestInterceptor> requestInterceptors;

    @Autowired(required = false)
    private List<HttpResponseInterceptor> responseInterceptors;

    private final RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();

    /**
     * Get an instance of {@link DSpaceHttpClientFactory} from the Spring context.
     * @return the bean instance
     */
    public static DSpaceHttpClientFactory getInstance() {
        return new DSpace().getSingletonService(DSpaceHttpClientFactory.class);
    }

    /**
     * Build an instance of {@link HttpClient} setting the proxy if configured.
     *
     * @return the client
     */
    public CloseableHttpClient build() {
        return build(create(requestConfig), true);
    }

    /**
     * return a Builder if an instance of {@link HttpClient} pre-setting the proxy if configured.
     *
     * @return the client
     */
    public HttpClientBuilder builder(boolean setProxy) {
        return configureBuilder(create(requestConfig), setProxy);
    }

    /**
     * Build an instance of {@link HttpClient} without setting the proxy, even if
     * configured.
     *
     * @return the client
     */
    public CloseableHttpClient buildWithoutProxy() {
        return build(create(requestConfig), false);
    }

    /**
     * Build an instance of {@link HttpClient} setting the proxy if configured,
     * disabling automatic retries and setting the maximum total connection.
     *
     * @param  maxConnTotal the maximum total connection value
     * @return              the client
     */
    public CloseableHttpClient buildWithoutAutomaticRetries(int maxConnTotal) {
        HttpClientBuilder clientBuilder = create(requestConfig)
                .disableAutomaticRetries()
                .setMaxConnTotal(maxConnTotal);
        return build(clientBuilder, true);
    }

    protected HttpClientBuilder create(RequestConfig rq) {
        return HttpClientBuilder.create().setDefaultRequestConfig(rq);
    }

    /**
     * Build an instance of {@link HttpClient} setting the proxy if configured with
     * the given request configuration.
     * @param  requestConfig the request configuration
     * @return               the client
     */
    public CloseableHttpClient buildWithRequestConfig(RequestConfig requestConfig) {
        String cookieSpec = requestConfig.getCookieSpec();
        RequestConfig rq = requestConfig;
        if (!CookieSpecs.STANDARD.equals(cookieSpec)) {
            rq = RequestConfig.copy(requestConfig).setCookieSpec(CookieSpecs.STANDARD).build();
        }
        return build(create(rq), true);
    }

    private CloseableHttpClient build(HttpClientBuilder clientBuilder, boolean setProxy) {
        return configureBuilder(clientBuilder, setProxy).build();
    }

    protected HttpClientBuilder configureBuilder(HttpClientBuilder clientBuilder, boolean setProxy) {
        if (setProxy) {
            clientBuilder.setRoutePlanner(proxyRoutePlanner);
        }
        getRequestInterceptors().forEach(clientBuilder::addInterceptorLast);
        getResponseInterceptors().forEach(clientBuilder::addInterceptorLast);
        return clientBuilder;
    }

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public List<HttpRequestInterceptor> getRequestInterceptors() {
        return emptyIfNull(requestInterceptors);
    }

    public void setRequestInterceptors(List<HttpRequestInterceptor> requestInterceptors) {
        this.requestInterceptors = requestInterceptors;
    }

    public List<HttpResponseInterceptor> getResponseInterceptors() {
        return emptyIfNull(responseInterceptors);
    }

    public void setResponseInterceptors(List<HttpResponseInterceptor> responseInterceptors) {
        this.responseInterceptors = responseInterceptors;
    }

    public DSpaceProxyRoutePlanner getProxyRoutePlanner() {
        return proxyRoutePlanner;
    }

    public void setProxyRoutePlanner(DSpaceProxyRoutePlanner proxyRoutePlanner) {
        this.proxyRoutePlanner = proxyRoutePlanner;
    }
}