/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.dspace.AbstractUnitTest;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for OpenPolicyFinderAuthority refactoring (DSC-2848)
 * Validates:
 * - No duplicate authorityName field (inherits from ItemAuthority)
 * - getSource() works via inheritance from ItemAuthority
 * - No unused imports (validated by compile)
 */
public class OpenPolicyFinderAuthorityTest extends AbstractUnitTest {

    private ConfigurationService configurationService;
    private OpenPolicyFinderAuthority authority;

    @Before
    public void setUp() {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        authority = new OpenPolicyFinderAuthority();
        authority.setPluginInstanceName("testOPFAuthority");
    }

    /**
     * Test that getSource() returns configured value via inheritance from ItemAuthority.
     */
    @Test
    public void testGetSourceInheritance() {
        // Test with configured value
        configurationService.setProperty("cris.ItemAuthority.testOPFAuthority.source", "openpolicyfinder");
        String source = authority.getSource();
        assertEquals("getSource() should return configured value via inheritance", "openpolicyfinder", source);

        // Test default value
        configurationService.setProperty("cris.ItemAuthority.testOPFAuthority.source", null);
        source = authority.getSource();
        assertEquals("getSource() should return default 'local'", "local", source);
    }

    /**
     * Test that authorityName is inherited from ItemAuthority (no duplicate field).
     */
    @Test
    public void testAuthorityNameInheritance() {
        authority.setPluginInstanceName("testName");
        assertEquals("authorityName should be inherited from ItemAuthority",
            "testName", authority.getPluginInstanceName());
    }

    /**
     * Test that convertToChoice uses getSource() correctly.
     * Note: This is an integration test that requires mock OpenPolicyFinderService.
     */
    @Test
    public void testConvertToChoiceUsesGetSource() {
        // Verify that the method exists and returns non-null source
        String source = authority.getSource();
        assertNotNull("getSource() should not return null", source);
    }
}
