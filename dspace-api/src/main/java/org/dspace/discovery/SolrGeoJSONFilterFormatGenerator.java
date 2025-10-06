/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.dspace.app.mapper.geomap.geojson.GeoJSONMapper;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class SolrGeoJSONFilterFormatGenerator<T extends GeoJSONMapper> extends SolrGeoFilterFormatGenerator<T>  {

    public SolrGeoJSONFilterFormatGenerator() {
        this((T) new GeoJSONMapper());
    }

    public SolrGeoJSONFilterFormatGenerator(T mapper) {
        super(mapper);
    }
}
