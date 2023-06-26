/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script.service.impl;

import static org.dspace.layout.script.service.CrisLayoutToolValidator.BOX2HIERARCHICAL_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.BOX2METADATA_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.BOX2METRICS_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.BOX_POLICY_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.BOX_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.METADATAGROUPS_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.METADATAGROUP_TYPE;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.TAB2BOX_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.TAB_POLICY_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.TAB_SHEET;
import static org.dspace.util.WorkbookUtils.createCell;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dspace.content.MetadataField;
import org.dspace.eperson.Group;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutCell;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutFieldBitstream;
import org.dspace.layout.CrisLayoutHierarchicalVocabulary2Box;
import org.dspace.layout.CrisLayoutMetric2Box;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.CrisMetadataGroup;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.script.service.CrisLayoutToolConverter;

/**
 * Implementation of {@link CrisLayoutToolConverter}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 *
 */
public class CrisLayoutToolConverterImpl implements CrisLayoutToolConverter {

    @Override
    public Workbook convert(List<CrisLayoutTab> tabs) {
        Workbook workbook = getTemplateWorkBook();
        buildTab(workbook, tabs);
        autoSizeAllSheetsColumns(workbook);
        return workbook;
    }

    private Workbook getTemplateWorkBook() {
        try (InputStream inputStream =
                 CrisLayoutToolConverterImpl.class
                     .getResourceAsStream("cris-layout-configuration-template.xls")) {
            return WorkbookFactory.create(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildTab(Workbook workbook, List<CrisLayoutTab> tabs) {
        Sheet sheet = workbook.getSheet(TAB_SHEET);
        tabs.forEach(tab -> {
            buildTabRow(sheet, tab);
            buildTab2box(workbook, tab);
            buildTabPolicy(workbook, tab);
        });
    }

    private void buildTabRow(Sheet sheet, CrisLayoutTab tab) {
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        createCell(row, 0, tab.getEntity().getLabel());
        createCell(row, 1, tab.getShortName());
        createCell(row, 2, tab.getHeader());
        createCell(row, 3, String.valueOf(tab.getPriority()));
        createCell(row, 4, convertToString(tab.isLeading()));
        createCell(row, 5, toSecurity(tab.getSecurity()));
    }

    private void buildTab2box(Workbook workbook, CrisLayoutTab tab) {
        Sheet sheet = workbook.getSheet(TAB2BOX_SHEET);
        for (int i = 0 ; i < tab.getRows().size() ; i++) {
            // position column into database starts from 0, so will increase 1
            int rowIndex = i + 1;
            tab.getRows().get(i).getCells()
                .forEach(cell -> {
                    buildTab2boxRow(sheet, rowIndex, cell);
                    buildBox(sheet.getWorkbook(), cell.getBoxes());
                    buildBoxPolicy(sheet.getWorkbook(), cell.getBoxes());
                });
        }
    }

    private void buildTab2boxRow(Sheet sheet, int cellIndex, CrisLayoutCell cell) {
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        createCell(row, 0, cell.getRow().getTab().getEntity().getLabel());
        createCell(row, 1, cell.getRow().getTab().getShortName());
        createCell(row, 2, String.valueOf(cellIndex));
        createCell(row, 3, cell.getRow().getStyle());
        createCell(row, 4, cell.getStyle());
        createCell(row, 5, getBoxesNames(cell.getBoxes()));
    }

    private String getBoxesNames(List<CrisLayoutBox> boxes) {
        return boxes.stream()
                    .map(box -> box.getShortname())
                    .collect(Collectors.joining(", "));
    }

    private void buildBox(Workbook workbook, List<CrisLayoutBox> boxes) {
        Sheet sheet = workbook.getSheet(BOX_SHEET);
        boxes.forEach(box -> {
            buildBoxRow(sheet, box);
            buildBox2metadata(sheet.getWorkbook(), box.getLayoutFields());
            buildBox2metrics(sheet.getWorkbook(), box);
            buildBoxHierarchical(sheet.getWorkbook(), box);
        });
    }

    private void buildBoxRow(Sheet sheet, CrisLayoutBox box) {
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        createCell(row, 0, box.getCell().getRow().getTab().getEntity().getLabel());
        createCell(row, 1, convertToString(box.getCollapsed()));
        createCell(row, 2, box.getType());
        createCell(row, 3, box.getShortname());
        createCell(row, 4, box.getHeader());
        createCell(row, 5, convertToString(box.isContainer()));
        createCell(row, 6, convertToString(box.getMinor()));
        createCell(row, 7, toSecurity(box.getSecurity()));
        createCell(row, 8, box.getStyle());
    }

    private void buildBox2metadata(Workbook workbook, List<CrisLayoutField> layoutFields) {
        Sheet sheet = workbook.getSheet(BOX2METADATA_SHEET);
        layoutFields.forEach(layoutField -> {
            buildBox2metadataRow(sheet, layoutField);
            buildMetadataGroups(sheet.getWorkbook(), layoutField.getCrisMetadataGroupList());
        });
    }

    private void buildBox2metadataRow(Sheet sheet, CrisLayoutField layoutField) {
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        createCell(row, 0, layoutField.getBox().getCell().getRow().getTab().getEntity().getLabel());
        createCell(row, 1, layoutField.getBox().getShortname());
        createCell(row, 2, String.valueOf(layoutField.getRow()));
        createCell(row, 3, String.valueOf(layoutField.getCell()));
        createCell(row, 4, getLayoutFieldType(layoutField));
        createCell(row, 5, getMetadataField(layoutField));
        createCell(row, 6, getMetadataValue(layoutField));
        createCell(row, 7, getBundle(layoutField));
        createCell(row, 8, layoutField.getLabel());
        createCell(row, 9, convertToString(layoutField.isLabelAsHeading()));
        createCell(row, 10, layoutField.getRendering());
        createCell(row, 11, convertToString(layoutField.isValuesInline()));
        createCell(row, 12, layoutField.getRowStyle());
        createCell(row, 13, layoutField.getCellStyle());
        createCell(row, 14, layoutField.getStyleLabel());
        createCell(row, 15, layoutField.getStyleValue());
    }

    private String getMetadataValue(CrisLayoutField layoutField) {
        String value = "";
        if (layoutField instanceof CrisLayoutFieldBitstream) {
            value = ((CrisLayoutFieldBitstream) layoutField).getMetadataValue();
        }
        return value;
    }

    private String getBundle(CrisLayoutField layoutField) {
        String value = "";
        if (layoutField instanceof CrisLayoutFieldBitstream) {
            value = ((CrisLayoutFieldBitstream) layoutField).getBundle();
        }
        return value;
    }

    private String getMetadataField(CrisLayoutField layoutField) {
        return Optional.ofNullable(layoutField.getMetadataField())
                       .map(metadataField -> metadataField.toString('.'))
                       .orElse("");
    }

    private String getLayoutFieldType(CrisLayoutField layoutField) {
        String type = layoutField.getType();
        if (CollectionUtils.isNotEmpty(layoutField.getCrisMetadataGroupList())) {
            type = METADATAGROUP_TYPE;
        }
        return type;
    }

    private void buildMetadataGroups(Workbook workbook, List<CrisMetadataGroup> crisMetadataGroups) {
        Sheet sheet = workbook.getSheet(METADATAGROUPS_SHEET);
        crisMetadataGroups
            .forEach(crisMetadataGroup ->
                buildMetadataGroupRow(sheet, crisMetadataGroup));
    }

    private void buildMetadataGroupRow(Sheet sheet, CrisMetadataGroup crisMetadataGroup) {
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        CrisLayoutField crisLayoutField = crisMetadataGroup.getCrisLayoutField();

        createCell(row, 0, crisLayoutField.getBox().getCell().getRow().getTab().getEntity().getLabel());
        createCell(row, 1, crisLayoutField.getMetadataField().toString('.'));
        createCell(row, 2, crisLayoutField.getType());
        createCell(row, 3, crisMetadataGroup.getMetadataField().toString('.'));
        createCell(row, 4, "");
        createCell(row, 5, "");
        createCell(row, 6, crisMetadataGroup.getLabel());
        createCell(row, 7, crisMetadataGroup.getRendering());
        createCell(row, 8, crisMetadataGroup.getStyleLabel());
        createCell(row, 9, crisMetadataGroup.getStyleValue());
    }

    private void buildBox2metrics(Workbook workbook, CrisLayoutBox box) {
        Sheet sheet = workbook.getSheet(BOX2METRICS_SHEET);
        buildBox2metricsRow(sheet, box);
    }

    private void buildBox2metricsRow(Sheet sheet, CrisLayoutBox box) {
        if (CollectionUtils.isNotEmpty(box.getMetric2box())) {
            Row row = sheet.createRow(sheet.getLastRowNum() + 1);
            createCell(row, 0, box.getCell().getRow().getTab().getEntity().getLabel());
            createCell(row, 1, box.getShortname());
            createCell(row, 2, getMetric2BoxTypes(box.getMetric2box()));
        }
    }

    private String getMetric2BoxTypes(List<CrisLayoutMetric2Box> crisLayoutMetric2Boxes) {
        return crisLayoutMetric2Boxes.stream()
                                     .map(CrisLayoutMetric2Box::getType)
                                     .collect(Collectors.joining(", "));
    }

    private void buildBoxHierarchical(Workbook workbook, CrisLayoutBox box) {
        Sheet sheet = workbook.getSheet(BOX2HIERARCHICAL_SHEET);
        buildBoxHierarchicalRow(sheet, box);
    }

    private void buildBoxHierarchicalRow(Sheet sheet, CrisLayoutBox box) {
        CrisLayoutHierarchicalVocabulary2Box hierarchicalVocabulary2Box = box.getHierarchicalVocabulary2Box();
        if (!Objects.isNull(hierarchicalVocabulary2Box)) {
            Row row = sheet.createRow(sheet.getLastRowNum() + 1);
            createCell(row, 0, box.getCell().getRow().getTab().getEntity().getLabel());
            createCell(row, 1, box.getShortname());
            createCell(row, 2, hierarchicalVocabulary2Box.getVocabulary());
            createCell(row, 3, hierarchicalVocabulary2Box.getMetadataField().toString('.'));
        }
    }

    private void buildTabPolicy(Workbook workbook, CrisLayoutTab tab) {
        Sheet sheet = workbook.getSheet(TAB_POLICY_SHEET);
        tab.getMetadataSecurityFields()
            .forEach(metadataField ->
                buildTabPolicyMetadataSecurityFieldRow(sheet, tab, metadataField)
            );

        tab.getGroupSecurityFields()
            .forEach(group ->
                buildTabPolicyGroupSecurityFieldRow(sheet, tab, group)
            );
    }

    private void buildTabPolicyMetadataSecurityFieldRow(Sheet sheet, CrisLayoutTab tab, MetadataField metadataField) {
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        createCell(row, 0, tab.getEntity().getLabel());
        createCell(row, 1, tab.getShortName());
        createCell(row, 2, metadataField.toString('.'));
        createCell(row, 3, "");
    }

    private void buildTabPolicyGroupSecurityFieldRow(Sheet sheet, CrisLayoutTab tab, Group group) {
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        createCell(row, 0, tab.getEntity().getLabel());
        createCell(row, 1, tab.getShortName());
        createCell(row, 2, "");
        createCell(row, 3, group.getName());
    }

    private void buildBoxPolicy(Workbook workbook, List<CrisLayoutBox> boxes) {
        Sheet sheet = workbook.getSheet(BOX_POLICY_SHEET);
        boxes.forEach(box -> {
            box.getMetadataSecurityFields()
                .forEach(metadataField ->
                    buildBoxPolicyMetadataSecurityFieldRow(sheet, box, metadataField)
                );

            box.getGroupSecurityFields()
                .forEach(group ->
                    buildBoxPolicyGroupSecurityFieldRow(sheet, box, group)
                );
        });
    }

    private void buildBoxPolicyMetadataSecurityFieldRow(Sheet sheet, CrisLayoutBox box, MetadataField metadataField) {
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        createCell(row, 0, box.getCell().getRow().getTab().getEntity().getLabel());
        createCell(row, 1, box.getShortname());
        createCell(row, 2, metadataField.toString('.'));
        createCell(row, 3, "");
    }

    private void buildBoxPolicyGroupSecurityFieldRow(Sheet sheet, CrisLayoutBox box, Group group) {
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        createCell(row, 0, box.getCell().getRow().getTab().getEntity().getLabel());
        createCell(row, 1, box.getShortname());
        createCell(row, 2, "");
        createCell(row, 3, group.getName());
    }

    private String convertToString(boolean value) {
        return value ? "y" : "n";
    }

    private String toSecurity(Integer security) {
        return String.valueOf(LayoutSecurity.valueOf(security))
                     .replaceAll("_", " ")
                     .replaceAll("AND", "&");
    }

    private void autoSizeAllSheetsColumns(Workbook workbook) {
        autoSizeColumns(workbook.getSheet(TAB_SHEET));
        autoSizeColumns(workbook.getSheet(TAB2BOX_SHEET));
        autoSizeColumns(workbook.getSheet(BOX_SHEET));
        autoSizeColumns(workbook.getSheet(BOX2HIERARCHICAL_SHEET));
        autoSizeColumns(workbook.getSheet(BOX2METADATA_SHEET));
        autoSizeColumns(workbook.getSheet(METADATAGROUPS_SHEET));
        autoSizeColumns(workbook.getSheet(BOX2METRICS_SHEET));
        autoSizeColumns(workbook.getSheet(TAB_POLICY_SHEET));
        autoSizeColumns(workbook.getSheet(BOX_POLICY_SHEET));
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

}
