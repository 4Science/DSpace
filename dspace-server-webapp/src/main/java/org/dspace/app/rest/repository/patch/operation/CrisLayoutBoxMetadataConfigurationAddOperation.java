/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;
import java.sql.SQLException;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.JsonValueEvaluator;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutFieldBitstream;
import org.dspace.layout.CrisLayoutFieldMetadata;
import org.dspace.layout.CrisMetadataGroup;
import org.dspace.layout.service.CrisLayoutFieldService;
import org.dspace.layout.service.CrisLayoutMetadataGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for CrisLayoutBoxMetadataConfiguration patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/layout/boxmetadataconfiguration/<:box-id> --H
 * "Authorization: Bearer ..." -H 'Content-Type: application/json' --data
 * '[{"op":"add","path":"/rows/0/<:fields>/0", "value":{"metadata":"orgunit.identifier.name",
 * "label":"Department Name", "rendering":"browselink", "fieldType":"metadata", "style":null }}]'
 * </code>
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class CrisLayoutBoxMetadataConfigurationAddOperation<D> extends PatchOperation<D> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_CONFIGURATION_PATH = "^/rows/[0-9]+/fields/.*$";

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MetadataFieldService metadataService;


    @Autowired
    private CrisLayoutFieldService fieldService;

    @Autowired
    private CrisLayoutMetadataGroupService crisLayoutMetadataGroupService;
    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.patch.operation.PatchOperation#perform
     * (org.dspace.core.Context, java.lang.Object, org.dspace.app.rest.model.patch.Operation)
     */
    @Override
    public D perform(Context context, D resource, Operation operation) throws SQLException {
        checkOperationValue(operation.getValue());
        if (supports(resource, operation)) {
            CrisLayoutBox box = (CrisLayoutBox) resource;
            try {
                JsonNode value = null;
                if (operation.getValue() instanceof JsonValueEvaluator) {
                    value = ((JsonValueEvaluator) operation.getValue()).getValueNode();
                } else {
                    value = objectMapper.readTree((String) operation.getValue());
                }
                Integer row = null;
                Integer position = null;
                String[] tks = operation.getPath().split("/");
                if (tks != null && tks.length > 0) {
                    for (int i = 0; i < tks.length; i++) {
                        if (tks[i].equalsIgnoreCase("rows") && tks.length > i + 1) {
                            row = parseInteger(tks[++i]);
                        } else if (tks[i].equalsIgnoreCase("fields") && tks.length > i + 1) {
                            position = parseInteger(tks[++i]);
                        }
                    }
                }
                if (value.isArray()) {
                    for (JsonNode v : value) {

                        CrisLayoutField layoutField = getLayoutFieldFromJsonNode(
                                context, box, row, position, v);
                        layoutField = fieldService.create(context, layoutField);
                        // if it has nested metadata-group
                        if (v.get("fieldType").asText().equalsIgnoreCase("metadatagroup")) {
                                     //save nested metadata
                            saveNestedMetadata(context, position, v.get("metadatagroup"), layoutField);
                        }

                    }
                } else {
                    CrisLayoutField layoutField = getLayoutFieldFromJsonNode(
                            context, box, row, position, value);
                    layoutField = fieldService.create(context, layoutField);
                    if (value.get("fieldType").asText().equalsIgnoreCase("metadatagroup")) {
                        //save nested metadata
                        saveNestedMetadata(context, position, value.get("metadatagroup"), layoutField);
                    }
                }
            } catch (UnprocessableEntityException e) {
                throw new UnprocessableEntityException(e.getMessage(), e);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        } else {
            throw new DSpaceBadRequestException("CrisLayoutBoxConfigurationAddOperation " +
                "does not support this operation");
        }
        return resource;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.patch.operation.PatchOperation#supports
     * (java.lang.Object, org.dspace.app.rest.model.patch.Operation)
     */
    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof CrisLayoutBox && operation.getOp().trim().equalsIgnoreCase(OPERATION_ADD)
                && operation.getPath().matches(OPERATION_CONFIGURATION_PATH));
    }

    /**
     * Create new CrisLayoutField from json node
     * @param context DSpace current context
     * @param
     * @param row
     * @param
     * @param node json value
     * @return
     * @throws JsonProcessingException
     * @throws SQLException
     * @throws AuthorizeException
     */
    private CrisLayoutField getLayoutFieldFromJsonNode(
            Context context, CrisLayoutBox box, Integer row, Integer position, JsonNode node)
            throws JsonProcessingException, SQLException, UnprocessableEntityException, AuthorizeException {
        JsonNode fieldType = node.get("fieldType");
        if (fieldType == null || StringUtils.isEmpty(fieldType.asText())) {
            throw new UnprocessableEntityException("The field must specify a fieldType");
        }

        CrisLayoutField field = null;
        String metadataType = null;
        if (StringUtils.equalsIgnoreCase(fieldType.asText(), "bitstream")) {
            field = new CrisLayoutFieldBitstream();
            JsonNode bitstreamNode = node.get("bitstream");
            if (bitstreamNode == null) {
                throw new UnprocessableEntityException("Bitstream node cannot be null for a bitstream fieldType");
            }
            JsonNode bundleNode = bitstreamNode.get("bundle");
            if (bundleNode != null && bundleNode.asText() != null) {
                ((CrisLayoutFieldBitstream) field).setBundle(bundleNode.asText());
            } else {
                throw new UnprocessableEntityException("Bundle cannot be null for a bitstream fieldType");
            }
            JsonNode metadataValueNode = bitstreamNode.get("metadataValue");
            if (bundleNode != null && bundleNode.asText() != null) {
                ((CrisLayoutFieldBitstream) field).setMetadataValue(metadataValueNode.asText());
            }
            JsonNode metadataNode = bitstreamNode.get("metadataField");
            if (metadataNode != null && metadataNode.asText() != null) {
                metadataType = metadataNode.asText();
            }
        } else {
            if (StringUtils.equalsIgnoreCase(fieldType.asText(), "metadatagroup")) {
                field = new CrisLayoutField();
                JsonNode metadataNode = node.get("metadatagroup").get("leading");
                if (metadataNode != null && metadataNode.asText() != null) {
                    metadataType = metadataNode.asText();
                }
            } else {
                field = new CrisLayoutFieldMetadata();
                JsonNode metadataNode = node.get("metadata");
                if (metadataNode != null && metadataNode.asText() != null) {
                    metadataType = metadataNode.asText();
                }
            }

        }

        field.setRow(row);
        field.setBox(box);

        MetadataField metadataField = metadataService.findByString(context, metadataType, '.');
        if (metadataField == null) {
            throw new UnprocessableEntityException("MetadataField <" + metadataType + "> not exsists!");
        }
        field.setMetadataField(metadataField);

        JsonNode labelNode = node.get("label");
        if (labelNode != null && labelNode.asText() != null) {
            field.setLabel(labelNode.asText());
        }

        JsonNode renderingNode = node.get("rendering");
        if (renderingNode != null && renderingNode.asText() != null) {
            field.setRendering(renderingNode.asText());
        }

        JsonNode styleNode = node.get("style");
        if (styleNode != null && styleNode.asText() != null) {
            field.setStyle(styleNode.asText());
        }

        JsonNode styleLabelNode = node.get("styleLabel");
        if (styleLabelNode != null && styleLabelNode.asText() != null) {
            field.setStyleLabel(styleLabelNode.asText());
        }

        JsonNode styleValueNode = node.get("styleValue");
        if (styleValueNode != null && styleValueNode.asText() != null) {
            field.setStyleValue(styleValueNode.asText());
        }

        Integer priority = null;
        List<CrisLayoutField> fields = fieldService.findFieldByBoxId(context, box.getID(), row);
        if (fields != null && !fields.isEmpty()) {
            if (position == null || position > fields.size()) {
                position = fields.size();
            }
            priority = position;
            fields.add(position, field);
            for (int i = position + 1; i < fields.size(); i++) {
                fields.get(i).setPriority(
                        fields.get(i).getPriority() + 1);
            }
            fieldService.update(context, fields.subList(position + 1, fields.size()));
        } else {
            priority = 0;
        }
        field.setPriority(priority);

        return field;
    }

    /**
     * Returns an Integer object holding the value of the specified String,
     * if the string cannot be parsed as an integer returns null
     * @param val
     * @return
     */
    private Integer parseInteger(String val) {
        Integer value = null;
        try {
            value = Integer.valueOf(val);
        } catch (Exception e) {
            value = null;
        }
        return value;
    }

    private void saveNestedMetadata( Context context, Integer position,
                                     JsonNode metadatagroup, CrisLayoutField crisLayoutField)
        throws SQLException, AuthorizeException {
        if (metadatagroup.get("elements") != null  && metadatagroup.get("elements").isArray() ) {
            // search if nested metadata exists
            for (JsonNode v : metadatagroup.get("elements")) {
                CrisMetadataGroup nestedField = new CrisMetadataGroup();
                // it is a metadata
                //control if metadata exists in database
                String metadata = null;
                JsonNode metadataNode = v.get("metadata");
                if (metadataNode != null && metadataNode.asText() != null) {

                    metadata = metadataNode.asText();
                }
                MetadataField metadataField = metadataService.findByString(context, metadata, '.');
                if (metadataField == null) {
                    throw new UnprocessableEntityException("MetadataField <" + metadata + "> not exsists!");
                }
                nestedField.setMetadataField(metadataField);
                //else new cris layout field nested will be added in database
                JsonNode labelNode = v.get("label");
                if (labelNode != null && labelNode.asText() != null) {
                    nestedField.setLabel(labelNode.asText());
                }

                JsonNode renderingNode = v.get("rendering");
                if (renderingNode != null && renderingNode.asText() != null) {
                    nestedField.setRendering(renderingNode.asText());
                }

                JsonNode styleNode = v.get("style");
                if (styleNode != null && styleNode.asText() != null) {
                    nestedField.setStyle(styleNode.asText());
                }

                JsonNode styleLabelNode = v.get("styleLabel");
                if (styleLabelNode != null && styleLabelNode.asText() != null) {
                    nestedField.setStyleLabel(styleLabelNode.asText());
                }

                JsonNode styleValueNode = v.get("styleValue");
                if (styleValueNode != null && styleValueNode.asText() != null) {
                    nestedField.setStyleValue(styleValueNode.asText());
                }
                Integer priority = 0;
                List<CrisMetadataGroup> nested_fields = crisLayoutMetadataGroupService.
                    findNestedFieldByFieldId(context,crisLayoutField.getID()) ;
                if (nested_fields != null && !nested_fields.isEmpty()) {
                    if (position == null || position > nested_fields.size()) {
                        position = nested_fields.size();
                    }
                    priority = position;
                    nested_fields.add(position, nestedField);
                    for (int i = position + 1; i < nested_fields.size(); i++) {


                        nested_fields.get(i).setPriority(
                            nested_fields.get(i).getPriority() + 1);
                    }
                    crisLayoutMetadataGroupService
                        .update(context, nested_fields.subList(position + 1, nested_fields.size()));
                } else {
                    priority = 0;
                }
                nestedField.setPriority(priority);
                nestedField.setCrisLayoutField(crisLayoutField);
                crisLayoutMetadataGroupService.create(context, nestedField);
            }

        }
    }
}