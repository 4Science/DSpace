/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.marcxml2item.parser;

import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import jakarta.annotation.PostConstruct;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.marcxml2item.model.ItemsImportMapping;
import org.dspace.app.marcxml2item.model.ItemsImportMapping.MetadataField;
import org.dspace.app.marcxml2item.reader.ItemsImportMetadataFieldReader;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Service implementation class for the MarcXmlParser
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 **/
public class MarcXmlParserImpl implements MarcXmlParser {

    public static final String TYPE_FILTER_PROPERTY_PREFIX = "marc-xml.items-import.types";
    public static final String TYPE_VOCABULARY_PROPERTY_PREFIX = "marc-xml.items-import.vocabulary";

    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    private XPath xPath;
    private DocumentBuilder documentBuilder;
    private Map<String, String> typeFilters;
    private Map<String, String> typeVocabularies;
    private Map<String, ItemsImportMetadataFieldReader> readers;

    @PostConstruct
    private void setup() {
        try {
            this.documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

        typeFilters = readAllTypeFilters();
        typeVocabularies = readAllTypeVocabularies();
        xPath = XPathFactory.newInstance().newXPath();
        readers = new DSpace().getServiceManager()
                              .getServicesByType(ItemsImportMetadataFieldReader.class)
                              .stream()
                              .collect(toMap(ItemsImportMetadataFieldReader::getReaderName, Function.identity()));
    }

    @Override
    public ItemsImportMapping parseMapping(String configuration) {
        if (StringUtils.isBlank(configuration)) {
            throw new IllegalArgumentException("No import mapping configuration defined");
        }
        if (!new File(configuration).exists()) {
            throw new IllegalStateException("No mapping file present for the import configuration");
        }

        ItemsImportMapping importMapping = readMappingConfiguration(configuration);
        validateMapping(importMapping);
        return importMapping;
    }

    @Override
    public List<List<MetadataValueDTO>> readItems(Context context, InputStream source, ItemsImportMapping mapping,
                                                   String expression) {
        try {
            Document document = documentBuilder.parse(source);
            NodeList nodeList = getNodeList(document, expression);
            List<List<MetadataValueDTO>> records = new ArrayList<List<MetadataValueDTO>>();
            for (int i = 0; i < nodeList.getLength(); i++) {
                records.add(readItemMetadataValues(context, nodeList.item(i), mapping));
            }
            return records;
        } catch (SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<MetadataValueDTO> readItemMetadataValues(Context context, Node record, ItemsImportMapping mapping) {
        String recordType = getRecordType(record).orElse(null);
        return readItemMetadataValues(context, record, recordType, mapping);
    }

    @Override
    public Optional<String> getRecordType(Node record) {
        for (String filterName : typeFilters.keySet()) {
            String filter = typeFilters.get(filterName);
            if (evaluateFilter(record, filter)) {
                return Optional.of(filterName);
            }
        }
        return Optional.empty();
    }

    @Override
    public Set<String> getAllRecordTypes() {
        return typeFilters.keySet();
    }

    private List<MetadataValueDTO> readItemMetadataValues(Context context, Node record,
                                                          String recordType, ItemsImportMapping mapping) {
        return readMetadataValues(context, record, recordType, mapping.getMetadataFields().getMetadataFields());
    }

    private MetadataValueDTO getItemType(Context context, String recordType) {
        String vocabulary = typeVocabularies.get(recordType);
        if (StringUtils.isBlank(vocabulary)) {
            throw new IllegalStateException("No vocabulary defined for record type " + recordType);
        }
        ChoiceAuthority authority = choiceAuthorityService.getChoiceAuthorityByAuthorityName(vocabulary.split(":")[0]);
        Choice choice = authority.getChoice(vocabulary, context.getCurrentLocale().toString());
        if (choice == null) {
            throw new IllegalStateException("No choice found by vocabulary " + vocabulary);
        }
        return new MetadataValueDTO("dc.type", choice.value, vocabulary, Choices.CF_ACCEPTED);
    }

    private List<MetadataValueDTO> readMetadataValues(Context context, Node record,
                                                      String recordType, List<MetadataField> fields) {

        List<MetadataValueDTO> metadataValues = new ArrayList<>();
        if (recordType != null) {
            metadataValues.add(getItemType(context, recordType));
        }
        for (ItemsImportMapping.MetadataField metadataField : fields) {
            ItemsImportMetadataFieldReader reader = readers.get(metadataField.getReader());
            NodeList nodeList = getNodeList(record, metadataField.getXPath());
            List<MetadataValueDTO> values = reader.readValues(context, metadataField.getField(), recordType, nodeList);
            metadataValues.addAll(values);
        }
        return metadataValues;
    }

    private NodeList getNodeList(Object item, String expression) {
        try {
            return (NodeList) xPath.compile(expression).evaluate(item, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("An error occurs evaluating path " + expression, e);
        }
    }

    private Boolean evaluateFilter(Node node, String path) {
        try {
            return (Boolean) xPath.compile(path).evaluate(node, XPathConstants.BOOLEAN);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("An error occurs evaluating path " + path, e);
        }
    }

    private ItemsImportMapping readMappingConfiguration(String config) {
        try (FileReader mappingReader = new FileReader(config)) {
            JAXBContext jaxbContext = JAXBContext.newInstance(ItemsImportMapping.class);
            return (ItemsImportMapping) jaxbContext.createUnmarshaller().unmarshal(mappingReader);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void validateMapping(ItemsImportMapping importMapping) {
        List<String> unknownReaders = importMapping.getMetadataFields()
                                                   .getMetadataFields()
                                                   .stream()
                                                   .map(MetadataField::getReader)
                                                   .filter(reader -> !readers.containsKey(reader))
                                                   .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(unknownReaders)) {
            throw new IllegalStateException("The following configured readers are not defined: " + unknownReaders);
        }
    }

    private Map<String, String> readAllTypeFilters() {
        Map<String, String> filters = new HashMap<>();
        List<String> propertyKeys = configurationService.getPropertyKeys(TYPE_FILTER_PROPERTY_PREFIX);
        for (String propertyKey : propertyKeys) {
            String filter = configurationService.getProperty(propertyKey);
            String filterName = StringUtils.removeStart(propertyKey, TYPE_FILTER_PROPERTY_PREFIX + ".");
            filters.put(filterName, filter);
        }
        return filters;
    }

    private Map<String, String> readAllTypeVocabularies() {
        Map<String, String> vocabularies = new HashMap<>();
        for (String recordType : getAllRecordTypes()) {
            String vocabulary = configurationService.getProperty(TYPE_VOCABULARY_PROPERTY_PREFIX + "." + recordType);
            if (StringUtils.isNotBlank(vocabulary)) {
                vocabularies.put(recordType, vocabulary);
            }
        }
        return vocabularies;
    }

}
