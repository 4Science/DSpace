/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import static org.dspace.harvest.model.OAIHarvesterValidationResult.buildFromException;
import static org.dspace.harvest.model.OAIHarvesterValidationResult.buildFromExceptions;
import static org.dspace.harvest.model.OAIHarvesterValidationResult.valid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.util.XMLUtils;
import org.dspace.harvest.model.OAIHarvesterValidationResult;
import org.dspace.harvest.service.OAIHarvesterValidator;
import org.dspace.services.ConfigurationService;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Implementation of {@link OAIHarvesterValidator} that validate the given
 * element using an xsd.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OAIHarvesterValidatorImpl implements OAIHarvesterValidator {

    private static final Map<String, Schema> SCHEMA_CACHE = new HashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(OAIHarvesterValidatorImpl.class);

    private ConfigurationService configurationService;

    @Autowired
    public OAIHarvesterValidatorImpl(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public OAIHarvesterValidationResult validate(Element element, HarvestedCollection harvestRow) {
        return getXsdPath(harvestRow)
            .map(xsdPath -> validate(element, xsdPath))
            .orElseGet(() -> valid());
    }

    private OAIHarvesterValidationResult validate(Element element, String xsdPath) {
        try {

            Schema schema = getSchema(xsdPath);
            Validator validator = schema.newValidator();

            CustomErrorHandler errorHandler = new CustomErrorHandler();
            validator.setErrorHandler(errorHandler);

            validator.validate(new JDOMSource(element));

            return buildFromExceptions(errorHandler.getExceptions());

        } catch (SAXException | IOException e) {
            return buildFromException(e);
        }

    }

    private Schema getSchema(String xsdPath) throws SAXException {
        Schema schema = SCHEMA_CACHE.get(xsdPath);
        if (schema == null) {
            File xsdFile = new File(xsdPath);
            // Keep the factory fully locked down (no external DTD/schema access at all). The bundled
            // OpenAIRE CERIF XSDs still need to pull in sibling vocabulary schemas (xs:import/xs:include)
            // and the W3C xml.xsd, so we grant those - and only those - through a resource resolver that
            // is confined to the validation directory of this specific xsd.
            SchemaFactory factory = XMLUtils.getSchemaFactory(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setResourceResolver(new LocalSchemaResolver(xsdFile.getAbsoluteFile().getParentFile()));
            schema = factory.newSchema(xsdFile);
            SCHEMA_CACHE.put(xsdPath, schema);
        }
        return schema;
    }

    private Optional<String> getXsdPath(HarvestedCollection harvestRow) {

        String validationDirectory = configurationService.getProperty("oai.harvester.validation-dir");
        if (StringUtils.isBlank(validationDirectory)) {
            LOGGER.warn("Harvest validation enabled but no oai.harvester.validation-dir property configured");
            return Optional.empty();
        }

        String metadataConfig = harvestRow.getHarvestMetadataConfig();
        String xsdNameProperty = "oai.harvester.validation." + metadataConfig + ".xsd";

        String xsdName = configurationService.getProperty(xsdNameProperty);
        if (StringUtils.isBlank(xsdName)) {
            LOGGER.warn("Harvest validation enabled but no " + xsdNameProperty + " property configured");
            return Optional.empty();
        }

        File xsdFile = new File(validationDirectory, xsdName);
        if (!xsdFile.exists()) {
            LOGGER.warn("Harvest validation enabled but no xsd found on path: " + xsdFile.getPath());
            return Optional.empty();
        }

        return Optional.of(xsdFile.getAbsolutePath());
    }

    private static class CustomErrorHandler implements ErrorHandler {

        private final List<Exception> exceptions;

        public CustomErrorHandler() {
            this.exceptions = new ArrayList<>();
        }

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            exceptions.add(exception);
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            exceptions.add(exception);
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            exceptions.add(exception);
        }

        public List<Exception> getExceptions() {
            return exceptions;
        }

    }

    /**
     * {@link LSResourceResolver} used while building the validation {@link Schema}. The
     * {@link SchemaFactory} itself is kept fully locked down (no external DTD/schema access), so this
     * resolver is the only channel through which imported/included schemas can be loaded. It grants
     * exactly two things and denies everything else:
     * <ul>
     *   <li>the W3C XML namespace schema ({@code xml.xsd}), served from a bundled classpath copy so no
     *       network access to w3.org is needed;</li>
     *   <li>sibling schemas referenced via {@code xs:import}/{@code xs:include}, but only if they
     *       resolve to a file located inside the allowed validation directory (or a subdirectory).</li>
     * </ul>
     * Any request that escapes the allowed directory, or targets any other protocol, resolves to
     * {@code null} and is therefore rejected by the locked-down factory.
     */
    private static class LocalSchemaResolver implements LSResourceResolver {

        private static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace";

        private static final String BUNDLED_XML_XSD = "org/dspace/app/util/xml.xsd";

        private final Path allowedBaseDir;

        private final DOMImplementationLS domImplementation;

        LocalSchemaResolver(File allowedBaseDir) {
            this.allowedBaseDir = allowedBaseDir.toPath().toAbsolutePath().normalize();
            try {
                this.domImplementation = (DOMImplementationLS) DOMImplementationRegistry.newInstance()
                    .getDOMImplementation("LS");
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException("Unable to initialize DOM LS implementation", e);
            }
        }

        @Override
        public LSInput resolveResource(String type, String namespaceURI, String publicId,
                                       String systemId, String baseURI) {

            if (XML_NAMESPACE.equals(namespaceURI) || (systemId != null && systemId.endsWith("/xml.xsd"))) {
                return resolveBundledXmlSchema(publicId, systemId, baseURI);
            }

            return resolveLocalSibling(publicId, systemId, baseURI);
        }

        private LSInput resolveBundledXmlSchema(String publicId, String systemId, String baseURI) {
            InputStream stream = OAIHarvesterValidatorImpl.class.getClassLoader()
                .getResourceAsStream(BUNDLED_XML_XSD);
            if (stream == null) {
                return null;
            }
            LSInput input = domImplementation.createLSInput();
            input.setByteStream(stream);
            input.setSystemId(systemId);
            input.setPublicId(publicId);
            input.setBaseURI(baseURI);
            return input;
        }

        private LSInput resolveLocalSibling(String publicId, String systemId, String baseURI) {
            if (StringUtils.isBlank(systemId)) {
                return null;
            }

            Path resolved;
            try {
                URI systemUri = URI.create(systemId);
                if (systemUri.isAbsolute() && !"file".equalsIgnoreCase(systemUri.getScheme())) {
                    // Reject any non-file protocol (e.g. http/https).
                    return null;
                }
                if (systemUri.getScheme() != null) {
                    resolved = Paths.get(systemUri);
                } else if (baseURI != null) {
                    resolved = Paths.get(URI.create(baseURI).resolve(systemId));
                } else {
                    resolved = allowedBaseDir.resolve(systemId);
                }
            } catch (IllegalArgumentException e) {
                return null;
            }

            resolved = resolved.toAbsolutePath().normalize();
            if (!resolved.startsWith(allowedBaseDir)) {
                // Path traversal outside the allowed validation directory: deny.
                return null;
            }

            File file = resolved.toFile();
            if (!file.exists() || !file.canRead()) {
                return null;
            }

            try {
                LSInput input = domImplementation.createLSInput();
                input.setByteStream(new FileInputStream(file));
                input.setSystemId(resolved.toUri().toString());
                input.setPublicId(publicId);
                input.setBaseURI(baseURI);
                return input;
            } catch (FileNotFoundException e) {
                return null;
            }
        }
    }

}
