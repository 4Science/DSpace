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

import java.io.IOException;
import java.util.Locale;

import org.dspace.AbstractDSpaceTest;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for DSpaceControlledVocabulary.
 *
 * @author mwood
 */
public class DSpaceControlledVocabularyTest extends AbstractDSpaceTest {

    private static final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    public DSpaceControlledVocabularyTest() {
    }

    @BeforeClass
    public static void setUpClass()
        throws Exception {

        configurationService.setProperty("webui.supported.locales", new String[] {Locale.ENGLISH.getLanguage(),
            Locale.ITALIAN.getLanguage(), Locale.GERMAN.getLanguage()});
    }

    @AfterClass
    public static void tearDownClass()
        throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getMatches method, of class DSpaceControlledVocabulary.
     * @throws java.io.IOException passed through.
     * @throws java.lang.ClassNotFoundException passed through.
     */
    @Test
    public void testGetMatchesByLabelNoLocale() throws IOException, ClassNotFoundException {

        final String PLUGIN_INTERFACE = "org.dspace.content.authority.ChoiceAuthority";

        // Ensure that 'id' attribute is optional
        String text = "north 40";
        int start = 0;
        int limit = 10;
        String locale = null;
        // This "farm" Controlled Vocab is included in TestEnvironment data
        // (under /src/test/data/dspaceFolder/) and it should be auto-loaded
        // by test configs in /src/test/data/dspaceFolder/config/local.cfg
        DSpaceControlledVocabulary instance = (DSpaceControlledVocabulary)
            CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(Class.forName(PLUGIN_INTERFACE), "farm");
        assertNotNull(instance);
        Choices result = instance.getMatches(text, start, limit, locale);
        assertEquals("north 40", result.values[0].value);
    }

    /**
     * Test of getMatches method of class
     * DSpaceControlledVocabulary using a localized controlled vocabulary with no locale (fallback to default)
     * @throws java.lang.ClassNotFoundException passed through.
     */
    @Test
    public void testGetMatchesByIdNoLocale() throws ClassNotFoundException {
        configurationService.setProperty("vocabulary.plugin.countries.storeIDs", true);
        final String PLUGIN_INTERFACE = "org.dspace.content.authority.ChoiceAuthority";

        String idValue = "DZA";
        String labelPart = "Alge";
        int start = 0;
        int limit = 10;
        // This "countries" Controlled Vocab is included in TestEnvironment data
        // (under /src/test/data/dspaceFolder/) and it should be auto-loaded
        // by test configs in /src/test/data/dspaceFolder/config/local.cfg
        DSpaceControlledVocabulary instance = (DSpaceControlledVocabulary)
            CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(Class.forName(PLUGIN_INTERFACE),
                "countries");
        assertNotNull(instance);
        Choices result = instance.getMatches(labelPart, start, limit, null);
        assertEquals("Africa::Algeria", result.values[0].value);
        assertEquals("Algeria", result.values[0].label);
        assertEquals("countries:" + idValue, result.values[0].authority);
    }

    /**
     * Test of getBestMatch method of class
     * DSpaceControlledVocabulary using a localized controlled vocabulary with no locale (fallback to default)
     * @throws java.lang.ClassNotFoundException passed through.
     */
    @Test
    public void testGetBestMatchByIdNoLocale() throws ClassNotFoundException {
        configurationService.setProperty("vocabulary.plugin.countries.storeIDs", true);
        final String PLUGIN_INTERFACE = "org.dspace.content.authority.ChoiceAuthority";

        String idValue = "DZA";
        // This "countries" Controlled Vocab is included in TestEnvironment data
        // (under /src/test/data/dspaceFolder/) and it should be auto-loaded
        // by test configs in /src/test/data/dspaceFolder/config/local.cfg
        DSpaceControlledVocabulary instance = (DSpaceControlledVocabulary)
            CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(Class.forName(PLUGIN_INTERFACE),
                "countries");
        assertNotNull(instance);
        Choices result = instance.getBestMatch(idValue, null);
        assertEquals("Africa::Algeria", result.values[0].value);
        assertEquals("Algeria", result.values[0].label);
        assertEquals("countries:" + idValue, result.values[0].authority);
    }

    /**
     * Test of getBestMatch method of class
     * DSpaceControlledVocabulary using a localized controlled vocabulary with no locale (fallback to default)
     *
     * @throws java.lang.ClassNotFoundException passed through.
     */
    @Test
    public void testGetBestMatchByLabelNoLocale() throws ClassNotFoundException {
        configurationService.setProperty("vocabulary.plugin.countries.storeIDs", false);
        final String PLUGIN_INTERFACE = "org.dspace.content.authority.ChoiceAuthority";

        String idValue = "Algeria";
        // This "countries" Controlled Vocab is included in TestEnvironment data
        // (under /src/test/data/dspaceFolder/) and it should be auto-loaded
        // by test configs in /src/test/data/dspaceFolder/config/local.cfg
        DSpaceControlledVocabulary instance = (DSpaceControlledVocabulary)
            CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(Class.forName(PLUGIN_INTERFACE),
                                                                               "countries");
        assertNotNull(instance);
        Choices result = instance.getBestMatch(idValue, null);
        assertEquals("Africa::Algeria", result.values[0].value);
        assertEquals("Algeria", result.values[0].label);
        assertEquals("countries:DZA", result.values[0].authority);
    }

    /**
     * Test of getMatches method of class
     * DSpaceControlledVocabulary using a localized controlled vocabulary with valid locale parameter (localized
     * label returned)
     */
    @Test
    public void testGetMatchesGermanLocale() throws ClassNotFoundException {
        final String PLUGIN_INTERFACE = "org.dspace.content.authority.ChoiceAuthority";

        String idValue = "DZA";
        String labelPart = "Alge";
        int start = 0;
        int limit = 10;
        // This "countries" Controlled Vocab is included in TestEnvironment data
        // (under /src/test/data/dspaceFolder/) and it should be auto-loaded
        // by test configs in /src/test/data/dspaceFolder/config/local.cfg
        DSpaceControlledVocabulary instance = (DSpaceControlledVocabulary)
            CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(Class.forName(PLUGIN_INTERFACE),
                "countries");
        assertNotNull(instance);
        Choices result = instance.getMatches(labelPart, start, limit, "de");
        assertEquals("Afrika::Algerien", result.values[0].value);
        assertEquals("Algerien", result.values[0].label);
        assertEquals("countries:" + idValue, result.values[0].authority);
    }

    /**
     * Test of getBestMatch method of class
     * DSpaceControlledVocabulary using a localized controlled vocabulary with valid locale parameter (localized
     * label returned)
     */
    @Test
    public void testGetBestMatchByIdGermanLocale() throws ClassNotFoundException {
        configurationService.setProperty("vocabulary.plugin.countries.storeIDs", true);
        final String PLUGIN_INTERFACE = "org.dspace.content.authority.ChoiceAuthority";

        String idValue = "DZA";
        // This "countries" Controlled Vocab is included in TestEnvironment data
        // (under /src/test/data/dspaceFolder/) and it should be auto-loaded
        // by test configs in /src/test/data/dspaceFolder/config/local.cfg
        DSpaceControlledVocabulary instance = (DSpaceControlledVocabulary)
            CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(Class.forName(PLUGIN_INTERFACE),
                "countries");
        assertNotNull(instance);
        Choices result = instance.getBestMatch(idValue, "de");
        assertEquals("Afrika::Algerien", result.values[0].value);
        assertEquals("Algerien", result.values[0].label);
        assertEquals("countries:" + idValue, result.values[0].authority);
    }

    /**
     * Test of getBestMatch method of class
     * DSpaceControlledVocabulary using a localized controlled vocabulary with valid locale parameter (localized
     * label returned)
     */
    @Test
    public void testGetBestMatchByLabelGermanLocale() throws ClassNotFoundException {
        configurationService.setProperty("vocabulary.plugin.countries.storeIDs", false);
        final String PLUGIN_INTERFACE = "org.dspace.content.authority.ChoiceAuthority";

        String idValue = "Algerien";
        // This "countries" Controlled Vocab is included in TestEnvironment data
        // (under /src/test/data/dspaceFolder/) and it should be auto-loaded
        // by test configs in /src/test/data/dspaceFolder/config/local.cfg
        DSpaceControlledVocabulary instance = (DSpaceControlledVocabulary)
            CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(Class.forName(PLUGIN_INTERFACE),
                                                                               "countries");
        assertNotNull(instance);
        Choices result = instance.getBestMatch(idValue, "de");
        assertEquals("Afrika::Algerien", result.values[0].value);
        assertEquals("Algerien", result.values[0].label);
        assertEquals("countries:DZA", result.values[0].authority);
    }

    /**
     * Test of getChoice method of class
     * DSpaceControlledVocabulary using a localized controlled vocabulary with no locale (fallback to default)
     * @throws java.lang.ClassNotFoundException passed through.
     */
    @Test
    public void testGetChoiceNoLocale() throws ClassNotFoundException {
        configurationService.setProperty("vocabulary.plugin.countries.storeIDs", true);
        final String PLUGIN_INTERFACE = "org.dspace.content.authority.ChoiceAuthority";

        String idValue = "DZA";
        // This "countries" Controlled Vocab is included in TestEnvironment data
        // (under /src/test/data/dspaceFolder/) and it should be auto-loaded
        // by test configs in /src/test/data/dspaceFolder/config/local.cfg
        DSpaceControlledVocabulary instance = (DSpaceControlledVocabulary)
            CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(Class.forName(PLUGIN_INTERFACE),
                "countries");
        assertNotNull(instance);
        Choice result = instance.getChoice(idValue, null);
        assertEquals("Africa::Algeria", result.value);
        assertEquals("Algeria", result.label);
        assertEquals("countries:" + idValue, result.authority);
    }

    /**
     * Test of getChoice method of class
     * DSpaceControlledVocabulary using a localized controlled vocabulary with valid locale parameter (localized
     * label returned)
     * @throws java.lang.ClassNotFoundException passed through.
     */
    @Test
    public void testGetChoiceGermanLocale() throws ClassNotFoundException {
        configurationService.setProperty("vocabulary.plugin.countries.storeIDs", true);
        final String PLUGIN_INTERFACE = "org.dspace.content.authority.ChoiceAuthority";

        String idValue = "DZA";
        // This "countries" Controlled Vocab is included in TestEnvironment data
        // (under /src/test/data/dspaceFolder/) and it should be auto-loaded
        // by test configs in /src/test/data/dspaceFolder/config/local.cfg
        DSpaceControlledVocabulary instance = (DSpaceControlledVocabulary)
            CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(Class.forName(PLUGIN_INTERFACE),
                "countries");
        assertNotNull(instance);
        Choice result = instance.getChoice(idValue, "de");
        assertEquals("Afrika::Algerien", result.value);
        assertEquals("Algerien", result.label);
        assertEquals("countries:" + idValue, result.authority);
    }
}
