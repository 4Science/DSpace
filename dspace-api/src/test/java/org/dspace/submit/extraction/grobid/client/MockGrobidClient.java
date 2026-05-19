/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.extraction.grobid.client;

import java.io.InputStream;

import org.dspace.submit.extraction.grobid.TEI;

public class MockGrobidClient extends GrobidClientImpl {

    public MockGrobidClient(String baseUrl) {
        super(baseUrl);
    }

    @Override
    public TEI processHeaderDocument(InputStream inputStream, ConsolidateHeaderEnum consolidateHeader) {
        return null;
    }

}
