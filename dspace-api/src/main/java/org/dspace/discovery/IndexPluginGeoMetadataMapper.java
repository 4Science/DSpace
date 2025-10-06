/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;


import static org.dspace.app.mapper.geomap.geojson.GeoJSONMapper.POINT_FORMAT;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.dto.MetadataValueDTO;

/**
 * A mapper class that converts latitude and longitude metadata values of a DSpaceObject
 * into a list of formatted geographic coordinates.
 * This class uses two IndexPluginMetadataMapper instances to extract latitude and longitude values, respectively,
 * and formats them according to a specified pattern.
 *
 * @param <T> the type of DSpaceObject being mapped
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class IndexPluginGeoMetadataMapper<T extends DSpaceObject> implements IndexPluginMapper<T, List<String>> {

    private static final Logger log = LogManager.getLogger(IndexPluginGeoMetadataMapper.class);

    protected IndexPluginMetadataMapper<T> latitudeMapper;
    protected IndexPluginMetadataMapper<T> longitudeMapper;
    protected String longLatPattern = POINT_FORMAT;

    @Override
    public List<String> map(T t) {
        List<MetadataValueDTO> latitude = latitudeMapper.map(t);
        List<MetadataValueDTO> longitude = longitudeMapper.map(t);
        if (latitude == null || longitude == null) {
            return null;
        }
        if (latitude.isEmpty() || longitude.isEmpty()) {
            return null;
        }

        List<String> latitudeValues =
            latitude.stream()
                    .map(MetadataValueDTO::getValue)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toList());

        List<String> longitudeValues =
            longitude.stream()
                     .map(MetadataValueDTO::getValue)
                     .filter(StringUtils::isNotEmpty)
                     .collect(Collectors.toList());

        if (latitudeValues.size() != longitudeValues.size()) {
            log.error(
                "Latitude and Longitude must have the same number of values," +
                    "found - Latitude: {}, Longitude: {}, for the dspaceobject: {}",
                latitudeValues.size(),
                longitudeValues.size(),
                t.getID()
            );
            return null;
        }

        return IntStream
            .range(0, longitudeValues.size())
            .mapToObj(i ->
                          MessageFormat.format(
                              longLatPattern, longitudeValues.get(i), latitudeValues.get(i)
                          )
            )
            .collect(Collectors.toList());
    }

    public IndexPluginMetadataMapper<T> getLatitudeMapper() {
        return latitudeMapper;
    }

    public void setLatitudeMapper(IndexPluginMetadataMapper<T> latitudeMapper) {
        this.latitudeMapper = latitudeMapper;
    }

    public IndexPluginMetadataMapper<T> getLongitudeMapper() {
        return longitudeMapper;
    }

    public void setLongitudeMapper(IndexPluginMetadataMapper<T> longitudeMapper) {
        this.longitudeMapper = longitudeMapper;
    }

    public String getLongLatPattern() {
        return longLatPattern;
    }

    public void setLongLatPattern(String longLatPattern) {
        this.longLatPattern = longLatPattern;
    }
}
