/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.client;

import static org.apache.http.client.methods.RequestBuilder.delete;
import static org.apache.http.client.methods.RequestBuilder.get;
import static org.apache.http.client.methods.RequestBuilder.post;
import static org.apache.http.client.methods.RequestBuilder.put;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.dspace.app.client.DSpaceHttpClientFactory;
import org.dspace.app.util.XMLUtils;
import org.dspace.orcid.OrcidToken;
import org.dspace.orcid.exception.OrcidClientException;
import org.dspace.orcid.model.OrcidEntityType;
import org.dspace.orcid.model.OrcidProfileSectionType;
import org.dspace.orcid.model.OrcidTokenResponseDTO;
import org.dspace.util.ThrowingSupplier;
import org.orcid.jaxb.model.v3.release.record.Address;
import org.orcid.jaxb.model.v3.release.record.Education;
import org.orcid.jaxb.model.v3.release.record.Employment;
import org.orcid.jaxb.model.v3.release.record.Funding;
import org.orcid.jaxb.model.v3.release.record.Keyword;
import org.orcid.jaxb.model.v3.release.record.OtherName;
import org.orcid.jaxb.model.v3.release.record.Person;
import org.orcid.jaxb.model.v3.release.record.PersonExternalIdentifier;
import org.orcid.jaxb.model.v3.release.record.Qualification;
import org.orcid.jaxb.model.v3.release.record.Record;
import org.orcid.jaxb.model.v3.release.record.ResearcherUrl;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.WorkBulk;
import org.orcid.jaxb.model.v3.release.record.summary.Works;
import org.orcid.jaxb.model.v3.release.search.expanded.ExpandedSearch;

/**
 * Implementation of {@link OrcidClient}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidClientImpl implements OrcidClient {

    /**
     * Mapping between ORCID JAXB models and the sub-paths on ORCID API.
     */
    private static final Map<Class<?>, String> PATHS_MAP = initializePathsMap();

    private final OrcidConfiguration orcidConfiguration;

    private final ObjectMapper objectMapper;

    public OrcidClientImpl(OrcidConfiguration orcidConfiguration) {
        this.orcidConfiguration = orcidConfiguration;
        this.objectMapper = new ObjectMapper();
    }

    private static Map<Class<?>, String> initializePathsMap() {
        Map<Class<?>, String> map = new HashMap<Class<?>, String>();
        map.put(Work.class, OrcidEntityType.PUBLICATION.getPath());
        map.put(Funding.class, OrcidEntityType.FUNDING.getPath());
        map.put(Employment.class, OrcidProfileSectionType.AFFILIATION.getPath());
        map.put(Education.class, OrcidProfileSectionType.EDUCATION.getPath());
        map.put(Qualification.class, OrcidProfileSectionType.QUALIFICATION.getPath());
        map.put(Address.class, OrcidProfileSectionType.COUNTRY.getPath());
        map.put(OtherName.class, OrcidProfileSectionType.OTHER_NAMES.getPath());
        map.put(ResearcherUrl.class, OrcidProfileSectionType.RESEARCHER_URLS.getPath());
        map.put(PersonExternalIdentifier.class, OrcidProfileSectionType.EXTERNAL_IDS.getPath());
        map.put(Keyword.class, OrcidProfileSectionType.KEYWORDS.getPath());
        return map;
    }

    @Override
    public OrcidTokenResponseDTO getAccessToken(String code) {

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("code", code));
        params.add(new BasicNameValuePair("grant_type", "authorization_code"));
        params.add(new BasicNameValuePair("client_id", orcidConfiguration.getClientId()));
        params.add(new BasicNameValuePair("client_secret", orcidConfiguration.getClientSecret()));

        HttpUriRequest httpUriRequest = RequestBuilder.post(orcidConfiguration.getTokenEndpointUrl())
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Accept", "application/json")
            .setEntity(new UrlEncodedFormEntity(params, Charset.defaultCharset()))
            .build();

        return executeAndParseJson(httpUriRequest, OrcidTokenResponseDTO.class);

    }

    @Override
    public OrcidTokenResponseDTO getWebhookAccessToken() {
        return getClientCredentialsAccessToken("/webhook");
    }

    @Override
    public Person getPerson(String accessToken, String orcid) {
        HttpUriRequest httpUriRequest = buildGetUriRequest(accessToken, "/" + orcid + "/person");
        return executeAndUnmarshall(httpUriRequest, false, Person.class);
    }

    @Override
    public Record getRecord(String accessToken, String orcid) {
        HttpUriRequest httpUriRequest = buildGetUriRequest(accessToken, "/" + orcid + "/record");
        return executeAndUnmarshall(httpUriRequest, false, Record.class);
    }

    @Override
    public Works getWorks(String accessToken, String orcid) {
        HttpUriRequest httpUriRequest = buildGetUriRequest(accessToken, "/" + orcid + "/works");
        Works works = executeAndUnmarshall(httpUriRequest, true, Works.class);
        return works != null ? works : new Works();
    }

    @Override
    public Works getWorks(String orcid) {
        HttpUriRequest httpUriRequest = buildGetUriRequestToPublicEndpoint("/" + orcid + "/works");
        Works works = executeAndUnmarshall(httpUriRequest, true, Works.class);
        return works != null ? works : new Works();
    }

    @Override
    public WorkBulk getWorkBulk(String accessToken, String orcid, List<String> putCodes) {
        String putCode = String.join(",", putCodes);
        HttpUriRequest httpUriRequest = buildGetUriRequest(accessToken, "/" + orcid + "/works/" + putCode);
        WorkBulk workBulk = executeAndUnmarshall(httpUriRequest, true, WorkBulk.class);
        return workBulk != null ? workBulk : new WorkBulk();
    }

    @Override
    public WorkBulk getWorkBulk(String orcid, List<String> putCodes) {
        String putCode = String.join(",", putCodes);
        HttpUriRequest httpUriRequest = buildGetUriRequestToPublicEndpoint("/" + orcid + "/works/" + putCode);
        WorkBulk workBulk = executeAndUnmarshall(httpUriRequest, true, WorkBulk.class);
        return workBulk != null ? workBulk : new WorkBulk();
    }

    @Override
    public <T> Optional<T> getObject(String accessToken, String orcid, String putCode, Class<T> clazz) {
        String path = getOrcidPathFromOrcidObjectType(clazz);
        HttpUriRequest httpUriRequest = buildGetUriRequest(accessToken, "/" + orcid + path + "/" + putCode);
        return Optional.ofNullable(executeAndUnmarshall(httpUriRequest, true, clazz));
    }

    @Override
    public <T> Optional<T> getObject(String orcid, String putCode, Class<T> clazz) {
        String path = getOrcidPathFromOrcidObjectType(clazz);
        HttpUriRequest httpUriRequest = buildGetUriRequestToPublicEndpoint("/" + orcid + path + "/" + putCode);
        return Optional.ofNullable(executeAndUnmarshall(httpUriRequest, true, clazz));
    }

    @Override
    public OrcidResponse push(String accessToken, String orcid, Object object) {
        String path = getOrcidPathFromOrcidObjectType(object.getClass());
        return execute(buildPostUriRequest(accessToken, "/" + orcid + path, object), false);
    }

    @Override
    public OrcidResponse update(String accessToken, String orcid, Object object, String putCode) {
        String path = getOrcidPathFromOrcidObjectType(object.getClass());
        return execute(buildPutUriRequest(accessToken, "/" + orcid + path + "/" + putCode, object), false);
    }

    @Override
    public OrcidResponse deleteByPutCode(String accessToken, String orcid, String putCode, String path) {
        return execute(buildDeleteUriRequest(accessToken, "/" + orcid + path + "/" + putCode), true);
    }

    @Override
    public OrcidResponse registerWebhook(String accessToken, String orcid, String url) {
        String webhookUrl = orcidConfiguration.getWebhookUrl();
        String encodedUrl = encodeUrl(url);
        return execute(buildPutUriRequest(accessToken, webhookUrl, "/" + orcid + "/webhook/" + encodedUrl), false);
    }

    @Override
    public OrcidResponse unregisterWebhook(String accessToken, String orcid, String url) {
        String webhookUrl = orcidConfiguration.getWebhookUrl();
        String encodedUrl = encodeUrl(url);
        return execute(buildDeleteUriRequest(accessToken, webhookUrl, "/" + orcid + "/webhook/" + encodedUrl), true);
    }

    @Override
    public ExpandedSearch expandedSearch(String accessToken, String query, int start, int rows) {
        String queryParams = formatExpandedSearchParameters(query, start, rows);
        HttpUriRequest httpUriRequest = buildGetUriRequest(accessToken, "/expanded-search" + queryParams);
        return executeAndUnmarshall(httpUriRequest, false, ExpandedSearch.class);
    }

    @Override
    public ExpandedSearch expandedSearch(String query, int start, int rows) {
        String queryParams = formatExpandedSearchParameters(query, start, rows);
        HttpUriRequest httpUriRequest = buildGetUriRequestToPublicEndpoint("/expanded-search" + queryParams);
        return executeAndUnmarshall(httpUriRequest, false, ExpandedSearch.class);
    }

    @Override
    public void revokeToken(OrcidToken orcidToken) {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("client_id", orcidConfiguration.getClientId()));
        params.add(new BasicNameValuePair("client_secret", orcidConfiguration.getClientSecret()));
        params.add(new BasicNameValuePair("token", orcidToken.getAccessToken()));

        executeSuccessful(buildPostForRevokeToken(new UrlEncodedFormEntity(params, Charset.defaultCharset())));
    }

    @Override
    public OrcidTokenResponseDTO getReadPublicAccessToken() {
        return getClientCredentialsAccessToken("/read-public");
    }

    private OrcidTokenResponseDTO getClientCredentialsAccessToken(String scope) {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("scope", scope));
        params.add(new BasicNameValuePair("grant_type", "client_credentials"));
        params.add(new BasicNameValuePair("client_id", orcidConfiguration.getClientId()));
        params.add(new BasicNameValuePair("client_secret", orcidConfiguration.getClientSecret()));

        HttpUriRequest httpUriRequest = RequestBuilder.post(orcidConfiguration.getTokenEndpointUrl())
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Accept", "application/json")
            .setEntity(new UrlEncodedFormEntity(params, Charset.defaultCharset()))
            .build();

        return executeAndParseJson(httpUriRequest, OrcidTokenResponseDTO.class);
    }

    private HttpUriRequest buildGetUriRequest(String accessToken, String relativePath) {
        return get(orcidConfiguration.getApiUrl() + relativePath.trim())
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Authorization", "Bearer " + accessToken)
            .build();
    }

    private HttpUriRequest buildGetUriRequestToPublicEndpoint(String relativePath) {
        return get(orcidConfiguration.getPublicUrl() + relativePath.trim())
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build();
    }

    private HttpUriRequest buildPostUriRequest(String accessToken, String relativePath, Object object) {
        return post(orcidConfiguration.getApiUrl() + relativePath.trim())
            .addHeader("Content-Type", "application/vnd.orcid+xml")
            .addHeader("Authorization", "Bearer " + accessToken)
            .setEntity(convertToEntity(object))
            .build();
    }

    private HttpUriRequest buildPostForRevokeToken(HttpEntity entity) {
        return post(orcidConfiguration.getRevokeUrl())
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .setEntity(entity)
            .build();
    }

    private HttpUriRequest buildPutUriRequest(String accessToken, String relativePath, Object object) {
        return put(orcidConfiguration.getApiUrl() + relativePath.trim())
            .addHeader("Content-Type", "application/vnd.orcid+xml")
            .addHeader("Authorization", "Bearer " + accessToken)
            .setEntity(convertToEntity(object))
            .build();
    }

    private HttpUriRequest buildPutUriRequest(String accessToken, String baseUrl, String relativePath) {
        return put(baseUrl + relativePath.trim())
            .addHeader("Content-Type", "application/vnd.orcid+xml")
            .addHeader("Authorization", "Bearer " + accessToken)
            .build();
    }

    private HttpUriRequest buildDeleteUriRequest(String accessToken, String relativePath) {
        return delete(orcidConfiguration.getApiUrl() + relativePath.trim())
            .addHeader("Authorization", "Bearer " + accessToken)
            .build();
    }

    private HttpUriRequest buildDeleteUriRequest(String accessToken, String baseUrl, String relativePath) {
        return delete(baseUrl + relativePath.trim())
            .addHeader("Authorization", "Bearer " + accessToken)
            .build();
    }

    private String formatExpandedSearchParameters(String query, int start, int rows) {
        return String.format("?q=%s&start=%s&rows=%s", query, start, rows);
    }

    private void executeSuccessful(HttpUriRequest httpUriRequest) {
        try (CloseableHttpClient client = DSpaceHttpClientFactory.getInstance().build()) {
            CloseableHttpResponse response = client.execute(httpUriRequest);
            if (isNotSuccessfull(response)) {
                throw new OrcidClientException(
                    getStatusCode(response),
                    "Operation " + httpUriRequest.getMethod() + " for the resource " + httpUriRequest.getURI() +
                    " was not successful: " + new String(response.getEntity().getContent().readAllBytes(),
                                                         StandardCharsets.UTF_8)
                );
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T executeAndParseJson(HttpUriRequest httpUriRequest, Class<T> clazz) {
        try (CloseableHttpClient client = DSpaceHttpClientFactory.getInstance().build()) {
            return executeAndReturns(() -> {
                CloseableHttpResponse response = client.execute(httpUriRequest);
                if (isNotSuccessfull(response)) {
                    throw new OrcidClientException(getStatusCode(response), formatErrorMessage(response));
                }
                return objectMapper.readValue(response.getEntity().getContent(), clazz);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Execute the given httpUriRequest, unmarshalling the content with the given
     * class.
     * @param  httpUriRequest       the http request to be executed
     * @param  handleNotFoundAsNull if true this method returns null if the response
     *                              status is 404, if false throws an
     *                              OrcidClientException
     * @param  clazz                the class to be used for the content unmarshall
     * @return                      the response body
     * @throws OrcidClientException if the incoming response is not successfull
     */
    private <T> T executeAndUnmarshall(HttpUriRequest httpUriRequest, boolean handleNotFoundAsNull, Class<T> clazz) {
        try (CloseableHttpClient client = DSpaceHttpClientFactory.getInstance().build()) {
            return executeAndReturns(() -> {
                CloseableHttpResponse response = client.execute(httpUriRequest);
                if (handleNotFoundAsNull && isNotFound(response)) {
                    return null;
                }
                if (isNotSuccessfull(response)) {
                    throw new OrcidClientException(getStatusCode(response), formatErrorMessage(response));
                }
                return unmarshall(response.getEntity(), clazz);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private OrcidResponse execute(HttpUriRequest httpUriRequest, boolean handleNotFoundAsNull) {
        try (CloseableHttpClient client = DSpaceHttpClientFactory.getInstance().build()) {
            return executeAndReturns(() -> {
                CloseableHttpResponse response = client.execute(httpUriRequest);
                if (handleNotFoundAsNull && isNotFound(response)) {
                    return new OrcidResponse(getStatusCode(response), null, getContent(response));
                }
                if (isNotSuccessfull(response)) {
                    throw new OrcidClientException(getStatusCode(response), formatErrorMessage(response));
                }
                return new OrcidResponse(getStatusCode(response), getPutCode(response), getContent(response));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T executeAndReturns(ThrowingSupplier<T, Exception> supplier) {
        try {
            return supplier.get();
        } catch (OrcidClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OrcidClientException(ex);
        }
    }

    private String marshall(Object object) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(object.getClass());
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(object, stringWriter);
        return stringWriter.toString();
    }

    @SuppressWarnings("unchecked")
    private <T> T unmarshall(HttpEntity entity, Class<T> clazz) throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
        XMLInputFactory xmlInputFactory = XMLUtils.getXMLInputFactory();
        XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(entity.getContent());
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return (T) unmarshaller.unmarshal(xmlStreamReader);
    }

    private HttpEntity convertToEntity(Object object) {
        try {
            return new StringEntity(marshall(object), StandardCharsets.UTF_8);
        } catch (JAXBException ex) {
            throw new IllegalArgumentException("The given object cannot be sent to ORCID", ex);
        }
    }

    private String formatErrorMessage(HttpResponse response) {
        try {
            return IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
        } catch (UnsupportedOperationException | IOException e) {
            return "Generic error";
        }
    }

    private boolean isNotSuccessfull(HttpResponse response) {
        int statusCode = getStatusCode(response);
        return statusCode < 200 || statusCode > 299;
    }

    private boolean isNotFound(HttpResponse response) {
        return getStatusCode(response) == HttpStatus.SC_NOT_FOUND;
    }

    private int getStatusCode(HttpResponse response) {
        return response.getStatusLine().getStatusCode();
    }

    private String getOrcidPathFromOrcidObjectType(Class<?> clazz) {
        String path = PATHS_MAP.get(clazz);
        if (path == null) {
            throw new IllegalArgumentException("The given class is not a supported ORCID object's class: " + clazz);
        }
        return path;
    }

    private String getContent(HttpResponse response) throws UnsupportedOperationException, IOException {
        HttpEntity entity = response.getEntity();
        return entity != null ? IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8.name()) : null;
    }

    /**
     * Returns the put code present in the given http response, if any. For more
     * details about the put code see For more details see
     * https://info.orcid.org/faq/what-is-a-put-code/
     * @param  response the http response coming from ORCID
     * @return          the put code, if any
     */
    private String getPutCode(HttpResponse response) {
        Header[] headers = response.getHeaders("Location");
        if (headers.length == 0) {
            return null;
        }
        String value = headers[0].getValue();
        return value.substring(value.lastIndexOf("/") + 1);
    }

    private String encodeUrl(String url) {
        try {
            return URLEncoder.encode(url, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new OrcidClientException(e);
        }
    }

}
