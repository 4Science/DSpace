/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.apache.fontbox.ttf.OpenTypeScript.UNKNOWN;
import static org.dspace.access.status.DefaultAccessStatusHelper.EMBARGO;
import static org.dspace.access.status.DefaultAccessStatusHelper.METADATA_ONLY;
import static org.dspace.access.status.DefaultAccessStatusHelper.OPEN_ACCESS;
import static org.dspace.access.status.DefaultAccessStatusHelper.RESTRICTED;
import static org.dspace.access.status.DefaultAccessStatusHelper.STATUS_FOR_ANONYMOUS;
import static org.dspace.access.status.DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER;

import java.time.LocalDate;

/**
 * Utility class for access status
 */
public class AccessStatus {
    /**
     * the status value
     */
    private String status;

    /**
     * the availability date if required
     */
    private LocalDate availabilityDate;

    /**
     * Construct a new access status
     *
     * @param status           the status value
     * @param availabilityDate the availability date
     */
    public AccessStatus(String status, LocalDate availabilityDate) {
        this.status = status;
        this.availabilityDate = availabilityDate;
    }

    /**
     * @return Returns the status value.
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status The status value.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return Returns the availability date.
     */
    public LocalDate getAvailabilityDate() {
        return availabilityDate;
    }

    /**
     * @param availabilityDate The availability date.
     */
    public void setAvailabilityDate(LocalDate availabilityDate) {
        this.availabilityDate = availabilityDate;
    }

    /**
     * Map a given status and date into a valid AccessStatus.
     *
     * @param status the requested status
     * @param date   the availability date, may be null
     * @return a resolved AccessStatus instance
     */
    public static AccessStatus toAccessStatus(String status, LocalDate date) {
        if (status == null) {
            return new AccessStatus(UNKNOWN, null);
        }

        switch (status) {
            case "openaccess":
                return new AccessStatus(OPEN_ACCESS, null);
            case "embargo":
                return new AccessStatus(EMBARGO, date);
            case "metadata-only":
                return new AccessStatus(METADATA_ONLY, null);
            case "restricted":
                return new AccessStatus(RESTRICTED, null);
            case "current":
                return new AccessStatus(STATUS_FOR_CURRENT_USER, null);
            case "anonymous":
                return new AccessStatus(STATUS_FOR_ANONYMOUS, null);

            default:
                return new AccessStatus(UNKNOWN, null);
        }
    }

}
