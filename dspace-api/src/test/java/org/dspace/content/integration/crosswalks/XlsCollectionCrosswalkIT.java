/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import static java.util.Arrays.asList;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.dspace.util.WorkbookUtils.getRowValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.util.DCInputsReader;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.core.Constants;
import org.dspace.core.CrisConstants;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the {@link XlsCollectionCrosswalk}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class XlsCollectionCrosswalkIT extends AbstractIntegrationTestWithDatabase {

    private XlsCollectionCrosswalk xlsCollectionCrosswalk;

    private BitstreamService bitstreamService;

    private BundleService bundleService;

    private ConfigurationService configurationService;

    private Community community;

    private static final String BITSTREAM_URL_FORMAT = "%s/api/core/bitstreams/%s/content";

    @Before
    public void setup() throws SQLException, AuthorizeException {

        StreamDisseminationCrosswalkMapper crosswalkMapper = new DSpace()
            .getSingletonService(StreamDisseminationCrosswalkMapper.class);
        assertThat(crosswalkMapper, notNullValue());

        xlsCollectionCrosswalk = (XlsCollectionCrosswalk) crosswalkMapper.getByType("collection-xls");

        bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
        bundleService = ContentServiceFactory.getInstance().getBundleService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        context.restoreAuthSystemState();

    }

    @Test
    public void testCollectionDisseminate() throws Exception {
        // skip test based on configuration
        Assume.assumeFalse(configurationService.getBooleanProperty("test.skip.cris", false));

        context.turnOffAuthorisationSystem();
        Collection collection = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withHandle("123456789/001")
            .withTitle("Test Publication")
            .withAlternativeTitle("Alternative publication title")
            .withRelationPublication("Published in publication")
            .withRelationDoi("doi:10.3972/test")
            .withRelationIsbn("ISBN-01")
            .withDoiIdentifier("doi:111.111/publication")
            .withIsbnIdentifier("978-3-16-148410-0")
            .withIssnIdentifier("2049-3630")
            .withIsiIdentifier("111-222-333")
            .withScopusIdentifier("99999999")
            .withLanguage("en")
            .withPublisher("Publication publisher")
            .withSubject("test")
            .withSubject("export")
            .withType("Controlled Vocabulary for Resource Type Genres::text::review")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthor("Walter White")
            .withAuthorAffiliation("Company")
            .withRelationProject("Test Project", "d9471fee-34fa-4a39-9658-443c4bb47b22")
            .withRelationGrantno(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withRelationFunding("Test Funding")
            .withRelationConference("The best Conference")
            .withRelationProduct("DataSet")
            .withDescription("Description")
            .withDescriptionAbstract("Description Abstract")
            .withIsPartOf("Journal")
            .build();

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withHandle("123456789/002")
            .withTitle("Second Publication")
            .withIsbnIdentifier("ISBN-002")
            .withIssnIdentifier("ISSN-002")
            .withIssnIdentifier("ISSN-003")
            .withIsiIdentifier("ISI-002")
            .withScopusIdentifier("SCOPUS-002")
            .withSubject("export")
            .withType("Controlled Vocabulary for Resource Type Genres::text::review")
            .withIssueDate("2020-01-01")
            .withAuthor("Jesse Pinkman")
            .withAuthorAffiliation("Company")
            .withEditor("Editor")
            .withEditorAffiliation("Editor Affiliation")
            .withRelationProject("Test Project")
            .withRelationGrantno("01")
            .withRelationConference("Conference1")
            .withRelationConference("Conference2")
            .withRelationProduct("DataSet")
            .withDescription("Publication Description")
            .withCitationIdentifier("CIT-01")
            .build();

        context.restoreAuthSystemState();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCollectionCrosswalk.disseminate(context, collection, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(5));

        String firstItemId = firstItem.getID().toString();
        String secondItemId = secondItem.getID().toString();

        Sheet mainSheet = workbook.getSheetAt(0);
        String[] mainSheetHeader = { "ID", "dc.identifier.doi", "dc.identifier.scopus", "dc.identifier.isi",
            "dc.identifier.adsbibcode", "dc.identifier.pmid", "dc.identifier.arxiv", "dc.identifier.issn",
            "dc.identifier.other", "dc.identifier.ismn", "dc.identifier.govdoc",
            "dc.identifier.uri", "dc.identifier.isbn", "dc.title", "dc.title.alternative", "dc.date.issued",
            "dc.type", "dc.language.iso", "dc.subject", "dc.description.abstract", "dc.relation.publication",
            "dc.relation.isbn", "dc.relation.doi", "dc.relation.ispartof", "dc.relation.ispartofseries",
            "dc.relation.issn", "dc.coverage.publication", "dc.coverage.isbn", "dc.coverage.doi",
            "dc.description.sponsorship", "dc.description.volume", "dc.description.issue", "dc.description.startpage",
            "dc.description.endpage", "dc.relation.conference", "dc.relation.product",
            "dc.identifier.citation", "dc.description" };
        String[] mainSheetFirstRow = { firstItemId, "doi:111.111/publication", "99999999",
            "111-222-333", "", "", "", "2049-3630", "", "", "", "http://localhost:4000/handle/123456789/001",
            "978-3-16-148410-0", "Test Publication", "Alternative publication title", "2020-01-01",
            "Controlled Vocabulary for Resource Type Genres::text::review", "en", "test||export",
            "Description Abstract", "Published in publication", "ISBN-01", "doi:10.3972/test", "Journal", "", "", "",
            "", "", "", "", "", "", "", "The best Conference", "DataSet", "", "Description" };
        String[] mainSheetSecondRow = { secondItemId, "", "SCOPUS-002", "ISI-002", "", "",
            "", "ISSN-002||ISSN-003", "", "", "", "http://localhost:4000/handle/123456789/002", "ISBN-002",
            "Second Publication", "", "2020-01-01", "Controlled Vocabulary for Resource Type Genres::text::review", "",
            "export", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "Conference1||Conference2", "DataSet",
            "CIT-01", "Publication Description" };

        asserThatSheetHas(mainSheet, "items", 3, mainSheetHeader, Arrays.asList(mainSheetFirstRow, mainSheetSecondRow));

        Sheet authorSheet = workbook.getSheetAt(1);
        String[] authorSheetHeader = { "PARENT-ID", "dc.contributor.author", "oairecerif.author.affiliation" };
        String[] authorSheetFirstRow = { firstItemId, "John Smith", "" };
        String[] authorSheetSecondRow = { firstItemId, "Walter White", "Company" };
        String[] authorSheetThirdRow = { secondItemId, "Jesse Pinkman", "Company" };

        asserThatSheetHas(authorSheet, "dc.contributor.author", 4, authorSheetHeader, asList(authorSheetFirstRow,
            authorSheetSecondRow, authorSheetThirdRow));

        Sheet editorSheet = workbook.getSheetAt(2);
        String[] editorSheetHeader = { "PARENT-ID", "dc.contributor.editor", "oairecerif.editor.affiliation" };
        String[] editorSheetFirstRow = { secondItemId, "Editor", "Editor Affiliation" };

        List<String[]> rows = new ArrayList<String[]>();
        rows.add(editorSheetFirstRow);
        asserThatSheetHas(editorSheet, "dc.contributor.editor", 2, editorSheetHeader, rows);

        Sheet projectSheet = workbook.getSheetAt(3);
        String[] projectSheetHeader = { "PARENT-ID", "dc.relation.project", "dc.relation.grantno" };
        String[] projectSheetFirstRow = { firstItemId, "Test Project$$d9471fee-34fa-4a39-9658-443c4bb47b22$$600", "" };
        String[] projectSheetSecondRow = { secondItemId, "Test Project", "01" };

        asserThatSheetHas(projectSheet, "dc.relation.project", 3, projectSheetHeader, asList(projectSheetFirstRow,
            projectSheetSecondRow));
    }

    @Test
    public void testCollectionDisseminateWithEmptyCollection() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection collection = createCollection(context, community)
            .withAdminGroup(eperson)
            .build();
        context.restoreAuthSystemState();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCollectionCrosswalk.disseminate(context, collection, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(2));

        Sheet mainSheet = workbook.getSheetAt(0);
        String[] mainSheetHeader = { "ID", "dc.contributor.author", "dc.title", "dc.title.alternative",
            "dc.date.issued", "dc.publisher", "dc.identifier.citation", "dc.relation.ispartofseries",
            "dc.identifier.doi", "dc.identifier.scopus", "dc.identifier.isi", "dc.identifier.adsbibcode",
            "dc.identifier.pmid", "dc.identifier.arxiv", "dc.identifier.issn", "dc.identifier.other",
            "dc.identifier.ismn", "dc.identifier.govdoc", "dc.identifier.uri", "dc.identifier.isbn",
            "dc.type", "dc.language.iso", "dc.subject", "dc.description.abstract", "dc.description.sponsorship",
            "dc.description" };
        assertThat(mainSheet.getPhysicalNumberOfRows(), equalTo(1));
        assertThat(getRowValues(mainSheet.getRow(0), mainSheetHeader.length), contains(mainSheetHeader));
    }

    @Test
    public void testCollectionDisseminateWithMockSubmissionFormConfiguration() throws Exception {

        try {
            DCInputsReader reader = mock(DCInputsReader.class);

            context.turnOffAuthorisationSystem();

            Collection collection = createCollection(context, community)
                .withSubmissionDefinition("publication")
                .withAdminGroup(eperson)
                .build();

            List<String> publicationMetadataFields = asList("dc.title", "dc.date.issued", "dc.subject");
            List<String> publicationMetadataFieldGroups = asList("dc.contributor.author");
            List<String> authorGroup = asList("dc.contributor.author", "oairecerif.author.affiliation");

            when(reader.getLanguagesForMetadata(collection, "dc.title", false)).thenReturn(Arrays.asList("en", "it"));
            when(reader.getSubmissionFormMetadata(collection)).thenReturn(publicationMetadataFields);
            when(reader.getSubmissionFormMetadataGroups(collection)).thenReturn(publicationMetadataFieldGroups);
            when(reader.getAllNestedMetadataByGroupName(collection, "dc.contributor.author")).thenReturn(authorGroup);

            xlsCollectionCrosswalk.setReader(reader);

            Item firstPublication = ItemBuilder.createItem(context, collection)
                .withEntityType("Publication")
                .withTitle("First publication")
                .withTitleForLanguage("Prima pubblicazione", "it")
                .withTitleForLanguage("Primera publicacion", "es")
                .withIssueDate("2020-01-01")
                .withAuthor("Walter White", "0ecd5452-aae2-4c18-9aec-0471bdcbadbc")
                .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
                .withAuthor("Jesse Pinkman")
                .withAuthorAffiliation("Company")
                .build();

            Item secondPublication = ItemBuilder.createItem(context, collection)
                .withEntityType("Publication")
                .withTitleForLanguage("Seconda pubblicazione", "it")
                .withTitleForLanguage("Second publication", "en")
                .withIssueDate("2019-01-01")
                .build();

            Item thirdPublication = ItemBuilder.createItem(context, collection)
                .withEntityType("Publication")
                .withTitle("Third publication")
                .withTitleForLanguage("Terza pubblicazione", "it")
                .withIssueDate("2018-01-01")
                .withAuthor("Carl Johnson")
                .build();

            Item fourthPublication = ItemBuilder.createItem(context, collection)
                .withEntityType("Publication")
                .withTitleForLanguage("Pubblicazione", "it")
                .withTitle("Fourth publication")
                .withIssueDate("2017-01-01")
                .withAuthor("Carl Johnson")
                .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
                .withAuthor("Red White")
                .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
                .withSubject("test")
                .withSubject("export")
                .build();

            context.restoreAuthSystemState();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            xlsCollectionCrosswalk.disseminate(context, collection, baos);

            Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));

            assertThat(workbook.getNumberOfSheets(), equalTo(3));

            String firstId = firstPublication.getID().toString();
            String secondId = secondPublication.getID().toString();
            String thirdId = thirdPublication.getID().toString();
            String fourthId = fourthPublication.getID().toString();

            Sheet mainSheet = workbook.getSheetAt(0);
            String[] header = { "ID", "dc.title", "dc.date.issued", "dc.subject", "dc.title[it]", "dc.title[en]" };
            String[] firstRow = { firstId, "First publication", "2020-01-01", "", "Prima pubblicazione", "" };
            String[] secondRow = { secondId, "", "2019-01-01", "", "Seconda pubblicazione", "Second publication" };
            String[] thirdRow = { thirdId, "Third publication", "2018-01-01", "", "Terza pubblicazione", "" };
            String[] fourthRow = { fourthId, "Fourth publication", "2017-01-01", "test||export", "Pubblicazione", "" };

            asserThatSheetHas(mainSheet, "items", 5, header, asList(firstRow, secondRow, thirdRow, fourthRow));

            Sheet authorSheet = workbook.getSheetAt(1);
            String[] authorSheetHeader = { "PARENT-ID", "dc.contributor.author", "oairecerif.author.affiliation" };
            String[] authorSheetFirstRow = { firstId, "Walter White$$0ecd5452-aae2-4c18-9aec-0471bdcbadbc$$600", "" };
            String[] authorSheetSecondRow = { firstId, "Jesse Pinkman", "Company" };
            String[] authorSheetThirdRow = { thirdId, "Carl Johnson", "" };
            String[] authorSheetFourthRow = { fourthId, "Carl Johnson", "" };
            String[] authorSheetFifthRow = { fourthId, "Red White", "" };

            asserThatSheetHas(authorSheet, "dc.contributor.author", 6, authorSheetHeader, asList(authorSheetFirstRow,
                authorSheetSecondRow, authorSheetThirdRow, authorSheetFourthRow, authorSheetFifthRow));

        } finally {
            this.xlsCollectionCrosswalk.setReader(new DCInputsReader());
        }

    }

    @Test
    public void testManyItemsDisseminate() throws Exception {
        // skip test based on configuration
        Assume.assumeFalse(configurationService.getBooleanProperty("test.skip.cris", false));

        context.turnOffAuthorisationSystem();
        Collection collection = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withEntityType("Publication")
            .withAdminGroup(eperson)
            .build();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withHandle("123456789/001")
            .withTitle("Test Publication")
            .withAlternativeTitle("Alternative publication title")
            .withRelationPublication("Published in publication")
            .withRelationDoi("doi:10.3972/test")
            .withRelationIsbn("ISBN-01")
            .withDoiIdentifier("doi:111.111/publication")
            .withIsbnIdentifier("978-3-16-148410-0")
            .withIssnIdentifier("2049-3630")
            .withIsiIdentifier("111-222-333")
            .withScopusIdentifier("99999999")
            .withLanguage("en")
            .withPublisher("Publication publisher")
            .withSubject("test")
            .withSubject("export")
            .withType("Controlled Vocabulary for Resource Type Genres::text::review")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthor("Walter White")
            .withAuthorAffiliation("Company")
            .withRelationProject("Test Project", "d9471fee-34fa-4a39-9658-443c4bb47b22")
            .withRelationGrantno(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withRelationFunding("Test Funding")
            .withRelationConference("The best Conference")
            .withRelationProduct("DataSet")
            .withDescription("Description")
            .withDescriptionAbstract("Description Abstract")
            .withIsPartOf("Journal")
            .build();

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withHandle("123456789/002")
            .withTitle("Second Publication")
            .withIsbnIdentifier("ISBN-002")
            .withIssnIdentifier("ISSN-002")
            .withIssnIdentifier("ISSN-003")
            .withIsiIdentifier("ISI-002")
            .withScopusIdentifier("SCOPUS-002")
            .withSubject("export")
            .withType("Controlled Vocabulary for Resource Type Genres::text::review")
            .withIssueDate("2020-01-01")
            .withAuthor("Jesse Pinkman")
            .withAuthorAffiliation("Company")
            .withEditor("Editor")
            .withEditorAffiliation("Editor Affiliation")
            .withRelationProject("Test Project")
            .withRelationGrantno("01")
            .withRelationConference("Conference1")
            .withRelationConference("Conference2")
            .withRelationProduct("DataSet")
            .withDescription("Publication Description")
            .withCitationIdentifier("CIT-01")
            .build();

        WorkspaceItem thirdItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle("Third Publication")
            .withDoiIdentifier("doi:222.111/publication")
            .withSubject("test")
            .withSubject("export")
            .withType("Controlled Vocabulary for Resource Type Genres::text::review")
            .withIssueDate("2022-01-01")
            .withAuthor("Luca Giamminonni")
            .withAuthorAffilitation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();

        context.restoreAuthSystemState();

        Iterator<Item> itemIterator = IteratorUtils.arrayIterator(firstItem, secondItem, thirdItem.getItem());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCollectionCrosswalk.disseminate(context, itemIterator, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(5));

        String firstItemId = firstItem.getID().toString();
        String secondItemId = secondItem.getID().toString();
        String thirdItemId = thirdItem.getItem().getID().toString();

        Sheet mainSheet = workbook.getSheetAt(0);
        String[] mainSheetHeader = { "ID", "dc.identifier.doi", "dc.identifier.scopus", "dc.identifier.isi",
            "dc.identifier.adsbibcode", "dc.identifier.pmid", "dc.identifier.arxiv", "dc.identifier.issn",
            "dc.identifier.other", "dc.identifier.ismn", "dc.identifier.govdoc",
            "dc.identifier.uri", "dc.identifier.isbn", "dc.title", "dc.title.alternative", "dc.date.issued",
            "dc.type", "dc.language.iso", "dc.subject", "dc.description.abstract", "dc.relation.publication",
            "dc.relation.isbn", "dc.relation.doi", "dc.relation.ispartof", "dc.relation.ispartofseries",
            "dc.relation.issn", "dc.coverage.publication", "dc.coverage.isbn", "dc.coverage.doi",
            "dc.description.sponsorship", "dc.description.volume", "dc.description.issue", "dc.description.startpage",
            "dc.description.endpage", "dc.relation.conference", "dc.relation.product",
            "dc.identifier.citation", "dc.description" };
        String[] mainSheetFirstRow = { firstItemId, "doi:111.111/publication", "99999999",
            "111-222-333", "", "", "", "2049-3630", "", "", "", "http://localhost:4000/handle/123456789/001",
            "978-3-16-148410-0", "Test Publication", "Alternative publication title", "2020-01-01",
            "Controlled Vocabulary for Resource Type Genres::text::review", "en", "test||export",
            "Description Abstract", "Published in publication", "ISBN-01", "doi:10.3972/test", "Journal", "", "", "",
            "", "", "", "", "", "", "", "The best Conference", "DataSet", "", "Description" };
        String[] mainSheetSecondRow = { secondItemId, "", "SCOPUS-002", "ISI-002", "", "",
            "", "ISSN-002||ISSN-003", "", "", "", "http://localhost:4000/handle/123456789/002", "ISBN-002",
            "Second Publication", "", "2020-01-01", "Controlled Vocabulary for Resource Type Genres::text::review", "",
            "export", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "Conference1||Conference2", "DataSet",
            "CIT-01", "Publication Description" };
        String[] mainSheetThirdRow = { thirdItemId, "doi:222.111/publication", "", "", "", "", "", "", "", "", "", "",
            "", "Third Publication", "", "2022-01-01", "Controlled Vocabulary for Resource Type Genres::text::review",
            "", "test||export", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" };

        asserThatSheetHas(mainSheet, "items", 4, mainSheetHeader,
            asList(mainSheetFirstRow, mainSheetSecondRow, mainSheetThirdRow));

        Sheet authorSheet = workbook.getSheetAt(1);
        String[] authorSheetHeader = { "PARENT-ID", "dc.contributor.author", "oairecerif.author.affiliation" };
        String[] authorSheetFirstRow = { firstItemId, "John Smith", "" };
        String[] authorSheetSecondRow = { firstItemId, "Walter White", "Company" };
        String[] authorSheetThirdRow = { secondItemId, "Jesse Pinkman", "Company" };
        String[] authorSheetFourthRow = { thirdItemId, "Luca Giamminonni", "" };

        asserThatSheetHas(authorSheet, "dc.contributor.author", 5, authorSheetHeader,
            asList(authorSheetFirstRow, authorSheetSecondRow, authorSheetThirdRow, authorSheetFourthRow));

        Sheet editorSheet = workbook.getSheetAt(2);
        String[] editorSheetHeader = { "PARENT-ID", "dc.contributor.editor", "oairecerif.editor.affiliation" };
        String[] editorSheetFirstRow = { secondItemId, "Editor", "Editor Affiliation" };

        List<String[]> rows = new ArrayList<String[]>();
        rows.add(editorSheetFirstRow);
        asserThatSheetHas(editorSheet, "dc.contributor.editor", 2, editorSheetHeader, rows);

        Sheet projectSheet = workbook.getSheetAt(3);
        String[] projectSheetHeader = { "PARENT-ID", "dc.relation.project", "dc.relation.grantno" };
        String[] projectSheetFirstRow = { firstItemId, "Test Project$$d9471fee-34fa-4a39-9658-443c4bb47b22$$600", "" };
        String[] projectSheetSecondRow = { secondItemId, "Test Project", "01" };

        asserThatSheetHas(projectSheet, "dc.relation.project", 3, projectSheetHeader, asList(projectSheetFirstRow,
            projectSheetSecondRow));
    }

    @Test
    public void testManyItemsDisseminateWithWrongObjectTypes() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection collection = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withEntityType("Publication")
            .withAdminGroup(eperson)
            .build();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withHandle("123456789/001")
            .withTitle("Test Publication")
            .build();

        context.restoreAuthSystemState();

        Iterator<? extends DSpaceObject> iterator = IteratorUtils.arrayIterator(firstItem, admin);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        IllegalArgumentException argumentException = Assert.assertThrows(IllegalArgumentException.class,
            () -> xlsCollectionCrosswalk.disseminate(context, iterator, baos));

        assertThat(argumentException.getMessage(), is("The xsl export supports only items. "
            + "Found object with type " + Constants.EPERSON + " and id " + admin.getID()));
    }

    @Test
    public void testManyItemsDisseminateWithDifferentCollections() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection collection1 = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withEntityType("Publication")
            .withAdminGroup(eperson)
            .build();
        Collection collection2 = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withEntityType("Publication")
            .withAdminGroup(eperson)
            .build();

        Item firstItem = ItemBuilder.createItem(context, collection1)
            .withHandle("123456789/001")
            .withTitle("Test Publication")
            .build();

        Item secondItem = ItemBuilder.createItem(context, collection2)
            .withHandle("123456789/002")
            .withTitle("Second Publication")
            .build();

        context.restoreAuthSystemState();

        Iterator<Item> itemIterator = IteratorUtils.arrayIterator(firstItem, secondItem);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        IllegalArgumentException argumentException = Assert.assertThrows(IllegalArgumentException.class,
            () -> xlsCollectionCrosswalk.disseminate(context, itemIterator, baos));

        assertThat(argumentException.getMessage(), is("It is not possible to export items from two different"
            + " collections: item " + secondItem.getID() + " is not in collection " + collection1.getID()));
    }

    @Test
    public void testDisseminateWithUnexpectedLanguageOnMetadataValues() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection collection = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withHandle("123456789/001")
            .withTitleForLanguage("Test Publication", "de")
            .withAuthorForLanguage("John Smith", "de")
            .withAuthorAffiliationForLanguage("4Science", "de")
            .withDescriptionAbstract("Description Abstract")
            .withDoiIdentifierForLanguage("XXX", "de")
            .build();

        context.restoreAuthSystemState();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCollectionCrosswalk.disseminate(context, collection, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(5));

        String itemId = item.getID().toString();

        Sheet mainSheet = workbook.getSheetAt(0);
        String[] mainSheetHeader = { "ID", "dc.identifier.doi", "dc.identifier.scopus", "dc.identifier.isi",
            "dc.identifier.adsbibcode", "dc.identifier.pmid", "dc.identifier.arxiv", "dc.identifier.issn",
            "dc.identifier.other", "dc.identifier.ismn", "dc.identifier.govdoc",
            "dc.identifier.uri", "dc.identifier.isbn", "dc.title", "dc.title.alternative", "dc.date.issued",
            "dc.type", "dc.language.iso", "dc.subject", "dc.description.abstract", "dc.relation.publication",
            "dc.relation.isbn", "dc.relation.doi", "dc.relation.ispartof", "dc.relation.ispartofseries",
            "dc.relation.issn", "dc.coverage.publication", "dc.coverage.isbn", "dc.coverage.doi",
            "dc.description.sponsorship", "dc.description.volume", "dc.description.issue", "dc.description.startpage",
            "dc.description.endpage", "dc.relation.conference", "dc.relation.product",
            "dc.identifier.citation", "dc.description" };

        String[] mainSheetRow = { itemId, "", "", "", "", "", "", "", "", "", "",
            "http://localhost:4000/handle/123456789/001", "", "", "", "", "", "", "", "Description Abstract", "", "",
            "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" };

        List<String[]> rows = new ArrayList<String[]>();
        rows.add(mainSheetRow);
        asserThatSheetHas(mainSheet, "items", 2, mainSheetHeader, rows);

        Sheet authorSheet = workbook.getSheetAt(1);
        String[] authorSheetHeader = { "PARENT-ID", "dc.contributor.author", "oairecerif.author.affiliation" };
        asserThatSheetHas(authorSheet, "dc.contributor.author", 2, authorSheetHeader, List.of());

        Sheet editorSheet = workbook.getSheetAt(2);
        String[] editorSheetHeader = { "PARENT-ID", "dc.contributor.editor", "oairecerif.editor.affiliation" };
        asserThatSheetHas(editorSheet, "dc.contributor.editor", 1, editorSheetHeader, List.of());

        Sheet projectSheet = workbook.getSheetAt(3);
        String[] projectSheetHeader = { "PARENT-ID", "dc.relation.project", "dc.relation.grantno" };
        asserThatSheetHas(projectSheet, "dc.relation.project", 1, projectSheetHeader, List.of());
    }

    @Test
    public void testCollectionDisseminateWithBitstreamSheet() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection collection = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test Publication")
            .withDescription("Description")
            .build();

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Second Publication")
            .withDescription("Publication Description")
            .build();

        // Create first bundle and bitstream
        Bundle firstBundle = bundleService.create(context, firstItem, "TEST-BUNDLE");
        Bitstream firstBitstream = bitstreamService.create(context, getBitstreamSample("First bitstream sample"));
        bundleService.addBitstream(context, firstBundle, firstBitstream);

        // Add metadata to the first bitstream
        bitstreamService.addMetadata(context, firstBitstream, "dc", "title",
                                     null, null, List.of("test.txt"));
        bitstreamService.addMetadata(context, firstBitstream, "dc", "description",
                                     null, null, List.of("test description 1"));

        // Create second bundle and bitstream
        Bundle secondBundle = bundleService.create(context, secondItem, "TEST-BUNDLE2");
        Bitstream secondBitstream = bitstreamService.create(context, getBitstreamSample("Second bitstream sample"));
        bundleService.addBitstream(context, secondBundle, secondBitstream);

        // Add metadata to the second bitstream
        bitstreamService.addMetadata(context, secondBitstream, "dc", "title",
                                     null, null, List.of("test2.txt"));
        bitstreamService.addMetadata(context, secondBitstream, "dc", "description",
                                     null, null, List.of("test description 2"));

        context.restoreAuthSystemState();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCollectionCrosswalk.disseminate(context, collection, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(5));

        String firstItemId = firstItem.getID().toString();
        String secondItemId = secondItem.getID().toString();

        String[] bitstreamSheetHeaders = Stream.concat(
                XlsCollectionCrosswalk.BITSTREAM_BASE_HEADERS.stream(),
                Stream.of("dc.description", "dc.title")
            )
            .toArray(String[]::new);
        String[] firstRow = { firstItemId, getBitstreamLocationUrl(firstBitstream), "TEST-BUNDLE", "test description 1",
                "test.txt" };
        String[] secondRow = { secondItemId, getBitstreamLocationUrl(secondBitstream), "TEST-BUNDLE2",
                "test description 2", "test2.txt" };

        asserThatSheetHas(workbook.getSheetAt(4), "bitstream-metadata", 3,
                          bitstreamSheetHeaders, List.of(firstRow, secondRow));
    }

    @Test
    public void testCollectionDisseminateWithBitstreamSheetOneBitstream() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection collection = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test Publication")
            .withDescription("Description")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Second Publication")
            .withDescription("Publication Description")
            .build();

        // Create first bundle and bitstream
        Bundle firstBundle = bundleService.create(context, firstItem, "TEST-BUNDLE");
        Bitstream firstBitstream = bitstreamService.create(context, getBitstreamSample("First bitstream sample"));
        bundleService.addBitstream(context, firstBundle, firstBitstream);

        // Add metadata to the first bitstream
        bitstreamService.addMetadata(context, firstBitstream, "dc", "title",
                                     null, null, List.of("test.txt"));
        bitstreamService.addMetadata(context, firstBitstream, "dc", "description",
                                     null, null, List.of("test description 1"));

        context.restoreAuthSystemState();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCollectionCrosswalk.disseminate(context, collection, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(5));

        String firstItemId = firstItem.getID().toString();

        String[] bitstreamSheetHeaders = Stream.concat(
                XlsCollectionCrosswalk.BITSTREAM_BASE_HEADERS.stream(),
                Stream.of("dc.description", "dc.title")
            )
            .toArray(String[]::new);
        String[] firstRow = { firstItemId, getBitstreamLocationUrl(firstBitstream), "TEST-BUNDLE", "test description 1",
                "test.txt" };

        List<String[]> rowList = new ArrayList<>();
        rowList.add(firstRow);

        asserThatSheetHas(workbook.getSheetAt(4), "bitstream-metadata", 2, bitstreamSheetHeaders, rowList);
    }

    @Test
    public void testCollectionDisseminateWithBitstreamSheetEmptyBitstream() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection collection = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test Publication")
            .withDescription("Description")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Second Publication")
            .withDescription("Publication Description")
            .build();

        context.restoreAuthSystemState();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCollectionCrosswalk.disseminate(context, collection, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(5));

        String[] bitstreamSheetHeaders = XlsCollectionCrosswalk.BITSTREAM_BASE_HEADERS.stream().toArray(String[]::new);
        asserThatSheetHas(workbook.getSheetAt(4), "bitstream-metadata", 1, bitstreamSheetHeaders, List.of());
    }

    private InputStream getBitstreamSample(String text) {
        return new ByteArrayInputStream(text.getBytes());
    }

    @SuppressWarnings("unchecked")
    private void asserThatSheetHas(Sheet sheet, String name, int rowsNumber, String[] header, List<String[]> rows) {
        assertThat(sheet.getSheetName(), equalTo(name));
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(rowsNumber));
        assertThat(getRowValues(sheet.getRow(0), header.length), contains(header));

        int rowCount = 1;
        Matcher<List<String>>[] rowMatchers = rows.stream()
            .map(Matchers::contains)
            .toArray(Matcher[]::new);

        for (String[] row : rows) {
            assertThat(getRowValues(sheet.getRow(rowCount++), row.length), anyOf(rowMatchers));
        }
    }

    private String getBitstreamLocationUrl(Bitstream bitstream) {
        String dspaceServerUrl = configurationService.getProperty("dspace.server.url");
        return String.format(BITSTREAM_URL_FORMAT, dspaceServerUrl, bitstream.getID().toString());
    }

}
