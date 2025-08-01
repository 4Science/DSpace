/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.Logger;
import org.dspace.app.client.DSpaceHttpClientFactory;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * A basic link checker that is designed to be extended. By default this link checker
 * will check that all links stored in anyschema.anyelement.uri metadata fields return
 * a 20x status code.
 *
 * This link checker can be enhanced by extending this class, and overriding the
 * getURLs and checkURL methods.
 *
 * @author Stuart Lewis
 */

public class BasicLinkChecker extends AbstractCurationTask {

    // The status of the link checking of this item
    private int status = Curator.CURATE_UNSET;

    // The results of link checking this item
    private List<String> results = null;

    // The log4j logger for this class
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(BasicLinkChecker.class);

    protected static final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();


    /**
     * Perform the link checking.
     *
     * @param dso The DSpaaceObject to be checked
     * @return The curation task status of the checking
     * @throws java.io.IOException THrown if something went wrong
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException {
        // The results that we'll return
        StringBuilder results = new StringBuilder();

        // Unless this is  an item, we'll skip this item
        status = Curator.CURATE_SKIP;
        if (dso instanceof Item) {
            Item item = (Item) dso;

            // Get the URLs
            List<String> urls = getURLs(item);

            // Assume skip until we hit a URL to check
            status = Curator.CURATE_SKIP;
            results.append("Item: ").append(getItemHandle(item)).append("\n");

            // Check the URLs
            for (String url : urls) {
                boolean ok = checkURL(url, results);

                if (ok) {
                    status = Curator.CURATE_SUCCESS;
                } else {
                    status = Curator.CURATE_FAIL;
                }
            }
        }

        setResult(results.toString());
        report(results.toString());

        return status;
    }

    /**
     * Get the URLs to check
     *
     * @param item The item to extract URLs from
     * @return An array of URL Strings
     */
    protected List<String> getURLs(Item item) {
        // Get URIs from anyschema.anyelement.uri.*
        List<MetadataValue> urls = itemService.getMetadata(item, Item.ANY, Item.ANY, "uri", Item.ANY);
        ArrayList<String> theURLs = new ArrayList<String>();
        for (MetadataValue url : urls) {
            theURLs.add(url.getValue());
        }
        return theURLs;
    }

    /**
     * Check the URL and perform appropriate reporting
     *
     * @param url     The URL to check
     * @param results Result string with HTTP status codes
     * @return If the URL was OK or not
     */
    protected boolean checkURL(String url, StringBuilder results) {
        // Link check the URL
        int redirects = 0;
        int httpStatus = getResponseStatus(url, redirects);

        if ((httpStatus >= 200) && (httpStatus < 300)) {
            results.append(" - " + url + " = " + httpStatus + " - OK\n");
            return true;
        } else {
            results.append(" - " + url + " = " + httpStatus + " - FAILED\n");
            return false;
        }
    }

    /**
     * Get the response code for a URL.  If something goes wrong opening the URL, a
     * response code of 0 is returned.
     *
     * @param url The url to open
     * @return The HTTP response code (e.g. 200 / 301 / 404 / 500)
     */
    protected int getResponseStatus(String url, int redirects) {
        RequestConfig config = RequestConfig.custom().setRedirectsEnabled(true).build();
        try (CloseableHttpClient httpClient = DSpaceHttpClientFactory.getInstance().buildWithRequestConfig(config)) {
            CloseableHttpResponse httpResponse = httpClient.execute(new HttpGet(url));
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            int maxRedirect = configurationService.getIntProperty("curate.checklinks.max-redirect", 0);
            if ((statusCode == HttpURLConnection.HTTP_MOVED_TEMP || statusCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    statusCode == HttpURLConnection.HTTP_SEE_OTHER)) {
                String newUrl = httpResponse.getFirstHeader("Location").getValue();
                if (newUrl != null && (maxRedirect >= redirects || maxRedirect == -1)) {
                    redirects++;
                    return getResponseStatus(newUrl, redirects);
                }
            }
            return statusCode;
        } catch (IOException ioe) {
            // Must be a bad URL
            log.debug("Bad link: " + ioe.getMessage());
            return 0;
        }
    }

    /**
     * Internal utitity method to get a description of the handle
     *
     * @param item The item to get a description of
     * @return The handle, or in workflow
     */
    protected String getItemHandle(Item item) {
        String handle = item.getHandle();
        return (handle != null) ? handle : " in workflow";
    }

}
