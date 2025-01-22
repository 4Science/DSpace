/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.doi;

import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.mail.MessagingException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.logic.Filter;
import org.dspace.content.logic.FilterUtils;
import org.dspace.content.logic.TrueFilter;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.doi.strategy.DOIOrganiserStrategy;
import org.dspace.identifier.doi.strategy.DOIOrganiserStrategyFactory;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 *
 * @author Marsa Haoua
 * @author Pascal-Nicolas Becker
 */
public class DOIOrganiser {

    private static final Logger LOG = LogManager.getLogger(DOIOrganiser.class);
    private static final int DOI_ORGANISER_LIMIT = 50;

    private final DOIIdentifierProvider provider;
    private final Context context;
    private final int limit;
    private boolean quiet;
    protected HandleService handleService;
    protected ItemService itemService;
    protected DOIService doiService;
    protected ConfigurationService configurationService;
    // This filter will override the default provider filter / behaviour
    protected Filter filter;
    protected DOIOrganiserStrategyFactory doiStrategyFactory;

    /**
     * Constructor to be called within the main() method
     * @param context   - DSpace context
     * @param provider  - DOI identifier provider to use
     */
    public DOIOrganiser(Context context, DOIIdentifierProvider provider) {
        this.context = context;
        this.provider = provider;
        this.quiet = false;
        this.handleService = HandleServiceFactory.getInstance().getHandleService();
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.doiService = IdentifierServiceFactory.getInstance().getDOIService();
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        this.limit = configurationService.getIntProperty("doi-organiser-limit", DOI_ORGANISER_LIMIT);
        this.filter = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(
                "always_true_filter", TrueFilter.class);
        this.doiStrategyFactory = DOIOrganiserStrategyFactory.instance();
    }

    /**
     * Main command-line runner method as with other DSpace launcher commands
     * @param args  - the command line arguments to parse as parameters
     */
    public static void main(String[] args) {
        LOG.debug("Starting DOI organiser ");

        // setup Context
        Context context = new Context();

        // Started from commandline, don't use the authentication system.
        context.turnOffAuthorisationSystem();

        DOIOrganiser organiser = new DOIOrganiser(context,
            new DSpace().getSingletonService(DOIIdentifierProvider.class));
        // run command line interface
        runCLI(context, organiser, args);

        try {
            context.complete();
        } catch (SQLException sqle) {
            System.err.println("Cannot save changes to database: " + sqle.getMessage());
            System.exit(-1);
        }

    }

    public static void runCLI(Context context, DOIOrganiser organiser, String[] args) {
        // initialize options
        Options options = buildOptions();

        // initialize parser
        CommandLineParser parser = new DefaultParser();
        CommandLine line = null;
        HelpFormatter helpformater = new HelpFormatter();

        try {
            line = parser.parse(options, args);
        } catch (ParseException ex) {
            LOG.fatal(ex);
            System.exit(1);
        }

        // process options
        // user asks for help
        if (line.hasOption('h') || 0 == line.getOptions().length) {
            helpformater.printHelp("\nDOI organiser\n", options);
        }

        if (line.hasOption('q')) {
            organiser.setQuiet();
        }

        if (line.hasOption('l')) {
            organiser.list("reservation", null, null, DOIIdentifierProvider.TO_BE_RESERVED);
            organiser.list("registration", null, null, DOIIdentifierProvider.TO_BE_REGISTERED);
            organiser.list("update", null, null,
                           DOIIdentifierProvider.UPDATE_BEFORE_REGISTRATION,
                           DOIIdentifierProvider.UPDATE_REGISTERED,
                           DOIIdentifierProvider.UPDATE_RESERVED);
            organiser.list("deletion", null, null, DOIIdentifierProvider.TO_BE_DELETED);
        }

        int limit = organiser.getLimit();
        int offset = -1;

        if (line.hasOption("li")) {
            limit = Integer.parseInt(line.getOptionValue("li"));
        }

        if (line.hasOption("o")) {
            offset = Integer.parseInt(line.getOptionValue("o"));
        }

        // Do we get a filter?
        if (line.hasOption("filter")) {
            String filter = line.getOptionValue("filter");
            if (null != filter) {
                organiser.filter = FilterUtils.getFilterFromConfiguration(filter);
            }
        }

        if (organiser.provider == null) {
            System.out.println("Cannot find any provider, configure a proper DOIIdentifierProvider and try again!");
            return;
        }

        List<DOIOrganiserStrategy> strategies = new ArrayList<>();
        if (line.hasOption('s')) {
            strategies.add(organiser.doiStrategyFactory.reserveStrategy(context, organiser.filter, offset, limit));
        }

        if (line.hasOption('r')) {
            strategies.add(organiser.doiStrategyFactory.registerStrategy(context, organiser.filter, offset, limit));
        }

        if (line.hasOption('u')) {
            strategies.add(organiser.doiStrategyFactory.updateStrategy(context, organiser.filter, offset, limit));
        }

        if (line.hasOption('d')) {
            strategies.add(organiser.doiStrategyFactory.deleteStrategy(context, offset, limit));
        }

        if (line.hasOption("reserve-doi")) {
            String identifier = line.getOptionValue("reserve-doi");

            if (null == identifier) {
                helpformater.printHelp("\nDOI organiser\n", options);
            } else {
                try {
                    strategies.add(organiser.doiStrategyFactory.reserveStrategy(organiser.resolveToDOI(identifier)));
                } catch (SQLException | IllegalArgumentException | IllegalStateException | IdentifierException ex) {
                    LOG.error(ex);
                }
            }
        }

        if (line.hasOption("register-doi")) {
            String identifier = line.getOptionValue("register-doi");

            if (null == identifier) {
                helpformater.printHelp("\nDOI organiser\n", options);
            } else {
                try {
                    strategies.add(organiser.doiStrategyFactory.registerStrategy(organiser.resolveToDOI(identifier)));
                } catch (SQLException | IllegalArgumentException | IllegalStateException | IdentifierException ex) {
                    LOG.error(ex);
                }
            }

        }

        if (line.hasOption("update-doi")) {
            String identifier = line.getOptionValue("update-doi");

            if (null == identifier) {
                helpformater.printHelp("\nDOI organiser\n", options);
            } else {
                try {
                    strategies.add(organiser.doiStrategyFactory.updateStrategy(organiser.resolveToDOI(identifier)));
                } catch (SQLException | IllegalArgumentException | IllegalStateException | IdentifierException ex) {
                    LOG.error(ex);
                }
            }
        }

        if (line.hasOption("delete-doi")) {
            String identifier = line.getOptionValue("delete-doi");

            if (null == identifier) {
                helpformater.printHelp("\nDOI organiser\n", options);
            } else {
                try {
                    strategies.add(organiser.doiStrategyFactory.deleteStrategy(organiser.resolveToDOI(identifier)));
                } catch (SQLException | IllegalArgumentException | IllegalStateException | IdentifierException ex) {
                    LOG.error(ex);
                }
            }
        }

        for (DOIOrganiserStrategy strategy : strategies) {
            strategy.apply(context);
        }

        System.out.println("Process Completed!");
        LOG.info("Process Completed!");

    }

    private static Options buildOptions() {
        Options options = new Options();

        options.addOption("h", "help", false, "Help");
        options.addOption("l", "list", false,
            "List all objects to be reserved, registered, deleted of updated "
        );
        options.addOption("r", "register-all", false,
            "Perform online registration for all identifiers queued for registration."
        );
        options.addOption("s", "reserve-all", false,
            "Perform online reservation for all identifiers queued for reservation."
        );
        options.addOption("u", "update-all", false,
            "Perform online metadata update for all identifiers queued for metadata update."
        );
        options.addOption("d", "delete-all", false,
            "Perform online deletion for all identifiers queued for deletion."
        );
        options.addOption("q", "quiet", false,
            "Turn the command line output off."
        );
        options.addOption("o", "offset", true, "The records offset");
        options.addOption("li", "limit", true, "The records limit");

        options.addOption(
            Option.builder().optionalArg(true).longOpt("filter").hasArg().argName("filterName")
                  .desc("Use the specified filter name instead of the provider's filter. Defaults to a special " +
                        "'always true' filter to force operations")
                  .build()
        );

        options.addOption(
            Option.builder()
                  .longOpt("register-doi")
                  .hasArg()
                  .argName("DOI|ItemID|handle")
                  .desc("Register a specified identifier. "
                        + "You can specify the identifier by ItemID, Handle or"
                        + " DOI.")
                  .build()
        );

        options.addOption(
            Option.builder()
                  .longOpt("reserve-doi")
                  .hasArg()
                  .argName("DOI|ItemID|handle")
                  .desc("Reserve a specified identifier online. "
                        + "You can specify the identifier by ItemID, Handle or "
                        + "DOI.")
                  .build()
        );

        options.addOption(
            Option.builder()
                  .longOpt("update-doi")
                  .hasArg()
                  .argName("DOI|ItemID|handle")
                  .desc("Update online an object for a given DOI identifier"
                        + " or ItemID or Handle. A DOI identifier or an ItemID or a"
                        + " Handle is needed.")
                  .build()
        );

        options.addOption(
            Option.builder()
                  .argName("DOI|ItemID|handle")
                  .longOpt("delete-doi")
                  .hasArg()
                  .desc("Delete a specified identifier.")
                  .build()
        );

        return options;
    }

    /**
     * list DOIs queued for reservation or registration
     * @param processName   - process name for display
     * @param out           - output stream (eg. STDOUT)
     * @param err           - error output stream (eg. STDERR)
     * @param status        - status codes
     */
    public void list(String processName, PrintStream out, PrintStream err, Integer ... status) {
        String indent = "    ";
        if (null == out) {
            out = System.out;
        }
        if (null == err) {
            err = System.err;
        }

        try {
            List<DOI> doiList = doiService.getDOIsByStatus(context, List.of(status), limit);
            if (!doiList.isEmpty()) {
                out.println("First " + limit + " DOIs queued for " + processName + ": ");
            } else {
                out.println("There are no DOIs queued for " + processName + ".");
            }
            for (DOI doiRow : doiList) {
                out.print(indent + DOI.SCHEME + doiRow.getDoi());
                DSpaceObject dso = doiRow.getDSpaceObject();
                if (null != dso) {
                    out.println(" (belongs to item with handle " + dso.getHandle() + ")");
                } else {
                    out.println(" (cannot determine handle of assigned object)");
                }
            }
            out.println("");
        } catch (SQLException ex) {
            err.println("Error in database Connection: " + ex.getMessage());
            ex.printStackTrace(err);
        }
    }

    /**
     * Finds the TableRow in the Doi table that belongs to the specified
     * DspaceObject.
     *
     * @param identifier Either an ItemID, a DOI or a handle. If the identifier
     *                   contains digits only we treat it as ItemID, if not we try to find a
     *                   matching doi or a handle (in this order).
     * @return The TableRow or null if the Object does not have a DOI.
     * @throws SQLException             if database error
     * @throws IllegalArgumentException If the identifier is null, an empty
     *                                  String or specifies an DSpaceObject that is not an item. We currently
     *                                  support DOIs for items only, but this may change once...
     * @throws IllegalStateException    If the identifier was a valid DOI that is
     *                                  not stored in our database or if it is a handle that is not bound to an
     *                                  DSpaceObject.
     * @throws IdentifierException      if identifier error
     */
    public DOI resolveToDOI(String identifier)
        throws SQLException, IllegalArgumentException, IllegalStateException, IdentifierException {
        if (null == identifier || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier is null or empty.");
        }

        DOI doiRow = null;
        String doi = null;

        // detect it identifer is ItemID, handle or DOI.
        // try to detect ItemID
        if (identifier
            .matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}")) {
            DSpaceObject dso = itemService.find(context, UUID.fromString(identifier));

            if (null != dso) {
                doi = provider.mint(context, dso, this.filter);
                doiRow = doiService.findByDoi(context, doi.substring(DOI.SCHEME.length()));
                return doiRow;
            } else {
                throw new IllegalStateException("You specified an ItemID, that is not stored in our database.");
            }
        }

        // detect handle
        DSpaceObject dso = handleService.resolveToObject(context, identifier);

        if (null != dso) {
            if (dso.getType() != Constants.ITEM) {
                throw new IllegalArgumentException(
                    "Currently DSpace supports DOIs for Items only. "
                        + "Cannot process specified handle as it does not identify an Item.");
            }

            doi = provider.mint(context, dso, this.filter);
            doiRow = doiService.findByDoi(context, doi.substring(DOI.SCHEME.length()));
            return doiRow;
        }
        // detect DOI
        try {
            doi = doiService.formatIdentifier(identifier);
            // If there's no exception: we found a valid DOI. :)
            doiRow = doiService.findByDoi(context,
                                          doi.substring(DOI.SCHEME.length()));
            if (null == doiRow) {
                throw new IllegalStateException("You specified a valid DOI, that is not stored in our database.");
            }
        } catch (DOIIdentifierException ex) {
            // Identifier was not recognized as DOI.
            LOG.error("It wasn't possible to detect this identifier:  "
                          + identifier
                          + " Exceptions code:  "
                          + ex.codeToString(ex.getCode()), ex);

            if (!quiet) {
                System.err.println("It wasn't possible to detect this DOI identifier: " + identifier);
            }
        }

        return doiRow;
    }

    public int getLimit() {
        return limit;
    }

    /**
     * Set this runner to be in quiet mode, suppressing console output
     */
    private void setQuiet() {
        this.quiet = true;
    }

}
