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
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

/*
 * @author Jurgen Mamani
 */
public class BulkImportFileUtil {

    public static final String REMOTE = "REMOTE";

    private static final String LOCAL = "LOCAL";

    public static final String FTP = "FTP";

    private static final String HTTP_PREFIX = "http:";

    private static final String HTTPS_PREFIX = "https:";

    private static final String LOCAL_PREFIX = "file:";

    private static final String FTP_PREFIX = "ftp:";

    private static final String UNKNOWN = "UNKNOWN";

    protected DSpaceRunnableHandler handler;

    public BulkImportFileUtil(DSpaceRunnableHandler handler) {
        this.handler = handler;
    }

    public Optional<InputStream> getInputStream(String path) {
        String fileLocationType = getFileLocationTypeByPath(path);

        if (UNKNOWN.equals(fileLocationType)) {
            handler.logWarning("File path is of UNKNOWN type: [" + path + "]");
            return Optional.empty();
        }

        return getInputStream(path, fileLocationType);
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
                    return Optional.of(getInputStreamOfRemoteFile(path));
                case LOCAL:
                    return Optional.of(getInputStreamOfLocalFile(path));
                case FTP:
                    return Optional.of(getInputStreamOfFtpFile(path));
                default:
            }
        } catch (IOException e) {
            handler.logError(e.getMessage());
        }

        return Optional.empty();
    }


    private InputStream getInputStreamOfLocalFile(String path) throws IOException {
        String orginalPath = path;
        path = path.replace(LOCAL_PREFIX + "//", "");
        ConfigurationService configurationService = new DSpace().getConfigurationService();
        String bulkUploadFolder = configurationService.getProperty("bulk-uploads.local-folder");
        if (!StringUtils.startsWith(path, "/")) {
            path = bulkUploadFolder + (StringUtils.endsWith(bulkUploadFolder, "/") ? path : "/" + path);
        }
        File file = new File(path);
        String canonicalPath = file.getCanonicalPath();
        if (!StringUtils.startsWith(canonicalPath, bulkUploadFolder)) {
            throw new IOException("Access to the specified file " + orginalPath + " is not allowed");
        }
        if (!file.exists()) {
            throw new IOException("file " + orginalPath + " is not found");
        }
        return FileUtils.openInputStream(file);
    }

    private InputStream getInputStreamOfRemoteFile(String url) throws IOException {
        return new URL(url).openStream();
    }

    private InputStream getInputStreamOfFtpFile(String url) throws IOException {
        URL urlObject = new URL(url);
        URLConnection urlConnection = urlObject.openConnection();
        return urlConnection.getInputStream();
    }
}
