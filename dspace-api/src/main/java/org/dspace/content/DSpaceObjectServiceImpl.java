/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.handle.service.HandleService;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation class for the DSpaceObject.
 * All DSpaceObject service classes should extend this class since it implements some basic methods which all
 * DSpaceObjects
 * are required to have.
 *
 * @param <T> class type
 * @author kevinvandevelde at atmire.com
 */
public abstract class DSpaceObjectServiceImpl<T extends DSpaceObject> implements DSpaceObjectService<T> {

    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(DSpaceObjectServiceImpl.class);

    @Autowired(required = true)
    protected ChoiceAuthorityService choiceAuthorityService;
    @Autowired(required = true)
    protected HandleService handleService;
    @Autowired(required = true)
    protected MetadataValueService metadataValueService;
    @Autowired(required = true)
    protected MetadataFieldService metadataFieldService;
    @Autowired(required = true)
    protected MetadataAuthorityService metadataAuthorityService;
    @Autowired(required = true)
    protected RelationshipService relationshipService;

    public DSpaceObjectServiceImpl() {

    }

    @Override
    public String getName(T dso) {
        String value = getMetadataFirstValue(dso, MetadataSchemaEnum.DC.getName(), "title", null, Item.ANY);
        return value == null ? "" : value;
    }

    @Override
    public ArrayList<String> getIdentifiers(Context context, T dso) {
        ArrayList<String> identifiers = new ArrayList<>();

        IdentifierService identifierService =
            new DSpace().getSingletonService(IdentifierService.class);

        if (identifierService != null) {
            identifiers.addAll(identifierService.lookup(context, dso));
        } else {
            log.warn("No IdentifierService found, will return an list containing "
                         + "the Handle only.");
            if (dso.getHandle() != null) {
                identifiers.add(handleService.getCanonicalForm(dso.getHandle()));
            }
        }

        return identifiers;
    }

    @Override
    public DSpaceObject getParentObject(Context context, T dso) throws SQLException {
        return null;
    }

    @Override
    public DSpaceObject getAdminObject(Context context, T dso, int action) throws SQLException {
        if (action == Constants.ADMIN) {
            throw new IllegalArgumentException("Illegal call to the DSpaceObject.getAdminObject method");
        }
        return dso;
    }

    @Override
    public String getTypeText(T dso) {
        return Constants.typeText[dso.getType()];
    }

    @Override
    public List<MetadataValue> getMetadata(T dso, String schema, String element, String qualifier, String lang) {
        // Build up list of matching values
        List<MetadataValue> values = new ArrayList<>();
        for (MetadataValue dcv : dso.getMetadata()) {
            if (match(schema, element, qualifier, dcv)) {
                values.add(dcv);
            }
        }

        values = getFilteredMetadataValuesByLanguage(values, lang);

        // Sort the metadataValues if they have been modified,
        // is used to preserve the default order.
        if (dso.isMetadataModified()) {
            values.sort(MetadataValueComparators.defaultComparator);
        }
        // Create an array of matching values
        return values;
    }

    @Override
    public List<MetadataValue> getMetadataByMetadataString(T dso, String mdString) {
        StringTokenizer dcf = new StringTokenizer(mdString, ".");

        String[] tokens = {"", "", ""};
        int i = 0;
        while (dcf.hasMoreTokens()) {
            tokens[i] = dcf.nextToken().trim();
            i++;
        }
        String schema = tokens[0];
        String element = tokens[1];
        String qualifier = tokens[2];

        List<MetadataValue> values = getMetadata(dso, schema, element, qualifier);

        return values;
    }

    private List<MetadataValue> getMetadata(T dso, String schema, String element, String qualifier) {
        List<MetadataValue> values;
        if (Item.ANY.equals(qualifier)) {
            values = getMetadata(dso, schema, element, Item.ANY, Item.ANY);
        } else if ("".equals(qualifier)) {
            values = getMetadata(dso, schema, element, null, Item.ANY);
        } else {
            values = getMetadata(dso, schema, element, qualifier, Item.ANY);
        }
        return values;
    }

    @Override
    public String getMetadata(T dso, String value) {
        List<MetadataValue> metadataValues = getMetadataByMetadataString(dso, value);

        if (CollectionUtils.isNotEmpty(metadataValues)) {
            return metadataValues.iterator().next().getValue();
        }
        return null;
    }

    @Override
    public List<MetadataValue> getMetadata(T dso, String mdString, String authority) {
        String[] elements = getElements(mdString);
        return getMetadata(dso, elements[0], elements[1], elements[2], elements[3], authority);
    }

    @Override
    public List<MetadataValue> getMetadata(T dso, String schema, String element, String qualifier, String lang,
                                           String authority) {
        List<MetadataValue> metadata = getMetadata(dso, schema, element, qualifier, lang);
        List<MetadataValue> result = new ArrayList<>(metadata);
        if (!Item.ANY.equals(authority)) {
            Iterator<MetadataValue> iterator = result.iterator();
            while (iterator.hasNext()) {
                MetadataValue metadataValue = iterator.next();
                if (!StringUtils.equals(authority, metadataValue.getAuthority())) {
                    iterator.remove();
                }
            }
        }
        return result;
    }

    @Override
    public List<MetadataValue> addMetadata(Context context, T dso, String schema, String element, String qualifier,
                            String lang, List<String> values) throws SQLException {
        MetadataField metadataField = metadataFieldService.findByElement(context, schema, element, qualifier);
        if (metadataField == null) {
            throw new SQLException(
                "bad_dublin_core schema=" + schema + "." + element + "." + qualifier + ". Metadata field does not " +
                    "exist!");
        }

        return addMetadata(context, dso, metadataField, lang, values);
    }

    @Override
    public List<MetadataValue> addMetadata(Context context, T dso, String schema, String element, String qualifier,
                               String lang, List<String> values, List<String> authorities, List<Integer> confidences)
        throws SQLException {
        // We will not verify that they are valid entries in the registry
        // until update() is called.
        MetadataField metadataField = metadataFieldService.findByElement(context, schema, element, qualifier);
        if (metadataField == null) {
            throw new SQLException(
                "bad_dublin_core schema=" + schema + "." + element + "." + qualifier + ". Metadata field does not " +
                    "exist!");
        }
        return addMetadata(context, dso, metadataField, lang, values, authorities, confidences);
    }

    @Override
    public List<MetadataValue> addMetadata(Context context, T dso, MetadataField metadataField, String lang,
                                           List<String> values, List<String> authorities, List<Integer> confidences)
            throws SQLException {

        //Set place to list length of all metadatavalues for the given schema.element.qualifier combination.
        // Subtract one to adhere to the 0 as first element rule
        final Supplier<Integer> placeSupplier =  () ->
                this.getMetadata(dso, metadataField.getMetadataSchema().getName(), metadataField.getElement(),
                        metadataField.getQualifier(), Item.ANY).size() - 1;

        return addMetadata(context, dso, metadataField, lang, values, authorities, confidences, placeSupplier);

    }

    /**
     * Add metadata value(s) to a MetadataField of a DSpace Object
     * @param context current DSpace context
     * @param dso DSpaceObject to modify
     * @param metadataField MetadataField to add values to
     * @param lang Language code to add
     * @param values One or more metadata values to add
     * @param authorities One or more authorities to add
     * @param confidences One or more confidences to add (for authorities)
     * @param placeSupplier Supplier of "place" for new metadata values
     * @return List of newly added metadata values
     * @throws SQLException if database error occurs
     * @throws IllegalArgumentException for an empty list of values
     */
    public List<MetadataValue> addMetadata(Context context, T dso, MetadataField metadataField, String lang,
            List<String> values, List<String> authorities, List<Integer> confidences, Supplier<Integer> placeSupplier)
                    throws SQLException {

        return addMetadata(context, dso, metadataField, lang, values, authorities, confidences, placeSupplier, null);
    }

    public List<MetadataValue> addMetadata(Context context, T dso, MetadataField metadataField, String lang,
        List<String> values, List<String> authorities, List<Integer> confidences, Supplier<Integer> placeSupplier,
        Integer securityLevel) throws SQLException {

        // Throw an error if we are attempting to add empty values
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Cannot add empty values to a new metadata field " +
                                                   metadataField.toString() + " on object with uuid = " +
                                                   dso.getID().toString() + " and type = " + getTypeText(dso));
        }

        Collection collection = getCollection(context, dso);
        boolean authorityControlled = metadataAuthorityService.isAuthorityAllowed(metadataField,
                dso.getType(), collection);

        boolean nonValidAuthority = isNonValidAuthority(authorityControlled, authorities);
        if (nonValidAuthority) {
            throw new IllegalArgumentException("The metadata field \"" +
                    metadataField.toString()
                    + "\"" + " is not authority controlled but authorities were provided. Values:\""
                    + authorities + "\"");
        }

        boolean authorityRequired = metadataAuthorityService.isAuthorityRequired(metadataField, dso.getType(),
                collection);
        // Throw an error if we are attempting to add empty values
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Cannot add empty values to a new metadata field " +
                                                   metadataField.toString() + " on object with uuid = " +
                                                   dso.getID().toString() + " and type = " + getTypeText(dso));
        }

        List<MetadataValue> newMetadata = new ArrayList<>();
        // We will not verify that they are valid entries in the registry
        // until update() is called.
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) != null) {
                if (authorities != null && authorities.size() >= i) {
                    if (StringUtils.startsWith(authorities.get(i), Constants.VIRTUAL_AUTHORITY_PREFIX)) {
                        continue;
                    }
                }
                MetadataValue metadataValue = metadataValueService.create(context, dso, metadataField);
                newMetadata.add(metadataValue);

                metadataValue.setPlace(placeSupplier.get());
                metadataValue.setSecurityLevel(securityLevel);

                metadataValue.setLanguage(lang == null ? null : lang.trim());

                // Logic to set Authority and Confidence:
                //  - normalize an empty string for authority to NULL.
                //  - if authority key is present, use given confidence or NOVALUE if not given
                //  - otherwise, preserve confidence if meaningful value was given since it may document a failed
                // authority lookup
                //  - CF_UNSET signifies no authority nor meaningful confidence.
                //  - it's possible to have empty authority & CF_ACCEPTED if e.g. user deletes authority key
                if (authorityControlled) {
                    if (authorities != null && authorities.get(i) != null && authorities.get(i).length() > 0) {
                        metadataValue.setAuthority(authorities.get(i));
                        metadataValue.setConfidence(confidences == null ? Choices.CF_NOVALUE : confidences.get(i));
                    } else {
                        metadataValue.setAuthority(null);
                        metadataValue.setConfidence(confidences == null ? Choices.CF_UNSET : confidences.get(i));
                    }
                    // authority sanity check: if authority is required, was it supplied?
                    // XXX FIXME? can't throw a "real" exception here without changing all the callers to expect it, so
                    // use a runtime exception
                    if (authorityRequired &&
                        (metadataValue.getAuthority() == null || metadataValue.getAuthority().length() == 0) &&
                        isNotPlaceholderMetadataValue(values.get(i).trim())
                    ) {
                        throw new IllegalArgumentException("The metadata field \"" + metadataField
                                .toString() + "\" requires an authority key but none was provided. Value=\"" + values
                                .get(i) + "\"");
                    }
                }
                // remove control unicode char
                String temp = values.get(i).trim();
                char[] dcvalue = temp.toCharArray();
                for (int charPos = 0; charPos < dcvalue.length; charPos++) {
                    if (Character.isISOControl(dcvalue[charPos]) &&
                            !String.valueOf(dcvalue[charPos]).equals("\u0009") &&
                            !String.valueOf(dcvalue[charPos]).equals("\n") &&
                            !String.valueOf(dcvalue[charPos]).equals("\r")) {
                        dcvalue[charPos] = ' ';
                    }
                }
                metadataValue.setValue(String.valueOf(dcvalue));
                //An update here isn't needed, this is persited upon the merge of the owning object
//            metadataValueService.update(context, metadataValue);
                dso.addDetails(metadataField.toString());
            }
        }
        setMetadataModified(dso);
        return newMetadata;
    }

    private Collection getCollection(Context context, T dso) throws SQLException {
        Collection col = null;
        switch (dso.getType()) {
            case Constants.ITEM:
                Item item = (Item) dso;
                col = (Collection) item.getItemService().getParentObject(context, item);
                break;
            case Constants.BITSTREAM:
                Bitstream bit = (Bitstream) dso;
                BitstreamService bitService = ContentServiceFactory.getInstance().getBitstreamService();
                DSpaceObject pDSO = bitService.getParentObject(context, bit);
                if (pDSO != null && pDSO.getType() == Constants.ITEM) {
                    Item pItem = (Item) pDSO;
                    col = (Collection) pItem.getItemService().getParentObject(context, pItem);
                }
                return null;
            default:
                break;
        }
        return col;
    }

    private boolean isNotPlaceholderMetadataValue(String metadataValue) {
        return !StringUtils.equals(metadataValue, PLACEHOLDER_PARENT_METADATA_VALUE);
    }

    @Override
    public MetadataValue addSecuredMetadata(Context context, T dso, String schema, String element,
        String qualifier, String lang, String value, String authority, int confidence, Integer securityLevel)
        throws SQLException {

        MetadataField metadataField = metadataFieldService.findByElement(context, schema, element, qualifier);
        if (metadataField == null) {
            throw new SQLException(
                "bad_dublin_core schema=" + schema + "." + element + "." + qualifier + ". Metadata field does not " +
                    "exist!");
        }

        return addSecuredMetadata(context, dso, metadataField, lang, value, authority, confidence, securityLevel);

    }

    @Override
    public MetadataValue addSecuredMetadata(Context context, T dso, MetadataField metadataField, String lang,
        String value, String authority, int confidence, Integer securityLevel) throws SQLException {

        // Set place to list length of all metadatavalues for the given
        // schema.element.qualifier combination.
        // Subtract one to adhere to the 0 as first element rule
        final Supplier<Integer> placeSupplier = () -> this
            .getMetadata(dso, metadataField.getMetadataSchema().getName(), metadataField.getElement(),
                metadataField.getQualifier(), Item.ANY)
            .size() - 1;

        return addMetadata(context, dso, metadataField, lang, Arrays.asList(value), Arrays.asList(authority),
            Arrays.asList(confidence), placeSupplier, securityLevel).stream().findFirst().orElse(null);

    }

    @Override
    public void addAndShiftRightSecuredMetadata(Context context, T dso, String schema, String element, String qualifier,
        String lang, String value, String authority, int confidence, int index, Integer securitylevel)
        throws SQLException {
        List<MetadataValue> list = getMetadata(dso, schema, element, qualifier);

        int idx = 0;
        int place = 0;
        boolean last = true;
        for (MetadataValue rr : list) {
            if (idx == index) {
                MetadataValue newMetadata = addMetadata(context, dso, schema, element, qualifier,
                        lang, value, authority, confidence);

                moveSingleMetadataValue(context, dso, place, newMetadata);
                place++;
                last = false;
            }
            moveSingleMetadataValue(context, dso, place, rr);
            place++;
            idx++;
        }
        if (last) {
            addSecuredMetadata(context, dso, schema, element, qualifier,
                    lang, value, authority, confidence, securitylevel);

        }
    }

    @Override
    public MetadataValue addMetadata(Context context, T dso, MetadataField metadataField, String language,
                            String value, String authority, int confidence) throws SQLException {
        List<MetadataValue> metadataValues =
            addMetadata(context, dso, metadataField, language, Arrays.asList(value), Arrays.asList(authority),
                        Arrays.asList(confidence));
        return CollectionUtils.isNotEmpty(metadataValues) ? metadataValues.get(0) : null;
    }

    @Override
    public MetadataValue addMetadata(Context context, T dso, String schema, String element, String qualifier,
                             String lang, String value) throws SQLException {
        List<MetadataValue> metadataValues =
            addMetadata(context, dso, schema, element, qualifier, lang, Arrays.asList(value));
        return CollectionUtils.isNotEmpty(metadataValues) ? metadataValues.get(0) : null;
    }

    @Override
    public MetadataValue addMetadata(Context context, T dso, MetadataField metadataField, String language, String value)
        throws SQLException {
        List<MetadataValue> metadataValues =
            addMetadata(context, dso, metadataField, language, Arrays.asList(value));
        return CollectionUtils.isNotEmpty(metadataValues) ? metadataValues.get(0) : null;
    }

    @Override
    public List<MetadataValue> addMetadata(Context context, T dso, MetadataField metadataField, String language,
                                           List<String> values)
        throws SQLException {
        if (metadataField != null) {
            String fieldKey = metadataAuthorityService
                .makeFieldKey(metadataField.getMetadataSchema().getName(), metadataField.getElement(),
                              metadataField.getQualifier());
            Collection collection = getCollection(context, dso);
            if (metadataAuthorityService.isAuthorityAllowed(metadataField, dso.getType(), collection)
                    && choiceAuthorityService.isChoicesConfigured(fieldKey, dso.getType(), collection)) {
                List<String> authorities = new ArrayList<>();
                List<Integer> confidences = new ArrayList<>();
                for (int i = 0; i < values.size(); ++i) {
                    getAuthoritiesAndConfidences(fieldKey, dso.getType(), collection,
                            values, authorities, confidences, i);
                }
                return addMetadata(context, dso, metadataField, language, values, authorities, confidences);
            } else {
                return addMetadata(context, dso, metadataField, language, values, null, null);
            }
        }
        return new ArrayList<>(0);
    }

    @Override
    public MetadataValue addMetadata(Context context, T dso, String schema, String element, String qualifier,
                            String lang, String value, String authority, int confidence) throws SQLException {
        return addMetadata(context, dso, schema, element, qualifier, lang, Arrays.asList(value),
                Arrays.asList(authority), Arrays.asList(confidence)).stream().findFirst().orElse(null);
    }

    @Override
    public void clearMetadata(Context context, T dso, String schema, String element, String qualifier, String lang)
        throws SQLException {
        Iterator<MetadataValue> metadata = dso.getMetadata().iterator();
        while (metadata.hasNext()) {
            MetadataValue metadataValue = metadata.next();
            // If this value matches, delete it
            if (match(schema, element, qualifier, lang, metadataValue)) {
                metadata.remove();
                metadataValueService.delete(context, metadataValue);
            }
        }
        dso.setMetadataModified();
    }

    @Override
    public void removeMetadataValues(Context context, T dso, List<MetadataValue> values) throws SQLException {
        Iterator<MetadataValue> metadata = dso.getMetadata().iterator();
        while (metadata.hasNext()) {
            MetadataValue metadataValue = metadata.next();
            if (values.contains(metadataValue)) {
                metadata.remove();
                metadataValueService.delete(context, metadataValue);
            }
        }
        dso.setMetadataModified();
    }

    /**
     * Retrieve first metadata field value
     *
     * @param dso       The DSpaceObject which we ask for metadata.
     * @param schema    the schema for the metadata field. <em>Must</em> match
     *                  the <code>name</code> of an existing metadata schema.
     * @param element   the element to match, or <code>Item.ANY</code>
     * @param qualifier the qualifier to match, or <code>Item.ANY</code>
     * @param language  the language to match, or <code>Item.ANY</code>
     * @return the first metadata field value
     */
    @Override
    public String getMetadataFirstValue(T dso, String schema, String element, String qualifier, String language) {
        List<MetadataValue> metadataValues = getMetadata(dso, schema, element, qualifier, language);
        if (CollectionUtils.isNotEmpty(metadataValues)) {
            return metadataValues.iterator().next().getValue();
        }
        return null;
    }

    /**
     * Retrieve first metadata field value
     *
     * @param dso       The DSpaceObject which we ask for metadata.
     * @param field     {schema, element, qualifier} for the desired field.
     * @param language  the language to match, or <code>Item.ANY</code>
     * @return the first metadata field value
     */
    @Override
    public String getMetadataFirstValue(T dso, MetadataFieldName field, String language) {
        List<MetadataValue> metadataValues
                = getMetadata(dso, field.schema, field.element, field.qualifier, language);
        if (CollectionUtils.isNotEmpty(metadataValues)) {
            return metadataValues.get(0).getValue();
        }
        return null;
    }

    /**
     * Set first metadata field value
     *
     * @throws SQLException if database error
     */
    @Override
    public void setMetadataSingleValue(Context context, T dso, String schema, String element, String qualifier,
                                       String language, String value) throws SQLException {
        if (value != null) {
            clearMetadata(context, dso, schema, element, qualifier, language);
            addMetadata(context, dso, schema, element, qualifier, language, value);
            dso.setMetadataModified();
        }
    }

    @Override
    public void setMetadataSingleValue(Context context, T dso, MetadataFieldName field,
            String language, String value)
            throws SQLException {
        if (value != null) {
            clearMetadata(context, dso, field.schema, field.element, field.qualifier,
                    language);

            String newValueLanguage = Item.ANY.equals(language) ? null : language;
            addMetadata(context, dso, field.schema, field.element, field.qualifier,
                    newValueLanguage, value);
            dso.setMetadataModified();
        }
    }

    /**
     * Utility method for pattern-matching metadata elements.  This
     * method will return <code>true</code> if the given schema,
     * element, qualifier and language match the schema, element,
     * qualifier and language of the <code>DCValue</code> object passed
     * in.  Any or all of the element, qualifier and language passed
     * in can be the <code>Item.ANY</code> wildcard.
     *
     * @param schema        the schema for the metadata field. <em>Must</em> match
     *                      the <code>name</code> of an existing metadata schema.
     * @param element       the element to match, or <code>Item.ANY</code>
     * @param qualifier     the qualifier to match, or <code>Item.ANY</code>
     * @param language      the language to match, or <code>Item.ANY</code>
     * @param metadataValue the Dublin Core value
     * @return <code>true</code> if there is a match
     */
    protected boolean match(String schema, String element, String qualifier,
                            String language, MetadataValue metadataValue) {

        MetadataField metadataField = metadataValue.getMetadataField();
        MetadataSchema metadataSchema = metadataField.getMetadataSchema();
        // We will attempt to disprove a match - if we can't we have a match
        if (!Item.ANY.equals(element) && !StringUtils.equals(element, metadataField.getElement())) {
            // Elements do not match, no wildcard
            return false;
        }

        if (StringUtils.isBlank(qualifier)) {
            // Value must be unqualified
            if (metadataField.getQualifier() != null) {
                // Value is qualified, so no match
                return false;
            }
        } else if (!Item.ANY.equals(qualifier)) {
            // Not a wildcard, so qualifier must match exactly
            if (!StringUtils.equals(qualifier, metadataField.getQualifier())) {
                return false;
            }
        }

        if (language == null) {
            // Value must be null language to match
            if (metadataValue.getLanguage() != null) {
                // Value is qualified, so no match
                return false;
            }
        } else if (!Item.ANY.equals(language)) {
            // Not a wildcard, so language must match exactly
            if (!StringUtils.equals(language, metadataValue.getLanguage())) {
                return false;
            }
        }

        if (!Item.ANY.equals(schema)) {
            if (!StringUtils.equals(schema, metadataSchema.getName())) {
                // The namespace doesn't match
                return false;
            }
        }

        // If we get this far, we have a match
        return true;
    }

    /**
     * Utility method for pattern-matching metadata elements.  This
     * method will return <code>true</code> if the given schema,
     * element and qualifier match the schema, element,
     * and qualifier of the <code>DCValue</code> object passed
     * in.  Any or all of the element and qualifier passed
     * in can be the <code>Item.ANY</code> wildcard.
     *
     * @param schema        the schema for the metadata field. <em>Must</em> match
     *                      the <code>name</code> of an existing metadata schema.
     * @param element       the element to match, or <code>Item.ANY</code>
     * @param qualifier     the qualifier to match, or <code>Item.ANY</code>
     * @param metadataValue the Dublin Core value
     * @return <code>true</code> if there is a match
     */
    protected boolean match(String schema, String element, String qualifier, MetadataValue metadataValue) {

        MetadataField metadataField = metadataValue.getMetadataField();
        MetadataSchema metadataSchema = metadataField.getMetadataSchema();
        // We will attempt to disprove a match - if we can't we have a match
        if (!element.equals(Item.ANY) && !element.equals(metadataField.getElement())) {
            // Elements do not match, no wildcard
            return false;
        }

        if (StringUtils.isBlank(qualifier)) {
            // Value must be unqualified
            if (metadataField.getQualifier() != null) {
                // Value is qualified, so no match
                return false;
            }
        } else if (!qualifier.equals(Item.ANY)) {
            // Not a wildcard, so qualifier must match exactly
            if (!qualifier.equals(metadataField.getQualifier())) {
                return false;
            }
        }

        if (!schema.equals(Item.ANY)) {
            if (metadataSchema != null && !metadataSchema.getName().equals(schema)) {
                // The namespace doesn't match
                return false;
            }
        }

        // If we get this far, we have a match
        return true;
    }

    protected void getAuthoritiesAndConfidences(String fieldKey, int dsoType, Collection collection,
            List<String> values, List<String> authorities, List<Integer> confidences, int i) {
        String locale = I18nUtil.getDefaultLocale().getLanguage();
        if (dsoType == Constants.ITEM) {
            Choices c = choiceAuthorityService.getBestMatch(fieldKey, values.get(i), dsoType, collection, locale);
            authorities.add(c.values.length > 0 ? c.values[0].authority : null);
            confidences.add(c.confidence);
        } else {
            authorities.add(null);
            confidences.add(Choices.CF_UNSET);
        }
    }


    /**
     * Splits "schema.element.qualifier.language" into an array.
     * <p>
     * The returned array will always have length greater than or equal to 4
     * <p>
     * Values in the returned array can be empty or null.
     *
     * @param fieldName field name
     * @return array
     */
    protected String[] getElements(String fieldName) {
        String[] tokens = StringUtils.split(fieldName, ".");

        int add = 4 - tokens.length;
        if (add > 0) {
            tokens = ArrayUtils.addAll(tokens, new String[add]);
        }

        return tokens;
    }

    /**
     * Splits "schema.element.qualifier.language" into an array.
     * <p>
     * The returned array will always have length greater than or equal to 4
     * <p>
     * When @param fill is true, elements that would be empty or null are replaced by Item.ANY
     *
     * @param fieldName field name
     * @return array
     */
    protected String[] getElementsFilled(String fieldName) {
        String[] elements = getElements(fieldName);
        for (int i = 0; i < elements.length; i++) {
            if (StringUtils.isBlank(elements[i])) {
                elements[i] = Item.ANY;
            }
        }
        return elements;
    }

    protected String[] getMDValueByField(String field) {
        StringTokenizer dcf = new StringTokenizer(field, ".");

        String[] tokens = {"", "", ""};
        int i = 0;
        while (dcf.hasMoreTokens()) {
            tokens[i] = dcf.nextToken().trim();
            i++;
        }

        if (i != 0) {
            return tokens;
        } else {
            return getMDValueByLegacyField(field);
        }
    }

    @Override
    public void update(Context context, T dso) throws SQLException, AuthorizeException {
        if (dso.isMetadataModified()) {
            /*
            Update the order of the metadata values
             */
            // A map created to store the latest place for each metadata field
            Map<MetadataField, Integer> fieldToLastPlace = new HashMap<>();
            List<MetadataValue> metadataValues;
            if (dso.getType() == Constants.ITEM) {
                metadataValues = getMetadata(dso, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
            } else {
                metadataValues = dso.getMetadata();
            }
            //This inline sort function will sort the MetadataValues based on their place in ascending order
            //If two places are the same then the MetadataValue instance will be placed before the
            //RelationshipMetadataValue instance.
            //This is done to ensure that the order is correct.
            metadataValues.sort((o1, o2) -> {
                int compare = o1.getPlace() - o2.getPlace();
                if (compare == 0) {
                    if (o1 instanceof RelationshipMetadataValue && o2 instanceof RelationshipMetadataValue) {
                        return compare;
                    } else if (o1 instanceof RelationshipMetadataValue) {
                        return 1;
                    } else if (o2 instanceof RelationshipMetadataValue) {
                        return -1;
                    }
                }
                return compare;
            });
            for (MetadataValue metadataValue : metadataValues) {
                //Retrieve & store the place for each metadata value
                if (
                    // For virtual MDVs with useForPlace=true,
                    // update both the place of the metadatum and the place of the Relationship.
                    // E.g. for an Author relationship,
                    //   the place should be updated using the same principle as dc.contributor.author.
                    StringUtils.startsWith(metadataValue.getAuthority(), Constants.VIRTUAL_AUTHORITY_PREFIX)
                        && metadataValue instanceof RelationshipMetadataValue
                        && ((RelationshipMetadataValue) metadataValue).isUseForPlace()
                ) {
                    int mvPlace = getMetadataValuePlace(fieldToLastPlace, metadataValue);
                    metadataValue.setPlace(mvPlace);
                    String authority = metadataValue.getAuthority();
                    String relationshipId = StringUtils.split(authority, "::")[1];
                    Relationship relationship = relationshipService.find(context, Integer.parseInt(relationshipId));

                    if (relationship != null) {
                        if (relationship.getLeftItem() == (Item) dso) {
                            relationship.setLeftPlace(mvPlace);
                        } else {
                            relationship.setRightPlace(mvPlace);
                        }
                        relationshipService.update(context, relationship);
                    }

                } else if (
                    // Otherwise, just set the place of the metadatum
                    // ...unless the metadatum in question is a relation.* metadatum.
                    // This case is a leftover from when a Relationship is removed and copied to metadata.
                    // If we let its place change the order of any remaining Relationships will be affected.
                    // todo: this makes it so these leftover MDVs can't be reordered later on
                    !StringUtils.equals(
                        metadataValue.getMetadataField().getMetadataSchema().getName(), "relation"
                    )
                ) {
                    int mvPlace = getMetadataValuePlace(fieldToLastPlace, metadataValue);
                    metadataValue.setPlace(mvPlace);
                }
            }
        }
    }

    /**
     * Retrieve the place of the metadata value
     *
     * @param fieldToLastPlace the map containing the latest place of each metadata field
     * @param metadataValue    the metadata value that needs to get a place
     * @return The new place for the metadata value
     */
    protected int getMetadataValuePlace(Map<MetadataField, Integer> fieldToLastPlace, MetadataValue metadataValue) {
        MetadataField metadataField = metadataValue.getMetadataField();
        if (fieldToLastPlace.containsKey(metadataField)) {
            fieldToLastPlace.put(metadataField, fieldToLastPlace.get(metadataField) + 1);
        } else {
            // The metadata value place starts at 0
            fieldToLastPlace.put(metadataField, 0);
        }
        return fieldToLastPlace.get(metadataField);
    }

    protected String[] getMDValueByLegacyField(String field) {
        switch (field) {
            case "introductory_text":
                return new String[] {MetadataSchemaEnum.DC.getName(), "description", null};
            case "short_description":
                return new String[] {MetadataSchemaEnum.DC.getName(), "description", "abstract"};
            case "side_bar_text":
                return new String[] {MetadataSchemaEnum.DC.getName(), "description", "tableofcontents"};
            case "copyright_text":
                return new String[] {MetadataSchemaEnum.DC.getName(), "rights", null};
            case "name":
                return new String[] {MetadataSchemaEnum.DC.getName(), "title", null};
            case "provenance_description":
                return new String[] {MetadataSchemaEnum.DC.getName(), "provenance", null};
            case "license":
                return new String[] {MetadataSchemaEnum.DC.getName(), "rights", "license"};
            case "user_format_description":
                return new String[] {MetadataSchemaEnum.DC.getName(), "format", null};
            case "source":
                return new String[] {MetadataSchemaEnum.DC.getName(), "source", null};
            case "relationship":
                return new String[] {"dspace", "entity", "type"};
            case "firstname":
                return new String[] {"eperson", "firstname", null};
            case "lastname":
                return new String[] {"eperson", "lastname", null};
            case "phone":
                return new String[] {"eperson", "phone", null};
            case "language":
                return new String[] {"eperson", "language", null};
            case "entity-type":
                return new String[] { "dspace", "entity", "type" };
            case "submission-type":
                return new String[] { MetadataSchemaEnum.CRIS.getName(), "submission", "definition" };
            case "workflow-name":
                return new String[] { MetadataSchemaEnum.CRIS.getName(), "workflow", "name" };
            case "shared-workspace":
                return new String[] { MetadataSchemaEnum.CRIS.getName(), "workspace", "shared" };
            default:
                return new String[] {null, null, null};
        }
    }

    @Override
    public void addAndShiftRightMetadata(Context context, T dso, String schema, String element, String qualifier,
                                         String lang, String value, String authority, int confidence, int index)
            throws SQLException {

        List<MetadataValue> list = getMetadata(dso, schema, element, qualifier);

        int idx = 0;
        int place = 0;
        boolean last = true;
        for (MetadataValue rr : list) {
            if (idx == index) {
                MetadataValue newMetadata = addMetadata(context, dso, schema, element, qualifier,
                        lang, value, authority, confidence);

                moveSingleMetadataValue(context, dso, place, newMetadata);
                place++;
                last = false;
            }
            moveSingleMetadataValue(context, dso, place, rr);
            place++;
            idx++;
        }
        if (last) {
            addMetadata(context, dso, schema, element, qualifier,
                    lang, value, authority, confidence);
        }
    }

    @Override
    public void moveMetadata(Context context, T dso, String schema, String element, String qualifier, int from, int to)
            throws SQLException, IllegalArgumentException {
        if (from == to) {
            throw new IllegalArgumentException("The \"from\" location MUST be different from \"to\" location");
        }

        List<MetadataValue> list =
            getMetadata(dso, schema, element, qualifier).stream()
                                                        .sorted(Comparator.comparing(MetadataValue::getPlace))
                                                        .collect(Collectors.toList());


        if (from >= list.size() || to >= list.size() || to < 0 || from < 0) {
            throw new IllegalArgumentException(
                "The \"from\" and \"to\" locations MUST exist for the operation to be successful." +
                        "\n To and from indices must be between 0 and " + (list.size() - 1) +
                        "\n Idx from:" + from + " Idx to: " + to);
        }

        int idx = 0;
        MetadataValue moved = null;
        for (MetadataValue md : list) {
            if (idx == from) {
                moved = md;
                break;
            }
            idx++;
        }

        idx = 0;
        int place = 0;
        boolean last = true;
        for (MetadataValue rr : list) {
            if (idx == to && to < from) {
                moveSingleMetadataValue(context, dso, place, moved);
                place++;
                last = false;
            }
            if (idx != from) {
                moveSingleMetadataValue(context, dso, place, rr);
                place++;
            }
            if (idx == to && to > from) {
                moveSingleMetadataValue(context, dso, place, moved);
                place++;
                last = false;
            }
            idx++;
        }
        if (last) {
            moveSingleMetadataValue(context, dso, place, moved);
        }
    }

    /**
     * Supports moving metadata by updating the place of the metadata value.
     *
     * @param context current DSpace session.
     * @param dso     unused.
     * @param place   ordinal position of the value in the list of that field's values.
     * @param rr      the value to be placed.
     */
    protected void moveSingleMetadataValue(Context context, T dso, int place, MetadataValue rr) {
        //just move the metadata
        rr.setPlace(place);
    }

    @Override
    public void replaceMetadata(Context context, T dso, String schema, String element, String qualifier, String lang,
                                String value, String authority, int confidence, int index) throws SQLException {

        List<MetadataValue> list = getMetadata(dso, schema, element, qualifier);

        removeMetadataValues(context, dso, Arrays.asList(list.get(index)));
        addAndShiftRightMetadata(context, dso, schema, element, qualifier, lang, value, authority, confidence, index);
    }

    @Override
    public void replaceSecuredMetadata(Context context, T dso, String schema, String element, String qualifier,
        String lang, String value, String authority, int confidence, int index, Integer securityLevel)
        throws SQLException {
        List<MetadataValue> list = getMetadata(dso, schema, element, qualifier);
        removeMetadataValues(context, dso, Arrays.asList(list.get(index)));
        addAndShiftRightSecuredMetadata(context, dso, schema, element, qualifier, lang, value, authority, confidence,
            index, securityLevel);
    }

    @Override
    public void setMetadataModified(T dso) {
        dso.setMetadataModified();
    }

    @Override
    public MetadataValue addMetadata(Context context, T dso, String schema, String element, String qualifier,
            String lang, String value, String authority, int confidence, int place) throws SQLException {

        throw new NotImplementedException();

    }

    private boolean isNonValidAuthority(boolean authorityControlled, List<String> authorities) {
        return !authorityControlled && authorities != null && authorities.size() > 0 && authorities.get(0) != null;
    }

    /**
     * Returns all the metadata values filtered by the input language.
     *
     * @param metadataValues list of metadata values.
     * @param language the language that must metadataValues filtered by.
     *
     * @return the metadata values
     */
    protected List<MetadataValue> getFilteredMetadataValuesByLanguage(List<MetadataValue> metadataValues,
                                                                   String language) {
        List<MetadataValue> matchedValues = new ArrayList<>();

        if (language == null) {
            for (MetadataValue value : metadataValues) {
                if (value.getLanguage() == null) {
                    matchedValues.add(value);
                }
            }
        } else if (!language.equals(Item.ANY)) {
            Locale[] locales = I18nUtil.getSupportedLocales();
            Map<String, List<MetadataValue>> metadataMap =
                metadataValues.stream().collect(Collectors.groupingBy(v -> v.getMetadataField().toString()));

            for (Map.Entry<String, List<MetadataValue>> metadata : metadataMap.entrySet()) {
                matchedValues.addAll(filterMetadataValuesByLanguage(language, locales, metadata.getValue()));
            }
        } else if (language.equals(Item.ANY)) {
            matchedValues = metadataValues;
        }
        return matchedValues;
    }

    private List<MetadataValue> filterMetadataValuesByLanguage(String language, Locale [] locales,
                                                               List<MetadataValue> metadataValues) {

        List<MetadataValue> matchedValues;

//        search for input language in supported locales
//        then get metadata values contains language equal to input language
        matchedValues = filterByLanguageInSupportedLocales(metadataValues, locales, language);

        if (matchedValues.isEmpty()) {
//            here input language not found in supported locales.
//            so, check that input language contains '_' like 'en_US' , and search for first part 'en'
            matchedValues = filterByLanguageInSupportedLocales(metadataValues, locales, language.split("_")[0]);
        }

        if (matchedValues.isEmpty()) {
//            here input language not found in supported locales.
//            so, search for metadata value has language equal to any supported locales language
            for (Locale locale : locales) {
                matchedValues = filterByLanguage(metadataValues, locale.getLanguage());
                if (!matchedValues.isEmpty()) {
                    break;
                }
            }
        }

        if (matchedValues.isEmpty()) {
//            here input language not found in supported locales.
//            and no metadata value has language equal to any supported locales language was found
//            so, will search for metadata value with language equal to input language
            matchedValues = filterByLanguage(metadataValues, language);
        }

        if (matchedValues.isEmpty()) {
//            here no matching found so, will return all metadata values
            return metadataValues;
        }

        return matchedValues;
    }

    private List<MetadataValue> filterByLanguageInSupportedLocales(List<MetadataValue> metadataValues,
                                                                   Locale[] locales, String language) {

        List<MetadataValue> matchedValues = new ArrayList<>();

        for (Locale locale : locales) {
            if (locale.getLanguage().equals(language)) {
                matchedValues = filterByLanguage(metadataValues, language);
            }
        }
        return matchedValues;
    }

    private List<MetadataValue> filterByLanguage(List<MetadataValue> metadataValues, String language) {

        List<MetadataValue> matchedValues = new ArrayList<>();

        for (MetadataValue value : metadataValues) {
            if (value.getLanguage() != null && value.getLanguage().equals(language)) {
                matchedValues.add(value);
            }
        }
        return matchedValues;
    }

}
