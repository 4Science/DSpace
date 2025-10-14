/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.marcxml2item.validator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.marcxml2item.exception.XmlValidationException;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.xml.sax.SAXException;

/**
 * Implementation of {@link XMLValidator} for Simple XML validation.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class SimpleXmlValidator implements XMLValidator {

    private static final Logger log = LogManager.getLogger(SimpleXmlValidator.class);

    private static final String XSD_FILE_NAME = "simple-xml.xsd";

    @Override
    public void validate(byte[] xmlContent, DSpaceRunnableHandler handler) {
        try (InputStream xmlInputStream = new ByteArrayInputStream(xmlContent)) {
            Validator validator = getValidator();
            StreamSource sourceXML = new StreamSource(xmlInputStream);
            validator.validate(sourceXML);
        } catch (SAXException | IOException e) {
            var errorMessage = "Marc XML validation failed with error: " + e.getMessage();
            log.error(errorMessage);
            if (handler != null) {
                handler.logError(errorMessage);
            }
            throw new XmlValidationException(errorMessage);
        }
    }

    private Validator getValidator() throws SAXException {
        try (InputStream inputStream = SimpleXmlValidator.class.getResourceAsStream(XSD_FILE_NAME)) {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new StreamSource(inputStream));
            return schema.newValidator();
        } catch (SAXException | IOException e) {
            throw new XmlValidationException("Error while creating validator", e);
        }
    }

}