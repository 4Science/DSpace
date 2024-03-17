/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.unpaywall.service.impl;


import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.io.IOUtils.copy;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.unpaywall.service.UnpaywallClientAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnpaywallClientAPIImpl implements UnpaywallClientAPI {

    private static final String LOCATION_HEADER = "Location";
    private static final String REFERER_HEADER = "Referer";
    public static final String BEST_OA_LOCATION = "best_oa_location";
    public static final String URL_FOR_PDF = "url_for_pdf";
    public static final String URL = "url";
    private final Logger logger = LoggerFactory.getLogger(UnpaywallClientAPIImpl.class);
    private final CloseableHttpClient client;
    private final long downloadTimeout;
    public static final String UNPAYWALL_DOWNLOAD_TIMEOUT = "unpaywall.download.timeout";
    public static final Long DEFAULT_UNPAYWALL_DOWNLOAD_TIMEOUT = 60L;

    private final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance()
                             .getConfigurationService();

    private int timeout;

    public UnpaywallClientAPIImpl() {
        HttpClientBuilder custom = HttpClients.custom();
        client = custom.disableAutomaticRetries().setMaxConnTotal(5)
                .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(timeout).build()).build();
        downloadTimeout = this.configurationService.getLongProperty(UNPAYWALL_DOWNLOAD_TIMEOUT,
                DEFAULT_UNPAYWALL_DOWNLOAD_TIMEOUT);
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public InputStream downloadResource(String pdfUrl) throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder.create()
                .setConnectionTimeToLive(downloadTimeout, TimeUnit.SECONDS).build()) {
            HttpGet httpGet = new HttpGet(pdfUrl);
            httpGet.addHeader("Accept", "audio/*, video/*, image/*, text/*");
            HttpResponse response = executeHttpCall(client, pdfUrl, httpGet);
            return new BufferedInputStream(response.getEntity().getContent());
        }
    }

    private static HttpResponse executeHttpCall(HttpClient client, String pdfUrl, HttpGet httpGet) throws IOException {
        HttpResponse response = client.execute(httpGet);

        // If request returns 301, then get new url from headers and repeat
        while (response.getStatusLine().getStatusCode() == 301) {
            httpGet = new HttpGet(response.getFirstHeader(LOCATION_HEADER).getValue());
            httpGet.addHeader("Accept", "audio/*, video/*, image/*, text/*");
            response = executeHttpCall(client, pdfUrl, httpGet);
        }

        // If request returns 400, then try to get resource using referer
        if (response.getStatusLine().getStatusCode() == 400) {
            if (pdfUrl.equals(httpGet.getFirstHeader(REFERER_HEADER))) {
                throw new RuntimeException("Cannot retrieve the unpaywall resource");
            }
            httpGet.addHeader(REFERER_HEADER, pdfUrl);
            response = executeHttpCall(client, pdfUrl, httpGet);
        }

        if (response.getStatusLine().getStatusCode() == 403) {
            throw new RuntimeException("Unable to download file, forbidden access");
        }
        return response;
    }

    @Override
    public Optional<String> callUnpaywallApi(String doi) {
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
                    logger.error("Http call failed: " + statusLine);
                    throw new RuntimeException("Http call failed: " + statusLine);
            }
        } catch (URISyntaxException | IOException e) {
            logger.error("Cannot fetch unpaywall", e);
            throw new RuntimeException("Cannot fetch unpaywall", e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    private String getEmail() {
        String email = configurationService.getProperty("unpaywall.email");
        if (StringUtils.isBlank(email)) {
            logger.error("\"unpaywall.email\" property cannot be empty.");
            throw new RuntimeException("\"unpaywall.email\" property cannot be empty.");
        }
        return email;
    }
}
