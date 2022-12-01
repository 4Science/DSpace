/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.csl;

import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link CSLGenerator} that uses an external web service to
 * generate citations.
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CSLWebServiceGenerator implements CSLGenerator {

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public String generate(DSpaceListItemDataProvider itemDataProvider, String style, String format) {

        if (!isWebServiceAvailable()) {
            throw new IllegalStateException("The web service to generate citations is not available");
        }

        HttpUriRequest postRequest = composePostRequest(itemDataProvider.toJson(), style, format);

        HttpResponse response = sendRequest(postRequest);

        if (isNotSuccessfull(response)) {
            String errorMessage = "An error occurs generating CSL citations using external web service:";
            throw new RuntimeException(errorMessage + formatErrorMessage(response));
        }

        return getContent(response);
    }

    private HttpUriRequest composePostRequest(String requestBody, String style, String format) {
        return RequestBuilder.post(getWebServiceUrl())
            .addParameter("responseformat", "html")
            .addParameter("style", removeCslSuffix(style))
            .addParameter("outputformat", format)
            .setEntity(new StringEntity(requestBody, APPLICATION_JSON))
            .build();
    }

    private HttpResponse sendRequest(HttpUriRequest request) {
        try {
            return HttpClientBuilder.create().build().execute(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isNotSuccessfull(HttpResponse response) {
        int statusCode = getStatusCode(response);
        return statusCode < 200 || statusCode > 299;
    }

    private int getStatusCode(HttpResponse response) {
        return response.getStatusLine().getStatusCode();
    }

    private String getContent(HttpResponse response) {
        try {
            HttpEntity entity = response.getEntity();
            return entity != null ? IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8.name()) : null;
        } catch (UnsupportedOperationException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String formatErrorMessage(HttpResponse response) {
        return "Status " + getStatusCode(response) + " - " + getContent(response);
    }

    public boolean isWebServiceAvailable() {
        return StringUtils.isNotBlank(getWebServiceUrl());
    }

    private String getWebServiceUrl() {
        return configurationService.getProperty("csl.web-service.url");
    }

    private String removeCslSuffix(String style) {
        return StringUtils.removeEnd(style, ".csl");
    }

}
