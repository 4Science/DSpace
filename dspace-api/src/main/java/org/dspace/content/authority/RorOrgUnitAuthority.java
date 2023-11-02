/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content.authority;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dspace.content.authority.factory.ItemAuthorityServiceFactory;
import org.dspace.ror.ROROrgUnitDTO;
import org.dspace.ror.service.RORApiService;
import org.dspace.ror.service.RORApiServiceImpl;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

public class RorOrgUnitAuthority extends ItemAuthority {

    private final RORApiService rorApiService = dspace.getSingletonService(RORApiServiceImpl.class);
    private final ItemAuthorityServiceFactory itemAuthorityServiceFactory =
        dspace.getServiceManager().getServiceByName("itemAuthorityServiceFactory", ItemAuthorityServiceFactory.class);
    private final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    private String authorityName;

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        super.setPluginInstanceName(authorityName);
        Choices solrChoices = super.getMatches(text, start, limit, locale);

        return solrChoices.values.length == 0 ? getRORApiMatches(text, start, limit) : solrChoices;
    }

    private Choices getRORApiMatches(String text, int start, int limit) {
        Choice[] rorApiChoices = getChoiceFromRORQueryResults(
            rorApiService.getOrgUnits(text).stream()
                         .filter(ou -> "active".equals(ou.getStatus()))
                         .collect(Collectors.toList())
        ).toArray(new Choice[0]);

        int confidenceValue = itemAuthorityServiceFactory.getInstance(authorityName)
                                                         .getConfidenceForChoices(rorApiChoices);

        return new Choices(rorApiChoices, start, rorApiChoices.length, confidenceValue,
                           rorApiChoices.length > (start + limit), 0);
    }

    private List<Choice> getChoiceFromRORQueryResults(List<ROROrgUnitDTO> orgUnits) {
        return orgUnits
            .stream()
            .map(orgUnit -> new Choice(composeAuthorityValue(orgUnit.getIdentifier()), orgUnit.getName(),
                                       orgUnit.getName(), buildExtras(orgUnit)))
            .collect(Collectors.toList());
    }

    private Map<String, String> buildExtras(ROROrgUnitDTO orgUnit) {
        return new HashMap<>();
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
