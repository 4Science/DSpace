/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.model.BrowseIndexRest.BROWSE_TYPE_FLAT;
import static org.dspace.app.rest.model.BrowseIndexRest.BROWSE_TYPE_HIERARCHICAL;
import static org.dspace.app.rest.model.BrowseIndexRest.BROWSE_TYPE_VALUE_LIST;
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.content.authority.DSpaceControlledVocabularyIndex;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.hamcrest.Matcher;

/**
 * Utility class to construct a Matcher for a browse index
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Raf Ponsaerts (raf dot ponsaerts at atmire dot com)
 */
public class BrowseIndexMatcher {

    static ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();
    static ChoiceAuthorityService choiceAuthorityService =
            ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
    public static BrowseIndex[] bis = getAllBrowseIndices();

    private BrowseIndexMatcher() { }

    private static BrowseIndex[] getAllBrowseIndices() {
        List<BrowseIndex> browseIndices = new ArrayList<>();
        browseIndices.addAll(List.of(getConfiguredBrowseIndices()));
        browseIndices.addAll(getVocabularyIndices());
        return browseIndices.toArray(new BrowseIndex[0]);
    }

    private static BrowseIndex[] getConfiguredBrowseIndices() {
        try {
            return BrowseIndex.getBrowseIndices();
        } catch (BrowseException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<DSpaceControlledVocabularyIndex> getVocabularyIndices() {
        // Get all names of vocabularies in the project (configured in /dspace/config/controlled-vocabularies/)
        File vocDir = new File(configurationService.getProperty("dspace.dir") + "/config/controlled-vocabularies");
        // And store them in a list
        List<DSpaceControlledVocabularyIndex> vocabularies = new ArrayList<>();

        for (File file : vocDir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".xml") && !file.getName().matches("(.*)_[a-z]{2}\\.xml")) {
                String vocName = file.getName().substring(0, file.getName().lastIndexOf('.'));
                // Get the vocabulary index, if not null add to the list
                DSpaceControlledVocabularyIndex vocBrowseIndex = choiceAuthorityService.getVocabularyIndex(vocName);
                if (vocBrowseIndex != null) {
                    vocabularies.add(vocBrowseIndex);
                }
            }
        }
        return vocabularies;
    }

    public static Matcher<? super Object>[] createBrowseMatchers(List<BrowseIndex> browseIndices) {
        return browseIndices.stream()
                .map(BrowseIndexMatcher::matchBrowseIndex)
                .toArray(Matcher[]::new);
    }

    public static Matcher<? super Object> matchBrowseIndex(BrowseIndex bi) {
        if (bi instanceof DSpaceControlledVocabularyIndex) {
            DSpaceControlledVocabularyIndex vocbi = (DSpaceControlledVocabularyIndex) bi;
            return allOf(
                    hasJsonPath("$.metadata", contains(getIndexMetadata(vocbi))),
                    hasJsonPath("$.browseType", equalToIgnoringCase(BROWSE_TYPE_HIERARCHICAL)),
                    hasJsonPath("$.type", equalToIgnoringCase("browse")),
                    hasJsonPath("$.facetType", equalToIgnoringCase(vocbi.getFacetConfig().getIndexFieldName())),
                    hasJsonPath("$.vocabulary", equalToIgnoringCase(vocbi.getVocabulary().getPluginInstanceName())),
                    hasJsonPath("$._links.vocabulary.href",
                                is(REST_SERVER_URL + String.format("submission/vocabularies/%s/",
                                                                   vocbi.getVocabulary().getPluginInstanceName()))),
                    hasJsonPath("$._links.items.href",
                                is(REST_SERVER_URL + String.format("discover/browses/%s/items",
                                                                   vocbi.getVocabulary().getPluginInstanceName()))),
                    hasJsonPath("$._links.entries.href",
                                is(REST_SERVER_URL + String.format("discover/browses/%s/entries",
                                                                   vocbi.getVocabulary().getPluginInstanceName()))),
                    hasJsonPath("$._links.self.href",
                                is(REST_SERVER_URL + String.format("discover/browses/%s",
                                                                   vocbi.getVocabulary().getPluginInstanceName())))
            );
        } else {
            return allOf(
                    hasJsonPath("$.metadata", contains(getIndexMetadata(bi))),
                    hasJsonPath("$.browseType", equalToIgnoringCase(getBrowseType(bi))),
                    hasJsonPath("$.type", equalToIgnoringCase("browse")),
                    hasJsonPath("$.dataType", equalToIgnoringCase(bi.getDataType())),
                    hasJsonPath("$.order", equalToIgnoringCase(bi.getDefaultOrder())),
                    hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
                    hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/" + bi.getName())),
                    hasJsonPath("$._links.entries.href", is(REST_SERVER_URL + "discover/browses/"
                                                                    + bi.getName() + "/entries")),
                    hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/"
                                                                  + bi.getName() + "/items"))
            );
        }
    }

    private static String[] getIndexMetadata(BrowseIndex bi) {
        if (bi instanceof DSpaceControlledVocabularyIndex) {
            DSpaceControlledVocabularyIndex vocObj = (DSpaceControlledVocabularyIndex) bi;
            return vocObj.getMetadataFields().toArray(new String[0]);
        } else if (bi.isMetadataIndex()) {
            return bi.getMetadata().split(",");
        } else {
            return new String[]{bi.getSortOption().getMetadata()};
        }
    }

    private static String getBrowseType(BrowseIndex bi) {
        if (bi.isMetadataIndex()) {
            return BROWSE_TYPE_VALUE_LIST;
        } else {
            return BROWSE_TYPE_FLAT;
        }
    }


    public static Matcher<? super Object> subjectBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.subject.*")),
            hasJsonPath("$.browseType", equalToIgnoringCase(BROWSE_TYPE_VALUE_LIST)),
            hasJsonPath("$.type", equalToIgnoringCase("browse")),
            hasJsonPath("$.uniqueType", equalToIgnoringCase("discover.browse")),
            hasJsonPath("$.dataType", equalToIgnoringCase("text")),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/subject")),
            hasJsonPath("$._links.entries.href", is(REST_SERVER_URL + "discover/browses/subject/entries")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/subject/items"))
        );
    }

    public static Matcher<? super Object> titleBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.title")),
            hasJsonPath("$.browseType", equalToIgnoringCase(BROWSE_TYPE_FLAT)),
            hasJsonPath("$.type", equalToIgnoringCase("browse")),
            hasJsonPath("$.uniqueType", equalToIgnoringCase("discover.browse")),
            hasJsonPath("$.dataType", equalToIgnoringCase("title")),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/title")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/title/items"))
        );
    }

    public static Matcher<? super Object> rsoTitleBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.title")),
            hasJsonPath("$.browseType", equalToIgnoringCase(BROWSE_TYPE_FLAT)),
            hasJsonPath("$.type", equalToIgnoringCase("browse")),
            hasJsonPath("$.uniqueType", equalToIgnoringCase("discover.browse")),
            hasJsonPath("$.dataType", equalToIgnoringCase("title")),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/rsoTitle")),
            hasJsonPath("$._links.entries.href", is(REST_SERVER_URL + "discover/browses/rsoTitle/entries")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/rsoTitle/items"))
        );
    }

    public static Matcher<? super Object> contributorBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.contributor.*", "dc.creator")),
            hasJsonPath("$.browseType", equalToIgnoringCase(BROWSE_TYPE_VALUE_LIST)),
            hasJsonPath("$.type", equalToIgnoringCase("browse")),
            hasJsonPath("$.uniqueType", equalToIgnoringCase("discover.browse")),
            hasJsonPath("$.dataType", equalToIgnoringCase("text")),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/author")),
            hasJsonPath("$._links.entries.href", is(REST_SERVER_URL + "discover/browses/author/entries")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/author/items"))
        );
    }

    public static Matcher<? super Object> dateIssuedBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.date.issued")),
            hasJsonPath("$.browseType", equalToIgnoringCase(BROWSE_TYPE_FLAT)),
            hasJsonPath("$.type", equalToIgnoringCase("browse")),
            hasJsonPath("$.uniqueType", equalToIgnoringCase("discover.browse")),
            hasJsonPath("$.dataType", equalToIgnoringCase("date")),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/dateissued")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/dateissued/items"))
        );
    }

    public static Matcher<? super Object> rodeptBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("cris.virtual.department")),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/rodept")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/rodept/items"))
        );
    }

    public static Matcher<? super Object> typeBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.type")),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/type")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/type/items"))
        );
    }

    public static Matcher<? super Object> rpnameBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.title")),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/rpname")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/rpname/items"))
        );
    }

    public static Matcher<? super Object> rpdeptBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("person.affiliation.name")),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/rpdept")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/rpdept/items"))
        );
    }

    public static Matcher<? super Object> ounameBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.title")),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/ouname")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/ouname/items"))
        );
    }

    public static Matcher<? super Object> pjtitleBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.title")),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/pjtitle")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/pjtitle/items"))
        );
    }

    public static Matcher<? super Object> eqtitleBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.title")),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/eqtitle")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/eqtitle/items"))
        );
    }

    public static Matcher<? super Object> typesBrowseIndex() {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.type")),
            hasJsonPath("$.browseType", is("hierarchicalBrowse")),
            hasJsonPath("$.facetType", is("itemtype")),
            hasJsonPath("$.type", is("browse")),
            hasJsonPath("$.uniqueType", is("discover.browse")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/types")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/types/items"))
                    );
    }

    public static Matcher<? super Object> hierarchicalBrowseIndex(
        String vocabulary, String facetType, String metadata
    ) {
        return allOf(
            hasJsonPath("$.metadata", contains(metadata)),
            hasJsonPath("$.browseType", equalToIgnoringCase(BROWSE_TYPE_HIERARCHICAL)),
            hasJsonPath("$.type", equalToIgnoringCase("browse")),
            hasJsonPath("$.uniqueType", equalToIgnoringCase("discover.browse")),
            hasJsonPath("$.facetType", equalToIgnoringCase(facetType)),
            hasJsonPath("$.vocabulary", equalToIgnoringCase(vocabulary)),
            hasJsonPath("$._links.vocabulary.href",
                        is(REST_SERVER_URL + String.format("submission/vocabularies/%s/", vocabulary))),
            hasJsonPath("$._links.items.href",
                        is(REST_SERVER_URL + String.format("discover/browses/%s/items", vocabulary))),
            hasJsonPath("$._links.entries.href",
                        is(REST_SERVER_URL + String.format("discover/browses/%s/entries", vocabulary))),
            hasJsonPath("$._links.self.href",
                        is(REST_SERVER_URL + String.format("discover/browses/%s", vocabulary)))
        );
    }
}
