/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.ror.client;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.client.methods.RequestBuilder.get;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dspace.ror.ROROrgUnitDTO;
import org.dspace.ror.ROROrgUnitListDTO;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

public class RORApiClientImpl implements RORApiClient {

    public static final int TIMEOUT_MS = 15 * 1000;

    @Autowired
    private ConfigurationService configurationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RequestConfig requestConfig = RequestConfig.custom()
                                                             .setConnectTimeout(TIMEOUT_MS)
                                                             .setConnectionRequestTimeout(TIMEOUT_MS)
                                                             .setSocketTimeout(TIMEOUT_MS)
                                                             .build();

    @Override
    public List<ROROrgUnitDTO> searchOrganizations(String text) {
        RorResponse response = performGetRequest(buildGetWithQueryExact(getRORApiUrl(), text.trim()));

        if (isNotFound(response)) {
            return Collections.emptyList();
        }

        if (isNotSuccessful(response)) {
            String message = "ROR API request was not successful. "
                + "Status: " + response.getStatusCode() + " - Content: " + response.getContent();
            throw new RuntimeException(message);
        }

        ROROrgUnitListDTO orgUnits = parseResponse(response, ROROrgUnitListDTO.class);

        return List.of(orgUnits.getItems());
    }

    @Override
    public Optional<ROROrgUnitDTO> findOrganizationByRORId(String rorId) {
        RorResponse response = performGetRequest(buildGetWithRORId(getRORApiUrl(), rorId));

        if (isNotFound(response)) {
            return Optional.empty();
        }

        if (isNotSuccessful(response)) {
            String message = "ROR API request was not successful. "
                + "Status: " + response.getStatusCode() + " - Content: " + response.getContent();
            throw new RuntimeException(message);
        }

        ROROrgUnitDTO orgUnit = parseResponse(response, ROROrgUnitDTO.class);

        return Optional.ofNullable(orgUnit);
    }

    private RorResponse performGetRequest(HttpUriRequest request) {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            CloseableHttpResponse httpResponse = httpClient.execute(request);
            int statusCode = getStatusCode(httpResponse);
            HttpEntity entity = httpResponse.getEntity();
            return new RorResponse(statusCode, getContent(httpResponse));
//            return httpResponse;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpUriRequest buildGetWithRORId(String url, String rorId) {
        return get(url + "/" + rorId).setConfig(requestConfig).build();
    }

    private HttpUriRequest buildGetWithQuery(String url, String value) {
        return get(url).addParameter("query", value).setConfig(requestConfig).build();
    }

    private HttpUriRequest buildGetWithQueryExact(String url, String value) {
        return get(url).addParameter("query", "\"" + value + "\"").setConfig(requestConfig).build();
    }

    private <T> T parseResponse(RorResponse response, Class<T> clazz) {
        try {
            return objectMapper.readValue(response.getContent(), clazz);
        } catch (UnsupportedOperationException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getContent(HttpResponse response) {
        try {
            HttpEntity entity = response.getEntity();
            return entity != null ? IOUtils.toString(entity.getContent(), UTF_8) : null;
        } catch (UnsupportedOperationException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isNotSuccessful(RorResponse response) {
        int statusCode = response.getStatusCode();
        return statusCode < 200 || statusCode > 299;
    }

    private boolean isNotFound(RorResponse response) {
        return response.getStatusCode() == HttpStatus.SC_NOT_FOUND;
    }

    private int getStatusCode(HttpResponse response) {
        return response.getStatusLine().getStatusCode();
    }

    private String getRORApiUrl() {
        return configurationService.getProperty("ror.orgunit-import.api-url");
    }

    private static class RorResponse {
        private final int statusCode;
        private final String content;

        public RorResponse(int statusCode, String content) {

            this.statusCode = statusCode;
            this.content = content;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getContent() {
            return content;
        }
    }
}
