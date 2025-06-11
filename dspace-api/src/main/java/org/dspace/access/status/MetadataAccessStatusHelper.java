/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.access.status;

import static org.dspace.content.Item.ANY;

import java.sql.SQLException;
import java.util.Date;

import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.joda.time.LocalDate;

/**
 * implementation of the access status helper.
 * The getAccessStatusFromItem method provides a simple logic to
 * retrieve the access status of an item based on the provided metadata
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class MetadataAccessStatusHelper implements AccessStatusHelper {

    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private ConfigurationService  configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();

    private String accessStatusMetadata;
    private String availabilityDateMetadata;

    public MetadataAccessStatusHelper() {
        super();
        this.accessStatusMetadata = configurationService.getProperty(
                "access.status.access-status-metadata", "datacite.rights");
        this.availabilityDateMetadata = configurationService.getProperty(
                "access.status.availability-date-metadata", "datacite.available");
    }

    /**
     * Determines the access status of an item based on metadata.
     *
     * @param context     the DSpace context
     * @param item        the item to check for embargoes
     * @param threshold   the embargo threshold date
     * @return an access status value
     */
    @Override
    public String getAccessStatusFromItem(Context context, Item item, Date threshold) {

        if (item == null) {
            return UNKNOWN;
        }

        String status = itemService.getMetadataFirstValue(item,
                new MetadataFieldName(accessStatusMetadata), ANY);
        String date = itemService.getMetadataFirstValue(item,
                new MetadataFieldName(availabilityDateMetadata), ANY);

        if (status == null) {
            return UNKNOWN;
        }

        if (EMBARGO.equals(status)) {
            if (date != null) {
                LocalDate embargoDate = parseDate(date);
                if (embargoDate == null || embargoDate.isBefore(LocalDate.now())) {
                    return OPEN_ACCESS;
                }
            }
        }

        return AccessStatus.toAccessStatus(status).getStatus();
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     *
     * @param context     the DSpace context
     * @param item        the item to embargo
     * @return an embargo date
     */
    @Override
    public String getEmbargoFromItem(Context context, Item item, Date threshold) throws SQLException {

        // If Item status is not "embargo" then return a null embargo date.
        String accessStatus = getAccessStatusFromItem(context, item, threshold);

        if (item == null || !accessStatus.equals(EMBARGO)) {
            return null;
        }

        return itemService.getMetadataFirstValue(item, new MetadataFieldName(availabilityDateMetadata), ANY);
    }

}
