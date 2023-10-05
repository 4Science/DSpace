/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.packager;

import static java.util.Objects.nonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.dspace.content.crosswalk.MetadataValidationException;
import org.eclipse.jetty.util.StringUtil;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * Class to manage the MAG manifest document for MAG importer classes.
 */
public class MAGManifest {

    public static final String MANIFEST_FILE = "mag.xml";
    private static final List<Character> PATH_CHARS_TO_TRUNCATE = List.of('.', '/');

    public static final Namespace magNS = Namespace.getNamespace("mag", "http://www.iccu.sbn.it/metaAG1.pdf");
    public static final Namespace xlinkNS = Namespace.getNamespace("xlink", "http://www.w3.org/TR/xlink");
    public static final Namespace dcNS = Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/");
    public static final Namespace nisoNS = Namespace.getNamespace("niso", "http://www.niso.org/pdfs/DataDict.pdf");

    protected Element mag = null;
    protected SAXBuilder parser = null;

    protected MAGManifest(SAXBuilder builder, Element mag) {
        this.mag = mag;
        this.parser = builder;
    }

    public static MAGManifest create(InputStream is, boolean validate)
            throws IOException, MetadataValidationException {
        SAXBuilder builder = new SAXBuilder(validate);
        builder.setIgnoringElementContentWhitespace(true);

        if (validate) {
            builder.setFeature("http://apache.org/xml/features/validation/schema", true);
        } else {
            builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        }

        Document magDocument;

        try {
            magDocument = builder.build(is);
        } catch (JDOMException je) {
            throw new MetadataValidationException("Error validating MAG in " + is.toString(), je);
        }

        return new MAGManifest(builder, magDocument.getRootElement());
    }

    public List<Element> getFiles() {
        List<Element> elements = getElementsByXPath("/mag:metadigit/img", true);
        if (nonNull(elements)) {
            return elements;
        }
        return new ArrayList<>();
    }

    public String getOriginalFileName(Element file) {
        Element element = getElementByXPath("file", false, file);
        String filePath = element.getAttributeValue("href", xlinkNS);
        if (StringUtil.isBlank(filePath)) {
            throw new RuntimeException(
                    "Invalid MAG Manifest: /file does not have xlink:href=\"URL\" attribute.");
        }
        return fixFilePath(filePath);
    }

    public String getThumbnailFileName(Element file) {
        Element element = getElementByXPath("altimg/file", false, file);
        String filePath = element.getAttributeValue("href", xlinkNS);
        if (StringUtil.isBlank(filePath)) {
            throw new RuntimeException(
                    "Invalid MAG Manifest: /altimg/file does not have" +
                            " xlink:href=\"URL\" attribute.");
        }
        return fixFilePath(filePath);
    }

    private static String fixFilePath(String path) {
        String fixedPath = path.replace('\\', '/');
        int truncateIndex = -1;
        for (int i = 0; i < fixedPath.length(); i++) {
            if (PATH_CHARS_TO_TRUNCATE.contains(fixedPath.charAt(i))) {
                truncateIndex = i;
            } else {
                break;
            }
        }
        return fixedPath.substring(truncateIndex + 1);
    }

    public Integer getOriginalSequenceId(Element file) {
        Element seqID = getElementByXPath("sequence_number", true, file);
        return nonNull(seqID) && nonNull(seqID.getValue()) ? Integer.parseInt(seqID.getValue()) : null;
    }

    protected Element getElementByXPath(String path, boolean nullOk, Element sourse) {
        XPathExpression<Element> xpath = XPathFactory.instance()
                .compile(path, Filters.element(), null, magNS, xlinkNS, dcNS, nisoNS);
        Element result = xpath.evaluateFirst(sourse);
        if (result == null && nullOk) {
            return null;
        } else if (result == null && !nullOk) {
            throw new RuntimeException("MAGManifest: Failed to resolve XPath, path=\"" + path + "\"");
        } else {
            return result;
        }
    }

    protected Element getElementByXPath(String path, boolean nullOk) {
        return getElementByXPath(path, nullOk, mag);
    }

    protected List<Element> getElementsByXPath(String path, boolean nullOk) {
        XPathExpression<Element> xpath = XPathFactory.instance()
                .compile(path, Filters.element(), null, magNS, xlinkNS, dcNS, nisoNS);
        List<Element> result = xpath.evaluate(mag);
        if (result == null && nullOk) {
            return null;
        } else if (result == null && !nullOk) {
            throw new RuntimeException("MAGManifest: Failed to resolve XPath, path=\"" + path + "\"");
        } else {
            return result;
        }
    }

    public InputStream getMagsAsStream() {
        XMLOutputter outputPretty = new XMLOutputter(Format.getPrettyFormat());
        return new ByteArrayInputStream(
                outputPretty.outputString(mag).getBytes(StandardCharsets.UTF_8));
    }
}
