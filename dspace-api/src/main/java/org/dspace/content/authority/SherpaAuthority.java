/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.dspace.authority.service.AuthorityValueService.REFERENCE;
import static org.dspace.authority.service.AuthorityValueService.SPLIT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.ISSNValidator;
import org.dspace.app.sherpa.SHERPAService;
import org.dspace.app.sherpa.v2.SHERPAJournal;
import org.dspace.app.sherpa.v2.SHERPAResponse;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link ChoiceAuthority} that search Journals using the
 * SHERPA API.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 * @author Luca Giamminonni (luca.giamminonni at 4science.com)
 */
public class SherpaAuthority extends ItemAuthority {

    private static final String  TYPE = "publication";
    private static final String  ISSN_FIELD = "issn";
    private static final String  TITLE_FILED = "title";
    private static final String  PREDICATE_EQUALS = "equals";
    private static final String  PREDICATE_CONTAINS_WORD = "contains word";

    /**
     * the name assigned to the specific instance by the PluginService, @see
     * {@link NameAwarePlugin}
     **/
    private String authorityName;

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private DSpace dspace = new DSpace();
    private SHERPAService sherpaService = dspace.getSingletonService(SHERPAService.class);
    private List<SherpaExtraMetadataGenerator> generators = dspace.getServiceManager()
                                   .getServicesByType(SherpaExtraMetadataGenerator.class);

    @Override
    public String getLabel(String key, String locale) {
        Choices choices = getMatches(key, 0, 1, locale);
        return choices.values.length == 1 ? choices.values[0].label : StringUtils.EMPTY;
    }

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {

        Choices itemChoices = getLocalItemChoices(text, start, limit, locale);

        int sherpaSearchStart = start > itemChoices.total ? start - itemChoices.total : 0;
        int sherpaSearchLimit = limit > itemChoices.values.length ? limit - itemChoices.values.length : 0;

        Choices choicesFromSherpa = getSherpaChoices(text, sherpaSearchStart, sherpaSearchLimit);
        int total = itemChoices.total + choicesFromSherpa.total;

        Choice[] choices = addAll(itemChoices.values, choicesFromSherpa.values);

        return new Choices(choices, start, total, calculateConfidence(choices), total > (start + limit), 0);

    }

    private Choices getLocalItemChoices(String text, int start, int limit, String locale) {
        if (isLocalItemChoicesEnabled()) {
            return super.getMatches(text, start, limit, locale);
        }
        return new Choices(Choices.CF_UNSET);
    }

    private Choices getSherpaChoices(String text, int start, int limit) {

        boolean isIssn = ISSNValidator.getInstance().isValid(text);
        String field = isIssn ? ISSN_FIELD : TITLE_FILED;
        String predicate = isIssn ? PREDICATE_EQUALS : PREDICATE_CONTAINS_WORD;

        List<SHERPAJournal> journals = getJournalsFromSherpa(field, predicate, text, start, limit);

        Choice[] results = journals.stream()
            .map(journal -> convertToChoice(journal))
            .toArray(Choice[]::new);

        // From Sherpa we don't get the total number of results for a specific search,
        // so the pagination count may be incorrect
        int total = sherpaService.performCountRequest(TYPE, field, predicate, text);

        if (total <= 0) {
            total = results.length;
        }

        return new Choices(results, start, total, calculateConfidence(results), total > (start + limit), 0);

    }

    private List<SHERPAJournal> getJournalsFromSherpa(String field, String predicate,
        String text, int start, int limit) {

        if (limit <= 0) {
            return List.of();
        }

        SHERPAResponse sherpaResponse = sherpaService.performRequest(TYPE, field, predicate, text, start, limit);

        if (sherpaResponse == null || CollectionUtils.isEmpty(sherpaResponse.getJournals())) {
            return List.of();
        }

        return sherpaResponse.getJournals();

    }

    private Choice convertToChoice(SHERPAJournal journal) {
        String authority = composeAuthorityValue(journal);
        Map<String, String> extras = getSherpaExtra(journal);
        String title = journal.getTitles().get(0);
        return new Choice(authority, title, title, extras, getSource());
    }

    private Map<String, String> getSherpaExtra(SHERPAJournal journal) {
        Map<String, String> extras = new HashMap<String, String>();
        if (CollectionUtils.isNotEmpty(generators)) {
            for (SherpaExtraMetadataGenerator generator : generators) {
                extras.putAll(generator.build(journal));
            }
        }
        return extras;
    }

    private String composeAuthorityValue(SHERPAJournal journal) {

        if (CollectionUtils.isEmpty(journal.getIssns())) {
            return "";
        }

        String issn = journal.getIssns().get(0);

        String prefix = configurationService.getProperty("sherpa.authority.prefix", REFERENCE + "ISSN" + SPLIT);
        return prefix.endsWith(SPLIT) ? prefix + issn : prefix + SPLIT + issn;

    }

    @Override
    public String getPluginInstanceName() {
        return this.authorityName;
    }

    @Override
    public void setPluginInstanceName(String name) {
        this.authorityName = name;
    }

    @Override
    public String getLinkedEntityType() {
        return configurationService.getProperty("cris." + this.authorityName + ".entityType", "Journal");
    }

    private boolean isLocalItemChoicesEnabled() {
        return configurationService.getBooleanProperty("cris." + this.authorityName + ".local-item-choices-enabled");
    }

}