/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script;

import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.BOX2METADATA_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.BOX2METRICS_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.BOX_POLICY_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.BOX_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.METADATAGROUPS_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.TAB2BOX_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.TAB_POLICY_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.TAB_SHEET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.content.EntityType;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.factory.CrisLayoutServiceFactory;
import org.dspace.layout.service.CrisLayoutTabService;
import org.dspace.util.WorkbookUtils;
import org.junit.After;
import org.junit.Test;

/**
 * Integration tests for {@link ExportCrisLayoutToolScript}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 *
 */
public class ExportCrisLayoutToolScriptIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_XLS_DIR_PATH = "./target/testing/dspace/assetstore/layout/script";

    private CrisLayoutTabService tabService = CrisLayoutServiceFactory.getInstance().getTabService();

    @After
    public void after() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        List<CrisLayoutTab> tabs = tabService.findAll(context);
        for (CrisLayoutTab tab : tabs) {
            tabService.delete(context, tab);
        }
        context.restoreAuthSystemState();
    }

    @Test
    public void testWithValidLayout() throws Exception {
        context.turnOffAuthorisationSystem();
        createEntityType("Publication");
        createEntityType("Person");
        GroupBuilder.createGroup(context)
            .withName("Researchers")
            .build();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("export-valid-layout-with-3-tabs.xls");
        String[] args = new String[] { "cris-layout-tool", "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat(infoMessages, hasSize(6));
        assertThat(infoMessages.get(0), containsString("The given workbook is valid. Proceed with the import"));
        assertThat(infoMessages.get(1), containsString("The workbook has been parsed correctly, "
            + "found 3 tabs to import"));
        assertThat(infoMessages.get(2), containsString("Proceed with the clearing of the previous layout"));
        assertThat(infoMessages.get(3), containsString("Found 0 tabs to delete"));
        assertThat(infoMessages.get(4), containsString("The previous layout has been deleted, "
            + "proceed with the import of the new configuration"));
        assertThat(infoMessages.get(5), containsString("Import completed successfully"));

        assertThat(tabService.findAll(context), hasSize(3));

        args = new String[] { "export-cris-layout-tool"};
        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        File importedFile = new File(fileLocation);
        File exportedFile = new File("cris-layout-tool-exported.xls");
        exportedFile.deleteOnExit();

        try (FileInputStream fisImported = new FileInputStream(importedFile);
             FileInputStream fisExported = new FileInputStream(exportedFile)) {
            Workbook importedWorkbook = WorkbookFactory.create(fisImported);
            Workbook exportedWorkbook = WorkbookFactory.create(fisExported);

            Sheet importedTab = importedWorkbook.getSheet(TAB_SHEET);
            Sheet exportedTab = exportedWorkbook.getSheet(TAB_SHEET);

            assertEqualsSheets(importedTab, exportedTab, 6);

            Sheet importedTab2Box = importedWorkbook.getSheet(TAB2BOX_SHEET);
            Sheet exportedTab2Box = exportedWorkbook.getSheet(TAB2BOX_SHEET);

            assertEqualsSheets(importedTab2Box, exportedTab2Box, 6);

            Sheet importedBox = importedWorkbook.getSheet(BOX_SHEET);
            Sheet exportedBOX = exportedWorkbook.getSheet(BOX_SHEET);

            assertEqualsSheets(importedBox, exportedBOX, 9);

            Sheet importedBox2Metadata = importedWorkbook.getSheet(BOX2METADATA_SHEET);
            Sheet exportedBOX2Metadata = exportedWorkbook.getSheet(BOX2METADATA_SHEET);

            assertEqualsSheets(importedBox2Metadata, exportedBOX2Metadata, 16);


            Sheet importedMetadataGroups = importedWorkbook.getSheet(METADATAGROUPS_SHEET);
            Sheet exportedMetadataGroups = exportedWorkbook.getSheet(METADATAGROUPS_SHEET);

            assertEqualsSheets(importedMetadataGroups, exportedMetadataGroups, 10);


            Sheet importedBox2Metrics = importedWorkbook.getSheet(BOX2METRICS_SHEET);
            Sheet exportedBox2Metrics = exportedWorkbook.getSheet(BOX2METRICS_SHEET);

            assertEqualsSheets(importedBox2Metrics, exportedBox2Metrics, 3);


            Sheet importedTabPolicy = importedWorkbook.getSheet(TAB_POLICY_SHEET);
            Sheet exportedTabPolicy = exportedWorkbook.getSheet(TAB_POLICY_SHEET);

            assertEqualsSheets(importedTabPolicy, exportedTabPolicy, 4);

            Sheet importedBoxPolicy = importedWorkbook.getSheet(BOX_POLICY_SHEET);
            Sheet exportedBOXPolicy = exportedWorkbook.getSheet(BOX_POLICY_SHEET);

            assertEqualsSheets(importedBoxPolicy, exportedBOXPolicy, 4);
        }

    }

    private EntityType createEntityType(String entityType) {
        return EntityTypeBuilder.createEntityTypeBuilder(context, entityType).build();
    }

    private String getXlsFilePath(String name) {
        return new File(BASE_XLS_DIR_PATH, name).getAbsolutePath();
    }

    private void assertEqualsSheets(Sheet sheet1, Sheet sheet2, int cellsCount) {
        assertEquals(
            WorkbookUtils
                .getRows(sheet1)
                .map(row ->
                    convertToString(WorkbookUtils.getRowValues(row, cellsCount))
                )
                .sorted()
                .collect(Collectors.toList()),
            WorkbookUtils
                .getRows(sheet2)
                .map(row ->
                    convertToString(WorkbookUtils.getRowValues(row, cellsCount))
                )
                .sorted()
                .collect(Collectors.toList())
        );
    }

    private String convertToString(List<String> list) {
        StringBuilder value = new StringBuilder();
        list.forEach(s -> value.append(s));
        return String.valueOf(value);
    }
}
