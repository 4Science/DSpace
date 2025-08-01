/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.FactoryConfigurationError;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Collection;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.core.Utils;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.submit.factory.SubmissionServiceFactory;
import org.dspace.submit.model.UploadConfiguration;
import org.dspace.submit.model.UploadConfigurationService;
import org.dspace.utils.DSpace;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Submission form generator for DSpace. Reads and parses the installation
 * form definitions file, submission-forms.xml, from the configuration directory.
 * A forms definition details the page and field layout of the metadata
 * collection pages used by the submission process. Each forms definition
 * starts with a unique name that gets associated with that form set.
 *
 * The file also specifies which collections use which form sets. At a
 * minimum, the definitions file must define a default mapping from the
 * placeholder collection #0 to the distinguished form 'default'. Any
 * collections that use a custom form set are listed paired with the name
 * of the form set they use.
 *
 * The definitions file also may contain sets of value pairs. Each value pair
 * will contain one string that the user reads, and a paired string that will
 * supply the value stored in the database if its sibling display value gets
 * selected from a choice list.
 *
 * @author Brian S. Hughes
 * @version $Revision$
 */

public class DCInputsReader {
    /**
     * The ID of the default collection. Will never be the ID of a named
     * collection
     */
    public static final String DEFAULT_COLLECTION = "default";

    /**
     * Name of the form definition XML file
     */
    static final String FORM_DEF_FILE = "submission-forms.xml";

    /**
     * Keyname for storing dropdown value-pair set name
     */
    static final String PAIR_TYPE_NAME = "value-pairs-name";

    /**
     * Reference to the forms definitions map, computed from the forms
     * definition file
     */
    private Map<String, List<List<Map<String, String>>>> formDefns = null;

    /**
     * Reference to the value-pairs map, computed from the forms definition file
     */
    private Map<String, List<String>> valuePairs = null;    // Holds display/storage pairs

    /**
     * Mini-cache of last DCInputSet requested. If submissions are not typically
     * form-interleaved, there will be a modest win.
     */
    private DCInputSet lastInputSet = null;

    /**
     * Parse an XML encoded submission forms template file, and create a hashmap
     * containing all the form information. This hashmap will contain three top
     * level structures: a map between collections and forms, the definition for
     * each page of each form, and lists of pairs of values that populate
     * selection boxes.
     *
     * @throws DCInputsReaderException if input reader error
     */

    public DCInputsReader()
        throws DCInputsReaderException {
        // Load from default file
        String defsFile = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir")
            + File.separator + "config" + File.separator + FORM_DEF_FILE;

        buildInputs(defsFile);
    }


    public DCInputsReader(String fileName)
        throws DCInputsReaderException {
        buildInputs(fileName);
    }


    private void buildInputs(String fileName)
        throws DCInputsReaderException {
        formDefns = new HashMap<String, List<List<Map<String, String>>>>();
        valuePairs = new HashMap<String, List<String>>();

        File inputFile = new File(fileName);
        String inputFileDir = inputFile.toPath().normalize().getParent().toString();

        String uri = "file:" + inputFile.getAbsolutePath();

        try {
            // This document builder will *not* disable external
            // entities as they can be useful in managing large forms, but
            // it will restrict them to be within the directory that the
            // current input form XML file exists (or a sub-directory)
            DocumentBuilder db = XMLUtils.getTrustedDocumentBuilder(inputFileDir);
            Document doc = db.parse(uri);
            doNodes(doc);
            checkValues();
        } catch (FactoryConfigurationError fe) {
            throw new DCInputsReaderException("Cannot create Submission form parser", fe);
        } catch (Exception e) {
            throw new DCInputsReaderException("Error creating submission forms: " + e);
        }
    }

    public Iterator<String> getPairsNameIterator() {
        return valuePairs.keySet().iterator();
    }

    public List<String> getPairs(String name) {
        return valuePairs.get(name);
    }

    /**
     * Returns the set of DC inputs used for a particular collection, or the default
     * set if no inputs defined for the collection
     *
     * @param collection collection for which search the set of DC inputs
     * @return DC input set
     * @throws DCInputsReaderException if no default set defined
     */
    public List<DCInputSet> getInputsByCollection(Collection collection)
        throws DCInputsReaderException {
        SubmissionConfig config;
        try {
            config = SubmissionServiceFactory.getInstance().getSubmissionConfigService()
                        .getSubmissionConfigByCollection(collection);
            String formName = config.getSubmissionName();
            if (formName == null) {
                throw new DCInputsReaderException("No form designated as default");
            }
            List<DCInputSet> results = new ArrayList<DCInputSet>();
            for (int idx = 0; idx < config.getNumberOfSteps(); idx++) {
                SubmissionStepConfig step = config.getStep(idx);
                if (SubmissionStepConfig.INPUT_FORM_STEP_NAME.equals(step.getType())) {
                    results.add(getInputsByFormName(step.getId()));
                }
            }
            return results;
        } catch (SubmissionConfigReaderException e) {
            throw new DCInputsReaderException("No form designated as default", e);
        }
    }

    public List<DCInputSet> getInputsUploadByCollection(Collection collection)
            throws DCInputsReaderException {
        SubmissionConfig config;
        try {
            config = new SubmissionConfigReader().getSubmissionConfigByCollection(collection);
            String formName = config.getSubmissionName();
            if (formName == null) {
                throw new DCInputsReaderException("No form designated as default");
            }
            List<DCInputSet> results = new ArrayList<DCInputSet>();
            for (int idx = 0; idx < config.getNumberOfSteps(); idx++) {
                SubmissionStepConfig step = config.getStep(idx);
                if (SubmissionStepConfig.UPLOAD_STEP_NAME.equals(step.getType())) {
                    UploadConfigurationService uploadConfigurationService = new DSpace().getServiceManager()
                            .getServiceByName("uploadConfigurationService", UploadConfigurationService.class);
                    UploadConfiguration uploadConfig = uploadConfigurationService.getMap().get(step.getId());
                    results.add(getInputsByFormName(uploadConfig.getMetadata()));
                }
            }
            return results;
        } catch (SubmissionConfigReaderException e) {
            throw new DCInputsReaderException("No form designated as default", e);
        }

    }

    public List<DCInputSet> getInputsGroupByCollection(Collection collection)
            throws DCInputsReaderException {
        SubmissionConfig config;
        try {
            config = new SubmissionConfigReader().getSubmissionConfigByCollection(collection);
            String formName = config.getSubmissionName();
            if (formName == null) {
                throw new DCInputsReaderException("No form designated as default");
            }
            List<DCInputSet> results = new ArrayList<DCInputSet>();
            for (int idx = 0; idx < config.getNumberOfSteps(); idx++) {
                SubmissionStepConfig step = config.getStep(idx);
                if (SubmissionStepConfig.INPUT_FORM_STEP_NAME.equals(step.getType())) {
                    results.addAll(getInputsByGroup(step.getId()));
                }
            }
            return results;
        } catch (SubmissionConfigReaderException e) {
            throw new DCInputsReaderException("No form designated as default", e);
        }

    }

    public List<DCInputSet> getInputsBySubmissionName(String name)
        throws DCInputsReaderException {
        SubmissionConfig config;
        try {
            config = SubmissionServiceFactory.getInstance().getSubmissionConfigService()
                        .getSubmissionConfigByName(name);
            String formName = config.getSubmissionName();
            if (formName == null) {
                throw new DCInputsReaderException("No form designated as default");
            }
            List<DCInputSet> results = new ArrayList<DCInputSet>();
            for (int idx = 0; idx < config.getNumberOfSteps(); idx++) {
                SubmissionStepConfig step = config.getStep(idx);
                if (SubmissionStepConfig.INPUT_FORM_STEP_NAME.equals(step.getType())) {
                    results.add(getInputsByFormName(step.getId()));
                }
            }
            return results;
        } catch (SubmissionConfigReaderException e) {
            throw new DCInputsReaderException("No form designated as default", e);
        }
    }

    /**
     * Returns the set of DC inputs used for a particular input form
     *
     * @param formName input form unique name
     * @return DC input set
     * @throws DCInputsReaderException if not found
     */
    public DCInputSet getInputsByFormName(String formName)
        throws DCInputsReaderException {
        // check mini-cache, and return if match
        if (lastInputSet != null && lastInputSet.getFormName().equals(formName)) {
            return lastInputSet;
        }
        // cache miss - construct new DCInputSet
        List<List<Map<String, String>>> pages = formDefns.get(formName);
        if (pages == null) {
            throw new DCInputsReaderException("Missing the " + formName + " form");
        }
        lastInputSet = new DCInputSet(this, formName, pages, valuePairs);
        return lastInputSet;
    }

    /**
     * Returns true if an input form with the given name exists.
     *
     * @param formName the form name to check
     * @return true if the input form exists, false otherwise
     */
    public boolean hasFormWithName(String formName) {
        return formDefns.get(formName) != null;
    }

    /**
     * Returns all the field names in the given form.
     *
     * @param formName the form name
     * @return the field names
     * @throws DCInputsReaderException if not found
     */
    public List<String> getAllFieldNamesByFormName(String formName) throws DCInputsReaderException {
        return Arrays.stream(getInputsByFormName(formName).getFields())
            .flatMap(dcInputs -> Arrays.stream(dcInputs))
            .map(dcInput -> dcInput.getFieldName())
            .collect(Collectors.toList());
    }

    /**
     * Returns a list of set of DC inputs belonging to group field used for a particular input form
     *
     * @param formName input form unique name
     * @return List of DC input set
     * @throws DCInputsReaderException if not found
     */
    public List<DCInputSet> getInputsByGroup(String formName)
            throws DCInputsReaderException {

        List<DCInputSet> results = new ArrayList<DCInputSet>();

        // cache miss - construct new DCInputSet
        List<List<Map<String, String>>> pages = formDefns.get(formName);
        if (pages == null) {
            return results;
        }

        Iterator<List<Map<String, String>>> iterator = pages.iterator();

        while (iterator.hasNext()) {
            List<Map<String, String>> input = iterator.next();

            for (Map<String, String> entry : input) {
                Set<Entry<String, String>> entrySet =
                        entry.entrySet();

                for (Entry<String, String> attr : entrySet) {
                    if (attr.getKey().equals("input-type") &&
                            (attr.getValue().equals("group") || attr.getValue().equals("inline-group"))) {
                        String schema = entry.get("dc-schema");
                        String element = entry.get("dc-element");
                        String qualifier = entry.get("dc-qualifier");
                        String subFormName = formName + "-" + Utils.standardize(schema, element, qualifier, "-");
                        results.add(getInputsByFormName(subFormName));
                    }
                }
            }

        }

        return results;
    }
    /**
     * @return the number of defined input forms
     */
    public int countInputs() {
        return formDefns.size();
    }


    /**
     * Returns all the Input forms with pagination
     *
     * @param limit  max number of Input Forms to return
     * @param offset number of Input form to skip in the return
     * @return the list of input forms
     * @throws DCInputsReaderException
     */
    public List<DCInputSet> getAllInputs(Integer limit, Integer offset) throws DCInputsReaderException {
        int idx = 0;
        int count = 0;
        List<DCInputSet> subConfigs = new LinkedList<DCInputSet>();
        for (String key : formDefns.keySet()) {
            if (offset == null || idx >= offset) {
                count++;
                subConfigs.add(getInputsByFormName(key));
            }
            idx++;
            if (count >= limit) {
                break;
            }
        }
        return subConfigs;
    }


    /**
     * Process the top level child nodes in the passed top-level node. These
     * should correspond to the collection-form maps, the form definitions, and
     * the display/storage word pairs.
     *
     * @param n top-level DOM node
     */
    private void doNodes(Node n)
        throws SAXException, DCInputsReaderException {
        if (n == null) {
            return;
        }
        Node e = getElement(n);
        NodeList nl = e.getChildNodes();
        int len = nl.getLength();
        boolean foundDefs = false;
        for (int i = 0; i < len; i++) {
            Node nd = nl.item(i);
            if ((nd == null) || isEmptyTextNode(nd)) {
                continue;
            }
            String tagName = nd.getNodeName();
            if (tagName.equals("form-definitions")) {
                processDefinition(nd);
                foundDefs = true;
            } else if (tagName.equals("form-value-pairs")) {
                processValuePairs(nd);
            }
            // Ignore unknown nodes
        }
        if (!foundDefs) {
            throw new DCInputsReaderException("No form definition found");
        }
    }

    /**
     * Process the form-definitions section of the XML file. Each element is
     * formed thusly: <form name="formname">...row...</form> Each rows
     * subsection is formed: <row> ...fields... </row> Each field
     * is formed from: dc-element, dc-qualifier, label, hint, input-type name,
     * required text, and repeatable flag.
     */
    private void processDefinition(Node e)
        throws SAXException, DCInputsReaderException {
        int numForms = 0;
        NodeList nl = e.getChildNodes();
        int len = nl.getLength();
        for (int i = 0; i < len; i++) {
            Node nd = nl.item(i);
            // process each form definition
            if (nd.getNodeName().equals("form")) {
                numForms++;
                String formName = getAttribute(nd, "name");
                if (formName == null) {
                    throw new SAXException("form element has no name attribute");
                }
                List<List<Map<String, String>>> rows = new ArrayList<List<Map<String, String>>>(); // the form
                // contains rows of fields
                formDefns.put(formName, rows);
                NodeList pl = nd.getChildNodes();
                int lenpg = pl.getLength();
                for (int j = 0; j < lenpg; j++) {
                    Node npg = pl.item(j);
                    if (npg.getNodeName().equals("row")) {
                        List<Map<String, String>> fields = new ArrayList<Map<String, String>>(); // the fields in the
                        // row
                        // process each row definition
                        processRow(formName, j, npg, fields);
                        rows.add(fields);
                    }
                }
                // sanity check number of fields
                if (rows.size() < 1) {
                    throw new DCInputsReaderException("Form " + formName + " has no rows");
                }
            }
        }
        if (numForms == 0) {
            throw new DCInputsReaderException("No form definition found");
        }
    }

    /**
     * Process parts of a row
     */
    private void processRow(String formName, int rowIdx, Node n, List<Map<String, String>> fields)
        throws SAXException, DCInputsReaderException {

        NodeList pl = n.getChildNodes();
        int lenpg = pl.getLength();
        for (int j = 0; j < lenpg; j++) {
            Node npg = pl.item(j);

            if (npg.getNodeName().equals("field")) {
                // process each field definition
                Map<String, String> field = new HashMap<String, String>();
                processField(formName, npg, field);
                fields.add(field);
                String key = field.get(PAIR_TYPE_NAME);
                if (StringUtils
                    .isNotBlank(key)) {
                    String schema = field.get("dc-schema");
                    String element = field.get("dc-element");
                    String qualifier = field
                        .get("dc-qualifier");
                    String metadataField = schema + "."
                        + element;
                    if (StringUtils.isNotBlank(qualifier)) {
                        metadataField += "." + qualifier;
                    }
                }
                // we omit the duplicate validation, allowing multiple
                // fields definition for
                // the same metadata and different visibility/type-bind
            } else if (StringUtils.equalsIgnoreCase(npg.getNodeName(), "relation-field")) {
                Map<String, String> relationField = new HashMap<>();
                processField(formName, npg, relationField);
                fields.add(relationField);
            }

        }
        // sanity check number of fields
        if (fields.size() < 1) {
            throw new DCInputsReaderException("Form " + formName + ", row " + rowIdx + " has no fields");
        }
    }


    /**
     * Process parts of a field
     * At the end, make sure that input-types 'qualdrop_value' and
     * 'twobox' are marked repeatable. Complain if dc-element, label,
     * or input-type are missing.
     */
    private void processField(String formName, Node n, Map<String, String> field)
        throws SAXException {
        NodeList nl = n.getChildNodes();
        int len = nl.getLength();
        for (int i = 0; i < len; i++) {
            Node nd = nl.item(i);
            if (!isEmptyTextNode(nd)) {
                String tagName = nd.getNodeName();
                String value = getValue(nd);
                field.put(tagName, value);
                if (tagName.equals("input-type")) {
                    handleInputTypeTagName(formName, field, nd, value);
                } else if (tagName.equals("vocabulary")) {
                    String closedVocabularyString = getAttribute(nd, "closed");
                    field.put("closedVocabulary", closedVocabularyString);
                } else if (tagName.equals("language")) {
                    if (Boolean.valueOf(value)) {
                        String pairTypeName = getAttribute(nd, PAIR_TYPE_NAME);
                        if (pairTypeName == null) {
                            throw new SAXException("Form " + formName + ", field " +
                                                       field.get("dc-element") +
                                                       "." + field.get("dc-qualifier") +
                                                       " has no language attribute");
                        } else {
                            field.put("language." + PAIR_TYPE_NAME, pairTypeName);
                        }
                    }
                } else if (StringUtils.equalsIgnoreCase(tagName, "linked-metadata-field")) {
                    for (int j = 0; j < nd.getChildNodes().getLength(); j ++) {
                        Node nestedNode = nd.getChildNodes().item(j);
                        String nestedTagName = nestedNode.getNodeName();
                        String nestedValue = getValue(nestedNode);
                        field.put(nestedTagName, nestedValue);
                        if (nestedTagName.equals("input-type")) {
                            handleInputTypeTagName(formName, field, nestedNode, nestedValue);
                        }
                    }
                }
            }
        }
        String missing = null;
        String nodeName = n.getNodeName();
        if (field.get("dc-element") == null &&
                (nodeName.equals("field") || field.containsKey("linked-metadata-field"))) {
            missing = "dc-element";
        }
        if (field.get("label") == null) {
            missing = "label";
        }
        if (field.get("input-type") == null &&
                (nodeName.equals("field") || field.containsKey("linked-metadata-field"))) {
            missing = "input-type";
        }
        if (missing != null) {
            String msg = "Required field " + missing + " missing on form " + formName;
            throw new SAXException(msg);
        }
        String type = field.get("input-type");
        if (StringUtils.isNotBlank(type) && (type.equals("twobox") || type.equals("qualdrop_value"))) {
            String rpt = field.get("repeatable");
            if ((rpt == null) ||
                ((!rpt.equalsIgnoreCase("yes")) &&
                    (!rpt.equalsIgnoreCase("true")))) {
                String msg = "The field \'" + field.get("label") + "\' must be repeatable";
                throw new SAXException(msg);
            }
        }
    }

    private void handleInputTypeTagName(String formName, Map<String, String> field, Node nd, String value)
        throws SAXException {
        if (value.equals("dropdown")
            || value.equals("qualdrop_value")
            || value.equals("list")) {
            String pairTypeName = getAttribute(nd, PAIR_TYPE_NAME);
            if (pairTypeName == null) {
                throw new SAXException("Form " + formName + ", field " +
                                           field.get("dc-element") +
                                           "." + field.get("dc-qualifier") +
                                           " has no name attribute");
            } else {
                field.put(value + "." + PAIR_TYPE_NAME, pairTypeName);
            }
        }
    }

    /**
     * Check that this is the only field with the name dc-element.dc-qualifier
     * If there is a duplicate, return an error message, else return null;
     */
    private String checkForDups(String formName, Map<String, String> field, List<List<Map<String, String>>> pages) {
        int matches = 0;
        String err = null;
        String schema = field.get("dc-schema");
        String elem = field.get("dc-element");
        String qual = field.get("dc-qualifier");
        if ((schema == null) || (schema.equals(""))) {
            schema = MetadataSchemaEnum.DC.getName();
        }
        String schemaTest;

        for (int i = 0; i < pages.size(); i++) {
            List<Map<String, String>> pg = pages.get(i);
            for (int j = 0; j < pg.size(); j++) {
                Map<String, String> fld = pg.get(j);
                if ((fld.get("dc-schema") == null) ||
                    ((fld.get("dc-schema")).equals(""))) {
                    schemaTest = MetadataSchemaEnum.DC.getName();
                } else {
                    schemaTest = fld.get("dc-schema");
                }

                // Are the schema and element the same? If so, check the qualifier
                if (((fld.get("dc-element")).equals(elem)) &&
                    (schemaTest.equals(schema))) {
                    String ql = fld.get("dc-qualifier");
                    if (qual != null) {
                        if ((ql != null) && ql.equals(qual)) {
                            matches++;
                        }
                    } else if (ql == null) {
                        matches++;
                    }
                }
            }
        }
        if (matches > 1) {
            err = "Duplicate field " + schema + "." + elem + "." + qual + " detected in form " + formName;
        }

        return err;
    }


    /**
     * Process the form-value-pairs section of the XML file.
     * Each element is formed thusly:
     * <value-pairs name="..." dc-term="...">
     * <pair>
     * <display>displayed name-</display>
     * <storage>stored name</storage>
     * </pair>
     * For each value-pairs element, create a new vector, and extract all
     * the pairs contained within it. Put the display and storage values,
     * respectively, in the next slots in the vector. Store the vector
     * in the passed in hashmap.
     */
    private void processValuePairs(Node e)
        throws SAXException {
        NodeList nl = e.getChildNodes();
        int len = nl.getLength();
        for (int i = 0; i < len; i++) {
            Node nd = nl.item(i);
            String tagName = nd.getNodeName();

            // process each value-pairs set
            if (tagName.equals("value-pairs")) {
                String pairsName = getAttribute(nd, PAIR_TYPE_NAME);
                String dcTerm = getAttribute(nd, "dc-term");
                if (pairsName == null) {
                    String errString =
                        "Missing name attribute for value-pairs for DC term " + dcTerm;
                    throw new SAXException(errString);
                }
                List<String> pairs = new ArrayList<String>();
                valuePairs.put(pairsName, pairs);
                NodeList cl = nd.getChildNodes();
                int lench = cl.getLength();
                for (int j = 0; j < lench; j++) {
                    Node nch = cl.item(j);
                    String display = null;
                    String storage = null;

                    if (nch.getNodeName().equals("pair")) {
                        NodeList pl = nch.getChildNodes();
                        int plen = pl.getLength();
                        for (int k = 0; k < plen; k++) {
                            Node vn = pl.item(k);
                            String vName = vn.getNodeName();
                            if (vName.equals("displayed-value")) {
                                display = getValue(vn);
                            } else if (vName.equals("stored-value")) {
                                storage = getValue(vn);
                                if (storage == null) {
                                    storage = "";
                                }
                            } // ignore any children that aren't 'display' or 'storage'
                        }
                        pairs.add(display);
                        pairs.add(storage);
                    } // ignore any children that aren't a 'pair'
                }
            } // ignore any children that aren't a 'value-pair'
        }
    }


    /**
     * Check that all referenced value-pairs are present
     * and field is consistent
     *
     * Throws DCInputsReaderException if detects a missing value-pair.
     */

    private void checkValues()
        throws DCInputsReaderException {
        // Step through every field of every page of every form
        Iterator<String> ki = formDefns.keySet().iterator();
        while (ki.hasNext()) {
            String idName = ki.next();
            List<List<Map<String, String>>> rows = formDefns.get(idName);
            for (int j = 0; j < rows.size(); j++) {
                List<Map<String, String>> fields = rows.get(j);
                for (int i = 0; i < fields.size(); i++) {
                    Map<String, String> fld = fields.get(i);
                    // verify reference in certain input types
                    String type = fld.get("input-type");
                    if (StringUtils.isNotBlank(type) && (type.equals("dropdown")
                        || type.equals("qualdrop_value")
                        || type.equals("list"))) {
                        String pairsName = fld.get(type + "." + PAIR_TYPE_NAME);
                        List<String> v = valuePairs.get(pairsName);
                        if (v == null) {
                            String errString = "Cannot find value pairs for " + pairsName;
                            throw new DCInputsReaderException(errString);
                        }
                    }

                    // we omit the "required" and "visibility" validation, provided this must be checked in the
                    // processing class
                    // only when it makes sense (if the field isn't visible means that it is not applicable,
                    // therefore it can't be required)
                }
            }
        }
    }

    private Node getElement(Node nd) {
        NodeList nl = nd.getChildNodes();
        int len = nl.getLength();
        for (int i = 0; i < len; i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                return n;
            }
        }
        return null;
    }

    private boolean isEmptyTextNode(Node nd) {
        boolean isEmpty = false;
        if (nd.getNodeType() == Node.TEXT_NODE) {
            String text = nd.getNodeValue().trim();
            if (text.length() == 0) {
                isEmpty = true;
            }
        }
        return isEmpty;
    }

    /**
     * Returns the value of the node's attribute named <name>
     */
    private String getAttribute(Node e, String name) {
        NamedNodeMap attrs = e.getAttributes();
        int len = attrs.getLength();
        if (len > 0) {
            int i;
            for (i = 0; i < len; i++) {
                Node attr = attrs.item(i);
                if (name.equals(attr.getNodeName())) {
                    return attr.getNodeValue().trim();
                }
            }
        }
        //no such attribute
        return null;
    }

    /**
     * Returns the value found in the Text node (if any) in the
     * node list that's passed in.
     */
    private String getValue(Node nd) {
        NodeList nl = nd.getChildNodes();
        int len = nl.getLength();
        for (int i = 0; i < len; i++) {
            Node n = nl.item(i);
            short type = n.getNodeType();
            if (type == Node.TEXT_NODE) {
                return n.getNodeValue().trim();
            }
        }
        // Didn't find a text node
        return null;
    }

    public String getInputFormNameByCollectionAndField(Collection collection, String field)
        throws DCInputsReaderException {
        ArrayList<List<DCInputSet>> arrayInputSets = new ArrayList<List<DCInputSet>>();
        arrayInputSets.add(getInputsGroupByCollection(collection));
        arrayInputSets.add(getInputsByCollection(collection));
        arrayInputSets.add(getInputsUploadByCollection(collection));

        for (List<DCInputSet> inputSets: arrayInputSets) {
            for (DCInputSet inputSet : inputSets) {
                String[] tokenized = Utils.tokenize(field);
                String schema = tokenized[0];
                String element = tokenized[1];
                String qualifier = tokenized[2];
                if (StringUtils.isBlank(qualifier)) {
                    qualifier = null;
                }
                String standardized = Utils.standardize(schema, element, qualifier, ".");

                if (inputSet.isFieldPresent(standardized)) {
                    return inputSet.getFormName();
                }
            }
        }
        throw new DCInputsReaderException("No field configuration found!");
    }

    /**
     * Returns all the metadata fields configured in the submission form of the
     * given collection that are not group inputs.
     *
     * @param  collection              the collection
     * @return                         the metadata fields
     * @throws DCInputsReaderException if an error occurs reading the form
     *                                 configuration
     */
    public List<String> getSubmissionFormMetadata(Collection collection) throws DCInputsReaderException {
        return getSubmissionFormMetadata(collection, false);
    }

    /**
     * Returns all the metadata fields configured in the submission form of the
     * given collection that are group inputs.
     *
     * @param  collection              the collection
     * @return                         the metadata fields
     * @throws DCInputsReaderException if an error occurs reading the form
     *                                 configuration
     */
    public List<String> getSubmissionFormMetadataGroups(Collection collection) throws DCInputsReaderException {
        return getSubmissionFormMetadata(collection, true);
    }

    private List<String> getSubmissionFormMetadata(Collection collection, boolean group)
        throws DCInputsReaderException {
        return getAllInputsByCollection(collection)
            .filter(dcInput -> group ? isGroupType(dcInput) : !isGroupType(dcInput))
            .flatMap(dcInput -> getMetadataFieldsFromDcInput(dcInput).stream())
            .collect(Collectors.toList());
    }

    public List<String> getLanguagesForMetadata(Collection collection, String metadata, boolean group)
        throws DCInputsReaderException {

        Optional<DCInput> dcInputMetadata = getAllInputsByCollection(collection)
            .filter(dcInput -> group ? isGroupType(dcInput) : !isGroupType(dcInput))
            .filter(dcInput -> {
                try {
                    return group
                        ? getAllNestedMetadataByGroupName(collection, dcInput.getFieldName()).contains(metadata)
                        : getMetadataFieldsFromDcInput(dcInput).contains(metadata);
                } catch (DCInputsReaderException e) {
                    throw new RuntimeException(e);
                }
            })
            .findFirst();
        if (dcInputMetadata.isPresent()) {
            dcInputMetadata.get().getAllLanguageValues();
        }
        return List.of();

    }

    public List<String> getAllNestedMetadataByGroupName(Collection collection, String groupName)
        throws DCInputsReaderException {

        DCInputSet groupInputSet = findGroupInputSetByMetadataGroupName(collection, groupName);

        return Arrays.stream(groupInputSet.getFields())
            .flatMap(dcInputs -> Arrays.stream(dcInputs))
            .flatMap(dcInput -> getMetadataFieldsFromDcInput(dcInput).stream())
            .collect(Collectors.toList());

    }

    public List<String> getSubmissionFormMetadata(SubmissionStepConfig submissionStepConfig)
        throws DCInputsReaderException {

        DCInputSet inputSet = getInputsByFormName(submissionStepConfig.getId());

        return Arrays.stream(inputSet.getFields())
            .flatMap(dcInputs -> Arrays.stream(dcInputs))
            .flatMap(dcInput -> getMetadataFieldsFromDcInput(dcInput).stream())
            .collect(Collectors.toList());

    }


    private DCInputSet findGroupInputSetByMetadataGroupName(Collection collection, String groupName)
        throws DCInputsReaderException {

        return getInputsByCollection(collection).stream()
            .filter(inputSet -> inputSet.isFieldPresent(groupName))
            .findFirst()
            .map(inputSet -> getInputByMetadataGroupName(inputSet, groupName))
            .orElseThrow(() -> new DCInputsReaderException("No nested metadata group found by name: " + groupName));

    }

    private DCInputSet getInputByMetadataGroupName(DCInputSet dcInputSet, String groupName) {
        try {
            return getInputsByFormName(dcInputSet.getFormName() + "-" + groupName.replaceAll("\\.", "-"));
        } catch (DCInputsReaderException ex) {
            throw new RuntimeException("An error occurs searching a DCInputSet by metadata group name", ex);
        }
    }

    private Stream<DCInput> getAllInputsByCollection(Collection collection) throws DCInputsReaderException {
        return getInputsByCollection(collection).stream()
            .flatMap(dcInputSet -> Arrays.stream(dcInputSet.getFields()))
            .flatMap(dcInputs -> Arrays.stream(dcInputs));
    }

    private List<String> getMetadataFieldsFromDcInput(DCInput dcInput) {
        if (!"qualdrop_value".equals(dcInput.getInputType())) {
            return List.of(dcInput.getFieldName());
        }

        return dcInput.getAllStoredValues().stream()
            .map(value -> StringUtils.isNotBlank(value) ? dcInput.getFieldName() + '.' + value : dcInput.getFieldName())
            .collect(Collectors.toList());
    }

    private boolean isGroupType(DCInput dcInput) {
        return "group".equals(dcInput.getInputType()) || "inline-group".equals(dcInput.getInputType());
    }

    public List<String> getUploadMetadataFieldsFromCollection(Collection collection) throws DCInputsReaderException {
        return getInputsUploadByCollection(collection)
            .stream()
            .flatMap(dcInputSet -> Arrays.stream(dcInputSet.getFields()))
            .flatMap(dcInputs -> Arrays.stream(dcInputs))
            .flatMap(dcInput -> getMetadataFieldsFromDcInput(dcInput).stream())
            .collect(Collectors.toList());
    }

}
