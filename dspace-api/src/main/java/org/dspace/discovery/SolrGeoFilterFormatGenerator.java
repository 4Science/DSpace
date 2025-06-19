/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.mapper.geomap.AbstractGeoMapMapper;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class SolrGeoFilterFormatGenerator<T extends AbstractGeoMapMapper> {

    public static boolean isValidOperator(String uOperator) {
        return isPointOperator(uOperator) || isPolygonOperator(uOperator);
    }

    public static boolean isPointOperator(String uOperator) {
        return POINT.equals(uOperator);
    }

    public static boolean isPolygonOperator(String uOperator) {
        return POLYGON.equals(uOperator);
    }

    public static <A extends AbstractGeoMapMapper> SolrGeoFilterFormatGenerator<A> getInstance(Class<A> clazz) {
        try {
            return new SolrGeoFilterFormatGenerator<A>(clazz.getConstructor().newInstance());
        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static final String POINT = "POINT" ;
    public static final String POLYGON = "POLYGON" ;

    private final T mapper;

    public SolrGeoFilterFormatGenerator(T mapper) {
        this.mapper = mapper;
    }

    public String generate(String operator, String value) {
        String filterQueryValue = null;
        String uOperator;
        if (StringUtils.isNotBlank(operator) && isValidOperator(uOperator = operator.toUpperCase())) {
            if (isPolygonOperator(uOperator)) {
                filterQueryValue =
                    mapper.map(
                        Optional.ofNullable(mapper.getPolygonType()),
                        value,
                        SolrGeomapFilterService.SOLR_GEO_FILTER_PATTERN,
                        false
                    );
            } else {
                filterQueryValue =
                    mapper.map(
                        Optional.ofNullable(mapper.getPointType()),
                        value,
                        SolrGeomapFilterService.SOLR_GEO_FILTER_PATTERN,
                        false
                    );
            }
            filterQueryValue = formatIntersects(filterQueryValue);
        }
        return filterQueryValue;
    }

    protected String formatIntersects(String filter) {
        return MessageFormat.format(
            SolrGeomapFilterService.SOLR_INTERSECT_FILTER,
            filter
        );
    }
}
