/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.solr;

import java.io.IOException;
import java.util.Optional;

import org.apache.http.client.config.CookieSpecs;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.HttpClientBuilderFactory;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * This is a simple HttpClientBuilderFactory that sets the cookie-policy for all solr-related
 * clients.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class DSpaceSolrHttpClientBuilderFactory implements HttpClientBuilderFactory {


    private static final Logger log = LoggerFactory.getLogger(DSpaceSolrHttpClientBuilderFactory.class);

    @Override
    public SolrHttpClientBuilder getHttpClientBuilder(Optional<SolrHttpClientBuilder> builder) {
        return builder.isPresent() ? getBuilder(builder.get()) : getBuilder();
    }

    public SolrHttpClientBuilder getBuilder() {
        return getBuilder(HttpClientUtil.getHttpClientBuilder());
    }

    public SolrHttpClientBuilder getBuilder(SolrHttpClientBuilder builder) {
        log.info("Creating custom HttpClientBuilderFactory to handle cookie policies.");
        // We need to override the default cookie-spec of all the Core instances..
        HttpClientUtil.setCookiePolicy(CookieSpecs.STANDARD);
        return builder;
    }

    @Override
    public void setup(Http2SolrClient client) {
        HttpClientBuilderFactory.super.setup(client);
    }

    @Override
    public void close() throws IOException {
        HttpClientUtil.clearRequestInterceptors();
    }

}
