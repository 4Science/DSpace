/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderJournal;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderPublisher;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class OpenPolicyFinderExtraIPublisherMetadataGenerator implements OpenPolicyFinderExtraMetadataGenerator {

    private String relatedInputFormMetadata;

    @Override
    public Map<String, String> build(OpenPolicyFinderJournal journal) {
        Map<String, String> extras = new HashMap<>();
        OpenPolicyFinderPublisher publisher = journal.getPublisher();
        String publisherName = Objects.nonNull(publisher) ? publisher.getName() : StringUtils.EMPTY;
        extras.put("data-" + this.relatedInputFormMetadata, publisherName);
        extras.put(this.relatedInputFormMetadata, publisherName);
        return extras;
    }

    public void setRelatedInputFormMetadata(String relatedInputFormMetadata) {
        this.relatedInputFormMetadata = relatedInputFormMetadata;
    }

}