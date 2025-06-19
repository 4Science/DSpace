/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Adds geo-point from a target metadata into the Solr search index.
 *
 * <p>
 * To activate the plugin, add the following line to discovery.xml
 * <pre>
 * {@code
 *   <bean id="solrServiceGeoPointIndexPlugin" class="org.dspace.discovery.SolrServiceGeoPointIndexPlugin">
 *     <constructor-arg index="0" value="dspace.location.geopoint"/>
 *     <constructor-arg index="1" value="geo_p"/>
 *   </bean>
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
 *
 */
public class SolrServiceGeoPointIndexPlugin implements SolrServiceIndexPlugin {

    private static final Pattern GEO_POINT_REGEX =
        Pattern.compile("(-|)(180|(1[0-7][0-9]|[1-9][0-9]|[0-9])(\\.\\d+)*)\\,(-|)(90|([1-8][0-9]|[0-9])(\\.\\d+)*)");
    private static final Logger logger = LoggerFactory.getLogger(SolrServiceGeoPointIndexPlugin.class);

    @Autowired(required = true)
    private ItemService itemService;

    private final IndexPluginMapper<Item, String> metadataMapper;
    private final String indexFieldName;

    public SolrServiceGeoPointIndexPlugin(IndexPluginMapper<Item, String> metadataMapper, String indexFieldName) {
        super();
        this.metadataMapper = metadataMapper;
        this.indexFieldName = indexFieldName;
    }

    @Override
    public void additionalIndex(Context context, IndexableObject dso, SolrInputDocument document) {
        if (!(dso instanceof IndexableItem)) {
            return;
        }

        Item item = ((IndexableItem) dso).getIndexedObject();
        String geoPoint = metadataMapper.map(item);
        if (geoPoint == null) {
            return;
        }

        Matcher matcher = GEO_POINT_REGEX.matcher(geoPoint);
        if (!matcher.matches()) {
            logger.error(
                "Cannot index the value {} with the mapper {} for the item with uuid {}! " +
                "Requested format is {}!",
                geoPoint, metadataMapper.getClass(), item.getID(), GEO_POINT_REGEX
            );
            return;
        }
        document.addField(indexFieldName, geoPoint);
    }

}
