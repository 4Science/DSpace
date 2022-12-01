/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.Iterator;
import java.util.Optional;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.integration.crosswalks.csl.CSLGeneratorFactory;
import org.dspace.content.integration.crosswalks.csl.DSpaceListItemDataProvider;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoveryConfigurationUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link VirtualField} that generates the citation for the
 * given item or, if a relation name is provided, for all the publications
 * related to the given item.
 * 
 * Example:
 * <ul>
 * <li><b>virtual.citations.apa.researchoutputs</b> generates the citations for
 * all the publications found with the researchoutputs relation in the apa
 * style</li>
 * <li><b>virtual.citations.chicago</b> generates the citation for the given
 * item in the chicago style</li>
 * </ul>
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class VirtualFieldCitations implements VirtualField {

    private final static Logger LOGGER = LoggerFactory.getLogger(VirtualFieldCitations.class);

    @Autowired
    private DiscoveryConfigurationUtilsService searchConfigurationUtilsService;

    @Autowired
    private ObjectFactory<DSpaceListItemDataProvider> dSpaceListItemDataProviderObjectFactory;

    @Autowired
    private CSLGeneratorFactory cslGeneratorFactory;

    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {

        String[] virtualFieldName = fieldName.split("\\.");
        if (virtualFieldName.length < 2 || virtualFieldName.length > 4) {
            LOGGER.warn("Invalid citations virtual field: " + fieldName);
            return new String[] {};
        }

        String style = getStyle(virtualFieldName);

        Iterator<Item> itemIterator = getRelationName(virtualFieldName)
            .map(relationName -> findRelatedItems(context, item, relationName))
            .orElseGet(() -> IteratorUtils.singletonIterator(item));

        return generateCitations(context, itemIterator, style);

    }

    private String[] generateCitations(Context context, Iterator<Item> itemIterator, String style) {

        DSpaceListItemDataProvider itemDataProvider = getDSpaceListItemDataProviderInstance();

        itemDataProvider.processItems(itemIterator);

        String citations = cslGeneratorFactory.getCSLGenerator().generate(itemDataProvider, style, "text");
        return citations != null ? citations.split("\n") : new String[] {};

    }

    private DSpaceListItemDataProvider getDSpaceListItemDataProviderInstance() {
        return dSpaceListItemDataProviderObjectFactory.getObject();
    }

    private String getStyle(String[] virtualFieldName) {
        String style = virtualFieldName[2];
        if (!StringUtils.endsWith(style, ".csl")) {
            style = style + ".csl";
        }
        return style;
    }

    private Optional<String> getRelationName(String[] virtualFieldName) {
        if (virtualFieldName.length == 3) {
            return Optional.empty();
        }
        return Optional.of(virtualFieldName[3]);
    }

    private Iterator<Item> findRelatedItems(Context context, Item item, String relationName) {
        return searchConfigurationUtilsService.findByRelation(context, item, relationName);
    }

}
