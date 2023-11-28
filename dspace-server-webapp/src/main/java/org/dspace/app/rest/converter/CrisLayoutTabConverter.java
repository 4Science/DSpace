/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.app.rest.model.CrisLayoutBoxRest;
import org.dspace.app.rest.model.CrisLayoutCellRest;
import org.dspace.app.rest.model.CrisLayoutRowRest;
import org.dspace.app.rest.model.CrisLayoutTabRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.CrisLayoutTabRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutCell;
import org.dspace.layout.CrisLayoutRow;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.dspace.services.RequestService;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from Entity CrisLayoutTab to the REST data model
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class CrisLayoutTabConverter implements DSpaceConverter<CrisLayoutTab, CrisLayoutTabRest> {

    @Autowired
    private EntityTypeService eService;

    @Autowired
    private CrisLayoutBoxService crisLayoutBoxService;

    @Autowired
    private CrisLayoutBoxConverter boxConverter;

    @Autowired
    private RequestService requestService;

    @Autowired
    private ItemService itemService;

    @Override
    public CrisLayoutTabRest convert(CrisLayoutTab model, Projection projection) {
        CrisLayoutTabRest rest = new CrisLayoutTabRest();
        rest.setId(model.getID());
        rest.setEntityType(model.getEntity().getLabel());
        rest.setCustomFilter(model.getCustomFilter());
        rest.setShortname(model.getShortName());
        rest.setHeader(model.getHeader());
        rest.setPriority(model.getPriority());
        rest.setSecurity(model.getSecurity());
        rest.setRows(convertRows(getScopeItem(), model.getRows(), projection));
        rest.setLeading(model.isLeading());
        return rest;
    }

    @Override
    public Class<CrisLayoutTab> getModelClass() {
        return CrisLayoutTab.class;
    }

    public CrisLayoutTab toModel(Context context, CrisLayoutTabRest rest) {
        CrisLayoutTab tab = new CrisLayoutTab();
        tab.setHeader(rest.getHeader());
        tab.setPriority(rest.getPriority());
        tab.setSecurity(LayoutSecurity.valueOf(rest.getSecurity()));
        tab.setShortName(rest.getShortname());
        tab.setEntity(findEntityType(context, rest));
        tab.setCustomFilter(rest.getCustomFilter());
        tab.setLeading(rest.isLeading());
        rest.getRows().forEach(row -> tab.addRow(toRowModel(context, row)));
        return tab;
    }

    private List<CrisLayoutRowRest> convertRows(Item item, List<CrisLayoutRow> rows, Projection projection) {
        return rows.stream()
            .map(row -> convertRow(item, row, projection))
            .filter(row -> CollectionUtils.isNotEmpty(row.getCells()))
            .collect(Collectors.toList());
    }

    private CrisLayoutRowRest convertRow(Item item, CrisLayoutRow row, Projection projection) {
        CrisLayoutRowRest rest = new CrisLayoutRowRest();
        rest.setStyle(row.getStyle());
        rest.setCells(convertCells(item, row.getCells(), projection));
        return rest;
    }

    private List<CrisLayoutCellRest> convertCells(Item item, List<CrisLayoutCell> cells, Projection projection) {
        return cells.stream()
            .map(cell -> convertCell(item, cell, projection))
            .filter(cell -> CollectionUtils.isNotEmpty(cell.getBoxes()))
            .collect(Collectors.toList());
    }

    private CrisLayoutCellRest convertCell(Item item, CrisLayoutCell cell, Projection projection) {
        CrisLayoutCellRest rest = new CrisLayoutCellRest();
        rest.setStyle(cell.getStyle());
        rest.setBoxes(convertBoxes(item, cell.getBoxes(), projection));
        return rest;
    }

    private List<CrisLayoutBoxRest> convertBoxes(Item item, List<CrisLayoutBox> boxes, Projection projection) {
        return boxes.stream()
            .filter(box -> item == null || hasAccess(item, box))
            .map(box -> boxConverter.convert(box, projection))
            .collect(Collectors.toList());
    }

    private boolean hasAccess(Item item, CrisLayoutBox box) {
        Context context = ContextUtil.obtainCurrentRequestContext();
        return crisLayoutBoxService.hasContent(context, box, item)
            && crisLayoutBoxService.hasAccess(context, box, item);
    }

    private CrisLayoutRow toRowModel(Context context, CrisLayoutRowRest rowRest) {
        CrisLayoutRow row = new CrisLayoutRow();
        row.setStyle(rowRest.getStyle());
        rowRest.getCells().forEach(cell -> row.addCell(toCellModel(context, cell)));
        return row;
    }

    private CrisLayoutCell toCellModel(Context context, CrisLayoutCellRest cellRest) {
        CrisLayoutCell cell = new CrisLayoutCell();
        cell.setStyle(cellRest.getStyle());
        cellRest.getBoxes().forEach(box -> cell.addBox(boxConverter.toModel(context, box)));
        return cell;
    }

    private EntityType findEntityType(Context context, CrisLayoutTabRest rest) {
        try {
            return eService.findByEntityType(context, rest.getEntityType());
        } catch (SQLException e) {
            throw new SQLRuntimeException(e.getMessage(), e);
        }
    }

    private Item getScopeItem() {
        return Optional.ofNullable(requestService.getCurrentRequest())
            .map(rq -> (String) rq.getAttribute(CrisLayoutTabRestRepository.SCOPE_ITEM_ATTRIBUTE))
            .map(itemId -> findItem(itemId))
            .orElse(null);
    }

    private Item findItem(String uuid) {
        try {
            return itemService.find(ContextUtil.obtainCurrentRequestContext(), UUIDUtils.fromString(uuid));
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }
}
