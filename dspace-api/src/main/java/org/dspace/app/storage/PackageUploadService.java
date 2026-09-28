/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.storage;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;


public class PackageUploadService implements InitializingBean {

    private static final Logger log = LogManager.getLogger(PackageUploadService.class);

    @Autowired
    List<StorageStrategy> strategies;

    @Override
    public void afterPropertiesSet() {
        strategies.forEach(s -> log.info("  — {}", s.getClass().getSimpleName()));
    }

    public void upload(String key, byte[] bytes, String contentType) throws IOException {
        for (StorageStrategy strategy : strategies) {
            if (strategy.canHandle(key)) {
                log.debug("Selected strategy {} for key {}", strategy.getClass().getSimpleName(), key);
                strategy.upload(key, bytes, contentType);
                return;
            }
        }

        throw new IOException("No StorageStrategy found for key: " + key);
    }

}
