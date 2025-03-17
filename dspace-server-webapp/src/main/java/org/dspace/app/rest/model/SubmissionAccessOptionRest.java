/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.RestResourceController;

/**
 * The Access Condition Section Configuration REST Resource
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class SubmissionAccessOptionRest extends BaseObjectRest<String> {

    private static final long serialVersionUID = -7708437586052984082L;

    public static final String NAME = "submissionaccessoption";
    public static final String PLURAL_NAME = "submissionaccessoptions";
    public static final String CATEGORY = RestAddressableModel.CONFIGURATION;

    private String id;

    private Boolean canChangeDiscoverable;

    private List<AccessConditionOptionRest> accessConditionOptions;

    private boolean singleAccessCondition;

    private boolean required;

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getCanChangeDiscoverable() {
        return canChangeDiscoverable;
    }

    public void setCanChangeDiscoverable(Boolean canChangeDiscoverable) {
        this.canChangeDiscoverable = canChangeDiscoverable;
    }

    public List<AccessConditionOptionRest> getAccessConditionOptions() {
        if (Objects.isNull(accessConditionOptions)) {
            accessConditionOptions = new ArrayList<>();
        }
        return accessConditionOptions;
    }

    public void setAccessConditionOptions(List<AccessConditionOptionRest> accessConditionOptions) {
        this.accessConditionOptions = accessConditionOptions;
    }

    public boolean isSingleAccessCondition() {
        return singleAccessCondition;
    }

    public void setSingleAccessCondition(boolean singleAccessCondition) {
        this.singleAccessCondition = singleAccessCondition;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    @JsonIgnore
    @SuppressWarnings("rawtypes")
    public Class getController() {
        return RestResourceController.class;
    }

}
