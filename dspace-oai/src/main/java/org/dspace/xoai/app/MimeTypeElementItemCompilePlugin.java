/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.xoai.util.ItemUtils;

/**
 * MimeTypeElementItemCompilePlugin to expose the mimetype on the OAI as
 * dc.format.mimetype
 *
 */
public class MimeTypeElementItemCompilePlugin implements XOAIExtensionItemCompilePlugin {

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    @Override
    public Metadata additionalMetadata(Context context, Metadata metadata, Item item) {
        Element dc;
        List<Element> elements = metadata.getElement();
        for (String fileFormat : getFileFormats(context, item)) {
            if (ItemUtils.getElement(elements, "dc") != null) {
                dc = ItemUtils.getElement(elements, "dc");
            } else {
                dc = ItemUtils.create("dc");
            }

            Element format = ItemUtils.create("format");
            Element mimetype = ItemUtils.create("mimetype");
            Element none = ItemUtils.create("none");
            format.getElement().add(mimetype);
            mimetype.getElement().add(none);
            none.getField().add(ItemUtils.createValue("value", fileFormat));

            dc.getElement().add(format);
        }
        return metadata;
    }

    private List<String> getFileFormats(Context context, Item item) {
        List<String> formats = new ArrayList<>();
        try {
            for (Bundle b : itemService.getBundles(item, "ORIGINAL")) {
                for (Bitstream bs : b.getBitstreams()) {
                    if (!formats.contains(bs.getFormat(context).getMIMEType())) {
                        formats.add(bs.getFormat(context).getMIMEType());
                    }
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return formats;
    }
}
