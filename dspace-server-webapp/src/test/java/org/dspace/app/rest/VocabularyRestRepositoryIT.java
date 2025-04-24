/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.MediaType;

import org.dspace.app.rest.matcher.VocabularyMatcher;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.repository.SubmissionFormRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authority.AuthorityValueServiceImpl;
import org.dspace.authority.PersonAuthorityValue;
import org.dspace.authority.factory.AuthorityServiceFactory;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.authority.DCInputAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.edit.EditItem;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class handles all Authority related IT. It alters some config to run the tests, but it gets cleared again
 * after every test
 */
public class VocabularyRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private MetadataAuthorityService metadataAuthorityService;

    @Autowired
    private SubmissionFormRestRepository submissionFormRestRepository;

    @Autowired
    private PluginService pluginService;

    @Autowired
    private ChoiceAuthorityService cas;

    @Before
    public void setup() throws Exception {
        super.setUp();
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                new String[] {
                        "org.dspace.content.authority.SolrAuthority = SolrAuthorAuthority",
                        "org.dspace.content.authority.SHERPARoMEOPublisher = SRPublisher",
                        "org.dspace.content.authority.SHERPARoMEOJournalTitle = SRJournalTitle"
                });

        configurationService.setProperty("solr.authority.server",
                "${solr.server}/authority");
        configurationService.setProperty("choices.plugin.dc.contributor.author",
                "SolrAuthorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author",
                "authorLookup");
        configurationService.setProperty("authority.controlled.dc.contributor.author",
                "true");
        configurationService.setProperty("authority.author.indexer.field.1",
                "dc.contributor.author");

        configurationService.setProperty("choices.plugin.dc.publisher",
                "SRPublisher");
        configurationService.setProperty("choices.presentation.dc.publisher",
                "lookup");
        configurationService.setProperty("authority.controlled.dc.publisher",
                "true");

        configurationService.setProperty("choices.plugin.dc.relation.ispartof",
                "SRJournalTitle");
        configurationService.setProperty("choices.presentation.dc.relation.ispartof",
                "lookup");
        configurationService.setProperty("authority.controlled.dc.relation.ispartof",
                "true");

        // These clears have to happen so that the config is actually reloaded in those classes. This is needed for
        // the properties that we're altering above and this is only used within the tests
        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();

        // The following line is needed to call init() method in the ChoiceAuthorityServiceImpl class, without it
        // the `submissionConfigService` will be null what will cause a NPE in the clearCache() method
        // https://github.com/DSpace/DSpace/issues/9292
        cas.getChoiceAuthoritiesNames();
        cas.clearCache();

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("A parent community for all our test")
                .build();
        context.restoreAuthSystemState();
        PersonAuthorityValue person1 = new PersonAuthorityValue();
        person1.setId(String.valueOf(UUID.randomUUID()));
        person1.setLastName("Shirasaka");
        person1.setFirstName("Seiko");
        person1.setValue("Shirasaka, Seiko");
        person1.setField("dc_contributor_author");
        person1.setLastModified(new Date());
        person1.setCreationDate(new Date());
        AuthorityServiceFactory.getInstance().getAuthorityIndexingService().indexContent(person1);

        PersonAuthorityValue person2 = new PersonAuthorityValue();
        person2.setId(String.valueOf(UUID.randomUUID()));
        person2.setLastName("Miller");
        person2.setFirstName("Tyler E");
        person2.setValue("Miller, Tyler E");
        person2.setField("dc_contributor_author");
        person2.setLastModified(new Date());
        person2.setCreationDate(new Date());
        AuthorityServiceFactory.getInstance().getAuthorityIndexingService().indexContent(person2);

        AuthorityServiceFactory.getInstance().getAuthorityIndexingService().commit();
    }

    @Override
    @After
    // We need to cleanup the authorities cache once than the configuration has been restored
    public void destroy() throws Exception {
        super.destroy();
        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();
        cas.clearCache();
    }

    @Test
    public void findAllTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies"))
                .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.vocabularies", Matchers.containsInAnyOrder(
                     VocabularyMatcher.matchProperties("srsc", "srsc", false, true),
                     VocabularyMatcher.matchProperties("common_iso_languages", "common_iso_languages", true, false),
                     VocabularyMatcher.matchProperties("SRPublisher", "SRPublisher", false, false),
                     VocabularyMatcher.matchProperties("patent_types", "patent_types", true, false),
                     VocabularyMatcher.matchProperties("types", "types", false, true),
                     VocabularyMatcher.matchProperties("gender", "gender", true, false),
                     VocabularyMatcher.matchProperties("SolrAuthorAuthority", "SolrAuthorAuthority", false, false),
                     VocabularyMatcher.matchProperties("SRJournalTitle", "SRJournalTitle", false, false),
                     VocabularyMatcher.matchProperties("common_types", "common_types", true, false),
                     VocabularyMatcher.matchProperties("publication-coar-types", "publication-coar-types", false, true),
                     VocabularyMatcher.matchProperties("currency", "currency", true, false)
                 )))
                .andExpect(jsonPath("$._links.self.href",
                    Matchers.containsString("api/submission/vocabularies")))
                .andExpect(jsonPath("$.page.totalElements", is(11)));
    }

    @Test
    public void findOneSRSC_Test() throws Exception {
        getClient().perform(get("/api/submission/vocabularies/srsc"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", is(
                            VocabularyMatcher.matchProperties("srsc", "srsc", false, true)
                        )));
    }

    @Test
    public void findOneCommonTypesTest() throws Exception {
        getClient().perform(get("/api/submission/vocabularies/common_types"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", is(
                            VocabularyMatcher.matchProperties("common_types", "common_types", true, false)
                        )));
    }

    @Test
    public void correctSrscQueryTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(
            get("/api/submission/vocabularies/srsc/entries")
                .param("filter", "Research")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                        VocabularyMatcher.matchVocabularyEntry("Research Subject Categories",
                          "", "vocabularyEntry"),
                        VocabularyMatcher.matchVocabularyEntry("Family research",
                          "SOCIAL SCIENCES::Social sciences::Social work::Family research",
                          "vocabularyEntry"))))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(26)))
                .andExpect(jsonPath("$.page.totalPages", Matchers.is(13)))
                .andExpect(jsonPath("$.page.size", Matchers.is(2)));
    }

    @Test
    public void notScrollableVocabularyRequiredQueryTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/srsc/entries"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void noResultsSrscQueryTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(
            get("/api/submission/vocabularies/srsc/entries")
                .param("metadata", "dc.subject")
                .param("filter", "Research2")
                .param("size", "1000"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
    }

    @Test
    public void vocabularyEntriesCommonTypesWithPaginationTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token)
                .perform(get("/api/submission/vocabularies/common_types/entries").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                        VocabularyMatcher.matchVocabularyEntry("Animation", "Animation", "vocabularyEntry"),
                        VocabularyMatcher.matchVocabularyEntry("Article", "Article", "vocabularyEntry")
                        )))
                .andExpect(jsonPath("$._embedded.entries[*].authority").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(22)))
                .andExpect(jsonPath("$.page.totalPages", Matchers.is(11)))
                .andExpect(jsonPath("$.page.size", Matchers.is(2)));

        //second page
        getClient(token).perform(get("/api/submission/vocabularies/common_types/entries")
                .param("size", "2")
                .param("page", "1"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                         VocabularyMatcher.matchVocabularyEntry("Book", "Book", "vocabularyEntry"),
                         VocabularyMatcher.matchVocabularyEntry("Book chapter", "Book chapter", "vocabularyEntry")
                         )))
                 .andExpect(jsonPath("$._embedded.entries[*].authority").doesNotExist())
                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(22)))
                 .andExpect(jsonPath("$.page.totalPages", Matchers.is(11)))
                 .andExpect(jsonPath("$.page.size", Matchers.is(2)));
    }

    @Test
    public void vocabularyEntriesCommon_typesWithQueryTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/common_types/entries")
                .param("filter", "Book")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                        VocabularyMatcher.matchVocabularyEntry("Book", "Book", "vocabularyEntry"),
                        VocabularyMatcher.matchVocabularyEntry("Book chapter", "Book chapter", "vocabularyEntry")
                        )))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)))
                .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
                .andExpect(jsonPath("$.page.size", Matchers.is(2)));
    }

    @Test
    public void vocabularyEntriesDCInputAuthorityLocalesTest() throws Exception {
        String[] supportedLanguage = {"it","uk"};
        configurationService.setProperty("default.locale","it");
        configurationService.setProperty("webui.supported.locales",supportedLanguage);
        // These clears have to happen so that the config is actually reloaded in those classes. This is needed for
        // the properties that we're altering above and this is only used within the tests
        submissionFormRestRepository.reload();
        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();
        cas.clearCache();

        Locale uk = new Locale("uk");
        Locale it = new Locale("it");

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token)
                .perform(get("/api/submission/vocabularies/common_iso_languages/entries")
                        .param("size", "2")
                        .locale(it))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                        VocabularyMatcher.matchVocabularyEntry("N/A", "", "vocabularyEntry"),
                        VocabularyMatcher.matchVocabularyEntry("Inglese (USA)", "en_US", "vocabularyEntry")
                        )))
                .andExpect(jsonPath("$._embedded.entries[*].authority").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)))
                .andExpect(jsonPath("$.page.totalPages", Matchers.is(6)))
                .andExpect(jsonPath("$.page.size", Matchers.is(2)));

        getClient(token)
                .perform(get("/api/submission/vocabularies/common_iso_languages/entries")
                        .param("size", "2")
                        .locale(uk))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                        VocabularyMatcher.matchVocabularyEntry("N/A", "", "vocabularyEntry"),
                        VocabularyMatcher.matchVocabularyEntry("Американська (USA)", "en_US", "vocabularyEntry")
                        )))
                .andExpect(jsonPath("$._embedded.entries[*].authority").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(12)))
                .andExpect(jsonPath("$.page.totalPages", Matchers.is(6)))
                .andExpect(jsonPath("$.page.size", Matchers.is(2)));

        configurationService.setProperty("default.locale","en");
        configurationService.setProperty("webui.supported.locales",null);
        submissionFormRestRepository.reload();
        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();
        cas.clearCache();
    }


    @Test
    public void correctSolrQueryTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(
                get("/api/submission/vocabularies/SolrAuthorAuthority/entries")
                        .param("filter", "Shirasaka")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.entries", Matchers.contains(
                    VocabularyMatcher.matchVocabularyEntry("Shirasaka, Seiko", "Shirasaka, Seiko", "vocabularyEntry")
                    )))
                .andExpect(jsonPath("$._embedded.entries[0].authority").isNotEmpty())
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));

        // test also an entry provided by ORCID
        getClient(token).perform(
                get("/api/submission/vocabularies/SolrAuthorAuthority/entries")
                        .param("filter", "Bollini")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.entries", Matchers.contains(
                    VocabularyMatcher.matchVocabularyEntry("Bollini, Andrea", "Bollini, Andrea", "vocabularyEntry")
                    )))
                .andExpect(jsonPath("$._embedded.entries[0].authority",
                            Matchers.is(AuthorityValueServiceImpl.GENERATE + "orcid"
                                    + AuthorityValueServiceImpl.SPLIT + "0000-0002-9029-1854")))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));
    }

    @Test
    public void noResultsSolrQueryTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(
                get("/api/submission/vocabularies/SolrAuthorAuthority/entries")
                        .param("filter", "Smith")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
    }

    @Test
    public void sherpaJournalTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(
                get("/api/submission/vocabularies/SRJournalTitle/entries")
                        .param("filter", "Lancet")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.entries", Matchers.contains(
                    VocabularyMatcher.matchVocabularyEntry("The Lancet", "The Lancet", "vocabularyEntry")
                    )))
                .andExpect(jsonPath("$._embedded.entries[0].authority",
                        Matchers.is("0140-6736")))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));
    }

    @Test
    public void sherpaPublisherTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(
                get("/api/submission/vocabularies/SRPublisher/entries")
                        .param("filter", "PLOS")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.entries", Matchers.contains(
                    VocabularyMatcher.matchVocabularyEntry("Public Library of Science", "Public Library of Science",
                            "vocabularyEntry")
                    )))
                .andExpect(jsonPath("$._embedded.entries[0].authority",
                        Matchers.is("112")))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));
    }

    @Test
    public void findByMetadataAndCollectionTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test collection")
                                                 .build();
        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/search/byMetadataAndCollection")
                        .param("metadata", "dc.type")
                        .param("collection", collection.getID().toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", is(
                            VocabularyMatcher.matchProperties("common_types", "common_types", true, false)
                        )));
    }

    @Test
    public void findByLabelAndValueTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test collection")
                                                 .build();
        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/common_iso_countries/entries")
                        .param("size", "10")
                        .param("filter", "al")
                        .param("metadata", "dc.language.iso")
                        .param("collection", collection.getID().toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                                VocabularyMatcher.matchVocabularyEntry("Italia", "IT", "vocabularyEntry"),
                                VocabularyMatcher.matchVocabularyEntry("Albania", "AL", "vocabularyEntry"),
                                VocabularyMatcher.matchVocabularyEntry("Algeria", "DZ", "vocabularyEntry"),
                                VocabularyMatcher.matchVocabularyEntry("Australia", "AU", "vocabularyEntry")
                        )))
                        .andExpect(jsonPath("$._embedded.entries", Matchers.not(Matchers.containsInAnyOrder(
                                VocabularyMatcher.matchVocabularyEntry("Botswana", "BW", "vocabularyEntry")
                        ))))
                        .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)))
                        .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
                        .andExpect(jsonPath("$.page.size", Matchers.is(10)));
    }

    @Test
    public void findByMetadataAndCollectionWithMetadataWithoutVocabularyTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Test collection")
            .build();
        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/search/byMetadataAndCollection")
                .param("metadata", "dc.title")
                .param("collection", collection.getID().toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    public void findByMetadataAndCollectionUnprocessableEntityTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test collection")
                                                 .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/search/byMetadataAndCollection")
                        .param("metadata", "dc.not.exist")
                        .param("collection", collection.getID().toString()))
                        .andExpect(status().isUnprocessableEntity());

        getClient(token).perform(get("/api/submission/vocabularies/search/byMetadataAndCollection")
                .param("metadata", "dc.type")
                .param("collection", UUID.randomUUID().toString()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void findByMetadataAndCollectionBadRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Test collection")
                                                 .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        //missing metadata
        getClient(token).perform(get("/api/submission/vocabularies/search/byMetadataAndCollection")
                        .param("collection", collection.getID().toString()))
                        .andExpect(status().isBadRequest());

        //missing collection
        getClient(token).perform(get("/api/submission/vocabularies/search/byMetadataAndCollection")
                .param("metadata", "dc.type"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void linkedEntitiesWithExactParamTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/common_types/entries")
                .param("filter", "Animation")
                .param("exact", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.entries", Matchers.contains(
                    VocabularyMatcher.matchVocabularyEntry("Animation", "Animation", "vocabularyEntry")
                    )))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));
    }

    @Test
    public void linkedEntitiesWithFilterAndEntryIdTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/srsc/entries")
                        .param("filter", "Research")
                        .param("entryID", "VR131402"))
                        .andExpect(status().isBadRequest());
    }

    @Test
    public void controlledVocabularyWithHierarchyStoreSetTrueTest() throws Exception {
        context.turnOffAuthorisationSystem();
        try {
            configurationService.setProperty("authority.controlled.dc.type", "true");
            metadataAuthorityService.clearCache();

            String vocabularyName = "publication-coar-types";
            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Root Community")
                                              .build();

            Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                              .withEntityType("Publication")
                                              .withName("Collection 1")
                                              .build();

            Item itemA = ItemBuilder.createItem(context, col)
                                    .withTitle("Test Item A")
                                    .withIssueDate("2023-04-04")
                                    .withType("Resource Types::text::book::book part", vocabularyName + ":c_3248")
                                    .build();

            EditItem editItem = new EditItem(context, itemA);

            context.restoreAuthSystemState();

            String tokenAdmin = getAuthToken(admin.getEmail(), password);

            getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.metadata", Matchers.allOf(
                                         hasJsonPath("$['dc.title'][0].value", is("Test Item A")),
                                         hasJsonPath(
                                                 "$['dc.type'][0].value", is("Resource Types::text::book::book part")),
                                         hasJsonPath("$['dc.type'][0].authority", is(vocabularyName + ":c_3248")),
                                         hasJsonPath("$['dc.type'][0].confidence", is(600))
                                         )));

            AtomicReference<String> selectedLeafValue = new AtomicReference<>();
            AtomicReference<String> selectedLeafauthority = new AtomicReference<>();

            getClient(tokenAdmin).perform(get("/api/submission/vocabularies/" + vocabularyName + "/entries")
                                 .param("metadata", "dc.type")
                                 .param("entryID", vocabularyName + ":c_b239"))
                                 .andExpect(status().isOk())
                                 .andDo(result -> selectedLeafValue.set(read(result.getResponse().getContentAsString(),
                                                                        "$._embedded.entries[0].value")))
                                 .andDo(result -> selectedLeafauthority.set(
                                         read(result.getResponse().getContentAsString(),
                                                 "$._embedded.entries[0].authority")));

            List<Operation> operations = new ArrayList<Operation>();
            Map<String, String> value = new HashMap<String, String>();
            value.put("value", selectedLeafValue.get());
            value.put("authority", selectedLeafauthority.get());
            value.put("confidence", "600");
            operations.add(new ReplaceOperation("/sections/controlled-vocabulary-test/dc.type/0", value));

            String patchBody = getPatchContent(operations);
            getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":MODE-VOC")
                                 .content(patchBody)
                                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                                 .andExpect(status().isOk());

            getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.metadata", Matchers.allOf(
                                       hasJsonPath("$['dc.title'][0].value", is("Test Item A")),
                                       hasJsonPath("$['dc.type'][0].value", is("text::journal::editorial")),
                                       hasJsonPath("$['dc.type'][0].authority", is(vocabularyName + ":c_b239")),
                                         hasJsonPath("$['dc.type'][0].confidence", is(600))
                                       )));
        } finally {
            configurationService.setProperty("authority.controlled.dc.type", "false");
            metadataAuthorityService.clearCache();
        }
    }

    @Test
    public void controlledVocabularyWithHierarchyStoreSetFalseTest() throws Exception {
        context.turnOffAuthorisationSystem();
        try {
            configurationService.setProperty("authority.controlled.dc.type", "true");
            metadataAuthorityService.clearCache();

            String vocabularyName = "publication-coar-types";
            configurationService.setProperty("vocabulary.plugin." + vocabularyName + ".hierarchy.store", false);
            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Root Community")
                                              .build();

            Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                              .withEntityType("Publication")
                                              .withName("Collection 1")
                                              .build();

            Item itemA = ItemBuilder.createItem(context, col)
                                    .withTitle("Test Item A")
                                    .withIssueDate("2023-04-04")
                                    .withType("Resource Types::text::book::book part", vocabularyName + ":c_3248")
                                    .build();

            EditItem editItem = new EditItem(context, itemA);

            context.restoreAuthSystemState();

            String tokenAdmin = getAuthToken(admin.getEmail(), password);

            getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.metadata", Matchers.allOf(
                                         hasJsonPath("$['dc.title'][0].value", is("Test Item A")),
                                         hasJsonPath("$['dc.type'][0].value",
                                                 is("Resource Types::text::book::book part")),
                                         hasJsonPath("$['dc.type'][0].authority", is(vocabularyName + ":c_3248")),
                                         hasJsonPath("$['dc.type'][0].confidence", is(600))
                                         )));

            AtomicReference<String> selectedLeafValue = new AtomicReference<>();
            AtomicReference<String> selectedLeafauthority = new AtomicReference<>();

            getClient(tokenAdmin).perform(get("/api/submission/vocabularies/" + vocabularyName + "/entries")
                                 .param("metadata", "dc.type")
                                 .param("entryID", vocabularyName + ":c_b239"))
                                 .andExpect(status().isOk())
                                 .andDo(result -> selectedLeafValue.set(read(result.getResponse().getContentAsString(),
                                                                        "$._embedded.entries[0].value")))
                                 .andDo(result -> selectedLeafauthority.set(
                                         read(result.getResponse().getContentAsString(),
                                                                            "$._embedded.entries[0].authority")));

            List<Operation> operations = new ArrayList<Operation>();
            Map<String, String> value = new HashMap<String, String>();
            value.put("value", selectedLeafValue.get());
            value.put("authority", selectedLeafauthority.get());
            value.put("confidence", "600");
            operations.add(new ReplaceOperation("/sections/controlled-vocabulary-test/dc.type/0", value));

            String patchBody = getPatchContent(operations);
            getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":MODE-VOC")
                                 .content(patchBody)
                                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                                 .andExpect(status().isOk());

            getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.metadata", Matchers.allOf(
                                         hasJsonPath("$['dc.title'][0].value", is("Test Item A")),
                                         hasJsonPath("$['dc.type'][0].value", is("editorial")),
                                         hasJsonPath("$['dc.type'][0].authority", is(vocabularyName + ":c_b239")),
                                         hasJsonPath("$['dc.type'][0].confidence", is(600))
                                         )));
        } finally {
            configurationService.setProperty("authority.controlled.dc.type", "false");
            metadataAuthorityService.clearCache();
        }
    }

    @Test
    public void controlledVocabularyWithHierarchySuggestSetTrueTest() throws Exception {
        String vocabularyName = "publication-coar-types";

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        // default configuration
        getClient(tokenAdmin).perform(get("/api/submission/vocabularies/" + vocabularyName + "/entries")
                             .param("metadata", "dc.type")
                             .param("entryID", vocabularyName + ":c_b239"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded.entries[0]", Matchers.allOf(
                                     hasJsonPath("$.authority", is(vocabularyName + ":c_b239")),
                                     // the display value without suggestions
                                     hasJsonPath("$.display", is("editorial")),
                                     hasJsonPath("$.value", is("text::journal::editorial"))
                                     )));

        configurationService.setProperty("vocabulary.plugin." + vocabularyName + ".hierarchy.suggest", true);

        getClient(tokenAdmin).perform(get("/api/submission/vocabularies/" + vocabularyName + "/entries")
                .param("metadata", "dc.type")
                .param("entryID", vocabularyName + ":c_b239"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.entries[0]", Matchers.allOf(
                        hasJsonPath("$.authority", is(vocabularyName + ":c_b239")),
                        // now the display value with suggestions
                        hasJsonPath("$.display", is("text::journal::editorial")),
                        hasJsonPath("$.value", is("text::journal::editorial"))
                        )));
    }

}
