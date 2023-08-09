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
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.kernel.ServiceManager;
import org.dspace.scripts.DSpaceRunnable;
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

    private ChoiceAuthorityService choiceAuthorityService;

    @Override
    public void setup() throws ParseException {
        context = new Context();
        ServiceManager serviceManager = new DSpace().getServiceManager();
        itemSearcherMapper = new DSpace().getSingletonService(ItemSearcherMapper.class);
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

        for (MetadataValue metadata : item.getMetadata()) {

            String authority = metadata.getAuthority();
            if (isAuthorityAlreadySet(authority) || StringUtils.isBlank(authority)) {
                continue;
            }

            String fieldKey = getFieldKey(metadata);
            String entityType = choiceAuthorityService.getLinkedEntityType(fieldKey);

            String [] providerAndId = getProviderAndId(authority);

            if (providerAndId == null) {
                referencesNotResolved.add(getAuthorityNotResolvedMessage(item, authority));
                continue;
            }

            Item searchedItem = itemSearcherMapper.search(context, providerAndId[1], providerAndId[2], item);
            if (searchedItem == null) {
                referencesNotResolved.add(getItemNotFoundMessage(item, authority, providerAndId));
                continue;
            }

            String searchedItemEntityType = itemService.getEntityType(searchedItem);

            if (StringUtils.equals(entityType, searchedItemEntityType)) {
                choiceAuthorityService.setReferenceWithAuthority(metadata, searchedItem);
                referencesResolved.add(getReferenceResolvedMessage(item, providerAndId, searchedItem));
            } else {
                referencesNotResolved.add(getReferenceNotResolvedForDifferentEntityTypeMessage(item, authority,
                    fieldKey, entityType, searchedItem, searchedItemEntityType));
            }

            context.uncacheEntity(searchedItem);

        }
        context.uncacheEntity(item);

    }

    private String getAuthorityNotResolvedMessage(Item item, String authority) {
        return "The item with uuid: " + item.getID() + " and reference value: " + authority + " has not been solved!";
    }

    private String getItemNotFoundMessage(Item item, String authority, String[] providerAndId) {
        return "The item with uuid: " + item.getID() + " and reference value: "
            + authority + " because item with " + providerAndId[1] + ":" + providerAndId[2]
            + " does not found on database";
    }

    private String getReferenceResolvedMessage(Item item, String[] providerAndId, Item searchedItem) {
        return "The starting item with uuid: " + item.getID() + " and reference value "
            + providerAndId[1] + ":" + providerAndId[2] + " was resolved for item with uuid: "
            + searchedItem.getID();
    }

    private String getReferenceNotResolvedForDifferentEntityTypeMessage(Item item, String authority, String fieldKey,
        String entityType, Item searchedItem, String searchedItemEntityType) {
        return "The item with uuid: " + item.getID() + " and reference value: "
            + authority + " on metadata " + fieldKey
            + " was not resolved, because the linked EntityType and EntityType of referenced item("
            + searchedItem.getID() + ") are different (" + entityType + ", "
            + searchedItemEntityType + ")";
    }

    private String getFieldKey(MetadataValue metadata) {
        return metadata.getMetadataField().toString('_');
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