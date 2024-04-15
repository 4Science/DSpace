/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.beacon;

import static org.dspace.core.Constants.READ;
import static org.dspace.eperson.Group.ANONYMOUS;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

/**
 * Script to generate the beacon list
 * The configuration for the metadata field and the resolven can be taken from the beacon.cfg file
 * It is possible to use one field for some identifier (e.g. dc.identifier.other) and identify the corresponding values
 * using their resolver,
 * e.g. all values in dc.identifier.other which start with <a href="http://d-nb.info/gnd/">...</a> .
 * Alternative resolvers can be
 * specifed (optionally) which are later normalized to the main identifier
 * Output
 * - verbose: on handler log for debugging
 * - file: print result as process result file with the given filename from the parameter
 * For the specification see: <a href="https://gbv.github.io/beaconspec/beacon.html">...</a>
 *
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 *
 */
public class BeaconFileScript extends DSpaceRunnable<BeaconFileScriptConfiguration<BeaconFileScript>> {

    private AuthorizeService authorizeService;

    private ItemService itemService;

    private ConfigurationService configurationService;

    private Context context;

    private boolean VERBOSE;

    private String PRINTFILE = null;

    private String metadatafield;

    @Override
    public void setup() throws ParseException {
        this.authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.configurationService = new DSpace().getConfigurationService();
        this.metadatafield = configurationService.getProperty("beacon.metadatafield");
        this.VERBOSE = commandLine.hasOption('v');
        if (commandLine.hasOption('f')) {
            this.PRINTFILE = commandLine.getOptionValue('f');
        }
    }

    @Override
    public void internalRun() throws Exception {

        if ((!VERBOSE && PRINTFILE == null) ||
            (VERBOSE && PRINTFILE != null)) {
            throw new Exception("Only one of the output options can be specified");
        }

        if (VERBOSE) {
            context = new Context(Context.Mode.READ_ONLY);
        } else if (PRINTFILE != null) {
            context = new Context(Context.Mode.READ_WRITE);
        }

        String mainresolver = configurationService.getProperty("beacon.mainresolver");
        String[] additionalresolvers = configurationService.getArrayProperty("beacon.additionalresolver");

        assignCurrentUserInContext();
        assignSpecialGroupsInContext();

        if (!this.authorizeService.isAdmin(context)) {
            throw new IllegalArgumentException("The user cannot generate the beacon file");
        }

        StringBuilder sb = new StringBuilder();

        sb.append("#FORMAT: Beacon").append(System.lineSeparator());
        for (String propertykey : configurationService.getPropertyKeys("beacon.header")) {
            String key = propertykey.replace("beacon.header.", "");
            String value = configurationService.getProperty(propertykey);
            if (value != null) {
                sb.append("#").append(key.toUpperCase()).append(": ").append(value).append(System.lineSeparator());
            }
        }

        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        sdf.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Europe/Berlin")));
        String textdate = sdf.format(date);

        sb.append("#TIMESTAMP: ").append(textdate).append(System.lineSeparator());
        try {
            Iterator<Item> items = itemService.findArchivedByMetadataField(context, metadatafield, Item.ANY);
            List<Item> itemlist = new ArrayList<>();
            // filter inactive/hidden profiles

            while (items.hasNext()) {
                Item item = items.next();
                if (!item.isDiscoverable() || !isVisible(item)) {
                    continue;
                }
                itemlist.add(item);
            }

            //Normalize identifier. remove resolver or additionalresolver
            for (Item item : itemlist) {
                List<MetadataValue> mvals = itemService.getMetadataByMetadataString(item, metadatafield);
                for (MetadataValue mval : mvals) {
                    //filter all values not starting with any of the resolver
                    String val = mval.getValue();
                    if (configurationService.getBooleanProperty("beacon.metadatafield.filterresolver")) {
                        boolean skip = !val.startsWith(mainresolver);
                        //check main resolver
                        //check additional resolvers
                        if (additionalresolvers != null) {
                            for (String additionalresolver : additionalresolvers) {
                                if (val.startsWith(additionalresolver)) {
                                    skip = false;
                                    break;
                                }
                            }
                        }
                        if (skip) {
                            continue;
                        }
                    }
                    //normalize identifiers and replace the values
                    if (val != null && val.startsWith(mainresolver)) {
                        val = val.replace(mainresolver, "");
                    } else if (val != null && additionalresolvers != null) {
                        for (String additionalresolver : additionalresolvers) {
                            if (val.startsWith(additionalresolver)) {
                                val = val.replace(additionalresolver, "");
                                break;
                            }
                        }
                    }
                    sb.append(val).append(System.lineSeparator());
                    break;
                }

            }
            String result = sb.toString();
            if (VERBOSE) {
                handler.logInfo(result);
            } else if (PRINTFILE != null) {
                try {
                    handler.writeFilestream(context, PRINTFILE,
                        new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)), "BEACON");
                } catch (Exception e) {
                    handler.logError(e.getMessage());
                }
            }
            context.complete();
            handler.logInfo("Beacon file completed successfully");
        } catch (Exception e) {
            handler.handleException(e);
            context.abort();
        } finally {
            if (context.isValid()) {
                context.close();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public BeaconFileScriptConfiguration<BeaconFileScript> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("beacon-file",
            BeaconFileScriptConfiguration.class);
    }

    private void assignCurrentUserInContext() throws SQLException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    private void assignSpecialGroupsInContext() throws SQLException {
        for (UUID uuid : handler.getSpecialGroups()) {
            context.setSpecialGroup(uuid);
        }
    }

    public boolean isVisible(Item item) {
        return item.getResourcePolicies().stream()
            .filter(policy -> policy.getGroup() != null)
            .anyMatch(policy -> READ == policy.getAction() && ANONYMOUS.equals(policy.getGroup().getName()));
    }

}
