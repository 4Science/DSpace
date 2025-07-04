package org.dspace.xoai.tests.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;

import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import org.dspace.app.util.factory.UtilServiceFactory;
import org.dspace.app.util.service.MetadataExposureService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xoai.app.XOAIExtensionsPluginFactory;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.util.ItemUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

public class ItemUtilsTest {

    private Context context;
    private Item item;
    private MetadataValue metadataValue;
    private MetadataField metadataField;
    private MetadataSchema metadataSchema;

    @Before
    public void setUp() {
        context = mock(Context.class);
        item = mock(Item.class);
        metadataValue = mock(MetadataValue.class);
        metadataField = mock(MetadataField.class);
        metadataSchema = mock(MetadataSchema.class);

        when(metadataValue.getMetadataField()).thenReturn(metadataField);
        when(metadataSchema.getName()).thenReturn("dc");
        when(metadataField.getMetadataSchema()).thenReturn(metadataSchema);
        when(metadataField.getElement()).thenReturn("title");
        when(metadataField.getQualifier()).thenReturn(null);
        when(metadataValue.getLanguage()).thenReturn(null);
        when(metadataValue.getValue()).thenReturn("Test Title");
        when(metadataValue.getAuthority()).thenReturn(null);
        when(item.getHandle()).thenReturn("123456789/1");
        when(item.getLastModified()).thenReturn(new Date());
    }

    @Test
    public void testRetrieveMetadata() throws SQLException, AuthorizeException, IOException {
        try (
            MockedStatic<DSpaceItem> dspaceItemMockedStatic = mockStatic(DSpaceItem.class);
            MockedStatic<UtilServiceFactory> ultilServiceFactory = mockStatic(UtilServiceFactory.class);
            MockedStatic<ContentServiceFactory> contentServiceFactory = mockStatic(ContentServiceFactory.class);
            MockedStatic<DSpaceServicesFactory> dspaceServicesFactory = mockStatic(DSpaceServicesFactory.class);
            MockedStatic<AuthorizeServiceFactory> authorizeServiceFactory = mockStatic(AuthorizeServiceFactory.class);
            MockedStatic<HandleServiceFactory> handleServiceFactoryMockedStatic = mockStatic(HandleServiceFactory.class);
            MockedStatic<XOAIExtensionsPluginFactory> xoaiExtensionsPluginFactoryMockedStatic = mockStatic(XOAIExtensionsPluginFactory.class);
            MockedStatic<ContentAuthorityServiceFactory> contentAuthorityServiceFactoryMockedStatic = mockStatic(
                ContentAuthorityServiceFactory.class)
        ) {

            XOAIExtensionsPluginFactory xoaiExtensionsPluginFactory = mock(XOAIExtensionsPluginFactory.class);
            xoaiExtensionsPluginFactoryMockedStatic.when(XOAIExtensionsPluginFactory::getInstance)
                                                      .thenReturn(xoaiExtensionsPluginFactory);

            ContentAuthorityServiceFactory contentAuthorityServiceFactory = mock(ContentAuthorityServiceFactory.class);
            contentAuthorityServiceFactoryMockedStatic.when(ContentAuthorityServiceFactory::getInstance)
                                                      .thenReturn(contentAuthorityServiceFactory);

            HandleServiceFactory handleServiceFactory = mock(HandleServiceFactory.class);
            handleServiceFactoryMockedStatic.when(HandleServiceFactory::getInstance).thenReturn(handleServiceFactory);

            AuthorizeServiceFactory authorizeServiceInstance = mock(AuthorizeServiceFactory.class);
            authorizeServiceFactory.when(AuthorizeServiceFactory::getInstance).thenReturn(authorizeServiceInstance);

            UtilServiceFactory utilServiceInstance = mock(UtilServiceFactory.class);
            ultilServiceFactory.when(UtilServiceFactory::getInstance).thenReturn(utilServiceInstance);

            ContentServiceFactory contentServiceInstance = mock(ContentServiceFactory.class);
            contentServiceFactory.when(ContentServiceFactory::getInstance).thenReturn(contentServiceInstance);

            DSpaceServicesFactory dspaceServiceInstance = mock(DSpaceServicesFactory.class);
            dspaceServicesFactory.when(DSpaceServicesFactory::getInstance).thenReturn(dspaceServiceInstance);

            // Mock static services
            ItemService itemService = mock(ItemService.class);
            MetadataExposureService metadataExposureService = mock(MetadataExposureService.class);
            ConfigurationService configurationService = mock(ConfigurationService.class);

            when(contentServiceInstance.getItemService()).thenReturn(itemService);
            when(dspaceServiceInstance.getConfigurationService()).thenReturn(configurationService);
            when(utilServiceInstance.getMetadataExposureService()).thenReturn(metadataExposureService);

            when(itemService.getMetadata(eq(item), any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(metadataValue));
            when(metadataExposureService.isHidden(any(), any(), any(), any())).thenReturn(false);
            when(configurationService.getProperty(any())).thenReturn("test");

            dspaceItemMockedStatic.when(() -> DSpaceItem.buildIdentifier(any())).thenReturn("oai:test:123456789/1");

            Metadata metadata = ItemUtils.retrieveMetadata(context, item);

            assertNotNull(metadata);
            assertFalse(metadata.getElement().isEmpty());
            assertTrue(metadata.getElement().stream().anyMatch(e -> "dc".equals(e.getName())));
            assertTrue(metadata.getElement().stream().anyMatch(e -> "bundles".equals(e.getName())));
            assertTrue(metadata.getElement().stream().anyMatch(e -> "others".equals(e.getName())));
            assertTrue(metadata.getElement().stream().anyMatch(e -> "repository".equals(e.getName())));
            assertTrue(metadata.getElement().stream().anyMatch(e -> "license".equals(e.getName())));
        }
    }
}