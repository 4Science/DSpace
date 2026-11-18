/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.bulkimport.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;

import org.dspace.AbstractDSpaceTest;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

public class ImportFileUtilTest extends AbstractDSpaceTest {

    private ImportFileUtil importFileUtil;

    private ConfigurationService configurationService;
    private MockedStatic<DSpaceServicesFactory> dSpaceServicesFactoryMockedStatic;
    private URLConnection urlConnection;

    @Before
    public void setUp() {
        // Initialize the main class under test
        importFileUtil = spy(new ImportFileUtil());

        // Setup common mocks
        dSpaceServicesFactoryMockedStatic = mockStatic(DSpaceServicesFactory.class);
        DSpaceServicesFactory dSpaceServicesFactory = mock(DSpaceServicesFactory.class);
        configurationService = mock(ConfigurationService.class);
        urlConnection = mock(URLConnection.class);


        // Setup common mock behavior
        dSpaceServicesFactoryMockedStatic.when(DSpaceServicesFactory::getInstance)
                                         .thenReturn(dSpaceServicesFactory);
        when(dSpaceServicesFactory.getConfigurationService()).thenReturn(configurationService);
    }

    @After
    public void tearDown() {
        if (dSpaceServicesFactoryMockedStatic != null) {
            dSpaceServicesFactoryMockedStatic.close();
        }
    }

    @Test
    public void getInputStream_shouldReturnStream_whenRemoteHostIsInAllowedList() throws Exception {
        // Given
        String allowedHost = "example.com";
        when(configurationService.getArrayProperty("allowed.ips.import"))
            .thenReturn(new String[] {allowedHost});

        String path = "http://example.com/file.txt";
        URL url = mock(URL.class);
        InputStream mockStream = mock(InputStream.class);

        doReturn(url).when(importFileUtil).getUrl(path);
        when(url.getProtocol()).thenReturn("http");
        when(url.getHost()).thenReturn(allowedHost);
        doReturn(mockStream).when(importFileUtil).openStream(url);

        // When
        Optional<InputStream> result = importFileUtil.getInputStream(path);

        // Then
        assertTrue(result.isPresent());
        verify(importFileUtil).openStream(url);
    }

    @Test
    public void getInputStream_shouldReturnEmpty_whenNoIpRestrictionConfigured() throws Exception {
        // Given an empty allowlist, remote retrieval must fail closed (SSRF fix, DSAP-2026-004).
        when(configurationService.getArrayProperty("allowed.ips.import"))
            .thenReturn(new String[] {});

        String path = "http://example.com/file.txt";
        URL url = mock(URL.class);

        doReturn(url).when(importFileUtil).getUrl(path);
        when(url.getProtocol()).thenReturn("http");
        when(url.getHost()).thenReturn("example.com");

        // When
        Optional<InputStream> result = importFileUtil.getInputStream(path);

        // Then: nothing is fetched and no stream is opened.
        assertFalse(result.isPresent());
        verify(importFileUtil, never()).openStream(url);
    }

    @Test
    public void getInputStream_shouldReturnEmpty_whenAllowlistIsNull() throws Exception {
        // Given a null allowlist (property absent), remote retrieval must fail closed.
        when(configurationService.getArrayProperty("allowed.ips.import"))
            .thenReturn(null);

        String path = "http://example.com/file.txt";
        URL url = mock(URL.class);

        doReturn(url).when(importFileUtil).getUrl(path);
        when(url.getProtocol()).thenReturn("http");
        when(url.getHost()).thenReturn("example.com");

        // When
        Optional<InputStream> result = importFileUtil.getInputStream(path);

        // Then
        assertFalse(result.isPresent());
        verify(importFileUtil, never()).openStream(url);
    }

    @Test
    public void getInputStream_shouldReturnStream_whenWildcardConfigured() throws Exception {
        // Given the explicit "allow all" escape hatch, any host is permitted.
        when(configurationService.getArrayProperty("allowed.ips.import"))
            .thenReturn(new String[] {"*"});

        String path = "http://anything.example/file.txt";
        URL url = mock(URL.class);
        InputStream mockStream = mock(InputStream.class);

        doReturn(url).when(importFileUtil).getUrl(path);
        when(url.getProtocol()).thenReturn("http");
        when(url.getHost()).thenReturn("anything.example");
        doReturn(mockStream).when(importFileUtil).openStream(url);

        // When
        Optional<InputStream> result = importFileUtil.getInputStream(path);

        // Then
        assertTrue(result.isPresent());
        verify(importFileUtil).openStream(url);
    }

    @Test
    public void getInputStream_shouldNotThrow_whenAllowlistContainsNullElement() throws Exception {
        // Given a null element in the configured array, the set must still build (no NPE)
        // and an allowed host must still match.
        String allowedHost = "example.com";
        when(configurationService.getArrayProperty("allowed.ips.import"))
            .thenReturn(new String[] {null, allowedHost});

        String path = "http://example.com/file.txt";
        URL url = mock(URL.class);
        InputStream mockStream = mock(InputStream.class);

        doReturn(url).when(importFileUtil).getUrl(path);
        when(url.getProtocol()).thenReturn("http");
        when(url.getHost()).thenReturn(allowedHost);
        doReturn(mockStream).when(importFileUtil).openStream(url);

        // When
        Optional<InputStream> result = importFileUtil.getInputStream(path);

        // Then
        assertTrue(result.isPresent());
        verify(importFileUtil).openStream(url);
    }

    @Test
    public void getInputStream_shouldReturnEmpty_whenRemoteSchemeIsNotHttp() throws Exception {
        // Given a non-http(s) URL reaching the remote path, retrieval must be refused
        // before the allowlist is even consulted.
        String path = "http://example.com/file.txt";
        URL url = mock(URL.class);

        doReturn(url).when(importFileUtil).getUrl(path);
        when(url.getProtocol()).thenReturn("gopher");

        // When
        Optional<InputStream> result = importFileUtil.getInputStream(path);

        // Then
        assertFalse(result.isPresent());
        verify(importFileUtil, never()).openStream(url);
    }

    @Test
    public void getInputStream_shouldReturnEmpty_whenRemoteHostIsNotAllowed() throws IOException {
        // Given
        when(configurationService.getArrayProperty("allowed.ips.import"))
            .thenReturn(new String[] {"otherdomain.com"});

        String path = "http://example.com/file.txt";
        URL url = mock(URL.class);
        doReturn(url).when(importFileUtil).getUrl(path);
        when(url.getProtocol()).thenReturn("http");
        when(url.getHost()).thenReturn("example.com");

        // When
        Optional<InputStream> result = importFileUtil.getInputStream(path);

        // Then
        assertFalse(result.isPresent());
        verify(importFileUtil, never()).openStream(url);
    }


    @Test
    public void getInputStream_shouldReturnEmpty_whenProtocolIsUnknown() {
        // Given
        String path = "unknown://file.txt";

        // When
        Optional<InputStream> result = importFileUtil.getInputStream(path);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    public void getInputStream_shouldReturnEmpty_whenLocalFileDoesNotExist() {
        // Given
        when(configurationService.getProperty("uploads.local-folder"))
            .thenReturn("/local/uploads");

        String path = "file://file.txt";

        // When
        Optional<InputStream> result = importFileUtil.getInputStream(path);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    public void getInputStream_shouldReturnStream_whenFtpConnectionSucceeds() throws Exception {
        // Given the FTP host is allowlisted
        when(configurationService.getArrayProperty("allowed.ips.import"))
            .thenReturn(new String[] {"example.com"});

        String ftpPath = "ftp://example.com/file.txt";
        URL mockUrl = mock(URL.class);
        InputStream mockInputStream = mock(InputStream.class);

        doReturn(mockUrl).when(importFileUtil).getUrl(ftpPath);
        when(mockUrl.getHost()).thenReturn("example.com");
        when(mockUrl.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getInputStream()).thenReturn(mockInputStream);

        // When
        Optional<InputStream> result = importFileUtil.getInputStream(ftpPath);

        // Then
        assertTrue(result.isPresent());
        verify(urlConnection).getInputStream();
        // The same connect/read timeouts applied to HTTP(S) are applied to FTP too.
        verify(urlConnection).setConnectTimeout(30_000);
        verify(urlConnection).setReadTimeout(30_000);
    }

    @Test
    public void getInputStream_shouldReturnStream_whenFtpHostMatchesWildcard() throws Exception {
        // Given the explicit "allow all" escape hatch, FTP retrieval from any host is permitted.
        when(configurationService.getArrayProperty("allowed.ips.import"))
            .thenReturn(new String[] {"*"});

        String ftpPath = "ftp://any.example/file.txt";
        URL mockUrl = mock(URL.class);
        InputStream mockInputStream = mock(InputStream.class);

        doReturn(mockUrl).when(importFileUtil).getUrl(ftpPath);
        when(mockUrl.getHost()).thenReturn("any.example");
        when(mockUrl.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getInputStream()).thenReturn(mockInputStream);

        // When
        Optional<InputStream> result = importFileUtil.getInputStream(ftpPath);

        // Then
        assertTrue(result.isPresent());
        verify(urlConnection).getInputStream();
    }

    @Test
    public void getInputStream_shouldReturnEmpty_whenFtpStreamIsNull() throws Exception {
        // Given the FTP host is allowlisted
        when(configurationService.getArrayProperty("allowed.ips.import"))
            .thenReturn(new String[] {"example.com"});

        DSpaceRunnableHandler handler = mock(DSpaceRunnableHandler.class);
        ImportFileUtil utilWithHandler = spy(new ImportFileUtil(handler));

        String ftpPath = "ftp://example.com/file.txt";
        URL mockUrl = mock(URL.class);

        doReturn(mockUrl).when(utilWithHandler).getUrl(ftpPath);
        when(mockUrl.getHost()).thenReturn("example.com");
        when(mockUrl.openConnection()).thenReturn(urlConnection);
        when(urlConnection.getInputStream()).thenReturn(null);

        // When
        Optional<InputStream> result = utilWithHandler.getInputStream(ftpPath);

        // Then: the failure is logged as an error and an empty result is returned.
        assertFalse(result.isPresent());
        verify(urlConnection).getInputStream();
        verify(handler).logError("Failed to obtain InputStream for FTP URL: ftp://example.com/file.txt. "
            + "getInputStream() returned null");
    }

    @Test
    public void getInputStream_shouldReturnEmpty_whenFtpHostNotAllowed() throws Exception {
        // Given a non-empty allowlist that does not contain the FTP host, the FTP path must
        // reject the URL without opening any connection.
        when(configurationService.getArrayProperty("allowed.ips.import"))
            .thenReturn(new String[] {"otherdomain.com"});

        String ftpPath = "ftp://example.com/file.txt";
        URL mockUrl = mock(URL.class);

        doReturn(mockUrl).when(importFileUtil).getUrl(ftpPath);
        when(mockUrl.getHost()).thenReturn("example.com");

        // When
        Optional<InputStream> result = importFileUtil.getInputStream(ftpPath);

        // Then: no connection is ever opened.
        assertFalse(result.isPresent());
        verify(mockUrl, never()).openConnection();
    }

    @Test
    public void getInputStream_shouldReturnEmpty_whenFtpAllowlistIsNull() throws Exception {
        // Given a null allowlist (property absent), the FTP path must fail closed too.
        when(configurationService.getArrayProperty("allowed.ips.import"))
            .thenReturn(null);

        String ftpPath = "ftp://example.com/file.txt";
        URL mockUrl = mock(URL.class);

        doReturn(mockUrl).when(importFileUtil).getUrl(ftpPath);
        when(mockUrl.getHost()).thenReturn("example.com");

        // When
        Optional<InputStream> result = importFileUtil.getInputStream(ftpPath);

        // Then: no connection is ever opened.
        assertFalse(result.isPresent());
        verify(mockUrl, never()).openConnection();
    }

}
