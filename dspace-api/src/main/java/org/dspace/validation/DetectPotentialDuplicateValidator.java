/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation;

import static org.dspace.app.deduplication.model.DuplicateDecisionType.WORKFLOW;
import static org.dspace.app.deduplication.model.DuplicateDecisionType.WORKSPACE;
import static org.dspace.validation.service.ValidationService.OPERATION_PATH_SECTIONS;
import static org.dspace.validation.util.ValidationUtils.addError;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.app.deduplication.model.DuplicateDecisionType;
import org.dspace.app.deduplication.model.DuplicateDecisionValue;
import org.dspace.app.deduplication.utils.DuplicateItemInfo;
import org.dspace.app.deduplication.utils.IDedupUtils;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.DSpaceObject;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.discovery.SearchServiceException;
import org.dspace.validation.model.ValidationError;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link SubmissionStepValidator} that check if the current
 * object has a duplication without decision.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 *
 */
public class DetectPotentialDuplicateValidator implements SubmissionStepValidator {

    private static final String ERROR_VALIDATION_DUPLICATION = "error.validation.detect-duplicate";

    @Autowired
    private IDedupUtils dedupUtils;

    @Autowired
    private ItemService itemService;

    private String name;

    @Override
    public List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionStepConfig config) {

        List<ValidationError> errors = new ArrayList<>();

        List<DuplicateItemInfo> duplicates = findDuplicates(context, obj);

        if (atLeastOneDecisionHasNotBeenMade(context, obj, duplicates)) {
            addError(errors, ERROR_VALIDATION_DUPLICATION, "/" + OPERATION_PATH_SECTIONS + "/" + config.getId());
        }

        return errors;
    }

    private boolean atLeastOneDecisionHasNotBeenMade(Context context, InProgressSubmission<?> obj,
        List<DuplicateItemInfo> duplicates) {

        DuplicateDecisionType decisionType = isNotWorkspaceItem(obj) ? WORKFLOW : WORKSPACE;

        return getDistinctDecisions(context, duplicates, decisionType).stream()
            .anyMatch(decision -> decision == null);

    }

    private List<DuplicateItemInfo> findDuplicates(Context context, InProgressSubmission<?> obj) {

        UUID itemID = obj.getItem().getID();
        int typeID = obj.getItem().getType();
        boolean check = isNotWorkspaceItem(obj);

        try {
            return dedupUtils.getDuplicateByIDandType(context, itemID, typeID, check);
        } catch (SQLException | SearchServiceException e) {
            throw new RuntimeException(e);
        }

    }

    private java.util.Collection<DuplicateDecisionValue> getDistinctDecisions(Context context,
        List<DuplicateItemInfo> duplicates, DuplicateDecisionType decisionType) {

        Map<UUID, DuplicateDecisionValue> decisions = new HashMap<UUID, DuplicateDecisionValue>();

        for (DuplicateItemInfo duplicate : duplicates) {

            DuplicateDecisionValue decision = duplicate.getDecision(decisionType);

            DSpaceObject duplicateItem = duplicate.getDuplicateItem();

            if (isNotLastVersion(context, (Item) duplicateItem)) {
                continue;
            }

            UUID itemUuid = duplicate.getDuplicateItem().getID();

            if (!decisions.containsKey(itemUuid) || decision != null) {
                decisions.put(itemUuid, decision);
            }

        }

        return decisions.values();

    }

    private boolean isNotLastVersion(Context context, Item item) {
        try {
            return !itemService.isLatestVersion(context, item);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private boolean isNotWorkspaceItem(InProgressSubmission<?> obj) {
        return !(obj instanceof WorkspaceItem);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
