/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.solr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

/**
 * Unit tests for {@link SolrCoreExportImport}.
 *
 * <p>Tests the pure-computation methods of the export utility without requiring
 * a running DSpace instance or Solr server.  Private methods are accessed via
 * reflection; the class instance is created through Mockito so that the
 * {@code DSpaceServicesFactory} field-initialiser is never invoked.
 *
 * <p>Covered:
 * <ul>
 *   <li>UUID ↔ unsigned {@link BigInteger} round-trip conversion</li>
 *   <li>UUID key-space partitioning (non-overlap, contiguity, edge cases)</li>
 *   <li>{@code buildSelectUrl} parameter encoding</li>
 *   <li>RFC 4180 CSV value escaping</li>
 *   <li>Core-name → date-field mapping ({@code getDateFieldForCore})</li>
 *   <li>Export-strategy method existence and signatures</li>
 *   <li>CSV document serialisation ({@code writeDocsToCsv})</li>
 * </ul>
 *
 * @author 4Science DSpace Team
 */
public class SolrCoreExportImportTest {

    // ── JUnit 4 rules ──────────────────────────────────────────────────────

    /** Provides a temporary directory that is automatically deleted after each test. */
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // ── Subject ────────────────────────────────────────────────────────────

    /**
     * Bare instance created by Mockito/Objenesis without invoking the real
     * constructor, so {@code DSpaceServicesFactory.getInstance()} is never
     * called.  Only used as the receiver for instance-method reflection calls.
     */
    private SolrCoreExportImport instance;

    // ── Reflected methods ──────────────────────────────────────────────────

    private Method uuidToUnsignedBigIntMethod;
    private Method unsignedBigIntToUuidMethod;
    private Method partitionUuidSpaceMethod;
    private Method escapeCsvValueMethod;
    private Method buildSelectUrlMethod;
    private Method getDateFieldForCoreMethod;
    private Method writeDocsToCsvMethod;

    // ── UuidRange inner-class access ───────────────────────────────────────

    private Class<?> uuidRangeClass;
    private Field    uuidRangeStartField;
    private Field    uuidRangeEndField;

    // ── Jackson helper ─────────────────────────────────────────────────────

    private final ObjectMapper mapper = new ObjectMapper();

    // ── Setup ──────────────────────────────────────────────────────────────

    /**
     * Resolves all reflected handles once before each test.
     *
     * @throws Exception if any method or field cannot be found (signals a
     *                   regression in the production class)
     */
    @Before
    public void setUp() throws Exception {
        // Create a bare instance without triggering DSpace service initialisation.
        instance = Mockito.mock(SolrCoreExportImport.class);

        // ── Private static methods ──────────────────────────────────────────

        uuidToUnsignedBigIntMethod = SolrCoreExportImport.class
                .getDeclaredMethod("uuidToUnsignedBigInt", UUID.class);
        uuidToUnsignedBigIntMethod.setAccessible(true);

        unsignedBigIntToUuidMethod = SolrCoreExportImport.class
                .getDeclaredMethod("unsignedBigIntToUuid", BigInteger.class);
        unsignedBigIntToUuidMethod.setAccessible(true);

        escapeCsvValueMethod = SolrCoreExportImport.class
                .getDeclaredMethod("escapeCsvValue", String.class);
        escapeCsvValueMethod.setAccessible(true);

        buildSelectUrlMethod = SolrCoreExportImport.class
                .getDeclaredMethod("buildSelectUrl",
                        String.class, String.class, String.class,
                        String.class, String.class, int.class);
        buildSelectUrlMethod.setAccessible(true);

        // ── Private instance methods ────────────────────────────────────────

        partitionUuidSpaceMethod = SolrCoreExportImport.class
                .getDeclaredMethod("partitionUuidSpace",
                        String.class, String.class, int.class);
        partitionUuidSpaceMethod.setAccessible(true);

        getDateFieldForCoreMethod = SolrCoreExportImport.class
                .getDeclaredMethod("getDateFieldForCore");
        getDateFieldForCoreMethod.setAccessible(true);

        writeDocsToCsvMethod = SolrCoreExportImport.class
                .getDeclaredMethod("writeDocsToCsv",
                        com.fasterxml.jackson.databind.JsonNode.class,
                        Path.class, boolean.class, List.class);
        writeDocsToCsvMethod.setAccessible(true);

        // ── UuidRange private inner class ───────────────────────────────────

        for (Class<?> inner : SolrCoreExportImport.class.getDeclaredClasses()) {
            if ("UuidRange".equals(inner.getSimpleName())) {
                uuidRangeClass = inner;
                break;
            }
        }
        assertNotNull("UuidRange inner class must exist", uuidRangeClass);

        uuidRangeStartField = uuidRangeClass.getDeclaredField("start");
        uuidRangeStartField.setAccessible(true);
        uuidRangeEndField = uuidRangeClass.getDeclaredField("end");
        uuidRangeEndField.setAccessible(true);
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private BigInteger toUnsignedBigInt(UUID uuid) throws Exception {
        return (BigInteger) uuidToUnsignedBigIntMethod.invoke(null, uuid);
    }

    private UUID toBigIntUuid(BigInteger bi) throws Exception {
        return (UUID) unsignedBigIntToUuidMethod.invoke(null, bi);
    }

    @SuppressWarnings("unchecked")
    private List<Object> partitionUuidSpace(String min, String max, int n) throws Exception {
        return (List<Object>) partitionUuidSpaceMethod.invoke(instance, min, max, n);
    }

    private String escapeCsv(String value) throws Exception {
        return (String) escapeCsvValueMethod.invoke(null, value);
    }

    private String buildSelectUrl(String baseUrl, String query, String fq,
            String sortField, String cursorMark, int rows) throws Exception {
        return (String) buildSelectUrlMethod.invoke(null,
                baseUrl, query, fq, sortField, cursorMark, rows);
    }

    private String getDateFieldForCore(String core) throws Exception {
        Field coreNameField = SolrCoreExportImport.class.getDeclaredField("coreName");
        coreNameField.setAccessible(true);
        coreNameField.set(instance, core);
        return (String) getDateFieldForCoreMethod.invoke(instance);
    }

    private String rangeStart(Object range) throws Exception {
        return (String) uuidRangeStartField.get(range);
    }

    private String rangeEnd(Object range) throws Exception {
        return (String) uuidRangeEndField.get(range);
    }

    // ══════════════════════════════════════════════════════════════════════
    // 1. UUID ↔ unsigned BigInteger conversion
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Zero UUID ({@code 00000000-0000-0000-0000-000000000000}) must map to
     * {@link BigInteger#ZERO}.
     */
    @Test
    public void testUuidToUnsignedBigInt_zeroUuidMapsToZero() throws Exception {
        UUID zeroUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
        assertEquals("Zero UUID must map to BigInteger.ZERO",
                BigInteger.ZERO, toUnsignedBigInt(zeroUuid));
    }

    /**
     * Max UUID ({@code ffffffff-ffff-ffff-ffff-ffffffffffff}) must map to
     * {@code 2^128 − 1}.
     */
    @Test
    public void testUuidToUnsignedBigInt_maxUuidMapsToMaxBigInt() throws Exception {
        UUID maxUuid = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        BigInteger expected = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE);
        assertEquals("Max UUID must map to 2^128 − 1", expected, toUnsignedBigInt(maxUuid));
    }

    /**
     * Any UUID must convert to a non-negative {@link BigInteger}.
     * Verifies the unsigned-correction logic for both MSB-negative and
     * LSB-negative UUIDs (i.e. high bit set in the underlying {@code long}).
     */
    @Test
    public void testUuidToUnsignedBigInt_alwaysNonNegative() throws Exception {
        List<UUID> candidates = List.of(
                // MSB-negative long (high bit of most-significant bits set)
                UUID.fromString("ffffffff-ffff-ffff-0000-000000000000"),
                // LSB-negative long (high bit of least-significant bits set)
                UUID.fromString("00000000-0000-0000-ffff-ffffffffffff"),
                // Both halves negative
                UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"),
                // Typical random UUID
                UUID.fromString("3b4c5d6e-7f80-91a2-b3c4-d5e6f7081920")
        );
        for (UUID uuid : candidates) {
            assertTrue("Result must be non-negative for UUID " + uuid,
                    toUnsignedBigInt(uuid).signum() >= 0);
        }
    }

    /**
     * {@code unsignedBigIntToUuid(BigInteger.ZERO)} must return the zero UUID.
     */
    @Test
    public void testUnsignedBigIntToUuid_zeroProducesZeroUuid() throws Exception {
        assertEquals("BigInteger.ZERO must produce the zero UUID",
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                toBigIntUuid(BigInteger.ZERO));
    }

    /**
     * {@code unsignedBigIntToUuid(2^128 − 1)} must return the max UUID.
     */
    @Test
    public void testUnsignedBigIntToUuid_maxBigIntProducesMaxUuid() throws Exception {
        BigInteger maxBi = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE);
        assertEquals("Max BigInteger must produce the max UUID",
                UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"),
                toBigIntUuid(maxBi));
    }

    /**
     * Full round-trip: {@code uuid → BigInteger → uuid} must recover the
     * original for a representative set of fixed and random UUIDs.
     */
    @Test
    public void testUuidConversion_roundTripPreservesOriginal() throws Exception {
        List<UUID> testUuids = List.of(
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"),
                UUID.fromString("12345678-1234-1234-1234-123456789abc"),
                UUID.fromString("80000000-0000-0000-0000-000000000000"),
                UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffffff"),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        for (UUID original : testUuids) {
            UUID recovered = toBigIntUuid(toUnsignedBigInt(original));
            assertEquals("Round-trip failed for UUID " + original, original, recovered);
        }
    }

    /**
     * Numeric order must be consistent with lexicographic UUID string order
     * for same-length hex UUIDs in the same UUID version.
     * Specifically, a UUID whose first segment is larger must produce a
     * larger {@link BigInteger}.
     */
    @Test
    public void testUuidConversion_numericOrderConsistentWithHexOrder() throws Exception {
        UUID smaller = UUID.fromString("10000000-0000-0000-0000-000000000000");
        UUID larger  = UUID.fromString("90000000-0000-0000-0000-000000000000");
        assertTrue("Larger UUID hex must produce larger BigInteger",
                toUnsignedBigInt(larger).compareTo(toUnsignedBigInt(smaller)) > 0);
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. UUID Space Partitioning
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Partitioning the full UUID space [0, max] into 4 ranges must produce
     * exactly 4 non-overlapping, contiguous ranges where:
     * <ul>
     *   <li>range[0].start == min</li>
     *   <li>range[3].end   == max</li>
     *   <li>range[i].end + 1 == range[i+1].start  (contiguous)</li>
     * </ul>
     */
    @Test
    public void testPartitionUuidSpace_fourNonOverlappingContiguousRanges() throws Exception {
        String min = "00000000-0000-0000-0000-000000000000";
        String max = "ffffffff-ffff-ffff-ffff-ffffffffffff";

        List<Object> ranges = partitionUuidSpace(min, max, 4);

        assertEquals("Must produce exactly 4 partitions", 4, ranges.size());
        assertEquals("First partition must start at min UUID", min, rangeStart(ranges.get(0)));
        assertEquals("Last partition must end at max UUID",   max, rangeEnd(ranges.get(3)));

        for (int i = 0; i < ranges.size() - 1; i++) {
            BigInteger endI     = toUnsignedBigInt(UUID.fromString(rangeEnd(ranges.get(i))));
            BigInteger startNext = toUnsignedBigInt(UUID.fromString(rangeStart(ranges.get(i + 1))));
            BigInteger gap = startNext.subtract(endI);
            assertEquals("Gap between partition " + i + " and " + (i + 1) + " must be exactly 1",
                    BigInteger.ONE, gap);
        }
    }

    /**
     * n=1 must produce a single partition that covers the full requested range.
     */
    @Test
    public void testPartitionUuidSpace_singlePartitionCoversEntireRange() throws Exception {
        String min = "10000000-0000-0000-0000-000000000000";
        String max = "f0000000-0000-0000-0000-000000000000";

        List<Object> ranges = partitionUuidSpace(min, max, 1);

        assertEquals("Must produce exactly 1 partition", 1, ranges.size());
        assertEquals("Single partition must start at min", min, rangeStart(ranges.get(0)));
        assertEquals("Single partition must end at max",   max, rangeEnd(ranges.get(0)));
    }

    /**
     * All partition boundaries must be valid UUID strings parseable with
     * {@link UUID#fromString(String)}.
     */
    @Test
    public void testPartitionUuidSpace_allBoundariesAreValidUuids() throws Exception {
        String min = "00000000-0000-0000-0000-000000000000";
        String max = "ffffffff-ffff-ffff-ffff-ffffffffffff";

        List<Object> ranges = partitionUuidSpace(min, max, 8);

        for (int i = 0; i < ranges.size(); i++) {
            // UUID.fromString throws IllegalArgumentException on invalid input
            try {
                UUID.fromString(rangeStart(ranges.get(i)));
                UUID.fromString(rangeEnd(ranges.get(i)));
            } catch (IllegalArgumentException e) {
                fail("Partition " + i + " boundary is not a valid UUID: " + e.getMessage());
            }
        }
    }

    /**
     * Non-UUID key values (e.g. DSpace-prefixed IDs used by the search core)
     * must cause the method to throw {@link IllegalArgumentException}.
     */
    @Test
    public void testPartitionUuidSpace_nonUuidKeyThrowsIllegalArgumentException() throws Exception {
        try {
            partitionUuidSpace("Collection-abc123", "XmlWorkflowItem-xyz789", 4);
            fail("Expected IllegalArgumentException for non-UUID keys");
        } catch (InvocationTargetException ite) {
            assertTrue("Root cause must be IllegalArgumentException",
                    ite.getCause() instanceof IllegalArgumentException);
        }
    }

    /**
     * Nearly-identical UUIDs (differing by 1 in the least-significant bit)
     * must not cause an exception and must produce at least one valid range.
     */
    @Test
    public void testPartitionUuidSpace_nearlyIdenticalUuidsProduceAtLeastOneRange() throws Exception {
        String min = "10000000-0000-0000-0000-000000000000";
        String max = "10000000-0000-0000-0000-000000000001";

        List<Object> ranges = partitionUuidSpace(min, max, 2);

        assertTrue("Must produce at least 1 partition", ranges.size() >= 1);
        for (Object r : ranges) {
            assertNotNull(UUID.fromString(rangeStart(r)));
            assertNotNull(UUID.fromString(rangeEnd(r)));
        }
    }

    /**
     * The union of all partition ranges must exactly cover every document
     * between min and max (no gaps, no overlap — verified numerically).
     */
    @Test
    public void testPartitionUuidSpace_rangesCoverFullSpaceWithNoGaps() throws Exception {
        String min = "00000000-0000-0000-0000-000000000000";
        String max = "ffffffff-ffff-ffff-ffff-ffffffffffff";
        int n = 7; // prime number to stress the remainder-handling

        List<Object> ranges = partitionUuidSpace(min, max, n);

        BigInteger totalSpan = toUnsignedBigInt(UUID.fromString(max))
                .subtract(toUnsignedBigInt(UUID.fromString(min)))
                .add(BigInteger.ONE);

        BigInteger sum = BigInteger.ZERO;
        for (Object r : ranges) {
            BigInteger lo = toUnsignedBigInt(UUID.fromString(rangeStart(r)));
            BigInteger hi = toUnsignedBigInt(UUID.fromString(rangeEnd(r)));
            sum = sum.add(hi.subtract(lo).add(BigInteger.ONE));
        }

        assertEquals("Sum of all partition sizes must equal total span", totalSpan, sum);
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. buildSelectUrl
    // ══════════════════════════════════════════════════════════════════════

    /**
     * The generated URL must contain all mandatory Solr parameters.
     */
    @Test
    public void testBuildSelectUrl_containsAllRequiredParameters() throws Exception {
        String url = buildSelectUrl(
                "http://localhost:8983/solr/statistics",
                "*:*", null, "uid", "*", 10000);

        assertTrue("URL must contain /select path",        url.contains("/select"));
        assertTrue("URL must contain q= parameter",        url.contains("q="));
        assertTrue("URL must contain sort= parameter",     url.contains("sort="));
        assertTrue("URL must contain cursorMark= parameter", url.contains("cursorMark="));
        assertTrue("URL must contain rows=10000",          url.contains("rows=10000"));
        assertTrue("URL must contain wt=json",             url.contains("wt=json"));
    }

    /**
     * When a filter query is provided, {@code fq=} must appear in the URL.
     */
    @Test
    public void testBuildSelectUrl_withFilterQueryIncludesFqParam() throws Exception {
        String url = buildSelectUrl(
                "http://localhost:8983/solr/statistics",
                "*:*", "uid:[00000000 TO ffffffff]", "uid", "*", 5000);

        assertTrue("fq= must be present when filter query is provided", url.contains("fq="));
    }

    /**
     * When the filter query is {@code null}, {@code fq=} must be absent.
     */
    @Test
    public void testBuildSelectUrl_withoutFilterQueryOmitsFqParam() throws Exception {
        String url = buildSelectUrl(
                "http://localhost:8983/solr/statistics",
                "*:*", null, "uid", "*", 5000);

        assertFalse("fq= must be absent when filter query is null", url.contains("fq="));
    }

    /**
     * The sort direction must be ascending ({@code asc}) as required by cursorMark.
     */
    @Test
    public void testBuildSelectUrl_sortIsAscending() throws Exception {
        String url = buildSelectUrl(
                "http://localhost:8983/solr/statistics",
                "*:*", null, "uid", "*", 10000);

        assertTrue("Sort must be ascending (cursorMark requirement)", url.contains("asc"));
    }

    /**
     * A cursorMark containing special characters (e.g. {@code +}, {@code =},
     * {@code /}) must be percent-encoded, not appear verbatim after
     * {@code cursorMark=}.
     */
    @Test
    public void testBuildSelectUrl_cursorMarkIsUrlEncoded() throws Exception {
        String rawCursorMark = "AoE=foo/bar+baz==";
        String url = buildSelectUrl(
                "http://localhost:8983/solr/statistics",
                "*:*", null, "uid", rawCursorMark, 1000);

        // The raw, unencoded string must not appear verbatim
        assertFalse("Raw cursorMark must be URL-encoded in the URL",
                url.contains("cursorMark=" + rawCursorMark));
        // The cursorMark parameter key must still be present
        assertTrue("cursorMark= parameter key must be present", url.contains("cursorMark="));
    }

    /**
     * The base URL must be used as the URL prefix.
     */
    @Test
    public void testBuildSelectUrl_preservesBaseUrl() throws Exception {
        String baseUrl = "http://prod-solr:8983/solr/statistics";
        String url = buildSelectUrl(baseUrl, "*:*", null, "uid", "*", 10000);

        assertTrue("URL must start with the provided base URL", url.startsWith(baseUrl));
    }

    // ══════════════════════════════════════════════════════════════════════
    // 4. escapeCsvValue  (RFC 4180)
    // ══════════════════════════════════════════════════════════════════════

    /** Plain values without special characters must pass through unchanged. */
    @Test
    public void testEscapeCsvValue_plainValue() throws Exception {
        assertEquals("hello", escapeCsv("hello"));
        assertEquals("123",   escapeCsv("123"));
        assertEquals("a b c", escapeCsv("a b c"));
    }

    /** Values containing a comma must be double-quoted. */
    @Test
    public void testEscapeCsvValue_valueWithComma() throws Exception {
        assertEquals("\"a,b\"", escapeCsv("a,b"));
        assertEquals("\"first,second,third\"", escapeCsv("first,second,third"));
    }

    /** Internal double-quotes must be doubled and the value must be wrapped in quotes. */
    @Test
    public void testEscapeCsvValue_valueWithDoubleQuote() throws Exception {
        assertEquals("\"he said \"\"hi\"\"\"", escapeCsv("he said \"hi\""));
        assertEquals("\"\"\"only quotes\"\"\"", escapeCsv("\"only quotes\""));
    }

    /** Values containing a line-feed must be double-quoted. */
    @Test
    public void testEscapeCsvValue_valueWithLineFeed() throws Exception {
        assertEquals("\"line1\nline2\"", escapeCsv("line1\nline2"));
    }

    /** Values containing a carriage-return must be double-quoted. */
    @Test
    public void testEscapeCsvValue_valueWithCarriageReturn() throws Exception {
        assertEquals("\"line1\rline2\"", escapeCsv("line1\rline2"));
    }

    /** {@code null} must be treated as an empty string. */
    @Test
    public void testEscapeCsvValue_nullProducesEmptyString() throws Exception {
        assertEquals("", escapeCsv(null));
    }

    /** An empty string must be returned as-is (no quoting). */
    @Test
    public void testEscapeCsvValue_emptyStringPassesThrough() throws Exception {
        assertEquals("", escapeCsv(""));
    }

    /** A value that is just a single comma must be double-quoted. */
    @Test
    public void testEscapeCsvValue_onlyCommaIsQuoted() throws Exception {
        assertEquals("\",\"", escapeCsv(","));
    }

    /** A value combining comma and double-quote must be handled correctly. */
    @Test
    public void testEscapeCsvValue_commaAndDoubleQuoteCombined() throws Exception {
        // Input:  a,"b"
        // Output: "a,""b"""
        assertEquals("\"a,\"\"b\"\"\"", escapeCsv("a,\"b\""));
    }

    // ══════════════════════════════════════════════════════════════════════
    // 5. getDateFieldForCore
    // ══════════════════════════════════════════════════════════════════════

    /** {@code statistics} core must use the {@code time} date field. */
    @Test
    public void testGetDateFieldForCore_statisticsUsesTimeField() throws Exception {
        assertEquals("time", getDateFieldForCore("statistics"));
    }

    /** {@code audit} core must use the {@code timeStamp} date field. */
    @Test
    public void testGetDateFieldForCore_auditUsesTimeStampField() throws Exception {
        assertEquals("timeStamp", getDateFieldForCore("audit"));
    }

    /** Any unknown core must fall back to {@code lastModified}. */
    @Test
    public void testGetDateFieldForCore_unknownCoreFallsBackToLastModified() throws Exception {
        assertEquals("lastModified", getDateFieldForCore("search"));
        assertEquals("lastModified", getDateFieldForCore("oai"));
        assertEquals("lastModified", getDateFieldForCore("dedup"));
        assertEquals("lastModified", getDateFieldForCore("qaevent"));
        assertEquals("lastModified", getDateFieldForCore("authority"));
        assertEquals("lastModified", getDateFieldForCore("ocr"));
        assertEquals("lastModified", getDateFieldForCore("suggestion"));
    }

    // ══════════════════════════════════════════════════════════════════════
    // 6. Export strategy method existence and signatures
    // ══════════════════════════════════════════════════════════════════════

    /**
     * All four export-strategy methods must exist, be private, and accept
     * exactly one {@link String} parameter (the Solr base URL).
     */
    @Test
    public void testStrategyDispatch_allStrategyMethodsExistWithCorrectSignature()
            throws Exception {
        String[] methods = {
            "exportCoreWithUuidRanges",
            "exportCoreWithCursorMark",
            "exportCoreWithDateRanges",
            "exportCoreAuto"
        };

        for (String methodName : methods) {
            Method m = SolrCoreExportImport.class.getDeclaredMethod(methodName, String.class);
            assertNotNull("Method must exist: " + methodName, m);
            assertEquals(
                    "Method " + methodName + " must accept exactly 1 parameter",
                    1, m.getParameterTypes().length);
            assertEquals(
                    "Method " + methodName + " parameter must be String",
                    String.class, m.getParameterTypes()[0]);
        }
    }

    /**
     * The {@code partitionUuidSpace} helper must exist with the expected
     * three-parameter signature.
     */
    @Test
    public void testStrategyDispatch_partitionUuidSpaceMethodHasCorrectSignature()
            throws Exception {
        Method m = SolrCoreExportImport.class.getDeclaredMethod(
                "partitionUuidSpace", String.class, String.class, int.class);
        assertNotNull(m);
        Class<?>[] params = m.getParameterTypes();
        assertEquals(3, params.length);
        assertEquals(String.class, params[0]);
        assertEquals(String.class, params[1]);
        assertEquals(int.class,    params[2]);
    }

    // ══════════════════════════════════════════════════════════════════════
    // 7. writeDocsToCsv — CSV serialisation
    // ══════════════════════════════════════════════════════════════════════

    /**
     * A single-document batch (first batch) must produce a CSV file with one
     * header row followed by one data row.
     */
    @Test
    public void testWriteDocsToCsv_firstBatchWritesHeaderAndDataRow() throws Exception {
        Path outFile = tempFolder.newFile("out.csv").toPath();

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("uid",   "00000000-0000-0000-0000-000000000001");
        doc.put("title", "Test title");
        docs.add(doc);

        List<String> fields = List.of("uid", "title");
        writeDocsToCsvMethod.invoke(instance, docs, outFile, false, fields);

        List<String> lines = Files.readAllLines(outFile);
        assertEquals("First batch must produce exactly 2 lines (header + 1 row)", 2, lines.size());
        assertEquals("uid,title", lines.get(0));
        assertEquals("00000000-0000-0000-0000-000000000001,Test title", lines.get(1));
    }

    /**
     * A subsequent batch (appendMode=true) must append data rows without
     * repeating the header.
     */
    @Test
    public void testWriteDocsToCsv_subsequentBatchAppendsWithoutHeader() throws Exception {
        Path outFile = tempFolder.newFile("out.csv").toPath();
        List<String> fields = List.of("uid", "title");

        // First batch
        ArrayNode batch1 = mapper.createArrayNode();
        ObjectNode doc1 = mapper.createObjectNode();
        doc1.put("uid",   "00000000-0000-0000-0000-000000000001");
        doc1.put("title", "Doc one");
        batch1.add(doc1);
        writeDocsToCsvMethod.invoke(instance, batch1, outFile, false, fields);

        // Second batch (append)
        ArrayNode batch2 = mapper.createArrayNode();
        ObjectNode doc2 = mapper.createObjectNode();
        doc2.put("uid",   "00000000-0000-0000-0000-000000000002");
        doc2.put("title", "Doc two");
        batch2.add(doc2);
        writeDocsToCsvMethod.invoke(instance, batch2, outFile, true, fields);

        List<String> lines = Files.readAllLines(outFile);
        assertEquals("Two batches must produce 3 lines total (1 header + 2 rows)", 3, lines.size());
        assertEquals("uid,title",                                     lines.get(0));
        assertEquals("00000000-0000-0000-0000-000000000001,Doc one",  lines.get(1));
        assertEquals("00000000-0000-0000-0000-000000000002,Doc two",  lines.get(2));
    }

    /**
     * Multi-valued JSON array fields must be joined with a comma and the
     * combined value must be quoted (because the internal comma would otherwise
     * break CSV parsing).
     */
    @Test
    public void testWriteDocsToCsv_multivaluedFieldIsJoinedAndQuoted() throws Exception {
        Path outFile = tempFolder.newFile("out.csv").toPath();
        List<String> fields = List.of("uid", "tags");

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("uid", "00000000-0000-0000-0000-000000000003");
        doc.putArray("tags").add("alpha").add("beta").add("gamma");
        docs.add(doc);

        writeDocsToCsvMethod.invoke(instance, docs, outFile, false, fields);

        List<String> lines = Files.readAllLines(outFile);
        assertEquals(2, lines.size());
        // Multi-valued "alpha,beta,gamma" contains commas → must be quoted
        assertEquals("uid,tags", lines.get(0));
        assertEquals("00000000-0000-0000-0000-000000000003,\"alpha,beta,gamma\"", lines.get(1));
    }

    /**
     * A field absent from the document must produce an empty CSV cell (not an
     * error).
     */
    @Test
    public void testWriteDocsToCsv_missingFieldProducesEmptyCell() throws Exception {
        Path outFile = tempFolder.newFile("out.csv").toPath();
        List<String> fields = List.of("uid", "missing_field");

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("uid", "00000000-0000-0000-0000-000000000004");
        // "missing_field" is intentionally absent
        docs.add(doc);

        writeDocsToCsvMethod.invoke(instance, docs, outFile, false, fields);

        List<String> lines = Files.readAllLines(outFile);
        assertEquals(2, lines.size());
        assertEquals("uid,missing_field", lines.get(0));
        assertEquals("00000000-0000-0000-0000-000000000004,", lines.get(1));
    }

    /**
     * A value containing a comma must be double-quoted in the output CSV so
     * that it parses as a single cell.
     */
    @Test
    public void testWriteDocsToCsv_valueWithCommaIsQuoted() throws Exception {
        Path outFile = tempFolder.newFile("out.csv").toPath();
        List<String> fields = List.of("uid", "title");

        ArrayNode docs = mapper.createArrayNode();
        ObjectNode doc = mapper.createObjectNode();
        doc.put("uid",   "00000000-0000-0000-0000-000000000005");
        doc.put("title", "Hello, World");
        docs.add(doc);

        writeDocsToCsvMethod.invoke(instance, docs, outFile, false, fields);

        List<String> lines = Files.readAllLines(outFile);
        assertEquals(2, lines.size());
        assertEquals("uid,title", lines.get(0));
        assertEquals("00000000-0000-0000-0000-000000000005,\"Hello, World\"", lines.get(1));
    }
}
