/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rdf.conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.util.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.AbstractDSpaceTest;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the metadata-rdf-mapping.ttl configuration file.
 * Validates that all condition and matcher regexes compile, schema conformance,
 * and specific URL pattern handling (DOI, Handle, local, catch-all).
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class MetadataRDFMappingConfigurationTest extends AbstractDSpaceTest {

    private static final Logger log = LogManager.getLogger(MetadataRDFMappingConfigurationTest.class);

    private static final String RDF_CONFIG_DIR = "/config/modules/rdf";
    private static final String BAD_REGEX_TTL = "/org/dspace/rdf/conversion/metadata-rdf-mapping-bad-regex.ttl";

    private static Model validMapping;
    private static Model schema;
    private static Model badRegexMapping;

    @BeforeClass
    public static void setUpClass() throws Exception {
        String dspaceDir = kernelImpl.getConfigurationService().getProperty("dspace.dir");
        assertNotNull("dspace.dir must be configured", dspaceDir);

        String rdfDir = dspaceDir + RDF_CONFIG_DIR;
        String mappingPath = rdfDir + "/metadata-rdf-mapping.ttl";
        String schemaPath = rdfDir + "/metadata-rdf-schema.ttl";

        validMapping = loadModelFromFile(mappingPath);
        assertNotNull("Valid mapping TTL must load from: " + mappingPath, validMapping);
        assertFalse("Valid mapping TTL must not be empty", validMapping.isEmpty());

        schema = loadModelFromFile(schemaPath);
        assertNotNull("Schema TTL must load from: " + schemaPath, schema);

        badRegexMapping = loadModelFromClasspath(BAD_REGEX_TTL);
        assertNotNull("Bad regex TTL must load", badRegexMapping);
    }

    private static Model loadModelFromFile(String filePath) throws Exception {
        try (InputStream is = new FileInputStream(new File(filePath))) {
            String lang = filePath.endsWith(".ttl") ? "TURTLE" : FileUtils.guessLang(filePath);
            Model model = ModelFactory.createDefaultModel();
            model.read(is, null, lang);
            return model;
        } catch (Exception e) {
            log.error("Cannot load file: " + filePath, e);
            return null;
        }
    }

    private static Model loadModelFromClasspath(String resourcePath) {
        InputStream is = MetadataRDFMappingConfigurationTest.class.getResourceAsStream(resourcePath);
        if (is == null) {
            log.error("Cannot find resource: " + resourcePath);
            return null;
        }
        Model model = ModelFactory.createDefaultModel();
        model.read(is, null, FileUtils.guessLang(resourcePath));
        return model;
    }

    @Test
    public void allConditionRegexesCompile() {
        List<String> errors = new ArrayList<>();
        ResIterator iter = validMapping.listSubjectsWithProperty(DMRM.metadataName);
        assertTrue("Must find at least one DSpaceMetadataRDFMapping", iter.hasNext());
        while (iter.hasNext()) {
            Resource mapping = iter.nextResource();
            String uri = mapping.getURI() != null ? mapping.getURI() : "blank";
            Resource nameRes = mapping.getPropertyResourceValue(DMRM.metadataName);
            String name = nameRes != null ? nameRes.asLiteral().getLexicalForm() : "unknown";
            String condition = null;
            if (mapping.hasProperty(DMRM.condition)) {
                condition = mapping.getProperty(DMRM.condition).getObject().asLiteral().getLexicalForm();
            }
            if (condition != null && !condition.isEmpty()) {
                try {
                    Pattern.compile(condition);
                } catch (PatternSyntaxException e) {
                    errors.add("Mapping '" + name + "' (" + uri + ") has invalid condition regex: '"
                                   + condition + "': " + e.getMessage());
                }
            }
        }
        if (!errors.isEmpty()) {
            fail("Found " + errors.size() + " invalid condition regexes:\n  " + String.join("\n  ", errors));
        }
    }

    @Test
    public void allMatcherRegexesCompile() {
        List<String> errors = new ArrayList<>();
        ResIterator iter = validMapping.listSubjectsWithProperty(DMRM.metadataName);
        while (iter.hasNext()) {
            Resource mapping = iter.nextResource();
            String uri = mapping.getURI() != null ? mapping.getURI() : "blank";
            Resource nameRes = mapping.getPropertyResourceValue(DMRM.metadataName);
            String name = nameRes != null ? nameRes.asLiteral().getLexicalForm() : "unknown";
            StmtIterator createsStmtIter = mapping.listProperties(DMRM.creates);
            while (createsStmtIter.hasNext()) {
                Resource result = createsStmtIter.nextStatement().getObject().asResource();
                if (!result.hasProperty(DMRM.object)) {
                    continue;
                }
                RDFNode objectNode = result.getProperty(DMRM.object).getObject();
                if (!objectNode.isResource()) {
                    continue;
                }
                Resource objectRes = objectNode.asResource();
                if (objectRes.hasProperty(DMRM.modifier)) {
                    Resource modifier = objectRes.getProperty(DMRM.modifier).getObject().asResource();
                    if (modifier.hasProperty(DMRM.matcher)) {
                        String matcher = modifier.getProperty(DMRM.matcher).getObject().asLiteral().getLexicalForm();
                        try {
                            Pattern.compile(matcher);
                        } catch (PatternSyntaxException e) {
                            errors.add("Mapping '" + name + "' (" + uri + ") has invalid matcher regex: '"
                                           + matcher + "': " + e.getMessage());
                        }
                    }
                }
            }
        }
        if (!errors.isEmpty()) {
            fail("Found " + errors.size() + " invalid matcher regexes:\n  " + String.join("\n  ", errors));
        }
    }

    @Test
    public void validatesSuccessfullyAgainstSchema() {
        Reasoner reasoner = ReasonerRegistry.getRDFSSimpleReasoner().bindSchema(schema);
        InfModel inf = ModelFactory.createInfModel(reasoner, validMapping);
        ValidityReport reports = inf.validate();
        assertTrue("Mapping TTL should be valid against schema", reports.isValid());
    }

    @Test
    public void badRegexMapping_invalidConditionIsRejected() {
        Resource badMapping = locateMappingByName(badRegexMapping, "dc.test.badcondition");
        assertNotNull("Must find badcondition mapping", badMapping);
        MetadataRDFMapping result = MetadataRDFMapping.getMetadataRDFMapping(badMapping, "http://test.org/item/1");
        assertNull("Mapping with unclosed character class condition should be rejected (return null)", result);
    }

    @Test
    public void badRegexMapping_goodConditionSucceeds() {
        Resource goodMapping = locateMappingByName(badRegexMapping, "dc.test.good");
        assertNotNull("Must find good mapping", goodMapping);
        MetadataRDFMapping result = MetadataRDFMapping.getMetadataRDFMapping(goodMapping, "http://test.org/item/1");
        assertNotNull("Mapping with valid regex should succeed", result);
    }

    @Test
    public void emptyConditionIsValid() {
        Resource emptyMapping = locateMappingByName(badRegexMapping, "dc.test.empty");
        assertNotNull("Must find empty regex mapping", emptyMapping);
        MetadataRDFMapping result = MetadataRDFMapping.getMetadataRDFMapping(emptyMapping, "http://test.org/item/1");
        assertNotNull("Mapping with empty condition string should succeed", result);
    }

    @Test
    public void doiCondition_matchesDoiUrl() {
        String mappingName = "dc.identifier.uri";
        String doiCondition = getConditionForMapping(validMapping, mappingName, "^https?://dx.doi.org/");
        assertNotNull("doi condition should exist for dc.identifier.uri", doiCondition);
        Pattern p = Pattern.compile(doiCondition);
        assertTrue("doi condition should match http://dx.doi.org/10.1000/xyz123",
                       p.matcher("http://dx.doi.org/10.1000/xyz123").matches());
        assertTrue("doi condition should match https://dx.doi.org/10.1000/xyz123",
                       p.matcher("https://dx.doi.org/10.1000/xyz123").matches());
        assertFalse("doi condition should NOT match http://hdl.handle.net/12345/abc",
                        p.matcher("http://hdl.handle.net/12345/abc").matches());
        assertFalse("doi condition should NOT match http://localhost:8080/handle/12345/abc",
                        p.matcher("http://localhost:8080/handle/12345/abc").matches());
    }

    @Test
    public void handleCondition_matchesHandleUrl() {
        String handleCondition = getConditionForMapping(validMapping, "dc.identifier.uri", "^https?://hdl.handle.net/");
        assertNotNull("handle condition should exist", handleCondition);
        Pattern p = Pattern.compile(handleCondition);
        assertTrue("handle condition should match http://hdl.handle.net/12345/abc",
                       p.matcher("http://hdl.handle.net/12345/abc").matches());
        assertTrue("handle condition should match https://hdl.handle.net/12345/abc",
                       p.matcher("https://hdl.handle.net/12345/abc").matches());
        assertFalse("handle condition should NOT match http://dx.doi.org/10.1000/xyz",
                        p.matcher("http://dx.doi.org/10.1000/xyz").matches());
    }

    @Test
    public void localHandleCondition_matchesLocalUrl() {
        String localCondition = getConditionForMapping(validMapping, "dc.identifier.uri",
                                                            "^https?://localhost:8080/handle/");
        assertNotNull("local handle condition should exist", localCondition);
        Pattern p = Pattern.compile(localCondition);
        assertTrue("local condition should match http://localhost:8080/handle/12345",
                       p.matcher("http://localhost:8080/handle/12345").matches());
        assertTrue("local condition should match https://localhost:8080/handle/12345",
                       p.matcher("https://localhost:8080/handle/12345").matches());
        assertFalse("local condition should NOT match http://dx.doi.org/10.1000/xyz",
                        p.matcher("http://dx.doi.org/10.1000/xyz").matches());
    }

    @Test
    public void uriCatchAll_matchesEverythingElse() {
        String uriCondition = getConditionForMapping(validMapping, "dc.identifier.uri",
                                                         "^(?!https?://dx.doi.org/)");
        assertNotNull("uri catch-all condition should exist", uriCondition);
        Pattern p = Pattern.compile(uriCondition);
        assertTrue("catch-all should match https://example.org/resource/1",
                       p.matcher("https://example.org/resource/1").matches());
        assertTrue("catch-all should match http://my.server.edu/handle/abc",
                       p.matcher("http://my.server.edu/handle/abc").matches());
        assertFalse("catch-all should NOT match http://dx.doi.org/10.1000/xyz",
                        p.matcher("http://dx.doi.org/10.1000/xyz").matches());
        assertFalse("catch-all should NOT match https://dx.doi.org/10.1000/xyz",
                        p.matcher("https://dx.doi.org/10.1000/xyz").matches());
        assertFalse("catch-all should NOT match http://hdl.handle.net/12345/abc",
                        p.matcher("http://hdl.handle.net/12345/abc").matches());
        assertFalse("catch-all should NOT match https://hdl.handle.net/12345/abc",
                        p.matcher("https://hdl.handle.net/12345/abc").matches());
        assertFalse("catch-all should NOT match http://localhost:8080/handle/12345",
                        p.matcher("http://localhost:8080/handle/12345").matches());
        assertFalse("catch-all should NOT match https://localhost:8080/handle/12345",
                        p.matcher("https://localhost:8080/handle/12345").matches());
    }

    @Test
    public void doiMatcher_extractsDoi() {
        String matcher = locateMatcherInMapping(validMapping, "dc.identifier.uri",
                                                     "^https?://dx.doi.org/");
        assertNotNull("doi matcher should exist", matcher);
        Pattern p = Pattern.compile(matcher);
        assertTrue("doi matcher should match http://dx.doi.org/10.1000/xyz",
                       p.matcher("http://dx.doi.org/10.1000/xyz").matches());
        assertTrue("doi matcher should match https://dx.doi.org/10.1000/xyz",
                       p.matcher("https://dx.doi.org/10.1000/xyz").matches());
    }

    @Test
    public void httpOnlyCondition_alsoAcceptsHttps() {
        String doiCond = getConditionForMapping(validMapping, "dc.identifier.uri", "^https?://dx.doi.org/");
        assertNotNull("doi condition should exist", doiCond);
        Pattern doiPattern = Pattern.compile(doiCond);
        String handleCond = getConditionForMapping(validMapping, "dc.identifier.uri", "^https?://hdl.handle.net/");
        assertNotNull("handle condition should exist", handleCond);
        Pattern handlePattern = Pattern.compile(handleCond);
        String localCond = getConditionForMapping(validMapping, "dc.identifier.uri",
            "^https?://localhost:8080/handle/");
        assertNotNull("local handle condition should exist", localCond);
        Pattern localPattern = Pattern.compile(localCond);
        assertTrue("doi condition should match http URL",
                       doiPattern.matcher("http://dx.doi.org/10.1000/xyz").matches());
        assertTrue("doi condition should match https URL",
                       doiPattern.matcher("https://dx.doi.org/10.1000/xyz").matches());
        assertTrue("handle condition should match http URL",
                       handlePattern.matcher("http://hdl.handle.net/12345/abc").matches());
        assertTrue("handle condition should match https URL",
                       handlePattern.matcher("https://hdl.handle.net/12345/abc").matches());
        assertTrue("local condition should match http://localhost:8080/handle/12345",
                       localPattern.matcher("http://localhost:8080/handle/12345").matches());
        assertTrue("local condition should match https://localhost:8080/handle/12345",
                       localPattern.matcher("https://localhost:8080/handle/12345").matches());
    }

    @Test
    public void languageIsoRegex_isValidAndTransformsCorrectly() {
        String matcher = locateMatcherInMapping(validMapping, "dc.language.iso", null);
        assertNotNull("dc.language.iso should have a matcher", matcher);
        Pattern p = Pattern.compile(matcher);
        assertTrue("iso matcher should match en_US", p.matcher("en_US").matches());
        assertTrue("iso matcher should match it_IT", p.matcher("it_IT").matches());
        String replacement = locateReplacementInMapping(validMapping, "dc.language.iso");
        assertNotNull("dc.language.iso should have a replacement", replacement);
        assertEquals("replacement should transform en_US to en-US",
                         "en-US", p.matcher("en_US").replaceAll(replacement));
    }

    private Resource locateMappingByName(Model model, String metadataName) {
        ResIterator iter = model.listSubjectsWithProperty(DMRM.metadataName);
        while (iter.hasNext()) {
            Resource mapping = iter.nextResource();
            if (mapping.hasProperty(DMRM.metadataName)) {
                String name = mapping.getProperty(DMRM.metadataName).getObject().asLiteral().getLexicalForm();
                if (metadataName.equals(name)) {
                    return mapping;
                }
            }
        }
        return null;
    }

    private String getConditionForMapping(Model model, String metadataName, String expectedPrefix) {
        ResIterator iter = model.listSubjectsWithProperty(DMRM.metadataName);
        while (iter.hasNext()) {
            Resource mapping = iter.nextResource();
            if (!mapping.hasProperty(DMRM.metadataName)) {
                continue;
            }
            String name = mapping.getProperty(DMRM.metadataName).getObject().asLiteral().getLexicalForm();
            if (!metadataName.equals(name)) {
                continue;
            }
            if (!mapping.hasProperty(DMRM.condition)) {
                continue;
            }
            String condition = mapping.getProperty(DMRM.condition).getObject().asLiteral().getLexicalForm();
            if (expectedPrefix != null && !condition.startsWith(expectedPrefix)) {
                continue;
            }
            return condition;
        }
        return null;
    }

    private String locateMatcherInMapping(Model model, String metadataName, String conditionPrefix) {
        ResIterator iter = model.listSubjectsWithProperty(DMRM.metadataName);
        while (iter.hasNext()) {
            Resource mapping = iter.nextResource();
            if (!mapping.hasProperty(DMRM.metadataName)) {
                continue;
            }
            String name = mapping.getProperty(DMRM.metadataName).getObject().asLiteral().getLexicalForm();
            if (!metadataName.equals(name)) {
                continue;
            }
            if (conditionPrefix != null && mapping.hasProperty(DMRM.condition)) {
                String condition = mapping.getProperty(DMRM.condition).getObject().asLiteral().getLexicalForm();
                if (!condition.startsWith(conditionPrefix)) {
                    continue;
                }
            }
            StmtIterator createsStmtIter = mapping.listProperties(DMRM.creates);
            while (createsStmtIter.hasNext()) {
                Resource result = createsStmtIter.nextStatement().getObject().asResource();
                if (!result.hasProperty(DMRM.object)) {
                    continue;
                }
                RDFNode objectNode = result.getProperty(DMRM.object).getObject();
                if (!objectNode.isResource()) {
                    continue;
                }
                Resource objectRes = objectNode.asResource();
                if (objectRes.hasProperty(DMRM.modifier)) {
                    Resource modifier = objectRes.getProperty(DMRM.modifier).getObject().asResource();
                    if (modifier.hasProperty(DMRM.matcher)) {
                        return modifier.getProperty(DMRM.matcher).getObject().asLiteral().getLexicalForm();
                    }
                }
            }
        }
        return null;
    }

    private String locateReplacementInMapping(Model model, String metadataName) {
        ResIterator iter = model.listSubjectsWithProperty(DMRM.metadataName);
        while (iter.hasNext()) {
            Resource mapping = iter.nextResource();
            if (!mapping.hasProperty(DMRM.metadataName)) {
                continue;
            }
            String name = mapping.getProperty(DMRM.metadataName).getObject().asLiteral().getLexicalForm();
            if (!metadataName.equals(name)) {
                continue;
            }
            StmtIterator createsStmtIter = mapping.listProperties(DMRM.creates);
            while (createsStmtIter.hasNext()) {
                Resource result = createsStmtIter.nextStatement().getObject().asResource();
                if (!result.hasProperty(DMRM.object)) {
                    continue;
                }
                RDFNode objectNode = result.getProperty(DMRM.object).getObject();
                if (!objectNode.isResource()) {
                    continue;
                }
                Resource objectRes = objectNode.asResource();
                if (objectRes.hasProperty(DMRM.modifier)) {
                    Resource modifier = objectRes.getProperty(DMRM.modifier).getObject().asResource();
                    if (modifier.hasProperty(DMRM.replacement)) {
                        return modifier.getProperty(DMRM.replacement).getObject().asLiteral().getLexicalForm();
                    }
                }
            }
        }
        return null;
    }
}
