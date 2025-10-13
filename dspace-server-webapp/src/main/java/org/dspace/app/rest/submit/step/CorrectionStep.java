/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.CorrectionBitstream;
import org.dspace.app.rest.model.step.CorrectionData;
import org.dspace.app.rest.model.step.CorrectionPolicy;
import org.dspace.app.rest.model.step.SectionData;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.factory.PatchOperationFactory;
import org.dspace.app.rest.submit.factory.impl.PatchOperation;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.Relationship;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.RequestService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.dspace.versioning.ItemCorrectionService;

/**
 * Correction step for DSpace Spring Rest.
 *
 * In order to find which metadata has changed, removed or added, this class retrieve the set of metadata in the
 * original pubblication, and metadata after the correction step.
 * The intersection of the two set contains the unchanged metadata, the original set of metadata "minus" the
 * intersection contains the removed metadata and set of the corrected metadata "minus" the intersection contains the
 * new metadata has been added during the correction step
 *
 * @author David Vivarelli
 */
public class CorrectionStep extends AbstractProcessingStep {

    private static final String CORRECTION_STEP_OPERATION_ENTRY = "correction";

    protected RequestService requestService = new DSpace().getRequestService();

    @Override
    public SectionData getData(SubmissionService submissionService, InProgressSubmission obj,
                                  SubmissionStepConfig config) throws Exception {
        HttpServletRequest request = requestService.getCurrentRequest().getHttpServletRequest();
        Context context = ContextUtil.obtainContext(request);

        ItemCorrectionService itemCorrectionService =
            DSpaceServicesFactory.getInstance().getServiceManager().getApplicationContext()
                .getBean(ItemCorrectionService.class);

        Item item = obj.getItem();
        CorrectionData result = CorrectionData.newCorrection();
        Relationship relationship = itemCorrectionService.getCorrectionItemRelationship(context, item);
        if (relationship == null) {
            // return empty when is not a correction item
            return new SectionData() {
            };
        }
        Map<String, String> metadataToLabelMap = getMetadataLabels(obj.getCollection());
        Set<String> metadataFields =
            metadataToLabelMap.entrySet().stream().map(x -> x.getKey()).collect(Collectors.toSet());

        Item originalItem = relationship.getRightItem();
        Item correctedItem = relationship.getLeftItem();
        //get the difference between items metadata
        for (String metadata : metadataFields) {
            String label = metadataToLabelMap.get(metadata);
            Set<String> originalValue =
                itemService.getMetadataByMetadataString(originalItem, metadata).stream().map(x -> x.getValue().trim())
                    .collect(
                        Collectors.toSet());
            Set<String> correctedValue =
                itemService.getMetadataByMetadataString(correctedItem, metadata).stream().map(x -> x.getValue().trim())
                    .collect(
                        Collectors.toSet());
            //get the common values from the two set
            Set<String> commonValues = new HashSet<String>(originalValue); // use the copy constructor
            commonValues.retainAll(correctedValue);

            //subtract from the original metadata set the common values, these one are the element which has
            //been removed
            originalValue.removeAll(commonValues); //metadata removed by correction
            //subtract from the corrected metadata set the common values, these one are the element which has
            //been added in the correction
            correctedValue.removeAll(commonValues); //metadata added by correction
            if (CollectionUtils.isNotEmpty(originalValue) ||
                CollectionUtils.isNotEmpty(correctedValue)) {
                result.addMetadata(metadata, correctedValue, originalValue, label);
            }
        }
        //get the difference between bistream
        BitstreamService bitstreamService =
            DSpaceServicesFactory.getInstance().getServiceManager().getApplicationContext()
                .getBean(BitstreamService.class);
        Set<Bitstream> originalBitstream = new HashSet<>();
        bitstreamService.getItemBitstreams(context, originalItem).forEachRemaining(x -> originalBitstream.add(x));
        Map<String, Bitstream> checksumBitstreamOriginalMap = originalBitstream.stream().filter(x -> {
            try {
                return !x.isDeleted();
            } catch (SQLException e) {
                return false;
            }
        }).collect(Collectors.toMap(x -> x.getChecksum(), x -> x));

        Set<Bitstream> correctedBitstream = new HashSet();
        bitstreamService.getItemBitstreams(context, correctedItem).forEachRemaining(x -> correctedBitstream.add(x));
        Map<String, Bitstream> checksumBitstreamCorrectedMap = correctedBitstream.stream().filter(x -> {
            try {
                return !x.isDeleted();
            } catch (SQLException e) {
                return false;
            }
        }).collect(Collectors.toMap(x -> x.getChecksum(), x -> x));


        Set<String> union = new HashSet(checksumBitstreamOriginalMap.keySet());
        union.addAll(checksumBitstreamCorrectedMap.keySet());

        //loop over the original bitstream set
        for (String checksum : union) {
            Bitstream original = checksumBitstreamOriginalMap.get(checksum);
            Bitstream corrected = checksumBitstreamCorrectedMap.get(checksum);
            //bitstream exists in both set (original and corrected) check for its medatadata
            CorrectionBitstream correctionBitstream = getBitstreamMetadataCorrections(context, metadataToLabelMap,
                original, corrected);
            if (isBitstreamChanged(correctionBitstream)) {
                result.addBitstream(correctionBitstream);
            }
        }

        return result;
    }

    private boolean isBitstreamChanged(CorrectionBitstream correctionBitstream) {
        return CollectionUtils.isNotEmpty(correctionBitstream.getPolicies()) ||
            CollectionUtils.isNotEmpty(correctionBitstream.getMetadata());
    }

    private CorrectionBitstream getBitstreamMetadataCorrections(Context context,
                                                                Map<String, String> metadataToLabelMap,
                                                                Bitstream originalBitstream,
                                                                Bitstream correctedBitstream) throws SQLException {
        CorrectionBitstream result = CorrectionData.newCorrection().newBitstream();

        String type = "";
        String filename = null;
        CorrectionData.OperationType operationType = null;
        if (originalBitstream == null && correctedBitstream != null) {
            type = correctedBitstream.getFormatDescription(context);
            filename = correctedBitstream.getName();
            operationType = CorrectionData.OperationType.ADD;
        } else if (originalBitstream != null && correctedBitstream == null) {
            type = originalBitstream.getFormatDescription(context);
            filename = originalBitstream.getName();

            operationType = CorrectionData.OperationType.REMOVE;

        } else {
            type = correctedBitstream.getFormatDescription(context);
            filename = correctedBitstream.getName();
            operationType = CorrectionData.OperationType.MODIFY;
        }

        result.addFilename(filename)
            .addOperationType(operationType);

        Set<String> metadataFields = new HashSet();
        if (originalBitstream != null) {
            metadataFields.addAll(
                originalBitstream.getMetadata().stream().map(x -> buildMetadataName(x.getMetadataField())).collect(
                    Collectors.toSet()));
        }
        if (correctedBitstream != null) {
            metadataFields.addAll(
                correctedBitstream.getMetadata().stream().map(x -> buildMetadataName(x.getMetadataField())).collect(
                    Collectors.toSet()));
        }

        for (String metadata : metadataFields) {
            Set<String> originalValues = new HashSet<>();
            if (originalBitstream != null) {
                originalValues = bitstreamService.getMetadata(originalBitstream, metadata, Item.ANY).stream()
                    .map(x -> x.getValue().trim())
                    .collect(Collectors.toSet());
            }
            Set<String> correctedValues = new HashSet<>();
            if (correctedBitstream != null) {
                correctedValues = bitstreamService.getMetadata(correctedBitstream, metadata, Item.ANY).stream()
                    .map(x -> x.getValue().trim())
                    .collect(Collectors.toSet());
            }
            //get the common values from the two set
            Set<String> commonValues = new HashSet<>(originalValues); // use the copy constructor
            commonValues.retainAll(correctedValues);
            originalValues.removeAll(commonValues); //metadata removed by correction
            //subtract from the corrected metadata set the common values, these one are the element which has
            //been added in the correction
            correctedValues.removeAll(commonValues); //metadata added by correction

            if (metadataToLabelMap.containsKey(metadata) &&
                (CollectionUtils.isNotEmpty(correctedValues) ||
                    CollectionUtils.isNotEmpty(originalValues))) {
                result.addMetadata(metadata, correctedValues, originalValues,
                    metadataToLabelMap.get(metadata));
            }
        }


        //check the resource policy on the bit stream
        ResourcePolicy originalPolicy = null;
        ResourcePolicy correctedPolicy = null;

        if (originalBitstream != null) {
            originalPolicy =
                originalBitstream.getResourcePolicies().stream()
                    .filter(x -> ResourcePolicy.TYPE_CUSTOM.equals(x.getRpType())).findFirst().orElse(null);
        }

        if (correctedBitstream != null) {
            correctedPolicy = correctedBitstream.getResourcePolicies().stream()
                .filter(x -> ResourcePolicy.TYPE_CUSTOM.equals(x.getRpType())).findFirst().orElse(null);
        }
        result.addPolicy(checkPolicyAccessCondition(originalPolicy, correctedPolicy));
        result.addPolicy(checkPolicyDescription(originalPolicy, correctedPolicy));
        result.addPolicy(checkPolicyStartDate(originalPolicy, correctedPolicy));
        result.addPolicy(checkPolicyEndDate(originalPolicy, correctedPolicy));
        result.addPolicy(checkPolicyGroup(originalPolicy, correctedPolicy));
        result.addPolicy(checkPolicyPerson(originalPolicy, correctedPolicy));

        return result;
    }

    private CorrectionPolicy checkPolicyAccessCondition(ResourcePolicy originalPolicy,
                                                        ResourcePolicy correctedPolicy) {

        Optional<String> original = Optional.ofNullable(originalPolicy == null ? null :
            originalPolicy.getRpName());
        Optional<String> corrected = Optional.ofNullable(correctedPolicy == null ? null :
            correctedPolicy.getRpName());

        if (original.isEmpty() && corrected.isEmpty()) {
            return null;
        }

        if (originalPolicy == null &&
            correctedPolicy != null &&
            StringUtils.isNotBlank(correctedPolicy.getRpName())) {
            return new CorrectionPolicy(corrected.get(), null, "Access condition type");

        } else if (originalPolicy != null &&
            correctedPolicy == null &&
            StringUtils.isNotBlank(originalPolicy.getRpName())) {
            return new CorrectionPolicy(null, original.get(), "Access condition type");

        } else {
            return new CorrectionPolicy(corrected.get(), original.get(), "Access condition type");
        }
    }

    private CorrectionPolicy checkPolicyDescription(ResourcePolicy originalPolicy,
                                                    ResourcePolicy correctedPolicy) {

        Optional<String> original = Optional.ofNullable(originalPolicy == null ? null :
            originalPolicy.getRpDescription());
        Optional<String> corrected = Optional.ofNullable(correctedPolicy == null ? null :
            correctedPolicy.getRpDescription());
        if (original.isEmpty() && corrected.isEmpty()) {
            return null;
        }
        if (original.isEmpty() && corrected.isPresent()) {
            return new CorrectionPolicy(corrected.get(), null, "Access description");

        } else if (original.isPresent() && corrected.isEmpty()) {
            return new CorrectionPolicy(null, original.get(),
                "Access description");

        } else {
            return new CorrectionPolicy(corrected.get(), original.get(), "Access description");

        }

    }

    private CorrectionPolicy checkPolicyStartDate(ResourcePolicy originalPolicy,
                                                  ResourcePolicy correctedPolicy) {

        Optional<Date> original = Optional.ofNullable(originalPolicy == null ? null :
            originalPolicy.getStartDate());
        Optional<Date> corrected = Optional.ofNullable(correctedPolicy == null ? null :
            correctedPolicy.getStartDate());

        if (original.isEmpty() && corrected.isEmpty()) {
            return null;
        }
        if (original.isEmpty() && corrected.isPresent()) {
            return new CorrectionPolicy(corrected.get().toString(), null, "Access grant from");

        } else if (original.isPresent() && corrected.isEmpty()) {
            return new CorrectionPolicy(null, original.get().toString(), "Access grant from");
        } else {
            return new CorrectionPolicy(corrected.get().toString(), original.get().toString(), "Access grant from");
        }

    }

    private CorrectionPolicy checkPolicyEndDate(ResourcePolicy originalPolicy,
                                                ResourcePolicy correctedPolicy) {
        Optional<Date> original = Optional.ofNullable(originalPolicy == null ? null :
            originalPolicy.getEndDate());
        Optional<Date> corrected = Optional.ofNullable(correctedPolicy == null ? null :
            correctedPolicy.getEndDate());

        if (original.isEmpty() && corrected.isEmpty()) {
            return null;
        }

        if (original.isEmpty() && corrected.isPresent()) {
            return new CorrectionPolicy(corrected.get().toString(),
                null, "Access grant until");
        } else if (original.isPresent() && corrected.isEmpty()) {
            return new CorrectionPolicy(null,
                original.get().toString(), "Access grant until");
        } else {
            return new CorrectionPolicy(corrected.get().toString(),
                original.get().toString(), "Access grant until");
        }

    }

    private CorrectionPolicy checkPolicyPerson(ResourcePolicy originalPolicy,
                                               ResourcePolicy correctedPolicy) {
        Optional<EPerson> original = Optional.ofNullable(originalPolicy == null ? null :
            originalPolicy.getEPerson());
        Optional<EPerson> corrected = Optional.ofNullable(correctedPolicy == null ? null :
            correctedPolicy.getEPerson());

        if (original.isEmpty() && corrected.isEmpty()) {
            return null;
        }
        if (original.isEmpty() && corrected.isPresent()) {
            return new CorrectionPolicy(corrected.get().getFullName(),
                null, "Access grant to user");
        } else if (original.isPresent() && corrected.isEmpty()) {
            return new CorrectionPolicy(null,
                original.get().getFullName(), "Access grant to user");
        } else {
            return new CorrectionPolicy(corrected.get().getFullName(),
                original.get().getFullName(), "Access grant to user");
        }

    }

    private CorrectionPolicy checkPolicyGroup(ResourcePolicy originalPolicy,
                                              ResourcePolicy correctedPolicy) {
        Optional<Group> original = Optional.ofNullable(originalPolicy == null ? null :
            originalPolicy.getGroup());
        Optional<Group> corrected = Optional.ofNullable(correctedPolicy == null ? null :
            correctedPolicy.getGroup());

        if (original.isEmpty() && corrected.isEmpty()) {
            return null;
        }

        if (original.isEmpty() && corrected.isPresent()) {
            return new CorrectionPolicy(corrected.get().getName(), null, "Access grant to group");
        } else if (original.isPresent() && corrected.isEmpty()) {
            return new CorrectionPolicy(null, original.get().getName(), "Access grant to group");
        } else {
            return new CorrectionPolicy(corrected.get().getName(), original.get().getName(), "Access grant to group");
        }

    }


    private Map<String, String> getMetadataLabels(Collection collection) throws Exception {
        Map<String, String> map = new HashMap<>();
        DCInputsReader inputsReader = new DCInputsReader();
        List<DCInputSet> inputSet = inputsReader.getInputsByCollection(collection);
        for (DCInputSet input : inputSet) {
            for (int i = 0; i < input.getFields().length; i++) {
                for (int j = 0; j < input.getFields()[i].length; j++) {
                    DCInput field = input.getFields()[i][j];
                    String fullName = field.getFieldName();
                    String label = field.getLabel();
                    map.put(fullName, label);
                }
            }
        }
        return map;
    }

    private String buildMetadataName(MetadataField metadataField) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(metadataField.getMetadataSchema().getName());
        stringBuffer.append(".");
        stringBuffer.append(metadataField.getElement());
        if (!StringUtils.isBlank(metadataField.getQualifier())) {
            stringBuffer.append(".");
            stringBuffer.append(metadataField.getQualifier());
        }
        return stringBuffer.toString();
    }

    @Override
    public void doPatchProcessing(Context context, HttpServletRequest currentRequest, InProgressSubmission source,
            Operation op, SubmissionStepConfig stepConf) throws Exception {
        if (op.getPath().endsWith("correction")) {

            PatchOperation<String> patchOperation = new PatchOperationFactory()
                .instanceOf(CORRECTION_STEP_OPERATION_ENTRY, op.getOp());
            patchOperation.perform(context, currentRequest, source, op);

        }

    }

}
