/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.dspace.app.rest.model.RootRest;
import org.dspace.app.util.Util;
import org.dspace.core.CrisConstants;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * This class' purpose is to test the RootConvertor class.
 */
@RunWith(MockitoJUnitRunner.class)
public class RootConverterTest {

    @InjectMocks
    private RootConverter rootConverter;

    @Mock
    private ConfigurationService configurationService;

    private MockHttpServletRequest request = new MockHttpServletRequest();

    private String serverURL = "https://dspace-rest/server";
    private String serverSSRURL = "http://internal-rest:8080/server";

    @Before
    public void setUp() throws Exception {
        when(configurationService.getProperty("dspace.ui.url")).thenReturn("dspaceurl");
        when(configurationService.getProperty("dspace.name")).thenReturn("dspacename");
        when(configurationService.getProperty("dspace.server.url")).thenReturn(serverURL);
        when(configurationService.getProperty("dspace.server.ssr.url", serverURL)).thenReturn(serverSSRURL);

    }

    @Test
    public void testReturnCorrectClass() throws Exception {
        assertEquals(rootConverter.convert(request).getClass(), RootRest.class);
    }

    @Test
    public void testCorrectPropertiesSetFromConfigurationService() throws Exception {
        String restUrl = "/server/api";
        request.setScheme("https");
        request.setServerName("dspace-rest");
        request.setServerPort(443);
        request.setRequestURI(restUrl);
        RootRest rootRest = rootConverter.convert(request);
        assertEquals("dspaceurl", rootRest.getDspaceUI());
        assertEquals("dspacename", rootRest.getDspaceName());
        assertEquals(serverURL, rootRest.getDspaceServer());
        assertEquals(CrisConstants.DSPACE_BASE_VERSION, rootRest.getDspaceVersion());
        assertEquals(Util.getSourceVersion(), rootRest.getCrisVersion());
    }

    @Test
    public void testReturnNotNull() throws Exception {
        assertNotNull(rootConverter.convert(request));
    }

    @Test
    public void testCorrectInternalUrlSetFromConfigurationService() throws Exception {
        String restUrl = "/server/api";
        request.setScheme("http");
        request.setServerName("internal-rest");
        request.setServerPort(8080);
        request.setRequestURI(restUrl);
        RootRest rootRest = rootConverter.convert(request);
        assertEquals("dspaceurl", rootRest.getDspaceUI());
        assertEquals("dspacename", rootRest.getDspaceName());
        assertEquals(serverSSRURL, rootRest.getDspaceServer());
        assertEquals(CrisConstants.DSPACE_BASE_VERSION, rootRest.getDspaceVersion());
        assertEquals(Util.getSourceVersion(), rootRest.getCrisVersion());
    }
}
