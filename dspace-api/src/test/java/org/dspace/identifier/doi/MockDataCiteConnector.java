/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;

import org.apache.http.client.methods.HttpUriRequest;

/**
 * @author Andrea Bollini
 */
public class MockDataCiteConnector extends DataCiteConnector {

    /**
     * Mock the internal method to send requests prepared by the caller to DataCite
     * to simulate a successful response (200, 201) as appropriate
     */
    protected DataCiteResponse sendHttpRequest(HttpUriRequest req, String doi) throws DOIIdentifierException {

        if (doi != null && doi.contains("invalid-doi")) {
            return new DataCiteResponse(500, "OK");
        }
        if (req.getMethod().contains("GET") || req.getMethod().contains("DELETE")) {
            return new DataCiteResponse(200, "OK");
        } else {
            return new DataCiteResponse(201, "OK");
        }
    }
}