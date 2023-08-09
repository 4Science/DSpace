/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;
import org.dspace.iiif.util.IIIFSharedUtils;

/**
 * The ToC transformer implements {@link BulkImportValueTransformer}
 * and permits to manage the iiif.toc metadata.
 * 
 * Since the {@link IIIFSharedUtils#TOC_SEPARATOR} conflicts with the "||" used as metadata separator,
 * the XLS file must use the ">>>" separator for the iiif.toc metadata.
 * This transformer then replaces the custom separator with {@link IIIFSharedUtils#TOC_SEPARATOR}.
 * 
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ToCBulkImportValueTransformer implements BulkImportValueTransformer {

    public MetadataValueVO transform(Context context, MetadataValueVO metadataValue) {

        // the iiif.toc metadata uses the ">>>" separator
        // now convert the ">>>" separator to the ToC separator
        String value = metadataValue.getValue();
        if (StringUtils.isNotBlank(value)) {
            return new MetadataValueVO(
                    StringUtils.replace(value, ">>>", IIIFSharedUtils.TOC_SEPARATOR),
                    metadataValue.getAuthority(), metadataValue.getConfidence(),
                    metadataValue.getSecurityLevel());
        }

        return metadataValue;
    }

}