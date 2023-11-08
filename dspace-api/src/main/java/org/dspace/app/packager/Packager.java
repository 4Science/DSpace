/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.packager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.packager.PackageDisseminator;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageIngester;
import org.dspace.content.packager.PackageParameters;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowException;

/**
 * Command-line interface to the Packager plugin.
 * <p>
 * This class ONLY exists to provide a CLI for the packager plugins. It does not
 * "manage" the plugins and it is not called from within DSpace, but the name
 * follows a DSpace convention.
 * <p>
 * It can invoke one of the Submission (SIP) packagers to create a new DSpace
 * Item out of a package, or a Dissemination (DIP) packager to write an Item out
 * as a package.
 * <p>
 * Usage is as follows:<br>
 * (Add the -h option to get the command to show its own help)
 *
 * <pre>
 *  1. To submit a SIP  (submissions tend to create a *new* object, with a new handle.  If you want to restore an
 *  object, see -r option below)
 *   dspace packager
 *       -e {ePerson}
 *       -t {PackagerType}
 *       -p {parent-handle} [ -p {parent2} ...]
 *       [-o {name}={value} [ -o {name}={value} ..]]
 *       [-a] --- also recursively ingest all child packages of the initial package
 *                (child pkgs must be referenced from parent pkg)
 *       [-w]   --- skip Workflow
 *       {package-filename}
 *
 *   {PackagerType} must match one of the aliases of the chosen Packager
 *   plugin.
 *
 *   The &quot;-w&quot; option circumvents Workflow, and is optional.  The &quot;-o&quot;
 *   option, which may be repeated, passes options to the packager
 *   (e.g. &quot;metadataOnly&quot; to a DIP packager).
 *
 *  2. To restore an AIP  (similar to submit mode, but attempts to restore with the handles/parents specified in AIP):
 *   dspace packager
 *       -r     --- restores a object from a package info, including the specified handle (will throw an error if
 *       handle is already in use)
 *       -e {ePerson}
 *       -t {PackagerType}
 *       [-o {name}={value} [ -o {name}={value} ..]]
 *       [-a] --- also recursively restore all child packages of the initial package
 *                (child pkgs must be referenced from parent pkg)
 *       [-k]   --- Skip over errors where objects already exist and Keep Existing objects by default.
 *                  Use with -r to only restore objects which do not already exist.  By default, -r will throw an error
 *                  and rollback all changes when an object is found that already exists.
 *       [-f]   --- Force a restore (even if object already exists).
 *                  Use with -r to replace an existing object with one from a package (essentially a delete and
 *                  restore).
 *                  By default, -r will throw an error and rollback all changes when an object is found that already
 *                  exists.
 *       [-i {identifier-handle-of-object}] -- Optional when -f is specified.  When replacing an object, you can
 *       specify the
 *                  object to replace if it cannot be easily determined from the package itself.
 *       {package-filename}
 *
 *   Restoring is very similar to submitting, except that you are recreating pre-existing objects.  So, in a restore,
 *   the object(s) are
 *   being recreated based on the details in the AIP.  This means that the object is recreated with the same handle
 *   and same parent/children
 *   objects.  Not all {PackagerTypes} may support a "restore".
 *
 *  3. To write out a DIP:
 *   dspace packager
 *       -d
 *       -e {ePerson}
 *       -t {PackagerType}
 *       -i {identifier-handle-of-object}
 *       [-a] --- also recursively disseminate all child objects of this object
 *       [-o {name}={value} [ -o {name}={value} ..]]
 *       {package-filename}
 *
 *   The &quot;-d&quot; switch chooses a Dissemination packager, and is required.
 *   The &quot;-o&quot; option, which may be repeated, passes options to the packager
 *   (e.g. &quot;metadataOnly&quot; to a DIP packager).
 * </pre>
 *
 * Note that {package-filename} may be "-" for standard input or standard
 * output, respectively.
 *
 * @author Larry Stone
 * @author Tim Donohue
 * @version $Revision$
 */
public class Packager extends DSpaceRunnable<PackagerScriptConfiguration> {

    public static String PACKAGER_FILENAME_PREFIX = "packager";

    /* Various private global settings/options */
    protected String packageType = null;
    protected boolean submit = true;
    protected boolean userInteractionEnabled = true;

    protected String sourceFile = null;
    protected String[] parents = null;
    protected String identifier = null;
    protected PackageParameters pkgParams = new PackageParameters();

    protected static final EPersonService epersonService =
            EPersonServiceFactory.getInstance().getEPersonService();

    protected static final PluginService pluginService =
            CoreServiceFactory.getInstance().getPluginService();

    /**
     * Ingest one or more DSpace objects from package(s) based on the
     * options passed to the 'packager' script.  This method is called
     * for both 'submit' (-s) and 'restore' (-r) modes.
     * <p>
     * Please note that replace (-r -f) mode calls the replace() method instead.
     *
     * @param context    DSpace Context
     * @param sip        PackageIngester which will actually ingest the package
     * @param pkgParams  Parameters to pass to individual packager instances
     * @param sourceFile location of the source package to ingest
     * @param parentObjs Parent DSpace object(s) to attach new object to
     * @throws IOException           if IO error
     * @throws SQLException          if database error
     * @throws FileNotFoundException if file doesn't exist
     * @throws AuthorizeException    if authorization error
     * @throws CrosswalkException    if crosswalk error
     * @throws PackageException      if packaging error
     */
    protected void ingest(Context context, PackageIngester sip, PackageParameters pkgParams, String sourceFile,
                          DSpaceObject parentObjs[])
        throws IOException, SQLException, FileNotFoundException, AuthorizeException, CrosswalkException,
        PackageException {
        // make sure we have an input file
        File pkgFile = new File(sourceFile);

        if (!pkgFile.exists()) {
            handler.logError("Package located at " + sourceFile + " does not exist!");
            throw new RuntimeException("Package located at " + sourceFile + " does not exist!");
        }

        handler.logInfo("Ingesting package located at " + sourceFile);

        //find first parent (if specified) -- this will be the "owner" of the object
        DSpaceObject parent = null;
        if (parentObjs != null && parentObjs.length > 0) {
            parent = parentObjs[0];
        }
        //NOTE: at this point, Parent may be null -- in which case it is up to the PackageIngester
        // to either determine the Parent (from package contents) or throw an error.

        try {
            //If we are doing a recursive ingest, call ingestAll()
            if (pkgParams.recursiveModeEnabled()) {
                handler.logInfo("Also ingesting all referenced packages (recursive mode)..");
                handler.logInfo("This may take a while," +
                        " please check your logs for ongoing status while we process each package.");

                //ingest first package & recursively ingest anything else that package references (child packages, etc)
                List<String> hdlResults = sip.ingestAll(context, parent, pkgFile, pkgParams, null);

                if (hdlResults != null) {
                    //Report total objects created
                    handler.logInfo("CREATED a total of " + hdlResults.size() + " DSpace Objects.");

                    String choiceString = null;
                    //Ask if user wants full list printed to command line, as this may be rather long.
                    if (this.userInteractionEnabled) {
                        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                        handler.logInfo("Would you like to view a list of all objects that were created? [y/n]: ");
                        choiceString = input.readLine();
                    } else {
                        // user interaction disabled -- default answer to 'yes', as
                        // we want to provide user with as detailed a report as possible.
                        choiceString = "y";
                    }

                    // Provide detailed report if user answered 'yes'
                    if (choiceString.equalsIgnoreCase("y")) {
                        for (String result : hdlResults) {
                            DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService()
                                                                   .resolveToObject(context, result);

                            if (dso != null) {

                                if (pkgParams.restoreModeEnabled()) {
                                    handler.logInfo("RESTORED DSpace " + Constants.typeText[dso.getType()] +
                                                           " [ hdl=" + dso.getHandle() + ", dbID=" + dso
                                        .getID() + " ] ");
                                } else {
                                    handler.logInfo("CREATED new DSpace " + Constants.typeText[dso.getType()] +
                                                           " [ hdl=" + dso.getHandle() + ", dbID=" + dso
                                        .getID() + " ] ");
                                }
                            }
                        }
                    }
                }

            } else {
                //otherwise, just one package to ingest
                try {
                    DSpaceObject dso = sip.ingest(context, parent, pkgFile, pkgParams, null);

                    if (dso != null) {
                        if (pkgParams.restoreModeEnabled()) {
                            handler.logInfo("RESTORED DSpace " + Constants.typeText[dso.getType()] +
                                                   " [ hdl=" + dso.getHandle() + ", dbID=" + dso.getID() + " ] ");
                        } else {
                            handler.logInfo("CREATED new DSpace " + Constants.typeText[dso.getType()] +
                                                   " [ hdl=" + dso.getHandle() + ", dbID=" + dso.getID() + " ] ");
                        }
                    }
                } catch (IllegalStateException ie) {
                    // NOTE: if we encounter an IllegalStateException, this means the
                    // handle is already in use and this object already exists.

                    //if we are skipping over (i.e. keeping) existing objects
                    if (pkgParams.keepExistingModeEnabled()) {
                        handler.logInfo(
                            "SKIPPED processing package '" + pkgFile + "', as an Object already exists with this " +
                                "handle.");
                    } else {
                        // Pass this exception on -- which essentially causes a full rollback of all changes (this
                        // is the default)
                        throw ie;
                    }
                }
            }
        } catch (WorkflowException e) {
            throw new PackageException(e);
        }
    }


    /**
     * Disseminate one or more DSpace objects into package(s) based on the
     * options passed to the 'packager' script
     *
     * @param context    DSpace context
     * @param dip        PackageDisseminator which will actually create the package
     * @param dso        DSpace Object to disseminate as a package
     * @param pkgParams  Parameters to pass to individual packager instances
     * @param outputFile File where final package should be saved
     * @throws IOException           if IO error
     * @throws SQLException          if database error
     * @throws FileNotFoundException if file doesn't exist
     * @throws AuthorizeException    if authorization error
     * @throws CrosswalkException    if crosswalk error
     * @throws PackageException      if packaging error
     */
    protected void disseminate(Context context, PackageDisseminator dip,
                               DSpaceObject dso, PackageParameters pkgParams,
                               String outputFile)
        throws IOException, SQLException, FileNotFoundException, AuthorizeException, CrosswalkException,
        PackageException {
        // initialize output file
        File pkgFile = new File(outputFile);

        handler.logInfo("Disseminating DSpace " + Constants.typeText[dso.getType()] +
                               " [ hdl=" + dso.getHandle() + " ] to " + outputFile);

        //If we are doing a recursive dissemination of this object & all its child objects, call disseminateAll()
        if (pkgParams.recursiveModeEnabled()) {
            handler.logInfo("Also disseminating all child objects (recursive mode)..");
            handler.logInfo("This may take a while," +
                    " please check your logs for ongoing status while we process each package.");

            //disseminate initial object & recursively disseminate all child objects as well
            List<File> fileResults = dip.disseminateAll(context, dso, pkgParams, pkgFile);

            if (fileResults != null) {
                //Report total files created
                handler.logInfo("CREATED a total of " + fileResults.size() + " dissemination package files.");

                String choiceString = null;
                //Ask if user wants full list printed to command line, as this may be rather long.
                if (this.userInteractionEnabled) {
                    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                    handler.logInfo("Would you like to view a list of all files that were created? [y/n]: ");
                    choiceString = input.readLine();
                } else {
                    // user interaction disabled -- default answer to 'yes', as
                    // we want to provide user with as detailed a report as possible.
                    choiceString = "y";
                }

                // Provide detailed report if user answered 'yes'
                if (choiceString.equalsIgnoreCase("y")) {
                    for (File result : fileResults) {
                        handler.logInfo("CREATED package file: " + result.getCanonicalPath());
                    }
                }
            }
        } else {
            //otherwise, just disseminate a single object to a single package file
            dip.disseminate(context, dso, pkgParams, pkgFile);

            if (pkgFile.exists()) {
                handler.logInfo("CREATED package file: " + pkgFile.getCanonicalPath());
            }
        }
    }


    /**
     * Replace an one or more existing DSpace objects with the contents of
     * specified package(s) based on the options passed to the 'packager' script.
     * This method is only called for full replaces ('-r -f' options specified)
     *
     * @param context      DSpace Context
     * @param sip          PackageIngester which will actually replace the object with the package
     * @param pkgParams    Parameters to pass to individual packager instances
     * @param sourceFile   location of the source package to ingest as the replacement
     * @param objToReplace DSpace object to replace (may be null if it will be specified in the package itself)
     * @throws IOException           if IO error
     * @throws SQLException          if database error
     * @throws FileNotFoundException if file doesn't exist
     * @throws AuthorizeException    if authorization error
     * @throws CrosswalkException    if crosswalk error
     * @throws PackageException      if packaging error
     */
    protected void replace(Context context, PackageIngester sip, PackageParameters pkgParams, String sourceFile,
                           DSpaceObject objToReplace)
        throws IOException, SQLException, FileNotFoundException, AuthorizeException, CrosswalkException,
        PackageException {

        // make sure we have an input file
        File pkgFile = new File(sourceFile);

        if (!pkgFile.exists()) {
            handler.logError("Package located at " + sourceFile + " does not exist!");
            throw new RuntimeException("Package located at " + sourceFile + " does not exist!");
        }

        handler.logInfo("Replacing DSpace object(s) with package located at " + sourceFile);
        if (objToReplace != null) {
            handler.logInfo("Will replace existing DSpace " + Constants.typeText[objToReplace.getType()] +
                                   " [ hdl=" + objToReplace.getHandle() + " ]");
        }
        // NOTE: At this point, objToReplace may be null.  If it is null, it is up to the PackageIngester
        // to determine which Object needs to be replaced (based on the handle specified in the pkg, etc.)

        try {
            //If we are doing a recursive replace, call replaceAll()
            if (pkgParams.recursiveModeEnabled()) {
                //ingest first object using package & recursively replace anything else that package references
                // (child objects, etc)
                List<String> hdlResults = sip.replaceAll(context, objToReplace, pkgFile, pkgParams);

                if (hdlResults != null) {
                    //Report total objects replaced
                    handler.logInfo("REPLACED a total of " + hdlResults.size() + " DSpace Objects.");

                    String choiceString = null;
                    //Ask if user wants full list printed to command line, as this may be rather long.
                    if (this.userInteractionEnabled) {
                        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                        handler.logInfo("Would you like to view a list of all objects that were replaced? [y/n]: ");
                        choiceString = input.readLine();
                    } else {
                        // user interaction disabled -- default answer to 'yes', as
                        // we want to provide user with as detailed a report as possible.
                        choiceString = "y";
                    }

                    // Provide detailed report if user answered 'yes'
                    if (choiceString.equalsIgnoreCase("y")) {
                        for (String result : hdlResults) {
                            DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService()
                                                                   .resolveToObject(context, result);

                            if (dso != null) {
                                handler.logInfo("REPLACED DSpace " + Constants.typeText[dso.getType()] +
                                                       " [ hdl=" + dso.getHandle() + " ] ");
                            }
                        }
                    }


                }
            } else {
                //otherwise, just one object to replace
                DSpaceObject dso = sip.replace(context, objToReplace, pkgFile, pkgParams);

                if (dso != null) {
                    handler.logInfo("REPLACED DSpace " + Constants.typeText[dso.getType()] +
                                           " [ hdl=" + dso.getHandle() + " ] ");
                }
            }
        } catch (WorkflowException e) {
            throw new PackageException(e);
        }
    }

    @Override
    public PackagerScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager()
                .getServiceByName("packager", PackagerScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        // look for flag to disable all user interaction
        setUserInteraction();

        if (commandLine.hasOption('w')) {
            pkgParams.setWorkflowEnabled(false);
        }
        if (commandLine.hasOption('r')) {
            pkgParams.setRestoreModeEnabled(true);
        }
        // keep-existing is only valid in restoreMode (-r) -- otherwise ignore -k option.
        if (commandLine.hasOption('k') && pkgParams.restoreModeEnabled()) {
            pkgParams.setKeepExistingModeEnabled(true);
        }
        // force-replace is only valid in restoreMode (-r) -- otherwise ignore -f option.
        if (commandLine.hasOption('f') && pkgParams.restoreModeEnabled()) {
            pkgParams.setReplaceModeEnabled(true);
        }
        if (commandLine.hasOption('p')) {
            parents = commandLine.getOptionValues('p');
        }
        if (commandLine.hasOption('t')) {
            packageType = commandLine.getOptionValue('t');
        }
        if (commandLine.hasOption('i')) {
            identifier = commandLine.getOptionValue('i');
        }
        if (commandLine.hasOption('a')) {
            // enable 'recursiveMode' param to packager implementations,
            // in case it helps with packaging or ingestion process
            pkgParams.setRecursiveModeEnabled(true);
        }
        if (commandLine.hasOption('d')) {
            submit = false;
        }
    }

    @Override
    public void internalRun() throws Exception {
        if (commandLine.hasOption('h')) {
            printCustomHelp();
            return;
        }

        // sanity checks on arg list: required args
        if (packageType == null) {
            handler.logError("Missing package type parameter");
            throw new UnsupportedOperationException("Missing package type parameter");
        }

        if (commandLine.hasOption('o')) {
            String popt[] = commandLine.getOptionValues('o');
            for (int i = 0; i < popt.length; ++i) {
                String pair[] = popt[i].split("\\=", 2);
                if (pair.length == 2) {
                    pkgParams.addProperty(pair[0].trim(), pair[1].trim());
                } else if (pair.length == 1) {
                    pkgParams.addProperty(pair[0].trim(), "");
                } else {
                    handler.logError("Illegal package option format: \"" + popt[i] + "\"");
                    throw new UnsupportedOperationException("Illegal package option format: \"" + popt[i] + "\"");
                }
            }
        }

        // find the EPerson, assign to context
        Context context = new Context(Context.Mode.BATCH_EDIT);
        EPerson eperson = getEPerson(context);
        context.setCurrentUser(eperson);

        //If we are in REPLACE mode
        if (pkgParams.replaceModeEnabled()) {
            PackageIngester sip = (PackageIngester) pluginService
                .getNamedPlugin(PackageIngester.class, packageType);
            if (sip == null) {
                handler.logError("Unknown package type: " + packageType);
                throw new RuntimeException("Unknown package type: " + packageType);
            }

            DSpaceObject objToReplace = null;

            //if a specific identifier was specified, make sure it is valid
            if (identifier != null && identifier.length() > 0) {
                objToReplace = HandleServiceFactory.getInstance().getHandleService()
                                                   .resolveToObject(context, identifier);
                if (objToReplace == null) {
                    throw new IllegalArgumentException("Bad identifier/handle -- "
                                                           + "Cannot resolve handle \"" + identifier + "\"");
                }
            }

            String choiceString = null;
            if (userInteractionEnabled) {
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                handler.logWarning("You are running the packager in REPLACE mode.");
                handler.logWarning(
                    "REPLACE mode may be potentially dangerous as it will automatically remove and replace contents" +
                        " within DSpace.");
                handler.logWarning(
                    "We highly recommend backing up all your DSpace contents (files & database) before continuing.");
                handler.logInfo("Would you like to continue? [y/n]: ");
                choiceString = input.readLine();
            } else {
                //user interaction disabled -- default answer to 'yes', otherwise script won't continue
                choiceString = "y";
            }

            if (choiceString.equalsIgnoreCase("y")) {
                handler.logInfo("Beginning replacement process...");

                try {
                    setSourceFile(context, true);

                    //replace the object from the source file
                    replace(context, sip, pkgParams, sourceFile, objToReplace);

                    //commit all changes & exit successfully
                    context.complete();
                } catch (Exception e) {
                    context.abort();
                    throw new RuntimeException(e);
                }
            }

        } else if (submit || pkgParams.restoreModeEnabled()) {
            //else if normal SUBMIT mode (or basic RESTORE mode -- which is a special type of submission)

            PackageIngester sip = (PackageIngester) pluginService
                .getNamedPlugin(PackageIngester.class, packageType);
            if (sip == null) {
                handler.logError("Unknown package type: " + packageType);
                throw new RuntimeException("Unknown package type: " + packageType);
            }

            // validate each parent arg (if any)
            DSpaceObject parentObjs[] = null;
            if (parents != null) {
                handler.logInfo("Destination parents:");

                parentObjs = new DSpaceObject[parents.length];
                for (int i = 0; i < parents.length; i++) {
                    // sanity check: did handle resolve?
                    parentObjs[i] = HandleServiceFactory.getInstance().getHandleService().resolveToObject(context,
                                                                                                          parents[i]);
                    if (parentObjs[i] == null) {
                        throw new IllegalArgumentException(
                            "Bad parent list -- "
                                + "Cannot resolve parent handle \""
                                + parents[i] + "\"");
                    }
                    handler.logInfo((i == 0 ? "Owner: " : "Parent: ")
                                           + parentObjs[i].getHandle());
                }
            }

            try {
                setSourceFile(context, true);

                //ingest the object from the source file
                ingest(context, sip, pkgParams, sourceFile, parentObjs);

                //commit all changes & exit successfully
                context.complete();
            } catch (Exception e) {
                context.abort();
                throw new RuntimeException(e);
            }
        } else {
            // else, if DISSEMINATE mode

            //retrieve specified package disseminator
            PackageDisseminator dip = (PackageDisseminator) pluginService
                .getNamedPlugin(PackageDisseminator.class, packageType);
            if (dip == null) {
                handler.logError("Unknown package type: " + packageType);
                throw new RuntimeException("Unknown package type: " + packageType);
            }

            DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService()
                                                   .resolveToObject(context, identifier);
            if (dso == null) {
                throw new IllegalArgumentException("Bad identifier/handle -- "
                                                       + "Cannot resolve handle \"" + identifier + "\"");
            }

            try {
                setSourceFile(context, false);

                //disseminate the requested object
                disseminate(context, dip, dso, pkgParams, sourceFile);

                attachOutput(context, dso.getID());

                //commit all changes & exit successfully
                context.complete();
            } catch (Exception e) {
                context.abort();
                throw new RuntimeException(e);
            }
        }
    }

    public void printCustomHelp() {
        printHelp();

        StringBuilder helpString = new StringBuilder();
        if (commandLine.hasOption('t')) {
            helpString.append("--------------------------------------------------------------\n");
            helpString.append("Additional options for the " + commandLine.getOptionValue('t') + " packager:\n");
            helpString.append("--------------------------------------------------------------\n");
            helpString.append("(These options may be specified using --option as described above)\n");

            PackageIngester sip = (PackageIngester) pluginService
                .getNamedPlugin(PackageIngester.class, commandLine.getOptionValue('t'));

            if (sip != null) {
                helpString.append("\n\n" + commandLine.getOptionValue('t')
                    + " Submission (SIP) plugin options:\n");
                helpString.append(sip.getParameterHelp() + "\n");
            } else {
                helpString.append("\nNo valid Submission plugin found for "
                        + commandLine.getOptionValue('t') + " type.\n");
            }

            PackageDisseminator dip = (PackageDisseminator) pluginService
                .getNamedPlugin(PackageDisseminator.class, commandLine.getOptionValue('t'));

            if (dip != null) {
                helpString.append("\n\n" + commandLine.getOptionValue('t')
                    + " Dissemination (DIP) plugin options:\n");
                helpString.append(dip.getParameterHelp() + "\n");
            } else {
                helpString.append("\nNo valid Dissemination plugin found for "
                            + commandLine.getOptionValue('t') + " type.\n");
            }
        } else {
            //otherwise, display list of valid packager types
            helpString.append("\nAvailable Submission Package (SIP) types:\n");
            String pn[] = pluginService
                .getAllPluginNames(PackageIngester.class);
            for (int i = 0; i < pn.length; ++i) {
                helpString.append("  " + pn[i] + "\n");
            }
            helpString.append("\nAvailable Dissemination Package (DIP) types:\n");
            pn = pluginService.getAllPluginNames(PackageDisseminator.class);
            for (int i = 0; i < pn.length; ++i) {
                helpString.append("  " + pn[i] + "\n");
            }
        }
        handler.logInfo(helpString.toString());
    }

    public void setSourceFile(Context context, boolean isGiven)
            throws IOException, AuthorizeException {
        if (isGiven) {
            if (!commandLine.hasOption('z') && !commandLine.hasOption('u')) {
                handler.logError("Missing source file");
                throw new UnsupportedOperationException("Missing source file");
            }
            Optional<InputStream> optionalFileStream = Optional.empty();
            if (commandLine.hasOption('z')) {
                // manage zip via upload
                String fileName = commandLine.getOptionValue('z');
                optionalFileStream = handler.getFileStream(context, fileName);
            } else {
                // manage zip via remote url
                String remoteUrl = commandLine.getOptionValue('u');
                optionalFileStream = Optional.ofNullable(new URL(remoteUrl).openStream());
            }
            if (optionalFileStream.isPresent()) {
                sourceFile = FileUtils.getTempDirectoryPath() + File.separator
                        + UUID.randomUUID();
                FileUtils.copyInputStreamToFile(optionalFileStream.get(), new File(sourceFile));
            } else {
                throw new IllegalArgumentException(
                        "Error reading file, the file couldn't be found for filename: " + sourceFile);
            }
        } else {
            sourceFile = FileUtils.getTempDirectoryPath() + File.separator
                    + PACKAGER_FILENAME_PREFIX + "-" + UUID.randomUUID();
        }
    }

    public void setUserInteraction() {
        // from UI the user interaction is disabled
        userInteractionEnabled = false;
    }

    public EPerson getEPerson(Context context) throws SQLException {
        EPerson eperson = epersonService.find(context, this.getEpersonIdentifier());
        if (eperson == null) {
            handler.logError("EPerson cannot be found: " + this.getEpersonIdentifier());
            throw new UnsupportedOperationException("EPerson cannot be found: " + this.getEpersonIdentifier());
        }
        return eperson;
    }

    public void attachOutput(Context context, UUID itemUuid)
            throws FileNotFoundException, IOException, SQLException, AuthorizeException {
        // write input stream on handler
        File source = new File(sourceFile);
        try (InputStream sourceInputStream = new FileInputStream(source)) {
            handler.writeFilestream(context, PACKAGER_FILENAME_PREFIX + "-" + itemUuid,
                    sourceInputStream, packageType.toLowerCase());
        } finally {
            source.delete();
        }
    }
}
