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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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

    private AuthorizeService authorizeService;

    private Map<String, DCInputsReader> inputReaders;
    private DCInputsReader defaultInputReader;
    private List<String> supportedLocales;

    @Override
    @SuppressWarnings("unchecked")
    public void setup() throws ParseException {
        this.epersonService = EPersonServiceFactory.getInstance().getEPersonService();
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

        supportedLocales = List.of(configurationService.getArrayProperty("webui.supported.locales",
                new String[]{"en"}));
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
        Map<String, Map<String, String>> steps = submissionConfigReader.getSafeStepDefns();

        // Submission definitions
        Map<String, List<Map<String, String>>> subDefs = submissionConfigReader.getSafeSubmitDefns();

        // Form definitions
        Map<String, List<List<Map<String, String>>>> formDef = inputReader.getFormDefns();

        // Value pairs
        Map<String, List<String>> valuePairs = inputReader.getSafeValuePairs();

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
        int rowIndex = sheet.getLastRowNum();
        for (String valuePairName : valuePairs.keySet()) {
            List<String> pairs = valuePairs.get(valuePairName);

            int valuePairsColumns = supportedLocales.size() + 1;
            int valuePairSize = pairs.size() / valuePairsColumns;
            checkRowExists(sheet, valuePairSize);

            // create display label column
            header.createCell(headerColumnsCount.getAndIncrement()).setCellValue(valuePairName);
            // Create header cells for each locale except "en"
            supportedLocales.stream()
                    .filter(locale -> !locale.equalsIgnoreCase("en"))
                    .forEach(locale ->
                            header.createCell(headerColumnsCount.getAndIncrement())
                                    .setCellValue(valuePairName + "_" + locale));
            //this is the "en" column
            header.createCell(headerColumnsCount.getAndIncrement()).setCellValue(valuePairName);

            //set values
            int i = 0;
            for (String value : pairs) {
                AtomicInteger column = new AtomicInteger(headerColumnsCount.get() - valuePairsColumns
                        + (i % valuePairsColumns));
                sheet.getRow((i / valuePairsColumns) + 1).createCell(column.get()).setCellValue(value);
                i++;
                sheet.autoSizeColumn(column.get());
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
        header.createCell(0).setCellValue("form-name");
        header.createCell(1).setCellValue("row-number");
        header.createCell(2).setCellValue("field-style");
        header.createCell(3).setCellValue("parent");
        header.createCell(4).setCellValue("schema");
        header.createCell(5).setCellValue("dc-element");
        header.createCell(6).setCellValue("dc-qualifier");
        header.createCell(7).setCellValue("input-type");
        header.createCell(8).setCellValue("list-name");
        header.createCell(9).setCellValue("validation");
        header.createCell(10).setCellValue("repeatable");
        header.createCell(11).setCellValue("restriction");
        header.createCell(12).setCellValue("label");
        header.createCell(13).setCellValue("required");
        header.createCell(14).setCellValue("hint");
        header.createCell(15).setCellValue("type-bind");
        header.createCell(16).setCellValue("displayitem");
        header.createCell(17).setCellValue("formatter");
        header.createCell(18).setCellValue("vocabulary");
        header.createCell(19).setCellValue("closedvocabulary");
        header.createCell(20).setCellValue("multilanguage-value-pairs");

        AtomicInteger lastColumnIndex = new AtomicInteger(20);

        supportedLocales.stream().filter(locale -> !locale.equalsIgnoreCase("en"))
                .forEach(locale -> {
                    header.createCell(lastColumnIndex.incrementAndGet()).setCellValue("label_" + locale);
                    header.createCell(lastColumnIndex.incrementAndGet()).setCellValue("required_" + locale);
                    header.createCell(lastColumnIndex.incrementAndGet()).setCellValue("hint_" + locale);
                });

        for (Map.Entry<String, List<List<Map<String, String>>>> entry : formDef.entrySet()) {
            String submissionDef = entry.getKey();
            int formRowCounter = 1;

            for (List<Map<String, String>> formRow : entry.getValue()) {
                for (Map<String, String> field : formRow) {
                    int rowIndex = sheet.getLastRowNum() + 1;
                    Row row = sheet.createRow(rowIndex);
                    AtomicInteger columnIndex = new AtomicInteger(0);

                    row.createCell(columnIndex.getAndIncrement()).setCellValue(submissionDef);
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(formRowCounter++);
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(field.get("style"));
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(calculateParent(submissionDef));
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(field.get("dc-schema"));
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(field.get("dc-element"));
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(field.get("dc-qualifier"));
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(field.get("input-type"));
                    String listName = field.get(field.get("input-type") + ".value-pairs-name");
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(listName);
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(field.get("validation"));
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(field.get("repeatable"));
                    String restriction = field.get("visibility");
                    if (StringUtils.isBlank(restriction)) {
                        restriction = StringUtils.equals(field.get("readonly"), "all")
                                ? "readonly" : field.get("readonly");
                    } else {
                        restriction = "hidden";
                    }
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(restriction);
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(field.get("label"));
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(field.get("required"));
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(field.get("hint"));
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(field.get("type-bind"));
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(field.get("displayitem"));
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(field.get("formatter"));
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(field.get("vocabulary"));
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(field.get("closedVocabulary"));
                    row.createCell(columnIndex.getAndIncrement()).setCellValue(field.get("multilanguage-value-pairs"));

                    // TODO
                    // FIXME: we must handle the multilanguage columns

                }
            }

        }
        for (int i = 0; i <= lastColumnIndex.get() + (supportedLocales.size() * 3); i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String calculateParent(String submissionDef) {
        if (StringUtils.isBlank(submissionDef) || !submissionDef.contains("-")
                || StringUtils.countMatches(submissionDef, "-") < 2) {
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

            int rowIndex = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(rowIndex);

            row.createCell(0).setCellValue(thisOrEmpty(stepId));
            row.createCell(1).setCellValue(thisOrEmpty(stepEntry.get("type")));
            row.createCell(2).setCellValue(thisOrEmpty(stepEntry.get("mandatory")));
            row.createCell(3).setCellValue(thisOrEmpty(reconstructRestrictions(stepEntry)));
            row.createCell(4).setCellValue(thisOrEmpty(stepEntry.get("opened")));
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
            for (Map<String, String> stepEntry : submissionSteps) {
                String stepId = stepEntry.get("id");

                int rowIndex = sheet.getLastRowNum() + 1;
                Row row = sheet.createRow(rowIndex);

                row.createCell(0).setCellValue(thisOrEmpty(submissionName));
                row.createCell(1).setCellValue(thisOrEmpty(stepId));
                row.createCell(2).setCellValue(thisOrEmpty(order++));
            }

        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
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
