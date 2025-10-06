/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.unpaywall.service.impl;


import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.io.IOUtils.copy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.app.client.DSpaceHttpClientFactory;
import org.dspace.identifier.DOI;
import org.dspace.identifier.doi.DOIIdentifierException;
import org.dspace.identifier.service.DOIService;
import org.dspace.service.impl.HttpConnectionPoolService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.unpaywall.service.UnpaywallClientAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link UnpaywallClientAPI}
 */
public class UnpaywallClientAPIImpl implements UnpaywallClientAPI {

    private static final String LOCATION_HEADER = "Location";
    private static final String REFERER_HEADER = "Referer";
    public static final String URL = "url";
    private final Logger logger = LoggerFactory.getLogger(UnpaywallClientAPIImpl.class);
    private final CloseableHttpClient client;

    private final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance()
                             .getConfigurationService();

    private final DOIService doiService = DSpaceServicesFactory.getInstance().getServiceManager()
                                                               .getServicesByType(DOIService.class).get(0);

    public UnpaywallClientAPIImpl(HttpConnectionPoolService connectionPoolService) {
        client = connectionPoolService.getClient();
    }

    @Override
    public File downloadResource(String pdfUrl) throws IOException {
        try (CloseableHttpClient client = DSpaceHttpClientFactory.getInstance().build()) {
            HttpGet httpGet = buildHttpGetRequest(pdfUrl);
            HttpResponse response = executeHttpCall(client, pdfUrl, httpGet);
            return tempFile(response);
        }
    }

    private static HttpGet buildHttpGetRequest(String pdfUrl) {
        HttpGet httpGet = new HttpGet(pdfUrl);
        httpGet.addHeader("Accept", "audio/*, video/*, image/*, text/*, */*");
        return httpGet;
    }

    private static File tempFile(HttpResponse response) throws IOException {
        File file = File.createTempFile("unpaywall", "download");
        try (
            InputStream inputStream = response.getEntity().getContent();
            OutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return file;
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
        String normDoi;
        try {
            normDoi = doiService.formatIdentifier(doi).substring(DOI.SCHEME.length());
        } catch (DOIIdentifierException | IllegalArgumentException e) {
            logger.warn("cannot use {} to lookup in unpaywall", doi);
            return empty();
        }
        String email = getEmail();
        HttpGet method = null;

        try {
            URIBuilder uriBuilder = new URIBuilder(endpoint + normDoi);
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