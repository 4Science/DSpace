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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;

import org.dspace.AbstractDSpaceTest;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

public class ImportFileUtilTest extends AbstractDSpaceTest {

    private ImportFileUtil importFileUtil;

    private DSpaceServicesFactory dSpaceServicesFactory;
    private ConfigurationService configurationService;
    private MockedStatic<DSpaceServicesFactory> dSpaceServicesFactoryMockedStatic;

    @Before
    public void setUp() {
        // Initialize the main class under test
        importFileUtil = spy(new ImportFileUtil());

        // Setup common mocks
        dSpaceServicesFactoryMockedStatic = mockStatic(DSpaceServicesFactory.class);
        dSpaceServicesFactory = mock(DSpaceServicesFactory.class);
        configurationService = mock(ConfigurationService.class);

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
        when(url.getHost()).thenReturn(allowedHost);
        doReturn(mockStream).when(importFileUtil).openStream(url);

        // When
        Optional<InputStream> result = importFileUtil.getInputStream(path);

        // Then
        assertTrue(result.isPresent());
        verify(importFileUtil).openStream(url);
    }

    @Test
    public void getInputStream_shouldReturnStream_whenNoIpRestrictionConfigured() throws Exception {
        // Given
        when(configurationService.getArrayProperty("allowed.ips.import"))
            .thenReturn(new String[] {});

        String path = "http://example.com/file.txt";
        URL url = mock(URL.class);
        InputStream mockStream = mock(InputStream.class);

        doReturn(url).when(importFileUtil).getUrl(path);
        doReturn(mockStream).when(importFileUtil).openStream(url);

        // When
        Optional<InputStream> result = importFileUtil.getInputStream(path);

        // Then
        assertTrue(result.isPresent());
        verify(importFileUtil).openStream(url);
    }

    @Test
    public void getInputStream_shouldReturnEmpty_whenRemoteHostIsNotAllowed() throws IOException {
        // Given
        when(configurationService.getArrayProperty("allowed.ips.import"))
            .thenReturn(new String[] {"otherdomain.com"});

        String path = "http://example.com/file.txt";
        URL url = mock(URL.class);
        doReturn(url).when(importFileUtil).getUrl(path);
        when(url.getHost()).thenReturn("example.com");

        // When
        Optional<InputStream> result = importFileUtil.getInputStream(path);

        // Then
        assertFalse(result.isPresent());
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
    public void getInputStream_shouldReturnEmpty_whenLocalFileDoesNotExist() throws Exception {
        // Given
        when(configurationService.getProperty("uploads.local-folder"))
            .thenReturn("/local/uploads");

        String path = "file://file.txt";

        // When
        Optional<InputStream> result = importFileUtil.getInputStream(path);

        // Then
        assertFalse(result.isPresent());
    }
}
