/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service.impl;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.discovery.configuration.DiscoveryConfigurationUtilsService;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBoxConfiguration;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutFieldBitstream;
import org.dspace.layout.dao.CrisLayoutBoxDAO;
import org.dspace.layout.service.CrisLayoutBoxAccessService;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.dspace.metrics.CrisItemMetricsService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of service to manage Boxes component of layout
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 */
public class CrisLayoutBoxServiceImpl implements CrisLayoutBoxService {

    @Autowired
    private CrisLayoutBoxDAO dao;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private CrisLayoutBoxAccessService crisLayoutBoxAccessService;

    @Autowired
    private DiscoveryConfigurationUtilsService searchConfigurationUtilsService;

    @Autowired
    private CrisItemMetricsService crisMetricService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    private final Logger log = org.apache.logging.log4j.LogManager.getLogger(CrisLayoutBoxServiceImpl.class);

    public CrisLayoutBoxServiceImpl() {
    }

    @Override
    public CrisLayoutBox create(Context context) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to create a Box");
        }
        return dao.create(context, new CrisLayoutBox());
    }

    @Override
    public CrisLayoutBox find(Context context, int id) throws SQLException {
        return dao.findByID(context, CrisLayoutBox.class, id);
    }

    @Override
    public void update(Context context, CrisLayoutBox boxList) throws SQLException, AuthorizeException {
        update(context, Collections.singletonList(boxList));
    }

    @Override
    public void update(Context context, List<CrisLayoutBox> boxList) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to update a Box");
        }
        if (CollectionUtils.isNotEmpty(boxList)) {
            for (CrisLayoutBox box : boxList) {
                dao.save(context, box);
            }
        }
    }

    @Override
    public void delete(Context context, CrisLayoutBox box) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to delete a Box");
        }
        box.getMetric2box().clear();
        dao.delete(context, box);
    }

    @Override
    public CrisLayoutBox create(Context context, CrisLayoutBox box) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to create a Box");
        }
        return dao.create(context, box);
    }

    @Override
    public CrisLayoutBox create(Context context, EntityType eType, String boxType, boolean collapsed,
                                boolean minor) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You must be an admin to create a Box");
        }
        CrisLayoutBox box = new CrisLayoutBox();
        box.setEntitytype(eType);
        box.setCollapsed(collapsed);
        box.setMinor(minor);
        box.setType(boxType);
        return dao.create(context, box);

    }

    @Override
    public List<CrisLayoutBox> findByEntityType(Context context, String entityType,
        Integer limit, Integer offset) throws SQLException {
        return dao.findByEntityType(context, entityType, limit, offset);
    }

    @Override
    public boolean hasContent(Context context, CrisLayoutBox box, Item item) {
        String boxType = box.getType();

        if (StringUtils.isEmpty(boxType)) {
            return hasMetadataBoxContent(context, box, item);
        }

        switch (boxType.toUpperCase()) {
            case "RELATION":
                return hasRelationBoxContent(context, box, item);
            case "METRICS":
                return hasMetricsBoxContent(context, box, item);
            case "IIIFVIEWER":
            case "IIIFTOOLBAR":
                return isIiifEnabled(item);
            case "HIERARCHY":
                return hasHierarchicBoxContent(context, box, item);
            case "VIDEOVIEWER":
                return hasMediaResources(context, box, item);
            case "METADATA":
            default:
                return hasMetadataBoxContent(context, box, item);
        }

    }

    @Override
    public boolean hasAccess(Context context, CrisLayoutBox box, Item item) {
        return crisLayoutBoxAccessService.hasAccess(context, context.getCurrentUser(), box, item);
    }

    @Override
    public CrisLayoutBoxConfiguration getConfiguration(CrisLayoutBox box) {
        return new CrisLayoutBoxConfiguration(box);
    }

    private boolean hasMetadataBoxContent(Context context, CrisLayoutBox box, Item item) {

        List<CrisLayoutField> boxFields = box.getLayoutFields();
        if (CollectionUtils.isEmpty(boxFields)) {
            return false;
        }

        for (CrisLayoutField field : boxFields) {

            if (field.isMetadataField() && isMetadataFieldPresent(item, field.getMetadataField())) {
                return true;
            }

            if (field.isBitstreamField() && isBitstreamPresent(context, item, (CrisLayoutFieldBitstream) field)) {
                return true;
            }

        }

        return false;
    }

    private boolean isMetadataFieldPresent(DSpaceObject item, MetadataField metadataField) {
        return item.getMetadata().stream()
            .anyMatch(metadataValue -> Objects.equals(metadataField, metadataValue.getMetadataField()));
    }

    private boolean isBitstreamPresent(Context context, Item item, CrisLayoutFieldBitstream field) {
        Map<String, String> filters = new HashMap<>();
        if (field.getMetadataField() != null && StringUtils.isNotBlank(field.getMetadataValue())) {
            filters.put(field.getMetadataField().toString('.'), field.getMetadataValue());
        }
        try {
            return bitstreamService.findShowableByItem(context, item.getID(), field.getBundle(),
                filters, false).size() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean hasRelationBoxContent(Context context, CrisLayoutBox box, Item item) {
        Iterator<Item> relatedItems = searchConfigurationUtilsService.findByRelation(context, item, box.getShortname());
        return relatedItems.hasNext();
    }

    protected boolean hasMetricsBoxContent(Context context, CrisLayoutBox box, Item item) {

        if (box.getMetric2box().isEmpty() || currentUserIsNotAllowedToReadItem(context, item)) {
            return false;
        }

        final Set<String> boxTypes = new HashSet<>();
        box.getMetric2box().forEach(b -> {
            boxTypes.add(b.getType());
            crisMetricService.embeddableFallback(b.getType()).ifPresent(boxTypes::add);
        });
        if (this.crisMetricService.getEmbeddableMetrics(context, item.getID(), null).stream()
            .filter(m -> boxTypes.contains(m.getMetricType())).count() > 0) {
            return true;
        }
        if (this.crisMetricService.getStoredMetrics(context, item.getID()).stream()
            .filter(m -> boxTypes.contains(m.getMetricType())).count() > 0) {
            return true;
        }
        return false;
    }

    private boolean hasHierarchicBoxContent(Context context, CrisLayoutBox box, Item item) {
        if (box.getHierarchicalVocabulary2Box() != null && !currentUserIsNotAllowedToReadItem(context, item)) {
            try {
                return choiceAuthorityService
                    .getChoiceAuthorityByAuthorityName(box.getHierarchicalVocabulary2Box()
                                                           .getVocabulary()).isHierarchical();
            } catch (Exception e) {
                log.warn(e.getMessage());
                return false;
            }
        }

        return false;
    }

    private boolean hasMediaResources(Context context, CrisLayoutBox box, Item item) {
        List<Bundle> bundles = item.getBundles();
        try {
            boolean hasRes = bundles.stream().anyMatch(bundle -> bundle.getName().startsWith("VIDEO-ACCESS"));
            return hasRes;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isIiifEnabled(Item item) {
        return BooleanUtils.toBoolean(itemService.getMetadataFirstValue(item,
            new MetadataFieldName("dspace.iiif.enabled"), Item.ANY));
    }

    private boolean currentUserIsNotAllowedToReadItem(Context context, Item item) {
        try {
            return !authorizeService.authorizeActionBoolean(context, item, Constants.READ);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    public List<CrisLayoutBox> findByEntityAndType(Context context,String entity, String type) {

        try {
            return dao.findByEntityAndType(context, entity, type);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
