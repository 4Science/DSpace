/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content.authority;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.authority.factory.ItemAuthorityServiceFactory;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.ror.service.RorImportMetadataSourceServiceImpl;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

public class RorOrgUnitAuthority extends ItemAuthority {

    private final RorImportMetadataSourceServiceImpl rorImportMetadataSource = new DSpace().getServiceManager()
        .getServicesByType(RorImportMetadataSourceServiceImpl.class).get(0);

    private final ItemAuthorityServiceFactory itemAuthorityServiceFactory =
        dspace.getServiceManager().getServiceByName("itemAuthorityServiceFactory", ItemAuthorityServiceFactory.class);
    private final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    private String authorityName;

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {

        super.setPluginInstanceName(authorityName);
        Choices solrChoices = super.getMatches(text, start, limit, locale);

        try {
            return solrChoices.values.length == 0 ? getRORApiMatches(text, start, limit) : solrChoices;
        } catch (MetadataSourceException e) {
            throw new RuntimeException(e);
        }
    }

    private Choices getRORApiMatches(String text, int start, int limit) throws MetadataSourceException {
        Choice[] rorApiChoices = getChoiceFromRORQueryResults(rorImportMetadataSource.getRecords(text, 0, 0))
            .toArray(new Choice[0]);

        int confidenceValue = itemAuthorityServiceFactory.getInstance(authorityName)
                                                         .getConfidenceForChoices(rorApiChoices);

        return new Choices(rorApiChoices, start, rorApiChoices.length, confidenceValue,
                           rorApiChoices.length > (start + limit), 0);
    }

    private List<Choice> getChoiceFromRORQueryResults(Collection<ImportRecord> orgUnits) {
        return orgUnits
            .stream()
            .map(orgUnit -> new Choice(composeAuthorityValue(getIdentifier(orgUnit)), getName(orgUnit),
                getName(orgUnit), buildExtras(orgUnit)))
            .collect(Collectors.toList());
    }

    private String getIdentifier(ImportRecord orgUnit) {
        return orgUnit.getValue("organization", "identifier", "ror").stream()
            .findFirst()
            .map(metadata -> metadata.getValue())
            .orElse(null);
    }

    private String getName(ImportRecord orgUnit) {
        return orgUnit.getValue("dc", "title", null).stream()
            .findFirst()
            .map(metadata -> metadata.getValue())
            .orElse(null);
    }

    private Map<String, String> buildExtras(ImportRecord orgUnit) {

        Map<String, String> extras = new LinkedHashMap<String, String>();

        addExtra(extras, getIdentifier(orgUnit), "id");

        orgUnit.getSingleValue("dc", "type", null)
            .ifPresent(type -> addExtra(extras, type, "type"));

        String acronym = orgUnit.getValue("oairecerif", "acronym", null).stream()
            .map(MetadatumDTO::getValue)
            .collect(Collectors.joining(", "));

        if (StringUtils.isNotBlank(acronym)) {
            addExtra(extras, acronym, "acronym");
        }

        return extras;
    }

    private void addExtra(Map<String, String> extras, String value, String extraType) {

        String key = getKey(extraType);

        if (useAsData(extraType)) {
            extras.put("data-" + key, value);
        }
        if (useForDisplaying(extraType)) {
            extras.put(key, value);
        }

    }

    private boolean useForDisplaying(String extraType) {
        return configurationService.getBooleanProperty("cris.OrcidAuthority."
            + getPluginInstanceName() + "." + extraType + ".display", true);
    }

    private boolean useAsData(String extraType) {
        return configurationService.getBooleanProperty("cris.OrcidAuthority."
            + getPluginInstanceName() + "." + extraType + ".as-data", true);
    }

    private String getKey(String extraType) {
        return configurationService.getProperty("cris.OrcidAuthority."
            + getPluginInstanceName() + "." + extraType + ".key", "ror_orgunit_" + extraType);
    }

    private String composeAuthorityValue(String rorId) {
        String prefix = configurationService.getProperty("ror.authority.prefix", "will be referenced::ROR-ID::");
        return prefix + rorId;
    }

    @Override
    public String getLinkedEntityType() {
        return configurationService.getProperty("cris.ItemAuthority." + authorityName + ".entityType");
    }

    @Override
    public void setPluginInstanceName(String name) {
        authorityName = name;
    }

    @Override
    public String getPluginInstanceName() {
        return authorityName;
    }
}
