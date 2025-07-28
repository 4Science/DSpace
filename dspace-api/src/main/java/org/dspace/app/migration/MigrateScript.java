/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.migration;

import static java.util.stream.Collectors.toMap;
import static org.dspace.util.WorkbookUtils.getCellValue;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dspace.app.bulkimport.exception.BulkImportException;
import org.dspace.batch.ImpBitstream;
import org.dspace.batch.ImpBitstreamMetadatavalue;
import org.dspace.batch.ImpMetadatavalue;
import org.dspace.batch.ImpRecord;
import org.dspace.batch.service.ImpBitstreamMetadatavalueService;
import org.dspace.batch.service.ImpBitstreamService;
import org.dspace.batch.service.ImpMetadatavalueService;
import org.dspace.batch.service.ImpRecordService;
import org.dspace.batch.service.ImpServiceFactory;
import org.dspace.content.Collection;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.authority.Choices;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.rdbms.MigrateConnectionManager;
import org.dspace.util.WorkbookUtils;
import org.dspace.utils.DSpace;

public class MigrateScript extends DSpaceRunnable<MigrateScriptConfiguration<MigrateScript>> {
    private Context context;
    private String filename;
    private Workbook workbook;

    private List<String> metadataOrcidToken = new LinkedList<String>();

    private HashMap<String, UUID> collectionUuidMap = new HashMap<>();

    private HashMap<String, String> doTypes = new HashMap<>();

    private HashMap<String, String> metadataVisibility = new HashMap<>();

    private HashMap<String, HashMap<String, String[]>> entityMetadataMap = new HashMap();

    private HashMap<String, HashMap<String, String[]>> entityNestedMetadataMap = new HashMap();

    private HashMap<String, HashMap<String, String[]>> linkMapping = new HashMap();

    public static final String RP_CSV = "/config/migration/rp_prop.csv";

    public static final String OU_CSV = "/config/migration/ou_prop.csv";

    public static final String PJ_CSV = "/config/migration/pj_prop.csv";

    public static final String DO_CSV = "/config/migration/do_prop.csv";

    private String restrictionEntityType;

    private ImpBitstreamService impBitstreamService = ImpServiceFactory.getInstance().getImpBitstreamService();
    private ImpBitstreamMetadatavalueService impBitstreamMetadatavalueService = ImpServiceFactory.getInstance()
        .getImpBitstreamMetadatavalueService();
    private ImpMetadatavalueService impMetadatavalueService = ImpServiceFactory.getInstance()
        .getImpMetadatavalueService();
    private ImpRecordService impRecordService = ImpServiceFactory.getInstance().getImpRecordService();

    private static ConfigurationService configurationService = DSpaceServicesFactory.getInstance()
        .getConfigurationService();

    private static CollectionService collectionService = ContentServiceFactory.getInstance()
        .getCollectionService();
    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    private MetadataSchemaService schemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
    private MetadataFieldService fieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

    private int impSeq = 0;
    private int impMedataSeq = 0;
    private int impBitstreamSeq = 0;
    private int impBitstreamMetadatavalueSeq = 0;

    private EPerson migrationEperson;

    private static final String SOURCE_REF = "MIGRATION";

    private long counterForCommit = 0;
    private HashSet<String> missingMetadata = new HashSet<>();
    private HashSet<String> missingAuthority = new HashSet<>();

    private String bitstreamBasePath;
    private boolean setLinkAsAuthority = false;

    @Override
    public MigrateScriptConfiguration<MigrateScript> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("migration", MigrateScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        context = new Context();
        filename = commandLine.getOptionValue('f');
        restrictionEntityType = commandLine.getOptionValue('r');
        bitstreamBasePath = commandLine.getOptionValue('b');
        bitstreamBasePath += bitstreamBasePath.endsWith("/") ? "" : "/";
        String epersonEmail = commandLine.getOptionValue('e');
        try {
            migrationEperson = ePersonService.findByEmail(context, epersonEmail);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void internalRun() throws Exception {
        try {
            InputStream inputStream = handler.getFileStream(context, filename)
                .orElseThrow(() -> new IllegalArgumentException("Error reading file, the file couldn't be "
                    + "found for filename: " + filename));
            workbook = createWorkbook(inputStream);
            getCollectionsFromWorkBook(workbook);
            getMetadataMapFromWorkBook(workbook);
            getNestedMapFromWorkBook(workbook);
            getDoTypes(workbook);
            getMetadataVisibility(workbook);
            getLinkMapping(workbook);
            MigrateConnectionManager.getAllProperties();
            doMigrate(RP_CSV, "Person", "rp");
            doMigrate(OU_CSV, "OrgUnit", "ou");
            doMigrate(PJ_CSV, "Project", "pj");
            Set<String> doEntities = entityMetadataMap.keySet();
            doEntities.removeAll(List.of("Person", "OrgUnit", "Project"));
            doEntities.forEach(doEntity -> {
                try {
                    doMigrate(DO_CSV, doEntity, doTypes.get(doEntity));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            printReportMetadata();
        } finally {
            context.complete();
        }

    }

    private void getLinkMapping(Workbook workbook2) {
        Sheet doTypesSheet = workbook.getSheet("links");
        setLinkAsAuthority = (doTypesSheet == null);
        if (setLinkAsAuthority) {
            return;
        }
        Map<String, Integer> headers = getHeaderMap(doTypesSheet);

        List<Row> rows = getRowsForSheet(doTypesSheet);
        rows.forEach(row -> {
            String oldEntityPrefix = WorkbookUtils.getCellValue(row, 0);
            String oldField = WorkbookUtils.getCellValue(row, 1);
            String labelMetadata = WorkbookUtils.getCellValue(row, 2);
            String valueMetadata = WorkbookUtils.getCellValue(row, 3);
            addLinkMetadataMap(oldEntityPrefix, oldField, labelMetadata, valueMetadata);
        });
    }

    private void printReportMetadata() {
        String logPath = configurationService.getProperty("dspace.dir") + "/log/";
        try (PrintWriter out = new PrintWriter(logPath + "missing_metadata.report")) {
            missingMetadata.forEach(metadata -> out.println(metadata));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try (PrintWriter out = new PrintWriter(logPath + "missing_authority.report")) {
            missingAuthority.forEach(metadata -> out.println(metadata));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void doMigrate(String csvPath, String entityType, String prefix) throws Exception {
        Reader in = new FileReader(configurationService.getProperty("dspace.dir") + csvPath);
        @SuppressWarnings("deprecation")
        Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader("crisid",
            "shortname", "parent_id", "positiondef",
            "nested_object_id", "visibility", "textvalue", "datevalue",
            "dtype", "rpvalue", "projectvalue", "ouvalue", "dovalue",
            "filefolder", "filename", "fileextension", "booleanvalue", "linkdescription",
            "linkvalue", "doublevalue", "classificationvalue", "custompointer", "sourceid", "sourceref").parse(in);
        boolean isHeader = true;
        String oldCrisId = null;
        String lastNestedObjectId = "-1";
        ImpRecord impRecord = null;
        int order = 0;
        int linkOrder = 0;
        for (CSVRecord record : records) {
            boolean isFile = false;
            boolean isLink = false;
            String fileFolder = null;
            String fileName = null;
            String fileExtension = null;
            if (isHeader) {
                isHeader = false;
                continue;
            }
            String crisId = record.get("crisid");
            if (StringUtils.isBlank(crisId) || StringUtils.isBlank(prefix)) {
                continue;
            }
            if (!crisId.startsWith(prefix)) {
                continue;
            }
            String shortname = record.get("shortname");
            String dtype = record.get("dtype");
            String nestedObjectId = record.get("nested_object_id");
            String textValue = null;
            boolean isPointer = false;
            String pointerType = null;
            String authority = null;
            if (dtype.equals("text") || dtype.equals("placeholder")) {
                textValue = record.get("textvalue");
            } else if (dtype.endsWith("pointer")) {
                isPointer = true;
                pointerType = dtype.replace("pointer", "") + "value";
                String pointerId = record.get(pointerType);
                String[] pointerInfo = MigrateConnectionManager.resolvePointer(dtype.replace("pointer", ""),
                    Integer.parseInt(pointerId));
                textValue = pointerInfo[1];
                authority = "will be referenced::LEGACY-ID::" + pointerInfo[0];
            } else if (dtype.equals("date")) {
                textValue = record.get("datevalue");
            } else if (dtype.equals("boolean")) {
                textValue = record.get("booleanvalue").toLowerCase();
            } else if (dtype.equals("double")) {
                textValue = record.get("doublevalue");
            } else if (dtype.equals("link")) {
                textValue = record.get("linkdescription");
                authority = record.get("linkvalue");
                isLink = true;
            } else if (dtype.equals("eperson")) {
                String custompointer = record.get("custompointer");
                System.out.println("\tid pointer for eperson: " + custompointer);
                String[] epersoninfo = MigrateConnectionManager.resolveEpersonPointer(Long.parseLong(custompointer));
                authority = epersoninfo[0];
                textValue = epersoninfo[1];
                if (StringUtils.isBlank(epersoninfo[0])) {
                    continue;
                }
            } else if (dtype.equals("file")) {
                isFile = true;
                fileFolder = record.get("filefolder");
                fileName = record.get("filename");
                fileExtension = record.get("fileextension");
            }
            String visibility = record.get("visibility");
            if (!crisId.equals(oldCrisId)) {
                System.out.println(crisId);
                impRecord = createImpRecord(context, crisId,
                    collectionService.find(context, collectionUuidMap.get(entityType)));
                oldCrisId = crisId;
                order = 0;
                linkOrder = 0;
                lastNestedObjectId = "-1";
                createImpMetadatavalue(context, impRecord, "cris", "legacyId",
                    null, "", crisId,
                    Integer.parseInt(metadataVisibility.get(visibility)), null, 0);
                if (StringUtils.isNotBlank(record.get("sourceref")) || StringUtils.isNotBlank(record.get("sourceid"))) {
                    createImpMetadatavalue(context, impRecord, "cris", "sourceId",
                            null, "", record.get("sourceref") + "::" + record.get("sourceid"),
                            Integer.parseInt(metadataVisibility.get(visibility)), null, 0);
                }
            }
            if ("-1".equals(nestedObjectId)) {
                if (!"-1".equals(lastNestedObjectId)) {
                    order = 0;
                }
            } else {
                if (!nestedObjectId.equals(lastNestedObjectId)) {
                    order++;
                }
            }
            lastNestedObjectId = nestedObjectId;
            String[] newMetadata = null;
            if (nestedObjectId.equals("-1")) {
                newMetadata = entityMetadataMap.get(entityType).get(shortname);
            } else {
                newMetadata = entityNestedMetadataMap.get(entityType).get(shortname);
            }
            if (isFile) {
                addBitstreamToImpRecord(impRecord, newMetadata[0], newMetadata[1], newMetadata[2], fileFolder, fileName,
                    fileExtension, entityType);
            } else if (isLink && !setLinkAsAuthority) {
                createImpMetadatavalueForLink(impRecord, prefix, linkOrder, shortname, textValue, authority);
                linkOrder++;
            } else if (ArrayUtils.isNotEmpty(newMetadata) && StringUtils.isNotBlank(newMetadata[0])) {
                Arrays.asList(newMetadata).forEach(val -> System.out.println("\t" + val));
                String[] metadataField = newMetadata[0].split(Pattern.quote("."));
                checkMetadataExists(metadataField);
                checkMissingAuthority(newMetadata[0], authority);
                if (metadataField.length >= 2) {
                    createImpMetadatavalue(context, impRecord, metadataField[0], metadataField[1],
                        metadataField.length > 2 ? metadataField[2] : null, newMetadata[1], textValue,
                        Integer.parseInt(metadataVisibility.get(visibility)), authority, order == 0 ? 0 : (order - 1));
                    System.out.println("\t\tText: " + textValue + "\tAuthority: " + authority);
                }
            }
            lastNestedObjectId = nestedObjectId;
            if (++counterForCommit % 100 == 0) {
                context.commit();
            }
        }

    }

    private void createImpMetadatavalueForLink(ImpRecord impRecord, String prefix, int linkOrder, String shortname,
        String textValue, String authority) throws SQLException {

        String[] linkMetadataFields = linkMapping.get(prefix).get(shortname);
        String labelMetadata = linkMetadataFields[0];
        String valueMetadata = linkMetadataFields[1];
        checkMetadataExists(labelMetadata);
        checkMetadataExists(valueMetadata);
        System.out.println("\tSetting link to ->" + labelMetadata + " as " + textValue);
        System.out.println("\tSetting link to ->" + valueMetadata + " as " + authority);
        String[] labelMetadataSplitted = labelMetadata.split(Pattern.quote("."));
        ImpMetadatavalue impMetadatavalue = new ImpMetadatavalue();
        impMetadatavalue.setMetadatavalueId(impMedataSeq++);
        impMetadatavalue.setImpRecord(impRecord);
        impMetadatavalue.setMetadataOrder(linkOrder);

        if (StringUtils.isBlank(textValue)) {
            textValue = "#PLACEHOLDER_PARENT_METADATA_VALUE#";
        }

        impMetadatavalueService.setMetadata(impMetadatavalue, labelMetadataSplitted[0], labelMetadataSplitted[1],
            labelMetadataSplitted.length > 2 ? labelMetadataSplitted[2] : null, null, textValue);
        impMetadatavalueService.create(context, impMetadatavalue);

        String[] valueMetadataSplitted = valueMetadata.split(Pattern.quote("."));
        impMetadatavalue = new ImpMetadatavalue();
        impMetadatavalue.setMetadatavalueId(impMedataSeq++);
        impMetadatavalue.setImpRecord(impRecord);
        impMetadatavalue.setMetadataOrder(linkOrder);

        if (StringUtils.isBlank(authority)) {
            authority = "#PLACEHOLDER_PARENT_METADATA_VALUE#";
        }
        impMetadatavalueService.setMetadata(impMetadatavalue, valueMetadataSplitted[0], valueMetadataSplitted[1],
            valueMetadataSplitted.length > 2 ? valueMetadataSplitted[2] : null, null, authority);
        impMetadatavalueService.create(context, impMetadatavalue);

    }

    private void addBitstreamToImpRecord(ImpRecord impRecord, String metadataField, String language,
        String defaultValue, String fileFolder, String fileName, String fileExtension, String entity)
        throws SQLException {
        String entityPath = getPathForEntity(entity);
        ImpBitstream impBitstream = new ImpBitstream();
        impBitstream.setImpRecord(impRecord);
        impBitstream.setImpBitstreamId(impBitstreamSeq++);
        impBitstream.setBundle("ORIGINAL");
        impBitstream.setPrimaryBitstream(true);
        fileFolder += fileFolder.endsWith("/") ? "" : "/";
        String fullFileName = fileName + "." + fileExtension;
        impBitstream.setFilepath(bitstreamBasePath + entityPath + fileFolder + fullFileName);
        impBitstreamService.create(context, impBitstream);
        addBitstreamImpMetadataValue(impBitstream, fullFileName, metadataField, language, defaultValue);
    }

    private String getPathForEntity(String entity) {
        switch (entity) {
            case "Person":
                return "rp-files/";
            case "OrgUnit":
                return "ou-files/";
            case "Project":
                return "rg-files/";
            default:
                return "do-files/";
        }
    }

    private void addBitstreamImpMetadataValue(ImpBitstream impBitstream, String fullFileName, String metadataField,
        String language, String defaultValue) throws SQLException {
        ImpBitstreamMetadatavalue impMetadatavalue = new ImpBitstreamMetadatavalue();
        impMetadatavalue.setImpBitstreamMetadatavalueId(impBitstreamMetadatavalueSeq++);
        impMetadatavalue.setImpBitstream(impBitstream);
        impMetadatavalue.setMetadataOrder(0);
        String[] metadataFieldSplitted = metadataField.split(Pattern.quote("."));
        impBitstreamMetadatavalueService.setMetadata(impMetadatavalue, metadataFieldSplitted[0],
            metadataFieldSplitted[1],
            metadataFieldSplitted.length == 2 ? null : metadataFieldSplitted[3], language, defaultValue);
        impBitstreamMetadatavalueService.create(context, impMetadatavalue);
    }

    private void checkMissingAuthority(String metadataField, String authority) {
        if (StringUtils.isBlank(authority) || missingAuthority.contains(metadataField)) {
            return;
        }
        boolean authorityControlled = configurationService.getBooleanProperty("authority.controlled." + metadataField);
        if (!authorityControlled) {
            missingAuthority.add(metadataField);
        }
    }

    private void checkMetadataExists(String[] metadataField) {
        try {
            String schema = metadataField[0];
            String element = metadataField[1];
            String qualifier = metadataField.length > 2 ? metadataField[2] : null;
            String fullMetadata = schema + "." + element + (StringUtils.isBlank(qualifier) ? "" : ("." + qualifier));
            if (missingMetadata.contains(fullMetadata)) {
                return;
            }
            MetadataSchema dbSchema = schemaService.find(context, schema);
            if (dbSchema == null) {
                missingMetadata.add(fullMetadata);
                return;
            }
            MetadataField mtField = fieldService.findByString(context, fullMetadata, '.');
            if (mtField == null) {
                missingMetadata.add(fullMetadata);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkMetadataExists(String metadataField) {
        checkMetadataExists(metadataField.split(Pattern.quote(".")));
    }

    private ImpRecord createImpRecord(Context context, String impRecordKey, Collection collection) throws SQLException {
        // create imp_record records
        String sourceRecordId = impRecordKey;
        ImpRecord impRecord = new ImpRecord();
        impRecord.setImpId(impSeq++);
        impRecordService.setImpCollection(impRecord, collection);
        impRecordService.setImpEperson(impRecord, migrationEperson);
        impRecord.setImpRecordId(sourceRecordId);
        impRecord.setImpSourceref(SOURCE_REF);
        impRecordService.setStatus(impRecord, ImpRecordService.REINSTATE_WITHDRAW_ITEM_STATUS);
        impRecordService.setOperation(impRecord, ImpRecordService.INSERT_OR_UPDATE_OPERATION);

        return impRecordService.create(context, impRecord);
    }

    private ImpMetadatavalue createImpMetadatavalue(Context context, ImpRecord impRecord,
        String schema, String element, String qualifier, String language, String value, Integer securityLevel,
        String authority, Integer order)
        throws SQLException {
        ImpMetadatavalue impMetadatavalue = new ImpMetadatavalue();
        impMetadatavalue.setMetadatavalueId(impMedataSeq++);
        impMetadatavalue.setImpRecord(impRecord);
        impMetadatavalue.setMetadataOrder(order);
        impMetadatavalueService.setMetadata(impMetadatavalue, schema, element, qualifier, language, value);
        if (StringUtils.isNotBlank(authority)) {
            impMetadatavalue.setImpAuthority(authority);
            impMetadatavalue.setImpConfidence(Choices.CF_UNSET);
        }
        if (securityLevel != -1) {
            impMetadatavalue.setSecurityLevel(securityLevel);
        }

        return impMetadatavalueService.create(context, impMetadatavalue);
    }

    private Workbook createWorkbook(InputStream is) {
        try {
            return WorkbookFactory.create(is);
        } catch (EncryptedDocumentException | IOException e) {
            throw new BulkImportException("An error occurs during the workbook creation", e);
        }
    }

    private void getCollectionsFromWorkBook(Workbook workbook) {
        Sheet collectionSheet = workbook.getSheet("collections");
        Map<String, Integer> headers = getHeaderMap(collectionSheet);

        List<Row> rows = getRowsForSheet(collectionSheet);
        rows.forEach(row -> {
            collectionUuidMap.put(WorkbookUtils.getCellValue(row, 0),
                UUID.fromString(WorkbookUtils.getCellValue(row, 1)));
        });
    }

    private void getMetadataMapFromWorkBook(Workbook workbook) {
        Sheet metadataSheet = workbook.getSheet("metadata");
        Map<String, Integer> headers = getHeaderMap(metadataSheet);

        List<Row> rows = getRowsForSheet(metadataSheet);
        rows.forEach(row -> {
            String entityType = WorkbookUtils.getCellValue(row, 0);
            String oldCrisField = WorkbookUtils.getCellValue(row, 1);

            String newMetadata;
            String qualifier = WorkbookUtils.getCellValue(row, 4);
            if (StringUtils.isNotBlank(qualifier)) {
                newMetadata = StringUtils.join(new String[] { WorkbookUtils.getCellValue(row, 2),
                    WorkbookUtils.getCellValue(row, 3), qualifier }, ".");
            } else {
                newMetadata = StringUtils.join(new String[] { WorkbookUtils.getCellValue(row, 2),
                    WorkbookUtils.getCellValue(row, 3) }, ".");
            }
            if (StringUtils.isNotBlank(newMetadata.replace(".", ""))) {
                String language = WorkbookUtils.getCellValue(row, 5);
                String defaultValue = WorkbookUtils.getCellValue(row, 6);
                addEntityMetadataMap(entityType, oldCrisField, newMetadata, language, defaultValue);
            }
        });
    }

    private void getNestedMapFromWorkBook(Workbook workbook) {
        Sheet noMetadataSheet = workbook.getSheet("nested_metadata");
        Map<String, Integer> headers = getHeaderMap(noMetadataSheet);

        List<Row> rows = getRowsForSheet(noMetadataSheet);
        rows.forEach(row -> {
            String entityType = WorkbookUtils.getCellValue(row, 0);
            String oldCrisField = WorkbookUtils.getCellValue(row, 1);

            String newMetadata;
            String qualifier = WorkbookUtils.getCellValue(row, 4);
            if (StringUtils.isNotBlank(qualifier)) {
                newMetadata = StringUtils.join(new String[] { WorkbookUtils.getCellValue(row, 2),
                    WorkbookUtils.getCellValue(row, 3), qualifier }, ".");
            } else {
                newMetadata = StringUtils.join(new String[] { WorkbookUtils.getCellValue(row, 2),
                    WorkbookUtils.getCellValue(row, 3) }, ".");
            }
            String language = WorkbookUtils.getCellValue(row, 5);
            addEntityNestedMetadataMap(entityType, oldCrisField, newMetadata, language);
        });
    }

    private void getDoTypes(Workbook workbook) {
        Sheet doTypesSheet = workbook.getSheet("do_types");
        Map<String, Integer> headers = getHeaderMap(doTypesSheet);

        List<Row> rows = getRowsForSheet(doTypesSheet);
        rows.forEach(row -> {
            String entityTypeNew = WorkbookUtils.getCellValue(row, 0);
            String oldPrefix = WorkbookUtils.getCellValue(row, 1);
            doTypes.put(entityTypeNew, oldPrefix);
        });
    }

    private void getOrcidToken(Workbook workbook) {
        Sheet orcidTokenSheet = workbook.getSheet("orcid_token");
        Map<String, Integer> headers = getHeaderMap(orcidTokenSheet);

        List<Row> rows = getRowsForSheet(orcidTokenSheet);
        rows.forEach(row -> {
            String schema = WorkbookUtils.getCellValue(row, 0);
            String element = WorkbookUtils.getCellValue(row, 1);
            String qualifier = WorkbookUtils.getCellValue(row, 3);
            metadataOrcidToken.add(schema);
            metadataOrcidToken.add(element);
            if (!StringUtils.isBlank(qualifier)) {
                metadataOrcidToken.add(qualifier);
            }
        });
    }

    private void getMetadataVisibility(Workbook workbook2) {
        Sheet doTypesSheet = workbook.getSheet("metadata_visibility");
        Map<String, Integer> headers = getHeaderMap(doTypesSheet);

        List<Row> rows = getRowsForSheet(doTypesSheet);
        rows.forEach(row -> {
            String legacyValue = WorkbookUtils.getCellValue(row, 0);
            String newValue = WorkbookUtils.getCellValue(row, 1);
            metadataVisibility.put(legacyValue, newValue);
        });
    }

    private List<Row> getRowsForSheet(Sheet entityRowSheet) {
        return WorkbookUtils.getRows(entityRowSheet)
            .filter(WorkbookUtils::isNotFirstRow)
            .filter(WorkbookUtils::isNotEmptyRow)
            .collect(Collectors.toList());
    }

    private Map<String, Integer> getHeaderMap(Sheet sheet) {
        return WorkbookUtils.getCells(sheet.getRow(0))
            .filter(cell -> StringUtils.isNotBlank(getCellValue(cell)))
            .collect(toMap(cell -> getCellValue(cell), cell -> cell.getColumnIndex(), handleDuplication(sheet)));
    }

    private BinaryOperator<Integer> handleDuplication(Sheet sheet) {
        return (i1, i2) -> {
            throw new BulkImportException("Sheet " + sheet.getSheetName() + " - Duplicated headers found on cells "
                + (i1 + 1) + " and " + (i2 + 1));
        };
    }

    private void addEntityMetadataMap(String entityType, String oldCrisField, String newMetadata, String language,
        String defaultValue) {
        if (StringUtils.isBlank(entityType) || StringUtils.isBlank(oldCrisField)) {
            return;
        }
        if (!entityMetadataMap.containsKey(entityType)) {
            entityMetadataMap.put(entityType, new HashMap<String, String[]>());
        }
        entityMetadataMap.get(entityType).put(oldCrisField, new String[] { newMetadata, language, defaultValue });
    }

    private void addLinkMetadataMap(String entityPrefix, String oldCrisField, String labelMetadata,
        String valueMetadata) {
        if (StringUtils.isBlank(entityPrefix)) {
            return;
        }
        if (!linkMapping.containsKey(entityPrefix)) {
            linkMapping.put(entityPrefix, new HashMap<String, String[]>());
        }
        linkMapping.get(entityPrefix).put(oldCrisField, new String[] { labelMetadata, valueMetadata });
    }

    private void addEntityNestedMetadataMap(String entityType, String oldCrisField, String newMetadata,
        String language) {
        if (StringUtils.isBlank(entityType)) {
            return;
        }
        if (!entityNestedMetadataMap.containsKey(entityType)) {
            entityNestedMetadataMap.put(entityType, new HashMap<String, String[]>());
        }
        entityNestedMetadataMap.get(entityType).put(oldCrisField, new String[] { newMetadata, language });
    }

}
