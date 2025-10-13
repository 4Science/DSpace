/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.security;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.service.MetadataExposureService;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.security.service.MetadataSecurityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataSecurityEvaluation;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.eperson.EPerson;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutFieldMetadata;
import org.dspace.layout.CrisMetadataGroup;
import org.dspace.layout.service.CrisLayoutBoxAccessService;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link MetadataSecurityService}.
 *
 * @author Mykhaylo Boychuk (4science.it)
 * @author Luca Giamminonni (4science.it)
 */
public class MetadataSecurityServiceImpl implements MetadataSecurityService {

    @Resource(name = "securityLevelsMap")
    private final Map<String, MetadataSecurityEvaluation> securityLevelsMap = new HashMap<>();

    @Autowired
    private ItemService itemService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private CrisLayoutBoxService crisLayoutBoxService;

    @Autowired
    private MetadataExposureService metadataExposureService;

    @Autowired
    private CrisLayoutBoxAccessService crisLayoutBoxAccessService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private ConfigurationService configurationService;

    private DCInputsReader dcInputsReader;

    @PostConstruct
    private void setup() throws DCInputsReaderException {
        this.dcInputsReader = new DCInputsReader();
    }

    @Override
    public List<MetadataValue> getPermissionFilteredMetadataValues(Context context, Item item) {
        return getPermissionFilteredMetadataValues(context, item, false);
    }

    @Override
    public List<MetadataValue> getPermissionFilteredMetadataValues(Context context, Item item, String metadataField) {
        return getPermissionFilteredMetadataValues(context, item, metadataField, false);
    }

    @Override
    public List<MetadataValue> getPermissionFilteredMetadataValues(Context context, Item item,
        boolean preventBoxSecurityCheck) {
        List<MetadataValue> values = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY, true);
        return getPermissionFilteredMetadata(context, item, values, preventBoxSecurityCheck);
    }

    @Override
    public List<MetadataValue> getPermissionAndLangFilteredMetadataFields(Context context, Item item,
                                                                              boolean preventBoxSecurityCheck) {
        String language = context != null ? context.getCurrentLocale().getLanguage() : Item.ANY;

        List<MetadataValue> values = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, language, true);
        return getPermissionFilteredMetadata(context, item, values, preventBoxSecurityCheck);
    }

    @Override
    public List<MetadataValue> getPermissionFilteredMetadataValues(Context context, Item item, String metadataField,
        boolean preventBoxSecurityCheck) {
        List<MetadataValue> metadataValues = itemService.getMetadataByMetadataString(item, metadataField);
        return getPermissionFilteredMetadata(context, item, metadataValues, preventBoxSecurityCheck);
    }

    @Override
    public boolean checkMetadataFieldVisibility(Context context, Item item, MetadataField metadataField) {
        List<CrisLayoutBox> boxes = findBoxes(context, item, false);
        return isMetadataFieldVisible(context, boxes, item, metadataField, false);
    }

    private List<MetadataValue> getPermissionFilteredMetadata(Context context, Item item,
        List<MetadataValue> metadataValues, boolean preventBoxSecurityCheck) {

        if (item.isWithdrawn() && isNotAdmin(context, item)) {
            return new ArrayList<MetadataValue>();
        }

        List<CrisLayoutBox> boxes = findBoxes(context, item, preventBoxSecurityCheck);

        Optional<List<DCInputSet>> inputs = submissionDefinitionInputs();
        if (inputs.isPresent()) {
            return getFromSubmission(context, boxes, item, inputs.get(), metadataValues, preventBoxSecurityCheck);
        }

        return metadataValues.stream()
            .filter(value -> isMetadataValueVisible(context, boxes, item, value, preventBoxSecurityCheck))
            .filter(value -> isMetadataValueReturnAllowed(context, item, value))
            .collect(Collectors.toList());

    }

    private boolean canEditItem(Context context, Item item) {
        if (context == null) {
            return false;
        }
        try {
            return this.itemService.canEdit(context, item);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<CrisLayoutBox> findBoxes(Context context, Item item, boolean preventBoxSecurityCheck) {
        if (context == null || preventBoxSecurityCheck) {
            // the context could be null if the converter is used to prepare test data or in a batch script
            return new ArrayList<CrisLayoutBox>();
        }

        String entityType = itemService.getEntityTypeLabel(item);
        try {
            return crisLayoutBoxService.findByEntityType(context, entityType, 1000, 0);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private boolean isMetadataValueVisible(Context context, List<CrisLayoutBox> boxes, Item item, MetadataValue value,
        boolean preventBoxSecurityCheck) {
        return isMetadataFieldVisible(context, boxes, item, value.getMetadataField(), preventBoxSecurityCheck);
    }

    private boolean isMetadataFieldVisible(Context context, List<CrisLayoutBox> boxes, Item item,
        MetadataField metadataField, boolean preventBoxSecurityCheck) {

        if (isPublicMetadataField(metadataField, boxes, preventBoxSecurityCheck)) {
            return true;
        }

        // stop any further costly check if the converter request the fastest strategy
        if (preventBoxSecurityCheck) {
            return false;
        }

        EPerson currentUser = context != null ? context.getCurrentUser() : null;
        List<CrisLayoutBox> notPublicBoxes = getNotPublicBoxes(metadataField, boxes);

        if (Objects.nonNull(currentUser)) {

            for (CrisLayoutBox box : notPublicBoxes) {
                if (crisLayoutBoxAccessService.hasAccess(context, currentUser, box, item)) {
                    return true;
                }
            }
        }

        // the metadata is not included in any box so use the default dspace security
        if (notPublicBoxes.isEmpty() && isMetadataFieldVisibleFor(context, item, metadataField)) {
            return true;
        }

        return false;
    }

    private boolean isMetadataFieldVisibleFor(Context context, Item item, MetadataField metadataField) {
        return isNotHidden(context, metadataField) || canEditItem(context, item);
    }

    private boolean isMetadataValueReturnAllowed(Context context, Item item, MetadataValue metadataValue) {
        Integer securityLevel = metadataValue.getSecurityLevel();
        if (securityLevel == null) {
            return true;
        }

        MetadataSecurityEvaluation metadataSecurityEvaluation = getMetadataSecurityEvaluator(securityLevel);
        try {
            return metadataSecurityEvaluation.allowMetadataFieldReturn(context, item, metadataValue.getMetadataField());
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private boolean isPublicMetadataField(MetadataField metadataField, List<CrisLayoutBox> boxes,
        boolean preventBoxSecurityCheck) {

        List<String> publicFields = preventBoxSecurityCheck || boxes.isEmpty() ?
                getPublicMetadataFromConfig() : getPublicMetadata(boxes);

        return publicFields.stream()
                .anyMatch(publicField -> metadataMatch(metadataField, publicField));
    }

    private boolean metadataMatch(MetadataField metadataField, String publicField) {
        if (metadataField == null || publicField == null) {
            return false;
        }
        if (publicField.contains(".*")) {
            final String exactMatch = publicField.replace(".*", "");
            StringBuffer qualifiedMatch = new StringBuffer(exactMatch).append(".");
            return exactMatch.equals(metadataField.toString('.')) ||
                    StringUtils.startsWith(metadataField.toString('.'), qualifiedMatch.toString());
        } else {
            return publicField.equals(metadataField.toString('.'));
        }
    }

    private List<String> getPublicMetadataFromConfig() {
        return List.of(configurationService.getArrayProperty("metadata.publicField"));
    }

    private List<String> getPublicMetadata(List<CrisLayoutBox> boxes) {
        return boxes.stream()
            .filter(box -> box.isPublic())
            .flatMap(box -> getAllMetadataFields(box).stream())
            .map(metadataField -> metadataField.toString('.'))
            .collect(Collectors.toList());
    }

    private List<CrisLayoutBox> getNotPublicBoxes(MetadataField metadataField, List<CrisLayoutBox> boxes) {
        return boxes.stream()
            .filter(box -> box.isNotPublic())
            .filter(box -> boxContainsMetadataField(box, metadataField))
            .collect(Collectors.toList());
    }

    private boolean boxContainsMetadataField(CrisLayoutBox box, MetadataField metadataField) {
        return getAllMetadataFields(box).contains(metadataField);
    }

    private Set<MetadataField> getAllMetadataFields(CrisLayoutBox box) {
        Set<MetadataField> metadataFields = new HashSet<>();
        for (CrisLayoutField field : box.getLayoutFields()) {
            if (field instanceof CrisLayoutFieldMetadata) {
                metadataFields.add(field.getMetadataField());
                for (CrisMetadataGroup metadataGroup : field.getCrisMetadataGroupList()) {
                    metadataFields.add(metadataGroup.getMetadataField());
                }
            }
        }
        return metadataFields;
    }

    private Optional<List<DCInputSet>> submissionDefinitionInputs() {
        return Optional.ofNullable(requestService.getCurrentRequest())
            .map(rq -> (String) rq.getAttribute("submission-name"))
            .map(this::dcInputsSet);
    }

    private List<DCInputSet> dcInputsSet(final String sd) {
        try {
            return dcInputsReader.getInputsBySubmissionName(sd);
        } catch (DCInputsReaderException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private boolean isNotAdmin(Context context, Item item) {
        try {
            return context == null || !authorizeService.isAdmin(context, item);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private List<MetadataValue> getFromSubmission(Context context, List<CrisLayoutBox> boxes, Item item,
        final List<DCInputSet> dcInputSets, final List<MetadataValue> metadataValues, boolean preventBoxSecurityCheck) {

        List<MetadataValue> filteredMetadataValues = new ArrayList<MetadataValue>();

        for (MetadataValue metadataValue : metadataValues) {
            MetadataField field = metadataValue.getMetadataField();
            if (dcInputsContainsField(dcInputSets, field)
                || isMetadataFieldVisible(context, boxes, item, field, preventBoxSecurityCheck)) {
                filteredMetadataValues.add(metadataValue);
            }
        }

        return filteredMetadataValues;
    }

    private boolean dcInputsContainsField(List<DCInputSet> dcInputSets, MetadataField metadataField) {
        return dcInputSets.stream().anyMatch((input) -> input.isFieldPresent(metadataField.toString('.')));
    }

    private boolean isNotHidden(Context context, MetadataField metadataField) {
        try {
            return metadataField != null &&
                    !metadataExposureService.isHidden(context, metadataField.getMetadataSchema().getName(),
                            metadataField.getElement(), metadataField.getQualifier());
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    public MetadataSecurityEvaluation getMetadataSecurityEvaluator(int securityValue) {
        return securityLevelsMap.get(securityValue + "");
    }
}