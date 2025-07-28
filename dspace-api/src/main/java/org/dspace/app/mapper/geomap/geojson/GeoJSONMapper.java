/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mapper.geomap.geojson;

import org.dspace.app.mapper.geomap.AbstractGeoMapMapper;

/**
 * <pre>
 * {@code
 *   {
 *     "type":"Polygon",
 *     "coordinates":[[
 *       [16.743178409469415,40.85864359881754],
 *       [17.389286104550443,40.792034735376525],
 *       [16.949666471567436,40.558902981544385],
 *       [16.743178409469415,40.85864359881754]
 *     ]]
 *   }
 * }
 * </pre>
 * or
 * <pre>
 * {@code
 *    {
 *       "type": "MultiPoint",
 *       "coordinates": [
 *          [16.743178409469415,40.85864359881754],
 *          [17.389286104550443,40.792034735376525],
 *       ]
 *     }
 * }
 * </pre>
 * or
 * <pre>
 * {@code
 *   {
 *      "type":"Point",
 *      "coordinates":
 *         [16.743178409469415,40.85864359881754]
 *    }
 * }
 * </pre>
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class GeoJSONMapper extends AbstractGeoMapMapper {

    public static final String POLYGON_TYPE = "Polygon";
    public static final String POINT_TYPE = "Point";
    public static final String MULTIPOINT_TYPE = "MultiPoint";
    public static final String POINT_FORMAT = "[{0},{1}]";
    public static final String POINT_FORMAT_PATTERN = "\\[(-?\\d+.\\d+)\\,(-?\\d+.\\d+)\\]";

    protected static final String GEOMETRY_JSON_TEMPLATE = "'{'\"type\":\"{0}\",\"coordinates\":{1}'}'";
    protected static final String POLYGON_COORDINATES = "[[{0}]]";
    protected static final String POINT_COORDINATES = "{0}";
    protected static final String MULTIPOINT_COORDINATES = "[{0}]";


    @Override
    public String getPolygonType() {
        return POLYGON_TYPE;
    }

    @Override
    public String getPointType() {
        return POINT_TYPE;
    }

    @Override
    public String getMultiPointType() {
        return MULTIPOINT_TYPE;
    }

    @Override
    protected String getPointFormat() {
        return POINT_FORMAT;
    }

    @Override
    protected String getPointFormatPattern() {
        return POINT_FORMAT_PATTERN;
    }

    @Override
    protected String getPolygonCoordinatesFormat() {
        return POLYGON_COORDINATES;
    }

    @Override
    protected String getPointCoordinatesFormat() {
        return POINT_COORDINATES;
    }

    @Override
    protected String getMultipointCoordinatesFormat() {
        return MULTIPOINT_COORDINATES;
    }

    @Override
    protected String getTemplate() {
        return GEOMETRY_JSON_TEMPLATE;
    }

}