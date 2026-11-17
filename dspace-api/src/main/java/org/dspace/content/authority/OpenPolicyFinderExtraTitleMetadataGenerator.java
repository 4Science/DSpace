/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderJournal;

/**
 * @author Emanuele Sanna (emanuele.sanna at 4science.com)
 */
public class OpenPolicyFinderExtraTitleMetadataGenerator implements OpenPolicyFinderExtraMetadataGenerator {

    private String relatedInputFormMetadata;

    @Override
    public Map<String, String> build(OpenPolicyFinderJournal journal) {
        Map<String, String> extras = new HashMap<>();
        List<String> titles = journal.getTitles();
        String value = CollectionUtils.isEmpty(titles) ? StringUtils.EMPTY : titles.get(0);
        extras.put("data-" + this.relatedInputFormMetadata, value);
        extras.put(this.relatedInputFormMetadata, value);
        return extras;
    }

    public void setRelatedInputFormMetadata(String relatedInputFormMetadata) {
        this.relatedInputFormMetadata = relatedInputFormMetadata;
    }

}
