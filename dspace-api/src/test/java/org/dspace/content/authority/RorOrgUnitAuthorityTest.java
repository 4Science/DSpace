/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static org.junit.Assert.assertEquals;

import org.dspace.AbstractUnitTest;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

public class RorOrgUnitAuthorityTest extends AbstractUnitTest {

    private RorOrgUnitAuthority authority;
    private ConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        authority = new RorOrgUnitAuthority();
        authority.setPluginInstanceName("RorOrgUnitAuthority");
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    }

    @Test
    public void testGetSourceDefault() {
        String source = authority.getSource();
        assertEquals("local", source);
    }

    @Test
    public void testGetSourceConfigured() {
        configurationService.setProperty("cris.ItemAuthority.RorOrgUnitAuthority.source", "ror");
        String source = authority.getSource();
        assertEquals("ror", source);
        configurationService.setProperty("cris.ItemAuthority.RorOrgUnitAuthority.source", null);
    }

    @Test
    public void testAuthorityNameInheritance() {
        authority.setPluginInstanceName("testRorAuthority");
        assertEquals("testRorAuthority", authority.getPluginInstanceName());
    }

}
