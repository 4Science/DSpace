/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import static org.apache.solr.client.solrj.impl.HttpClientUtil.SYS_PROP_HTTP_CLIENT_BUILDER_FACTORY;

import java.util.Optional;

import jakarta.inject.Named;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.dspace.service.impl.HttpConnectionPoolService;
import org.dspace.services.ConfigurationService;
import org.dspace.solr.DSpaceSolrHttpClientBuilderFactory;
import org.dspace.solr.SolrClientFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory of HtmlSolrClient instances.
 *
 * @author mwood
 */
public class HttpSolrClientFactory
    implements SolrClientFactory {

    @Autowired
    @Named("solrHttpConnectionPoolService")
    protected HttpConnectionPoolService httpConnectionPoolService;

    @Autowired
    protected ConfigurationService configurationService;

    static {
        System.setProperty(SYS_PROP_HTTP_CLIENT_BUILDER_FACTORY, DSpaceSolrHttpClientBuilderFactory.class.getName());
    }

    @Override
    public Optional<SolrClient> getClient(String urlProperty) {
        String solrService = configurationService.getProperty(urlProperty);
        if (solrService == null) {
            return Optional.empty();
        }
        SolrClient client = new HttpSolrClient.Builder()
            .withBaseSolrUrl(solrService)
            .withHttpClient(httpConnectionPoolService.getClient())
            .build();
        return Optional.of(client);
    }
}
