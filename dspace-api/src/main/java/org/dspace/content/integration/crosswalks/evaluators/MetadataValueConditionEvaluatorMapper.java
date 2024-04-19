/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.evaluators;

import java.util.Map;
import java.util.Set;

/**
 * A Mapper between instances of {@link MetadataValueConditionEvaluator} and their identifiers.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 *
 */
public class MetadataValueConditionEvaluatorMapper {

    private final Map<String, MetadataValueConditionEvaluator> conditionEvaluators;

    public MetadataValueConditionEvaluatorMapper(Map<String, MetadataValueConditionEvaluator> conditionEvaluators) {
        this.conditionEvaluators = conditionEvaluators;
    }

    public Set<String> getConditionEvaluatorNames() {
        return conditionEvaluators.keySet();
    }

    public MetadataValueConditionEvaluator getConditionEvaluator(String name) {
        return conditionEvaluators.get(name);
    }

    public boolean contains(String name) {
        return conditionEvaluators.containsKey(name);
    }

}
