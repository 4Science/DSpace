/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.script;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.authority.service.ItemSearcherMapper;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.ItemServiceImpl;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.kernel.ServiceManager;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link DSpaceRunnable} to update stale item references.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateItemReference
        extends DSpaceRunnable<UpdateItemReferenceScriptConfiguration<UpdateItemReference>> {

    private static final String AUTHORITY = AuthorityValueService.REFERENCE + "%";

    private static final Logger log = LogManager.getLogger(UpdateItemReference.class);

    private Context context;
    private Boolean onlyArchived;

    private ItemService itemService;
    private ItemSearcherMapper itemSearcherMapper;
    private ConfigurationService configurationService;
    private ChoiceAuthorityService choiceAuthorityService;

    @Override
    public void setup() throws ParseException {
        context = new Context();
        ServiceManager serviceManager = new DSpace().getServiceManager();
        itemSearcherMapper = new DSpace().getSingletonService(ItemSearcherMapper.class);
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        choiceAuthorityService = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
        itemService = serviceManager.getServiceByName(ItemServiceImpl.class.getName(), ItemServiceImpl.class);
        onlyArchived = commandLine.hasOption("a") ? null : true;
    }

    @Override
    public void internalRun() throws Exception {
        try {
            int countItems = 0;
            context.turnOffAuthorisationSystem();
            List<String> referencesResolved = new LinkedList<String>();
            List<String> referencesNotResolved = new LinkedList<String>();
            Iterator<Item> itemIterator = itemService.findByLikeAuthorityValue(context, AUTHORITY, onlyArchived);
            handler.logInfo("Script start");
            while (itemIterator.hasNext()) {
                Item item = itemIterator.next();
                countItems ++;
                resolveReferences(item, referencesResolved, referencesNotResolved);
            }
            context.commit();
            handler.logInfo("Have been processed " + countItems + " items");
            handler.logInfo("Have been resolved " + referencesResolved.size() + " references");
            referencesResolved.stream().forEach((m) -> handler.logInfo(m));
            handler.logInfo("Have not been resolved " + referencesNotResolved.size() + " references");
            referencesNotResolved.stream().forEach((m) -> handler.logInfo(m));
            handler.logInfo("Script end");
        } catch (SQLException e) {
            context.rollback();
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private void resolveReferences(Item item, List<String> referencesResolved, List<String> referencesNotResolved)
            throws SQLException, AuthorizeException {
        List<MetadataValue> metadataValues = item.getMetadata();
        for (MetadataValue metadata : metadataValues) {

            String authority = metadata.getAuthority();
            if (isAuthorityAlreadySet(authority) || StringUtils.isBlank(authority)) {
                continue;
            }

            String fieldKey = getFieldKey(metadata);
            String entityType = choiceAuthorityService.getLinkedEntityType(fieldKey);
            String [] providerAndId = getProviderAndId(authority);

            if (Objects.nonNull(providerAndId)) {
                Item searchedItem = itemSearcherMapper.search(context, providerAndId[1], providerAndId[2]);
                if (Objects.nonNull(searchedItem)) {
                    String searchedItemEntityType = itemService.getMetadataFirstValue(searchedItem,
                            "dspace", "entity", "type", Item.ANY);
                    if (StringUtils.equals(entityType, searchedItemEntityType)) {
                        setReferences(metadata, searchedItem, checkWhetherTitleNeedsToBeSet());
                        referencesResolved.add("The starting item with uuid: " + item.getID() + " and reference value "
                                + providerAndId[1] + ":" + providerAndId[2] + " was resolved for item with uuid: "
                                + searchedItem.getID());
                    } else {
                        referencesNotResolved.add("The item with uuid: " + item.getID() + " and reference value: "
                                + authority + " on metadata " + fieldKey
                                + " was not risolved, because the linked EntityType and EntityType of referenced item("
                                + searchedItem.getID() + ") are different (" + entityType + ", "
                                + searchedItemEntityType + ")");
                    }
                    context.uncacheEntity(searchedItem);
                } else {
                    referencesNotResolved.add("The item with uuid: " + item.getID() + " and reference value: "
                            + authority + " because item with " + providerAndId[1] + ":" + providerAndId[2]
                            + " does not found on database");
                }
            } else {
                referencesNotResolved.add("The item with uuid: " + item.getID() + " and reference value: " + authority
                        + " has not been solved!");
            }
        }
        context.uncacheEntity(item);
    }

    private String getFieldKey(MetadataValue metadata) {
        return metadata.getMetadataField().toString('_');
    }

    private boolean checkWhetherTitleNeedsToBeSet() {
        return configurationService.getBooleanProperty("cris.item-reference-resolution.override-metadata-value", false);
    }

    private void setReferences(MetadataValue metadataValue, Item item, boolean isValueToUpdate)
            throws SQLException, AuthorizeException {
        metadataValue.setAuthority(item.getID().toString());
        metadataValue.setConfidence(Choices.CF_ACCEPTED);
        String newMetadataValue = itemService.getMetadata(item, "dc.title");
        if (isValueToUpdate && StringUtils.isNotBlank(newMetadataValue)) {
            metadataValue.setValue(newMetadataValue);
        }
        itemService.update(context, item);
    }

    private String[] getProviderAndId(String authority) {
        String [] array = authority.split("::");
        return array.length == 3 ? array : null;
    }

    private boolean isAuthorityAlreadySet(String authority) {
        return isNotBlank(authority) && !isReferenceAuthority(authority);
    }

    private boolean isReferenceAuthority(String authority) {
        return StringUtils.startsWith(authority, AuthorityValueService.REFERENCE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public UpdateItemReferenceScriptConfiguration<UpdateItemReference> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("update-item-references",
                UpdateItemReferenceScriptConfiguration.class);
    }

}