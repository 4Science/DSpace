/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.content.DSpaceObject;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.factory.ContentServiceFactory;

/**
 * Maps a DSpaceObject to a metadata field value for indexing purposes.
 *
 * This class implements the IndexPluginMapper interface, allowing it to map
 * a DSpaceObject to a specific metadata field value, which is useful for
 * indexing and search functionalities within the DSpace framework.
 *
 * @param <T> the type of DSpaceObject being mapped
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class IndexPluginMetadataMapper<T extends DSpaceObject> implements IndexPluginMapper<T, List<MetadataValueDTO>> {

    protected final String metadataField;

    public IndexPluginMetadataMapper(String metadataField) {
        this.metadataField = metadataField;
    }

    @Override
    public List<MetadataValueDTO> map(T t) {
        return ContentServiceFactory.getInstance().getDSpaceObjectService(t)
                                    .getMetadataByMetadataString(t, metadataField)
                                    .stream()
                                    .map(MetadataValueDTO::new)
                                    .collect(Collectors.toList());
    }

}
