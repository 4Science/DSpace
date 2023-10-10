/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.AbstractExternalDataProvider;
import org.dspace.ror.ROROrgUnitDTO;
import org.dspace.ror.service.RORApiService;
import org.springframework.beans.factory.annotation.Autowired;

public class RorOrgUnitDataProvider extends AbstractExternalDataProvider {

    @Autowired
    private RORApiService rorApiService;

    private String sourceIdentifier;

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {
        return rorApiService.getOrgUnit(id)
            .map(this::convertToExternalDataObject);
    }

    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {
        return rorApiService.getOrgUnits(query).stream()
            .map(this::convertToExternalDataObject)
            .collect(Collectors.toList());
    }

    private ExternalDataObject convertToExternalDataObject(ROROrgUnitDTO orgUnit) {
        ExternalDataObject object = new ExternalDataObject(sourceIdentifier);
        object.setId(orgUnit.getIdentifier());
        object.setValue(orgUnit.getName());
        object.setDisplayValue(orgUnit.getName());
        object.setMetadata(rorApiService.getMetadataValues(orgUnit));
        return object;
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equals(sourceIdentifier, source);
    }

    @Override
    public int getNumberOfResults(String query) {
        return searchExternalDataObjects(query, 0, -1).size();
    }

    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    @Override
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

}
