/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mapper.geomap.wkt;

import java.text.MessageFormat;
import java.util.regex.Matcher;

import org.dspace.app.mapper.geomap.AbstractGeoMapMapper;

/**
 * <pre>
 *     {@code
 *      POINT(-10 30)
 *      POLYGON((-10 30, -40 40, -10 -20, 40 20, 0 0, -10 30))
 *      MULTIPOINT((3.5 5.6),(4.8 10.5))
 *     }
 * </pre>
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class GeoWKTMapper extends AbstractGeoMapMapper {

    public static final String POINT_TYPE = "POINT";
    public static final String POLYGON_TYPE = "POLYGON";
    public static final String MULTIPOINT = "MULTIPOINT";
    private static final String WKT_TEMPLATE = "{0}({1})" ;
    private static final String POLYGON_COORDINATES = "({0})";
    private static final String MULTIPOINT_COORDINATES = "({0})";
    private static final String POINT_COORDINATES = "{0}";
    private static final String POINT_FORMAT = "{0} {1}";
    private static final String POINT_FORMAT_PATTERN = "(-?\\d+.\\d+) (-?\\d+.\\d+)";

    protected String extractPointFrom(Matcher matcher) {
        return MessageFormat.format(getPointFormat(), matcher.group(2), matcher.group(1));
    }

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
        return MULTIPOINT;
    }

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
        return WKT_TEMPLATE;
    }

}
