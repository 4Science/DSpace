/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import static org.apache.solr.client.solrj.impl.HttpClientUtil.SYS_PROP_HTTP_CLIENT_BUILDER_FACTORY;

import jakarta.inject.Named;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.dspace.service.impl.HttpConnectionPoolService;
import org.dspace.solr.DSpaceSolrHttpClientBuilderFactory;
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

    static {
        System.setProperty(SYS_PROP_HTTP_CLIENT_BUILDER_FACTORY, DSpaceSolrHttpClientBuilderFactory.class.getName());
    }

    @Override
    public SolrClient getClient(String coreUrl) {
        return new HttpSolrClient.Builder()
            .withBaseSolrUrl(coreUrl)
            .withHttpClient(httpConnectionPoolService.getClient())
            .build();
    }
}
