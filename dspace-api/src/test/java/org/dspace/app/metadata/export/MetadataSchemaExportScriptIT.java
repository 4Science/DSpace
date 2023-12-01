/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metadata.export;

import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.MetadataFieldBuilder;
import org.dspace.builder.MetadataSchemaBuilder;
import org.dspace.content.MetadataSchema;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;


/**
 * Integration tests for {@link MetadataSchemaExportScript}
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 *
 */
public class MetadataSchemaExportScriptIT extends AbstractIntegrationTestWithDatabase {

    private final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    private MetadataSchema schema;
    private List<MetadataFieldBuilder> fields;
    private String fileLocation;

    @Before
    @SuppressWarnings("deprecation")
    public void beforeTests() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        schema = createMetadataSchema();
        fields = createFields();
        fileLocation = configurationService.getProperty("dspace.dir");
        context.restoreAuthSystemState();
    }

    private List<MetadataFieldBuilder> createFields() throws SQLException, AuthorizeException {
        return List.of(
            MetadataFieldBuilder.createMetadataField(context, schema, "first", "metadata", "notes first"),
            MetadataFieldBuilder.createMetadataField(context, schema, "second", "metadata", "notes second"),
            MetadataFieldBuilder.createMetadataField(context, schema, "third", "metadata", "notes third"),
            MetadataFieldBuilder.createMetadataField(context, schema, "element", null, null)
        );
    }

    private MetadataSchema createMetadataSchema() throws SQLException, AuthorizeException {
        return MetadataSchemaBuilder.createMetadataSchema(context, "test", "http://dspace.org/test").build();
    }

    @Test
    public void testMetadataSchemaExport() throws Exception {

        File xml = new File(fileLocation + "/test-types.xml");
        xml.deleteOnExit();

        String[] args =
            new String[] {
                "export-schema",
                "-i", schema.getID().toString(),
                "-f", xml.getAbsolutePath()
            };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(
            handler.getInfoMessages(),
            hasItem("Exporting the metadata-schema file for the schema " + schema.getName())
        );
        assertThat("The xml file should be created", xml.exists(), is(true));


        try (FileInputStream fis = new FileInputStream(xml)) {
            String content = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(content, containsString("<dc-schema>"));
            assertThat(content, containsString("<name>test</name>"));
            assertThat(content, containsString("<namespace>http://dspace.org/test</namespace>"));
            assertThat(content, containsString("<dc-type>"));
            assertThat(content, containsString("<schema>test</schema>"));
            assertThat(content, containsString("<element>first</element>"));
            assertThat(content, containsString("<qualifier>metadata</qualifier>"));
            assertThat(content, containsString("<scope_note>notes first</scope_note>"));
            assertThat(content, containsString("</dc-type>"));
            assertThat(content, containsString("<dc-type>"));
            assertThat(content, containsString("<schema>test</schema>"));
            assertThat(content, containsString("<element>third</element>"));
            assertThat(content, containsString("<qualifier>metadata</qualifier>"));
            assertThat(content, containsString("<scope_note>notes third</scope_note>"));
            assertThat(content, containsString("</dc-type>"));
            assertThat(content, containsString("<dc-type>"));
            assertThat(content, containsString("<schema>test</schema>"));
            assertThat(content, containsString("<element>element</element>"));
            assertThat(content, containsString("</dc-type>"));
        }
    }

    @Test
    public void testMetadataNotExistingSchemaExport() throws Exception {

        File xml = new File(fileLocation + "/test-types.xml");
        xml.deleteOnExit();

        String[] args =
            new String[] {
                    "export-schema",
                    "-i", "-1",
                    "-f", xml.getAbsolutePath()
        };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        assertThat(handler.getErrorMessages(), hasItem("Cannot find the metadata-schema with id: -1"));
        assertThat("The xml file should not be created", xml.exists(), is(false));
    }

}
