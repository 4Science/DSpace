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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link MetadataRDFMapping} parsing and conversion logic.
 * Tests matchesName, fulfills, convert, parseValueProcessor with various
 * pattern, modifier, and matcher configurations.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class MetadataRDFMappingTest {

    private static final String DSO_IRI = "http://test.org/item/12345";
    private static final String TEST_LANG = "en_US";

    private Model model;

    @Before
    public void setUp() {
        model = ModelFactory.createDefaultModel();
    }

    private Resource dsoIRI(Model m) {
        Resource r = m.createResource(DMRM.NS + "DSpaceObjectIRI");
        r.addProperty(RDF.type, DMRM.ResourceGenerator);
        return r;
    }

    private Resource dspaceValue(Model m) {
        Resource r = m.createResource(DMRM.NS + "DSpaceValue");
        r.addProperty(RDF.type, DMRM.LiteralGenerator);
        return r;
    }

    @Test
    public void matchesName_exactMatch() {
        MetadataRDFMapping mapping = new MetadataRDFMapping("dc.title", null, new ArrayList<>());
        assertTrue(mapping.matchesName("dc.title"));
    }

    @Test
    public void matchesName_caseInsensitive() {
        MetadataRDFMapping mapping = new MetadataRDFMapping("dc.title", null, new ArrayList<>());
        assertTrue(mapping.matchesName("DC.TITLE"));
        assertTrue(mapping.matchesName("Dc.Title"));
    }

    @Test
    public void matchesName_noMatch() {
        MetadataRDFMapping mapping = new MetadataRDFMapping("dc.title", null, new ArrayList<>());
        assertFalse(mapping.matchesName("dc.creator"));
    }

    @Test
    public void fulfills_noCondition() {
        MetadataRDFMapping mapping = new MetadataRDFMapping("dc.title", null, new ArrayList<>());
        assertTrue("Mapping with no condition should fulfill all values", mapping.fulfills("anything"));
    }

    @Test
    public void fulfills_matchingCondition() {
        Pattern condition = Pattern.compile("^http://dx.doi.org/.+");
        MetadataRDFMapping mapping = new MetadataRDFMapping("dc.identifier.uri", condition, new ArrayList<>());
        assertTrue("Should fulfill matching DOI URL",
                       mapping.fulfills("http://dx.doi.org/10.1000/xyz123"));
    }

    @Test
    public void fulfills_nonMatchingCondition() {
        Pattern condition = Pattern.compile("^http://dx.doi.org/.+");
        MetadataRDFMapping mapping = new MetadataRDFMapping("dc.identifier.uri", condition, new ArrayList<>());
        assertFalse("Should not fulfill non-DOI URL",
                        mapping.fulfills("http://example.org/resource"));
    }

    @Test
    public void convert_withSimpleMapping_addsTriple() {
        Model m = ModelFactory.createDefaultModel();
        Resource result = m.createResource()
            .addProperty(DMRM.subject, dsoIRI(m))
            .addProperty(DMRM.predicate, m.createProperty("http://purl.org/dc/elements/1.1/title"))
            .addProperty(DMRM.object, dspaceValue(m));

        List<Resource> results = new ArrayList<>();
        results.add(result);
        MetadataRDFMapping mapping = new MetadataRDFMapping("dc.title", null, results);

        mapping.convert("My Title", "en", DSO_IRI, m);

        assertTrue("Model should contain a triple after conversion",
                       m.contains(m.createResource(DSO_IRI),
                                  m.createProperty("http://purl.org/dc/elements/1.1/title"),
                                  m.createLiteral("My Title")));
    }

    @Test
    public void convert_withLiteralGeneratorAndPattern_usesPattern() {
        Model m = ModelFactory.createDefaultModel();
        Resource literalGen = m.createResource()
            .addProperty(RDF.type, DMRM.LiteralGenerator)
            .addProperty(DMRM.pattern, "info:doi/$DSpaceValue");
        Resource result = m.createResource()
            .addProperty(DMRM.subject, dsoIRI(m))
            .addProperty(DMRM.predicate, m.createProperty("http://purl.org/ontology/bibo/doi"))
            .addProperty(DMRM.object, literalGen);

        List<Resource> results = new ArrayList<>();
        results.add(result);
        MetadataRDFMapping mapping = new MetadataRDFMapping("dc.identifier.uri", null, results);

        mapping.convert("10.1000/xyz123", "en", DSO_IRI, m);

        assertTrue("Model should contain the patterned literal",
                       m.contains(m.createResource(DSO_IRI),
                                  m.createProperty("http://purl.org/ontology/bibo/doi"),
                                  m.createLiteral("info:doi/10.1000/xyz123")));
    }

    @Test
    public void convert_withModifier_appliesReplacement() {
        Model m = ModelFactory.createDefaultModel();
        Resource modifier = m.createResource()
            .addProperty(DMRM.matcher, "^(..)_(.*)$")
            .addProperty(DMRM.replacement, "$1-$2");
        Resource literalGen = m.createResource()
            .addProperty(RDF.type, DMRM.LiteralGenerator)
            .addProperty(DMRM.modifier, modifier)
            .addProperty(DMRM.pattern, "$DSpaceValue");
        Resource result = m.createResource()
            .addProperty(DMRM.subject, dsoIRI(m))
            .addProperty(DMRM.predicate, m.createProperty("http://purl.org/dc/elements/1.1/language"))
            .addProperty(DMRM.object, literalGen);

        List<Resource> results = new ArrayList<>();
        results.add(result);
        MetadataRDFMapping mapping = new MetadataRDFMapping("dc.language.iso", null, results);
        mapping.convert("en_US", "en", DSO_IRI, m);

        assertTrue("Model should contain the modified value en-US",
                       m.contains(m.createResource(DSO_IRI),
                                  m.createProperty("http://purl.org/dc/elements/1.1/language"),
                                  m.createLiteral("en-US")));
    }

    @Test
    public void convert_withDspaceLanguageTag_usesLanguageTag() {
        Model m = ModelFactory.createDefaultModel();
        Resource literalGen = m.createResource()
            .addProperty(RDF.type, DMRM.LiteralGenerator)
            .addProperty(DMRM.dspaceLanguageTag,
                         m.createTypedLiteral("true", XSDDatatype.XSDboolean))
            .addProperty(DMRM.pattern, "$DSpaceValue");
        Resource result = m.createResource()
            .addProperty(DMRM.subject, dsoIRI(m))
            .addProperty(DMRM.predicate, m.createProperty("http://purl.org/dc/terms/abstract"))
            .addProperty(DMRM.object, literalGen);

        List<Resource> results = new ArrayList<>();
        results.add(result);
        MetadataRDFMapping mapping = new MetadataRDFMapping("dc.description.abstract", null, results);

        mapping.convert("An abstract", TEST_LANG, DSO_IRI, m);

        assertTrue("Model should contain the value with language tag en-US",
                       m.contains(m.createResource(DSO_IRI),
                                  m.createProperty("http://purl.org/dc/terms/abstract"),
                                  m.createLiteral("An abstract", "en-US")));
    }

    @Test
    public void convert_withTypedLiteral_createsTypedLiteral() {
        Model m = ModelFactory.createDefaultModel();
        Resource literalGen = m.createResource()
            .addProperty(RDF.type, DMRM.LiteralGenerator)
            .addProperty(DMRM.pattern, "$DSpaceValue")
            .addProperty(DMRM.literalType,
                         m.createResource("http://www.w3.org/2001/XMLSchema#dateTime"));
        Resource result = m.createResource()
            .addProperty(DMRM.subject, dsoIRI(m))
            .addProperty(DMRM.predicate, m.createProperty("http://purl.org/dc/terms/issued"))
            .addProperty(DMRM.object, literalGen);

        List<Resource> results = new ArrayList<>();
        results.add(result);
        MetadataRDFMapping mapping = new MetadataRDFMapping("dc.date.issued", null, results);

        mapping.convert("2020-01-01", "en", DSO_IRI, m);

        Literal literal = m.listStatements(
                m.createResource(DSO_IRI),
                m.createProperty("http://purl.org/dc/terms/issued"),
                (RDFNode) null
            ).nextStatement().getObject().asLiteral();
        assertEquals("Literal should have xsd:dateTime datatype URI",
                         "http://www.w3.org/2001/XMLSchema#dateTime",
                         literal.getDatatype().getURI());
    }

    @Test
    public void convert_withResourceGenerator_createsResource() {
        Model m = ModelFactory.createDefaultModel();
        Resource resourceGen = m.createResource()
            .addProperty(RDF.type, DMRM.ResourceGenerator)
            .addProperty(DMRM.pattern, "$DSpaceValue");
        Resource result = m.createResource()
            .addProperty(DMRM.subject, dsoIRI(m))
            .addProperty(DMRM.predicate, m.createProperty("http://purl.org/dc/terms/relation"))
            .addProperty(DMRM.object, resourceGen);

        List<Resource> results = new ArrayList<>();
        results.add(result);
        MetadataRDFMapping mapping = new MetadataRDFMapping("dc.relation.uri", null, results);

        mapping.convert("http://example.org/related", "en", DSO_IRI, m);

        assertTrue("Model should contain a resource object from ResourceGenerator",
                       m.contains(m.createResource(DSO_IRI),
                                  m.createProperty("http://purl.org/dc/terms/relation"),
                                  m.createResource("http://example.org/related")));
    }

    @Test
    public void parseValueProcessor_withModifier_invalidMatcher_returnsNull() {
        Resource vp = model.createResource()
            .addProperty(DMRM.modifier,
                         model.createResource()
                             .addProperty(DMRM.matcher, "[unclosed")
                             .addProperty(DMRM.replacement, "x"))
            .addProperty(DMRM.pattern, "$DSpaceValue");

        MetadataRDFMapping mapping = new MetadataRDFMapping("test", null, new ArrayList<>());
        String result = mapping.parseValueProcessor(vp, "some value");
        assertNull("Invalid matcher should return null", result);
    }

    @Test
    public void parseValueProcessor_withoutPattern_usesDefaultDSpaceValue() {
        Resource vp = model.createResource();
        MetadataRDFMapping mapping = new MetadataRDFMapping("test", null, new ArrayList<>());
        String result = mapping.parseValueProcessor(vp, "test value");
        assertEquals("Should use default $DSpaceValue pattern", "test value", result);
    }

    @Test
    public void parseValueProcessor_withModifier_appliesReplacement() {
        Resource vp = model.createResource()
            .addProperty(DMRM.modifier,
                         model.createResource()
                             .addProperty(DMRM.matcher, "^(..)_(.*)$")
                             .addProperty(DMRM.replacement, "$1-$2"))
            .addProperty(DMRM.pattern, "$DSpaceValue");

        MetadataRDFMapping mapping = new MetadataRDFMapping("test", null, new ArrayList<>());
        String result = mapping.parseValueProcessor(vp, "en_US");
        assertEquals("Should apply modifier replacement", "en-US", result);
    }

    @Test
    public void fulfills_httpsUrl_doesNotMatchHttpCondition() {
        Pattern condition = Pattern.compile("^http://dx.doi.org/.+");
        MetadataRDFMapping mapping = new MetadataRDFMapping("dc.identifier.uri", condition, new ArrayList<>());
        assertFalse("https://dx.doi.org URL should NOT match http://dx.doi.org condition",
                        mapping.fulfills("https://dx.doi.org/10.1000/xyz123"));
    }
}
