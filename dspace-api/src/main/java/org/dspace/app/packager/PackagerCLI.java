/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.packager;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.content.packager.PackageDisseminator;
import org.dspace.content.packager.PackageIngester;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.eperson.EPerson;

/**
 * CLI variant for the {@link Packager} class.
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class PackagerCLI extends Packager {

    @Override
    public void printCustomHelp() {
        super.printHelp();

        PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
        if (commandLine.hasOption('t')) {
            System.out.println("\n--------------------------------------------------------------");
            System.out.println("Additional options for the " + commandLine.getOptionValue('t') + " packager:");
            System.out.println("--------------------------------------------------------------");
            System.out.println("(These options may be specified using --option as described above)");

            PackageIngester sip = (PackageIngester) pluginService
                .getNamedPlugin(PackageIngester.class, commandLine.getOptionValue('t'));

            if (sip != null) {
                System.out.println("\n\n" + commandLine.getOptionValue('t')
                    + " Submission (SIP) plugin options:\n");
                System.out.println(sip.getParameterHelp());
            } else {
                System.out.println("\nNo valid Submission plugin found for "
                        + commandLine.getOptionValue('t') + " type.");
            }

            PackageDisseminator dip = (PackageDisseminator) pluginService
                .getNamedPlugin(PackageDisseminator.class, commandLine.getOptionValue('t'));

            if (dip != null) {
                System.out.println("\n\n" + commandLine.getOptionValue('t')
                    + " Dissemination (DIP) plugin options:\n");
                System.out.println(dip.getParameterHelp());
            } else {
                System.out.println("\nNo valid Dissemination plugin found for "
                            + commandLine.getOptionValue('t') + " type.");
            }

        } else {
            //otherwise, display list of valid packager types
            System.out.println("\nAvailable Submission Package (SIP) types:");
            String pn[] = pluginService
                .getAllPluginNames(PackageIngester.class);
            for (int i = 0; i < pn.length; ++i) {
                System.out.println("  " + pn[i]);
            }
            System.out
                .println("\nAvailable Dissemination Package (DIP) types:");
            pn = pluginService.getAllPluginNames(PackageDisseminator.class);
            for (int i = 0; i < pn.length; ++i) {
                System.out.println("  " + pn[i]);
            }
        }
    }

    @Override
    public void setSourceFile(Context context, boolean isGiven) {
        String files[] = commandLine.getArgs();
        if (files.length > 0) {
            if (PACKAGER_FILENAME_PREFIX.equals(files[0]) && files.length > 1) {
                sourceFile = files[1];
            } else {
                sourceFile = files[0];
            }
        } else {
            handler.logError("Missing source file");
            throw new UnsupportedOperationException("Missing source file");
        }
    }

    @Override
    public void setUserInteraction() {
        if (commandLine.hasOption('u')) {
            userInteractionEnabled = false;
        }
    }

    @Override
    public EPerson getEPerson(Context context) throws SQLException {
        String eperson = null;
        if (commandLine.hasOption('e')) {
            eperson = commandLine.getOptionValue('e');
        }
        if (eperson == null) {
            handler.logError("Missing eperson parameter");
            throw new UnsupportedOperationException("EPerson cannot be found: " + this.getEpersonIdentifier());
        }

        return epersonService.findByEmail(context, eperson);
    }

    @Override
    public void attachOutput(Context context, UUID itemUuid) {}
}
