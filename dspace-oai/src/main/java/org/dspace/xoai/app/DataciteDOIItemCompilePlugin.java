/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import java.util.Arrays;
import java.util.List;

import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.integration.crosswalks.virtualfields.ItemDOIService;
import org.dspace.core.Context;
import org.dspace.xoai.util.ItemUtils;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * XOAIExtensionItemCompilePlugin aims to add structured information about the
 * DOIs of the item (if any).
 * The xoai document will be enriched with a structure like that
 * <code>
 *   <element name="other">
 *       <element name="datacite">
 *          <element name="primary">
 *              <field name="doi"></field>
 *          </element>
 *          <element name="alternative">
 *              <field name="doi"></field>
 *               ...
 *              <field name="doi"></field>
 *          </element>
 *       </element>
 *   </element>
 * </code>
 *
 */
public class DataciteDOIItemCompilePlugin implements XOAIExtensionItemCompilePlugin {

    @Autowired
    private ItemDOIService itemDOIService;

    @Override
    public Metadata additionalMetadata(Context context, Metadata metadata, Item item) {
        String primaryDoiValue = itemDOIService.getPrimaryDOIFromItem(item);
        String[] alternativeDoiValue = itemDOIService.getAlternativeDOIFromItem(item);
        Element datacite = ItemUtils.create("datacite");
        if (StringUtils.isNotBlank(primaryDoiValue)) {
            Element primary = ItemUtils.create("primary");
            datacite.getElement().add(primary);
            primary.getField().add(ItemUtils.createValue("doi", primaryDoiValue));
            if (alternativeDoiValue != null && alternativeDoiValue.length != 0) {
                Element alternative = ItemUtils.create("alternative");
                datacite.getElement().add(alternative);
                Arrays.stream(alternativeDoiValue)
                        .forEach(value -> alternative.getField().add(ItemUtils.createValue("doi", value)));
            }
            Element other;
            List<Element> elements = metadata.getElement();
            if (ItemUtils.getElement(elements, "others") != null) {
                other = ItemUtils.getElement(elements, "others");
            } else {
                other = ItemUtils.create("others");
            }
            other.getElement().add(datacite);
        }
        return metadata;
    }

}
