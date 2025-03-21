/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.hamcrest.Matcher;

/**
 * Helper class to simplify testing of the submission form configuration
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class SubmissionFormFieldMatcher {

    private SubmissionFormFieldMatcher() {
    }

    public static Matcher<? super Object> matchFormFieldDefinition(String type, String label, String mandatoryMessage,
        boolean repeatable, String hints, String metadata) {
        return matchFormFieldDefinition(type, label, null, mandatoryMessage, repeatable, hints, null, metadata,
            null);
    }

    public static Matcher<? super Object> matchFormFieldDefinition(String type, String label, String mandatoryMessage,
                                                                   boolean repeatable, String hints, String style,
                                                                   String metadata) {
        return matchFormFieldDefinition(type, label, null, mandatoryMessage, repeatable, hints, style, metadata);
    }

    public static Matcher<? super Object> matchFormFieldDefinition(String type, String label, String mandatoryMessage,
                                                                   boolean repeatable, String hints, String style,
                                                                   String metadata, String controlledVocabulary) {
        return matchFormFieldDefinition(type, label, null, mandatoryMessage, repeatable, hints, style, metadata,
            controlledVocabulary);
    }

    /**
     * Shortcut for the
     * {@link SubmissionFormFieldMatcher#matchFormFieldDefinition(String, String, String, String, boolean, String, String, String, String)}
     * with a null style and vocabulary name
     *
     * @param type
     *            the expected input type
     * @param label
     *            the expected label
     * @param typeBind
     *            the expected type-bind field(s)
     * @param mandatoryMessage
     *            the expected mandatoryMessage, can be null. If not empty the fiedl is expected to be flagged as
     *            mandatory
     * @param repeatable
     *            the expected repeatable flag
     * @param hints
     *            the expected hints message
     * @param metadata
     *            the expected metadata
     * @return a Matcher for all the condition above
     */
    public static Matcher<? super Object> matchFormFieldDefinition(String type, String label, String typeBind,
                                                                   String mandatoryMessage, boolean repeatable,
                                                                   String hints, String metadata) {
        return matchFormFieldDefinition(type, label, typeBind, mandatoryMessage, repeatable, hints, null, metadata);
    }

    /**
     * Shortcut for the
     * {@link SubmissionFormFieldMatcher#matchFormFieldDefinition(String, String, String, String, boolean, String, String, String, String)}
     * with a null controlled vocabulary
     *
     * @param type
     *            the expected input type
     * @param label
     *            the expected label
     * @param typeBind
     *            the expected type-bind field(s)
     * @param mandatoryMessage
     *            the expected mandatoryMessage, can be null. If not empty the field is expected to be flagged as
     *            mandatory
     * @param repeatable
     *            the expected repeatable flag
     * @param hints
     *            the expected hints message
     * @param style
     *            the expected style for the field, can be null. If null the corresponding json path is expected to be
     *            missing
     * @param metadata
     *            the expected metadata
     * @return a Matcher for all the condition above
     */
    public static Matcher<? super Object> matchFormFieldDefinition(String type, String label, String typeBind,
            String mandatoryMessage, boolean repeatable, String hints, String style, String metadata) {
        return matchFormFieldDefinition(type, label, typeBind, mandatoryMessage, repeatable, hints, style, metadata,
                null);
    }

    /**
     * Check the json representation of a submission form
     *
     * @param type
     *            the expected input type
     * @param label
     *            the expected label
     * @param typeBind
     *            the expected type-bind field(s)
     * @param mandatoryMessage
     *            the expected mandatoryMessage, can be null. If not empty the field is expected to be flagged as
     *            mandatory
     * @param repeatable
     *            the expected repeatable flag
     * @param hints
     *            the expected hints message
     * @param style
     *            the expected style for the field, can be null. If null the corresponding json path is expected to be
     *            missing
     * @param metadata
     *            the expected metadata
     * @param controlledVocabulary
     *            the expected controlled vocabulary, can be null. If null the corresponding json path is expected to be
     *            missing
     * @return a Matcher for all the condition above
     */
    public static Matcher<? super Object> matchFormFieldDefinition(String type, String label, String typeBind,
                                                                   String mandatoryMessage, boolean repeatable,
                                                                   String hints, String style, String metadata,
                                                                   String controlledVocabulary) {
        return allOf(
            // check each field definition
            hasJsonPath("$.input.type", is(type)),
            hasJsonPath("$.label", containsString(label)),
            typeBind != null ? hasJsonPath("$.typeBind", contains(typeBind)) : hasNoJsonPath("$.typeBind[0]"),
            hasJsonPath("$.selectableMetadata[0].metadata", is(metadata)),
            controlledVocabulary != null ? hasJsonPath("$.selectableMetadata[0].controlledVocabulary",
                    is(controlledVocabulary)) : hasNoJsonPath("$.selectableMetadata[0].controlledVocabulary"),
            mandatoryMessage != null ? hasJsonPath("$.mandatoryMessage", containsString(mandatoryMessage)) :
                hasNoJsonPath("$.mandatoryMessage"),
            hasJsonPath("$.mandatory", is(mandatoryMessage != null)),
            hasJsonPath("$.repeatable", is(repeatable)),
            style != null ? hasJsonPath("$.style", is(style)) :
                hasNoJsonPath("$.style"),
            hasJsonPath("$.hints", containsString(hints))
        );
    }

    /**
     * Creates a matcher to verify the properties of a selectableMetadata entry.
     *
     * @param metadata the metadata field to match (e.g., "dc.identifier.doi")
     * @param label the label associated with the metadata field (e.g., "DOI")
     * @param closed whether the metadata field is closed (true or false)
     * @return a Matcher that verifies the "metadata", "label", and "closed" properties of a selectableMetadata object
     */
    public static Matcher<Object> matchSelectableMetadata(String metadata, String label, boolean closed) {
        return allOf(
            hasJsonPath("$.metadata", is(metadata)),
            hasJsonPath("$.label", is(label)),
            hasJsonPath("$.closed", is(closed))
        );
    }

    /**
     * Creates a matcher to validate an object representing a language code in a JSON response.
     *
     * @param display The expected value of the "display" field, representing the name of the language.
     * @param code    The expected value of the "code" field, representing the language code.
     * @return A {@link Matcher} that validates the "display" and "code" fields of the JSON object.
     */
    public static Matcher<Object> matchLanguageCode(String display, String code) {
        return allOf(
            hasJsonPath("$.display", is(display)),
            hasJsonPath("$.code", is(code))
        );
    }


    /**
     * Check the json representation of an open relationship field.
     * This is a combination of an entity relationship lookup and a plain text metadata entry field
     *
     * @param type
     *            the expected input type
     * @param label
     *            the expected label
     * @param mandatoryMessage
     *            the expected mandatoryMessage, can be null. If not empty the field is expected to be flagged as
     *            mandatory
     * @param repeatable
     *            the expected repeatable flag
     * @param hints
     *            the expected hints message
     * @param metadata
     *            the expected metadata
     * @param relationshipType
     *            the type of relationship
     * @param filter
     *            the optional filter to be used for the lookup
     * @param searchConfiguration
     *            the searchConfiguration to be used for the lookup
     * @param nameVariants
     *            the optional name variants allowed flag
     * @return a Matcher for all the condition above
     */
    public static Matcher<? super Object> matchFormOpenRelationshipFieldDefinition(String type, String label,
                                                                                   String mandatoryMessage,
                                                                                   boolean repeatable,
                                                                                   String hints,
                                                                                   String metadata,
                                                                                   String relationshipType,
                                                                                   String filter,
                                                                                   String searchConfiguration,
                                                                                   boolean nameVariants) {
        return allOf(
            hasJsonPath("$.selectableRelationship.relationshipType", is(relationshipType)),
            hasJsonPath("$.selectableRelationship.filter", is(filter)),
            hasJsonPath("$.selectableRelationship.searchConfiguration", is(searchConfiguration)),
            hasJsonPath("$.selectableRelationship.nameVariants", is(String.valueOf(nameVariants))),
            matchFormFieldDefinition(type, label, null, mandatoryMessage, repeatable, hints, metadata));
    }

    /**
     * Check the json representation of a closed relationship field.
     * This is an entity relationship lookup without a plain text metadata entry field
     *
     * @param label
     *            the expected label
     * @param mandatoryMessage
     *            the expected mandatoryMessage, can be null. If not empty the field is expected to be flagged as
     *            mandatory
     * @param repeatable
     *            the expected repeatable flag
     * @param hints
     *            the expected hints message
     * @param relationshipType
     *            the type of relationship
     * @param filter
     *            the optional filter to be used for the lookup
     * @param searchConfiguration
     *            the searchConfiguration to be used for the lookup
     * @param nameVariants
     *            the optional name variants allowed flag
     * @return a Matcher for all the condition above
     */
    public static Matcher<? super Object> matchFormClosedRelationshipFieldDefinition(String label,
                                                                                     String mandatoryMessage,
                                                                                     boolean repeatable,
                                                                                     String hints,
                                                                                     String relationshipType,
                                                                                     String filter,
                                                                                     String searchConfiguration,
                                                                                     boolean nameVariants) {
        return allOf(
            hasJsonPath("$.selectableRelationship.relationshipType", is(relationshipType)),
            hasJsonPath("$.selectableRelationship.filter", is(filter)),
            hasJsonPath("$.selectableRelationship.searchConfiguration", is(searchConfiguration)),
            hasJsonPath("$.selectableRelationship.nameVariants", is(String.valueOf(nameVariants))),
            hasJsonPath("$.label", is(label)),
            mandatoryMessage != null ? hasJsonPath("$.mandatoryMessage", containsString(mandatoryMessage)) :
                hasNoJsonPath("$.mandatoryMessage"),
            hasJsonPath("$.mandatory", is(mandatoryMessage != null)),
            hasJsonPath("$.repeatable", is(repeatable)),
            hasJsonPath("$.hints", containsString(hints)),
            hasNoJsonPath("$.input.type"),
            hasNoJsonPath("$.selectableMetadata"));
    }

    /**
     * Check the json representation of a submission form to verify that it has the
     * expected label and the expected visibilities.
     *
     * @param  label      the label to check
     * @param  visibility the visibilities to check
     * @return            a Matcher for all the condition above
     */
    public static Matcher<? super Object> matchFormWithVisibility(String label, Map<String, String> visibility) {
        return allOf(
            hasJsonPath("$.label", containsString(label)),
            hasJsonPath("$.visibility", is(visibility)));
    }

    /**
     * Check the json representation of a submission form to verify that it has the
     * expected label and it has not the visibility attribute.
     *
     * @param  label the label to check
     * @return       a Matcher for all the condition above
     */
    public static Matcher<? super Object> matchFormWithoutVisibility(String label) {
        return allOf(
            hasJsonPath("$.label", containsString(label)),
            hasNoJsonPath("$.visibility"));
    }
}
