/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.dspace.builder.ItemBuilder.createItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.sql.SQLException;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the {@link ReferCrosswalk}.
 *
 * <p>NOTE: only the endnote dissemination test is ported so far (DURACOM-540
 * ticket 06). The remaining tests of the CRIS version of this class are ported
 * in DURACOM-540 ticket 08.</p>
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ReferCrosswalkIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_OUTPUT_DIR_PATH = "./target/testing/dspace/assetstore/crosswalk/";

    private StreamDisseminationCrosswalkMapper crosswalkMapper;

    private Community community;

    private Collection collection;

    @Before
    public void setup() throws SQLException, AuthorizeException {

        this.crosswalkMapper = new DSpace().getSingletonService(StreamDisseminationCrosswalkMapper.class);
        assertThat(crosswalkMapper, notNullValue());

        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community).withAdminGroup(eperson).build();
        context.restoreAuthSystemState();

    }

    @Test
    public void testPublicationEndnoteDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item publication = createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test Publication")
            .withDoiIdentifier("doi:111.111/publication")
            .withHandle("123456789/xxx")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith")
            .withAuthor("Walter White")
            .withAuthor("Jessie Pinkman")
            .withIsPartOf("Journal")
            .withPublisher("Publisher")
            .withVolume("V.01")
            .withIssue("Issue")
            .withDescriptionAbstract("This is a publication to test export")
            .withSubject("test")
            .withSubject("publication")
            .withCitationStartPage("2")
            .withCitationEndPage("20")
            .withLanguage("en")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("endnote");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, publication, out);

        try (FileInputStream fis = getFileInputStream("endnote")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    private void compareEachLine(String result, String expectedResult) {

        String[] resultLines = result.split("\n");
        String[] expectedResultLines = expectedResult.split("\n");

        assertThat(sameLineCountReason(resultLines, expectedResultLines),
            resultLines.length, equalTo(expectedResultLines.length));

        for (int i = 0; i < resultLines.length; i++) {
            if (expectedResultLines[i].contains("SKIP-IN-COMPARING")) {
                continue;
            }
            assertThat(removeTabs(resultLines[i]), equalTo(removeTabs(expectedResultLines[i])));
        }
    }

    private String sameLineCountReason(String[] resultLines, String[] expectedResultLines) {
        String message = "The result should have the same lines number of the expected result.";
        String result = String.join("\n", resultLines);
        String expectedResult = String.join("\n", expectedResultLines);
        return message + "\nExpected:\n" + expectedResult + "\nActual:\n" + result;
    }

    private String removeTabs(String string) {
        return string != null ? string.replace("\t", "").trim() : null;
    }

    private FileInputStream getFileInputStream(String name) throws FileNotFoundException {
        return new FileInputStream(new File(BASE_OUTPUT_DIR_PATH, name));
    }
}
