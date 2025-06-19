/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class SolrGeomapFilterService {

    // each point is issued as this [ 16.826866113744146, 41.134281200631847 ]
    // multiple points are listed as a matrix
    // [[642957.5168454978,4546150.636464767],[642957.5168454978,4561411.808859862]]
    public static final Pattern SOLR_GEO_FILTER_PATTERN =
        Pattern.compile("(?:\\[\\s*)(\\d+.\\d+)(?:\\s*,\\s*)(\\d+.\\d+)(?:\\s*\\])");
    public static final String SOLR_INTERSECT_FILTER = "\"Intersects({0})\"";

    protected static boolean isValidOperator(String uOperator) {
        return SolrGeoFilterFormatGenerator.isValidOperator(uOperator);
    }

    public enum GeoFilterFormat {
        WKT("WKT", new SolrWKTFilterFormatGenerator()),
        GEOJSON("GeoJSON", new SolrGeoJSONFilterFormatGenerator());

        private final String format;
        private final SolrGeoFilterFormatGenerator generator;

        private GeoFilterFormat(String format, SolrGeoFilterFormatGenerator generator) {
            this.format = format;
            this.generator = generator;
        }

        public String getFormat() {
            return format;
        }

        public SolrGeoFilterFormatGenerator getGenerator() {
            return generator;
        }

        public static GeoFilterFormat fromFormat(String format) {
            return Stream.of(GeoFilterFormat.values())
                         .filter(geoFilter -> format.equals(geoFilter.getFormat()))
                         .findFirst()
                         .orElse(null);
        }
    }

    public final String DEFAULT_FORMAT = "WKT";
    public final String SOLR_FILTER_FORMAT_PREFIX = "discovery.search.format";

    private final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    public String generateFilter(Context context, String field, String operator, String value) {
        GeoFilterFormat geoFilterFormat = GeoFilterFormat.fromFormat(
            this.configurationService.getProperty(
                SOLR_FILTER_FORMAT_PREFIX + "." + field,
                DEFAULT_FORMAT
            )
        );
        return geoFilterFormat.getGenerator().generate(operator, value);
    }

}
