/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.bulkimport.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.services.factory.DSpaceServicesFactory;

/*
 * @author Jurgen Mamani
 */
public class ImportFileUtil {

    private static final Logger log = LogManager.getLogger(ImportFileUtil.class);

    public static final String REMOTE = "REMOTE";

    private static final String LOCAL = "LOCAL";

    public static final String FTP = "FTP";

    private static final String HTTP_PREFIX = "http:";

    private static final String HTTPS_PREFIX = "https:";

    private static final String LOCAL_PREFIX = "file:";

    private static final String FTP_PREFIX = "ftp:";

    private static final String UNKNOWN = "UNKNOWN";

    /**
     * This method check if the given {@param possibleChild} is contained in the specified
     * {@param possibleParent}, i.e. it's a sub-path of it.
     *
     * @param possibleParent
     * @param possibleChild
     * @return true if sub-path, false otherwise.
     */
    private static boolean isChild(File possibleParent, File possibleChild) {
        File parent = possibleChild.getParentFile();
        while (parent != null) {
            if (parent.equals(possibleParent)) {
                return true;
            }
            parent = parent.getParentFile();
        }
        return false;
    }

    private static String[] getAllowedIps() {
        return DSpaceServicesFactory.getInstance()
                                    .getConfigurationService()
                                    .getArrayProperty("allowed.ips.import");
    }

    protected DSpaceRunnableHandler handler;

    public ImportFileUtil() {}

    public ImportFileUtil(DSpaceRunnableHandler handler) {
        this.handler = handler;
    }

    public Optional<InputStream> getInputStream(String path) {
        String fileLocationType = getFileLocationTypeByPath(path);

        if (UNKNOWN.equals(fileLocationType)) {
            logWarning("File path is of UNKNOWN type: [" + path + "]");
            return Optional.empty();
        }

        return getInputStream(path, fileLocationType);
    }

    protected void logWarning(String message) {
        log.warn(message);
        if (handler != null) {
            handler.logWarning(message);
        }
    }

    private String getFileLocationTypeByPath(String path) {
        if (StringUtils.isNotBlank(path)) {
            if (path.startsWith(HTTP_PREFIX) || path.startsWith(HTTPS_PREFIX)) {
                return REMOTE;
            } else if (path.startsWith(LOCAL_PREFIX)) {
                return LOCAL;
            } else if (path.startsWith(FTP_PREFIX)) {
                return FTP;
            } else {
                return UNKNOWN;
            }
        }

        return UNKNOWN;
    }

    private Optional<InputStream> getInputStream(String path, String fileLocationType) {
        try {
            switch (fileLocationType) {
                case REMOTE:
                    return getInputStreamOfRemoteFile(path);
                case LOCAL:
                    return getInputStreamOfLocalFile(path);
                case FTP:
                    return getInputStreamOfFtpFile(path);
                default:
            }
        } catch (IOException e) {
            logError(e.getMessage());
        }

        return Optional.empty();
    }

    private void logError(String message) {
        log.error(message);
        if (handler != null) {
            handler.logError(message);
        }
    }

    private Optional<InputStream> getInputStreamOfLocalFile(String path) throws IOException {
        Path uploadPath = Paths.get(
            DSpaceServicesFactory.getInstance()
                                 .getConfigurationService()
                                 .getProperty("uploads.local-folder")
        );
        File file = uploadPath.resolve(path.replace(LOCAL_PREFIX + "//", ""))
                              .toFile()
                              .getCanonicalFile();
        File possibleParent = uploadPath.toFile().getCanonicalFile();
        if (!isChild(possibleParent, file)) {
            throw new IOException("Access to the specified file " + path + " is not allowed");
        }
        if (!file.exists()) {
            throw new IOException("file " + path + " is not found");
        }
        return Optional.of(FileUtils.openInputStream(file));
    }

    private Optional<InputStream> getInputStreamOfRemoteFile(String path) throws IOException {
        return Optional.of(new URL(path))
                       .filter(url -> Set.of(getAllowedIps()).contains(url.getHost()))
                       .map(this::openStream);
    }

    protected InputStream openStream(URL url) {
        try {
            return url.openStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<InputStream> getInputStreamOfFtpFile(String url) throws IOException {
        URL urlObject = new URL(url);
        URLConnection urlConnection = urlObject.openConnection();
        return Optional.of(urlConnection.getInputStream());
    }
}
