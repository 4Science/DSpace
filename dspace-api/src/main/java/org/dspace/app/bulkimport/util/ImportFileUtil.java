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
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    public static final String REMOTE = "REMOTE";
    public static final String FTP = "FTP";
    private static final Logger log = LogManager.getLogger(ImportFileUtil.class);
    private static final String LOCAL = "LOCAL";
    private static final String HTTP_PREFIX = "http:";

    private static final String HTTPS_PREFIX = "https:";

    private static final String LOCAL_PREFIX = "file:";

    private static final String FTP_PREFIX = "ftp:";

    private static final String UNKNOWN = "UNKNOWN";

    /**
     * Wildcard value for {@code allowed.ips.import}. When present among the
     * configured values, remote retrieval is allowed for any host. This is the
     * deliberate, documented opt-in for operators who want the previous
     * fetch-any-host behavior; it must be configured explicitly.
     */
    private static final String ALLOW_ALL = "*";

    /**
     * Connect and read timeout (milliseconds) applied to remote/FTP fetches so a
     * slow or non-responsive host cannot hang the import indefinitely.
     */
    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    protected DSpaceRunnableHandler handler;

    public ImportFileUtil() {
    }

    public ImportFileUtil(DSpaceRunnableHandler handler) {
        this.handler = handler;
    }

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

    /**
     * Build the configured allowlist, discarding null and blank entries so a stray
     * null element cannot throw and a blank entry cannot act as an unintended match.
     *
     * @return the set of non-blank allowed hosts (never null)
     */
    private static Set<String> getAllowedHosts() {
        String[] configured = getAllowedIps();
        if (configured == null) {
            return Set.of();
        }
        return Arrays.stream(configured)
                     .filter(StringUtils::isNotBlank)
                     .map(String::trim)
                     .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Fail-closed allowlist decision shared by the remote and FTP fetch paths.
     *
     * <p>Retrieval is permitted only when the allowlist explicitly contains the
     * host, or when it contains the wildcard {@code *}. An empty or blank
     * allowlist permits nothing: this is the secure default. This replaces the
     * previous fail-open behavior where an empty allowlist authorized a fetch to
     * any host (SSRF primitive, DSAP-2026-004).
     *
     * @param host the URL host to check
     * @param path the original path, for logging
     * @return true if retrieval from the host is allowed
     */
    private boolean isHostAllowed(String host, String path) {
        Set<String> allowedHosts = getAllowedHosts();

        if (allowedHosts.isEmpty()) {
            logWarning(String.format(
                "Remote retrieval is disabled: 'allowed.ips.import' is empty. Rejected path: %s", path));
            return false;
        }

        if (allowedHosts.contains(ALLOW_ALL) || allowedHosts.contains(host)) {
            return true;
        }

        logWarning(String.format("Domain '%s' is not in the allowed list. Path: %s", host, path));
        return false;
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
        URL url = getUrl(path);

        if (!isHttpScheme(url)) {
            logWarning(String.format("Scheme '%s' is not allowed for remote retrieval. Path: %s",
                                     url.getProtocol(), path));
            return Optional.empty();
        }

        if (!isHostAllowed(url.getHost(), path)) {
            return Optional.empty();
        }

        return Optional.ofNullable(openStream(url));
    }

    private boolean isHttpScheme(URL url) {
        String protocol = url.getProtocol();
        return "http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol);
    }

    protected InputStream openStream(URL url) {
        try {
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            return connection.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected URL getUrl(String path) throws IOException {
        return new URL(path);
    }

    private Optional<InputStream> getInputStreamOfFtpFile(String path) throws IOException {
        URL urlObject = getUrl(path);

        if (!isHostAllowed(urlObject.getHost(), path)) {
            return Optional.empty();
        }

        URLConnection urlConnection = urlObject.openConnection();
        urlConnection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        urlConnection.setReadTimeout(READ_TIMEOUT_MS);
        InputStream inputStream = urlConnection.getInputStream();
        if (inputStream == null) {
            logError(
                String.format("Failed to obtain InputStream for FTP URL: %s. getInputStream() returned null", path));
            return Optional.empty();
        }
        return Optional.of(inputStream);
    }
}
