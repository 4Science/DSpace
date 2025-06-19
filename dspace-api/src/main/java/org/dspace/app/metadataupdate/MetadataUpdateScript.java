/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metadataupdate;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResultItemIterator;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.util.SimpleMapConverter;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetadataUpdateScript
    extends DSpaceRunnable<MetadataUpdateScriptConfiguration<MetadataUpdateScript>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataUpdateScript.class);
    private ItemService itemService;
    private SearchService searchService;
    private ConfigurationService configurationService;
    private SimpleMapConverter simpleMapConverter;
    protected Context context;
    protected MetadataFieldName metadata;
    protected String entityType;
    protected String file;

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MetadataUpdateScriptConfiguration<MetadataUpdateScript> getScriptConfiguration() {
        MetadataUpdateScriptConfiguration configuration = new DSpace().getServiceManager()
                .getServiceByName("metadata-update", MetadataUpdateScriptConfiguration.class);
        return configuration;
    }

    @Override
    public void setup() throws ParseException {
        DSpace dSpace = new DSpace();
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.searchService = dSpace.getSingletonService(SearchService.class);
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

        metadata = new MetadataFieldName(commandLine.getOptionValue("m"));
        entityType = commandLine.getOptionValue("en");
        file = commandLine.getOptionValue("f");

        this.simpleMapConverter = new SimpleMapConverter(file, configurationService);
    }

    @Override
    public void internalRun() throws Exception {

        context = new Context(Context.Mode.BATCH_EDIT);
        assignCurrentUserInContext(context);
        assignSpecialGroupsInContext(context);

        try {
            simpleMapConverter.getMapping().forEach((key, value) -> {

                if (!value.isEmpty() && !value.equals(key)) {

                    DiscoverResultItemIterator iterator = findIndexableObjects(key);
                    int count = 0;
                    while (iterator.hasNext()) {
                        Item item = iterator.next();
                        try {
                            Item reloadedItem = context.reloadEntity(item);
                            String currentValue = itemService.getMetadataFirstValue(reloadedItem,
                                metadata.schema, metadata.element, metadata.qualifier, Item.ANY);
                            if (StringUtils.equals(currentValue, key)) {
                                itemService.clearMetadata(context, reloadedItem, metadata.schema,
                                    metadata.element, metadata.qualifier,
                                    Item.ANY);
                                List.of(value.split(Pattern.quote("|")))
                                    .stream()
                                    .map(String::trim)
                                    .forEach(v -> {
                                        try {
                                            itemService.addMetadata(context, reloadedItem, metadata.schema,
                                                metadata.element, metadata.qualifier,
                                                null, v);
                                        } catch (SQLException e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                                itemService.update(context, reloadedItem);
                                context.uncacheEntity(reloadedItem);
                                count++;
                                if (count % 100 == 0) {
                                    context.commit();
                                }
                            }
                        } catch (SQLException | AuthorizeException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

            });

            context.complete();
            handler.logInfo("Metadata " + metadata + " has been updated successfully");
        } catch (Exception e) {
            handler.handleException(e);
            LOGGER.error(e.getMessage(), e);
            context.abort();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private DiscoverResultItemIterator findIndexableObjects(String value) {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.addDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.addFilterQueries("search.entitytype:" + entityType);
        discoverQuery.addFilterQueries("entityType_keyword:" + entityType);
        discoverQuery.addFilterQueries(metadata.toString() + ":\"" +
            searchService.escapeQueryChars(value) + "\"");

        return new DiscoverResultItemIterator(context, discoverQuery);
    }

    protected void assignCurrentUserInContext(Context context) throws ParseException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson;
            try {
                ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            context.setCurrentUser(ePerson);
        }
    }

    private void assignSpecialGroupsInContext(Context context) {
        for (UUID uuid : handler.getSpecialGroups()) {
            context.setSpecialGroup(uuid);
        }
    }

}
