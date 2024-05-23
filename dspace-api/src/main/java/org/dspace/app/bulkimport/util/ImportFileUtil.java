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
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/*
 * @author Jurgen Mamani
 */
public class ImportFileUtil {

    public static final String REMOTE = "REMOTE";

    private static final String LOCAL = "LOCAL";

    public static final String FTP = "FTP";

    private static final String HTTP_PREFIX = "http:";

    private static final String HTTPS_PREFIX = "https:";

    private static final String LOCAL_PREFIX = "file:";

    private static final String FTP_PREFIX = "ftp:";

    private static final String UNKNOWN = "UNKNOWN";

    protected DSpaceRunnableHandler handler;

    public ImportFileUtil(DSpaceRunnableHandler handler) {
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
                    return getInputStreamOfRemoteFile(path);
                case LOCAL:
                    return getInputStreamOfLocalFile(path);
                case FTP:
                    return getInputStreamOfFtpFile(path);
                default:
            }
        } catch (IOException e) {
            handler.logError(e.getMessage());
        }

        return Optional.empty();
    }


    private Optional<InputStream> getInputStreamOfLocalFile(String path) throws IOException {
        String originalPath = path;
        path = path.replace(LOCAL_PREFIX + "//", "");
        ConfigurationService configurationService = new DSpace().getConfigurationService();
        String bulkUploadFolder = configurationService.getProperty("uploads.local-folder");
        if (path.startsWith("../")) {
            validateRelativePath(bulkUploadFolder, originalPath, path);
            path = bulkUploadFolder + (StringUtils.endsWith(bulkUploadFolder, "/") ? path : "/" + path);
        } else if (path.startsWith("/") && !path.startsWith(bulkUploadFolder)) {
            throw new IOException("Access to the specified file " + originalPath + " is not allowed");
        }

        File file = new File(path);
        String canonicalPath = file.getCanonicalPath();
        if (!StringUtils.startsWith(canonicalPath, bulkUploadFolder)) {
            throw new IOException("Access to the specified file " + originalPath + " is not allowed");
        }
        if (!file.exists()) {
            throw new IOException("file " + originalPath + " is not found");
        }
        return Optional.of(FileUtils.openInputStream(file));
    }

    private void validateRelativePath(String bulkUploadFolder, String originalPath, String path) throws IOException {
        String[] splittedUploadFolderPath = bulkUploadFolder.split("/");
        String endUploadFolderPath = splittedUploadFolderPath[splittedUploadFolderPath.length - 1];
        if (!path.contains(endUploadFolderPath)) {
            throw new IOException("Access to the specified file " + originalPath + " is not allowed");
        }
        String[] splittedFilePath = path.split("/");
        int endUploadFolderPathPlace = 0;
        for ( int i = 0; i < splittedFilePath.length - 1; i++) {
            if (Objects.equals(splittedFilePath[i], endUploadFolderPath)) {
                endUploadFolderPathPlace = i;
                break;
            }
        }
        String relativePath = "/" + splittedFilePath[endUploadFolderPathPlace];
        int iterator = endUploadFolderPathPlace - 1;
        while (iterator > 0 || !splittedFilePath[iterator].equals("..")) {
            relativePath = "/" + splittedFilePath[iterator] + relativePath;
            iterator--;
        }
        if (!bulkUploadFolder.contains(relativePath) || relativePath.equals(bulkUploadFolder)) {
            throw new IOException("Access to the specified file " + originalPath + " is not allowed");
        }
    }

    private Optional<InputStream> getInputStreamOfRemoteFile(String path) throws IOException {
        String url = path.replace(HTTPS_PREFIX + "//", "").replace(HTTP_PREFIX + "//", "");
        String[] allowedIpsImport = DSpaceServicesFactory.getInstance().getConfigurationService()
                        .getArrayProperty("allowed.ips.import");
        if (Arrays.stream(allowedIpsImport).noneMatch(allowedIp -> allowedIp.equals(url))) {
            return Optional.empty();
        }
        return Optional.of(generateUrl(path));
    }

    public InputStream generateUrl(String path) throws IOException {
        return new URL(path).openStream();
    }

    private Optional<InputStream> getInputStreamOfFtpFile(String url) throws IOException {
        URL urlObject = new URL(url);
        URLConnection urlConnection = urlObject.openConnection();
        return Optional.of(urlConnection.getInputStream());
    }
}
