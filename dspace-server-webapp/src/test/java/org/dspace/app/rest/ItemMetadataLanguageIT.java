/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataLanguageDoesNotExist;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests validating multilingual metadata filtering on the
 * {@code GET /api/core/items/{uuid}} REST endpoint.
 *
 * <p>The REST layer filters item metadata via
 * {@code MetadataSecurityServiceImpl#getPermissionAndLangFilteredMetadataFields},
 * which applies a language-family match based on the request {@code Accept-Language}
 * header.  These tests exercise that logic end-to-end.</p>
 *
 * @author DSpace Developers
 */
public class ItemMetadataLanguageIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    /**
     * Items with dc.title values in "en", "en_US", "en_UK" and null-language must all be visible
     * when the client requests the "en" locale.  The "de" value must NOT appear because an
     * English match already exists (no fallback triggered).
     */
    @Test
    public void testSimplePage_languageFamilyFiltering() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection").build();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitleForLanguage("Title en", "en")
                .withTitleForLanguage("Title en_US", "en_US")
                .withTitleForLanguage("Title en_UK", "en_UK")
                .withTitleForLanguage("Title de", "de")
                .withTitle("Title null lang")
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/items/" + item.getID())
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Title en")))
                .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Title en_US")))
                .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Title en_UK")))
                .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Title null lang")))
                .andExpect(jsonPath("$.metadata", matchMetadataLanguageDoesNotExist("dc.title", "de")));
    }

    /**
     * A dc.title value with a null text_lang must always be returned, regardless of the
     * request locale.
     */
    @Test
    public void testSimplePage_nullLanguageAlwaysVisible() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection").build();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Title null lang")
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/items/" + item.getID())
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Title null lang")));
    }

    /**
     * A dc.title value with {@code text_lang="*"} is a wildcard sentinel and must always be
     * returned regardless of the request locale.
     */
    @Test
    public void testSimplePage_starLanguageAlwaysVisible() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection").build();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitleForLanguage("Title star lang", "*")
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/items/" + item.getID())
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Title star lang")));
    }

    /**
     * When a dc.title value exists only in German and the client requests English,
     * the fallback mechanism must kick in and return the German value (since no English match
     * exists).
     */
    @Test
    public void testSimplePage_fallback_noEnValues() throws Exception {
        configurationService.setProperty("webui.supported.locales", "en, de");

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection").build();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitleForLanguage("Titel de", "de")
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/items/" + item.getID())
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Titel de")));
    }

    /**
     * When both "en_US" and "de" dc.title values exist and the client requests "en",
     * only the English family value must appear — the German value must be excluded
     * because a language match was found (no fallback).
     */
    @Test
    public void testSimplePage_noFallback_enValuesExist() throws Exception {
        configurationService.setProperty("webui.supported.locales", "en, de");

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection").build();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitleForLanguage("Title en_US", "en_US")
                .withTitleForLanguage("Title de", "de")
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/items/" + item.getID())
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "Title en_US")))
                .andExpect(jsonPath("$.metadata", matchMetadataLanguageDoesNotExist("dc.title", "de")));
    }

    /**
     * A dc.title value in Japanese ("ja"), which is NOT listed in the supported locales,
     * must still be returned when no supported-locale match exists for the requested "en"
     * locale (fallback: include all field values).
     */
    @Test
    public void testSimplePage_unsupportedLanguageAlwaysVisible() throws Exception {
        configurationService.setProperty("webui.supported.locales", "en, de");

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection").build();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitleForLanguage("タイトル ja", "ja")
                .build();
        context.restoreAuthSystemState();

        // No "en" value exists → fallback kicks in → all field values returned, including "ja"
        getClient().perform(get("/api/core/items/" + item.getID())
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata", matchMetadata("dc.title", "タイトル ja")));
    }

    /**
     * There is no dedicated export REST endpoint that accepts an all-languages flag in the
     * current DSpace REST API.  The export pipeline is exercised via the {@code metadata-export}
     * script (see MetadataExportIT) and the XLS crosswalk (see XlsCrosswalkIT), both of which
     * bypass the language filter entirely.
     *
     * <p>This test is intentionally left as a no-op placeholder to document the design decision.
     * If a future REST export endpoint is added, this method should be updated.</p>
     */
    @Test
    public void testExportEndpoint_allLanguagesReturned() throws Exception {
        // No REST export endpoint with per-language control exists at this time.
        // Export coverage is provided by MetadataExportIT and XlsCrosswalkIT.
    }
}
