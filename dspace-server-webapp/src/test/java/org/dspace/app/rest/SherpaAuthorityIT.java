/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.matcher.ItemAuthorityMatcher.matchItemAuthorityProperties;
import static org.dspace.app.rest.matcher.ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations;
import static org.dspace.authority.service.AuthorityValueService.GENERATE;
import static org.dspace.authority.service.AuthorityValueService.REFERENCE;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.authority.SherpaAuthority;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link SherpaAuthority}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 *
 */
public class SherpaAuthorityIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    private Collection collection;

    @Before
    public void setup() {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Test collection")
            .build();

        context.restoreAuthSystemState();

    }

    @Test
    public void testWithoutLocalItems() throws Exception {

        configurationService.setProperty("cris.SherpaAuthority.local-item-choices-enabled", true);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/SherpaAuthority/entries")
            .param("filter", "test journal"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                sherpaEntry("The Lancet", REFERENCE, "0140-6736", "Elsevier"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(20)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));

    }

    @Test
    public void testWithLocalItems() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstJournal = buildJournal("Test Journal 1");
        Item secondJournal = buildJournal("Test Journal 2");
        buildJournal("My Journal");
        Item thirdJournal = buildJournal("Test Journal 3");

        context.restoreAuthSystemState();

        configurationService.setProperty("cris.SherpaAuthority.local-item-choices-enabled", true);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/SherpaAuthority/entries")
            .param("filter", "test journal"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                localEntry("Test Journal 1", firstJournal),
                localEntry("Test Journal 2", secondJournal),
                localEntry("Test Journal 3", thirdJournal),
                sherpaEntry("The Lancet", REFERENCE, "0140-6736", "Elsevier"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(20)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)));

    }

    @Test
    public void testWithLocalItemsButLocalItemChoicesDisabled() throws Exception {

        context.turnOffAuthorisationSystem();

        buildJournal("Test Journal 1");
        buildJournal("Test Journal 2");
        buildJournal("My Journal");
        buildJournal("Test Journal 3");

        context.restoreAuthSystemState();

        configurationService.setProperty("cris.SherpaAuthority.local-item-choices-enabled", false);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/SherpaAuthority/entries")
            .param("filter", "test journal"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                sherpaEntry("The Lancet", REFERENCE, "0140-6736", "Elsevier"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(20)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));

    }

    @Test
    public void testPaginationWithLocalItemChoicesEnabled() throws Exception {

        configurationService.setProperty("cris.SherpaAuthority.local-item-choices-enabled", true);

        context.turnOffAuthorisationSystem();

        Item firstJournal = buildJournal("authority_test 1");
        Item secondJournal = buildJournal("authority_test 2");
        buildJournal("My Journal");
        Item thirdJournal = buildJournal("authority_test 3");

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/SherpaAuthority/entries")
            .param("filter", "authority_test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                localEntry("authority_test 1", firstJournal),
                localEntry("authority_test 2", secondJournal),
                localEntry("authority_test 3", thirdJournal),
                sherpaEntry("The Lancet", REFERENCE, "0140-6736", "Elsevier"),
                sherpaEntry("Nature Synthesis", REFERENCE, "2731-0582", "Nature Research"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(20)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(5)));

        getClient(token).perform(get("/api/submission/vocabularies/SherpaAuthority/entries")
            .param("filter", "authority_test")
            .param("page", "0")
            .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                localEntry("authority_test 1", firstJournal),
                localEntry("authority_test 2", secondJournal))))
            .andExpect(jsonPath("$.page.size", Matchers.is(2)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(3)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(5)));

        getClient(token).perform(get("/api/submission/vocabularies/SherpaAuthority/entries")
            .param("filter", "authority_test")
            .param("page", "1")
            .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                localEntry("authority_test 3", thirdJournal),
                sherpaEntry("The Lancet", REFERENCE, "0140-6736", "Elsevier"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(2)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(3)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(5)));

        getClient(token).perform(get("/api/submission/vocabularies/SherpaAuthority/entries")
            .param("filter", "authority_test")
            .param("page", "2")
            .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                sherpaEntry("Nature Synthesis", REFERENCE, "2731-0582", "Nature Research"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(2)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(3)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(5)));

        getClient(token).perform(get("/api/submission/vocabularies/SherpaAuthority/entries")
            .param("filter", "authority_test")
            .param("page", "0")
            .param("size", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                localEntry("authority_test 1", firstJournal),
                localEntry("authority_test 2", secondJournal),
                localEntry("authority_test 3", thirdJournal))))
            .andExpect(jsonPath("$.page.size", Matchers.is(3)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(2)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(5)));

        getClient(token).perform(get("/api/submission/vocabularies/SherpaAuthority/entries")
            .param("filter", "authority_test")
            .param("page", "1")
            .param("size", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                sherpaEntry("The Lancet", REFERENCE, "0140-6736", "Elsevier"),
                sherpaEntry("Nature Synthesis", REFERENCE, "2731-0582", "Nature Research"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(3)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(2)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(5)));
    }

    @Test
    public void testPaginationWithLocalItemChoicesDisabled() throws Exception {

        configurationService.setProperty("cris.SherpaAuthority.local-item-choices-enabled", false);

        context.turnOffAuthorisationSystem();

        buildJournal("authority_test 1");
        buildJournal("authority_test 2");
        buildJournal("My Journal");
        buildJournal("authority_test 3");

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/SherpaAuthority/entries")
            .param("filter", "authority_test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                sherpaEntry("The Lancet", REFERENCE, "0140-6736", "Elsevier"),
                sherpaEntry("Nature Synthesis", REFERENCE, "2731-0582", "Nature Research"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(20)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));

        getClient(token).perform(get("/api/submission/vocabularies/SherpaAuthority/entries")
            .param("filter", "authority_test")
            .param("page", "0")
            .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                sherpaEntry("The Lancet", REFERENCE, "0140-6736", "Elsevier"),
                sherpaEntry("Nature Synthesis", REFERENCE, "2731-0582", "Nature Research"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(2)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));

        getClient(token).perform(get("/api/submission/vocabularies/SherpaAuthority/entries")
            .param("filter", "authority_test")
            .param("page", "0")
            .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                sherpaEntry("The Lancet", REFERENCE, "0140-6736", "Elsevier"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(2)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));

        getClient(token).perform(get("/api/submission/vocabularies/SherpaAuthority/entries")
            .param("filter", "authority_test")
            .param("page", "1")
            .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                sherpaEntry("Nature Synthesis", REFERENCE, "2731-0582", "Nature Research"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(2)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));

    }

    @Test
    public void testWithWillBeGeneratedAuthority() throws Exception {

        configurationService.setProperty("sherpa.authority.prefix", "will be generated::ISSN");

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/SherpaAuthority/entries")
            .param("filter", "test journal"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", containsInAnyOrder(
                sherpaEntry("The Lancet", GENERATE, "0140-6736", "Elsevier"))))
            .andExpect(jsonPath("$.page.size", Matchers.is(20)))
            .andExpect(jsonPath("$.page.totalPages", Matchers.is(1)))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));

    }

    private Item buildJournal(String title) {
        return ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withEntityType("Journal")
            .build();
    }

    private Matcher<? super Object> localEntry(String title, Item journal) {
        return matchItemAuthorityProperties(journal.getID().toString(), title, title, "vocabularyEntry");
    }

    private Matcher<? super Object> sherpaEntry(String title, String authorityPrefix, String issn, String publisher) {

        String authority = authorityPrefix + "ISSN::" + issn;
        Map<String, String> otherInformation = Map.of("dc_publisher", publisher, "dc_relation_issn", issn,
            "data-dc_publisher", publisher, "data-dc_relation_issn", issn);

        return matchItemAuthorityWithOtherInformations(authority, title, title, "vocabularyEntry", otherInformation);

    }

}
