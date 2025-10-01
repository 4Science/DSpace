/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.marcxml2item;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.marcxml2item.model.ItemsImportMapping;
import org.dspace.app.marcxml2item.parser.MarcXmlParser;
import org.dspace.app.marcxml2item.parser.MarcXmlParserImpl;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.CollectionServiceImpl;
import org.dspace.content.Item;
import org.dspace.content.ItemServiceImpl;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.kernel.ServiceManager;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Script to import items from XML file.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 **/
public class XmlToItemImportScript extends DSpaceRunnable<XmlToItemImportScriptConfiguration<XmlToItemImportScript>> {

    private static final Logger log = LogManager.getLogger(XmlToItemImportScript.class);

    public static final String XML_TO_ITEM_SCRIPT_NAME = "import-xml-to-item";

    private static final String XML_MAPPING_PATH = "/config/crosswalks/epfl/epfl-items-mapping-for-xml-import.xml";
    private static final String DSPACE_DIR_PROPERTY_NAME = "dspace.dir";
    private static final String ITEMS_XPATH = "//record";

    private String xmlFile;
    private String collectionUuid;

    private Context context;
    private Collection collection;
    private MarcXmlParser xmlParser;
    private ItemService itemService;
    private AuthorizeService authorizeService;
    private CollectionService collectionService;
    private InstallItemService installItemService;
    private ConfigurationService configurationService;
    private WorkspaceItemService workspaceItemService;
    private MetadataFieldService metadataFieldService;
    private MetadataSchemaService metadataSchemaService;

    @Override
    public void setup() throws ParseException {
        ServiceManager sm = new DSpace().getServiceManager();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
        metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
        metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
        installItemService = ContentServiceFactory.getInstance().getInstallItemService();
        itemService = sm.getServiceByName(ItemServiceImpl.class.getName(), ItemServiceImpl.class);
        collectionService = sm.getServiceByName(CollectionServiceImpl.class.getName(), CollectionServiceImpl.class);
        xmlParser = sm.getServiceByName("org.dspace.app.marcxml2item.parser.MarcXmlParserImpl", MarcXmlParserImpl.class);

        xmlFile = commandLine.getOptionValue('f');
        collectionUuid = commandLine.getOptionValue('c');
    }

    @Override
    public void internalRun() throws Exception {
        assignCurrentUserInContext();
        assignSpecialGroupsInContext();
        getCollection();
        try {
            context.turnOffAuthorisationSystem();
            importItemsFromXML();
            context.complete();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handler.handleException(e);
            context.abort();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private void importItemsFromXML() {
        List<List<MetadataValueDTO>> parsedItemsFields = parseXmlFromFile();
        handler.logInfo("XML is parsed");

        for (List<MetadataValueDTO> parsedItemField : parsedItemsFields) {
            handler.logInfo("Start creating an item");
            addItemFromMetadata(parsedItemField);
        }
        handler.logInfo("All Items are added");
    }

    private void addItemFromMetadata(List<MetadataValueDTO> parsedItemField) {
        WorkspaceItem workspaceItem = createWorkspaceItem();
        Item item = workspaceItem.getItem();
        handler.logInfo("WorkspaceItem is created");

        for (MetadataValueDTO field : parsedItemField) {
            addMetadata(field, item);
        }
        handler.logInfo("All metadata is added");

        addItemToCollection(item);
        depositItem(workspaceItem);
        handler.logInfo("Item is created");
    }

    private WorkspaceItem createWorkspaceItem() {
        try {
            return workspaceItemService.create(context, collection, false);
        } catch (AuthorizeException | SQLException e) {
            handler.logInfo("ERROR: workspaceItem creation failed");
            throw new RuntimeException(e);
        }
    }

    private void addItemToCollection(Item item) {
        try {
            item.setOwningCollection(collection);
            collectionService.addItem(context, collection, item);
        } catch (SQLException | AuthorizeException e) {
            handler.logInfo("ERROR: adding item to collection failed");
            throw new RuntimeException(e);
        }
    }

    private void depositItem(WorkspaceItem workspaceItem) {
        try {
            installItemService.installItem(context, workspaceItem);
        } catch (SQLException | AuthorizeException e) {
            handler.logInfo("ERROR: deposit item to collection failed");
            throw new RuntimeException(e);
        }
    }

    private void addMetadata(MetadataValueDTO metadataValue, Item item) {
        String schema = metadataValue.getSchema();
        String element = metadataValue.getElement();
        String qualifier = metadataValue.getQualifier();
        try {
            MetadataSchema metadataSchema = metadataSchemaService.find(context, schema);
            if (metadataSchema == null) {
                metadataSchemaService.create(context, schema, schema);
            }
            if (metadataFieldService.findByElement(context, schema, element, qualifier) == null) {
                metadataFieldService.create(context, metadataSchema, element, qualifier, null);
                handler.logInfo("metadataFiled " + metadataValue.getMetadataField() + " is created");
            }
            itemService.addMetadata(context, item, schema, element, qualifier,
                    metadataValue.getLanguage(), metadataValue.getValue(),
                    metadataValue.getAuthority(), metadataValue.getConfidence());
            handler.logInfo(metadataValue + " is added");
        } catch (SQLException | AuthorizeException | NonUniqueMetadataException e) {
            handler.logInfo("ERROR: adding metadata to item failed");
            throw new RuntimeException(e);
        }
    }

    private List<List<MetadataValueDTO>> parseXmlFromFile() {
        InputStream inputStream;
        try {
            inputStream = handler.getFileStream(context, xmlFile).orElseThrow(() -> new IllegalArgumentException(
                                            "Error reading file, the file couldn't be found for filename: " + xmlFile));
        } catch (IOException | AuthorizeException e) {
            handler.logInfo("ERROR: xml parsing failed");
            throw new RuntimeException(e);
        }

        var dspaceDir = configurationService.getProperty(DSPACE_DIR_PROPERTY_NAME);
        ItemsImportMapping itemsImportMapping = xmlParser.parseMapping(dspaceDir + XML_MAPPING_PATH);
        return xmlParser.readItems(context, inputStream, itemsImportMapping, ITEMS_XPATH);
    }

    private void getCollection() {
        UUID collectionUUID = UUID.fromString(collectionUuid);
        try {
            collection = collectionService.find(context, collectionUUID);
            if (Objects.isNull(collection)) {
                throw new RuntimeException("Collection with uuid" + collectionUUID + "does not exist!");
            }
            if (!authorizeService.isAdmin(context, collection)) {
                throw new RuntimeException("User " + context.getCurrentUser().getEmail()
                        + " cannot submit to collection " + collection.getID());
            }
        } catch (SQLException e) {
            handler.logInfo("ERROR: failed to get collection");
            throw new RuntimeException(e);
        }
    }

    private void assignCurrentUserInContext() throws SQLException {
        this.context = new Context();
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    private void assignSpecialGroupsInContext() {
        for (UUID uuid : handler.getSpecialGroups()) {
            context.setSpecialGroup(uuid);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public XmlToItemImportScriptConfiguration<XmlToItemImportScript> getScriptConfiguration() {
        ServiceManager serviceManager = new DSpace().getServiceManager();
        return serviceManager.getServiceByName(XML_TO_ITEM_SCRIPT_NAME, XmlToItemImportScriptConfiguration.class);
    }

}
