package org.dspace.xoai.tests.util;

import static com.lyncode.xoai.dataprovider.core.Granularity.Second;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.lyncode.xoai.dataprovider.xml.XmlOutputContext;
import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import org.dspace.app.util.factory.UtilServiceFactory;
import org.dspace.app.util.service.MetadataExposureService;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
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
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.util.ItemUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ItemUtilsTest {

    private static MockedStatic<DSpaceItem> dspaceItemMockedStatic;
    private static MockedStatic<UtilServiceFactory> ultilServiceFactory;
    private static MockedStatic<ContentServiceFactory> contentServiceFactory;
    private static MockedStatic<DSpaceServicesFactory> dspaceServicesFactory;
    private static MockedStatic<AuthorizeServiceFactory> authorizeServiceFactory;
    private static MockedStatic<HandleServiceFactory> handleServiceFactoryMockedStatic;
    private static MockedStatic<ContentAuthorityServiceFactory> contentAuthorityServiceFactoryMockedStatic;

    private static ContentAuthorityServiceFactory contentAuthorityServiceFactory;
    private static HandleServiceFactory handleServiceFactory;
    private static AuthorizeServiceFactory authorizeServiceInstance;
    private static UtilServiceFactory utilServiceInstance;
    private static ContentServiceFactory contentServiceInstance;
    private static DSpaceServicesFactory dspaceServiceInstance;
    private static ItemService itemService;
    private static MetadataExposureService metadataExposureService;
    private static ConfigurationService configurationService;
    private static AuthorizeService authorizeService;

    private Context context;
    private Item item;
    private MetadataValue metadataValue;
    private MetadataField metadataField;
    private MetadataSchema metadataSchema;

    @BeforeClass
    public static void setUpClass() {
        dspaceItemMockedStatic = mockStatic(DSpaceItem.class);
        ultilServiceFactory = mockStatic(UtilServiceFactory.class);
        contentServiceFactory = mockStatic(ContentServiceFactory.class);
        dspaceServicesFactory = mockStatic(DSpaceServicesFactory.class);
        authorizeServiceFactory = mockStatic(AuthorizeServiceFactory.class);
        handleServiceFactoryMockedStatic = mockStatic(HandleServiceFactory.class);
        contentAuthorityServiceFactoryMockedStatic = mockStatic(ContentAuthorityServiceFactory.class);

        contentAuthorityServiceFactory = mock(ContentAuthorityServiceFactory.class);;
        handleServiceFactory = mock(HandleServiceFactory.class);
        authorizeServiceInstance = mock(AuthorizeServiceFactory.class);
        utilServiceInstance = mock(UtilServiceFactory.class);
        contentServiceInstance = mock(ContentServiceFactory.class);
        dspaceServiceInstance = mock(DSpaceServicesFactory.class);

        // Mock static services
        itemService = mock(ItemService.class);
        metadataExposureService = mock(MetadataExposureService.class);
        configurationService = mock(ConfigurationService.class);
        authorizeService = mock(AuthorizeService.class);

    }

    @AfterClass
    public static void tearDownClass() {
        dspaceItemMockedStatic.close();
        ultilServiceFactory.close();
        contentServiceFactory.close();
        dspaceServicesFactory.close();
        authorizeServiceFactory.close();
        handleServiceFactoryMockedStatic.close();
        contentAuthorityServiceFactoryMockedStatic.close();
    }

    private static String formatXml(String xml) {
        try {
            Source xmlInput = new StreamSource(new StringReader(xml));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (Exception e) {
            System.err.println("Error formatting XML: " + e.getMessage());
            return xml; // Return original XML if formatting fails
        }
    }

    private static void match(Metadata metadata, String path, String value) {
        String[] split = path.split("\\.");
        int i = 0;
        List<Element> elements = metadata.getElement();

        // Navigate through element hierarchy
        while (i < split.length - 2 && !elements.isEmpty()) {
            String name = split[i++];
            elements = elements
                .stream()
                .filter(e -> name.equals(e.getName()))
                .flatMap(e -> e.getElement().stream())
                .toList();
        }

        // Get the final field name
        String elementName = split[i++];
        String fieldName = split[i];

        // Find the field and assert its value
        boolean found = elements.stream()
                                .filter(e -> Objects.equals(e.getName(), elementName))
                                .flatMap(e -> e.getField().stream())
                                .filter(f -> fieldName.equals(f.getName()))
                                .map(Element.Field::getValue)
                                .anyMatch(fieldValue -> Objects.equals(fieldValue, value));

        assertTrue("Field not found: " + path, found);
    }

    private static void matchElement(Metadata metadata, String path) {
        assertTrue("Element not found: " + path, navigate(metadata, path));
    }

    private static void notMatchElement(Metadata metadata, String path) {
        assertFalse("Element found: " + path, navigate(metadata, path));
    }

    private static boolean navigate(Metadata metadata, String path) {
        String[] split = path.split("\\.");
        int i = 0;
        List<Element> elements = metadata.getElement();

        // Navigate through element hierarchy
        boolean found = false;
        while (i < split.length && !elements.isEmpty()) {
            String name = split[i++];
            if (i == split.length) {
                // This is the final element we're looking for
                found = elements.stream().anyMatch(e -> name.equals(e.getName()));
                break;
            } else {
                // Navigate deeper
                elements = elements
                    .stream()
                    .filter(e -> name.equals(e.getName()))
                    .flatMap(e -> e.getElement().stream())
                    .toList();
            }
        }
        return found;
    }

    private static void printMetadata(Metadata metadata) throws XMLStreamException {
        // Print metadata in XML format using metadata.write() method
        System.out.println("=== METADATA XML OUTPUT ===");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        XmlOutputContext outputContext = XmlOutputContext.emptyContext(output, Second);
        try {
            // First write metadata to the output stream
            metadata.write(outputContext);
            outputContext.getWriter().flush();

            // Get the XML as a string
            String xmlString = output.toString("UTF-8");

            // Format the XML for better readability
            String formattedXml = formatXml(xmlString);

            // Print the formatted XML
            System.out.println(formattedXml);
        } catch (Exception e) {
            System.err.println("Error writing metadata: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                outputContext.getWriter().close();
            } catch (Exception e) {
                // Ignore close exceptions
            }
        }
        System.out.println("=== END METADATA XML OUTPUT ===");
    }

    private static Bitstream mockBitstream() throws SQLException {
        Bitstream bitstream = mock(Bitstream.class);
        BitstreamFormat bitstreamFormat = mock(BitstreamFormat.class);

        when(bitstream.getID()).thenReturn(UUID.randomUUID());
        when(bitstream.getFormat(any())).thenReturn(bitstreamFormat);
        when(bitstream.getChecksum()).thenReturn("checksum");
        when(bitstream.getChecksumAlgorithm()).thenReturn("checksum-algorithm");
        when(bitstream.getSource()).thenReturn("dc.source");
        when(bitstream.getName()).thenReturn("dc.title");
        when(bitstream.getDescription()).thenReturn("bitstream-description");
        when(bitstream.getSequenceID()).thenReturn(1);
        when(bitstreamFormat.getMIMEType()).thenReturn("application/pdf");
        return bitstream;
    }

    private static MetadataValue mockMetadataValue(
        String metadata, String value
    ) {
        MetadataValue metadataValue = mock(MetadataValue.class);
        MetadataField metadataField = mock(MetadataField.class);
        MetadataSchema metadataSchema = mock(MetadataSchema.class);

        when(metadataField.getMetadataSchema()).thenReturn(metadataSchema);

        String[] split = metadata.split("\\.");

        String schemaValue = split[0];
        String elementValue = split[1];
        String qualifierValue = split.length == 3 ? split[2] : null;
        when(metadataSchema.getName()).thenReturn(schemaValue);
        when(metadataField.getElement()).thenReturn(elementValue);
        when(metadataField.getQualifier()).thenReturn(qualifierValue);

        when(metadataValue.getMetadataField()).thenReturn(metadataField);
        when(metadataValue.getSchema()).thenReturn(schemaValue);
        when(metadataValue.getElement()).thenReturn(elementValue);
        when(metadataValue.getQualifier()).thenReturn(qualifierValue);
        when(metadataValue.getValue()).thenReturn(value);
        when(metadataValue.getMetadataField().toString(eq('.'))).thenReturn(metadata);
        when(metadataValue.getMetadataField().toString()).thenReturn(metadata);

        return metadataValue;
    }

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

        contentAuthorityServiceFactoryMockedStatic.when(ContentAuthorityServiceFactory::getInstance)
                                                  .thenReturn(contentAuthorityServiceFactory);
        handleServiceFactoryMockedStatic.when(HandleServiceFactory::getInstance).thenReturn(handleServiceFactory);
        authorizeServiceFactory.when(AuthorizeServiceFactory::getInstance).thenReturn(authorizeServiceInstance);
        ultilServiceFactory.when(UtilServiceFactory::getInstance).thenReturn(utilServiceInstance);
        contentServiceFactory.when(ContentServiceFactory::getInstance).thenReturn(contentServiceInstance);
        dspaceServicesFactory.when(DSpaceServicesFactory::getInstance).thenReturn(dspaceServiceInstance);
    }

    @After
    public void tearDown() {
        contentAuthorityServiceFactoryMockedStatic.clearInvocations();
        handleServiceFactoryMockedStatic.clearInvocations();
        authorizeServiceFactory.clearInvocations();
        ultilServiceFactory.clearInvocations();
        contentServiceFactory.clearInvocations();
        dspaceServicesFactory.clearInvocations();
    }

    @Test
    public void testRetrieveMetadata() throws SQLException, XMLStreamException {
        when(contentServiceInstance.getItemService()).thenReturn(itemService);
        when(dspaceServiceInstance.getConfigurationService()).thenReturn(configurationService);
        when(utilServiceInstance.getMetadataExposureService()).thenReturn(metadataExposureService);

        when(itemService.getMetadata(eq(item), any(), any(), any(), any()))
            .thenReturn(Collections.singletonList(metadataValue));
        when(metadataExposureService.isHidden(any(), any(), any(), any())).thenReturn(false);
        when(configurationService.getProperty(any())).thenReturn("test");

        dspaceItemMockedStatic.when(() -> DSpaceItem.buildIdentifier(any())).thenReturn("oai:test:123456789/1");

        Metadata metadata = ItemUtils.retrieveMetadata(context, item);

        printMetadata(metadata);

        assertNotNull(metadata);
        assertFalse(metadata.getElement().isEmpty());
        assertTrue(metadata.getElement().stream().anyMatch(e -> "dc".equals(e.getName())));
        assertTrue(metadata.getElement().stream().anyMatch(e -> "bundles".equals(e.getName())));
        assertTrue(metadata.getElement().stream().anyMatch(e -> "others".equals(e.getName())));
        assertTrue(metadata.getElement().stream().anyMatch(e -> "repository".equals(e.getName())));
        assertTrue(metadata.getElement().stream().anyMatch(e -> "license".equals(e.getName())));

        match(metadata, "dc.title.none.value", "Test Title");
        match(metadata, "others.handle", "123456789/1");
        match(metadata, "others.identifier", "oai:test:123456789/1");
        match(metadata, "repository.url", "test");
        match(metadata, "repository.name", "test");
        match(metadata, "repository.mail", "test");
    }

    @Test
    public void testRetrieveBitstreamTechnicalmetadata() throws SQLException, XMLStreamException {
        Bundle originalBundle = mock(Bundle.class);
        Bitstream bitstream = mockBitstream();
        MetadataValue dctitle = mockMetadataValue("dc.title", "dc.title");
        MetadataValue iiiftoc = mockMetadataValue("iiif.toc", "Index|||Coperta anteriore|||Controguardia ant.");
        MetadataValue imageheight = mockMetadataValue("image.height", "500");
        MetadataValue imagewidth = mockMetadataValue("image.width", "600");
        when(bitstream.getMetadata()).thenReturn(
            List.of(
                dctitle,
                iiiftoc,
                imageheight,
                imagewidth
            )
        );

        when(originalBundle.getName()).thenReturn("ORIGINAL");
        when(originalBundle.getBitstreams()).thenReturn(List.of(bitstream));

        when(item.getBundles()).thenReturn(List.of(originalBundle));


        when(contentServiceInstance.getItemService()).thenReturn(itemService);
        when(dspaceServiceInstance.getConfigurationService()).thenReturn(configurationService);
        when(utilServiceInstance.getMetadataExposureService()).thenReturn(metadataExposureService);
        when(authorizeServiceInstance.getAuthorizeService()).thenReturn(authorizeService);

        when(itemService.getMetadata(eq(item), any(), any(), any(), any()))
            .thenReturn(Collections.singletonList(metadataValue));
        when(metadataExposureService.isHidden(any(), any(), any(), any())).thenReturn(false);
        when(configurationService.getProperty(any())).thenReturn("test");

        dspaceItemMockedStatic.when(() -> DSpaceItem.buildIdentifier(any())).thenReturn("oai:test:123456789/1");

        Metadata metadata = ItemUtils.retrieveMetadata(context, item);

        printMetadata(metadata);

        assertNotNull(metadata);
        assertFalse(metadata.getElement().isEmpty());
        assertTrue(metadata.getElement().stream().anyMatch(e -> "dc".equals(e.getName())));
        assertTrue(metadata.getElement().stream().anyMatch(e -> "bundles".equals(e.getName())));
        assertTrue(metadata.getElement().stream().anyMatch(e -> "others".equals(e.getName())));
        assertTrue(metadata.getElement().stream().anyMatch(e -> "repository".equals(e.getName())));
        assertTrue(metadata.getElement().stream().anyMatch(e -> "license".equals(e.getName())));


        // Test all top-level elements exist
        matchElement(metadata, "dc");
        matchElement(metadata, "bundles");
        matchElement(metadata, "others");
        matchElement(metadata, "repository");
        matchElement(metadata, "license");

        // Test DC metadata structure
        match(metadata, "dc.title.none.value", "Test Title");

        // Test bundles structure
        matchElement(metadata, "bundles.bundle");
        match(metadata, "bundles.bundle.name", "ORIGINAL");
        matchElement(metadata, "bundles.bundle.bitstreams");
        matchElement(metadata, "bundles.bundle.bitstreams.bitstream");

        // Test bitstream fields
        match(metadata, "bundles.bundle.bitstreams.bitstream.name", "dc.title");
        match(metadata, "bundles.bundle.bitstreams.bitstream.originalName", "dc.title");
        match(metadata, "bundles.bundle.bitstreams.bitstream.description", "bitstream-description");
        match(metadata, "bundles.bundle.bitstreams.bitstream.format", "application/pdf");
        match(metadata, "bundles.bundle.bitstreams.bitstream.size", "0");
        match(metadata, "bundles.bundle.bitstreams.bitstream.checksum", "checksum");
        match(metadata, "bundles.bundle.bitstreams.bitstream.checksumAlgorithm", "checksum-algorithm");
        match(metadata, "bundles.bundle.bitstreams.bitstream.sid", "1");
        match(metadata, "bundles.bundle.bitstreams.bitstream.primary", "true");

        // Test bitstream technical metadata
        matchElement(metadata, "bundles.bundle.bitstreams.bitstream.dc");
        match(metadata, "bundles.bundle.bitstreams.bitstream.dc.title.none.value", "dc.title");

        matchElement(metadata, "bundles.bundle.bitstreams.bitstream.iiif");
        match(metadata, "bundles.bundle.bitstreams.bitstream.iiif.toc.none.value",
              "Index|||Coperta anteriore|||Controguardia ant.");

        matchElement(metadata, "bundles.bundle.bitstreams.bitstream.image");
        match(metadata, "bundles.bundle.bitstreams.bitstream.image.height.none.value", "500");
        match(metadata, "bundles.bundle.bitstreams.bitstream.image.width.none.value", "600");

        // Test others metadata
        match(metadata, "others.handle", "123456789/1");
        match(metadata, "others.identifier", "oai:test:123456789/1");
        // Note: lastModifyDate is dynamic, so we just check it exists
        matchElement(metadata, "others");

        // Test repository metadata
        match(metadata, "repository.url", "test");
        match(metadata, "repository.name", "test");
        match(metadata, "repository.mail", "test");

        // Test license element exists (even if empty)
        matchElement(metadata, "license");
    }


    @Test
    public void testFiltersExcludedBitstreamTechnicalmetadata() throws SQLException, XMLStreamException {
        Bundle originalBundle = mock(Bundle.class);
        Bitstream bitstream = mockBitstream();
        MetadataValue dctitle = mockMetadataValue("dc.title", "dc.title");
        MetadataValue iiiftoc = mockMetadataValue("iiif.toc", "Index|||Coperta anteriore|||Controguardia ant.");
        MetadataValue imageheight = mockMetadataValue("image.height", "500");
        MetadataValue imagewidth = mockMetadataValue("image.width", "600");
        when(bitstream.getMetadata()).thenReturn(
            List.of(
                dctitle,
                iiiftoc,
                imageheight,
                imagewidth
            )
        );

        when(originalBundle.getName()).thenReturn("original");
        when(originalBundle.getBitstreams()).thenReturn(List.of(bitstream));

        when(item.getBundles()).thenReturn(List.of(originalBundle));


        when(contentServiceInstance.getItemService()).thenReturn(itemService);
        when(dspaceServiceInstance.getConfigurationService()).thenReturn(configurationService);
        when(utilServiceInstance.getMetadataExposureService()).thenReturn(metadataExposureService);
        when(authorizeServiceInstance.getAuthorizeService()).thenReturn(authorizeService);

        when(itemService.getMetadata(eq(item), any(), any(), any(), any()))
            .thenReturn(Collections.singletonList(metadataValue));
        when(metadataExposureService.isHidden(any(), any(), any(), any())).thenReturn(false);
        when(configurationService.getProperty(eq("oai.bitstream.baseUrl"))).thenReturn("http://localhost:8080/oai");
        when(configurationService.getProperty(eq(ItemUtils.BITSTREAM_METADATA_EXCLUDED))).thenReturn("dc.title");

        dspaceItemMockedStatic.when(() -> DSpaceItem.buildIdentifier(any())).thenReturn("oai:test:123456789/1");

        Metadata metadata = ItemUtils.retrieveMetadata(context, item);

        printMetadata(metadata);

        assertNotNull(metadata);
        assertFalse(metadata.getElement().isEmpty());
        assertTrue(metadata.getElement().stream().anyMatch(e -> "dc".equals(e.getName())));
        assertTrue(metadata.getElement().stream().anyMatch(e -> "bundles".equals(e.getName())));
        assertTrue(metadata.getElement().stream().anyMatch(e -> "others".equals(e.getName())));
        assertTrue(metadata.getElement().stream().anyMatch(e -> "repository".equals(e.getName())));
        assertTrue(metadata.getElement().stream().anyMatch(e -> "license".equals(e.getName())));

        // Test all top-level elements exist
        matchElement(metadata, "dc");
        matchElement(metadata, "bundles");
        matchElement(metadata, "others");
        matchElement(metadata, "repository");
        matchElement(metadata, "license");

        // Test DC metadata structure
        match(metadata, "dc.title.none.value", "Test Title");

        // Test bundles structure
        matchElement(metadata, "bundles.bundle");
        match(metadata, "bundles.bundle.name", "original");
        matchElement(metadata, "bundles.bundle.bitstreams");
        matchElement(metadata, "bundles.bundle.bitstreams.bitstream");

        // Test bitstream fields
        match(metadata, "bundles.bundle.bitstreams.bitstream.name", "dc.title");
        match(metadata, "bundles.bundle.bitstreams.bitstream.originalName", "dc.title");
        match(metadata, "bundles.bundle.bitstreams.bitstream.description", "bitstream-description");
        match(metadata, "bundles.bundle.bitstreams.bitstream.format", "application/pdf");
        match(metadata, "bundles.bundle.bitstreams.bitstream.size", "0");
        match(metadata, "bundles.bundle.bitstreams.bitstream.checksum", "checksum");
        match(metadata, "bundles.bundle.bitstreams.bitstream.checksumAlgorithm", "checksum-algorithm");
        match(metadata, "bundles.bundle.bitstreams.bitstream.sid", "1");
        match(metadata, "bundles.bundle.bitstreams.bitstream.primary", "false");

        // Test bitstream technical metadata
        notMatchElement(metadata, "bundles.bundle.bitstreams.bitstream.dc");

        matchElement(metadata, "bundles.bundle.bitstreams.bitstream.iiif");
        match(metadata, "bundles.bundle.bitstreams.bitstream.iiif.toc.none.value",
              "Index|||Coperta anteriore|||Controguardia ant.");

        matchElement(metadata, "bundles.bundle.bitstreams.bitstream.image");
        match(metadata, "bundles.bundle.bitstreams.bitstream.image.height.none.value", "500");
        match(metadata, "bundles.bundle.bitstreams.bitstream.image.width.none.value", "600");
    }

}