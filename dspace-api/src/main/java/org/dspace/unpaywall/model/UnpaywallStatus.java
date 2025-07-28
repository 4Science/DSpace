/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.unpaywall.model;

/**
 * Class representing an unpaywall api call status.
 */
public enum UnpaywallStatus {

    PENDING,
    NOT_FOUND,
    SUCCESSFUL,
    NO_FILE,
    IMPORTED
}
