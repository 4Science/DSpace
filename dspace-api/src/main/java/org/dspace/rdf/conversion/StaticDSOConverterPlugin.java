/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rdf.conversion;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.rdf.RDFUtil;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class StaticDSOConverterPlugin
    implements ConverterPlugin {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(StaticDSOConverterPlugin.class);

    public static final String CONSTANT_DATA_FILENAME_KEY_PREFIX = "rdf.constant.data.";
    public static final String CONSTANT_DATA_GENERAL_KEY_SUFFIX = "GENERAL";

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Override
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public Model convert(Context context, DSpaceObject dso)
        throws SQLException {
        // As we do not use data of any DSpaceObject, we do not have to check
        // permissions here. We provide only static data out of configuration
        // files.

        Model general = this.readFile(CONSTANT_DATA_GENERAL_KEY_SUFFIX,
                                      RDFUtil.generateIdentifier(context, dso));
        Model typeSpecific = this
            .readFile(ContentServiceFactory.getInstance().getDSpaceObjectService(dso).getTypeText(dso),
                      RDFUtil.generateIdentifier(context, dso));

        if (general == null) {
            return typeSpecific;
        }
        if (typeSpecific == null) {
            return general;
        }
        typeSpecific.setNsPrefixes(general);
        typeSpecific.add(general);
        general.close();
        return typeSpecific;
    }

    protected Model readFile(String fileSuffix, String base) {
        String path = configurationService.getProperty(
            CONSTANT_DATA_FILENAME_KEY_PREFIX + fileSuffix);
        if (path == null) {
            log.error("Cannot find dspace-rdf configuration (looking for "
                          + "property " + CONSTANT_DATA_FILENAME_KEY_PREFIX
                          + fileSuffix + ")!");

            throw new RuntimeException("Cannot find dspace-rdf configuration "
                                           + "(looking for property " +
                                           CONSTANT_DATA_FILENAME_KEY_PREFIX + fileSuffix + ")!");
        }

        log.debug("Going to read static data from file '" + path + "'.");
        InputStream is = null;
        Model staticDataModel = null;
        try {
            try {
                is = interpolateFile(path);
            } catch (IOException e) {
                throw new RuntimeException("Error during processing of the configuration file " + path, e);
            }
            if (is == null) {
                log.warn("StaticDSOConverterPlugin cannot find file '" + path
                             + "', ignoring...");
                return null;
            }

            staticDataModel = ModelFactory.createDefaultModel();
            staticDataModel.read(is, base, FileUtils.guessLang(path));
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    // nothing to do here.
                }
            }
        }
        if (staticDataModel.isEmpty()) {
            staticDataModel.close();
            return null;
        }
        return staticDataModel;
    }

    /**
     * Process the rdf configuration file to interpolate variable using the DSpace ConfigurationService
     *
     * @param filePath      full path to the rdf configuration file to process
     * @return InputStream  representing the output obtained from the processed file
     * @throws IOException
     */
    public InputStream interpolateFile(String filePath) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String transformed = (String) configurationService.getConfiguration().getInterpolator()
                        .interpolate(line);
                out.write(transformed.getBytes(StandardCharsets.UTF_8));
                out.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
            }
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    @Override
    public boolean supports(int type) {
        switch (type) {
            case (Constants.COLLECTION):
                return true;
            case (Constants.COMMUNITY):
                return true;
            case (Constants.ITEM):
                return true;
            case (Constants.SITE):
                return true;
            default:
                return false;
        }
    }
}
