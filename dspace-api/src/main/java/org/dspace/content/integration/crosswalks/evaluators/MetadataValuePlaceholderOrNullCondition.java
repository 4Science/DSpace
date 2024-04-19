/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.evaluators;

import org.dspace.core.Context;
import org.dspace.core.CrisConstants;

/**
 * Implementation of {@link MetadataValueConditionEvaluator} to evaluate
 * if the given Metadata Value is null or some Placeholder Value
 *
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 *
 */
public class MetadataValuePlaceholderOrNullCondition extends MetadataValueConditionEvaluator {

    @Override
    protected boolean doTest(Context context, String value, String condition) {
        return value == null || value.contentEquals(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE);
    }
}
