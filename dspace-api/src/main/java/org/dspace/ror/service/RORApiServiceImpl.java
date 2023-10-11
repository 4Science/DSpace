/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.ror.service;

import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.ror.ROROrgUnitDTO;
import org.dspace.ror.client.RORApiClient;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

public class RORApiServiceImpl implements RORApiService {

    private static final String ORGUNIT_MAPPING_PREFIX = "ror.orgunit-import.api.metadata-field.";

    @Autowired
    private RORApiClient apiClient;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public List<ROROrgUnitDTO> getOrgUnits(String query) {
        return apiClient.searchOrganizations(query);
    }

    @Override
    public Optional<ROROrgUnitDTO> getOrgUnit(String rorId) {
        return apiClient.findOrganizationByRORId(rorId);
    }

    @Override
    public List<MetadataValueDTO> getMetadataValues(String rorId) {
        return getOrgUnit(rorId)
            .map(this::getMetadataValues)
            .orElse(getInactiveMetadataField());
    }

    @Override
    public List<String> getMetadataFields() {
        return configurationService.getPropertyKeys(ORGUNIT_MAPPING_PREFIX).stream()
                                   .map(key -> configurationService.getProperty(key))
                                   .filter(this::isMetadataField)
                                   .collect(Collectors.toList());
    }

    @Override
    public List<MetadataValueDTO> getMetadataValues(ROROrgUnitDTO orgUnit) {

        List<MetadataValueDTO> metadataValues = new ArrayList<>();

        getPersonMetadataField("name")
            .flatMap(field -> getMetadataValue(orgUnit.getName(), field))
            .ifPresent(metadataValues::add);

        getPersonMetadataField("acronym")
            .flatMap(field -> getMetadataArrayValue(orgUnit.getAcronyms(), field))
            .ifPresent(metadataValues::add);

        getPersonMetadataField("url")
            .flatMap(field -> getMetadataValue(orgUnit.getUrl(), field))
            .ifPresent(metadataValues::add);

        getPersonMetadataField("identifier")
            .flatMap(field -> getMetadataValue(orgUnit.getIdentifier(), field))
            .ifPresent(metadataValues::add);

        return metadataValues;

    }

    private List<MetadataValueDTO> getInactiveMetadataField() {
        return getPersonMetadataField("active")
            .flatMap(field -> getMetadataValue("false", field))
            .map(List::of)
            .orElse(List.of());
    }

    private Optional<MetadataValueDTO> getMetadataValue(String value, String field) {
        return Optional.ofNullable(value)
                       .filter(StringUtils::isNotBlank)
                       .map(metadataValue -> new MetadataValueDTO(field, metadataValue));
    }

    private Optional<MetadataValueDTO> getMetadataArrayValue(String[] values, String field) {
        String joinedAcronym = Arrays.stream(values)
                                     .filter(StringUtils::isNotBlank)
                                     .collect(Collectors.joining("/"));
        return StringUtils.isNotEmpty(joinedAcronym)
            ? Optional.of(new MetadataValueDTO(field, joinedAcronym))
            : Optional.empty();
    }

    private boolean isMetadataField(String property) {
        return property != null && property.contains(".");
    }

    private Optional<String> getPersonMetadataField(String fieldName) {
        return ofNullable(configurationService.getProperty(ORGUNIT_MAPPING_PREFIX + fieldName));
    }

    public RORApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(RORApiClient apiClient) {
        this.apiClient = apiClient;
    }
}
