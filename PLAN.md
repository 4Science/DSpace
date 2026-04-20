# Implementation Plan — DSC-896

## Summary

This ticket addresses three distinct but interconnected concerns around multilingual metadata in
DSpace:

1. **Export / import** — The service layer's `getFilteredMetadataValuesByLanguage` fuzzy-fallback
   currently corrupts exact-match semantics for any caller that supplies a specific language code.
   Export and import pipelines must retrieve *all* languages for all metadata (including values whose
   `text_lang` is `null`, `*`, or an arbitrary locale). The fix is to strip the fallback logic from
   the core `getMetadata` path and restore plain exact-match behaviour, so that export/import always
   obtains the full, unfiltered picture when passing `Item.ANY`.

2. **Simple-page display** — The UI should show, for a given user locale (e.g. `en`): (a) values
   whose `text_lang` starts with the user's language code (covers `en`, `en_US`, `en_UK`, etc.),
   (b) values with `null`, blank/empty, or `*` language (always visible), and (c) as a **fallback**,
   if no values remain for a given metadata field after (a)+(b) filtering, all values for that field
   are included regardless of language. This ensures no metadata field disappears silently.

3. **Technical-view "all languages" projection** — Administrators need a way to see every metadata
   value in every language without any filtering. A new REST projection (or query parameter, e.g.
   `projection=allLanguages`) will bypass language filtering entirely. This mode is used exclusively
   in the technical/admin view; it is never the default for public-facing pages.

Additionally, an **optional Flyway migration** (opt-in, not run by default) normalises `text_lang`
to `null` for values that carry the installation's `default.language` code (typically `en_US`) on
existing deployments where the language was never intentionally set.

---

## Affected Areas

| Module | Package / Path |
|--------|----------------|
| `dspace-api` | `org.dspace.content.DSpaceObjectServiceImpl` |
| `dspace-api` | `org.dspace.content.ItemServiceImpl` |
| `dspace-api` | `org.dspace.content.security.MetadataSecurityServiceImpl` |
| `dspace-api` | `org.dspace.content.security.MetadataSecurityService` (interface) |
| `dspace-api` | `org.dspace.core.I18nUtil` |
| `dspace-api` | `org.dspace.app.bulkimport.service.BulkImportWorkbookBuilderImpl` |
| `dspace-server-webapp` | `org.dspace.app.rest.converter.MetadataConverter` |
| `dspace-server-webapp` | REST repositories / projections that call `getPermissionAndLangFilteredMetadataFields` |
| Config | `dspace/config/dspace.cfg` — `webui.supported.locales` / `default.locale` / `default.language` |

---

## Files to Create

| File | Reason |
|------|--------|
| `dspace-api/src/test/java/org/dspace/content/DSpaceObjectServiceImplLanguageFilterIT.java` | Integration tests covering exact-match semantics after removing the fallback |
| `dspace-api/src/test/java/org/dspace/content/security/MetadataSecurityServiceImplLangIT.java` | Integration tests for UI-layer filtering: language-family match, null/*/empty always-show, fallback rule |
| `dspace-server-webapp/src/test/java/org/dspace/app/rest/ItemMetadataLanguageIT.java` | REST integration tests for simple-page vs technical-view (allLanguages projection) |
| `dspace-api/src/main/resources/org/dspace/storage/rdbms/sqlmigration/postgres/V{ver}_{date}__Optional_normalize_text_lang_default_language.sql` | **Optional** Flyway migration (PostgreSQL) — sets `text_lang = null` where it equals `default.language` and `default.language` is not configured |
| `dspace-api/src/main/resources/org/dspace/storage/rdbms/sqlmigration/h2/V{ver}_{date}__Optional_normalize_text_lang_default_language.sql` | **Optional** Flyway migration (H2) — same as above for test databases |

---

## Files to Modify

| File | Change needed |
|------|---------------|
| `dspace-api/src/main/java/org/dspace/content/DSpaceObjectServiceImpl.java` | Remove `getFilteredMetadataValuesByLanguage` call from `getMetadata(…, lang)`. Restore exact-match: `null` → null-lang values; `Item.ANY` → all; else → `StringUtils.equals(lang, dcv.getLanguage())`. Deprecate the old private fallback methods. |
| `dspace-api/src/main/java/org/dspace/content/ItemServiceImpl.java` | Same fix at line 2148 — remove `getFilteredMetadataValuesByLanguage` call from the `getMetadata` override. |
| `dspace-api/src/main/java/org/dspace/content/security/MetadataSecurityServiceImpl.java` | Rewrite `getPermissionAndLangFilteredMetadataFields` with new UI filter rules (language-family prefix match, null/blank/`*` always-show, per-field fallback). Add `isAllLanguagesProjection(Context)` check to bypass filtering for technical view. |
| `dspace-api/src/main/java/org/dspace/content/security/MetadataSecurityService.java` | Update JavaDoc on `getPermissionAndLangFilteredMetadataFields` to document the new contract. |
| `dspace-api/src/main/java/org/dspace/app/bulkimport/service/BulkImportWorkbookBuilderImpl.java` | Confirm `isLanguageSupported` only governs column header generation, not value suppression. Remove any filtering that silently drops values in unsupported languages during export. |
| `dspace-server-webapp/src/main/java/org/dspace/app/rest/converter/MetadataConverter.java` | Detect `allLanguages` projection flag and route to `getPermissionFilteredMetadataValues` (unfiltered by language) instead of `getPermissionAndLangFilteredMetadataFields`. |
| `dspace/config/dspace.cfg` | Update comments for `webui.supported.locales` and `default.language` explaining the new semantics. |
| `dspace-api/src/test/java/org/dspace/content/ItemTest.java` | Update test at line 469 if exact-match change alters existing assertions. |

---

## Implementation Approach

### Step 1 — Restore exact-match semantics in the service layer

In `DSpaceObjectServiceImpl.getMetadata(T dso, String schema, String element, String qualifier, String lang)`:

- Replace the call to `getFilteredMetadataValuesByLanguage(values, lang)` with plain exact-match logic:
  - `lang == null` → keep values where `dcv.getLanguage() == null`
  - `lang.equals(Item.ANY)` → keep all values (no change)
  - else → keep values where `StringUtils.equals(lang, dcv.getLanguage())`
- Deprecate `getFilteredMetadataValuesByLanguage`, `filterMetadataValuesByLanguage`,
  `filterByLanguageInSupportedLocales`, and `filterByLanguage`. They must no longer be called from
  the core retrieval path.
- Apply the identical fix to `ItemServiceImpl.getMetadata` (line 2148).

**Impact**: export/import, authority lookup, and all callers passing `Item.ANY` are unaffected.
Callers relying on the fuzzy fallback will now get stricter results — that is the intended outcome.
(See Open Questions §1 for the risk assessment of those callers.)

### Step 2 — Implement correct UI-layer filtering in MetadataSecurityServiceImpl

Rewrite `getPermissionAndLangFilteredMetadataFields` with the following rules, applied
**per metadata field** (grouped by `schema.element.qualifier`):

```
requestLang = context.getCurrentLocale().getLanguage()   // e.g. "en"

For each MetadataValue mv in the field group:
  mvLang = mv.getLanguage()
  INCLUDE if:
    - mvLang is null, blank/empty, or equals "*"          (always-show)
    - mvLang.toLowerCase().startsWith(requestLang.toLowerCase())
      // covers "en", "en_US", "en_UK", "en-GB" etc.

After filtering the group:
  if result is empty:
    FALLBACK → include ALL values for that field group    (no value must disappear)
```

Where `requestLang` is derived from the current context locale, and `supportedLocales` (loaded from
`webui.supported.locales`, falling back to `I18nUtil.getDefaultLocale()`) is **no longer used to
decide what to hide** — instead, language-family prefix matching replaces the old "in supported
list" guard.

The `*` sentinel value must be treated identically to `null` throughout (always-show).
Blank/empty strings are also normalised to always-show at read time (no DB change required).

Helper methods to add in `MetadataSecurityServiceImpl`:
- `isAlwaysVisible(String lang)` — returns `true` for null, blank, `"*"`
- `matchesRequestLocale(String stored, String requested)` — case-insensitive `startsWith`
- `applyFallback(Map<String, List<MetadataValue>> grouped, Map<String, List<MetadataValue>> filtered)`
  — merges fallback groups back in where the filtered result is empty

### Step 3 — "All languages" projection for the technical view

Add an `allLanguages` projection mechanism so the technical/admin view receives every metadata
value without any language filtering:

- **Option A (preferred)**: add a named DSpace REST `Projection` (e.g. `AllMetadataLanguagesProjection`)
  that sets a flag detected by `MetadataConverter`. When this projection is active,
  `MetadataConverter` calls `getPermissionFilteredMetadataValues` (permission-filtered but
  language-unfiltered) instead of `getPermissionAndLangFilteredMetadataFields`.
- **Option B (simpler)**: add a boolean query parameter `?allLanguages=true` on item REST endpoints;
  store the flag in `Context` for the duration of the request.

The choice between A and B is an **open question** (see §Q3). Regardless of approach:
- The technical view is the only consumer; it must require at minimum `WRITE` permission on the item
  (or be admin-only) to prevent information leakage of potentially-sensitive language variants.
- Document in `dspace.cfg` that this feature exists and how to enable it in the Angular UI.

### Step 4 — Normalise comparison of language codes (defensive)

Invalid `text_lang` values (`"EN"`, `""`, `" "`) exist in production. The UI-layer helpers from
Step 2 already handle these at read time:

- Blank/empty → treated as always-visible (no language filtering applied)
- Wrong case (e.g. `"EN"`) → `startsWith` with `toLowerCase()` normalises this at comparison time

No DB changes are required for this step.

### Step 5 — Optional Flyway migration

Provide an **opt-in** migration that sets `text_lang = null` for all `metadatavalue` rows where
`text_lang` equals the value of `default.language` in `dspace.cfg`, for installations where that
language was set as a default (typically `en_US`) but was never intentionally recorded as a real
language tag.

Implementation approach:
- The migration SQL reads the configured `default.language` value at migration time via a
  conditional `UPDATE` or a script-based migration class.
- Alternatively, supply it as a standalone SQL script that an administrator runs manually after
  reviewing the data — **not** a versioned Flyway migration that runs automatically.
- Mark clearly in comments that this migration is destructive (it converts language-tagged values to
  untagged) and should only be applied after confirming that no records intentionally carry the
  default language code.

```sql
-- Optional: run manually after reviewing affected rows with:
-- SELECT count(*) FROM metadatavalue WHERE text_lang = '<default.language value>';
UPDATE metadatavalue SET text_lang = NULL WHERE text_lang = 'en_US';
-- Replace 'en_US' with the actual default.language value for your installation.
```

PostgreSQL and H2 variants must both be provided if packaged as Flyway migrations.

### Step 6 — Export / import validation

- **Export**: `BulkImportWorkbookBuilderImpl.isLanguageSupported` (line 417) — confirm it only
  controls whether a dedicated language-suffixed column header is created in the spreadsheet, not
  whether the value is written. Values in any language (including unsupported ones) must appear in
  the exported workbook. Remove any suppression if found.
- **Import**: `ItemImportServiceImpl` (lines 957–981) — stores the language attribute as-is from
  the XML source; no change needed. The fix in Step 1 ensures that `addMetadata` with an exact
  language code is stored correctly.
- **DIM/QDC crosswalks**: already pass language through directly; no change needed.

### Step 7 — REST layer wiring verification

Confirm that the REST layer routes item display requests through
`getPermissionAndLangFilteredMetadataFields` (Step 2) and technical-view requests through the
`allLanguages` projection (Step 3). Specifically:

- `MetadataConverter` — insert projection detection before the `getPermissionAndLangFilteredMetadataFields` call.
- `ItemRestRepository` — ensure the `Projection` object is propagated to `MetadataConverter`.
- Any other REST repository that exposes item metadata (e.g. workspace items, workflow items) should
  apply the same routing logic.

### Step 8 — Configuration documentation

Update `dspace.cfg`:

```properties
# Languages available for the user interface.
# Used to determine language-family matching in the simple item page:
#   - Metadata with text_lang starting with the user's locale language (e.g. "en", "en_US") is shown.
#   - Metadata with null, blank, or "*" text_lang is always shown.
#   - If no values remain for a metadata field after filtering, ALL values for that field are shown
#     (fallback rule — no field disappears silently).
# This setting does NOT affect service-layer metadata retrieval or export/import.
# webui.supported.locales = en, de

# default.language controls the language tag applied to new metadata values in the submission form.
# The optional text_lang normalisation migration uses this value.
# default.language = en_US
```

---

## Test Strategy

### Unit tests (no DB)

- `DSpaceObjectServiceImplTest` — mock DSO with `text_lang` values `null`, `"en"`, `"EN"`, `""`,
  `"ja"`, `"en_US"`; assert `getMetadata(…, "en")` returns only the value with `text_lang = "en"`.
- `MetadataSecurityServiceImplTest` — mock context locale `"en"`, supported locales `["en","de"]`:
  - `null` language → included
  - `"*"` language → included
  - `""` (blank) → included
  - `"en"` → included (exact match)
  - `"en_US"` → included (starts with `"en"`)
  - `"en_UK"` → included (starts with `"en"`)
  - `"de"` → excluded (starts with `"de"`, not `"en"`)
  - `"ja"` → included (not in supported locales, fallback rule kicks in)
  - Field with only `"de"` values when user is `"en"` → fallback → `"de"` value included

### Integration tests (H2)

- `DSpaceObjectServiceImplLanguageFilterIT` — items with mixed `text_lang` values; assert
  `getMetadata(…, "en")` returns exactly one match; `getMetadata(…, Item.ANY)` returns all.
- `MetadataSecurityServiceImplLangIT` — item with Japanese title and English title; assert `en`
  locale response contains both (Japanese triggers fallback or is always-show depending on which
  field group); item with only `de` title shows `de` value when user is `en` (fallback).
- `ItemTest` line 469 — review and update if exact-match change affects existing assertion.

### REST integration tests

- `ItemMetadataLanguageIT.testSimplePage_languageFamilyFiltering` — `GET /api/core/items/{uuid}`
  with `Accept-Language: en`; verify `en_US` and `en_UK` values present, `de` value absent (unless
  no `en` value exists for that field), null-lang values present.
- `ItemMetadataLanguageIT.testSimplePage_fallback` — item with only `de` title; `en` request →
  fallback → `de` value returned.
- `ItemMetadataLanguageIT.testTechnicalView_allLanguages` — `GET /api/core/items/{uuid}?projection=allLanguages`
  (admin token); verify all language variants returned.
- `ItemMetadataLanguageIT.testTechnicalView_unauthorizedAnonymous` — same request without admin
  token → `allLanguages` projection not applied (or 403).
- Export round-trip test: export item with Japanese and English titles → import → verify both
  values preserved.

---

## Open Questions / Risks

1. **Callers that relied on the fuzzy fallback** — Code that calls `getMetadata(…, "en")` expecting
   to receive `"en_US"` values will now get an empty list. A codebase-wide search for
   `getMetadata(` with non-`ANY`, non-`null` language arguments is required before implementation.
   These callers must either be updated to pass `Item.ANY` or to use the new UI-layer filter.

2. **Solr indexing** — `SolrServiceValuePairsIndexPlugin` calls `I18nUtil.getSupportedLocales()`
   directly. It is likely unaffected (it does not call `getMetadata` with a specific lang), but must
   be confirmed before merging.

3. **"All languages" projection: query param vs named projection vs Context flag** — Three
   implementation options exist. The named `Projection` approach (Option A) is cleanest
   architecturally but requires a new projection class and wiring. The `?allLanguages=true` query
   parameter (Option B) is simpler but less idiomatic in DSpace 7. A `Context`-level flag (Option C)
   avoids REST-layer changes but is harder to test. **Decision needed before implementation starts.**

4. **Authorization for `allLanguages` projection** — Must be restricted to users with at least
   `WRITE` (or admin) permission on the item to avoid leaking metadata in languages that may contain
   sensitive or draft content. The exact permission level must be agreed.

5. **Optional migration scope** — The migration targets values where `text_lang = default.language`.
   If an installation has intentionally stored metadata in `en_US` (e.g. a field explicitly tagged
   with that language by an editor), the migration would incorrectly set it to `null`. The migration
   must be manual / opt-in with a clear data-review step. Decide whether to ship it as a Flyway
   versioned migration (disabled by default) or as a standalone SQL script in the documentation.

6. **`*` sentinel handling** — The `*` wildcard is mentioned in the ticket as equivalent to `null`.
   Confirm whether `*` is actually written to `text_lang` in any production data, or whether it only
   appears as `Item.ANY` in Java code (which is never persisted). If `*` can appear in the DB,
   `isAlwaysVisible` must guard against it; if not, it is a code-only concern.
