/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rdf.conversion;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.dspace.AbstractDSpaceTest;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for SimpleDSORelationsConverterPlugin TTL configuration files.
 * Validates that simple-relations-prefixes.ttl and metadata-prefixes.ttl
 * parse as valid RDF.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class SimpleDSORelationsConverterPluginTest extends AbstractDSpaceTest {

    private static final String RDF_CONFIG_DIR = "/config/modules/rdf";
    private static String rdfDir;

    @BeforeClass
    public static void setUpClass() throws Exception {
        String dspaceDir = kernelImpl.getConfigurationService().getProperty("dspace.dir");
        assertNotNull("dspace.dir must be configured", dspaceDir);
        rdfDir = dspaceDir + RDF_CONFIG_DIR;
    }

    @Test
    public void simpleRelationsPrefixesFileParsesAsValidRDF() throws Exception {
        String filePath = rdfDir + "/simple-relations-prefixes.ttl";
        try (FileInputStream is = new FileInputStream(new File(filePath))) {
            Model model = ModelFactory.createDefaultModel();
            model.read(is, null, "TURTLE");
            assertNotNull("Parsed model must not be null", model);
        }
    }

    @Test
    public void metadataPrefixesFileParsesAsValidRDF() throws Exception {
        String filePath = rdfDir + "/metadata-prefixes.ttl";
        try (FileInputStream is = new FileInputStream(new File(filePath))) {
            Model model = ModelFactory.createDefaultModel();
            model.read(is, null, "TURTLE");
            assertNotNull("Parsed model must not be null", model);
        }
    }
}
