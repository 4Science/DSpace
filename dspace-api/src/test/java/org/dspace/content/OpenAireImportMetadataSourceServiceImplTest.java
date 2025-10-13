/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractUnitTest;
import org.dspace.app.util.XMLUtils;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.metadatamapping.contributor.MetadataContributor;
import org.dspace.kernel.ServiceManager;
import org.dspace.utils.DSpace;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit Tests for class {OpenAireImportMetadataSourceServiceImpl}
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class OpenAireImportMetadataSourceServiceImplTest extends AbstractUnitTest {

    private ServiceManager serviceManager;

    private static final String OPENAIRE_FILE = "/org/dspace/app/openaire-publications/openaire-publication-record.xml";
    private static final int EXPECTED_METADATA_COUNT = 21;

    @Before
    @Override
    public void init() {
        super.init();
        this.serviceManager = new DSpace().getServiceManager();
    }

    @Test
    public void checkOpenairePublicationsMappingTransformationsTest() throws Exception {
        List<MetadatumDTO> metadataValues = new ArrayList<>();
        Map<MetadataFieldConfig, MetadataContributor> metadataFieldMap = (Map<MetadataFieldConfig, MetadataContributor>)
                               serviceManager.getServiceByName("openairePublicationsMetadataFieldMap", Map.class);

        String openAireSourceXmlString = readDocumentFromResource(OPENAIRE_FILE);
        List<Element> records = splitToRecords(openAireSourceXmlString);
        if (records.isEmpty()) {
            throw new IllegalStateException("No records extracted from OpenAIRE XML");
        }

        for (MetadataFieldConfig key : metadataFieldMap.keySet()) {
            Collection<MetadatumDTO> result = metadataFieldMap.get(key).contributeMetadata(records.get(0));
            if (result != null) {
                metadataValues.addAll(result);
            }
        }

        for (MetadatumDTO metadataValue : metadataValues) {
            System.out.println("Field: " + metadataValue.getField() + " | Value: " + metadataValue.getValue());
        }

        assertNotNull(metadataValues);
        assertEquals(EXPECTED_METADATA_COUNT, metadataValues.size());

        // check metadata values and fields
        assertEquals("dc.title", metadataValues.get(0).getField());
        assertEquals("COVID", metadataValues.get(0).getValue());
        assertEquals("dc.title", metadataValues.get(1).getField());
        assertEquals("COVID.", metadataValues.get(1).getValue());
        assertEquals("dc.date.issued", metadataValues.get(2).getField());
        assertEquals("2021-02-26", metadataValues.get(2).getValue());
        assertEquals("dc.date.issued", metadataValues.get(3).getField());
        assertEquals("2021-04-01", metadataValues.get(3).getValue());
        assertEquals("dc.subject", metadataValues.get(4).getField());
        assertEquals("Foreword", metadataValues.get(4).getValue());
        assertEquals("dc.subject", metadataValues.get(5).getField());
        assertEquals("General Medicine", metadataValues.get(5).getValue());
        assertEquals("datacite.subject.fos", metadataValues.get(6).getField());
        assertEquals("03 medical and health sciences", metadataValues.get(6).getValue());
        assertEquals("datacite.subject.fos", metadataValues.get(7).getField());
        assertEquals("0302 clinical medicine", metadataValues.get(7).getValue());
        assertEquals("datacite.subject.fos", metadataValues.get(8).getField());
        assertEquals("0502 economics and business", metadataValues.get(8).getValue());
        assertEquals("datacite.subject.fos", metadataValues.get(9).getField());
        assertEquals("05 social sciences", metadataValues.get(9).getValue());
        assertEquals("dc.contributor.author", metadataValues.get(10).getField());
        assertEquals("Van Rhee, James A.", metadataValues.get(10).getValue());
        assertEquals("dc.identifier", metadataValues.get(11).getField());
        assertEquals("10.1016/j.cpha.2021.01.002", metadataValues.get(11).getValue());
        assertEquals("dc.identifier", metadataValues.get(12).getField());
        assertEquals("33655081", metadataValues.get(12).getValue());
        assertEquals("dc.identifier", metadataValues.get(13).getField());
        assertEquals("PMC7906009", metadataValues.get(13).getValue());
        assertEquals("dc.identifier.other", metadataValues.get(14).getField());
        assertEquals("doi_dedup___::a987394eee2a5e9e43704a164fb11202", metadataValues.get(14).getValue());
        assertEquals("dc.publisher", metadataValues.get(15).getField());
        assertEquals("Elsevier BV", metadataValues.get(15).getValue());
        assertEquals("dc.relation.issn", metadataValues.get(16).getField());
        assertEquals("2405-7991", metadataValues.get(16).getValue());
        assertEquals("dc.identifier.doi", metadataValues.get(17).getField());
        assertEquals("10.1016/j.cpha.2021.01.002", metadataValues.get(17).getValue());
        assertEquals("dc.identifier.pmid", metadataValues.get(18).getField());
        assertEquals("33655081", metadataValues.get(18).getValue());
        assertEquals("oaire.citation.volume", metadataValues.get(19).getField());
        assertEquals("6", metadataValues.get(19).getValue());
        assertEquals("dc.relation.ispartof", metadataValues.get(20).getField());
        assertEquals("Physician Assistant Clinics", metadataValues.get(20).getValue());
    }

    private List<Element> splitToRecords(String recordsSrc) {
        try {
            SAXBuilder saxBuilder = XMLUtils.getSAXBuilder();
            Document document = saxBuilder.build(new StringReader(recordsSrc));
            Element root = document.getRootElement();
            List<Namespace> namespaces = Arrays.asList(
                 Namespace.getNamespace("dri", "http://www.driver-repository.eu/namespace/dri"),
                 Namespace.getNamespace("oaf", "http://namespace.openaire.eu/oaf"),
                 Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance"));
            XPathExpression<Element> xpath = XPathFactory.instance().compile("/response/results/result",
                                                                     Filters.element(), null, namespaces);
            List<Element> recordsList = xpath.evaluate(root);
            return recordsList;
        } catch (JDOMException | IOException e) {
            return List.of();
        }
    }

    private String readDocumentFromResource(String name) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(name)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + name);
            }
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }

}
