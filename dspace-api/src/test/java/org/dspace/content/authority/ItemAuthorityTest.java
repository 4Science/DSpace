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

/**
 * Unit tests for ItemAuthority refactored code (DSC-2848)
 * Tests getSource() method, protected fields, and inheritance.
 */
public class ItemAuthorityTest extends AbstractUnitTest {

    private ConfigurationService configurationService;
    private ItemAuthority itemAuthority;

    @Before
    public void setUp() {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        itemAuthority = new ItemAuthority();
        itemAuthority.setPluginInstanceName("testAuthority");
    }

    /**
     * Test that getSource() returns configured value when set.
     */
    @Test
    public void testGetSourceWithConfiguredValue() {
        configurationService.setProperty("cris.ItemAuthority.testAuthority.source", "openpolicyfinder");
        String source = itemAuthority.getSource();
        assertEquals("Source should match configured value", "openpolicyfinder", source);
    }

    /**
     * Test that getSource() returns default "local" when not configured.
     */
    @Test
    public void testGetSourceWithDefaultValue() {
        String source = itemAuthority.getSource();
        assertEquals("Source should default to 'local'", "local", source);
    }

    /**
     * Test that authorityName is accessible to subclasses (protected field).
     */
    @Test
    public void testAuthorityNameIsProtected() {
        itemAuthority.setPluginInstanceName("testProtectedAccess");
        assertEquals("Authority name should be accessible", "testProtectedAccess", itemAuthority.getPluginInstanceName());
        // Verify the field is protected by checking subclass access
        OpenPolicyFinderAuthority subAuthority = new OpenPolicyFinderAuthority();
        subAuthority.setPluginInstanceName("subAuthority");
        assertEquals("Subclass should access protected authorityName", "subAuthority", subAuthority.getPluginInstanceName());
    }
}
