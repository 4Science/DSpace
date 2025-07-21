/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submission;

import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.I18nUtil;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for the `submission-xml-to-xls` script.
 * This test verifies the correctness of the exported XLS file
 * generated from submission XML configurations.
 * <p>
 * The test checks various aspects of the exported file, including:
 * <ul>
 *   <li>Steps definitions</li>
 *   <li>Submission definitions</li>
 *   <li>Form value pairs</li>
 *   <li>Form definitions</li>
 *   <li>Fixed values</li>
 * </ul>
 * </p>
 * <p>
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 *
 */
public class SubmissionXmlToXlsScriptIT extends AbstractIntegrationTestWithDatabase {

    private DCInputsReader defaultInputReader;
    private SubmissionConfigReader submissionConfigReader;
    private ConfigurationService configurationService;

    @After
    public void after() throws SQLException, AuthorizeException {
    }

    @Before
    public void before() throws Exception {
        this.defaultInputReader = new DCInputsReader();
        this.submissionConfigReader = new SubmissionConfigReader();
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    }

    @Test
    public void checkExportedSubmissionXLS() throws Exception {
        context.turnOffAuthorisationSystem();

        String[] args = new String[] { "submission-xml-to-xls" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        File exportedFile = new File("submission-form.xls");
        exportedFile.deleteOnExit();

        Map<String, Map<String, String>> steps = new TreeMap<>(this.submissionConfigReader.getSafeStepDefns());
        Map<String, List<Map<String, String>>> subDefs =
                new TreeMap<>(this.submissionConfigReader.getSafeSubmitDefns());
        Map<String, List<String>> valuePairs = new TreeMap<>(defaultInputReader.getSafeValuePairs());


        assertThat(handler.getFileStream(context, "submission-form.xls").isPresent(), is(true));
        try (InputStream fisExported = handler.getFileStream(context, "submission-form.xls").get()) {
            Workbook exportedWorkbook = WorkbookFactory.create(fisExported);

            // check the number of sheets is equal to 4
            assertThat(exportedWorkbook.getNumberOfSheets(), is(4));

            // steps definition check
            Sheet stepsDefinitions = exportedWorkbook.getSheet("steps-definition");
            // row count is already -1 there's no need to + 1 for header row
            assertThat(stepsDefinitions.getLastRowNum(), is(steps.size()));
            // check step ids
            assertThat(getValues(getColumnValues(stepsDefinitions, 0, true)),
                    contains(getValues(steps.keySet().stream())));
            // check step types
            assertThat(getValues(getColumnValues(stepsDefinitions, 1, true)),
                    containsInAnyOrder(getValues(steps
                            .values()
                            .stream()
                            .map(obj -> obj.get("type")))));
            // check step mandatory fields
            assertThat(getValues(getColumnValues(stepsDefinitions, 2, true)),
                    containsInAnyOrder(getValues(steps
                            .values()
                            .stream()
                            .map(obj -> obj.get("mandatory")))));

            // check sheet 2 for form definitions
            Sheet submissionsDefinitions = exportedWorkbook.getSheet("submissions-definition");
            assertThat(getValuesNoDuplicates(getColumnValues(submissionsDefinitions, 0, true)),
                    contains(getValues(subDefs.keySet().stream())));
            // check all IDs are found in the excel
            assertThat(getValues(getColumnValues(submissionsDefinitions, 1, true)),
                    contains(getValues(subDefs
                            .values()
                            .stream()
                            .flatMap(obj -> obj
                                    .stream()
                                    .map(entry -> entry.get("id"))))));

            // check form value pairs sheet
            Sheet valuePairsSheet = exportedWorkbook.getSheet("forms-value-pairs");
            // check max number of rows equal to the max number of values in value pairs
            assertThat(valuePairsSheet.getLastRowNum(), is(valuePairs.values()
                    .stream()
                    .map(List::size)
                    .map(size -> size / 2 + 1) // each value pair list has 2 values (Display and stored value)
                    .max(Integer::compareTo)
                    .orElse(1)));
            int additionalLanguagesCount = configurationService.getArrayProperty("inputforms.additional-languages",
                    new String[0]).length;
            int columnCount = getColumnCount(valuePairsSheet);
            int expectedColumnsPerEntry = 2 + additionalLanguagesCount;
            // check the number of columns is equal to the number of value pairs (2 is minimum) + additional languages
            assertThat(valuePairs.size(), is((columnCount / expectedColumnsPerEntry)));

        }

        context.restoreAuthSystemState();
    }


    @Test
    public void checkExportedSubmissionXLSValuePairsValue() throws Exception {
        context.turnOffAuthorisationSystem();

        String[] args = new String[] { "submission-xml-to-xls" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        File exportedFile = new File("submission-form.xls");
        exportedFile.deleteOnExit();

        Map<String, List<String>> valuePairs = new TreeMap<>(defaultInputReader.getSafeValuePairs());

        assertThat(handler.getFileStream(context, "submission-form.xls").isPresent(), is(true));

        try (InputStream fisExported = handler.getFileStream(context, "submission-form.xls").get()) {
            Workbook exportedWorkbook = WorkbookFactory.create(fisExported);

            // check form value pairs sheet
            Sheet valuePairsSheet = exportedWorkbook.getSheet("forms-value-pairs");
            // check max number of rows equal to the max number of values in value pairs
            assertThat(valuePairsSheet.getLastRowNum(), is(valuePairs.values()
                    .stream()
                    .map(List::size)
                    .map(size -> size / 2 + 1) // each value pair list has 2 values (Display and stored value)
                    .max(Integer::compareTo)
                    .orElse(1)));
            String[] additionalLanguages = configurationService.getArrayProperty("inputforms.additional-languages",
                    new String[0]);
            valuePairs.keySet().stream().findAny().ifPresent(randomValuePairsName -> {
                System.out.println("Random value pairs name: " + randomValuePairsName);
                List<List<String>> valuePairsStandardValues =
                        getColumnValuesByColoumnName(valuePairsSheet, randomValuePairsName);
                // pos 0 is Display value, pos 1 is stored value
                assertThat(valuePairsStandardValues.size(), is(2));

                Iterator<String> systemValuePairsIterator = valuePairs.get(randomValuePairsName).iterator();
                assertThat(valuePairsStandardValues.get(0).size(), is(valuePairs.get(randomValuePairsName).size() / 2));
                assertThat(valuePairsStandardValues.get(1).size(), is(valuePairs.get(randomValuePairsName).size() / 2));
                // check the values of the value pair match the values in the system
                valuePairsStandardValues.get(0).forEach(displayVal -> {
                    assertThat(displayVal, is(systemValuePairsIterator.next()));
                    systemValuePairsIterator.next(); // skip the stored value
                });


                Map<String, List<String>> additionalValues = new HashMap<>();
                Map<String, DCInputsReader> inputReaders = new HashMap();


                if (ArrayUtils.isNotEmpty(additionalLanguages)) {
                    for (String lang : additionalLanguages) {
                        List<List<String>> additionalValuesByLang =
                                getColumnValuesByColoumnName(valuePairsSheet, randomValuePairsName + "_" + lang );
                        assertThat(additionalValuesByLang.size(), is(1));
                        additionalValues.put(lang, additionalValuesByLang.get(0));
                        try {
                            inputReaders.put(lang,
                                    new DCInputsReader(I18nUtil.getInputFormsFileName(Locale.forLanguageTag(lang))));
                        } catch (DCInputsReaderException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                // check the values of the value pair match the values in the system
                additionalValues.forEach((lang, values) -> {
                    Map<String, List<String>> valuePairsInLang =
                            new TreeMap<>(inputReaders.get(lang).getSafeValuePairs());
                    valuePairsInLang.keySet().stream().findAny().ifPresent(randomValuePairsNameInLang -> {

                        System.out.println("Random value pairs name: " + randomValuePairsNameInLang);
                        assertThat(values.size(), is(inputReaders.get(lang).getSafeValuePairs()
                                .get(randomValuePairsNameInLang).size() / 2));
                        Iterator<String> systemValuePairsIteratorLang = valuePairsInLang
                                .get(randomValuePairsNameInLang).iterator();
                        values.forEach(displayVal -> {
                            assertThat(displayVal, is(systemValuePairsIteratorLang.next()));
                            systemValuePairsIteratorLang.next(); // skip the stored value
                        });
                    });
                });
            });


        }

        context.restoreAuthSystemState();
    }

    @Test
    public void checkExportedSubmissionFormDefinitionXLS() throws Exception {
        context.turnOffAuthorisationSystem();

        String[] args = new String[] { "submission-xml-to-xls" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        File exportedFile = new File("submission-form.xls");
        exportedFile.deleteOnExit();

        Map<String, List<List<Map<String, String>>>> formDef = new TreeMap<>(defaultInputReader.getFormDefns());


        assertThat(handler.getFileStream(context, "submission-form.xls").isPresent(), is(true));
        try (InputStream fisExported = handler.getFileStream(context, "submission-form.xls").get()) {
            Workbook exportedWorkbook = WorkbookFactory.create(fisExported);

            // check the number of sheets is equal to 4
            assertThat(exportedWorkbook.getNumberOfSheets(), is(4));

            // steps definition check
            Sheet formsDefinitions = exportedWorkbook.getSheet("forms-definition");
            String[] additionalLanguages = configurationService.getArrayProperty("inputforms.additional-languages",
                    new String[0]);
            if (ArrayUtils.isNotEmpty(additionalLanguages)) {
                assertThat(getColumnValuesByColoumnName(formsDefinitions, "multilanguage-value-pairs"),
                        is(notNullValue()));
            }
            Arrays.stream(additionalLanguages).forEach(lang -> {
                // check the sheet contains the additional languages columns
                assertThat(getColumnValuesByColoumnName(formsDefinitions, "label_" + lang), is(notNullValue()));
                assertThat(getColumnValuesByColoumnName(formsDefinitions, "required_" + lang), is(notNullValue()));
                assertThat(getColumnValuesByColoumnName(formsDefinitions, "hint_" + lang), is(notNullValue()));
            });
            // check column exists
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "form-name"), is(notNullValue()));
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "row-number"), is(notNullValue()));
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "field-style"), is(notNullValue()));
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "parent"), is(notNullValue()));
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "schema"), is(notNullValue()));
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "dc-element"), is(notNullValue()));
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "dc-qualifier"), is(notNullValue()));
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "input-type"), is(notNullValue()));
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "list-name"), is(notNullValue()));
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "validation"), is(notNullValue()));
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "repeatable"), is(notNullValue()));
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "restriction"), is(notNullValue()));
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "label"), is(notNullValue()));
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "required"), is(notNullValue()));
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "hint"), is(notNullValue()));
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "type-bind"), is(notNullValue()));
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "displayitem"), is(notNullValue()));
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "formatter"), is(notNullValue()));
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "vocabulary"), is(notNullValue()));
            assertThat(getColumnValuesByColoumnName(formsDefinitions, "closedvocabulary"), is(notNullValue()));

            // check the sheet contains all the expected forms definitions
            assertThat(
                getColumnValuesByColoumnName(formsDefinitions, "form-name")
                    .stream().findFirst().orElse(Collections.emptyList())
                    .stream().distinct().collect(Collectors.toList()),
                containsInAnyOrder(getValues(formDef.keySet().stream()))
            );

            for (Row row : formsDefinitions) {
                if (row.getRowNum() == 0) {
                    continue; // skip header row
                }
                String form = getCellStringValue(row.getCell(0));
                String schema = getCellStringValue(row.getCell(4));
                String element = getCellStringValue(row.getCell(5));
                String qualifier = getCellStringValue(row.getCell(6));
                String type = getCellStringValue(row.getCell(7));

                Optional<Map<String, String>> field = formDef.get(form).stream()
                        .flatMap(Collection::stream)
                        .filter(Objects::nonNull)
                        .filter(def ->
                                StringUtils.equals(def.get("dc-schema"), schema)
                                && StringUtils.equals(def.get("dc-element"), element)
                                && StringUtils.equals(def.get("dc-qualifier"), qualifier)
                                        && StringUtils.equals(def.get("input-type"), type))
                        .findFirst();
                assertThat(field.isPresent(), is(true));

            }
        }

        context.restoreAuthSystemState();
    }

    @Test
    public void checkExportedSubmissionXLSFixedValues() throws Exception {
        context.turnOffAuthorisationSystem();

        String[] args = new String[]{"submission-xml-to-xls"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        File exportedFile = new File("submission-form.xls");
        exportedFile.deleteOnExit();

        assertThat(handler.getFileStream(context, "submission-form.xls").isPresent(), is(true));
        try (InputStream fisExported = handler.getFileStream(context, "submission-form.xls").get()) {
            Workbook exportedWorkbook = WorkbookFactory.create(fisExported);

            String[][] expectedValues = {
                    {"bitstream-metadata", "dc", "title", null, "onebox", null},
                    {"bitstream-metadata", "dc", "description", null, "textarea", null},
                    {"traditionalpageone", "dc", "title", null, "onebox", null},
                    {"traditionalpageone", "dc", "title", "alternative", "onebox", null},
                    {"traditionalpageone", "dc", "date", "issued", "date", "col-sm-4", null},
                    {"traditionalpageone-cris", "dc", "contributor", "author", "group", null},
            };

            // check the number of sheets is equal to 4
            assertThat(exportedWorkbook.getNumberOfSheets(), is(4));

            // steps definition check
            Sheet formsDefinitions = exportedWorkbook.getSheet("forms-definition");
            boolean matchCheck = Arrays.stream(expectedValues).allMatch(array ->
                    StreamSupport.stream(formsDefinitions.spliterator(), false)
                            .anyMatch(row -> {
                                if (row.getRowNum() != 0) {
                                    String form = getCellStringValue(row.getCell(0));
                                    String schema = getCellStringValue(row.getCell(4));
                                    String element = getCellStringValue(row.getCell(5));
                                    String qualifier = getCellStringValue(row.getCell(6));
                                    String type = getCellStringValue(row.getCell(7));
                                    String style = getCellStringValue(row.getCell(2));

                                    return StringUtils.equals(array[0], form)
                                            && StringUtils.equals(array[1], schema)
                                            && StringUtils.equals(array[2], element)
                                            && StringUtils.equals(array[3], qualifier)
                                            && StringUtils.equals(array[4], type)
                                            && StringUtils.equals(array[5], style);

                                }
                                return false;
                            })
            );
            assertThat(matchCheck, is(true));
        }

        context.restoreAuthSystemState();
    }

    private List<String> getValuesNoDuplicates(List<String> obj) {
        return obj.stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> getValues(List<String> obj) {
        return obj.stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    private String[] getValues(Stream<String> obj) {
        return obj.filter(StringUtils::isNotBlank).toArray(String[]::new);
    }



    private List<List<String>> getColumnValuesByColoumnName(Sheet sheet, String columnName) {
        Row row = sheet.getRow(0); //headers are always in the first row
        List<List<String>> values = new ArrayList<>();
        boolean exists = false;
        for (Cell cell : row) {
            if (cell.getStringCellValue().equalsIgnoreCase(columnName)) {
                exists = true;
                int columnIndex = cell.getColumnIndex();
                values.add(getColumnValues(sheet, columnIndex, false));
            }
        }
        return exists ? values : null; // return empty list if column found, null if not found
    }

    private List<String> getColumnValues(Sheet sheet, int columnIndex, boolean skipBlankCells) {
        List<String> values = new ArrayList<>();
        boolean isHeaderRow = true;
        for (Row row : sheet) {
            if (isHeaderRow) {
                isHeaderRow = false; // skip header row
                continue;
            }
            Cell cell = row.getCell(columnIndex);
            if (cell != null) {
                String value = getCellStringValue(cell);
                if (skipBlankCells && StringUtils.isBlank(value)) {
                    continue; // skip blank cells
                }
                values.add(value);
            }
        }
        return values;
    }

    private int getColumnCount(Sheet sheet) {
        int columnIndex = 0;
        boolean isEmpty;
        Row row = sheet.getRow(0);
        do {
            Cell cell = row.getCell(columnIndex++);
            isEmpty = cell == null || StringUtils.isEmpty(getCellStringValue(cell));
        } while (!isEmpty);
        return columnIndex;
    }


    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return Double.toString(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue(); // fallback
                } catch (IllegalStateException e) {
                    return Double.toString(cell.getNumericCellValue());
                }
            case BLANK:
            case ERROR:
            case _NONE:
            default:
                return null;
        }
    }


}
