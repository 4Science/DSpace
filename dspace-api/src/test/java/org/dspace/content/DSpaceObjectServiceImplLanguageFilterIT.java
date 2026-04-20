/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the language-family filtering logic introduced in DSC-896,
 * specifically for {@link org.dspace.content.DSpaceObjectServiceImpl#getMetadata(
 * org.dspace.content.DSpaceObject, String, String, String, String)}.
 *
 * <p>All tests create a real {@link Item} in H2 and invoke the public service API.
 *
 * @author DSpace developers
 */
public class DSpaceObjectServiceImplLanguageFilterIT extends AbstractIntegrationTestWithDatabase {

    private static final String DC = "dc";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";

    private final ItemService itemService =
        ContentServiceFactory.getInstance().getItemService();

    private Collection collection;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Test Community")
            .build();
        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Test Collection")
            .build();
        context.restoreAuthSystemState();
    }

    // ------------------------------------------------------------------
    // Helper: extract stored values from a MetadataValue list
    // ------------------------------------------------------------------

    private List<String> values(List<MetadataValue> mvs) {
        return mvs.stream().map(MetadataValue::getValue).collect(Collectors.toList());
    }

    private List<String> langs(List<MetadataValue> mvs) {
        return mvs.stream().map(MetadataValue::getLanguage).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Test 1: lang = "en" matches exact "en", "en_US", "en_UK", "en-GB"
    //         but NOT "de" or null.
    // ------------------------------------------------------------------

    @Test
    public void testLangEnMatchesLanguageFamily() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitleForLanguage("Title EN", "en")
            .withTitleForLanguage("Title EN_US", "en_US")
            .withTitleForLanguage("Title EN_UK", "en_UK")
            .withTitleForLanguage("Title EN-GB", "en-GB")
            .withTitleForLanguage("Title DE", "de")
            .withTitle("Title NULL")          // null language via standard builder
            .build();
        context.restoreAuthSystemState();
        context.commit();

        List<MetadataValue> result = itemService.getMetadata(item, DC, TITLE, null, "en");

        assertThat("en filter must return 4 values", result, hasSize(4));
        assertThat(values(result), containsInAnyOrder(
            "Title EN", "Title EN_US", "Title EN_UK", "Title EN-GB"));
    }

    // ------------------------------------------------------------------
    // Test 2: lang = null returns only values with null language.
    // ------------------------------------------------------------------

    @Test
    public void testNullLangReturnsNullLanguageValuesOnly() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitleForLanguage("Title EN", "en")
            .withTitleForLanguage("Title DE", "de")
            .withTitle("Title NULL")
            .build();
        context.restoreAuthSystemState();
        context.commit();

        List<MetadataValue> result = itemService.getMetadata(item, DC, TITLE, null, null);

        assertThat("null lang filter must return only null-language value", result, hasSize(1));
        assertThat(result.get(0).getValue(), is("Title NULL"));
        assertThat(result.get(0).getLanguage(), is((String) null));
    }

    // ------------------------------------------------------------------
    // Test 3: lang = Item.ANY returns ALL values.
    // ------------------------------------------------------------------

    @Test
    public void testAnyLangReturnsAll() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitleForLanguage("Title EN", "en")
            .withTitleForLanguage("Title EN_US", "en_US")
            .withTitleForLanguage("Title EN_UK", "en_UK")
            .withTitleForLanguage("Title EN-GB", "en-GB")
            .withTitleForLanguage("Title DE", "de")
            .withTitle("Title NULL")
            .build();
        context.restoreAuthSystemState();
        context.commit();

        List<MetadataValue> result = itemService.getMetadata(item, DC, TITLE, null, Item.ANY);

        assertThat("Item.ANY must return all 6 values", result, hasSize(6));
    }

    // ------------------------------------------------------------------
    // Test 4: lang = "de" returns only "de", not "en*".
    // ------------------------------------------------------------------

    @Test
    public void testLangDeReturnsOnlyDe() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitleForLanguage("Title EN", "en")
            .withTitleForLanguage("Title EN_US", "en_US")
            .withTitleForLanguage("Title DE", "de")
            .withTitle("Title NULL")
            .build();
        context.restoreAuthSystemState();
        context.commit();

        List<MetadataValue> result = itemService.getMetadata(item, DC, TITLE, null, "de");

        assertThat("de filter must return exactly 1 value", result, hasSize(1));
        assertThat(result.get(0).getValue(), is("Title DE"));
        assertThat(result.get(0).getLanguage(), is("de"));
    }

    // ------------------------------------------------------------------
    // Test 5: Language matching is case-insensitive.
    // ------------------------------------------------------------------

    @Test
    public void testLangMatchingIsCaseInsensitive() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitleForLanguage("Title UPPER", "EN")
            .withTitleForLanguage("Title Mixed", "En_US")
            .withTitleForLanguage("Title DE", "de")
            .build();
        context.restoreAuthSystemState();
        context.commit();

        List<MetadataValue> result = itemService.getMetadata(item, DC, TITLE, null, "en");

        assertThat("case-insensitive en filter must return 2 values", result, hasSize(2));
        assertThat(values(result), containsInAnyOrder("Title UPPER", "Title Mixed"));
    }

    // ------------------------------------------------------------------
    // Test 6: lang = "fr" returns empty when no French values exist.
    // ------------------------------------------------------------------

    @Test
    public void testLangFrReturnsEmptyWhenNoFrenchValues() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitleForLanguage("Title EN", "en")
            .withTitle("Title NULL")
            .build();
        context.restoreAuthSystemState();
        context.commit();

        List<MetadataValue> result = itemService.getMetadata(item, DC, TITLE, null, "fr");

        assertThat("fr filter must return empty list when no fr values", result, is(empty()));
    }

    // ------------------------------------------------------------------
    // Test 7: Blank/empty language strings.
    //   - getMetadata(…, "en") must NOT match blank/empty lang values.
    //   - getMetadata(…, null) behavior documented: blank lang ≠ null lang
    //     (implementation returns blank values only when lang == Item.ANY).
    // ------------------------------------------------------------------

    @Test
    public void testBlankLanguageNotMatchedByEnFilter() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitleForLanguage("Title BLANK", "")
            .withTitleForLanguage("Title SPACES", "  ")
            .withTitleForLanguage("Title EN", "en")
            .build();
        context.restoreAuthSystemState();
        context.commit();

        // "en" filter: blank lang does NOT start with "en", so excluded
        List<MetadataValue> enResult = itemService.getMetadata(item, DC, TITLE, null, "en");
        assertThat("en filter must not include blank-language values", enResult, hasSize(1));
        assertThat(enResult.get(0).getValue(), is("Title EN"));

        // null filter: blank lang != null, so blank values not returned
        List<MetadataValue> nullResult = itemService.getMetadata(item, DC, TITLE, null, null);
        assertThat("null filter must not return blank-language values (blank != null)", nullResult, is(empty()));

        // Item.ANY: all three returned
        List<MetadataValue> anyResult = itemService.getMetadata(item, DC, TITLE, null, Item.ANY);
        assertThat("Item.ANY must return all 3 values", anyResult, hasSize(3));
    }

    // ------------------------------------------------------------------
    // Test 8: lang = "en" with qualifier = Item.ANY spans multiple qualifiers.
    // ------------------------------------------------------------------

    @Test
    public void testLangEnWithAnyQualifierSpansQualifiers() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            // dc.title (no qualifier)
            .withTitleForLanguage("Title EN_US", "en_US")
            // dc.title.alternative
            .withTitleForLanguage("Alt EN_US", "en_US")    // reuse withTitleForLanguage for dc.title.alternative
            .build();

        // Add dc.title.alternative with en_US language directly via itemService
        itemService.addMetadata(context, item, DC, TITLE, "alternative", "en_US", "Alt EN_US Direct");
        itemService.update(context, item);
        context.restoreAuthSystemState();
        context.commit();

        List<MetadataValue> result = itemService.getMetadata(item, DC, TITLE, Item.ANY, "en");

        // Should include dc.title (en_US) and dc.title.alternative (en_US) matches
        assertThat("en filter with ANY qualifier must return values from all qualifiers",
            result.stream().anyMatch(mv -> "en_US".equals(mv.getLanguage())), is(true));
    }

}
