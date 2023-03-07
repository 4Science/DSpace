/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import static org.apache.commons.collections4.IteratorUtils.chainedIterator;
import static org.apache.commons.collections4.IteratorUtils.singletonListIterator;
import static org.dspace.app.bulkedit.BulkImport.ACCESS_CONDITION_HEADER;
import static org.dspace.app.bulkedit.BulkImport.ADDITIONAL_ACCESS_CONDITION_HEADER;
import static org.dspace.app.bulkedit.BulkImport.AUTHORITY_SEPARATOR;
import static org.dspace.app.bulkedit.BulkImport.BITSTREAMS_SHEET_NAME;
import static org.dspace.app.bulkedit.BulkImport.BITSTREAM_POSITION_HEADER;
import static org.dspace.app.bulkedit.BulkImport.BUNDLE_HEADER;
import static org.dspace.app.bulkedit.BulkImport.FILE_PATH_HEADER;
import static org.dspace.app.bulkedit.BulkImport.ID_HEADER;
import static org.dspace.app.bulkedit.BulkImport.LANGUAGE_SEPARATOR_PREFIX;
import static org.dspace.app.bulkedit.BulkImport.LANGUAGE_SEPARATOR_SUFFIX;
import static org.dspace.app.bulkedit.BulkImport.METADATA_SEPARATOR;
import static org.dspace.app.bulkedit.BulkImport.PARENT_ID_HEADER;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dspace.app.bulkedit.BulkImport;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkMode;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.integration.crosswalks.model.XlsCollectionSheet;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.dspace.services.ConfigurationService;
import org.dspace.submit.model.AccessConditionOption;
import org.dspace.submit.model.UploadConfiguration;
import org.dspace.submit.model.UploadConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.datetime.DateFormatter;

/**
 * Implementation of {@link StreamDisseminationCrosswalk} to export all the item
 * of the given collection in the xls format. This format is the same expected
 * by the import performed with {@link BulkImport}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class XlsCollectionCrosswalk implements ItemExportCrosswalk {

    private static Logger log = LogManager.getLogger(XlsCollectionCrosswalk.class);

    private static final String BITSTREAM_URL_FORMAT = "%s/api/core/bitstreams/%s/content";

    private static final DateFormatter DATE_FORMATTER = new DateFormatter("yyyy-MM-dd");

    @Autowired
    private ItemService itemService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ResourcePolicyService resourcePolicyService;

    @Autowired
    private UploadConfigurationService uploadConfigurationService;

    private DCInputsReader reader;

    @PostConstruct
    private void postConstruct() {
        try {
            this.setReader(new DCInputsReader());
        } catch (DCInputsReaderException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso) {
        return dso.getType() == Constants.COLLECTION;
    }

    @Override
    public String getMIMEType() {
        return "application/vnd.ms-excel";
    }

    @Override
    public String getFileName() {
        return "items.xls";
    }

    public CrosswalkMode getCrosswalkMode() {
        return CrosswalkMode.MULTIPLE;
    }

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {

        if (!canDisseminate(context, dso)) {
            throw new CrosswalkObjectNotSupported("Can only crosswalk a Collection");
        }

        Collection collection = (Collection) dso;

        Iterator<Item> itemIterator = itemService.findByCollection(context, collection);

        writeWorkbook(context, collection, itemIterator, out);

    }

    @Override
    public void disseminate(Context context, Iterator<? extends DSpaceObject> dsoIterator, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {

        if (!dsoIterator.hasNext()) {
            throw new IllegalArgumentException("At least one object must be provided to perform xsl export");
        }

        Item firstItem = convertToItem(dsoIterator.next());
        Collection collection = findCollection(context, firstItem);
        Iterator<Item> itemIterator = convertToItemIterator(dsoIterator);

        Iterator<Item> newItemIterator = chainedIterator(singletonListIterator(firstItem), itemIterator);
        writeWorkbook(context, collection, newItemIterator, out);

    }

    private void writeWorkbook(Context context, Collection collection, Iterator<Item> itemIterator, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {

        try (Workbook workbook = new XSSFWorkbook()) {

            XlsCollectionSheet mainSheet = writeMainSheetHeader(context, collection, workbook);
            List<XlsCollectionSheet> nestedMetadataSheets = writeNestedMetadataSheetsHeader(collection, workbook);
            XlsCollectionSheet bitstreamSheet = writeBitstreamSheetHeader(collection, workbook);

            writeWorkbookContent(context, itemIterator, mainSheet, nestedMetadataSheets, bitstreamSheet);

            List<XlsCollectionSheet> sheets = new ArrayList<XlsCollectionSheet>(nestedMetadataSheets);
            sheets.add(mainSheet);
            sheets.add(bitstreamSheet);
            autoSizeColumns(sheets);

            workbook.write(out);

        }

    }

    private List<XlsCollectionSheet> writeNestedMetadataSheetsHeader(Collection collection, Workbook workbook) {
        return getSubmissionFormMetadataGroups(collection).stream()
            .map(metadataGroup -> writeNestedMetadataSheetHeader(collection, workbook, metadataGroup))
            .collect(Collectors.toList());
    }

    private XlsCollectionSheet writeMainSheetHeader(Context context, Collection collection, Workbook workbook) {
        XlsCollectionSheet mainSheet = new XlsCollectionSheet(workbook, "items", false, collection);
        mainSheet.appendHeader(ID_HEADER);
        List<String> metadataFields = getSubmissionFormMetadata(collection);
        for (String metadataField : metadataFields) {
            mainSheet.appendHeaderIfNotPresent(metadataField);
        }
        return mainSheet;
    }

    private XlsCollectionSheet writeNestedMetadataSheetHeader(Collection collection, Workbook workbook, String field) {
        XlsCollectionSheet nestedMetadataSheet = new XlsCollectionSheet(workbook, field, true, collection);
        List<String> nestedMetadataFields = getSubmissionFormMetadataGroup(collection, field);
        nestedMetadataSheet.appendHeader(PARENT_ID_HEADER);
        for (String metadataField : nestedMetadataFields) {
            nestedMetadataSheet.appendHeader(metadataField);
        }
        return nestedMetadataSheet;
    }

    private XlsCollectionSheet writeBitstreamSheetHeader(Collection collection, Workbook workbook) {
        XlsCollectionSheet bitstreamSheet = new XlsCollectionSheet(workbook, BITSTREAMS_SHEET_NAME, true, collection);

        for (String bitstreamSheetHeader : BulkImport.BITSTREAMS_SHEET_HEADERS) {
            bitstreamSheet.appendHeader(bitstreamSheetHeader);
        }

        List<String> metadataFields = getMetadaFields(collection);
        for (String metadataField : metadataFields) {
            bitstreamSheet.appendHeaderIfNotPresent(metadataField);
        }

        return bitstreamSheet;
    }

    private void writeWorkbookContent(Context context, Iterator<Item> itemIterator, XlsCollectionSheet mainSheet,
        List<XlsCollectionSheet> nestedMetadataSheets, XlsCollectionSheet bitstreamSheet) throws SQLException {

        while (itemIterator.hasNext()) {

            Item item = itemIterator.next();

            if (isNotInCollection(context, item, mainSheet.getCollection())) {
                throw new IllegalArgumentException("It is not possible to export items from two different collections: "
                    + "item " + item.getID() + " is not in collection " + mainSheet.getCollection().getID());
            }

            writeMainSheet(context, item, mainSheet);

            nestedMetadataSheets.forEach(sheet -> writeNestedMetadataSheet(item, sheet));
            writeBitstreamSheet(context, item, bitstreamSheet);

            context.uncacheEntity(item);
        }

    }

    private void writeBitstreamSheet(Context context, Item item, XlsCollectionSheet sheet) {
        for (Bundle bundle : item.getBundles()) {
            String bundleName = bundle.getName();
            int bitstreamPosition = 1;
            for (Bitstream bitstream : bundle.getBitstreams()) {
                writeBitstreamRow(context, sheet, item, bitstream, bundleName, String.valueOf(bitstreamPosition));
                bitstreamPosition++;
            }
        }
    }

    private void writeBitstreamRow(Context context, XlsCollectionSheet bitstreamSheet, Item item,
        Bitstream bitstream, String bundle, String position) {
        bitstreamSheet.appendRow();
        writeBitstreamBaseValues(context, bitstreamSheet, item, bitstream, bundle, position);
        writeBitstreamMetadataValues(bitstreamSheet, bitstream);
    }

    private void writeBitstreamMetadataValues(XlsCollectionSheet bitstreamSheet, Bitstream bitstream) {
        for (String header : bitstreamSheet.getHeaders()) {
            if (isBitstreamMetadataFieldHeader(header)) {
                bitstreamService.getMetadataByMetadataString(bitstream, header)
                        .forEach(value -> writeMetadataValue(bitstreamSheet, header, value));
            }
        }
    }

    private void writeBitstreamBaseValues(Context context, XlsCollectionSheet bitstreamSheet,
        Item item, Bitstream bitstream, String bundleName, String position) {
        bitstreamSheet.setValueOnLastRow(PARENT_ID_HEADER, item.getID().toString());
        bitstreamSheet.setValueOnLastRow(FILE_PATH_HEADER, getBitstreamLocationUrl(bitstream));
        bitstreamSheet.setValueOnLastRow(BUNDLE_HEADER, bundleName);
        bitstreamSheet.setValueOnLastRow(BITSTREAM_POSITION_HEADER, position);
        bitstreamSheet.setValueOnLastRow(ACCESS_CONDITION_HEADER, getCustomResourcePolicies(context, bitstream));
        bitstreamSheet.setValueOnLastRow(ADDITIONAL_ACCESS_CONDITION_HEADER, "N");
    }

    private String getBitstreamLocationUrl(Bitstream bitstream) {
        return String.format(
                BITSTREAM_URL_FORMAT,
                configurationService.getProperty("dspace.server.url"),
                bitstream.getID().toString()
        );
    }

    private String getCustomResourcePolicies(Context context, Bitstream bitstream) {
        List<AccessConditionOption> uploadAccessConditions = getUploadAccessConditionOptionNames();
        List<ResourcePolicy> resourcePolicies = findCustomReadResourcePolicies(context, bitstream);
        return composeResourcePolicies(resourcePolicies, uploadAccessConditions);
    }

    private List<ResourcePolicy> findCustomReadResourcePolicies(Context context, Bitstream bitstream) {
        try {
            return resourcePolicyService.find(context, bitstream, Constants.READ, ResourcePolicy.TYPE_CUSTOM);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String composeResourcePolicies(List<ResourcePolicy> policies, List<AccessConditionOption> options) {
        return policies.stream()
            .flatMap(resourcePolicy -> formatResourcePolicy(resourcePolicy, options).stream())
            .collect(Collectors.joining(BulkImport.METADATA_SEPARATOR));
    }

    private Optional<String> formatResourcePolicy(ResourcePolicy policy, List<AccessConditionOption> options) {
        return getAccessConditionByName(options, policy.getRpName())
            .map(accessConditionOption -> formaResourcePolicy(policy, accessConditionOption));
    }

    private String formaResourcePolicy(ResourcePolicy policy, AccessConditionOption accessConditionOption) {

        String resourcePolicyAsString = policy.getRpName();

        Date startDate = policy.getStartDate();
        if (accessConditionOption.getHasStartDate() && startDate != null) {
            resourcePolicyAsString += BulkImport.ACCESS_CONDITION_ATTRIBUTES_SEPARATOR + formatDate(startDate);
        }

        Date endDate = policy.getEndDate();
        if (accessConditionOption.getHasEndDate() && endDate != null) {
            resourcePolicyAsString += BulkImport.ACCESS_CONDITION_ATTRIBUTES_SEPARATOR + formatDate(endDate);
        }

        String description = policy.getRpDescription();
        if (StringUtils.isNotBlank(description)) {
            resourcePolicyAsString += BulkImport.ACCESS_CONDITION_ATTRIBUTES_SEPARATOR + description;
        }

        return resourcePolicyAsString;
    }

    private Optional<AccessConditionOption> getAccessConditionByName(List<AccessConditionOption> options, String name) {
        return options.stream()
            .filter(accessConditionOption -> accessConditionOption.getName().equals(name))
            .findFirst();
    }

    private String formatDate(Date date) {
        return DATE_FORMATTER.print(date, Locale.getDefault());
    }

    private void writeMainSheet(Context context, Item item, XlsCollectionSheet mainSheet) {

        mainSheet.appendRow();

        List<String> headers = mainSheet.getHeaders();
        for (String header : headers) {

            if (header.equals(ID_HEADER)) {
                mainSheet.setValueOnLastRow(header, item.getID().toString());
                continue;
            }

            getMetadataValues(item, header).forEach(value -> writeMetadataValue(mainSheet, header, value));
        }

    }

    private void writeNestedMetadataSheet(Item item, XlsCollectionSheet nestedMetadataSheet) {

        String groupName = nestedMetadataSheet.getSheet().getSheetName();
        int groupSize = getMetadataGroupSize(item, groupName);
        List<String> headers = nestedMetadataSheet.getHeaders();

        Map<String, List<MetadataValue>> metadataValues = new HashMap<>();

        IntStream.range(0, groupSize).forEach(
            groupIndex -> writeNestedMetadataRow(item, nestedMetadataSheet, metadataValues, headers, groupIndex));

    }

    private void writeNestedMetadataRow(Item item, XlsCollectionSheet nestedMetadataSheet,
        Map<String, List<MetadataValue>> metadataValues, List<String> headers, int groupIndex) {

        nestedMetadataSheet.appendRow();

        for (String header : headers) {

            if (header.equals(PARENT_ID_HEADER)) {
                nestedMetadataSheet.setValueOnLastRow(header, item.getID().toString());
                continue;
            }

            List<MetadataValue> metadata = null;
            if (metadataValues.containsKey(header)) {
                metadata = metadataValues.get(header);
            } else {
                metadata = getMetadataValues(item, header);
                metadataValues.put(header, metadata);
            }

            if (metadata.size() <= groupIndex) {
                log.warn("The cardinality of group with nested metadata " + header + " is inconsistent "
                    + "for item with id " + item.getID());
                continue;
            }

            writeMetadataValue(nestedMetadataSheet, header, metadata.get(groupIndex));

        }

    }

    private void writeMetadataValue(XlsCollectionSheet sheet, String header, MetadataValue metadataValue) {

        String language = metadataValue.getLanguage();
        if (StringUtils.isBlank(language)) {
            sheet.appendValueOnLastRow(header, formatMetadataValue(metadataValue), METADATA_SEPARATOR);
            return;
        }

        if (isLanguageSupported(sheet.getCollection(), language, header, sheet.isNestedMetadata())) {
            String headerWithLanguage = header + LANGUAGE_SEPARATOR_PREFIX + language + LANGUAGE_SEPARATOR_SUFFIX;
            sheet.appendHeaderIfNotPresent(headerWithLanguage);
            sheet.appendValueOnLastRow(headerWithLanguage, formatMetadataValue(metadataValue), METADATA_SEPARATOR);
        }

    }

    private String formatMetadataValue(MetadataValue metadata) {

        String value = metadata.getValue();
        value = CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE.equals(value) ? "" : value;

        String authority = metadata.getAuthority();
        int confidence = metadata.getConfidence();

        if (StringUtils.isBlank(authority)) {
            return value;
        }

        return value + AUTHORITY_SEPARATOR + authority + AUTHORITY_SEPARATOR + confidence;
    }

    private boolean isBitstreamMetadataFieldHeader(String header) {
        return !ArrayUtils.contains(BulkImport.BITSTREAMS_SHEET_HEADERS, header);
    }

    private Iterator<Item> convertToItemIterator(Iterator<? extends DSpaceObject> dsoIterator) {
        return IteratorUtils.transformedIterator(dsoIterator, this::convertToItem);
    }

    private Item convertToItem(DSpaceObject dso) {
        if (dso.getType() != Constants.ITEM) {
            throw new IllegalArgumentException("The xsl export supports only items. "
                + "Found object with type " + dso.getType() + " and id " + dso.getID());
        }
        return (Item) dso;
    }

    private boolean isNotInCollection(Context context, Item item, Collection collection) throws SQLException {
        return !collection.equals(findCollection(context, item));
    }

    private Collection findCollection(Context context, Item item) throws SQLException {
        Collection collection = collectionService.findByItem(context, item);
        if (collection == null) {
            throw new IllegalArgumentException("No collection found for item with id: " + item.getID());
        }
        return collection;
    }

    private List<String> getSubmissionFormMetadataGroup(Collection collection, String groupName) {
        try {
            return this.reader.getAllNestedMetadataByGroupName(collection, groupName);
        } catch (DCInputsReaderException e) {
            throw new RuntimeException("An error occurs reading the input configuration "
                + "by group name " + groupName, e);
        }
    }

    private List<String> getSubmissionFormMetadata(Collection collection) {
        try {
            return this.reader.getSubmissionFormMetadata(collection);
        } catch (DCInputsReaderException e) {
            throw new RuntimeException("An error occurs reading the input configuration by collection", e);
        }
    }

    private List<String> getSubmissionFormMetadataGroups(Collection collection) {
        try {
            return this.reader.getSubmissionFormMetadataGroups(collection);
        } catch (DCInputsReaderException e) {
            throw new RuntimeException("An error occurs reading the input configuration by collection", e);
        }
    }

    private boolean isLanguageSupported(Collection collection, String language, String metadataField, boolean group) {
        try {
            List<String> languages = this.reader.getLanguagesForMetadata(collection, metadataField, group);
            return CollectionUtils.isNotEmpty(languages) ? languages.contains(language) : false;
        } catch (DCInputsReaderException e) {
            throw new RuntimeException("An error occurs reading the input configuration by collection", e);
        }
    }

    private int getMetadataGroupSize(Item item, String metadataGroupFieldName) {
        return getMetadataValues(item, metadataGroupFieldName).size();
    }

    private List<MetadataValue> getMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField);
    }

    private void autoSizeColumns(List<XlsCollectionSheet> sheets) {
        sheets.forEach(sheet -> autoSizeColumns(sheet.getSheet()));
    }

    private List<AccessConditionOption> getUploadAccessConditionOptionNames() {
        UploadConfiguration uploadConfiguration = uploadConfigurationService.getMap().get("upload");
        if (uploadConfiguration == null) {
            throw new IllegalStateException("No upload access conditions configuration found");
        }
        return uploadConfiguration.getOptions();
    }

    private void autoSizeColumns(Sheet sheet) {
        if (sheet.getPhysicalNumberOfRows() > 0) {
            Row row = sheet.getRow(sheet.getFirstRowNum());
            for (Cell cell : row) {
                int columnIndex = cell.getColumnIndex();
                sheet.autoSizeColumn(columnIndex);
            }
        }
    }

    public void setReader(DCInputsReader reader) {
        this.reader = reader;
    }

    private List<String> getMetadaFields(Collection collection) {
        List<String> metadataFields;
        try {
            metadataFields = reader.getUploadMetadataFieldsFromCollection(collection);
        } catch (DCInputsReaderException e) {
            throw new RuntimeException(e);
        }
        return metadataFields;
    }
}
