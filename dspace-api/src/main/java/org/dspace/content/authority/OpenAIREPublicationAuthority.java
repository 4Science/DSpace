/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.dspace.authority.service.AuthorityValueService.GENERATE;
import static org.dspace.authority.service.AuthorityValueService.SPLIT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.openaire.service.OpenAireImportMetadataSourceServiceImpl;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class OpenAIREPublicationAuthority extends ItemAuthority {

    private static final Logger log = LoggerFactory.getLogger(OpenAIREPublicationAuthority.class);

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        Choices itemChoices = super.getMatches(text, start, limit, locale);
        int openAIRESearchStart = start > itemChoices.total ? start - itemChoices.total : 0;
        int openAIRESearchLimit = limit > itemChoices.values.length ? limit - itemChoices.values.length : 0;

        try {

            Choices openAIREChoices = openAIREPublicationSearch(text, openAIRESearchStart, openAIRESearchLimit);
            int total = itemChoices.total + openAIREChoices.total;

            Choice[] choices = addAll(itemChoices.values, openAIREChoices.values);
            return new Choices(choices, start, total, calculateConfidence(choices), total > (start + limit), 0);

        } catch (Exception ex) {
            log.error("An error occurs performing projects search on OpenAIRE", ex);
            return itemChoices;
        }
    }

    private Choices openAIREPublicationSearch(String text, int start, int limit) {
        List<ImportRecord> records = importOpenAIREPublications(text, start, limit);
        if (CollectionUtils.isEmpty(records)) {
            return new Choices(Choices.CF_UNSET);
        }

        int total = records.size();
        Choice[] choices = records.stream()
                                  .map(this::convertToChoice)
                                  .toArray(Choice[]::new);

        return new Choices(choices, start, total, calculateConfidence(choices), total > (start + limit), 0);
    }

    private List<ImportRecord> importOpenAIREPublications(String text, int start, int limit) {
        try {
            return (List<ImportRecord>) getOpenAireService().getRecords(text, start, limit);
        } catch (MetadataSourceException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Choice convertToChoice(ImportRecord record) {
        String value = getMetadataValue(record, "dc", "title", null);
        String code = getMetadataValue(record, "dc", "identifier", null);
        String authority = getAuthorityPrefix() + code;
        String label = StringUtils.isNotBlank(code) ? value + "(" + code + ")" : value;
        return new Choice(authority, value, label, getOpenAireExtra(code), getSource());
    }

    private String getMetadataValue(ImportRecord record, String schema, String element, String qualifier) {
        return record.getValueList()
                     .stream()
                     .filter(metadatum -> StringUtils.equals(metadatum.getSchema(), schema))
                     .filter(metadatum -> StringUtils.equals(metadatum.getElement(), element))
                     .filter(metadatum -> StringUtils.equals(metadatum.getQualifier(), qualifier))
                     .map(metadatum -> metadatum.getValue())
                     .findFirst()
                     .orElse("");
    }

    private String getAuthorityPrefix() {
        return DSpaceServicesFactory.getInstance().getConfigurationService()
                     .getProperty("openaire-publication.authority.prefix",GENERATE + "OPENAIRE-PUBLICATION-ID" + SPLIT);
    }

    private Map<String, String> getOpenAireExtra(String value) {
        Map<String, String> extras = new HashMap<>();
        for (OpenAIREExtraMetadataGenerator extraMetadataGenerator : getExtraMetadataGenerators()) {
            extras.putAll(extraMetadataGenerator.build(value));
        }
        return extras;
    }

    private List<OpenAIREExtraMetadataGenerator> getExtraMetadataGenerators() {
        return this.dspace.getServiceManager().getServicesByType(OpenAIREExtraMetadataGenerator.class);
    }

    private OpenAireImportMetadataSourceServiceImpl getOpenAireService() {
        return this.dspace
                   .getServiceManager()
                   .getServiceByName("openaireImportServiceByTitle", OpenAireImportMetadataSourceServiceImpl.class);
    }

}
