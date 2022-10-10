/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script.service.impl;

/**
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class CrisLayoutToolRenderNumberSubTypeValidatorImpl extends CrisLayoutToolRenderValidatorImpl {

    private String followedBy = "";

    @Override
    protected boolean isSubTypeNotSupported(String renderType) {
        String subType = renderType.split("\\.")[1];
        subType = subType.endsWith(followedBy) ? subType.replace(followedBy, "").trim() : subType;
        return isSubTypeNotNumber(subType);
    }

    private boolean isSubTypeNotNumber(String subType) {
        return !subType.matches("[0-9]+");
    }

    public String getFollowedBy() {
        return followedBy;
    }

    public void setFollowedBy(String followedBy) {
        this.followedBy = followedBy;
    }
}
