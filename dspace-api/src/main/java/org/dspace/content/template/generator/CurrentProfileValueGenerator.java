/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template.generator;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.profile.ResearcherProfile;
import org.dspace.profile.service.ResearcherProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link TemplateValueGenerator} that returns a metadata
 * value with information of the profile related the current user
 * (e.g. ###CURRENTPROFILE### or ###CURRENTPROFILE.<metadata>###).
 *
 */
public class CurrentProfileValueGenerator implements TemplateValueGenerator {

    private static final Logger log = LoggerFactory.getLogger(CurrentProfileValueGenerator.class);

    @Autowired
    private ResearcherProfileService researcherProfileService;

    @Override
    public List<MetadataValueVO> generator(Context context, Item targetItem, Item templateItem, String extraParams) {
        EPerson currentUser = context.getCurrentUser();
        ResearcherProfile profile = null;
        try {
            //find profile by current user id
            profile = researcherProfileService.findById(context, currentUser.getID());
            if (profile != null) {
                if (StringUtils.isNotBlank(extraParams)) {
                    //back with the list of metadata
                    return profile.getItem().getMetadata().stream()
                            .filter(metadata -> extraParams.equals(metadata.getMetadataField().toString('.')))
                            .map(metadata -> new MetadataValueVO(metadata.getValue(), metadata.getAuthority()))
                            .collect(Collectors.toList());
                }
                //back with the information of the profile
                return Arrays
                .asList(new MetadataValueVO(profile.getItem().getName(), profile.getItem().getID().toString()));
            }
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage(), e);
        }
        //default case empty value
        return Arrays
              .asList(new MetadataValueVO(""));
    }

    public void setResearcherProfileService(ResearcherProfileService researcherProfileService) {
        this.researcherProfileService = researcherProfileService;
    }
}
