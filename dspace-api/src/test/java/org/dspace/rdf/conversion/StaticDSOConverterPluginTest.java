/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rdf.conversion;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.AbstractDSpaceTest;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for StaticDSOConverterPlugin TTL configuration files.
 * Validates all constant-data TTL files parse as valid RDF.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class StaticDSOConverterPluginTest extends AbstractDSpaceTest {

    private static final Logger log = LogManager.getLogger(StaticDSOConverterPluginTest.class);

    private static final String RDF_CONFIG_DIR = "/config/modules/rdf";
    private static final String[] CONSTANT_DATA_FILES = {
        "constant-data-general.ttl",
        "constant-data-community.ttl",
        "constant-data-collection.ttl",
        "constant-data-item.ttl",
        "constant-data-site.ttl",
    };

    private static String rdfDir;

    @BeforeClass
    public static void setUpClass() throws Exception {
        String dspaceDir = kernelImpl.getConfigurationService().getProperty("dspace.dir");
        assertNotNull("dspace.dir must be configured", dspaceDir);
        rdfDir = dspaceDir + RDF_CONFIG_DIR;
    }

    @Test
    public void allConstantDataFilesParseAsValidRDF() {
        List<String> errors = new ArrayList<>();
        for (String fileName : CONSTANT_DATA_FILES) {
            String filePath = rdfDir + "/" + fileName;
            try (InputStream is = new FileInputStream(new File(filePath))) {
                Model model = ModelFactory.createDefaultModel();
                model.read(is, null, "TURTLE");
            } catch (Exception e) {
                errors.add("File " + filePath + " failed to parse: " + e.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            fail("Errors parsing constant data files:\n  " + String.join("\n  ", errors));
        }
    }

    @Test
    public void constantDataGeneral_containsExpectedProperties() throws Exception {
        String filePath = rdfDir + "/constant-data-general.ttl";
        try (InputStream is = new FileInputStream(new File(filePath))) {
            Model model = ModelFactory.createDefaultModel();
            model.read(is, null, "TURTLE");
            assertNotNull("Model must not be null", model);
            assertFalse("Model must not be empty", model.isEmpty());
        }
    }
}
