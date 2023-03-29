/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.ISSNValidator;
import org.dspace.app.sherpa.SHERPAService;
import org.dspace.app.sherpa.v2.SHERPAJournal;
import org.dspace.app.sherpa.v2.SHERPAResponse;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class SherpaAuthority implements ChoiceAuthority, LinkableEntityAuthority {

    private static final String  TYPE = "publication";
    private static final String  ISSN_FIELD = "issn";
    private static final String  TITLE_FILED = "title";
    private static final String  PREDICATE_EQUALS = "equals";
    private static final String  PREDICATE_CONTAINS_WORD = "contains word";

    private static final Logger log = LoggerFactory.getLogger(SherpaAuthority.class);

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
    public Choices getBestMatch(String text, String locale) {
        return getMatches(text, 0, 2, locale);
    }

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        Choice[] choice = sherpaSearch(text, start, limit <= 0 ? 20 : limit);
        return new Choices(choice, start, choice.length, Choices.CF_AMBIGUOUS, false);
    }

    private Choice[] sherpaSearch(String text, int start, int limit) {
        if (Objects.nonNull(sherpaService)) {
            List<Choice> results = new ArrayList<Choice>();
            boolean isIssn = ISSNValidator.getInstance().isValid(text);
            String field = isIssn ? ISSN_FIELD : TITLE_FILED;
            String predicate = isIssn ? PREDICATE_EQUALS : PREDICATE_CONTAINS_WORD;
            SHERPAResponse sherpaResponse = sherpaService.performRequest(TYPE, field, predicate, text, start, limit);
            String authority;
            if (CollectionUtils.isNotEmpty(sherpaResponse.getJournals())) {
                for (SHERPAJournal journal : sherpaResponse.getJournals()) {
                    authority = CollectionUtils.isEmpty(journal.getIssns()) ? "" : journal.getIssns().get(0);
                    Map<String, String> extras = getSherpaExtra(journal);
                    String title = journal.getTitles().get(0);
                    results.add(new Choice(authority, title, title, extras));
                }
            }
            return results.toArray(new Choice[results.size()]);
        } else {
            log.warn("External source for SherpaAuthority not configured!");
            return new Choice[0];
        }
    }

    @Override
    public String getLabel(String key, String locale) {
        Choice[] choices = sherpaSearch(key, 0, 1);
        return choices.length == 1 ? choices[0].label : StringUtils.EMPTY;
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

    @Override
    public Map<String, String> getExternalSource() {
        return Map.of();
    }

}