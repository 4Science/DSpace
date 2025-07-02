/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.submission;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.submit.model.UploadConfigurationService;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link DSpaceRunnable} to perform a submission export via XLS file.
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 *
 */
public class SubmissionXmlToXls extends DSpaceRunnable<SubmissionXmlToXlsScriptConfiguration<SubmissionXmlToXls>> {

    private Context context;

    protected EPersonService epersonService;

    private ConfigurationService configurationService;

    private SubmissionConfigReader submissionConfigReader;

    private UploadConfigurationService uploadConfigurationService;

    private AuthorizeService authorizeService;

    private Map<String, DCInputsReader> inputReaders;
    private DCInputsReader defaultInputReader;
    private List<String> supportedLocales;
    private List<String> bitstreamSubmissions;
    private Map<HSSFColor.HSSFColorPredefined, CellStyle> styles;

    @Override
    @SuppressWarnings("unchecked")
    public void setup() throws ParseException {
        epersonService = EPersonServiceFactory.getInstance().getEPersonService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        uploadConfigurationService = AuthorizeServiceFactory.getInstance().getUploadConfigurationService();
        styles = new HashMap<>();
        bitstreamSubmissions = uploadConfigurationService
                .getMap()
                .values()
                .stream()
                .map(config -> config.getMetadata())
                .collect(Collectors.toList());

        supportedLocales = List.of(configurationService.getArrayProperty("inputforms.additional-languages",
                new String[]{}));
        supportedLocales = supportedLocales.stream().filter(locale -> !"en".equals(locale))
                .collect(Collectors.toList());
        try {
            // stores Steps and Submission definitions
            submissionConfigReader = new SubmissionConfigReader();
            // Stores the fields definitions for each form and the value pairs
            defaultInputReader = new DCInputsReader();
            Locale[] locales = I18nUtil.getSupportedLocales();
            inputReaders = new HashMap<>();
            for (Locale locale : locales) {
                if (!supportedLocales.contains(locale.toString())) {
                    continue; // skip unsupported locales
                }
                inputReaders.put(locale.toString(), new DCInputsReader(I18nUtil.getInputFormsFileName(locale)));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void internalRun() throws Exception {
        String locale = "en";
        DCInputsReader inputReader = inputReaders.get(locale);
        if (inputReader == null) {
            inputReader = defaultInputReader;
        }

        // Step definitions
        Map<String, Map<String, String>> steps = new TreeMap<>(submissionConfigReader.getSafeStepDefns());

        // Submission definitions
        Map<String, List<Map<String, String>>> subDefs = new TreeMap<>(submissionConfigReader.getSafeSubmitDefns());

        // Form definitions
        Map<String, List<List<Map<String, String>>>> formDef = new TreeMap<>(inputReader.getFormDefns());

        // Value pairs
        Map<String, List<String>> valuePairs = new TreeMap<>(inputReader.getSafeValuePairs());

        context = new Context();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();) {
            Workbook workbook = getTemplateWorkBook();

            processSheetSubmissionDefinitions(workbook, subDefs);
            processSheetSteps(workbook, steps);
            processSheetFormsDefinitions(workbook, formDef);
            processSheetFormValuePairs(workbook, valuePairs);

            workbook.write(out);

            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            handler.writeFilestream(context, "submission-form.xls", in, "application/vnd.ms-excel");

            handler.logInfo("Successfully exported submission form into file named submission-form.xls");
        } catch (Exception e) {
            handler.handleException(e);
        }

        try {
            context.complete();
        } catch (Exception e) {
            handler.handleException(e);
            context.abort();
        }
    }

    private void processSheetFormValuePairs(Workbook workbook, Map<String, List<String>> valuePairs) {
        Sheet sheet = workbook.getSheet("forms-value-pairs");
        Row header = sheet.createRow(sheet.getLastRowNum() + 1);
        AtomicInteger headerColumnsCount = new AtomicInteger();
        List<String> orderedValuePairs = new ArrayList<>(valuePairs.keySet());
        orderedValuePairs.sort(Comparator.naturalOrder());
        for (String valuePairName : orderedValuePairs) {
            if (StringUtils.isBlank(valuePairName)) {
                continue; // skip empty value pair names
            }
            List<String> pairs = valuePairs.get(valuePairName);

            int valuePairsColumns = supportedLocales.size() + 2;
            int valuePairSize = pairs.size() / 2;
            checkRowExists(sheet, valuePairSize);

            // create display label column
            header.createCell(headerColumnsCount.getAndIncrement()).setCellValue(valuePairName);
            // Create header cells for each locale except "en"
            supportedLocales.stream()
                    .forEach(locale ->
                            header.createCell(headerColumnsCount.getAndIncrement())
                                    .setCellValue(valuePairName + "_" + locale));
            //this is the "en" column
            header.createCell(headerColumnsCount.getAndIncrement()).setCellValue(valuePairName);

            // <stored-value>, <display-value-lang1>, <display-value-lang2>, <display-value-lang3>
            String[][] storedAndDisplay = new String[pairs.size() / 2][valuePairsColumns];

            // this for loop will handle the Display values across languages
            for (int i = 0; i < pairs.size(); i += 2) {
                storedAndDisplay[i / 2][0] = pairs.get(i); // display value
                for (int j = 1; j <= supportedLocales.size(); j++) {
                    //multilanguage store values
                    storedAndDisplay[i / 2][j] = inputReaders.get(supportedLocales.get(j - 1))
                            .getPairs(valuePairName).get(i);
                }
                // let's set the store values
                storedAndDisplay[i / 2][storedAndDisplay[i / 2].length - 1] = pairs.get(i + 1);
            }


            //set values
            for (int i = 0; i < storedAndDisplay.length; i++) {
                for (int j = 0; j < storedAndDisplay[i].length; j++) {
                    AtomicInteger column = new AtomicInteger(headerColumnsCount.get() - valuePairsColumns
                            + (j % valuePairsColumns));
                    String value = storedAndDisplay[i][j];
                    // i + 1 because the first row is the header
                    sheet.getRow(i + 1).createCell(column.get()).setCellValue(value);
                    sheet.autoSizeColumn(column.get());
                }
            }
        }

    }

    private void checkRowExists(Sheet sheet, int valuePairSize) {
        if (sheet.getLastRowNum() <= valuePairSize) {
            int previousRowCount = sheet.getLastRowNum();
            for (int i = 0; i <= (valuePairSize - previousRowCount); i++) {
                // Create empty rows until we reach the desired size
                sheet.createRow(sheet.getLastRowNum() + 1);
            }
        }
    }

    private void processSheetFormsDefinitions(Workbook workbook, Map<String, List<List<Map<String, String>>>> formDef) {
        Sheet sheet = workbook.getSheet("forms-definition");
        Row header = sheet.createRow(sheet.getLastRowNum() + 1);
        AtomicInteger columnIndex = new AtomicInteger(0);
        header.createCell(columnIndex.getAndIncrement()).setCellValue("form-name");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("row-number");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("field-style");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("parent");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("schema");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("dc-element");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("dc-qualifier");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("input-type");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("list-name");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("validation");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("repeatable");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("restriction");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("label");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("required");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("hint");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("type-bind");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("displayitem");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("formatter");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("vocabulary");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("closedvocabulary");
        header.createCell(columnIndex.getAndIncrement()).setCellValue("multilanguage-value-pairs");

        // Add columns for each supported locale
        supportedLocales.stream().filter(locale -> !locale.equalsIgnoreCase("en"))
                .forEach(locale -> {
                    header.createCell(columnIndex.getAndIncrement()).setCellValue("label_" + locale);
                    header.createCell(columnIndex.getAndIncrement()).setCellValue("required_" + locale);
                    header.createCell(columnIndex.getAndIncrement()).setCellValue("hint_" + locale);
                });

        for (Map.Entry<String, List<List<Map<String, String>>>> entry : formDef.entrySet()) {
            String submissionDef = entry.getKey();
            int formRowCounter = 1;

            HSSFColor.HSSFColorPredefined style = randomColor();
            for (List<Map<String, String>> formRow : entry.getValue()) {
                for (Map<String, String> field : formRow) {
                    int rowIndex = sheet.getLastRowNum() + 1;
                    Row row = sheet.createRow(rowIndex);
                    AtomicInteger columnCount = new AtomicInteger(0);

                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(submissionDef);
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(formRowCounter++);
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(field.get("style"));
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(calculateParent(submissionDef));
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(field.get("dc-schema"));
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(field.get("dc-element"));
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(field.get("dc-qualifier"));
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(field.get("input-type"));
                    String listName = field.get(field.get("input-type") + ".value-pairs-name");
                    if (StringUtils.isBlank(listName)) {
                        listName = field.get("value-pairs-name");
                    }
                    boolean isValuePairsMultilanguage = !StringUtils.equalsAny(field.get("input-type"),
                            "dropdown", "qualdrop_value", "list", "series");
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(isValuePairsMultilanguage ? "" :
                            listName);
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(field.get("validation"));
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(field.get("repeatable"));
                    String restriction = field.get("visibility");
                    if (StringUtils.isBlank(restriction)) {
                        restriction = StringUtils.equals(field.get("readonly"), "all")
                                ? "readonly" : field.get("readonly");
                    } else {
                        restriction = "hidden";
                    }
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(restriction);
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(field.get("label"));
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(field.get("required"));
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(field.get("hint"));
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(field.get("type-bind"));
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(field.get("displayitem"));
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(field.get("formatter"));
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(field.get("vocabulary"));
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(field.get("closedVocabulary"));
                    applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                            style).setCellValue(isValuePairsMultilanguage ?
                            listName : "");

                    supportedLocales.forEach(locale -> {
                        try {
                            DCInput dcInput = Arrays.stream(inputReaders.get(locale).getInputsByFormName(submissionDef)
                                            .getFields())
                                    .flatMap(dcInputs -> Arrays.stream(dcInputs).sequential())
                                    .filter(dc -> StringUtils.equals(dc.getSchema(), field.get("dc-schema"))
                                            && StringUtils.equals(dc.getElement(), field.get("dc-element"))
                                            && StringUtils.equals(dc.getQualifier(), field.get("dc-qualifier")))
                                    .findFirst().get();

                            String label = dcInput.getLabel();
                            String required = String.valueOf(dcInput.isRequired());
                            String hint = dcInput.getHints();

                            applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                                    style).setCellValue(thisOrEmpty(label));
                            applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                                    style).setCellValue(thisOrEmpty(required));
                            applyStyleToCell(workbook, row.createCell(columnCount.getAndIncrement()),
                                    style).setCellValue(thisOrEmpty(hint));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

                }
            }

        }
        for (int i = 0; i <= columnIndex.get() + (supportedLocales.size() * 3); i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String calculateParent(String submissionDef) {
        if (StringUtils.isBlank(submissionDef) || !submissionDef.contains("-")
                || StringUtils.countMatches(submissionDef, "-") < 2
                || bitstreamSubmissions.contains(submissionDef)) {
            return "";
        }
        int firstDash = submissionDef.indexOf('-');
        String result = (firstDash != -1) ? submissionDef.substring(firstDash + 1) : submissionDef;
        return result.replace("-","_");
    }

    private void processSheetSteps(Workbook workbook, Map<String, Map<String, String>> steps) {
        Sheet sheet = workbook.getSheet("steps-definition");
        Row header = sheet.createRow(sheet.getLastRowNum() + 1);
        header.createCell(0).setCellValue("step-id");
        header.createCell(1).setCellValue("step-type");
        header.createCell(2).setCellValue("required");
        header.createCell(3).setCellValue("restriction");
        header.createCell(4).setCellValue("opened");

        for (Map.Entry<String, Map<String, String>> entry : steps.entrySet()) {
            String stepId = entry.getKey();
            Map<String, String> stepEntry = entry.getValue();
            HSSFColor.HSSFColorPredefined style = randomColor();

            int rowIndex = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(rowIndex);

            applyStyleToCell(workbook, row.createCell(0),
                    style).setCellValue(thisOrEmpty(stepId));
            applyStyleToCell(workbook, row.createCell(1),
                    style).setCellValue(thisOrEmpty(stepEntry.get("type")));
            applyStyleToCell(workbook, row.createCell(2),
                    style).setCellValue(thisOrEmpty(stepEntry.get("mandatory")));
            applyStyleToCell(workbook, row.createCell(3),
                    style).setCellValue(thisOrEmpty(reconstructRestrictions(stepEntry)));
            applyStyleToCell(workbook, row.createCell(4),
                    style).setCellValue(thisOrEmpty(stepEntry.get("opened")));
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
    }

    private void processSheetSubmissionDefinitions(Workbook workbook, Map<String, List<Map<String, String>>> subDefs) {
        Sheet sheet = workbook.getSheet("submissions-definition");
        Row header = sheet.createRow(sheet.getLastRowNum() + 1);
        header.createCell(0).setCellValue("submission-name");
        header.createCell(1).setCellValue("step-id");
        header.createCell(2).setCellValue("order");

        for (Map.Entry<String, List<Map<String, String>>> entry : subDefs.entrySet()) {
            String submissionName = entry.getKey();
            List<Map<String, String>> submissionSteps = entry.getValue();

            int order = 1;
            HSSFColor.HSSFColorPredefined style = randomColor();
            for (Map<String, String> stepEntry : submissionSteps) {
                String stepId = stepEntry.get("id");

                int rowIndex = sheet.getLastRowNum() + 1;
                Row row = sheet.createRow(rowIndex);

                applyStyleToCell(workbook, row.createCell(0), style).setCellValue(thisOrEmpty(submissionName));
                applyStyleToCell(workbook, row.createCell(1), style).setCellValue(thisOrEmpty(stepId));
                applyStyleToCell(workbook, row.createCell(2), style).setCellValue(thisOrEmpty(order++));
            }

        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
    }

    private Cell applyStyleToCell(Workbook workbook, Cell cell, HSSFColor.HSSFColorPredefined hssfColor) {
        if (styles.containsKey(hssfColor)) {
            cell.setCellStyle(styles.get(hssfColor));
        } else {
            CellStyle cs = workbook.createCellStyle();
            cs.setFillForegroundColor(hssfColor.getIndex());
            cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cell.setCellStyle(cs);
            styles.put(hssfColor, cs);
        }
        return cell;
    }

    public static HSSFColor.HSSFColorPredefined randomColor() {
        HSSFColor.HSSFColorPredefined randomColor;
        do {
            HSSFColor.HSSFColorPredefined[] values = HSSFColor.HSSFColorPredefined.values();
            randomColor = values[new Random().nextInt(values.length)];
        } while (randomColor == HSSFColor.HSSFColorPredefined.BLACK);
        return randomColor;
    }

    public String reconstructRestrictions(Map<String, String> map) {
        String scope = map.getOrDefault("scope", "").trim();
        String visibility = map.getOrDefault("scope.visibility", "").trim();
        String visibilityOutside = map.getOrDefault("scope.visibilityOutside", "").trim();

        // Caso 1: casi speciali "hidden" e "readonly"
        if ("submission".equalsIgnoreCase(scope)) {
            if ("hidden".equalsIgnoreCase(visibility) && "hidden".equalsIgnoreCase(visibilityOutside)) {
                return "hidden";
            }
            if ("read-only".equalsIgnoreCase(visibility) && "read-only".equalsIgnoreCase(visibilityOutside)) {
                return "readonly";
            }
        }

        boolean isLimited = "hidden".equalsIgnoreCase(visibilityOutside);

        List<String> parts = new ArrayList<>();

        if (isLimited) {
            parts.add("limited to");
        }

        if (!scope.isEmpty()) {
            parts.add(scope);
        }

        if ("hidden".equalsIgnoreCase(visibility)) {
            parts.add("hidden");
        } else if ("read-only".equalsIgnoreCase(visibility)) {
            parts.add("readonly");
        }

        return String.join(" ", parts).trim();
    }


    private String normalizeVisibility(String visibility) {
        if ("read-only".equalsIgnoreCase(visibility)) {
            return "readonly";
        }
        return visibility.toLowerCase(); // es. "hidden"
    }



    private String thisOrEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private Workbook getTemplateWorkBook() {
        Workbook workbook = new HSSFWorkbook();

        workbook.createSheet("submissions-definition");
        workbook.createSheet("steps-definition");
        workbook.createSheet("forms-definition");
        workbook.createSheet("forms-value-pairs");

        return workbook;
    }

    protected boolean isAuthorized(Context context) {
        try {
            return authorizeService.isAdmin(context);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public SubmissionXmlToXlsScriptConfiguration<SubmissionXmlToXls> getScriptConfiguration() {
        return new DSpace().getServiceManager()
                .getServiceByName("submission-xml-to-xls", SubmissionXmlToXlsScriptConfiguration.class);
    }

}
