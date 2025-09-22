/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.viaf.contributors;

import com.fasterxml.jackson.databind.JsonNode;

public interface ViafIdExtractor {

    public String getViafId(JsonNode jsonNode, int recordNumberForNameSpace);

}
