/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.access.status;

import static org.dspace.content.Item.ANY;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.dspace.content.AccessStatus;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * implementation of the access status helper.
 * The getAccessStatusFromItem method provides a simple logic to
 * retrieve the access status of an item based on the provided metadata
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class MetadataAccessStatusHelper extends DefaultAccessStatusHelper {

    private final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    private final String accessStatusMetadata;
    private final String availabilityDateMetadata;

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
     * @param context   the DSpace context
     * @param item      the item to check for embargoes
     * @param threshold the embargo threshold date
     * @return an access status value
     */
    @Override
    public AccessStatus getAccessStatusFromItem(Context context, Item item, LocalDate threshold, String type) {
        if (item == null) {
            return new AccessStatus(UNKNOWN, null);
        }
        ItemService itemService =
            ContentServiceFactory.getInstance().getItemService();
        String status = itemService.getMetadataFirstValue(item,
                                                          new MetadataFieldName(accessStatusMetadata), ANY);
        String date = itemService.getMetadataFirstValue(item,
                                                        new MetadataFieldName(availabilityDateMetadata), ANY);

        if (status == null) {
            return new AccessStatus(UNKNOWN, null);
        }

        LocalDate embargoDate = null;
        if (EMBARGO.equals(status)) {
            if (date != null) {
                embargoDate = parseDate(date);
                if (embargoDate == null || embargoDate.isBefore(LocalDate.now())) {
                    return new AccessStatus(OPEN_ACCESS, embargoDate);
                }
            }
        }

        return AccessStatus.toAccessStatus(status, embargoDate);
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
