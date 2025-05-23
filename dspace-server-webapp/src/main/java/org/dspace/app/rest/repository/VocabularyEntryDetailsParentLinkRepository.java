/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.NotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.VocabularyEntryDetailsRest;
import org.dspace.app.rest.model.VocabularyRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.DSpaceControlledVocabulary;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository to expose the parent of a vocabulary entry details in an hierarchical vocabulary
 *
 * @author Mykhaylo Boychuk ($science.it)
 */
@Component(VocabularyRest.CATEGORY + "." + VocabularyEntryDetailsRest.PLURAL_NAME + "." +
    VocabularyEntryDetailsRest.PARENT)
public class VocabularyEntryDetailsParentLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @Autowired
    private AuthorityUtils authorityUtils;

    @PreAuthorize("permitAll()")
    public VocabularyEntryDetailsRest getParent(@Nullable HttpServletRequest request, String name,
            @Nullable Pageable optionalPageable, Projection projection) {
        Context context = obtainContext();
        String[] parts = StringUtils.split(name, ":", 2);
        if (parts.length != 2) {
            return null;
        }
        String vocabularyName = parts[0];
        String id = parts[1];

        ChoiceAuthority authority = choiceAuthorityService.getChoiceAuthorityByAuthorityName(vocabularyName);
        Choice choice = null;
        boolean fix = false;
        if (StringUtils.isNotBlank(id) && authority != null && authority.isHierarchical()) {
            //FIXME hack to deal with an improper use on the angular side of the node id (otherinformation.id) to
            // build a vocabulary entry details ID
            if (authority instanceof DSpaceControlledVocabulary && !StringUtils.startsWith(id, vocabularyName)) {
                fix = true;
                id = vocabularyName + DSpaceControlledVocabulary.ID_SPLITTER + id;
            }
            choice = choiceAuthorityService.getParentChoice(vocabularyName, id, context.getCurrentLocale().toString());
        } else {
            throw new NotFoundException();
        }
        return authorityUtils.convertEntryDetails(fix, choice, vocabularyName, authority.isHierarchical(),
                authority.storeAuthorityInMetadata(), utils.obtainProjection());
    }
}
