/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.mapper.geomap.geojson.GeoJSONMapper;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.validation.model.ValidationError;
import org.dspace.validation.service.GeoJsonValidationService;
import org.dspace.validation.service.factory.GeoJsonValidationServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Adds geo-coordinates extracted from a target mapper into the Solr search index.
 *
 * <p>
 * To activate the plugin, add the following line to discovery.xml
 * <pre>
 * {@code
 *    <bean id="solrServiceGeoPointIndexPlugin" class="org.dspace.discovery.SolrServiceGeoJsonGeometryIndexPlugin">
 *       <constructor-arg name="metadataMapper" ref="geoPointMapper"/>
 *       <constructor-arg name="indexFieldName" value="geo_p"/>
 *       <constructor-arg name="schemaURL" value="${dspace.dir}/config/schemas/geojson/Geometry.json"/>
 *       <constructor-arg name="pattern"
 *         value="#{T(org.dspace.app.mapper.geomap.geojson.GeoJSONMapper).POINT_FORMAT_PATTERN}"/>
 *    </bean>
 * }
 * </pre>
 *
 * <p>
 * After activating the plugin, rebuild the discovery index by executing:
 * <pre>
 * [dspace]/bin/dspace index-discovery -b
 * </pre>
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class SolrServiceGeoJsonGeometryIndexPlugin implements SolrServiceIndexPlugin {

    private static final Logger logger = LoggerFactory.getLogger(SolrServiceGeoJsonGeometryIndexPlugin.class);

    public static final String DEFAULT_COORDINATES_PATTERN = "(\\d+.\\d+),(\\d+.\\d+);{0,1}" ;

    @Autowired(required = true)
    private ItemService itemService;

    private final GeoJSONMapper jsonMapper = new GeoJSONMapper();
    private final IndexPluginMapper<Item, List<String>> metadataMapper;
    private final String indexFieldName;
    private final String schemaURL;
    private final Pattern coordinatesPattern;

    protected GeoJsonValidationService getGeoJsonValidationService() {
        return GeoJsonValidationServiceFactory.getInstance().getValidationService();
    }

    public SolrServiceGeoJsonGeometryIndexPlugin(
        IndexPluginMapper<Item, List<String>> metadataMapper, String indexFieldName,
        String schemaURL, String pattern
    ) {
        super();
        this.metadataMapper = metadataMapper;
        this.indexFieldName = indexFieldName;
        this.schemaURL = schemaURL;
        this.coordinatesPattern = Pattern.compile(pattern);
    }

    public SolrServiceGeoJsonGeometryIndexPlugin(
        IndexPluginMapper<Item, List<String>> metadataMapper, String indexFieldName, String schemaURL
    ) {
        this(metadataMapper, indexFieldName, schemaURL, DEFAULT_COORDINATES_PATTERN);
    }

    @Override
    public void additionalIndex(Context context, IndexableObject dso, SolrInputDocument document) {
        if (!(dso instanceof IndexableItem)) {
            return;
        }

        List<String> coordinates = metadataMapper.map(((IndexableItem) dso).getIndexedObject());
        if (coordinates == null || coordinates.isEmpty()) {
            return;
        }

        String type = jsonMapper.getPointType();
        if (coordinates.size() > 1) {
            type = jsonMapper.getMultiPointType();
        }

        String geoJSON =
            jsonMapper.map(type, coordinates, coordinatesPattern, true);

        if (StringUtils.isEmpty(geoJSON)) {
            return;
        }

        List<ValidationError> validationErrors =
            getGeoJsonValidationService().validateJson(
                schemaURL,
                geoJSON
            );
        if (validationErrors != null && !validationErrors.isEmpty()) {
            return;
        }

        document.addField(indexFieldName, geoJSON);
    }

}
