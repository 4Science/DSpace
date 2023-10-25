/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metadata.export.model;

import javax.xml.bind.JAXBElement;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class DspaceHeaderBuilder extends AbstractJaxbBuilder<DspaceHeader, String> {

    protected DspaceHeaderBuilder() {
        super(DspaceHeader.class);
    }

    public static DspaceHeaderBuilder createBuilder() {
        return new DspaceHeaderBuilder();
    }

    public DspaceHeaderBuilder withTitle(String title) {
        addChildElement(title, objectFactory::createTitle);
        return this;
    }

    public DspaceHeaderBuilder withContributorAuthor(String contributorAuthor) {
        addChildElement(contributorAuthor, objectFactory::createContributorAuthor);
        return this;
    }

    public DspaceHeaderBuilder withContributorEditor(String contributorEditor) {
        addChildElement(contributorEditor, objectFactory::createContributorEditor);
        return this;
    }

    public DspaceHeaderBuilder withDateCreated(String dateCreated) {
        addChildElement(dateCreated, objectFactory::createDateCreated);
        return this;
    }

    public DspaceHeaderBuilder withDescription(String description) {
        addChildElement(description, objectFactory::createDescription);
        return this;
    }

    public DspaceHeaderBuilder withDescriptionVersion(String descriptionVersion) {
        addChildElement(descriptionVersion, objectFactory::createDescriptionVersion);
        return this;
    }

    @Override
    protected void addChildElement(JAXBElement<String> v) {
        getObejct().getTitleOrContributorAuthorOrContributorEditor().add(v);
    }
}
