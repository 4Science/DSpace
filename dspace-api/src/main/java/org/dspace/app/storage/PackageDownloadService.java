/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;


public class PackageDownloadService implements InitializingBean {

    private static final Logger log = LogManager.getLogger(PackageDownloadService.class);

    /**
     * All registered download strategies, auto-discovered by Spring.
     * Package-private for test visibility.
     */
    @Autowired
    List<StorageStrategy> strategies;

    @Override
    public void afterPropertiesSet() {
        log.info("PackageDownloadService initialized with {} strategies:", strategies.size());
        strategies.forEach(s -> log.info("  — {}", s.getClass().getSimpleName()));
    }

    /**
     * Downloads the content at {@code url} to {@code target} using the first
     * matching strategy.
     *
     * @param url    source URL
     * @param target destination file on disk
     * @throws IOException if no strategy can handle the URL or the download fails
     */
    public void download(String url, Path target) throws IOException {
        for (StorageStrategy strategy : strategies) {
            if (strategy.canHandle(url)) {
                log.debug("Selected strategy {} for URL {}", strategy.getClass().getSimpleName(), url);
                strategy.download(url, target);
                return;
            }
        }

        throw new IOException("No StorageStrategy found for URL: " + url);
    }

}
