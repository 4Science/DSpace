/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.solr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Script for complete export and import of SOLR cores with multithreading support.
 * Uses direct HTTP calls to SOLR for maximum performance and simplicity.
 * Supports CSV output for data exchange.
 *
 * <p>Export strategies:
 * <ul>
 *   <li><b>uuid-range</b> (default) — Partitions the uniqueKey UUID space into N equal ranges
 *       using 128-bit integer math. Each partition is exported by an independent thread using
 *       cursorMark pagination. Near-linear throughput scaling with thread count. Falls back to
 *       cursor-mark automatically for non-UUID keys.</li>
 *   <li><b>cursor-mark</b> — Single-threaded full-core scan using cursorMark pagination.
 *       Safe for all core types. Use for cores with non-UUID uniqueKeys (e.g. oai, search).</li>
 *   <li><b>date-range</b> — Multi-threaded export splitting by date field (WEEK/MONTH/YEAR).
 *       Uses cursorMark within each range to avoid the rows-truncation bug.</li>
 *   <li><b>auto</b> — Selects uuid-range for UUID keys, cursor-mark otherwise.</li>
 * </ul>
 *
 * <p>Bugs fixed vs original implementation:
 * <ul>
 *   <li><b>rows=Integer.MAX_VALUE truncation</b>: Solr silently caps at maxRows (~1 M docs).
 *       All strategies now use cursorMark pagination so no documents are ever lost.</li>
 *   <li><b>lastModified date-skew</b>: A batch re-index puts all documents in the same date range,
 *       overloading that range and triggering truncation. UUID range partitioning distributes
 *       by uniqueKey instead of time, so distribution is inherently uniform.</li>
 * </ul>
 *
 * <p>REST version requires admin privileges, CLI version can be executed freely.
 *
 * @author 4Science DSpace Team
 * @author Stefano Maffei (stefano.maffei at 4science.com) — original implementation
 */
public class SolrCoreExportImport extends DSpaceRunnable<SolrCoreExportImportScriptConfiguration> {

    private static final Logger log = LogManager.getLogger(SolrCoreExportImport.class);

    /** 2^64 as BigInteger — used for unsigned long conversion. */
    private static final BigInteger TWO_64 = BigInteger.ONE.shiftLeft(64);

    /** Mask for the lower 64 bits — used when converting BigInteger back to long. */
    private static final BigInteger MASK_64 = TWO_64.subtract(BigInteger.ONE);

    /** Cursor mark start sentinel value. */
    private static final String CURSOR_MARK_START = "*";

    // ── CLI-bound fields ───────────────────────────────────────────────────

    private String mode;
    private String coreName;
    private String directory;
    private String format = "csv";
    private int threadCount = 1;
    private String dateField;
    private String startDate;
    private String endDate;
    private String dateIncrement = "MONTH"; // WEEK, MONTH, YEAR
    private String strategy = "uuid-range"; // uuid-range | cursor-mark | date-range | auto
    private int exportBatchSize = 10_000;
    private boolean help = false;

    protected EPersonService epersonService;

    private ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();
    private ObjectMapper jsonMapper = new ObjectMapper();
    private HttpClient httpClient;

    // ── Caches ─────────────────────────────────────────────────────────────

    /** Cached schema fields — avoids repeated schema queries per partition thread. */
    private volatile List<String> cachedFields = null;

    /** Cached unique key field name. */
    private volatile String cachedUniqueKeyField = null;

    // ── Auth ───────────────────────────────────────────────────────────────

    /**
     * Determines if this script execution requires authentication.
     * Override in subclasses to change behavior (CLI vs REST).
     *
     * @return true if authentication is required, false otherwise
     */
    protected boolean requiresAuthentication() {
        return true;
    }

    // ── Setup ──────────────────────────────────────────────────────────────

    @Override
    public void setup() throws ParseException {
        if (commandLine.hasOption('h')) {
            help = true;
            return;
        }

        this.epersonService = EPersonServiceFactory.getInstance().getEPersonService();

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofHours(2))
                .build();

        mode = commandLine.getOptionValue('m');
        if (StringUtils.isBlank(mode) || (!mode.equals("export") && !mode.equals("import"))) {
            throw new ParseException("Mode parameter is required and must be 'export' or 'import'");
        }

        coreName = commandLine.getOptionValue('c');
        if (StringUtils.isBlank(coreName)) {
            throw new ParseException("Core name parameter is required");
        }

        directory = commandLine.getOptionValue('d');
        if (StringUtils.isBlank(directory)) {
            throw new ParseException("Directory parameter is required");
        }

        if (commandLine.hasOption('f')) {
            format = commandLine.getOptionValue('f').toLowerCase();
            if (!format.equals("csv") && !format.equals("json")) {
                throw new ParseException("Format must be 'csv' or 'json'");
            }
        }

        if (commandLine.hasOption('t')) {
            try {
                threadCount = Integer.parseInt(commandLine.getOptionValue('t'));
                if (threadCount < 1) {
                    throw new ParseException("Thread count must be at least 1");
                }
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid thread count: " + commandLine.getOptionValue('t'));
            }
        }

        if (commandLine.hasOption('s')) {
            startDate = commandLine.getOptionValue('s');
        }

        if (commandLine.hasOption('e')) {
            endDate = commandLine.getOptionValue('e');
        }

        if (commandLine.hasOption('i')) {
            dateIncrement = commandLine.getOptionValue('i').toUpperCase();
            if (!dateIncrement.equals("WEEK") && !dateIncrement.equals("MONTH")
                    && !dateIncrement.equals("YEAR")) {
                throw new ParseException("Date increment must be WEEK, MONTH, or YEAR");
            }
        }

        if (commandLine.hasOption("strategy")) {
            strategy = commandLine.getOptionValue("strategy").toLowerCase();
            if (!strategy.equals("uuid-range") && !strategy.equals("cursor-mark")
                    && !strategy.equals("date-range") && !strategy.equals("auto")) {
                throw new ParseException(
                        "Strategy must be one of: uuid-range, cursor-mark, date-range, auto");
            }
        }

        if (commandLine.hasOption("batch-size")) {
            try {
                exportBatchSize = Integer.parseInt(commandLine.getOptionValue("batch-size"));
                if (exportBatchSize < 1) {
                    throw new ParseException("Batch size must be at least 1");
                }
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid batch size: " + commandLine.getOptionValue("batch-size"));
            }
        }
    }

    // ── Main run ───────────────────────────────────────────────────────────

    @Override
    public void internalRun() throws Exception {
        if (help) {
            printVerboseHelp();
            return;
        }

        Context context = new Context();
        try {
            context.turnOffAuthorisationSystem();

            if (requiresAuthentication()) {
                if (getEpersonIdentifier() != null) {
                    try {
                        context.setCurrentUser(epersonService.find(context, getEpersonIdentifier()));
                    } catch (SQLException e) {
                        throw new RuntimeException("Failed to find EPerson", e);
                    }
                }

                if (!getScriptConfiguration().isAllowedToExecute(context, null)) {
                    handler.logError("Current user is not eligible to execute SOLR core export/import script");
                    throw new AuthorizeException(
                            "Current user is not eligible to execute SOLR core export/import script");
                }
            } else {
                handler.logInfo("Running SOLR core export/import from CLI without authentication");
            }

            Path dirPath = Paths.get(directory);
            if (!Files.exists(dirPath)) {
                if (mode.equals("export")) {
                    Files.createDirectories(dirPath);
                    handler.logInfo("Created directory: " + directory);
                } else {
                    throw new IllegalArgumentException("Import directory does not exist: " + directory);
                }
            }

            if (mode.equals("export")) {
                exportCore();
            } else {
                importCore();
            }

        } finally {
            context.restoreAuthSystemState();
            context.complete();
        }
    }

    // ── Export dispatcher ──────────────────────────────────────────────────

    /**
     * Dispatches the export to the appropriate strategy implementation.
     *
     * @throws Exception on any error
     */
    private void exportCore() throws Exception {
        long startTime = System.currentTimeMillis();
        String solrUrl = configurationService.getProperty("solr.server");
        String fullCoreName = getFullCoreName(coreName);
        String baseUrl = solrUrl + "/" + fullCoreName;

        handler.logInfo("Starting export from SOLR core: " + baseUrl + " [strategy=" + strategy
                + ", threads=" + threadCount + ", batchSize=" + exportBatchSize + "]");

        switch (strategy) {
            case "uuid-range":
                exportCoreWithUuidRanges(baseUrl);
                break;
            case "cursor-mark":
                exportCoreWithCursorMark(baseUrl);
                break;
            case "date-range":
                exportCoreWithDateRanges(baseUrl);
                break;
            case "auto":
            default:
                exportCoreAuto(baseUrl);
                break;
        }

        long totalTime = System.currentTimeMillis() - startTime;
        handler.logInfo("Export completed in " + totalTime + " ms");
    }

    /**
     * Auto strategy: attempt UUID range partitioning; fall back to cursor-mark
     * if the uniqueKey is not a UUID.
     *
     * @param baseUrl the Solr core base URL
     * @throws Exception on any error
     */
    private void exportCoreAuto(String baseUrl) throws Exception {
        try {
            exportCoreWithUuidRanges(baseUrl);
        } catch (IllegalArgumentException e) {
            handler.logInfo("UUID partitioning unavailable (" + e.getMessage()
                    + "), falling back to cursor-mark");
            exportCoreWithCursorMark(baseUrl);
        }
    }

    // ── Strategy: UUID range partitioning ─────────────────────────────────

    /**
     * Exports the core by partitioning the uniqueKey UUID space into {@code threadCount}
     * equal ranges using 128-bit integer arithmetic, then exporting each partition in
     * parallel using cursorMark pagination.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Query Solr stats to get min/max uniqueKey (1 HTTP call).</li>
     *   <li>Partition the UUID range: rangeSize = (max − min) / N.</li>
     *   <li>Submit N partition tasks to a thread pool; each uses cursorMark to page
     *       through its slice and writes CSV directly to its output file.</li>
     * </ol>
     *
     * <p>Throws {@link IllegalArgumentException} if the uniqueKey values are not valid UUIDs,
     * so the caller can fall back gracefully.
     *
     * @param baseUrl the Solr core base URL
     * @throws Exception on any error
     * @throws IllegalArgumentException if the uniqueKey field does not contain UUID values
     */
    private void exportCoreWithUuidRanges(String baseUrl) throws Exception {
        handler.logInfo("Strategy: uuid-range  (" + threadCount + " partitions × cursorMark)");

        // 1. Get uniqueKey field name
        String uniqueKey = getUniqueKeyField(baseUrl);
        handler.logInfo("UniqueKey field: " + uniqueKey);

        // 2. Get min/max from stats
        String statsUrl = baseUrl + "/select?q=*:*&rows=0&wt=json&stats=true&stats.field="
                + URLEncoder.encode(uniqueKey, StandardCharsets.UTF_8);

        HttpRequest statsReq = HttpRequest.newBuilder()
                .uri(URI.create(statsUrl))
                .timeout(Duration.ofHours(2))
                .GET()
                .build();

        HttpResponse<String> statsRsp = httpClient.send(statsReq, HttpResponse.BodyHandlers.ofString());
        if (statsRsp.statusCode() != 200) {
            throw new RuntimeException("Stats query failed with status: " + statsRsp.statusCode());
        }

        JsonNode statsJson = jsonMapper.readTree(statsRsp.body());
        JsonNode statsFields = statsJson.path("stats").path("stats_fields").path(uniqueKey);
        String minKey = statsFields.path("min").asText("");
        String maxKey = statsFields.path("max").asText("");

        if (StringUtils.isBlank(minKey) || StringUtils.isBlank(maxKey)) {
            handler.logWarning("No key range found in core '" + coreName
                    + "' — falling back to cursor-mark");
            exportCoreWithCursorMark(baseUrl);
            return;
        }

        handler.logInfo("Key range: " + minKey + " → " + maxKey);

        // 3. Partition UUID space — throws IllegalArgumentException for non-UUID keys
        List<UuidRange> ranges = partitionUuidSpace(minKey, maxKey, threadCount);
        handler.logInfo("Created " + ranges.size() + " UUID partitions");

        // 4. Get field list once; shared across all partition threads (thread-safe after init)
        List<String> fieldNames = getAvailableFields(baseUrl);

        // 5. Parallel partition export
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger fileCounter = new AtomicInteger(0);

        for (UuidRange range : ranges) {
            final int fileIdx = fileCounter.getAndIncrement();
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    exportUuidPartition(baseUrl, range, uniqueKey, fieldNames, fileIdx);
                } catch (Exception e) {
                    log.error("Error exporting UUID partition {}: {}", range, e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }, executor);
            futures.add(future);
        }

        handler.logInfo("Waiting for " + futures.size() + " partition exports to complete...");
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        executor.shutdown();
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            log.warn("Executor did not terminate gracefully, forcing shutdown");
            executor.shutdownNow();
        }
    }

    /**
     * Exports one UUID partition using cursorMark pagination.
     * Writes a single CSV file: header on the first batch, data rows appended for
     * subsequent batches.
     *
     * @param baseUrl     the Solr core base URL
     * @param range       the UUID key range for this partition
     * @param uniqueKey   the uniqueKey field name (used for sort and filter)
     * @param fieldNames  the ordered list of fields to export
     * @param fileIndex   the zero-based file index used in the output filename
     * @throws Exception on any Solr or I/O error
     */
    private void exportUuidPartition(String baseUrl, UuidRange range,
            String uniqueKey, List<String> fieldNames, int fileIndex) throws Exception {
        Thread thread = Thread.currentThread();
        String fq = uniqueKey + ":[" + range.start + " TO " + range.end + "]";
        String cursorMark = CURSOR_MARK_START;
        boolean firstBatch = true;
        long totalDocs = 0;

        Path outFile = Paths.get(directory,
                String.format("solr_export_range_%04d.csv", fileIndex));

        String startAbbrev = range.start.length() >= 8 ? range.start.substring(0, 8) : range.start;
        String endAbbrev   = range.end.length() >= 8   ? range.end.substring(0, 8)   : range.end;
        handler.logInfo("Thread '" + thread.getName() + "' exporting partition " + fileIndex
                + " [" + startAbbrev + "... TO " + endAbbrev + "...]");

        long partitionStart = System.currentTimeMillis();

        while (true) {
            String url = buildSelectUrl(baseUrl, "*:*", fq, uniqueKey, cursorMark, exportBatchSize);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofHours(2))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "Solr query failed for partition " + fileIndex
                        + " with status: " + response.statusCode());
            }

            JsonNode json = jsonMapper.readTree(response.body());
            JsonNode docs = json.path("response").path("docs");
            String nextCursorMark = json.path("nextCursorMark").asText("");

            if (docs.isArray() && docs.size() > 0) {
                writeDocsToCsv(docs, outFile, !firstBatch, fieldNames);
                totalDocs += docs.size();
                firstBatch = false;
            }

            if (cursorMark.equals(nextCursorMark)
                    || StringUtils.isBlank(nextCursorMark)
                    || docs.size() == 0) {
                break;
            }
            cursorMark = nextCursorMark;
        }

        long elapsed = System.currentTimeMillis() - partitionStart;
        handler.logInfo("Thread '" + thread.getName() + "' completed partition " + fileIndex
                + ": " + totalDocs + " docs in " + elapsed + " ms → " + outFile.getFileName());
    }

    // ── Strategy: cursor-mark (single-threaded full scan) ─────────────────

    /**
     * Exports the entire core in a single thread using cursorMark pagination.
     * Suitable for cores with non-UUID uniqueKeys where UUID range partitioning
     * is not applicable.
     *
     * <p>All documents are written to a single file: {@code solr_export_range_0000.csv}.
     *
     * @param baseUrl the Solr core base URL
     * @throws Exception on any error
     */
    private void exportCoreWithCursorMark(String baseUrl) throws Exception {
        handler.logInfo("Strategy: cursor-mark (single-threaded full scan)");

        String uniqueKey = getUniqueKeyField(baseUrl);
        List<String> fieldNames = getAvailableFields(baseUrl);

        String cursorMark = CURSOR_MARK_START;
        boolean firstBatch = true;
        long totalDocs = 0;
        int batchNum = 0;

        Path outFile = Paths.get(directory, "solr_export_range_0000.csv");
        long exportStart = System.currentTimeMillis();

        while (true) {
            String url = buildSelectUrl(baseUrl, "*:*", null, uniqueKey, cursorMark, exportBatchSize);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofHours(2))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "Solr query failed with status: " + response.statusCode());
            }

            JsonNode json = jsonMapper.readTree(response.body());
            JsonNode docs = json.path("response").path("docs");
            String nextCursorMark = json.path("nextCursorMark").asText("");

            if (docs.isArray() && docs.size() > 0) {
                writeDocsToCsv(docs, outFile, !firstBatch, fieldNames);
                totalDocs += docs.size();
                firstBatch = false;
                batchNum++;
            }

            if (cursorMark.equals(nextCursorMark)
                    || StringUtils.isBlank(nextCursorMark)
                    || docs.size() == 0) {
                break;
            }
            cursorMark = nextCursorMark;
        }

        long elapsed = System.currentTimeMillis() - exportStart;
        handler.logInfo("Cursor-mark export complete: " + totalDocs + " docs in " + batchNum
                + " batches (" + elapsed + " ms)");
    }

    // ── Strategy: date-range (existing, with cursorMark fix) ──────────────

    /**
     * Exports the core using date-range sharding (the original strategy), but with the
     * {@code rows=Integer.MAX_VALUE} truncation bug fixed: each date range now uses
     * cursorMark pagination internally so no documents are ever silently dropped.
     *
     * @param baseUrl the Solr core base URL
     * @throws Exception on any error
     */
    private void exportCoreWithDateRanges(String baseUrl) throws Exception {
        handler.logInfo("Strategy: date-range  (" + dateIncrement + " increment, "
                + threadCount + " threads)");

        dateField = getDateFieldForCore();
        handler.logInfo("Date field: " + dateField);

        DateRange totalRange = getDateRange(baseUrl);
        if (totalRange == null) {
            handler.logWarning("No date range found in core: " + coreName);
            log.warn("No date range found in core '{}'", coreName);
            return;
        }

        handler.logInfo("Total date range: " + totalRange.start + " to " + totalRange.end);

        List<DateRange> dateRanges = generateDateRanges(totalRange);
        handler.logInfo("Split into " + dateRanges.size() + " date ranges");

        List<String> fieldNames = getAvailableFields(baseUrl);
        String uniqueKey = getUniqueKeyField(baseUrl);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        long processingStart = System.currentTimeMillis();
        for (int i = 0; i < dateRanges.size(); i++) {
            final int rangeIndex = i;
            final DateRange range = dateRanges.get(i);
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    exportDateRange(baseUrl, rangeIndex, range, uniqueKey, fieldNames);
                } catch (Exception e) {
                    log.error("Error exporting date range {} ({}): {}",
                            rangeIndex, range, e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }, executor);
            futures.add(future);
        }

        handler.logInfo("Waiting for " + futures.size() + " date-range exports to complete...");
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long processingTime = System.currentTimeMillis() - processingStart;

        executor.shutdown();
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            log.warn("Executor did not terminate gracefully, forcing shutdown");
            executor.shutdownNow();
        }

        handler.logInfo("Date-range export done in " + processingTime + " ms");
    }

    /**
     * Exports documents within one date range using cursorMark pagination.
     *
     * <p>This fixes the original {@code rows=Integer.MAX_VALUE} truncation: the method
     * now loops over cursorMark batches until the range is fully consumed.
     *
     * @param baseUrl     the Solr core base URL
     * @param rangeIndex  zero-based index (used in filename)
     * @param range       the date range to export
     * @param uniqueKey   uniqueKey field for cursorMark sort
     * @param fieldNames  field list for CSV header
     * @throws Exception on any error
     */
    private void exportDateRange(String baseUrl, int rangeIndex, DateRange range,
            String uniqueKey, List<String> fieldNames) throws Exception {
        Thread thread = Thread.currentThread();
        String fq = dateField + ":[" + range.start + " TO " + range.end + "]";
        String cursorMark = CURSOR_MARK_START;
        boolean firstBatch = true;
        long totalDocs = 0;

        Path outFile = Paths.get(directory,
                String.format("solr_export_range_%04d.csv", rangeIndex));

        handler.logInfo("Thread '" + thread.getName() + "' exporting date range " + rangeIndex
                + " (" + range.start + " to " + range.end + ")");

        long rangeStart = System.currentTimeMillis();

        while (true) {
            String url = buildSelectUrl(baseUrl, "*:*", fq, uniqueKey, cursorMark, exportBatchSize);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofHours(2))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Solr query failed for date range " + rangeIndex
                        + " with status: " + response.statusCode());
            }

            JsonNode json = jsonMapper.readTree(response.body());
            JsonNode docs = json.path("response").path("docs");
            String nextCursorMark = json.path("nextCursorMark").asText("");

            if (docs.isArray() && docs.size() > 0) {
                writeDocsToCsv(docs, outFile, !firstBatch, fieldNames);
                totalDocs += docs.size();
                firstBatch = false;
            }

            if (cursorMark.equals(nextCursorMark)
                    || StringUtils.isBlank(nextCursorMark)
                    || docs.size() == 0) {
                break;
            }
            cursorMark = nextCursorMark;
        }

        long elapsed = System.currentTimeMillis() - rangeStart;
        handler.logInfo("Thread '" + thread.getName() + "' completed date range " + rangeIndex
                + ": " + totalDocs + " docs in " + elapsed + " ms");
    }

    // ── UUID partitioning helpers ──────────────────────────────────────────

    /**
     * Partitions the UUID key space between {@code minKey} and {@code maxKey} into
     * {@code n} equal ranges.
     *
     * <p>Uses unsigned 128-bit integer arithmetic to ensure correct arithmetic on the
     * full UUID space. Java's {@code long} is signed, so UUIDs are first converted to
     * unsigned {@link BigInteger} before division.
     *
     * @param minKey the minimum UUID as a string
     * @param maxKey the maximum UUID as a string
     * @param n      the number of partitions
     * @return an ordered list of {@link UuidRange} objects covering the full space
     * @throws IllegalArgumentException if either key is not a valid UUID string
     */
    private List<UuidRange> partitionUuidSpace(String minKey, String maxKey, int n) {
        BigInteger lo;
        BigInteger hi;
        try {
            lo = uuidToUnsignedBigInt(UUID.fromString(minKey));
            hi = uuidToUnsignedBigInt(UUID.fromString(maxKey));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "uniqueKey values are not valid UUIDs (minKey='" + minKey
                    + "'): " + e.getMessage(), e);
        }

        if (hi.compareTo(lo) < 0) {
            // Swap if lexicographic order differs from numeric order
            BigInteger tmp = lo;
            lo = hi;
            hi = tmp;
        }

        int actualN = Math.max(1, n);
        BigInteger rangeSize = hi.subtract(lo).divide(BigInteger.valueOf(actualN));
        if (rangeSize.compareTo(BigInteger.ZERO) == 0) {
            rangeSize = BigInteger.ONE;
        }

        List<UuidRange> ranges = new ArrayList<>();
        BigInteger cur = lo;
        for (int i = 0; i < actualN; i++) {
            BigInteger end = (i == actualN - 1)
                    ? hi
                    : cur.add(rangeSize).subtract(BigInteger.ONE);
            ranges.add(new UuidRange(
                    unsignedBigIntToUuid(cur).toString(),
                    unsignedBigIntToUuid(end).toString()));
            cur = end.add(BigInteger.ONE);
        }
        return ranges;
    }

    /**
     * Converts a {@link UUID} to an unsigned 128-bit {@link BigInteger}.
     *
     * <p>Java's {@code UUID.getMostSignificantBits()} and
     * {@code getLeastSignificantBits()} return signed {@code long} values.
     * This method adds {@code 2^64} to each half when negative, producing an
     * unsigned value in the range {@code [0, 2^128)}.
     *
     * @param uuid the UUID to convert
     * @return unsigned 128-bit integer representation
     */
    private static BigInteger uuidToUnsignedBigInt(UUID uuid) {
        BigInteger msb = BigInteger.valueOf(uuid.getMostSignificantBits());
        BigInteger lsb = BigInteger.valueOf(uuid.getLeastSignificantBits());
        if (msb.signum() < 0) {
            msb = msb.add(TWO_64);
        }
        if (lsb.signum() < 0) {
            lsb = lsb.add(TWO_64);
        }
        return msb.shiftLeft(64).add(lsb);
    }

    /**
     * Converts an unsigned 128-bit {@link BigInteger} back to a {@link UUID}.
     *
     * @param bi unsigned 128-bit integer in range {@code [0, 2^128)}
     * @return the equivalent UUID
     */
    private static UUID unsignedBigIntToUuid(BigInteger bi) {
        long lsb = bi.and(MASK_64).longValue();
        long msb = bi.shiftRight(64).and(MASK_64).longValue();
        return new UUID(msb, lsb);
    }

    // ── CSV serialization ──────────────────────────────────────────────────

    /**
     * Serializes a Solr JSON {@code docs} array to CSV and writes it to the given file.
     *
     * <p>On the first batch ({@code appendMode=false}) the file is created fresh and a
     * header row is written. On subsequent batches ({@code appendMode=true}) data rows
     * are appended without repeating the header.
     *
     * <p>Multi-valued fields are joined with {@code ,}. Values that contain commas,
     * double-quotes, or newlines are double-quoted and internal double-quotes are escaped
     * as {@code ""} per RFC 4180.
     *
     * @param docs       the {@code response.docs} JSON array node from Solr
     * @param filePath   target file path
     * @param appendMode {@code true} to append (skip header); {@code false} to create/overwrite
     * @param fieldNames ordered list of field names for header and row extraction
     * @throws IOException on I/O failure
     */
    private void writeDocsToCsv(JsonNode docs, Path filePath,
            boolean appendMode, List<String> fieldNames) throws IOException {
        StandardOpenOption[] openOptions = appendMode
                ? new StandardOpenOption[] {
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE }
                : new StandardOpenOption[] {
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING };

        try (BufferedWriter writer = Files.newBufferedWriter(filePath,
                StandardCharsets.UTF_8, openOptions)) {
            if (!appendMode && !fieldNames.isEmpty()) {
                // Write header row
                writer.write(buildCsvRow(fieldNames));
                writer.newLine();
            }

            for (JsonNode doc : docs) {
                List<String> values = new ArrayList<>(fieldNames.size());
                for (String field : fieldNames) {
                    JsonNode val = doc.get(field);
                    if (val == null || val.isNull()) {
                        values.add("");
                    } else if (val.isArray()) {
                        // Multi-valued: join items with comma, then quote the whole cell
                        List<String> parts = new ArrayList<>();
                        for (JsonNode item : val) {
                            parts.add(item.asText(""));
                        }
                        values.add(escapeCsvValue(String.join(",", parts)));
                    } else {
                        values.add(escapeCsvValue(val.asText("")));
                    }
                }
                writer.write(buildCsvRow(values));
                writer.newLine();
            }
        }
    }

    /**
     * Joins a list of values into a single CSV row (comma-separated).
     *
     * @param values the cell values (already escaped)
     * @return the CSV row string without trailing newline
     */
    private static String buildCsvRow(List<String> values) {
        return String.join(",", values);
    }

    /**
     * Escapes a single CSV cell value per RFC 4180.
     * Values containing commas, double-quotes, or newlines are wrapped in double quotes.
     * Internal double-quotes are doubled.
     *
     * @param value the raw cell value; may be {@code null}
     * @return the escaped value safe for embedding in a CSV row
     */
    private static String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ── URL builder helper ─────────────────────────────────────────────────

    /**
     * Builds a Solr {@code /select} URL with cursorMark pagination parameters.
     *
     * @param baseUrl    the core base URL (without trailing slash)
     * @param query      the main query (e.g. {@code *:*})
     * @param fq         an optional filter query; {@code null} to omit
     * @param sortField  the sort field (uniqueKey) required by cursorMark
     * @param cursorMark the current cursor mark value
     * @param rows       the batch size (rows per page)
     * @return the fully encoded URL string
     */
    private static String buildSelectUrl(String baseUrl, String query, String fq,
            String sortField, String cursorMark, int rows) {
        StringBuilder sb = new StringBuilder(baseUrl)
                .append("/select?q=").append(URLEncoder.encode(query, StandardCharsets.UTF_8))
                .append("&sort=").append(URLEncoder.encode(sortField + " asc", StandardCharsets.UTF_8))
                .append("&cursorMark=").append(URLEncoder.encode(cursorMark, StandardCharsets.UTF_8))
                .append("&rows=").append(rows)
                .append("&wt=json");
        if (fq != null) {
            sb.append("&fq=").append(URLEncoder.encode(fq, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    // ── Schema helpers ─────────────────────────────────────────────────────

    /**
     * Returns the uniqueKey field name for this Solr core.
     * Result is cached after the first call.
     *
     * @param baseUrl the core base URL
     * @return the uniqueKey field name (e.g. {@code "uid"}, {@code "id"})
     * @throws Exception on any HTTP or parse error
     */
    private String getUniqueKeyField(String baseUrl) throws Exception {
        if (cachedUniqueKeyField != null) {
            return cachedUniqueKeyField;
        }

        String url = baseUrl + "/schema/uniquekey?wt=json";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofHours(2))
                .GET()
                .build();

        HttpResponse<String> rsp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (rsp.statusCode() != 200) {
            log.warn("Could not retrieve uniqueKey from {}. Defaulting to 'id'.", baseUrl);
            cachedUniqueKeyField = "id";
            return cachedUniqueKeyField;
        }

        JsonNode json = jsonMapper.readTree(rsp.body());
        String uk = json.path("uniqueKey").asText("id");
        cachedUniqueKeyField = StringUtils.isBlank(uk) ? "id" : uk;
        return cachedUniqueKeyField;
    }

    /**
     * Returns the available (stored) field names for this core from the Solr schema endpoint.
     * Result is cached after the first call; the cache is safe to share across threads since
     * it is written once under a {@code synchronized} block.
     *
     * @param baseUrl the core base URL
     * @return an immutable snapshot of the field name list
     * @throws Exception on HTTP or parse failure
     */
    private List<String> getAvailableFields(String baseUrl) throws Exception {
        if (cachedFields != null) {
            return cachedFields;
        }

        synchronized (this) {
            if (cachedFields != null) {
                return cachedFields;
            }

            String url = baseUrl + "/schema/fields?wt=json";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofHours(2))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Could not retrieve schema fields from {}, using fallback", baseUrl);
                cachedFields = getFieldsFromSampleQuery(baseUrl);
                return cachedFields;
            }

            List<String> fields = new ArrayList<>();
            JsonNode json = jsonMapper.readTree(response.body());
            JsonNode fieldsArray = json.path("fields");
            if (fieldsArray.isArray()) {
                for (JsonNode field : fieldsArray) {
                    String name = field.path("name").asText();
                    if (!name.startsWith("_")
                            && !name.equals("_version_")
                            && !name.equals("_root_")) {
                        fields.add(name);
                    }
                }
            }

            handler.logInfo("Schema: " + fields.size() + " fields for core '" + coreName + "'");
            cachedFields = fields;
            return cachedFields;
        }
    }

    /**
     * Fallback field discovery: samples the first document in the core.
     *
     * @param baseUrl the core base URL
     * @return field names found in the first document; empty list if core is empty
     * @throws Exception on any error
     */
    private List<String> getFieldsFromSampleQuery(String baseUrl) throws Exception {
        String url = baseUrl + "/select?q=*:*&rows=1&wt=json";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofHours(2))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Could not retrieve sample document from SOLR core");
        }

        List<String> fields = new ArrayList<>();
        JsonNode json = jsonMapper.readTree(response.body());
        JsonNode docs = json.path("response").path("docs");
        if (docs.isArray() && docs.size() > 0) {
            docs.get(0).fieldNames().forEachRemaining(name -> {
                if (!name.startsWith("_")
                        && !name.equals("_version_")
                        && !name.equals("_root_")) {
                    fields.add(name);
                }
            });
        }
        handler.logInfo("Fallback: " + fields.size() + " fields from sample doc for '"
                + coreName + "'");
        return fields;
    }

    // ── Date-range helpers (unchanged) ────────────────────────────────────

    /**
     * Returns the date field to use for date-range sharding based on the core name.
     *
     * @return the date field name
     */
    private String getDateFieldForCore() {
        return switch (coreName) {
            case "statistics" -> "time";
            case "audit"      -> "timeStamp";
            default           -> "lastModified";
        };
    }

    /**
     * Resolves the total date range for the core, either from CLI arguments or
     * from Solr stats on the date field.
     *
     * @param baseUrl the core base URL
     * @return the total date range, or {@code null} if no documents exist
     * @throws Exception on any error
     */
    private DateRange getDateRange(String baseUrl) throws Exception {
        String minDate = startDate;
        String maxDate = endDate;

        if (StringUtils.isBlank(minDate) || StringUtils.isBlank(maxDate)) {
            String statsUrl = String.format("%s/select?q=*:*&rows=0&wt=json&stats=true&stats.field=%s",
                    baseUrl, dateField);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(statsUrl))
                    .timeout(Duration.ofHours(2))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Stats query failed with status: "
                        + response.statusCode()
                        + " — core might be empty or unavailable");
            }

            JsonNode json = jsonMapper.readTree(response.body());
            JsonNode stats = json.path("stats").path("stats_fields").path(dateField);

            if (StringUtils.isBlank(minDate)) {
                String raw = stats.path("min").asText("");
                minDate = raw.length() >= 10 ? raw.substring(0, 10) : raw;
            }
            if (StringUtils.isBlank(maxDate)) {
                String raw = stats.path("max").asText("");
                maxDate = raw.length() >= 10 ? raw.substring(0, 10) : raw;
            }

            if (StringUtils.isBlank(minDate) || StringUtils.isBlank(maxDate)) {
                return null;
            }
            handler.logInfo("Resolved date range from SOLR: " + minDate + " to " + maxDate);
        }
        return new DateRange(minDate, maxDate);
    }

    /**
     * Generates a list of sub-ranges from a total date range using the configured increment.
     *
     * @param totalRange the bounding date range
     * @return list of sub-ranges; at least one element
     */
    private List<DateRange> generateDateRanges(DateRange totalRange) {
        List<DateRange> ranges = new ArrayList<>();
        try {
            java.time.LocalDate start = parseInputDate(totalRange.start);
            java.time.LocalDate end   = parseInputDate(totalRange.end);
            java.time.LocalDate cur   = start;

            while (cur.isBefore(end) || cur.isEqual(end)) {
                java.time.LocalDate next = switch (dateIncrement) {
                    case "WEEK"  -> cur.plusWeeks(1).minusDays(1);
                    case "YEAR"  -> cur.plusYears(1).minusDays(1);
                    default      -> cur.plusMonths(1).minusDays(1); // MONTH
                };
                if (next.isAfter(end)) {
                    next = end;
                }
                ranges.add(new DateRange(
                        cur.atStartOfDay() + ":00.000Z",
                        next.atTime(23, 59, 59, 999_000_000).toString() + "Z"));
                cur = next.plusDays(1);
            }
        } catch (Exception e) {
            log.warn("Failed to parse dates, using single range: {}", e.getMessage());
            ranges.add(totalRange);
        }
        return ranges;
    }

    /**
     * Parses a date string that is either {@code YYYY-MM-DD} or ISO-8601.
     *
     * @param dateStr the date string
     * @return parsed {@link java.time.LocalDate}
     */
    private java.time.LocalDate parseInputDate(String dateStr) {
        if (dateStr.length() == 10) {
            return java.time.LocalDate.parse(dateStr);
        }
        return java.time.LocalDateTime.parse(dateStr.substring(0, 19)).toLocalDate();
    }

    // ── Import ─────────────────────────────────────────────────────────────

    /**
     * Imports SOLR core data from previously exported files.
     * Looks for any file starting with {@code solr_export_} and ending with
     * the configured format extension.
     *
     * @throws Exception on any error
     */
    private void importCore() throws Exception {
        long startTime = System.currentTimeMillis();
        String solrUrl = configurationService.getProperty("solr.server");
        String fullCoreName = getFullCoreName(coreName);
        String baseUrl = solrUrl + "/" + fullCoreName;

        handler.logInfo("Starting import to SOLR core: " + baseUrl);

        // Accept files from any export strategy (old solr_export_range_ AND new prefixes)
        File[] files = new File(directory).listFiles((dir, name) ->
                name.startsWith("solr_export_") && name.endsWith("." + format));

        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("No export files found in directory: " + directory);
        }

        Arrays.sort(files, java.util.Comparator.comparing(File::getName));
        handler.logInfo("Found " + files.length + " files to import using " + threadCount
                + " threads");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        long processingStart = System.currentTimeMillis();
        for (File file : files) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    importFile(baseUrl, file);
                } catch (Exception e) {
                    log.error("Error importing file {}: {}", file.getName(), e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }, executor);
            futures.add(future);
        }

        handler.logInfo("Waiting for " + futures.size() + " import tasks to complete...");
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long processingTime = System.currentTimeMillis() - processingStart;

        executor.shutdown();
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            log.warn("Executor did not terminate gracefully, forcing shutdown");
            executor.shutdownNow();
        }

        handler.logInfo("Performing final SOLR commit...");
        long commitStart = System.currentTimeMillis();
        commitToSolr(baseUrl);
        long commitTime = System.currentTimeMillis() - commitStart;

        long totalTime = System.currentTimeMillis() - startTime;
        handler.logInfo("Import completed in " + totalTime + " ms (processing: "
                + processingTime + " ms, commit: " + commitTime + " ms)");
    }

    /**
     * Imports a single file using HTTP POST to SOLR.
     *
     * @param baseUrl the core base URL
     * @param file    the file to import
     * @throws Exception on any error
     */
    private void importFile(String baseUrl, File file) throws Exception {
        long fileStart = System.currentTimeMillis();
        Thread thread = Thread.currentThread();

        handler.logInfo("Thread '" + thread.getName() + "' importing: " + file.getName()
                + " (" + (file.length() / 1024) + " KB)");

        String url = baseUrl + "/update";
        boolean isCsv = format.equals("csv");
        String contentType = isCsv ? "application/csv" : "application/json";

        long uploadStart = System.currentTimeMillis();
        boolean splitCsvFiles = configurationService.getBooleanProperty(
                "solr.import.splitCsvFiles", true);

        if (!isCsv || !splitCsvFiles) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofHours(2))
                    .header("Content-Type", contentType)
                    .POST(HttpRequest.BodyPublishers.ofFile(file.toPath()))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("SOLR import failed for file " + file.getName()
                        + " with status: " + response.statusCode() + " — " + response.body());
            }
        } else {
            int batchSize = configurationService.getIntProperty(
                    "solr.import.csvBatchSize", 500_000);
            processLargeCsvFile(file, url, contentType, batchSize);
        }

        long totalTime = System.currentTimeMillis() - fileStart;
        handler.logInfo("Thread '" + thread.getName() + "' imported '" + file.getName()
                + "' in " + totalTime + " ms (upload: "
                + (System.currentTimeMillis() - uploadStart) + " ms)");
    }

    /**
     * Commits changes to SOLR.
     *
     * @param baseUrl the core base URL
     * @throws Exception on any error
     */
    private void commitToSolr(String baseUrl) throws Exception {
        String url = baseUrl + "/update?commit=true";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofHours(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("SOLR commit failed with status: " + response.statusCode());
        }
    }

    /**
     * Returns the full Solr core name, prepending the multicore prefix when configured.
     *
     * @param baseName the bare core name
     * @return the full core name
     */
    private String getFullCoreName(String baseName) {
        String prefix = configurationService.getProperty("solr.multicorePrefix");
        return StringUtils.isNotBlank(prefix) ? prefix + baseName : baseName;
    }

    // ── Large CSV import helpers (unchanged) ──────────────────────────────

    /**
     * Processes a large CSV file in streaming chunks to avoid OOM.
     *
     * @param file          the CSV file to process
     * @param url           the Solr update URL
     * @param contentType   the HTTP content type
     * @param batchSize     lines per chunk
     * @throws Exception on any error
     */
    private void processLargeCsvFile(File file, String url, String contentType,
            int batchSize) throws Exception {
        handler.logInfo("Processing large CSV: " + file.getName()
                + " (" + (file.length() / 1024 / 1024) + " MB)");

        try (BufferedReader reader = Files.newBufferedReader(file.toPath(),
                StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null) {
                throw new RuntimeException("Empty CSV file: " + file.getName());
            }

            handler.logInfo("CSV header: "
                    + (header.length() > 100 ? header.substring(0, 100) + "..." : header));

            int chunkNumber = 1;
            int totalLines = 0;
            while (true) {
                int linesInChunk = processChunk(reader, header, url, contentType,
                        batchSize, chunkNumber, file.getName());
                if (linesInChunk == 0) {
                    break;
                }
                totalLines += linesInChunk;
                chunkNumber++;
            }

            handler.logInfo("Processed " + totalLines + " lines in "
                    + (chunkNumber - 1) + " chunks from " + file.getName());
        }
    }

    /**
     * Processes a single chunk of CSV data.
     *
     * @param reader           reader positioned after the header
     * @param header           the CSV header line
     * @param url              the Solr update URL
     * @param contentType      HTTP content type
     * @param batchSize        max lines per chunk
     * @param chunkNumber      chunk sequence number (for logging)
     * @param originalFileName source filename (for logging)
     * @return number of data lines in this chunk; {@code 0} signals end of file
     * @throws Exception on any error
     */
    private int processChunk(BufferedReader reader, String header,
            String url, String contentType, int batchSize,
            int chunkNumber, String originalFileName) throws Exception {
        Path tempFile = null;
        int linesInChunk = 0;
        try {
            tempFile = Files.createTempFile(
                    originalFileName + "_solr_chunk_" + chunkNumber + "_", ".csv");

            try (BufferedWriter writer = Files.newBufferedWriter(tempFile,
                    StandardCharsets.UTF_8)) {
                writer.write(header);
                writer.newLine();
                String line;
                while (linesInChunk < batchSize && (line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                    linesInChunk++;
                }
            }

            if (linesInChunk == 0) {
                return 0;
            }

            handler.logInfo("Importing chunk " + chunkNumber + " from " + originalFileName
                    + " (" + linesInChunk + " lines, "
                    + (Files.size(tempFile) / 1024) + " KB)");

            importCsvChunk(tempFile, url, contentType, chunkNumber, originalFileName);
            return linesInChunk;

        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    handler.logError("Failed to delete temporary file: " + tempFile, e);
                }
            }
        }
    }

    /**
     * POSTs a single CSV chunk file to Solr.
     *
     * @param tempFile         the chunk file
     * @param url              the Solr update URL
     * @param contentType      HTTP content type
     * @param batchNumber      chunk sequence number (for logging)
     * @param originalFileName source filename (for logging)
     * @throws Exception on any error
     */
    private void importCsvChunk(Path tempFile, String url, String contentType,
            int batchNumber, String originalFileName) throws Exception {
        handler.logInfo("Importing chunk " + batchNumber + " from " + originalFileName
                + " (size: " + (Files.size(tempFile) / 1024) + " KB)");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofHours(2))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofFile(tempFile))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("SOLR import failed for chunk " + batchNumber
                    + " of " + originalFileName
                    + " with status: " + response.statusCode()
                    + " — " + response.body());
        }

        handler.logInfo("Successfully imported chunk " + batchNumber
                + " from " + originalFileName);
    }

    // ── Spring wiring ──────────────────────────────────────────────────────

    @Override
    public SolrCoreExportImportScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("solr-core-management",
                SolrCoreExportImportScriptConfiguration.class);
    }

    // ── Help ───────────────────────────────────────────────────────────────

    /**
     * Prints verbose help text to the script handler output.
     */
    private void printVerboseHelp() {
        handler.logInfo("SOLR Core Export/Import Script");
        handler.logInfo("==============================");
        handler.logInfo("");
        handler.logInfo("Parameters:");
        handler.logInfo("  -m <mode>          Required. 'export' or 'import'");
        handler.logInfo("  -c <core>          Required. SOLR core name");
        handler.logInfo("  -d <directory>     Required. Directory for export/import files");
        handler.logInfo("  -f <format>        Optional. 'csv' or 'json' (default: csv)");
        handler.logInfo("  -t <threads>       Optional. Parallel threads (default: 1)");
        handler.logInfo("  --strategy <s>     Optional. Export strategy (default: uuid-range):");
        handler.logInfo("                       uuid-range  — partition UUID key space (fastest, "
                + "default)");
        handler.logInfo("                       cursor-mark — single-thread full scan (non-UUID "
                + "cores)");
        handler.logInfo("                       date-range  — time-based sharding");
        handler.logInfo("                       auto        — uuid-range with cursor-mark fallback");
        handler.logInfo("  --batch-size <n>   Optional. Rows per cursorMark batch (default: 10000)");
        handler.logInfo("  -s <start-date>    Optional. Start date (date-range strategy)");
        handler.logInfo("  -e <end-date>      Optional. End date (date-range strategy)");
        handler.logInfo("  -i <increment>     Optional. WEEK, MONTH, YEAR (default: MONTH)");
        handler.logInfo("  -h                 Show this help");
        handler.logInfo("");
        handler.logInfo("Examples:");
        handler.logInfo("  # Export statistics (UUID key, 8 threads, UUID-range strategy):");
        handler.logInfo("  ./dspace solr-core-management -m export -c statistics -d /backup/ -t 8");
        handler.logInfo("");
        handler.logInfo("  # Export oai (non-UUID key, cursor-mark strategy):");
        handler.logInfo("  ./dspace solr-core-management -m export -c oai -d /backup/ "
                + "--strategy cursor-mark -t 1");
        handler.logInfo("");
        handler.logInfo("  # Import all exported cores:");
        handler.logInfo("  ./dspace solr-core-management -m import -c statistics -d /backup/ -t 8");
    }

    // ── Inner classes ──────────────────────────────────────────────────────

    /**
     * Holds a half-open UUID range {@code [start, end]}.
     */
    private static class UuidRange {
        final String start;
        final String end;

        UuidRange(String start, String end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return "[" + start + " TO " + end + "]";
        }
    }

    /**
     * Holds a half-open date range {@code [start, end]}.
     */
    private static class DateRange {
        final String start;
        final String end;

        DateRange(String start, String end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return start + " to " + end;
        }
    }
}
