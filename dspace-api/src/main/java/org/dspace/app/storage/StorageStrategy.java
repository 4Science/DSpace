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

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public interface StorageStrategy {

    /**
     * Returns true if this strategy can handle the given URL.
     *
     * @param url the source URL
     * @return true if this strategy should be used for the URL
     */
    boolean canHandle(String url);

    /**
     * Downloads the content at {@code url} to {@code target} on disk.
     * Implementations should use paginated I/O (FileChannel / ReadableByteChannel)
     * and never buffer the full file into heap memory.
     *
     * @param url    the source URL
     * @param target the destination file path
     * @throws IOException           if the download fails
     * @throws IllegalStateException if {@link #canHandle(String)} would return false
     */
    void download(String url, Path target) throws IOException;

    void upload(String key, byte[] bytes, String contentType) throws IOException;

}
