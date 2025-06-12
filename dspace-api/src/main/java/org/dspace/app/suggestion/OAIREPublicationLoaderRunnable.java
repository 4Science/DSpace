/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.suggestion.oaire.OAIREPublicationLoader;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResultItemIterator;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runner responsible to import metadata about authors from OpenAIRE to Solr.
 * This runner works in two ways:
 * If -s parameter with a valid UUID is received, then the specific researcher
 * with this UUID will be used.
 * Invocation without any parameter results in massive import, processing all
 * authors registered in DSpace.
 */

public class OAIREPublicationLoaderRunnable
    extends DSpaceRunnable<OAIREPublicationLoaderScriptConfiguration<OAIREPublicationLoaderRunnable>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAIREPublicationLoaderRunnable.class);

    private OAIREPublicationLoader oairePublicationLoader = null;

    protected Context context;

    protected String profile;

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public OAIREPublicationLoaderScriptConfiguration<OAIREPublicationLoaderRunnable> getScriptConfiguration() {
        OAIREPublicationLoaderScriptConfiguration configuration = new DSpace().getServiceManager()
                .getServiceByName("import-oaire-suggestions", OAIREPublicationLoaderScriptConfiguration.class);
        return configuration;
    }

    @Override
    public void setup() throws ParseException {

        oairePublicationLoader = new DSpace().getServiceManager().getServiceByName(
                "OAIREPublicationLoader", OAIREPublicationLoader.class);

        profile = commandLine.getOptionValue("s");
        if (profile == null) {
            LOGGER.info("No argument for -s, process all profile");
        } else {
            LOGGER.info("Process eperson item with UUID " + profile);
        }
    }

    @Override
    public void internalRun() throws Exception {

        context = new Context();

        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        if (StringUtils.isNotBlank(profile)) {
            discoverQuery.setQuery("search.resourceid:" + profile);
        }
        discoverQuery.setSortField("lastModified", DiscoverQuery.SORT_ORDER.asc);
        discoverQuery.addFilterQueries("search.resourcetype:Item");
        discoverQuery.addFilterQueries("dspace.entity.type:Person");

        DiscoverResultItemIterator iterator = new DiscoverResultItemIterator(context, discoverQuery);
        while (iterator.hasNext()) {
            oairePublicationLoader.importRecords(context, iterator.next(), null);
        }

        context.complete();
    }

}
