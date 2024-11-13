/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.unpaywall.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.unpaywall.service.UnpaywallClientAPI;

public class MockUnpaywallClientAPIImpl implements UnpaywallClientAPI {
    private static final String BASE_UNPAYWALL_DIR_PATH = "org/dspace/app/unpaywall/";


    @Override
    public File downloadResource(String pdfUrl) throws IOException {
        String responseFileName = BASE_UNPAYWALL_DIR_PATH.concat("unpaywall-api-resource.pdf");
        return new File(getClass().getClassLoader().getResource(responseFileName).getFile());
    }

    @Override
    public Optional<String> callUnpaywallApi(String doi) {
        if (StringUtils.endsWith(doi, "/not-found")) {
            return Optional.empty();
        } else {
            String responseFileName = BASE_UNPAYWALL_DIR_PATH.concat("unpaywall-api-response.json");
            try (InputStream unpaywallResponseStream = getClass().getClassLoader()
                        .getResourceAsStream(responseFileName)) {
                String unpaywallResponse = IOUtils.toString(unpaywallResponseStream, "UTF-8");
                return Optional.of(unpaywallResponse);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

}
