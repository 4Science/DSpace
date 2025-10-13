/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.marcxml2item.reader;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.authority.Choices;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Journal fonds reader implementation of {@link ItemsImportMetadataFieldReader}.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 **/
public class ItemsImportJournalFondsReader implements ItemsImportMetadataFieldReader {

    private static final Logger log = LogManager.getLogger(ItemsImportJournalFondsReader.class);

    private static final String READER_NAME = "journalfonds";

    private String issnXPath;
    private String journalfondTitleXPath;

    @Autowired
    private ItemService itemService;

    @Override
    public List<MetadataValueDTO> readValues(Context context, String metadataField, String type, NodeList nodeList) {
        if (StringUtils.isBlank(metadataField) || nodeList == null) {
            return new ArrayList<>();
        }

        List<MetadataValueDTO> metadataValues = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            String issn = getValue(node, issnXPath);
            String title = getValue(node, journalfondTitleXPath);
            if (StringUtils.isBlank(issn) || StringUtils.isBlank(title)) {
                continue;
            }

            Item relatedJournal = findRelatedJournal(context, issn);
            if (relatedJournal != null) {
                var authority = relatedJournal.getID().toString();
                metadataValues.add(new MetadataValueDTO(metadataField, title, authority, Choices.CF_ACCEPTED));
            }
        }
        return metadataValues;
    }

    private Item findRelatedJournal(Context context, String issn) {
        if (StringUtils.isBlank(issn)) {
            return null;
        }
        // Try with original ISSN
        Item journal = findJournalByIssn(context, issn);
        if (journal != null) {
            return journal;
        }
        // Try with ISSN without hyphens
        String cleanIssn = issn.replace("-", "");
        if (!cleanIssn.equals(issn)) {
            return findJournalByIssn(context, cleanIssn);
        }
        return null;
    }

    private Item findJournalByIssn(Context context, String issn) {
        Iterator<Item> items = getIterator(context, issn);
        while (items.hasNext()) {
            Item item = items.next();
            if (isJournalFonds(item)) {
                return item;
            }
        }
        return null;
    }

    private boolean isJournalFonds(Item item) {
        return item.getMetadata()
                   .stream()
                   .anyMatch(m -> "dspace.entity.type".equals(m.getMetadataField().toString('.'))
                                  && "JournalFonds".equalsIgnoreCase(m.getValue()));
    }

    private Iterator<Item> getIterator(Context context, String issn) {
        try {
            return itemService.findByMetadataField(context, "dc", "identifier", "issn", issn);
        } catch (SQLException | AuthorizeException | IOException e) {
            log.error("Error finding items by ISSN: {}", issn, e);
            throw new RuntimeException("Failed to find items by ISSN: " + issn, e);
        }
    }

    private String getValue(Node node, String path) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            Node item = (Node) xPath.compile(path).evaluate(node, XPathConstants.NODE);
            return item != null ? item.getTextContent().trim() : null;
        } catch (XPathExpressionException e) {
            log.error("XPath evaluation error for path: {}", path, e);
            throw new RuntimeException("XPath evaluation failed for path: " + path, e);
        }
    }

    @Override
    public String getReaderName() {
        return READER_NAME;
    }

    public void setIssnXPath(String issnXPath) {
        this.issnXPath = issnXPath;
    }

    public void setJournalfondTitleXPath(String journalfondTitleXPath) {
        this.journalfondTitleXPath = journalfondTitleXPath;
    }

}
