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
import org.dspace.app.openpolicyfinder.OpenPolicyFinderService;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderJournal;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderResponse;
import org.dspace.core.NameAwarePlugin;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link ChoiceAuthority} that search Journals
 * using the Open Policy Finder API.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 * @author Luca Giamminonni (luca.giamminonni at 4science.com)
 */
public class OpenPolicyFinderAuthority extends ItemAuthority {

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
    private OpenPolicyFinderService opfService = dspace.getSingletonService(OpenPolicyFinderService.class);
    private List<OpenPolicyFinderExtraMetadataGenerator> generators = dspace.getServiceManager()
                                                       .getServicesByType(OpenPolicyFinderExtraMetadataGenerator.class);

    @Override
    public String getLabel(String key, String locale) {
        Choices choices = getMatches(key, 0, 1, locale);
        return choices.values.length == 1 ? choices.values[0].label : StringUtils.EMPTY;
    }

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        Choices itemChoices = getLocalItemChoices(text, start, limit, locale);

        int opfSearchStart = start > itemChoices.total ? start - itemChoices.total : 0;
        int opfSearchLimit = limit > itemChoices.values.length ? limit - itemChoices.values.length : 0;

        Choices choicesFromOpenPolicyFinder = getOpenPolicyFinderChoices(text, opfSearchStart, opfSearchLimit);
        int total = itemChoices.total + choicesFromOpenPolicyFinder.total;

        Choice[] choices = addAll(itemChoices.values, choicesFromOpenPolicyFinder.values);

        return new Choices(choices, start, total, calculateConfidence(choices), total > (start + limit), 0);
    }

    private Choices getLocalItemChoices(String text, int start, int limit, String locale) {
        if (isLocalItemChoicesEnabled()) {
            return super.getMatches(text, start, limit, locale);
        }
        return new Choices(Choices.CF_UNSET);
    }

    private Choices getOpenPolicyFinderChoices(String text, int start, int limit) {
        boolean isIssn = ISSNValidator.getInstance().isValid(text);
        String field = isIssn ? ISSN_FIELD : TITLE_FILED;
        String predicate = isIssn ? PREDICATE_EQUALS : PREDICATE_CONTAINS_WORD;

        List<OpenPolicyFinderJournal> journals = getJournalsFromOpenPolicyFinder(field, predicate, text, start, limit);

        Choice[] results = journals.stream()
                                   .map(journal -> convertToChoice(journal))
                                   .toArray(Choice[]::new);

        // From OpenPolicyFinder we don't get the total number of results for a specific search,
        // so the pagination count may be incorrect
        int total = opfService.performCountRequest(TYPE, field, predicate, text);

        if (total <= 0) {
            total = results.length;
        }
        return new Choices(results, start, total, calculateConfidence(results), total > (start + limit), 0);
    }

    private List<OpenPolicyFinderJournal> getJournalsFromOpenPolicyFinder(String field, String predicate, String text,
                                                                          int start, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        OpenPolicyFinderResponse opfResponse = opfService.performRequest(TYPE, field, predicate, text, start, limit);
        if (opfResponse == null || CollectionUtils.isEmpty(opfResponse.getJournals())) {
            return List.of();
        }
        return opfResponse.getJournals();
    }

    private Choice convertToChoice(OpenPolicyFinderJournal journal) {
        String authority = composeAuthorityValue(journal);
        Map<String, String> extras = getOpenPolicyFinderExtra(journal);
        String title = journal.getTitles().get(0);
        return new Choice(authority, title, title, extras, getSource());
    }

    private Map<String, String> getOpenPolicyFinderExtra(OpenPolicyFinderJournal journal) {
        Map<String, String> extras = new HashMap<>();
        if (CollectionUtils.isNotEmpty(generators)) {
            for (OpenPolicyFinderExtraMetadataGenerator generator : generators) {
                extras.putAll(generator.build(journal));
            }
        }
        return extras;
    }

    private String composeAuthorityValue(OpenPolicyFinderJournal journal) {
        if (CollectionUtils.isEmpty(journal.getIssns())) {
            return "";
        }

        String issn = journal.getIssns().get(0);

        String prefix = configurationService.getProperty("opf.authority.prefix", REFERENCE + "ISSN" + SPLIT);
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
    public String[] getLinkedEntityTypes() {
        return configurationService.getArrayProperty("cris." + this.authorityName + ".entityType",
            new String[] {"Journal"});
    }

    @Override
    public String getPrimaryLinkedEntityType() {
        String entityType = configurationService.getProperty(
            "cris.ItemAuthority." + authorityName + ".primaryEntityType");
        if (org.apache.commons.lang3.StringUtils.isNotBlank(entityType)) {
            return entityType;
        }

        // fallback strategy
        String[] entityTypes = getLinkedEntityTypes();
        if (entityTypes != null && entityTypes.length == 1) {
            return entityTypes[0];
        }

        // default strategy
        return "Journal";
    }

    private boolean isLocalItemChoicesEnabled() {
        return configurationService.getBooleanProperty("cris." + this.authorityName + ".local-item-choices-enabled");
    }

}