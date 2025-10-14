/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.marcxml2item.parser;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.dspace.app.marcxml2item.model.ItemsImportMapping;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.w3c.dom.Node;

/**
 * Service interface class for the MarcXmlParserImpl
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 **/
public interface MarcXmlParser {

    Set<String> getAllRecordTypes();

    ItemsImportMapping parseMapping(String configuration);

    List<MetadataValueDTO> readItemMetadataValues(Context context, Node record, ItemsImportMapping mapping);

    List<List<MetadataValueDTO>> readItems (Context context, InputStream source, ItemsImportMapping mapping,
                                            String expression);

}
