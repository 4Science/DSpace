/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.commons.collections4.MapUtils;
import org.dspace.app.rest.RestResourceController;

/**
 * The SubmissionPanel REST Resource. It is not addressable directly, only used
 * as inline object in the SubmissionDefinition resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@JsonInclude(value = Include.NON_NULL)
public class SubmissionSectionRest extends BaseObjectRest<String> {

    public static final String NAME = "submissionsection";
    public static final String PLURAL_NAME = "submissionsections";
    public static final String ATTRIBUTE_NAME = "sections";

    private String header;
    private boolean mandatory;
    private boolean opened;
    private String sectionType;
    private ScopeEnum scope;
    private SubmissionVisibilityRest visibility;

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public boolean isOpened() {
        return opened;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public void setOpened(boolean opened) {
        this.opened = opened;
    }

    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    public ScopeEnum getScope() {
        return scope;
    }

    public void setScope(ScopeEnum scope) {
        this.scope = scope;
    }

    public SubmissionVisibilityRest getVisibility() {
        return visibility;
    }

    public void setVisibility(SubmissionVisibilityRest visibility) {
        if (visibility != null && (MapUtils.isNotEmpty(visibility.getVisibilities()))) {
            this.visibility = visibility;
        }
    }

    @Override
    public String getCategory() {
        return SubmissionDefinitionRest.CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    public String getSectionType() {
        return sectionType;
    }

    public void setSectionType(String panelType) {
        this.sectionType = panelType;
    }

}
