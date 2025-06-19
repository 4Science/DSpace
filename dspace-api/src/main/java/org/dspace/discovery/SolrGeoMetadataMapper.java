/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.List;
import java.util.Optional;

import org.dspace.content.DSpaceObject;


/**
 * A mapper class that handles the mapping of DSpace objects to a list of strings
 * for Solr indexing, specifically for geographic metadata. It utilizes two types
 * of mappers: default mappers and relation mappers. If the default mappers do not
 * provide a result, the relation mappers are used as a fallback.
 *
 * @param <T> the type of DSpaceObject being mapped
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class SolrGeoMetadataMapper<T extends DSpaceObject>
    implements IndexPluginMapper<T, List<String>> {

    IndexPluginMapper<T, List<String>> defaultMappers;
    IndexPluginMapper<T, List<String>> relationMappers;

    public List<String> map(T item) {
        return Optional.ofNullable(defaultMappers.map(item))
                       .orElseGet(() -> relationMappers.map(item));
    }

    public IndexPluginMapper<T, List<String>> getDefaultMappers() {
        return defaultMappers;
    }

    public void setDefaultMappers(IndexPluginMapper<T, List<String>> defaultMappers) {
        this.defaultMappers = defaultMappers;
    }

    public IndexPluginMapper<T, List<String>> getRelationMappers() {
        return relationMappers;
    }

    public void setRelationMappers(IndexPluginMapper<T, List<String>> relationMappers) {
        this.relationMappers = relationMappers;
    }
}
