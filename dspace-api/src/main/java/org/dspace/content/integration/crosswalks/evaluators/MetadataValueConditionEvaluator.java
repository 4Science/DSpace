/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.evaluators;

import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Context;

/**
 * Class that can be extended to evaluate a given condition on some metadatavalue.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 *
 */
public abstract class MetadataValueConditionEvaluator {

    /**
     * Test the given condition on the given metadtavalue.
     * The condition should have the format [not].evaluatorIdentifier.value,
     * where:
     * <ul>
     * <li> not can be used to negate the result of the condition
     * <li> evaluatorIdentifier is the unique identifier of the evaluator
     * <li> value can be any string useful to the evaluator
     * </ul>
     *
     * @param context the DSpace Context
     * @param value the item to evaluate
     * @param condition the condition to evaluate
     * @return the evaluation result
     */
    public final boolean test(Context context, String value, String condition) {
        if (StringUtils.isBlank(condition)) {
            return false;
        }

        if (condition.startsWith("not.")) {
            return !doTest(context, value, condition.substring("not.".length()));
        }

        return doTest(context, value, condition);

    }

    /**
     * Test the given condition on the given item.
     * The condition should have the format evaluatorIdentifier.value, where:
     * <ul>
     * <li> evaluatorIdentifier is the unique identifier of the evaluator
     * <li> value can be any string useful to the evaluator
     * </ul>
     *
     * @param context the DSpace Context
     * @param value the metadatavalue to evaluate
     * @param condition the condition to evaluate
     * @return the evaluation result
     */
    protected abstract boolean doTest(Context context, String value, String condition);

}
