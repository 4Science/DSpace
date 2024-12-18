/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.generators;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.identifier.DOI;

/**
 * Provide custom doi generation based on certain criteria.
 *
 *
 * @author Stefano Maffei (steph-ieffam @ 4Science.com)
 */
public interface DoiGenerationStrategy {
    static final char SLASH = '/';

    public static final String MD_SCHEMA = "dc";
    public static final String DOI_ELEMENT = "identifier";
    public static final String DOI_QUALIFIER = "uri";

    public boolean isApplicable(Context context, DSpaceObject dso);

    public String generateDoi(Context context, DSpaceObject dso, DOI doi);

    public DoiGenType getGenerationType();

    public String getDOIResolverAndPrefix(Context context, DSpaceObject dso, String prefix);

    public DoiNamespaceGenerator getDoiNamespaceGenerator();

    public static DoiGenerationStrategy getApplicableStrategy(Context context,
        Collection<DoiGenerationStrategy> strategies, Item item) {
        Set<DoiGenerationStrategy> customStrategies = strategies.stream()
            .filter(strategy -> strategy.getGenerationType().equals(DoiGenType.CUSTOM))
            .collect(Collectors.toSet());
        for (DoiGenerationStrategy strategy : customStrategies) {
            if (strategy.isApplicable(context, item)) {
                return strategy;
            }
        }
        return strategies.stream()
            .filter(strategy -> strategy.getGenerationType().equals(DoiGenType.DEFAULT))
            .findFirst().orElse(null);
    }
}
