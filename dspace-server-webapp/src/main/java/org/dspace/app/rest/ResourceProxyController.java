/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static java.net.URLDecoder.decode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for downloading resources from external sources.
 */
@RestController
@RequestMapping("/api/resource-proxy")
public class ResourceProxyController {

    /**
     * Endpoint for downloading resources from external sources (Solves CORS issues).
     *
     * @return Requested resource
     */
    @GetMapping
    public ResponseEntity<byte[]> proxyRequest(@RequestParam String externalSourceUrl) throws IOException {
        URL remoteUrl = new URL(decode(externalSourceUrl, StandardCharsets.UTF_8));
        InputStream inputStream = remoteUrl.openStream();
        byte[] fileBytes = StreamUtils.copyToByteArray(inputStream);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Disposition", "attachment; filename=" + getFileName(remoteUrl));
        return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    }

    private static String getFileName(URL url) {
        String path = url.getPath();
        String filename = path.endsWith("/pdf")
            ? path.substring(path.lastIndexOf('/') + 1)
            : path.substring(path.lastIndexOf('/') + 1, path.length() - 4);

        return path.contains("/pdf")
            ? filename + ".pdf"
            : filename;
    }
}
