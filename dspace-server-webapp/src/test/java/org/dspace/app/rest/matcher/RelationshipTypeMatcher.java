/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.Optional;

import org.dspace.content.EntityType;
import org.dspace.content.RelationshipType;
import org.hamcrest.Matcher;

public class RelationshipTypeMatcher {

    private RelationshipTypeMatcher() {}

    public static Matcher<? super Object> matchRelationshipTypeEntry(RelationshipType relationshipType) {
        return matchRelationshipTypeExplicitEntityTypes(relationshipType, relationshipType.getLeftType(),
                                                        relationshipType.getRightType());
    }

    private static Matcher<? super Object> matchRelationshipTypeExplicitEntityTypes(RelationshipType relationshipType,
                                                                                    EntityType leftType,
                                                                                    EntityType rightType) {
        return matchRelationshipTypeExplicitEntityTypeValues(relationshipType,
                                                             Optional.ofNullable(leftType).map(EntityType::getID)
                                                                     .orElse(null),
                                                             Optional.ofNullable(leftType).map(EntityType::getLabel)
                                                                     .orElse(null),
                                                             Optional.ofNullable(rightType).map(EntityType::getID)
                                                                     .orElse(null),
                                                             Optional.ofNullable(rightType).map(EntityType::getLabel)
                                                                     .orElse(null));
    }

    private static Matcher<? super Object> matchRelationshipTypeExplicitEntityTypeValues(
        RelationshipType relationshipType, Integer leftEntityTypeId,
        String leftEntityTypeLabel, Integer rightEntityTypeId,
        String rightEntityTypeLabel) {

        return matchExplicitRelationshipTypeValuesAndExplicitEntityTypeValues(relationshipType.getID(),
                                                                              relationshipType.getLeftwardType(),
                                                                              relationshipType.getRightwardType(),
                                                                              relationshipType.getLeftMinCardinality(),
                                                                              relationshipType.getLeftMaxCardinality(),
                                                                              relationshipType.getRightMinCardinality(),
                                                                              relationshipType.getRightMaxCardinality(),
                                                                              leftEntityTypeId, leftEntityTypeLabel,
                                                                              rightEntityTypeId, rightEntityTypeLabel,
                                                                              relationshipType.isCopyToLeft(),
                                                                              relationshipType.isCopyToRight());
    }

    private static Matcher<? super Object> matchExplicitRelationshipTypeValuesAndExplicitEntityType(int id,
        String leftwardType, String rightwardType, Integer leftMinCardinality, Integer leftMaxCardinality,
        Integer rightMinCardinality, Integer rightMaxCardinality,
        EntityType leftEntityType, EntityType rightEntityType, boolean copyToLeft, boolean copyToRight) {
        return matchExplicitRelationshipTypeValuesAndExplicitEntityTypeValues(id, leftwardType, rightwardType,
                                                                              leftMinCardinality, leftMaxCardinality,
                                                                              rightMinCardinality,
                                                                              rightMaxCardinality,
                                                                              leftEntityType.getID(),
                                                                              leftEntityType.getLabel(),
                                                                              rightEntityType.getID(),
                                                                              rightEntityType.getLabel(),
                                                                              copyToLeft, copyToRight);
    }

    private static Matcher<? super Object> matchExplicitRelationshipTypeValuesAndExplicitEntityTypeValues(int id,
        String leftwardType, String rightwardType, Integer leftMinCardinality, Integer leftMaxCardinality,
        Integer rightMinCardinality, Integer rightMaxCardinality, Integer leftEntityTypeId, String leftEntityTypeLabel,
        Integer rightEntityTypeId, String rightEntityTypeLabel, boolean copyToLeft, boolean copyToRight) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            hasJsonPath("$.leftwardType", is(leftwardType)),
            hasJsonPath("$.rightwardType", is(rightwardType)),
            hasJsonPath("$.copyToLeft", is(copyToLeft)),
            hasJsonPath("$.copyToRight", is(copyToRight)),
            hasJsonPath("$.leftMinCardinality", is(leftMinCardinality)),
            hasJsonPath("$.leftMaxCardinality", is(leftMaxCardinality)),
            hasJsonPath("$.rightMinCardinality", is(rightMinCardinality)),
            hasJsonPath("$.rightMaxCardinality", is(rightMaxCardinality)),
            hasJsonPath("$.type", is("relationshiptype")),
            hasJsonPath("$.uniqueType", is("core.relationshiptype")),
            hasJsonPath("$._links.self.href", containsString("/api/core/relationshiptypes/" + id)),
            hasJsonPath("$._links.leftType.href", containsString("/api/core/entitytypes/" + leftEntityTypeId)),
            hasJsonPath("$._links.rightType.href", containsString("/api/core/entitytypes/" + rightEntityTypeId))
        );
    }

    public static Matcher<? super Object> matchExplicitRestrictedRelationshipTypeValues(
                                              String leftwardType, String rightwardType) {
        return allOf(
                     hasJsonPath("$.leftwardType", is(leftwardType)),
                     hasJsonPath("$.rightwardType", is(rightwardType))
                     );
    }

}
