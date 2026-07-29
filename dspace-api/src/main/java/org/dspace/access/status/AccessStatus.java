/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.access.status;

/**
 * responsible to process what’s stored in datacite.rights
 * or any other configured metadata to the constant values
 * of access status
 *
 * @author Mohamed Eskander(mohamed.eskander at 4science.com)
 */
public enum AccessStatus {
    OPEN_ACCESS(AccessStatusHelper.OPEN_ACCESS),
    EMBARGO(AccessStatusHelper.EMBARGO),
    METADATA_ONLY(AccessStatusHelper.METADATA_ONLY),
    RESTRICTED(AccessStatusHelper.RESTRICTED),
    UNKNOWN(AccessStatusHelper.UNKNOWN);

    private final String status;

    AccessStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public static AccessStatus toAccessStatus(String value) {
        if (value == null) {
            return UNKNOWN;
        }

        switch (value.toLowerCase()) {
            case "openaccess":
                return OPEN_ACCESS;
            case "embargo":
                return EMBARGO;
            case "metadata-only":
                return METADATA_ONLY;
            case "restricted":
                return RESTRICTED;
            default:
                return UNKNOWN;
        }
    }
}
