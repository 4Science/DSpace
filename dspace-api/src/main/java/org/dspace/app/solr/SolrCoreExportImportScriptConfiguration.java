/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.solr;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * Script configuration for SOLR core export/import operations.
 * Allows complete export and import of SOLR cores with multithreading support
 * and configurable export strategies.
 *
 * @author 4Science DSpace Team
 * @author Stefano Maffei (stefano.maffei at 4science.com) — original implementation
 */
public class SolrCoreExportImportScriptConfiguration<T extends SolrCoreExportImport>
        extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("m", "mode", true,
                "Operation mode: 'export' to export core data, 'import' to import core data (required)");

            options.addOption("c", "core", true,
                "SOLR core name to export from or import to (required)");

            options.addOption("d", "directory", true,
                "Directory path for export/import files (required)");

            options.addOption("f", "format", true,
                "File format: 'csv' or 'json' (default: csv)");

            options.addOption("t", "threads", true,
                "Number of threads for parallel processing (default: 1)");

            options.addOption("s", "start-date", true,
                "Start date for date-range export (format: 2023-01-01). "
                + "Only used with --strategy date-range");

            options.addOption("e", "end-date", true,
                "End date for date-range export (format: 2023-12-31). "
                + "Only used with --strategy date-range");

            options.addOption("i", "increment", true,
                "Date increment for range splitting: WEEK, MONTH, or YEAR (default: MONTH). "
                + "Only used with --strategy date-range");

            options.addOption(null, "strategy", true,
                "Export strategy (default: uuid-range). "
                + "Choices: uuid-range | cursor-mark | date-range | auto. "
                + "uuid-range: partitions UUID key space into N equal ranges, each exported "
                + "in parallel using cursorMark (fastest, requires UUID uniqueKey). "
                + "cursor-mark: single-thread full-core scan using cursorMark (use for "
                + "non-UUID cores such as oai or search). "
                + "date-range: time-based sharding with cursorMark per range. "
                + "auto: tries uuid-range, falls back to cursor-mark for non-UUID keys.");

            options.addOption(null, "batch-size", true,
                "Number of rows per cursorMark batch for uuid-range and cursor-mark strategies "
                + "(default: 10000). Larger values reduce HTTP round-trips; smaller values "
                + "reduce peak memory.");

            options.addOption("h", "help", false, "Display help information");

            super.options = options;
        }
        return options;
    }
}
