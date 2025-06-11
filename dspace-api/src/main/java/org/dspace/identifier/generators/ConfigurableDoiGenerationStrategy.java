/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.generators;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.logic.Filter;
import org.dspace.core.Context;
import org.dspace.identifier.DOI;
import org.dspace.identifier.service.DOIService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Provide custom doi generation based on certain criteria.
 *
 *
 * @author Stefano Maffei (steph-ieffam @ 4Science.com)
 */
public class ConfigurableDoiGenerationStrategy implements DoiGenerationStrategy {

    @Autowired
    protected DOIService doiService;

    protected final Filter filter;
    protected final DoiNamespaceGenerator doiNamespaceGenerator;
    protected final DoiGenType generationType;

    ConfigurableDoiGenerationStrategy(
        Filter filter,
        DoiNamespaceGenerator doiNamespaceGenerator,
        DoiGenType generationType
    ) {
        this.filter = filter;
        this.doiNamespaceGenerator = doiNamespaceGenerator;
        this.generationType = generationType;
    }

    @Override
    public boolean isApplicable(Context context, DSpaceObject dso) {
        return filter.getResult(context, (Item) dso);
    }

    @Override
    public String generateDoi(Context context, DSpaceObject dso, DOI doi) {
        return doiNamespaceGenerator.getNamespace(context, (Item) dso) +
            doi.getID();
    }

    @Override
    public DoiGenType getGenerationType() {
        return generationType;
    }

    @Override
    public String getDOIResolverAndPrefix(Context context, DSpaceObject dso, String prefix) {
        return doiService.getResolver() + SLASH + prefix + SLASH
            + doiNamespaceGenerator.getNamespace(context, (Item) dso);
    }

    public DoiNamespaceGenerator getDoiNamespaceGenerator() {
        return doiNamespaceGenerator;
    }

}
