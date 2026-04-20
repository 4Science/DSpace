/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.security;

import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.security.service.MetadataSecurityService;
import org.dspace.content.service.ItemService;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the language-filtering logic in
 * {@link MetadataSecurityServiceImpl#getPermissionAndLangFilteredMetadataFields},
 * introduced in DSC-896.
 *
 * <p>
 * Tests exercise the {@code isAlwaysVisible} (null/blank/"*" language) and
 * {@code matchesRequestLocale} (family prefix match) rules, as well as the
 * per-field fallback when no value matches the requested locale.
 *
 * @author DSpace developers
 */
public class MetadataSecurityServiceImplLangIT extends AbstractIntegrationTestWithDatabase {

  private static final String DC = "dc";
  private static final String TITLE = "title";
  private static final String DESCRIPTION = "description";

  /** Retrieved by class type from the application context (no XML id). */
  private final MetadataSecurityService metadataSecurityService = new DSpace().getServiceManager()
      .getApplicationContext()
      .getBean(MetadataSecurityService.class);

  private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

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
  // Helper
  // ------------------------------------------------------------------

  /**
   * Runs {@code getPermissionAndLangFilteredMetadataFields} with authorization
   * checks bypassed (preventBoxSecurityCheck = true) so tests focus purely on
   * language filtering.
   */
  private List<MetadataValue> langFilter(Item item, Locale requestLocale) throws SQLException {
    context.setCurrentLocale(requestLocale);
    return metadataSecurityService
        .getPermissionAndLangFilteredMetadataFields(context, item, true);
  }

  private List<String> valuesFor(List<MetadataValue> mvs, String element) {
    return mvs.stream()
        .filter(mv -> element.equals(mv.getMetadataField().getElement()))
        .map(MetadataValue::getValue)
        .collect(Collectors.toList());
  }

  private List<String> langs(List<MetadataValue> mvs, String element) {
    return mvs.stream()
        .filter(mv -> element.equals(mv.getMetadataField().getElement()))
        .map(MetadataValue::getLanguage)
        .collect(Collectors.toList());
  }

  // ------------------------------------------------------------------
  // Test 1: null language is always visible regardless of locale.
  // A "de"-tagged value is excluded when locale = en.
  // ------------------------------------------------------------------

  @Test
  public void testNullLanguageIsAlwaysVisible() throws SQLException, AuthorizeException {
    context.turnOffAuthorisationSystem();
    Item item = ItemBuilder.createItem(context, collection)
        .withTitle("Title NULL") // language = null
        .withTitleForLanguage("Title DE", "de")
        .build();
    context.restoreAuthSystemState();
    context.commit();

    List<MetadataValue> result = langFilter(item, Locale.ENGLISH);

    List<String> titleValues = valuesFor(result, TITLE);
    assertThat("null-lang title must always be included", titleValues.contains("Title NULL"), is(true));
    assertThat("de title must be excluded when locale=en", titleValues.contains("Title DE"), is(false));
  }

  // ------------------------------------------------------------------
  // Test 2: "*" wildcard language is always visible.
  // ------------------------------------------------------------------

  @Test
  public void testWildcardLanguageIsAlwaysVisible() throws SQLException, AuthorizeException {
    context.turnOffAuthorisationSystem();
    Item item = ItemBuilder.createItem(context, collection)
        .withTitleForLanguage("Title STAR", "*")
        .withTitleForLanguage("Title DE", "de")
        .build();
    context.restoreAuthSystemState();
    context.commit();

    List<MetadataValue> result = langFilter(item, Locale.ENGLISH);

    List<String> titleValues = valuesFor(result, TITLE);
    assertThat("*-lang title must always be included", titleValues.contains("Title STAR"), is(true));
    // "de" is excluded unless fallback fires; since "*" matched, no fallback, de
    // excluded
    assertThat("de title excluded when * value already satisfies field", titleValues.contains("Title DE"),
        is(false));
  }

  // ------------------------------------------------------------------
  // Test 3: Blank language is always visible (isAlwaysVisible rule).
  // ------------------------------------------------------------------

  @Test
  public void testBlankLanguageIsAlwaysVisible() throws SQLException, AuthorizeException {
    context.turnOffAuthorisationSystem();
    Item item = ItemBuilder.createItem(context, collection)
        .withTitleForLanguage("Title BLANK", "")
        .withTitleForLanguage("Title DE", "de")
        .build();
    context.restoreAuthSystemState();
    context.commit();

    List<MetadataValue> result = langFilter(item, Locale.ENGLISH);

    List<String> titleValues = valuesFor(result, TITLE);
    assertThat("blank-lang title must always be included", titleValues.contains("Title BLANK"), is(true));
    // blank satisfies field → no fallback → de excluded
    assertThat("de title excluded when blank already satisfies field", titleValues.contains("Title DE"),
        is(false));
  }

  // ------------------------------------------------------------------
  // Test 4: Language-family match: "en", "en_US", "en_UK" included;
  // "de" excluded when locale = en.
  // ------------------------------------------------------------------

  @Test
  public void testLanguageFamilyMatchIncludesVariants() throws SQLException, AuthorizeException {
    context.turnOffAuthorisationSystem();
    Item item = ItemBuilder.createItem(context, collection)
        .withTitleForLanguage("Title EN", "en")
        .withTitleForLanguage("Title EN_US", "en_US")
        .withTitleForLanguage("Title EN_UK", "en_UK")
        .withTitleForLanguage("Title DE", "de")
        .build();
    context.restoreAuthSystemState();
    context.commit();

    List<MetadataValue> result = langFilter(item, Locale.ENGLISH);

    List<String> titleValues = valuesFor(result, TITLE);
    assertThat("en title must be included", titleValues.contains("Title EN"), is(true));
    assertThat("en_US title must be included", titleValues.contains("Title EN_US"), is(true));
    assertThat("en_UK title must be included", titleValues.contains("Title EN_UK"), is(true));
    assertThat("de title must be excluded", titleValues.contains("Title DE"), is(false));
  }

  // ------------------------------------------------------------------
  // Test 5: Unsupported/foreign language (e.g., "ja") — fallback fires
  // because "ja" is not matched by locale=en, and no en value exists.
  // ------------------------------------------------------------------

  @Test
  public void testForeignLanguageValueReturnedViaFallback() throws SQLException, AuthorizeException {
    context.turnOffAuthorisationSystem();
    Item item = ItemBuilder.createItem(context, collection)
        .withTitleForLanguage("Title JA", "ja")
        .build();
    context.restoreAuthSystemState();
    context.commit();

    List<MetadataValue> result = langFilter(item, Locale.ENGLISH);

    List<String> titleValues = valuesFor(result, TITLE);
    assertThat("ja title returned via fallback when no en value exists",
        titleValues.contains("Title JA"), is(true));
  }

  // ------------------------------------------------------------------
  // Test 6: Fallback fires when only "de" exists and locale = en.
  // "de" is supported but does not match "en", so field is empty
  // after filtering → fallback includes "de".
  // ------------------------------------------------------------------

  @Test
  public void testFallbackWhenOnlySupportedButNonMatchingLangExists()
      throws SQLException, AuthorizeException {
    context.turnOffAuthorisationSystem();
    Item item = ItemBuilder.createItem(context, collection)
        .withTitleForLanguage("Title DE", "de")
        .build();
    context.restoreAuthSystemState();
    context.commit();

    List<MetadataValue> result = langFilter(item, Locale.ENGLISH);

    List<String> titleValues = valuesFor(result, TITLE);
    assertThat("de title returned via fallback when no en value exists",
        titleValues.contains("Title DE"), is(true));
  }

  // ------------------------------------------------------------------
  // Test 7: No fallback when en values exist alongside de.
  // Only "en" is returned; "de" is excluded; no fallback needed.
  // ------------------------------------------------------------------

  @Test
  public void testNoFallbackWhenEnValueExists() throws SQLException, AuthorizeException {
    context.turnOffAuthorisationSystem();
    Item item = ItemBuilder.createItem(context, collection)
        .withTitleForLanguage("Title EN", "en")
        .withTitleForLanguage("Title DE", "de")
        .build();
    context.restoreAuthSystemState();
    context.commit();

    List<MetadataValue> result = langFilter(item, Locale.ENGLISH);

    List<String> titleValues = valuesFor(result, TITLE);
    assertThat("en title must be returned", titleValues.contains("Title EN"), is(true));
    assertThat("de title must NOT be returned (field not empty, no fallback)",
        titleValues.contains("Title DE"), is(false));
  }

  // ------------------------------------------------------------------
  // Test 8: Per-field fallback isolation.
  // dc.title has "en_US" → no fallback → de excluded.
  // dc.description has only "de" → fallback → de included.
  // ------------------------------------------------------------------

  @Test
  public void testPerFieldFallbackIsolation() throws SQLException, AuthorizeException {
    context.turnOffAuthorisationSystem();
    Item item = ItemBuilder.createItem(context, collection)
        .withTitleForLanguage("Title EN_US", "en_US")
        .withTitleForLanguage("Title DE", "de")
        .build();

    // Add dc.description with only "de"
    itemService.addMetadata(context, item, DC, DESCRIPTION, null, "de", "Description DE");
    itemService.update(context, item);
    context.restoreAuthSystemState();
    context.commit();

    List<MetadataValue> result = langFilter(item, Locale.ENGLISH);

    List<String> titleValues = valuesFor(result, TITLE);
    List<String> descriptionValues = valuesFor(result, DESCRIPTION);

    // dc.title: en_US matched → "Title EN_US" returned, "Title DE" excluded (no
    // fallback)
    assertThat("dc.title en_US must be returned", titleValues.contains("Title EN_US"), is(true));
    assertThat("dc.title de must be excluded (field not empty)", titleValues.contains("Title DE"), is(false));

    // dc.description: only "de" → no match for "en" → fallback → "de" included
    assertThat("dc.description de must be returned via fallback",
        descriptionValues.contains("Description DE"), is(true));
  }

}
