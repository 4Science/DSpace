package org.dspace.importer.external.core;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.el.MethodNotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.DoiCheck;
import org.dspace.importer.external.service.components.QuerySource;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;


public class CoreImportMetadataSourceServiceImpl
        extends AbstractImportMetadataSourceService<String>
        implements QuerySource {

    private static final Logger log = LogManager.getLogger(CoreImportMetadataSourceServiceImpl.class);

    @Autowired
    private LiveImportClient liveImportClient;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public String getImportSource() {
        return "core";
    }

    @Override
    public void init() throws Exception {
        // no-op
    }

    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query, start, count));
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query));
    }

    @Override
    public ImportRecord getRecord(String recordId) throws MetadataSourceException {
        List<ImportRecord> records = retry(new GetByIdCallable(recordId));
        return CollectionUtils.isEmpty(records) ? null : records.get(0);
    }

    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        List<ImportRecord> records = retry(new GetByIdCallable(query));
        return CollectionUtils.isEmpty(records) ? null : records.get(0);
    }

    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        return retry(new CountByQueryCallable(query));
    }

    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        return retry(new CountByQueryCallable(query));
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        return retry(new SearchByQueryCallable(query));
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for CORE");
    }

    public String getID(String query) {
        if (StringUtils.isBlank(query)) {
            return StringUtils.EMPTY;
        }
        String q = query;
        if (q.contains("%252F")) {
            q = q.replace("%252F", "/");
        }
        if (DoiCheck.isDoi(q)) {
            return "doi:\"" + q + "\"";
        }
        return StringUtils.EMPTY;
    }

    private int getTimeoutMs() {
        return configurationService.getIntProperty("core.timeout", 180000);
    }

    private int getDefaultPageSize() {
        return configurationService.getIntProperty("core.pageSize", 10);
    }

    private String getBaseUrl() {
        return configurationService.getProperty("core.api.url", "https://api.core.ac.uk/v3");
    }

    private String getWorksSearchPath() {
        return configurationService.getProperty("core.api.search.works", "/search/works");
    }

    private String getWorksByIdPath() {
        return configurationService.getProperty("core.api.works.byId", "/works");
    }

    private String getApiKey() {
        return StringUtils.trimToNull(configurationService.getProperty("core.apiKey"));
    }

    /**
     * Builds the request map expected by LiveImportClient.
     * - uriParameters: querystring params
     * - headers: HTTP headers (Authorization)
     */
    private Map<String, Map<String, String>> buildParams(Map<String, String> uriParameters) {
        Map<String, Map<String, String>> params = new HashMap<>();

        params.put("uriParameters", uriParameters != null ? uriParameters : new HashMap<>());

        Map<String, String> headers = new HashMap<>();
        String apiKey = getApiKey();
        if (apiKey != null) {
            headers.put("Authorization", "Bearer " + apiKey);
        } else {
            log.warn("CORE api key is not configured (property core.apiKey). Requests may fail with 401/403.");
        }
        params.put("headers", headers);

        return params;
    }

    private String buildWorksSearchUrl() {
        String baseUrl = getBaseUrl();
        String searchPath = getWorksSearchPath();

        String url = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = searchPath.startsWith("/") ? searchPath : "/" + searchPath;
        return url + path;
    }

    private String buildWorkByIdUrl(String identifier) {
        String baseUrl = getBaseUrl();
        String worksByIdPath = getWorksByIdPath();

        String url = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        String path = StringUtils.defaultIfBlank(worksByIdPath, "/works");
        path = path.startsWith("/") ? path : "/" + path;

        return url + path + "/" + encodePathSegment(identifier);
    }

    private String encodePathSegment(String segment) {
        try {
            return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            return segment;
        }
    }

    private class SearchByQueryCallable implements Callable<List<ImportRecord>> {

        private final Query query;

        private SearchByQueryCallable(String queryString, Integer start, Integer count) {
            Query q = new Query();
            q.addParameter("query", StringUtils.trimToNull(queryString));
            q.addParameter("start", start);
            q.addParameter("count", count);
            this.query = q;
        }

        private SearchByQueryCallable(Query query) {
            this.query = query != null ? query : new Query();
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            List<ImportRecord> records = new ArrayList<>();

            String raw = query.getParameterAsClass("query", String.class);
            String q = getID(raw);
            if (StringUtils.isBlank(q)) {
                q = StringUtils.trimToNull(raw);
            }
            if (q == null) {
                return records;
            }

            Integer start = query.getParameterAsClass("start", Integer.class);
            Integer count = query.getParameterAsClass("count", Integer.class);

            int offset = (start != null) ? Math.max(start, 0) : 0;
            int limit = (count != null && count > 0) ? count : getDefaultPageSize();

            Map<String, String> uriParameters = new HashMap<>();
            uriParameters.put("q", q);
            uriParameters.put("limit", Integer.toString(limit));
            uriParameters.put("offset", Integer.toString(offset));

            String url = buildWorksSearchUrl();
            String responseString = liveImportClient.executeHttpGetRequest(getTimeoutMs(), url,
                    buildParams(uriParameters));

            JsonNode jsonNode = convertStringJsonToJsonNode(responseString);
            if (jsonNode == null) {
                log.warn("CORE returned invalid JSON");
                return records;
            }

            JsonNode resultsNode = jsonNode.at("/results");
            if (resultsNode != null && resultsNode.isArray()) {
                for (JsonNode workNode : resultsNode) {
                    if (workNode == null || workNode.isMissingNode() || workNode.isNull()) {
                        continue;
                    }
                    records.add(transformSourceRecords(workNode.toString()));
                }
            } else {
                log.debug("CORE: missing /results array in response");
            }

            return records;
        }
    }

    private class GetByIdCallable implements Callable<List<ImportRecord>> {

        private final Query query;

        private GetByIdCallable(String recordId) {
            Query q = new Query();
            q.addParameter("id", StringUtils.trimToNull(recordId));
            this.query = q;
        }

        private GetByIdCallable(Query query) {
            this.query = query != null ? query : new Query();
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            List<ImportRecord> results = new ArrayList<>();

            String id = query.getParameterAsClass("id", String.class);
            id = StringUtils.trimToNull(id);
            if (id == null) {
                return results;
            }

            if (DoiCheck.isDoi(id)) {
                Query q = new Query();
                q.addParameter("query", id);
                q.addParameter("start", 0);
                q.addParameter("count", 10);
                List<ImportRecord> records = new SearchByQueryCallable(q).call();
                if (CollectionUtils.isNotEmpty(records)) {
                    results.add(records.get(0));
                }
                return results;
            }

            String url = buildWorkByIdUrl(id);

            String responseString;
            try {
                responseString = liveImportClient.executeHttpGetRequest(getTimeoutMs(), url,
                        buildParams(new HashMap<>()));
            } catch (RuntimeException e) {
                log.error("CORE getRecord failed for identifier={}", id, e);
                throw new MetadataSourceException("CORE getRecord failed for identifier=" + id, e);
            }

            JsonNode jsonNode = convertStringJsonToJsonNode(responseString);
            if (jsonNode == null || jsonNode.isMissingNode() || jsonNode.isNull()) {
                log.warn("CORE /works/{id} returned invalid JSON for identifier={}", id);
                return results;
            }

            results.add(transformSourceRecords(jsonNode.toString()));
            return results;
        }
    }

    private class CountByQueryCallable implements Callable<Integer> {

        private final Query query;

        private CountByQueryCallable(String queryString) {
            Query q = new Query();
            q.addParameter("query", StringUtils.trimToNull(queryString));
            this.query = q;
        }

        private CountByQueryCallable(Query query) {
            this.query = query != null ? query : new Query();
        }

        @Override
        public Integer call() throws Exception {
            String raw = query.getParameterAsClass("query", String.class);
            String q = getID(raw);
            if (StringUtils.isBlank(q)) {
                q = StringUtils.trimToNull(raw);
            }
            if (q == null) {
                return 0;
            }

            Map<String, String> uriParameters = new HashMap<>();
            uriParameters.put("q", q);
            uriParameters.put("limit", "1");
            uriParameters.put("offset", "0");

            String url = buildWorksSearchUrl();
            String responseString = liveImportClient.executeHttpGetRequest(getTimeoutMs(), url,
                    buildParams(uriParameters));

            JsonNode jsonNode = convertStringJsonToJsonNode(responseString);
            if (jsonNode == null) {
                log.warn("CORE returned invalid JSON");
                throw new MetadataSourceException("Could not read CORE source");
            }

            JsonNode totalNode = jsonNode.at("/totalHits");
            if (totalNode != null && totalNode.isNumber()) {
                return totalNode.asInt();
            }
            if (totalNode != null && totalNode.isTextual()) {
                try {
                    return Integer.parseInt(totalNode.asText());
                } catch (Exception e) {
                    log.debug("Could not parse totalHits: {}", totalNode.asText(), e);
                }
            }
            return 0;
        }
    }

    private JsonNode convertStringJsonToJsonNode(String json) {
        try {
            return new ObjectMapper().readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Unable to process json response.", e);
        }
        return null;
    }
}
