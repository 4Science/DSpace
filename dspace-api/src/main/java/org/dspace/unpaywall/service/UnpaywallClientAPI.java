/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.unpaywall.service;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Unpaywall Client Api
 */
public interface UnpaywallClientAPI {

    File downloadResource(String pdfUrl) throws IOException;

    Optional<String> callUnpaywallApi(String doi);

}
