/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.discovery;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.dspace.discovery.indexobject.factory.IndexObjectFactoryFactory;

/**
 * This Enum holds all the possible options and combinations for the Index discovery script
 */
public enum IndexClientOptions {
    REMOVE,
    CLEAN,
    DELETE,
    BUILD,
    BUILDANDSPELLCHECK,
    OPTIMIZE,
    SPELLCHECK,
    INDEX,
    UPDATE,
    FORCEUPDATE,
    UPDATEANDSPELLCHECK,
    FORCEUPDATEANDSPELLCHECK,
    HELP;

    public static final String TYPE_OPTION = "t";

    /**
     * This method resolves the CommandLine parameters to figure out which action the index-discovery script should
     * perform
     * @param commandLine   The relevant CommandLine for the index-discovery script
     * @return              The index-discovery option to be ran, parsed from the CommandLine
     */
    protected static IndexClientOptions getIndexClientOption(CommandLine commandLine) {
        if (commandLine.hasOption("h")) {
            return IndexClientOptions.HELP;
        } else if (commandLine.hasOption("r")) {
            return IndexClientOptions.REMOVE;
        } else if (commandLine.hasOption("c")) {
            return IndexClientOptions.CLEAN;
        } else if (commandLine.hasOption("d")) {
            return IndexClientOptions.DELETE;
        } else if (commandLine.hasOption("b")) {
            if (commandLine.hasOption("s")) {
                return IndexClientOptions.BUILDANDSPELLCHECK;
            } else {
                return IndexClientOptions.BUILD;
            }
        } else if (commandLine.hasOption("o")) {
            return IndexClientOptions.OPTIMIZE;
        } else if (commandLine.hasOption("s")) {
            return IndexClientOptions.SPELLCHECK;
        } else if (commandLine.hasOption("i")) {
            return IndexClientOptions.INDEX;
        } else {
            if (commandLine.hasOption("f") && commandLine.hasOption("s")) {
                return IndexClientOptions.FORCEUPDATEANDSPELLCHECK;
            } else if (commandLine.hasOption("f")) {
                return IndexClientOptions.FORCEUPDATE;
            } else if (commandLine.hasOption("s")) {
                return IndexClientOptions.UPDATEANDSPELLCHECK;
            } else {
                return IndexClientOptions.UPDATE;
            }
        }
    }

    protected static Options constructOptions() {
        Options options = new Options();
        List<String> indexableObjectTypes = IndexObjectFactoryFactory.getInstance().getIndexFactories().stream()
                .map((indexFactory -> indexFactory.getType())).collect(Collectors.toList());

        options
            .addOption("r", "remove", true, "remove an Item, Collection or Community from index based on its handle");
        options.addOption("i", "index", true,
                          "add or update an Item, Collection or Community based on its handle or uuid");
        options.addOption(TYPE_OPTION, "type", true, "reindex only specific type of " +
                "(re)indexable objects; options: " + Arrays.toString(indexableObjectTypes.toArray()));
        options.addOption("c", "clean", false,
                          "clean existing index removing any documents that no longer exist in the db");
        options.addOption("d", "delete", false,
                "delete all records from existing index");
        options.addOption("b", "build", false, "(re)build index, wiping out current one if it exists");
        options.addOption("s", "spellchecker", false, "Rebuild the spellchecker, can be combined with -b and -f.");
        options.addOption("t", "type", true,
                          "limit the update operation to a specific indexable object type " +
                          "(such as Item, ClaimedTask, PoolTask, etc.), can be combined with -f and -s.");
        options.addOption("f", "force", false,
                          "if updating existing index, force each handle to be reindexed even if uptodate");
        options.addOption("m", "nometric", false,
                          "DISABLE the full rebuild of cris metric in solr, can be combined with all the parameter.");
        options.addOption("h", "help", false, "print this help message");
        return options;
    }
}
