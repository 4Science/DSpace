/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sitemap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.SolrSearchCore;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Command-line utility for generating HTML and Sitemaps.org protocol Sitemaps.
 *
 * @author Robert Tansley
 * @author Stuart Lewis
 */
public class GenerateSitemaps {
    /**
     * Logger
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(GenerateSitemaps.class);

    private static final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    private static final SearchService searchService = SearchUtils.getSearchService();
    private static final GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    private static final int PAGE_SIZE = 1000;

    /**
     * Default constructor
     */
    private GenerateSitemaps() { }

    public static void main(String[] args) throws Exception {
        final String usage = GenerateSitemaps.class.getCanonicalName();

        CommandLineParser parser = new DefaultParser();
        HelpFormatter hf = new HelpFormatter();

        Options options = new Options();

        options.addOption("h", "help", false, "help");
        options.addOption("s", "no_sitemaps", false,
                          "do not generate sitemaps.org protocol sitemap");
        options.addOption("b", "no_htmlmap", false,
                          "do not generate a basic HTML sitemap");
        options.addOption("a", "ping_all", false,
                          "ping configured search engines");
        options
            .addOption("p", "ping", true,
                       "ping specified search engine URL");
        options
            .addOption("d", "delete", false,
                "delete sitemaps dir and its contents");

        CommandLine line = null;

        try {
            line = parser.parse(options, args);
        } catch (ParseException pe) {
            hf.printHelp(usage, options);
            System.exit(1);
        }

        if (line.hasOption('h')) {
            hf.printHelp(usage, options);
            System.exit(0);
        }

        if (line.getArgs().length != 0) {
            hf.printHelp(usage, options);
            System.exit(1);
        }

        /*
         * Sanity check -- if no sitemap generation or pinging to do, or deletion, print usage
         */
        if (line.getArgs().length != 0 || line.hasOption('d') || line.hasOption('b')
            && line.hasOption('s') && !line.hasOption('g')
            && !line.hasOption('m') && !line.hasOption('y')
            && !line.hasOption('p')) {
            System.err
                .println("Nothing to do (no sitemap to generate, no search engines to ping)");
            hf.printHelp(usage, options);
            System.exit(1);
        }

        // Note the negation (CLI options indicate NOT to generate a sitemap)
        if (!line.hasOption('b') || !line.hasOption('s')) {
            generateSitemaps(!line.hasOption('b'), !line.hasOption('s'));
        }

        if (line.hasOption('d')) {
            deleteSitemaps();
        }

        if (line.hasOption('a')) {
            pingConfiguredSearchEngines();
        }

        if (line.hasOption('p')) {
            try {
                pingSearchEngine(line.getOptionValue('p'));
            } catch (MalformedURLException me) {
                System.err
                    .println("Bad search engine URL (include all except sitemap URL)");
                System.exit(1);
            }
        }

        System.exit(0);
    }

    /**
     * Runs generate-sitemaps without any params for the scheduler (task-scheduler.xml).
     *
     * @throws SQLException if a database error occurs.
     * @throws IOException  if IO error occurs.
     */
    public static void generateSitemapsScheduled() throws IOException, SQLException {
        generateSitemaps(true, true);
    }

    /**
     * Delete the sitemaps directory and its contents if it exists
     * @throws IOException  if IO error occurs
     */
    public static void deleteSitemaps() throws IOException {
        File outputDir = new File(configurationService.getProperty("sitemap.dir"));
        if (!outputDir.exists() && !outputDir.isDirectory()) {
            log.error("Unable to delete sitemaps directory, doesn't exist or isn't a directort");
        } else {
            FileUtils.deleteDirectory(outputDir);
        }
    }

    /**
     * Generate sitemap.org protocol and/or basic HTML sitemaps.
     *
     * @param makeHTMLMap    if {@code true}, generate an HTML sitemap.
     * @param makeSitemapOrg if {@code true}, generate an sitemap.org sitemap.
     * @throws SQLException if database error
     *                      if a database error occurs.
     * @throws IOException  if IO error
     *                      if IO error occurs.
     */
    public static void generateSitemaps(boolean makeHTMLMap, boolean makeSitemapOrg) throws SQLException, IOException {
        String uiURLStem = configurationService.getProperty("dspace.ui.url");
        String sitemapStem = uiURLStem + "/sitemap";

        File outputDir = new File(configurationService.getProperty("sitemap.dir"));
        if (!outputDir.exists() && !outputDir.mkdir()) {
            log.error("Unable to create output directory");
        }

        AbstractGenerator html = null;
        AbstractGenerator sitemapsOrg = null;

        if (makeHTMLMap) {
            html = new HTMLSitemapGenerator(outputDir, sitemapStem, ".html");
        }

        if (makeSitemapOrg) {
            sitemapsOrg = new SitemapsOrgGenerator(outputDir, sitemapStem, ".xml");
        }

        Context c = new Context(Context.Mode.READ_ONLY);
        SolrSearchCore solrSearchCore = searchService.getSolrSearchCore();
        SolrClient solr = solrSearchCore.getSolr();
        Group anonymousGroup = groupService.findByName(c, Group.ANONYMOUS);
        String anonGroupId = "";
        if (anonymousGroup != null) {
            anonGroupId = anonymousGroup.getID().toString();
        }

        try {
            SolrQuery solrQuery = new SolrQuery(SearchUtils.RESOURCE_TYPE_FIELD + ":Community");
            solrQuery.addFilterQuery("read:g" + anonGroupId);
            solrQuery.setFields(SearchUtils.RESOURCE_ID_FIELD);
            solrQuery.setRows(PAGE_SIZE);
            int offset = 0;
            long commsCount = 0;
            QueryResponse rsp;
            do {
                solrQuery.setStart(offset);
                rsp = solr.query(solrQuery, solrSearchCore.REQUEST_METHOD);
                SolrDocumentList docs = rsp.getResults();
                commsCount = docs.getNumFound();
                Iterator iter = docs.iterator();

                while (iter.hasNext()) {
                    SolrDocument doc = (SolrDocument) iter.next();
                    String url = uiURLStem + "/communities/" + doc.getFieldValue(SearchUtils.RESOURCE_ID_FIELD);

                    if (makeHTMLMap) {
                        html.addURL(url, null);
                    }
                    if (makeSitemapOrg) {
                        sitemapsOrg.addURL(url, null);
                    }
                }
                offset += PAGE_SIZE;
            } while (offset < commsCount);

            solrQuery = new SolrQuery(SearchUtils.RESOURCE_TYPE_FIELD + ":Collection");
            solrQuery.addFilterQuery("read:g" + anonGroupId);
            solrQuery.setFields(SearchUtils.RESOURCE_ID_FIELD);
            solrQuery.setRows(PAGE_SIZE);
            offset = 0;
            long collsCount = 0;
            do {
                solrQuery.setStart(offset);
                rsp = solr.query(solrQuery, solrSearchCore.REQUEST_METHOD);
                SolrDocumentList docs = rsp.getResults();
                collsCount = docs.getNumFound();
                Iterator iter = docs.iterator();

                while (iter.hasNext()) {
                    SolrDocument doc = (SolrDocument) iter.next();
                    String url = uiURLStem + "/collections/" + doc.getFieldValue(SearchUtils.RESOURCE_ID_FIELD);

                    if (makeHTMLMap) {
                        html.addURL(url, null);
                    }
                    if (makeSitemapOrg) {
                        sitemapsOrg.addURL(url, null);
                    }
                }
                offset += PAGE_SIZE;
            } while (offset < collsCount);

            solrQuery = new SolrQuery(SearchUtils.RESOURCE_TYPE_FIELD + ":Item");
            solrQuery.setFields(SearchUtils.RESOURCE_ID_FIELD, "customurl", "search.entitytype");
            solrQuery.addFilterQuery("read:g" + anonGroupId);
            solrQuery.addFilterQuery("-discoverable:false");
            solrQuery.setRows(PAGE_SIZE);
            offset = 0;
            long itemsCount = 0;
            do {
                solrQuery.setStart(offset);
                rsp = solr.query(solrQuery, solrSearchCore.REQUEST_METHOD);
                SolrDocumentList docs = rsp.getResults();
                itemsCount = docs.getNumFound();
                Iterator iter = docs.iterator();

                while (iter.hasNext()) {
                    SolrDocument doc = (SolrDocument) iter.next();
                    String uuid = (String) doc.getFirstValue(SearchUtils.RESOURCE_ID_FIELD);
                    String entityType  = (String) doc.getFirstValue("search.entitytype");
                    String customUrl  = (String) doc.getFirstValue("customUrl");
                    String url = uiURLStem + "/items/" + uuid;

                    if (StringUtils.isNotBlank(customUrl)) {
                        url = uiURLStem + "/entities/" + StringUtils.lowerCase(entityType) + "/" + customUrl;
                    } else if (StringUtils.isNoneBlank(entityType)) {
                        url = uiURLStem + "/entities/" + StringUtils.lowerCase(entityType) + "/" + uuid;
                    }
                    if (makeHTMLMap) {
                        html.addURL(url, null);
                    }
                    if (makeSitemapOrg) {
                        sitemapsOrg.addURL(url, null);
                    }

                }
                offset += PAGE_SIZE;
            } while (offset < itemsCount);

            if (makeHTMLMap) {
                int files = html.finish();
                log.info(LogHelper.getHeader(c, "write_sitemap",
                                              "type=html,num_files=" + files + ",communities="
                                                  + commsCount + ",collections=" + collsCount
                                                  + ",items=" + itemsCount));
            }

            if (makeSitemapOrg) {
                int files = sitemapsOrg.finish();
                log.info(LogHelper.getHeader(c, "write_sitemap",
                                              "type=html,num_files=" + files + ",communities="
                                                  + commsCount + ",collections=" + collsCount
                                                  + ",items=" + itemsCount));
            }
        } catch (SolrServerException e) {
            throw new RuntimeException(e);
        } finally {
            c.abort();
        }
    }

    /**
     * Ping all search engines configured in {@code dspace.cfg}.
     *
     * @throws UnsupportedEncodingException theoretically should never happen
     */
    public static void pingConfiguredSearchEngines()
        throws UnsupportedEncodingException {
        String[] engineURLs = configurationService
            .getArrayProperty("sitemap.engineurls");

        if (ArrayUtils.isEmpty(engineURLs)) {
            log.warn("No search engine URLs configured to ping");
            return;
        }

        for (int i = 0; i < engineURLs.length; i++) {
            try {
                pingSearchEngine(engineURLs[i]);
            } catch (MalformedURLException me) {
                log.warn("Bad search engine URL in configuration: "
                             + engineURLs[i]);
            }
        }
    }

    /**
     * Ping the given search engine.
     *
     * @param engineURL Search engine URL minus protocol etc, e.g.
     *                  {@code www.google.com}
     * @throws MalformedURLException        if the passed in URL is malformed
     * @throws UnsupportedEncodingException theoretically should never happen
     */
    public static void pingSearchEngine(String engineURL)
        throws MalformedURLException, UnsupportedEncodingException {
        // Set up HTTP proxy
        if ((StringUtils.isNotBlank(configurationService.getProperty("http.proxy.host")))
            && (StringUtils.isNotBlank(configurationService.getProperty("http.proxy.port")))) {
            System.setProperty("proxySet", "true");
            System.setProperty("proxyHost", configurationService
                .getProperty("http.proxy.host"));
            System.getProperty("proxyPort", configurationService
                .getProperty("http.proxy.port"));
        }

        String sitemapURL = configurationService.getProperty("dspace.ui.url")
            + "/sitemap";

        URL url = new URL(engineURL + URLEncoder.encode(sitemapURL, "UTF-8"));

        try {
            HttpURLConnection connection = (HttpURLConnection) url
                .openConnection();

            BufferedReader in = new BufferedReader(new InputStreamReader(
                connection.getInputStream()));

            String inputLine;
            StringBuffer resp = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                resp.append(inputLine).append("\n");
            }
            in.close();

            if (connection.getResponseCode() == 200) {
                log.info("Pinged " + url.toString() + " successfully");
            } else {
                log.warn("Error response pinging " + url.toString() + ":\n"
                             + resp);
            }
        } catch (IOException e) {
            log.warn("Error pinging " + url.toString(), e);
        }
    }
}
