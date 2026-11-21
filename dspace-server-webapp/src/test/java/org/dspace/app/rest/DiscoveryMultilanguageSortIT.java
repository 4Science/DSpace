/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.CrisConstants;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

/**
 * REST integration tests for the language-aware Discovery sorting.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class DiscoveryMultilanguageSortIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @After
    public void after() {
        // Restore the configuration altered inside the tests (webui.supported.locales).
        DSpaceServicesFactory.getInstance().getConfigurationService().reloadConfig();
    }

    /**
     * Every item has a single title without a language tag. Both the Italian and the English
     * sort fields must fall back to that untagged value, so the ordering is identical in both locales.
     */
    @Test
    public void singleTitleWithoutLanguageUsesUntaggedValueInBothLocales() throws Exception {
        enableItAndEnLocales();
        Collection col = newCollection(newCommunity(), "Publication collection", "Publication");

        context.turnOffAuthorisationSystem();
        Item zebra = ItemBuilder.createItem(context, col).withTitle("Zebra").build();
        Item alpha = ItemBuilder.createItem(context, col).withTitle("Alpha").build();
        Item mango = ItemBuilder.createItem(context, col).withTitle("Mango").build();
        context.restoreAuthSystemState();

        // Italian and English produce the same order: Alpha, Mango, Zebra.
        expectOrder(searchTitleAsc(col, Locale.ITALIAN), alpha, mango, zebra);
        expectOrder(searchTitleAsc(col, Locale.ENGLISH), alpha, mango, zebra);
    }

    /**
     * Two titles, one tagged {@code it} and one tagged {@code en}. The ordering follows the
     * Italian values under the Italian locale and the English values under the English locale.
     */
    @Test
    public void titlesInItalianAndEnglishSortByCurrentLocale() throws Exception {
        enableItAndEnLocales();
        Collection col = newCollection(newCommunity(), "Publication collection", "Publication");

        Item[] items = buildItalianAndEnglishTitledItems(col);
        Item item1 = items[0]; // Bravo [it], Yankee [en]
        Item item2 = items[1]; // Zulu [it], Alpha [en]
        Item item3 = items[2]; // Mike [it], Mike [en]

        // Italian: Bravo < Mike < Zulu
        expectOrder(searchTitleAsc(col, Locale.ITALIAN), item1, item3, item2);
        // English: Alpha < Mike < Yankee
        expectOrder(searchTitleAsc(col, Locale.ENGLISH), item2, item3, item1);
    }

    /**
     * An item has an English tagged title and an Italian title stored without a language tag.
     * Because the Italian value is untagged, the Italian sort field must fall back to the
     * {@code default.locale} (English) value, not to the untagged value.
     */
    @Test
    public void italianTitleWithoutLanguageFallsBackToDefaultLocale() throws Exception {
        enableItAndEnLocales();
        Collection col = newCollection(newCommunity(), "Publication collection", "Publication");

        context.turnOffAuthorisationSystem();
        Item itemA = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Zucca", null)
                                .withTitleForLanguage("Banana", "en")
                                .build();
        Item itemB = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Ananas", "it")
                                .withTitleForLanguage("Cherry", "en")
                                .build();
        Item itemC = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Melone", "it")
                                .withTitleForLanguage("Mela", "en")
                                .build();
        context.restoreAuthSystemState();

        // Italian: Ananas(B) < Banana(A, fallback to en) < Melone(C); the untagged "Zucca" must be ignored.
        expectOrder(searchTitleAsc(col, Locale.ITALIAN), itemB, itemA, itemC);
        // English: Banana(A) < Cherry(B) < Mela(C)
        expectOrder(searchTitleAsc(col, Locale.ENGLISH), itemA, itemB, itemC);
    }

    /**
     * An item has three titles - Italian tagged, English tagged and a German one stored without
     * a language tag. The untagged German value must be ignored for both the Italian and the English sort
     * fields, which use their respective tagged values.
     */
    @Test
    public void untaggedUnsupportedLanguageTitleIsIgnoredWhenTaggedValuesExist() throws Exception {
        enableItAndEnLocales();
        var rootCommunity = newCommunity();
        Collection col = newCollection(rootCommunity, "Publication collection", "Publication");

        context.turnOffAuthorisationSystem();
        Item itemA = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Delta", "it")
                                .withTitleForLanguage("Echo", "en")
                                .withTitleForLanguage("Alfa", null)
                                .build();
        Item itemB = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Bravo", "it")
                                .withTitleForLanguage("Zulu", "en")
                                .build();
        Item itemC = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Foxtrot", "it")
                                .withTitleForLanguage("Charlie", "en")
                                .build();
        context.restoreAuthSystemState();

        // Italian: Bravo(B) < Delta(A) < Foxtrot(C)
        expectOrder(searchTitleAsc(col, Locale.ITALIAN), itemB, itemA, itemC);
        // English: Charlie(C) < Echo(A) < Zulu(B)
        expectOrder(searchTitleAsc(col, Locale.ENGLISH), itemC, itemA, itemB);
    }

    /**
     * The same two-language dataset, sorted descending: the order is reversed within each
     * locale. This also proves the descending sort bean ({@code sortTitleDesc}) is multilanguage.
     */
    @Test
    public void titlesSortedDescendingByCurrentLocale() throws Exception {
        enableItAndEnLocales();
        Collection col = newCollection(newCommunity(), "Publication collection", "Publication");

        Item[] items = buildItalianAndEnglishTitledItems(col);
        Item item1 = items[0]; // Bravo [it], Yankee [en]
        Item item2 = items[1]; // Zulu [it], Alpha [en]
        Item item3 = items[2]; // Mike [it], Mike [en]

        // Italian descending: Zulu > Mike > Bravo
        expectOrder(searchTitle(col, Locale.ITALIAN.getLanguage(), "DESC"), item2, item3, item1);
        // English descending: Yankee > Mike > Alpha
        expectOrder(searchTitle(col, Locale.ENGLISH.getLanguage(), "DESC"), item1, item3, item2);
    }

    /**
     * A request for a locale that is not supported must fall back to the {@code default.locale} (English)
     * ordering, never to a non-existent {@code dc.title.<unsupported>_sort} field.
     */
    @Test
    public void unsupportedAcceptLanguageFallsBackToDefaultLocaleOrder() throws Exception {
        enableItAndEnLocales();
        Collection col = newCollection(newCommunity(), "Publication collection", "Publication");

        Item[] items = buildItalianAndEnglishTitledItems(col);
        Item item1 = items[0]; // Bravo [it], Yankee [en]
        Item item2 = items[1]; // Zulu [it], Alpha [en]
        Item item3 = items[2]; // Mike [it], Mike [en]

        // Accept-Language: fr (unsupported) -> English order Alpha < Mike < Yankee
        expectOrder(searchTitle(col, Locale.FRENCH.getLanguage(), "ASC"), item2, item3, item1);
    }

    /**
     * With no {@code Accept-Language} header the search must use the {@code default.locale} (English) order.
     */
    @Test
    public void missingAcceptLanguageHeaderUsesDefaultLocaleOrder() throws Exception {
        enableItAndEnLocales();
        Collection col = newCollection(newCommunity(), "Publication collection", "Publication");

        Item[] items = buildItalianAndEnglishTitledItems(col);
        Item item1 = items[0]; // Bravo [it], Yankee [en]
        Item item2 = items[1]; // Zulu [it], Alpha [en]
        Item item3 = items[2]; // Mike [it], Mike [en]

        // No header -> English order Alpha < Mike < Yankee
        expectOrder(searchTitle(col, null, "ASC"), item2, item3, item1);
    }

    /**
     * A regional/compound language tag ({@code it-IT}, {@code en-US}) must be normalised to its language
     * ({@code it}, {@code en}) and produce the same order as the plain language.
     */
    @Test
    public void regionalAcceptLanguageIsNormalizedToLanguage() throws Exception {
        enableItAndEnLocales();
        var rootCommunity = newCommunity();
        Collection col = newCollection(rootCommunity, "Publication collection", "Publication");

        Item[] items = buildItalianAndEnglishTitledItems(col);
        Item item1 = items[0]; // Bravo [it], Yankee [en]
        Item item2 = items[1]; // Zulu [it], Alpha [en]
        Item item3 = items[2]; // Mike [it], Mike [en]

        // it-IT -> Italian order Bravo < Mike < Zulu
        expectOrder(searchTitle(col, "it-IT", "ASC"), item1, item3, item2);
        // en-US -> English order Alpha < Mike < Yankee
        expectOrder(searchTitle(col, "en-US", "ASC"), item2, item3, item1);
    }

    /**
     * An item that has a title in a supported non-default locale only ({@code it}) and no English value
     * resolves its English sort value through the last fallback step (first value of any language).
     */
    @Test
    public void onlyItalianTitleResolvesViaFirstValueInEnglishSearch() throws Exception {
        enableItAndEnLocales();
        Collection col = newCollection(newCommunity(), "Publication collection", "Publication");

        context.turnOffAuthorisationSystem();
        Item itemA = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Mela", "it")
                                .build();
        Item itemB = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Banana", "en")
                                .build();
        Item itemC = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Zebra", "it")
                                .withTitleForLanguage("Cherry", "en")
                                .build();
        context.restoreAuthSystemState();

        // English: Banana(B) < Cherry(C) < Mela(A, first-any fallback)
        expectOrder(searchTitle(col, Locale.ENGLISH.getLanguage(), "ASC"), itemB, itemC, itemA);
        // Italian: Banana(B, fallback to en) < Mela(A) < Zebra(C)
        expectOrder(searchTitle(col, Locale.ITALIAN.getLanguage(), "ASC"), itemB, itemA, itemC);
    }

    /**
     * When an item has neither the requested-language value nor the default-locale value, the untagged
     * value must be used - in preference to a tagged value in some other language (first-any step).
     */
    @Test
    public void untaggedValueUsedWhenNoTaggedOrDefaultValue() throws Exception {
        enableItAndEnLocales();
        Collection col = newCollection(newCommunity(), "Publication collection", "Publication");

        context.turnOffAuthorisationSystem();
        Item itemA = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Zzz", "de")
                                .withTitleForLanguage("Alpha", null)
                                .build();
        Item itemB = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Beta", "en")
                                .build();
        Item itemC = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Gamma", "en")
                                .build();
        context.restoreAuthSystemState();

        // English: itemA uses the untagged "Alpha" (not the German "Zzz") -> Alpha < Beta < Gamma
        expectOrder(searchTitle(col, Locale.ENGLISH.getLanguage(), "ASC"), itemA, itemB, itemC);
        // Italian: same resolution (A untagged, B/C via default locale) -> Alpha < Beta < Gamma
        expectOrder(searchTitle(col, Locale.ITALIAN.getLanguage(), "ASC"), itemA, itemB, itemC);
    }

    /**
     * When an item has several values in the SAME language, the sort field must use the FIRST value in
     * metadata order, not a later one.
     */
    @Test
    public void multipleValuesInSameLanguageUseFirstInMetadataOrder() throws Exception {
        enableItAndEnLocales();
        Collection col = newCollection(newCommunity(), "Publication collection", "Publication");

        context.turnOffAuthorisationSystem();
        Item itemA = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Zeta", "it")
                                .withTitleForLanguage("Alfa", "it")
                                .withTitleForLanguage("Mid", "en")
                                .build();
        Item itemB = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Beta", "it")
                                .withTitleForLanguage("Beta", "en")
                                .build();
        context.restoreAuthSystemState();

        // Italian uses A's FIRST it value "Zeta" (not "Alfa"): Beta(B) < Zeta(A)
        expectOrder(searchTitle(col, Locale.ITALIAN.getLanguage(), "ASC"), itemB, itemA);
    }

    /**
     * A single result set mixing all title shapes (tagged it+en, untagged only, en only, it only) must be
     * ordered correctly per locale, exercising every fallback step together within one Solr sort.
     */
    @Test
    public void mixedTaggedUntaggedAndSingleLanguageItemsInSameResult() throws Exception {
        enableItAndEnLocales();
        Collection col = newCollection(newCommunity(), "Publication collection", "Publication");

        context.turnOffAuthorisationSystem();
        Item itemA = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Delta", "it")
                                .withTitleForLanguage("Alpha", "en")
                                .build();
        Item itemB = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Bravo", null)
                                .build();
        Item itemC = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Charlie", "en")
                                .build();
        Item itemD = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Echo", "it")
                                .build();
        context.restoreAuthSystemState();

        // English: Alpha(A) < Bravo(B, untagged) < Charlie(C) < Echo(D, first-any)
        expectOrder(searchTitle(col, Locale.ENGLISH.getLanguage(), "ASC"), itemA, itemB, itemC, itemD);
        // Italian: Bravo(B, untagged) < Charlie(C, default locale) < Delta(A) < Echo(D)
        expectOrder(searchTitle(col, Locale.ITALIAN.getLanguage(), "ASC"), itemB, itemC, itemA, itemD);
    }

    /**
     * The default-locale fallback must honour the configured {@code default.locale}, not a hardcoded
     * English. With {@code default.locale=it}, an item without an English title falls back to its Italian
     * value when searched in English.
     */
    @Test
    public void defaultLocaleSetToNonEnglishDrivesFallback() throws Exception {
        configurationService.setProperty("default.locale", "it");
        enableLocales(Locale.ITALIAN.getLanguage(), Locale.ENGLISH.getLanguage());
        Collection col = newCollection(newCommunity(), "Publication collection", "Publication");

        context.turnOffAuthorisationSystem();
        Item itemA = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Zulu", "it")
                                .withTitleForLanguage("Alfa", null)
                                .build();
        Item itemB = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Mike", "it")
                                .withTitleForLanguage("Alpha", "en")
                                .build();
        context.restoreAuthSystemState();

        // English search: A has no en value, so it falls back to the default locale (it) value "Zulu",
        // NOT to the untagged "Alfa": Alpha(B) < Zulu(A).
        expectOrder(searchTitle(col, Locale.ENGLISH.getLanguage(), "ASC"), itemB, itemA);
    }

    /**
     * A placeholder metadata value ({@code PLACEHOLDER_PARENT_METADATA_VALUE}) must never be used as a sort
     * value; the item resolves through the normal fallback instead.
     */
    @Test
    public void placeholderTitleIsIgnoredInSortField() throws Exception {
        enableItAndEnLocales();
        Collection col = newCollection(newCommunity(), "Publication collection", "Publication");

        context.turnOffAuthorisationSystem();
        Item itemA = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Zulu", "en")
                                .withTitleForLanguage(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE, "it")
                                .build();
        Item itemB = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Mango", "en")
                                .build();
        Item itemC = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Alpha", "en")
                                .build();
        context.restoreAuthSystemState();

        // Italian: A's placeholder it value is ignored, so A resolves to the default-locale (en) "Zulu":
        // Alpha(C) < Mango(B) < Zulu(A). A placeholder leak would instead sort A first.
        expectOrder(searchTitle(col, Locale.ITALIAN.getLanguage(), "ASC"), itemC, itemB, itemA);
    }

    /**
     * Performs an ascending Discovery search sorted by {@code dc.title}, scoped to the given collection,
     * for the provided request locale, exactly as the frontend would.
     *
     * @param collection the collection used as search scope
     * @param locale     the locale sent through the {@code Accept-Language} header
     * @return the {@link ResultActions} of the REST call, already asserted to be {@code 200 OK}
     * @throws Exception if the request fails
     */
    private ResultActions searchTitleAsc(Collection collection, Locale locale) throws Exception {
        return searchTitle(collection, locale.getLanguage(), "ASC");
    }

    /**
     * Performs a Discovery search sorted by {@code dc.title} in the given direction, scoped to the given
     * collection, optionally sending an {@code Accept-Language} header, exactly as the frontend would.
     *
     * @param collection        the collection used as search scope
     * @param acceptLanguageTag the language tag for the {@code Accept-Language} header, or {@code null} to
     *                          send no header
     * @param direction         the sort direction ({@code ASC} or {@code DESC})
     * @return the {@link ResultActions} of the REST call, already asserted to be {@code 200 OK}
     * @throws Exception if the request fails
     */
    private ResultActions searchTitle(Collection collection, String acceptLanguageTag, String direction)
        throws Exception {
        var request = get("/api/discover/search/objects")
            .param("dsoType", "Item")
            .param("scope", collection.getID().toString())
            .param("sort", "dc.title," + direction);
        if (acceptLanguageTag != null) {
            request = request.header("Accept-Language", acceptLanguageTag);
        }
        return getClient().perform(request)
                          .andExpect(status().isOk())
                          .andExpect(jsonPath("$.type", is("discover")));
    }

    /**
     * Builds the canonical two-language dataset: three items with {@code it}/{@code en}
     * tagged titles. Returns them as {@code [item1, item2, item3]}.
     *
     * @param col the collection to create the items in
     * @return the created items, in creation order
     */
    private Item[] buildItalianAndEnglishTitledItems(Collection col) {
        context.turnOffAuthorisationSystem();
        Item item1 = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Bravo", "it")
                                .withTitleForLanguage("Yankee", "en")
                                .build();
        Item item2 = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Zulu", "it")
                                .withTitleForLanguage("Alpha", "en")
                                .build();
        Item item3 = ItemBuilder.createItem(context, col)
                                .withTitleForLanguage("Mike", "it")
                                .withTitleForLanguage("Mike", "en")
                                .build();
        context.restoreAuthSystemState();
        return new Item[] { item1, item2, item3 };
    }

    /**
     * Asserts that the search response contains exactly the given items, in the given order, by matching
     * the embedded indexable object id of each result against an ordered Hamcrest matcher.
     *
     * @param resultActions the search REST response
     * @param itemsInOrder  the items expected, in the exact order
     * @throws Exception if an assertion fails
     */
    private void expectOrder(ResultActions resultActions, Item... itemsInOrder) throws Exception {
        List<Matcher<? super Object>> orderedMatchers = Arrays.stream(itemsInOrder)
                                                              .map(this::matchItemId)
                                                              .collect(Collectors.toList());
        resultActions
            .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(itemsInOrder.length)))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.contains(orderedMatchers)));
    }

    private Matcher<? super Object> matchItemId(Item item) {
        return hasJsonPath("$._embedded.indexableObject.id", is(item.getID().toString()));
    }

    private void enableItAndEnLocales() {
        configurationService.setProperty("webui.supported.locales", new String[] { Locale.ENGLISH.getLanguage(),
                                                                                   Locale.ITALIAN.getLanguage() });
    }

    private void enableLocales(String... supportedLanguages) {
        configurationService.setProperty("webui.supported.locales", supportedLanguages);
    }

    private Community newCommunity() {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Root community")
                                          .build();
        context.restoreAuthSystemState();
        return parentCommunity;
    }

    private Collection newCollection(Community parentCommunity, String collectionName, String entityType) {
        context.turnOffAuthorisationSystem();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName(collectionName)
                                                 .withEntityType(entityType)
                                                 .build();
        context.restoreAuthSystemState();
        return collection;
    }

}
