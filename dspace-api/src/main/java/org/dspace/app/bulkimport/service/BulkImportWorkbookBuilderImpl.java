/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.service;

import static com.google.common.collect.Iterators.transform;
import static org.dspace.app.bulkedit.BulkImport.ACCESS_CONDITION_HEADER;
import static org.dspace.app.bulkedit.BulkImport.ADDITIONAL_ACCESS_CONDITION_HEADER;
import static org.dspace.app.bulkedit.BulkImport.BITSTREAMS_SHEET_NAME;
import static org.dspace.app.bulkedit.BulkImport.BITSTREAM_POSITION_HEADER;
import static org.dspace.app.bulkedit.BulkImport.BUNDLE_HEADER;
import static org.dspace.app.bulkedit.BulkImport.DISCOVERABLE_HEADER;
import static org.dspace.app.bulkedit.BulkImport.FILE_PATH_HEADER;
import static org.dspace.app.bulkedit.BulkImport.ID_HEADER;
import static org.dspace.app.bulkedit.BulkImport.LANGUAGE_SEPARATOR_PREFIX;
import static org.dspace.app.bulkedit.BulkImport.LANGUAGE_SEPARATOR_SUFFIX;
import static org.dspace.app.bulkedit.BulkImport.METADATA_ATTRIBUTES_SEPARATOR;
import static org.dspace.app.bulkedit.BulkImport.METADATA_SEPARATOR;
import static org.dspace.app.bulkedit.BulkImport.PARENT_ID_HEADER;

import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
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
import org.dspace.app.bulkimport.model.BulkImportSheet;
import org.dspace.app.bulkimport.model.BulkImportWorkbook;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.converter.ItemDTOConverter;
import org.dspace.content.converter.ItemToItemDTOConverter;
import org.dspace.content.dto.BitstreamDTO;
import org.dspace.content.dto.ItemDTO;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.dto.ResourcePolicyDTO;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.submit.model.AccessConditionOption;
import org.dspace.submit.model.UploadConfiguration;
import org.dspace.submit.model.UploadConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.datetime.DateFormatter;

/**
 * Implementation of {@link BulkImportWorkbookBuilder}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 *
 */
public class BulkImportWorkbookBuilderImpl implements BulkImportWorkbookBuilder {

    private static final Logger LOGGER = LogManager.getLogger(BulkImportWorkbookBuilderImpl.class);

    private static final DateFormatter DATE_FORMATTER = new DateFormatter("yyyy-MM-dd");

    @Autowired
    private UploadConfigurationService uploadConfigurationService;

    @Autowired
    private ItemToItemDTOConverter itemToItemDTOConverter;

    @Autowired
    private CollectionService collectionService;

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
    public <T> Workbook build(Context ctx, Collection collection, Iterator<T> sources, ItemDTOConverter<T> converter) {
        Iterator<ItemDTO> itemIterator = transform(sources, source -> converter.convert(ctx, source));
        return build(ctx, collection, itemIterator);
    }

    @Override
    public Workbook buildForItems(Context context, Collection collection, Iterator<Item> items) {
        Iterator<ItemDTO> itemIterator = transform(items, item -> convertItem(context, collection, item));
        return build(context, collection, itemIterator);
    }

    @Override
    public Workbook build(Context context, Collection collection, Iterator<ItemDTO> items) {

        Workbook workbook = new XSSFWorkbook();

        BulkImportSheet mainSheet = writeMainSheetHeader(collection, workbook);
        List<BulkImportSheet> nestedSheets = writeNestedMetadataSheetsHeader(collection, workbook);
        BulkImportSheet bitstreamSheet = writeBitstreamSheetHeader(collection, workbook);

        BulkImportWorkbook bulkImportWorkbook = new BulkImportWorkbook(mainSheet, nestedSheets, bitstreamSheet);

        writeWorkbookContent(items, bulkImportWorkbook);

        autoSizeColumns(bulkImportWorkbook.getAllSheets());

        return workbook;

    }

    private BulkImportSheet writeMainSheetHeader(Collection collection, Workbook workbook) {
        BulkImportSheet mainSheet = new BulkImportSheet(workbook, "items", false, collection);
        mainSheet.appendHeader(ID_HEADER);
        mainSheet.appendHeader(DISCOVERABLE_HEADER);
        List<String> metadataFields = getSubmissionFormMetadata(collection);
        for (String metadataField : metadataFields) {
            mainSheet.appendHeaderIfNotPresent(metadataField);
        }
        return mainSheet;
    }

    private List<BulkImportSheet> writeNestedMetadataSheetsHeader(Collection collection, Workbook workbook) {
        return getSubmissionFormMetadataGroups(collection).stream()
            .map(metadataGroup -> writeNestedMetadataSheetHeader(collection, workbook, metadataGroup))
            .collect(Collectors.toList());
    }

    private BulkImportSheet writeNestedMetadataSheetHeader(Collection collection, Workbook workbook, String field) {
        BulkImportSheet nestedMetadataSheet = new BulkImportSheet(workbook, field, true, collection);
        List<String> nestedMetadataFields = getSubmissionFormMetadataGroup(collection, field);
        nestedMetadataSheet.appendHeader(PARENT_ID_HEADER);
        for (String metadataField : nestedMetadataFields) {
            nestedMetadataSheet.appendHeader(metadataField);
        }
        return nestedMetadataSheet;
    }

    private BulkImportSheet writeBitstreamSheetHeader(Collection collection, Workbook workbook) {
        BulkImportSheet bitstreamSheet = new BulkImportSheet(workbook, BITSTREAMS_SHEET_NAME, true, collection);

        for (String bitstreamSheetHeader : BulkImport.BITSTREAMS_SHEET_HEADERS) {
            bitstreamSheet.appendHeader(bitstreamSheetHeader);
        }

        List<String> metadataFields = getUploadMetadaFields(collection);
        for (String metadataField : metadataFields) {
            bitstreamSheet.appendHeaderIfNotPresent(metadataField);
        }

        return bitstreamSheet;
    }

    private void writeWorkbookContent(Iterator<ItemDTO> items, BulkImportWorkbook workbook) {

        while (items.hasNext()) {

            ItemDTO item = items.next();

            writeMainSheet(item, workbook.getMainSheet());
            workbook.getNestedMetadataSheets().forEach(sheet -> writeNestedMetadataSheet(item, sheet));
            writeBitstreamSheet(item, workbook.getBitstreamSheet());

        }

    }

    private void writeMainSheet(ItemDTO item, BulkImportSheet mainSheet) {

        mainSheet.appendRow();

        for (String header : mainSheet.getHeaders()) {

            if (header.equals(ID_HEADER)) {
                mainSheet.setValueOnLastRow(header, item.getId().toString());
                continue;
            }

            if (header.equals(DISCOVERABLE_HEADER)) {
                mainSheet.setValueOnLastRow(header, item.isDiscoverable() ? "Y" : "N");
                continue;
            }

            item.getMetadataValues(header).forEach(value -> writeMetadataValue(mainSheet, header, value));
        }

    }

    private void writeNestedMetadataSheet(ItemDTO item, BulkImportSheet nestedMetadataSheet) {

        String groupName = nestedMetadataSheet.getSheet().getSheetName();
        int groupSize = getMetadataGroupSize(item, groupName);

        for (int groupIndex = 0; groupIndex < groupSize; groupIndex++) {
            writeNestedMetadataRow(item, nestedMetadataSheet, groupIndex);
        }

    }

    private void writeNestedMetadataRow(ItemDTO item, BulkImportSheet nestedMetadataSheet, int groupIndex) {

        nestedMetadataSheet.appendRow();

        for (String header : nestedMetadataSheet.getHeaders()) {

            if (header.equals(PARENT_ID_HEADER)) {
                nestedMetadataSheet.setValueOnLastRow(header, item.getId());
                continue;
            }

            List<MetadataValueDTO> metadata = item.getMetadataValues(header);

            if (metadata.size() <= groupIndex) {
                LOGGER.warn("The cardinality of group with nested metadata " + header + " is inconsistent "
                    + "for item with id " + item.getId());
                continue;
            }

            writeMetadataValue(nestedMetadataSheet, header, metadata.get(groupIndex));

        }

    }

    private void writeBitstreamSheet(ItemDTO item, BulkImportSheet sheet) {
        for (BitstreamDTO bitstream : item.getBitstreams()) {
            writeBitstreamRow(sheet, item, bitstream);
        }
    }

    private void writeBitstreamRow(BulkImportSheet bitstreamSheet, ItemDTO item, BitstreamDTO bitstream) {
        bitstreamSheet.appendRow();
        writeBitstreamBaseValues(bitstreamSheet, item, bitstream);
        writeBitstreamMetadataValues(bitstreamSheet, bitstream);
    }

    private void writeBitstreamMetadataValues(BulkImportSheet bitstreamSheet, BitstreamDTO bitstream) {
        for (String header : bitstreamSheet.getHeaders()) {
            if (isBitstreamMetadataFieldHeader(header)) {
                bitstream.getMetadataValues(header)
                    .forEach(value -> writeMetadataValue(bitstreamSheet, header, value));
            }
        }
    }

    private void writeBitstreamBaseValues(BulkImportSheet bitstreamSheet, ItemDTO item, BitstreamDTO bitstream) {
        bitstreamSheet.setValueOnLastRow(PARENT_ID_HEADER, item.getId());
        bitstreamSheet.setValueOnLastRow(FILE_PATH_HEADER, bitstream.getLocation());
        bitstreamSheet.setValueOnLastRow(BUNDLE_HEADER, bitstream.getBundleName());
        bitstreamSheet.setValueOnLastRow(BITSTREAM_POSITION_HEADER, getBitstreamPosition(bitstream));
        bitstreamSheet.setValueOnLastRow(ACCESS_CONDITION_HEADER, getResourcePoliciesAsString(bitstream));
        bitstreamSheet.setValueOnLastRow(ADDITIONAL_ACCESS_CONDITION_HEADER, "N");
    }

    private String getBitstreamPosition(BitstreamDTO bitstream) {
        return bitstream.getPosition() != null ? String.valueOf(bitstream.getPosition() + 1) : "";
    }

    private String getResourcePoliciesAsString(BitstreamDTO bitstream) {
        List<AccessConditionOption> uploadAccessConditions = getUploadAccessConditionOptionNames();
        List<ResourcePolicyDTO> resourcePolicies = getCustomReadResourcePolicies(bitstream);
        return composeResourcePolicies(resourcePolicies, uploadAccessConditions);
    }

    private List<ResourcePolicyDTO> getCustomReadResourcePolicies(BitstreamDTO bitstream) {
        return bitstream.getResourcePolicies().stream()
            .filter(resourcePolicy -> ResourcePolicy.TYPE_CUSTOM.equals(resourcePolicy.getType()))
            .filter(resourcePolicy -> Constants.READ == resourcePolicy.getAction())
            .collect(Collectors.toList());
    }

    private String composeResourcePolicies(List<ResourcePolicyDTO> policies, List<AccessConditionOption> options) {
        return policies.stream()
            .flatMap(resourcePolicy -> formatResourcePolicy(resourcePolicy, options).stream())
            .collect(Collectors.joining(BulkImport.METADATA_SEPARATOR));
    }

    private Optional<String> formatResourcePolicy(ResourcePolicyDTO policy, List<AccessConditionOption> options) {
        return getAccessConditionByName(options, policy.getName())
            .map(accessConditionOption -> formaResourcePolicy(policy, accessConditionOption));
    }

    private String formaResourcePolicy(ResourcePolicyDTO policy, AccessConditionOption accessConditionOption) {

        String resourcePolicyAsString = policy.getName();

        Date startDate = policy.getStartDate();
        if (accessConditionOption.getHasStartDate() && startDate != null) {
            resourcePolicyAsString += BulkImport.ACCESS_CONDITION_ATTRIBUTES_SEPARATOR + formatDate(startDate);
        }

        Date endDate = policy.getEndDate();
        if (accessConditionOption.getHasEndDate() && endDate != null) {
            resourcePolicyAsString += BulkImport.ACCESS_CONDITION_ATTRIBUTES_SEPARATOR + formatDate(endDate);
        }

        String description = policy.getDescription();
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

    private void writeMetadataValue(BulkImportSheet sheet, String header, MetadataValueDTO metadataValue) {

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

    private String formatMetadataValue(MetadataValueDTO metadata) {

        String value = metadata.getValue();

        String authority = metadata.getAuthority();
        int confidence = metadata.getConfidence();
        Integer securityLevel = metadata.getSecurityLevel();

        String formattedValue = CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE.equals(value) ? "" : value;

        if (StringUtils.isNotBlank(authority)) {
            formattedValue += METADATA_ATTRIBUTES_SEPARATOR + authority + METADATA_ATTRIBUTES_SEPARATOR + confidence;
        }

        if (securityLevel != null) {
            formattedValue += METADATA_ATTRIBUTES_SEPARATOR + BulkImport.SECURITY_LEVEL_PREFIX + securityLevel;
        }

        return formattedValue;
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

    private List<String> getUploadMetadaFields(Collection collection) {
        List<String> metadataFields;
        try {
            metadataFields = reader.getUploadMetadataFieldsFromCollection(collection);
        } catch (DCInputsReaderException e) {
            throw new RuntimeException(e);
        }
        return metadataFields;
    }

    private List<AccessConditionOption> getUploadAccessConditionOptionNames() {
        UploadConfiguration uploadConfiguration = uploadConfigurationService.getMap().get("upload");
        if (uploadConfiguration == null) {
            throw new IllegalStateException("No upload access conditions configuration found");
        }
        return uploadConfiguration.getOptions();
    }

    private String formatDate(Date date) {
        return DATE_FORMATTER.print(date, Locale.getDefault());
    }

    private boolean isBitstreamMetadataFieldHeader(String header) {
        return !ArrayUtils.contains(BulkImport.BITSTREAMS_SHEET_HEADERS, header);
    }

    private int getMetadataGroupSize(ItemDTO item, String metadataGroupFieldName) {
        return item.getMetadataValues(metadataGroupFieldName).size();
    }

    private void autoSizeColumns(List<BulkImportSheet> sheets) {
        sheets.forEach(sheet -> autoSizeColumns(sheet.getSheet()));
    }

    private ItemDTO convertItem(Context context, Collection collection, Item item) {

        if (isNotInCollection(context, item, collection)) {
            throw new IllegalArgumentException("It is not possible to export items from two different collections: "
                + "item " + item.getID() + " is not in collection " + collection.getID());
        }

        ItemDTO itemDTO = itemToItemDTOConverter.convert(context, item);

        try {
            context.uncacheEntity(item);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }

        return itemDTO;
    }

    private boolean isNotInCollection(Context context, Item item, Collection collection) {
        return !collection.equals(findCollection(context, item));
    }

    private Collection findCollection(Context context, Item item) {
        try {

            Collection collection = collectionService.findByItem(context, item);
            if (collection == null) {
                throw new IllegalArgumentException("No collection found for item with id: " + item.getID());
            }
            return collection;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

}
